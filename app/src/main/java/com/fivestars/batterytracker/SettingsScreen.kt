package com.fivestars.batterytracker

import android.app.LocaleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BatteryViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val configState by viewModel.batteryConfigState.collectAsState()
    val currentSnapshot by viewModel.currentSnapshot.collectAsState()
    val isShizukuAvailable by viewModel.isShizukuAvailable.collectAsState()
    val isShizukuGranted by viewModel.isShizukuPermissionGranted.collectAsState()
    val currentLang by viewModel.appLanguage.collectAsState()
    val currentTheme by viewModel.appThemeMode.collectAsState()
    val samplingConfig by viewModel.samplingConfig.collectAsState()
    val updateResult by viewModel.updateCheckResult.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showIntervalDialog by remember { mutableStateOf(false) }
    var showTimeDialog by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<BatteryViewModel.CsvImportResult?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val success = viewModel.exportCsvToUri(uri)
                snackbarHostState.showSnackbar(
                    if (success) context.getString(R.string.csv_export_success)
                    else context.getString(R.string.csv_export_error)
                )
            }
        }
    }

    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val result = viewModel.importCsvFromUri(uri)
                importResult = result
            }
        }
    }

    val appVersionName = remember(context) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.9"
        } catch (_: Exception) {
            "1.9"
        }
    }

    BackHandler(onBack = onNavigateBack)

    updateResult?.let { result ->
        when (result) {
            is UpdateCheckResult.UpdateAvailable -> {
                UpdateAvailableDialog(
                    result = result,
                    onDismiss = { viewModel.dismissUpdateResult() },
                    onDownload = { url ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("SettingsScreen", "Cannot open update URL", e)
                        }
                        viewModel.dismissUpdateResult()
                    }
                )
            }
            is UpdateCheckResult.UpToDate -> {
                AppAlertDialog(
                    onDismissRequest = { viewModel.dismissUpdateResult() },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.update_dialog_uptodate_title),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(stringResource(R.string.update_dialog_uptodate_desc, result.currentVersion))
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.dismissUpdateResult() }) {
                            Text("OK")
                        }
                    }
                )
            }
            is UpdateCheckResult.Error -> {
                AppAlertDialog(
                    onDismissRequest = { viewModel.dismissUpdateResult() },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.update_dialog_error_title),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(stringResource(R.string.update_dialog_error_desc))
                            if (result.message.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = result.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.dismissUpdateResult() }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }

    if (showThemeDialog) {
        ThemeDialog(
            currentTheme = currentTheme,
            onDismiss = { showThemeDialog = false },
            onSelectTheme = { newTheme ->
                viewModel.setAppThemeMode(newTheme)
                showThemeDialog = false
            }
        )
    }

    if (showLanguageDialog) {
        LanguageDialog(
            currentLang = currentLang,
            onDismiss = { showLanguageDialog = false },
            onSelectLang = { newLang ->
                viewModel.setAppLanguage(newLang)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val localeManager = context.getSystemService(LocaleManager::class.java)
                    if (newLang == "system") {
                        localeManager.applicationLocales = LocaleList.getEmptyLocaleList()
                    } else {
                        localeManager.applicationLocales = LocaleList.forLanguageTags(newLang)
                    }
                }
                showLanguageDialog = false
            }
        )
    }

    if (showEditDialog) {
        val activeMah = configState.customRatedCapacityMah ?: currentSnapshot?.designCapacityMah ?: 5840.0
        EditCapacityDialog(
            initialMah = activeMah,
            initialPresetLabel = configState.presetLabel,
            onDismiss = { showEditDialog = false },
            onSave = { newMah, label ->
                viewModel.setCustomRatedCapacity(newMah, label)
                showEditDialog = false
            },
            onResetAuto = {
                viewModel.resetToAutoDetection()
                showEditDialog = false
            }
        )
    }

    if (showIntervalDialog) {
        SamplingIntervalDialog(
            currentIntervalHours = samplingConfig.intervalHours,
            onDismiss = { showIntervalDialog = false },
            onSelectInterval = { hours ->
                viewModel.setSamplingIntervalHours(hours)
                showIntervalDialog = false
            }
        )
    }

    if (showTimeDialog) {
        SamplingTimePickerDialog(
            initialHour = samplingConfig.targetHour,
            initialMinute = samplingConfig.targetMinute,
            is24Hour = android.text.format.DateFormat.is24HourFormat(context),
            onDismiss = { showTimeDialog = false },
            onConfirm = { hour, minute ->
                viewModel.setSamplingTime(hour, minute)
                showTimeDialog = false
            }
        )
    }

    importResult?.let { result ->
        AppAlertDialog(
            onDismissRequest = { importResult = null },
            icon = {
                Icon(
                    imageVector = if (result.success && result.importedCount > 0) Icons.Default.CheckCircle
                    else if (result.success) Icons.Default.Info
                    else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (result.success) stringResource(R.string.csv_import_success_title)
                    else stringResource(R.string.csv_import_error_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    if (result.success) {
                        if (result.importedCount > 0) {
                            Text(stringResource(R.string.csv_import_success_msg, result.importedCount))
                            if (result.duplicateCount > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.csv_import_duplicates_msg, result.duplicateCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(stringResource(R.string.csv_import_all_duplicates, result.duplicateCount))
                        }
                    } else {
                        Text(
                            text = result.errorMessage ?: stringResource(R.string.csv_import_error_msg),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { importResult = null }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                title = { Text(stringResource(R.string.title_settings), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_section_battery_config),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Voce di impostazione principale: Capacità Nominale di Progetto
                val activeCapacity = configState.customRatedCapacityMah ?: currentSnapshot?.designCapacityMah ?: 5840.0
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEditDialog = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_rated_capacity_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.0f", activeCapacity)} mAh • ${configState.presetLabel}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (configState.isAutoDetection) stringResource(R.string.settings_mode_auto)
                                else stringResource(R.string.settings_mode_custom),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (configState.isAutoDetection) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.tertiary
                            )
                        }

                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Sezione Campionamento Automatico
                Text(
                    text = stringResource(R.string.settings_section_sampling),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                val samplingIntervalLabel = when (samplingConfig.intervalHours) {
                    48L -> stringResource(R.string.sampling_interval_48h_short)
                    168L -> stringResource(R.string.sampling_interval_168h_short)
                    else -> stringResource(R.string.sampling_interval_24h_short)
                }
                val formattedSamplingTime = String.format(Locale.getDefault(), "%02d:%02d", samplingConfig.targetHour, samplingConfig.targetMinute)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Switch principale: Abilita / Disabilita
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setSamplingEnabled(!samplingConfig.isEnabled) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_sampling_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (samplingConfig.isEnabled) {
                                        stringResource(
                                            R.string.settings_sampling_desc_active,
                                            samplingIntervalLabel,
                                            formattedSamplingTime
                                        )
                                    } else {
                                        stringResource(R.string.settings_sampling_desc_disabled)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = samplingConfig.isEnabled,
                                onCheckedChange = { viewModel.setSamplingEnabled(it) }
                            )
                        }

                        if (samplingConfig.isEnabled) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            // Selettore Intervallo
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showIntervalDialog = true }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.settings_sampling_interval_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = when (samplingConfig.intervalHours) {
                                            48L -> stringResource(R.string.sampling_interval_48h)
                                            168L -> stringResource(R.string.sampling_interval_168h)
                                            else -> stringResource(R.string.sampling_interval_24h)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(onClick = { showIntervalDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            // Selettore Orario Programmato
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTimeDialog = true }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.settings_sampling_time_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formattedSamplingTime,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.settings_sampling_time_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(onClick = { showTimeDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Sezione Selezione Lingua
                Text(
                    text = stringResource(R.string.settings_section_language),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                val currentLangDisplayName = when (currentLang) {
                    "it" -> stringResource(R.string.lang_it)
                    "en" -> stringResource(R.string.lang_en)
                    "es" -> stringResource(R.string.lang_es)
                    "fr" -> stringResource(R.string.lang_fr)
                    "de" -> stringResource(R.string.lang_de)
                    else -> stringResource(R.string.lang_system_default)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLanguageDialog = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_language_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentLangDisplayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { showLanguageDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Sezione Selezione Tema
                Text(
                    text = stringResource(R.string.settings_section_theme),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                val currentThemeDisplayName = when (currentTheme) {
                    AppThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                    AppThemeMode.LIGHT -> stringResource(R.string.theme_light)
                    AppThemeMode.DARK -> stringResource(R.string.theme_dark)
                    AppThemeMode.AMOLED -> stringResource(R.string.theme_amoled)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showThemeDialog = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DarkMode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_theme_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentThemeDisplayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { showThemeDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Sezione Dati & Backup
                Text(
                    text = stringResource(R.string.settings_section_backup),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val defaultFileName = "battery_history_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.csv"
                            exportCsvLauncher.launch(defaultFileName)
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_backup_export_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.settings_backup_export_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = {
                            val defaultFileName = "battery_history_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.csv"
                            exportCsvLauncher.launch(defaultFileName)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            importCsvLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "application/csv", "text/plain", "*/*"))
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Upload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_backup_import_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.settings_backup_import_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = {
                            importCsvLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "application/csv", "text/plain", "*/*"))
                        }) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Sezione Aggiornamenti
                Text(
                    text = stringResource(R.string.settings_section_updates),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isCheckingUpdate) {
                            viewModel.checkForUpdates(appVersionName)
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_check_updates_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.settings_check_updates_desc, appVersionName),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            IconButton(onClick = { viewModel.checkForUpdates(appVersionName) }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.settings_check_updates_title),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.settings_section_hardware_info),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InfoRow(
                            label = stringResource(R.string.settings_detected_device),
                            value = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} (${Build.DEVICE})"
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                        InfoRow(
                            label = stringResource(R.string.settings_bms_driver),
                            value = "/sys/class/oplus_chg/battery/ (SuperVOOC)"
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                        InfoRow(
                            label = stringResource(R.string.settings_shizuku_status),
                            value = if (isShizukuGranted) stringResource(R.string.settings_shizuku_connected)
                            else if (isShizukuAvailable) stringResource(R.string.settings_shizuku_waiting)
                            else stringResource(R.string.settings_shizuku_not_running)
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                        InfoRow(
                            label = stringResource(R.string.settings_app_version),
                            value = "$appVersionName - Oplus Edition (Oppo, OnePlus, Realme)"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeDialog(
    currentTheme: AppThemeMode,
    onDismiss: () -> Unit,
    onSelectTheme: (AppThemeMode) -> Unit
) {
    val options = listOf(
        Triple(AppThemeMode.SYSTEM, stringResource(R.string.theme_system), stringResource(R.string.theme_system_desc)),
        Triple(AppThemeMode.LIGHT, stringResource(R.string.theme_light), stringResource(R.string.theme_light_desc)),
        Triple(AppThemeMode.DARK, stringResource(R.string.theme_dark), stringResource(R.string.theme_dark_desc)),
        Triple(AppThemeMode.AMOLED, stringResource(R.string.theme_amoled), stringResource(R.string.theme_amoled_desc))
    )

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_theme_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                options.forEach { (mode, title, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTheme(mode) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == mode,
                            onClick = { onSelectTheme(mode) }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (currentTheme == mode) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun LanguageDialog(
    currentLang: String,
    onDismiss: () -> Unit,
    onSelectLang: (String) -> Unit
) {
    val options = listOf(
        "system" to stringResource(R.string.lang_system_default),
        "it" to stringResource(R.string.lang_it),
        "en" to stringResource(R.string.lang_en),
        "es" to stringResource(R.string.lang_es),
        "fr" to stringResource(R.string.lang_fr),
        "de" to stringResource(R.string.lang_de)
    )

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_section_language), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                options.forEach { (code, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectLang(code) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLang == code,
                            onClick = { onSelectLang(code) }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (currentLang == code) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EditCapacityDialog(
    initialMah: Double,
    initialPresetLabel: String,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit,
    onResetAuto: () -> Unit
) {
    var textValue by remember { mutableStateOf(String.format(Locale.US, "%.0f", initialMah)) }
    var selectedLabel by remember { mutableStateOf(initialPresetLabel) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedBrandFilter by remember { mutableStateOf("Tutti") }

    val filteredPresets = remember(searchQuery, selectedBrandFilter) {
        DevicePresets.ALL_PRESETS.filter { preset ->
            val matchesBrand = when (selectedBrandFilter) {
                "Oppo" -> preset.brand.equals("Oppo", ignoreCase = true)
                "OnePlus" -> preset.brand.equals("OnePlus", ignoreCase = true)
                "Realme" -> preset.brand.equals("Realme", ignoreCase = true)
                "Samsung" -> preset.brand.equals("Samsung", ignoreCase = true)
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                    preset.displayName.contains(searchQuery, ignoreCase = true) ||
                    preset.codeNames.any { it.contains(searchQuery, ignoreCase = true) }

            matchesBrand && matchesQuery
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        EnforceDialogHighRefreshRate()
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Intestazione
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dialog_capacity_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                    }
                }

                Text(
                    text = stringResource(R.string.dialog_capacity_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Campo Input Manuale
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            textValue = input
                            if (input.isNotBlank()) {
                                val match = DevicePresets.ALL_PRESETS.firstOrNull { it.ratedMah.toInt().toString() == input }
                                selectedLabel = match?.displayName ?: "Custom ($input mAh)"
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.dialog_capacity_manual_label)) },
                    trailingIcon = { Text("mAh", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Barra ricerca e filtri brand
                Text(
                    text = stringResource(R.string.dialog_capacity_popular_presets),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.dialog_capacity_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Chip selezione brand
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val brandOptions = listOf(
                        "Tutti" to stringResource(R.string.brand_all),
                        "Oppo" to stringResource(R.string.brand_oppo),
                        "OnePlus" to stringResource(R.string.brand_oneplus),
                        "Realme" to stringResource(R.string.brand_realme),
                        "Samsung" to stringResource(R.string.brand_samsung)
                    )
                    brandOptions.forEach { (brandKey, brandLabel) ->
                        FilterChip(
                            selected = selectedBrandFilter == brandKey,
                            onClick = { selectedBrandFilter = brandKey },
                            label = { Text(brandLabel, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Lista modelli ordinata alfabeticamente
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    if (filteredPresets.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.not_available), style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(6.dp)
                        ) {
                            items(filteredPresets, key = { it.displayName }) { preset ->
                                val isSelected = textValue == preset.ratedMah.toInt().toString()

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clickable {
                                            textValue = preset.ratedMah.toInt().toString()
                                            selectedLabel = preset.displayName
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Smartphone,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(22.dp)
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = preset.displayName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                            Text(
                                                text = "${preset.ratedMah.toInt()} mAh (${preset.typicalMah} mAh)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pulsanti inferiori
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onResetAuto,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_auto_detect), maxLines = 1)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Text(stringResource(R.string.cancel), maxLines = 1)
                        }

                        Button(
                            onClick = {
                                val mah = textValue.toDoubleOrNull()
                                if (mah != null && mah > 0) {
                                    onSave(mah, selectedLabel)
                                }
                            },
                            enabled = textValue.toDoubleOrNull()?.let { it > 0 } == true,
                            contentPadding = PaddingValues(horizontal = 18.dp)
                        ) {
                            Text(stringResource(R.string.save), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateAvailableDialog(
    result: UpdateCheckResult.UpdateAvailable,
    onDismiss: () -> Unit,
    onDownload: (String) -> Unit
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.update_dialog_available_title, result.release.versionName),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(
                        R.string.update_dialog_current_vs_latest,
                        result.currentVersion,
                        result.release.versionName
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (result.release.changelog.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.update_dialog_changelog_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = result.release.changelog,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val downloadTarget = result.release.apkDownloadUrl ?: result.release.releaseUrl
                    onDownload(downloadTarget)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.update_dialog_btn_download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun SamplingIntervalDialog(
    currentIntervalHours: Long,
    onDismiss: () -> Unit,
    onSelectInterval: (Long) -> Unit
) {
    val options = listOf(
        Pair(24L, stringResource(R.string.sampling_interval_24h)),
        Pair(48L, stringResource(R.string.sampling_interval_48h)),
        Pair(168L, stringResource(R.string.sampling_interval_168h))
    )

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_sampling_interval_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                options.forEach { (hours, label) ->
                    val isSelected = currentIntervalHours == hours
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectInterval(hours) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectInterval(hours) }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SamplingTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = is24Hour
    )

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_sampling_time_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

