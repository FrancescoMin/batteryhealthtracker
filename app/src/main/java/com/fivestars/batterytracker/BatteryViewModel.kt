package com.fivestars.batterytracker

import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BatteryViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "BatteryViewModel"
    private val database = AppDatabase.getDatabase(application)
    private val preferences = BatteryPreferences(application)
    private val repository = BatteryRepository(application)
    private val workManager = WorkManager.getInstance(application)

    val batteryConfigState: StateFlow<BatteryConfigState> = preferences.configState
    val appLanguage: StateFlow<String> = preferences.appLanguage
    val appThemeMode: StateFlow<AppThemeMode> = preferences.appThemeMode
    val samplingConfig: StateFlow<SamplingConfig> = preferences.samplingConfig

    fun setAppLanguage(langCode: String) {
        preferences.setAppLanguage(langCode)
    }

    fun setAppThemeMode(mode: AppThemeMode) {
        preferences.setAppThemeMode(mode)
    }

    fun setSamplingEnabled(enabled: Boolean) {
        preferences.setSamplingEnabled(enabled)
        val config = preferences.getSamplingConfig()
        BatteryWorkScheduler.schedule(
            context = getApplication(),
            isEnabled = enabled,
            intervalHours = config.intervalHours,
            targetHour = config.targetHour,
            targetMinute = config.targetMinute,
            forceReschedule = true
        )
    }

    fun setSamplingIntervalHours(hours: Long) {
        preferences.setSamplingIntervalHours(hours)
        val config = preferences.getSamplingConfig()
        BatteryWorkScheduler.schedule(
            context = getApplication(),
            isEnabled = config.isEnabled,
            intervalHours = hours,
            targetHour = config.targetHour,
            targetMinute = config.targetMinute,
            forceReschedule = true
        )
    }

    fun setSamplingTime(hour: Int, minute: Int) {
        preferences.setSamplingTime(hour, minute)
        val config = preferences.getSamplingConfig()
        BatteryWorkScheduler.schedule(
            context = getApplication(),
            isEnabled = config.isEnabled,
            intervalHours = config.intervalHours,
            targetHour = hour,
            targetMinute = minute,
            forceReschedule = true
        )
    }

    val allBatteryData: StateFlow<List<BatteryData>> = database.batteryDao()
        .getAllActiveBatteryData()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedBatteryData: StateFlow<List<BatteryData>> = database.batteryDao()
        .getAllTrashedBatteryData()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestBatteryData: StateFlow<BatteryData?> = database.batteryDao()
        .getLatestBatteryData()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentSnapshot = MutableStateFlow<BatterySnapshot?>(null)
    val currentSnapshot: StateFlow<BatterySnapshot?> = _currentSnapshot.asStateFlow()

    private val _livePowerData = MutableStateFlow<LivePowerData?>(null)
    val livePowerData: StateFlow<LivePowerData?> = _livePowerData.asStateFlow()

    private var livePowerJob: Job? = null

    fun startLivePowerStream() {
        if (livePowerJob?.isActive == true) return
        livePowerJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                try {
                    val currentBaseProtocol = _currentSnapshot.value?.chargingProtocol
                    val live = repository.getLivePowerData(currentBaseProtocol)
                    _livePowerData.value = live
                } catch (e: Exception) {
                    Log.w(tag, "Error updating live power data", e)
                }
                kotlinx.coroutines.delay(1500)
            }
        }
    }

    fun stopLivePowerStream() {
        livePowerJob?.cancel()
        livePowerJob = null
    }

    private val _isShizukuAvailable = MutableStateFlow(false)
    val isShizukuAvailable: StateFlow<Boolean> = _isShizukuAvailable.asStateFlow()

    private val _isShizukuPermissionGranted = MutableStateFlow(false)
    val isShizukuPermissionGranted: StateFlow<Boolean> = _isShizukuPermissionGranted.asStateFlow()

    private val _isRefreshing = MutableStateFlow(true)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _updateCheckResult = MutableStateFlow<UpdateCheckResult?>(null)
    val updateCheckResult: StateFlow<UpdateCheckResult?> = _updateCheckResult.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    val nextWorkScheduleTime: Flow<Long?> = workManager
        .getWorkInfosForUniqueWorkFlow(BatteryWorkScheduler.WORK_NAME)
        .map { workInfos ->
            val workInfo = workInfos.firstOrNull {
                it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
            }
            workInfo?.nextScheduleTimeMillis
        }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_PERMISSION_CODE) {
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            _isShizukuPermissionGranted.value = granted
            Log.d(tag, "Risultato richiesta permesso Shizuku: $granted")
            refreshSnapshot()
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkShizukuStatus()
        refreshSnapshot()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        checkShizukuStatus()
    }

    init {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionListener)
        } catch (e: Throwable) {
            Log.e(tag, "Errore registrazione listener Shizuku", e)
        }
        checkShizukuStatus()
        refreshSnapshot()
    }

    override fun onCleared() {
        super.onCleared()
        stopLivePowerStream()
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionListener)
        } catch (e: Throwable) {
            Log.e(tag, "Errore rimozione listener Shizuku", e)
        }
    }

    fun checkShizukuStatus() {
        val available = repository.isShizukuAvailable()
        val granted = repository.isShizukuPermissionGranted()
        Log.d(tag, "checkShizukuStatus: available=$available, granted=$granted")
        _isShizukuAvailable.value = available
        _isShizukuPermissionGranted.value = granted
    }

    fun requestShizukuPermission() {
        if (_isShizukuAvailable.value && !_isShizukuPermissionGranted.value) {
            try {
                Shizuku.requestPermission(SHIZUKU_PERMISSION_CODE)
            } catch (e: Throwable) {
                Log.e(tag, "Errore nella richiesta del permesso Shizuku", e)
            }
        }
    }

    private var refreshJob: Job? = null

    fun refreshSnapshot(resetProbe: Boolean = false) {
        if (refreshJob?.isActive == true) {
            Log.d(tag, "refreshSnapshot() già in corso, salto esecuzione concorrente")
            return
        }
        refreshJob = viewModelScope.launch {
            _isRefreshing.value = true
            try {
                if (resetProbe) {
                    repository.resetProbe()
                }
                checkShizukuStatus()
                val snapshot = repository.getBatterySnapshot()
                _currentSnapshot.value = snapshot
            } catch (e: Throwable) {
                Log.e(tag, "Errore durante refreshSnapshot", e)
            } finally {
                _isRefreshing.value = false
            }
            BatteryWidgetProvider.updateAllWidgets(getApplication())
            BatterySohWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun saveCurrentMeasurement() {
        viewModelScope.launch {
            refreshJob?.cancelAndJoin()
            _isRefreshing.value = true
            val snapshot = repository.getBatterySnapshot()
            _currentSnapshot.value = snapshot

            val record = BatteryData(
                timestamp = System.currentTimeMillis(),
                cycleCount = snapshot.cycleCount,
                healthPercentage = snapshot.healthPercentage,
                currentCapacityMah = snapshot.currentCapacityMah,
                source = snapshot.source
            )
            database.batteryDao().insert(record)

            // Mostra la notifica di sistema per il campionamento manuale
            NotificationHelper.showSamplingNotification(getApplication(), isManual = true, snapshot)

            // Aggiorna i widget della home screen
            BatteryWidgetProvider.updateAllWidgets(getApplication())
            BatterySohWidgetProvider.updateAllWidgets(getApplication())

            _isRefreshing.value = false
        }
    }

    fun moveToTrash(id: Int) {
        viewModelScope.launch {
            database.batteryDao().moveToTrash(id)
        }
    }

    fun moveMultipleToTrash(ids: List<Int>) {
        viewModelScope.launch {
            database.batteryDao().moveMultipleToTrash(ids)
        }
    }

    fun restoreFromTrash(id: Int) {
        viewModelScope.launch {
            database.batteryDao().restoreFromTrash(id)
        }
    }

    fun restoreMultipleFromTrash(ids: List<Int>) {
        viewModelScope.launch {
            database.batteryDao().restoreMultipleFromTrash(ids)
        }
    }

    fun deletePermanently(id: Int) {
        viewModelScope.launch {
            database.batteryDao().deletePermanently(id)
        }
    }

    fun deleteMultiplePermanently(ids: List<Int>) {
        viewModelScope.launch {
            database.batteryDao().deleteMultiplePermanently(ids)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            database.batteryDao().emptyTrash()
        }
    }

    fun deleteAllRecords() {
        viewModelScope.launch {
            database.batteryDao().deleteAll()
        }
    }

    fun setCustomRatedCapacity(mah: Double, label: String) {
        preferences.setCustomRatedCapacity(mah, label)
        refreshSnapshot()
    }

    fun resetToAutoDetection() {
        preferences.resetToAutoDetection()
        refreshSnapshot()
    }

    suspend fun exportCsvToUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val records = database.batteryDao().getAllActiveBatteryDataSync()
            val context = getApplication<Application>()
            val contentResolver = context.contentResolver

            contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                // Header CSV
                writer.write("ID,Timestamp,Data_Ora,Salute_Percentuale,Cicli_Carica,Capacita_Residua_mAh,Sorgente\n")
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                records.forEach { record ->
                    val dateFormatted = dateFormat.format(Date(record.timestamp))
                    val healthStr = record.healthPercentage?.toString() ?: "N/D"
                    val cyclesStr = record.cycleCount?.toString() ?: "N/D"
                    val capStr = record.currentCapacityMah?.let { String.format(Locale.US, "%.1f", it) } ?: "N/D"

                    writer.write("${record.id},${record.timestamp},\"$dateFormatted\",$healthStr,$cyclesStr,$capStr,\"${record.source}\"\n")
                }
            }
            true
        } catch (e: Exception) {
            Log.e(tag, "Errore durante l'esportazione del file CSV", e)
            false
        }
    }

    data class CsvImportResult(
        val totalRead: Int = 0,
        val importedCount: Int = 0,
        val duplicateCount: Int = 0,
        val success: Boolean = true,
        val errorMessage: String? = null
    )

    suspend fun importCsvFromUri(uri: Uri): CsvImportResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val context = getApplication<Application>()
            val contentResolver = context.contentResolver
            val lines = contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                reader.lineSequence().toList()
            } ?: return@withContext CsvImportResult(
                success = false,
                errorMessage = "Impossibile aprire il file selezionato"
            )

            val parseResult = BatteryTelemetryParser.parseBatteryCsv(lines)
            if (parseResult.records.isEmpty()) {
                return@withContext CsvImportResult(
                    totalRead = 0,
                    importedCount = 0,
                    duplicateCount = parseResult.duplicateInFileCount,
                    success = false,
                    errorMessage = null
                )
            }

            val existingTimestamps = database.batteryDao().getAllTimestamps().toSet()
            val recordsToInsert = parseResult.records.filter { !existingTimestamps.contains(it.timestamp) }
            val duplicateCount = (parseResult.records.size - recordsToInsert.size) + parseResult.duplicateInFileCount

            if (recordsToInsert.isNotEmpty()) {
                database.batteryDao().insertAll(recordsToInsert)
                refreshSnapshot()
            }

            CsvImportResult(
                totalRead = parseResult.records.size + parseResult.duplicateInFileCount,
                importedCount = recordsToInsert.size,
                duplicateCount = duplicateCount,
                success = true
            )
        } catch (e: Exception) {
            Log.e(tag, "Errore durante l'importazione del file CSV", e)
            CsvImportResult(
                success = false,
                errorMessage = e.localizedMessage ?: "Errore imprevisto"
            )
        }
    }

    data class BatteryProjection(
        val remainingCycles: Int,
        val totalCyclesAt80: Int
    )

    fun getBatteryProjection(): BatteryProjection? {
        val snap = currentSnapshot.value ?: return null
        val currentHealth = snap.healthPercentage ?: return null
        val currentCycles = snap.cycleCount ?: return null
        if (currentHealth <= 80) return BatteryProjection(0, currentCycles)
        val healthLost = (100 - currentHealth).coerceAtLeast(1)
        if (currentCycles <= 0) return null
        val cyclesPerPercentLoss = currentCycles.toDouble() / healthLost.toDouble()
        val healthRemainingTo80 = currentHealth - 80
        val remaining = (healthRemainingTo80 * cyclesPerPercentLoss).toInt().coerceIn(50, 8000)
        val total = currentCycles + remaining
        return BatteryProjection(remaining, total)
    }

    fun checkForUpdates(currentVersion: String) {
        if (_isCheckingUpdate.value) return
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val result = UpdateChecker.checkLatestRelease(currentVersion)
            _updateCheckResult.value = result
            _isCheckingUpdate.value = false
        }
    }

    fun dismissUpdateResult() {
        _updateCheckResult.value = null
    }

    companion object {
        const val SHIZUKU_PERMISSION_CODE = 4201
    }
}
