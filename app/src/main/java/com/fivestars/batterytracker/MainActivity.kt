package com.fivestars.batterytracker

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val historyDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
private val chartDateFormatter = SimpleDateFormat("dd/MM", Locale.getDefault())

enum class AppScreen {
    DASHBOARD,
    TRASH,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: BatteryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Forza la frequenza di aggiornamento a 120 Hz (o massimo supportato dal display) per garantire massima fluidità
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val supportedModes = display?.supportedModes ?: emptyArray()
            val maxMode = supportedModes.maxByOrNull { it.refreshRate }
            if (maxMode != null) {
                val params = window.attributes
                params.preferredDisplayModeId = maxMode.modeId
                window.attributes = params
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val params = window.attributes
            params.preferredRefreshRate = 120f
            window.attributes = params
        }

        setContent {
            val themeMode by viewModel.appThemeMode.collectAsState()
            BatteryTrackerTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BatteryTrackerMain(viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSnapshot()
    }
}

@Composable
fun BatteryTrackerMain(viewModel: BatteryViewModel) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }

    // Launcher per permesso notifiche (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val isShizukuAvailable by viewModel.isShizukuAvailable.collectAsState()
    val isShizukuGranted by viewModel.isShizukuPermissionGranted.collectAsState()

    LaunchedEffect(isShizukuAvailable, isShizukuGranted) {
        if (isShizukuAvailable && !isShizukuGranted) {
            viewModel.requestShizukuPermission()
        }
    }

    when (currentScreen) {
        AppScreen.DASHBOARD -> {
            BatteryTrackerDashboardScreen(
                viewModel = viewModel,
                onNavigateToTrash = { currentScreen = AppScreen.TRASH },
                onNavigateToSettings = { currentScreen = AppScreen.SETTINGS }
            )
        }
        AppScreen.TRASH -> {
            TrashScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = AppScreen.DASHBOARD }
            )
        }
        AppScreen.SETTINGS -> {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = AppScreen.DASHBOARD }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryTrackerDashboardScreen(
    viewModel: BatteryViewModel,
    onNavigateToTrash: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val history by viewModel.allBatteryData.collectAsState()
    val trashedItems by viewModel.trashedBatteryData.collectAsState()
    val currentSnapshot by viewModel.currentSnapshot.collectAsState()
    val isShizukuAvailable by viewModel.isShizukuAvailable.collectAsState()
    val isShizukuGranted by viewModel.isShizukuPermissionGranted.collectAsState()
    val nextScheduleTime by viewModel.nextWorkScheduleTime.collectAsState(initial = null)

    // Stato selezione multipla per eliminazione nello storico
    var selectedRecordIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Se l'utente preme il tasto indietro mentre ci sono elementi selezionati, annulla la selezione
    BackHandler(enabled = selectedRecordIds.isNotEmpty()) {
        selectedRecordIds = emptySet()
    }

    // Launcher per esportazione CSV via Storage Access Framework
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

    // Dialog per la spiegazione della salute della batteria (SOH)
    var showHealthInfoDialog by remember { mutableStateOf(false) }

    if (showHealthInfoDialog) {
        val isBm = currentSnapshot?.isShizukuUsed != true
        AlertDialog(
            onDismissRequest = { showHealthInfoDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isBm) Icons.Default.Warning else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isBm) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.dialog_health_info_title), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Column {
                        if (isBm) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.dialog_health_bm_warning_title),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = stringResource(R.string.dialog_health_bm_warning_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = stringResource(R.string.dialog_health_shizuku_desc),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHealthInfoDialog = false }) {
                    Text(stringResource(R.string.dialog_rm_info_close), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Dialog per la spiegazione delle oscillazioni di capacità residua (mAh / FCC)
    var showCapacityFluctuationDialog by remember { mutableStateOf(false) }

    if (showCapacityFluctuationDialog) {
        val isBm = currentSnapshot?.isShizukuUsed != true
        AlertDialog(
            onDismissRequest = { showCapacityFluctuationDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isBm) Icons.Default.Warning else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isBm) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.dialog_capacity_fluctuation_title), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Column {
                        if (isBm) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.dialog_capacity_bm_warning_title),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = stringResource(R.string.dialog_capacity_bm_warning_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = stringResource(R.string.dialog_capacity_fluctuation_desc),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCapacityFluctuationDialog = false }) {
                    Text(stringResource(R.string.dialog_rm_info_close), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Dialog per la spiegazione dei cicli di carica
    var showCyclesInfoDialog by remember { mutableStateOf(false) }

    if (showCyclesInfoDialog) {
        val isBm = currentSnapshot?.isShizukuUsed != true
        val isCyclesWarn = (isBm && (currentSnapshot?.cycleCount == null || currentSnapshot?.cycleCount == 0)) || currentSnapshot?.cycleCount == null
        AlertDialog(
            onDismissRequest = { showCyclesInfoDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isCyclesWarn) Icons.Default.Warning else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isCyclesWarn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.dialog_cycles_info_title), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Column {
                        if (isCyclesWarn) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.dialog_cycles_bm_warning_title),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = stringResource(R.string.dialog_cycles_bm_warning_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = stringResource(R.string.dialog_cycles_desc),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCyclesInfoDialog = false }) {
                    Text(stringResource(R.string.dialog_rm_info_close), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Dialog di conferma eliminazione multipla
    val selectionCount = selectedRecordIds.size
    if (showDeleteConfirmDialog && selectionCount > 0) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
            },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    if (selectionCount == 1) stringResource(R.string.dialog_delete_single_title)
                    else stringResource(R.string.dialog_delete_multi_title, selectionCount)
                )
            },
            text = {
                Text(
                    if (selectionCount == 1)
                        stringResource(R.string.dialog_delete_single_msg)
                    else
                        stringResource(R.string.dialog_delete_multi_msg, selectionCount)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val idsToDelete = selectedRecordIds.toList()
                        viewModel.moveMultipleToTrash(idsToDelete)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                if (idsToDelete.size == 1) context.getString(R.string.snackbar_deleted_single)
                                else context.getString(R.string.snackbar_deleted_multi, idsToDelete.size)
                            )
                        }
                        showDeleteConfirmDialog = false
                        selectedRecordIds = emptySet()
                    }
                ) {
                    Text(stringResource(R.string.yes), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (selectedRecordIds.isNotEmpty()) {
                val allVisibleIds = remember(history) { history.map { it.id }.toSet() }
                val areAllSelected = allVisibleIds.isNotEmpty() && selectedRecordIds.containsAll(allVisibleIds)

                // Barra contestuale quando almeno un box è selezionato
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { selectedRecordIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel_selection))
                        }
                    },
                    title = {
                        Text(
                            text = if (selectedRecordIds.size == 1) stringResource(R.string.selection_count_single) else stringResource(R.string.selection_count_multi, selectedRecordIds.size),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                selectedRecordIds = if (areAllSelected) {
                                    emptySet()
                                } else {
                                    allVisibleIds
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (areAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = if (areAllSelected) stringResource(R.string.action_deselect_all) else stringResource(R.string.action_select_all)
                            )
                        }

                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                // Barra standard
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = stringResource(R.string.title_dashboard),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                lineHeight = 19.sp
                            )
                        }
                    },
                    actions = {
                        // Pulsante Cestino con badge contatore
                        BadgedBox(
                            badge = {
                                if (trashedItems.isNotEmpty()) {
                                    Badge { Text("${trashedItems.size}") }
                                }
                            }
                        ) {
                            IconButton(onClick = onNavigateToTrash) {
                                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.title_trash))
                            }
                        }

                        IconButton(
                            onClick = {
                                val defaultFileName = "battery_history_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.csv"
                                exportCsvLauncher.launch(defaultFileName)
                            }
                        ) {
                            Icon(Icons.Default.Download, contentDescription = stringResource(R.string.action_export_csv))
                        }

                        IconButton(onClick = { viewModel.refreshSnapshot() }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh))
                        }

                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.action_settings))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.saveCurrentMeasurement()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.snapshot_saved_snackbar))
                    }
                },
                icon = { Icon(Icons.Default.ElectricBolt, contentDescription = null) },
                text = { Text(stringResource(R.string.save_reading_fab)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item(key = "shizuku_card") {
                Spacer(modifier = Modifier.height(16.dp))
                // Banner stato Shizuku / Sorgente dati
                ShizukuStatusCard(
                    isAvailable = isShizukuAvailable,
                    isGranted = isShizukuGranted,
                    onRequestPermission = { viewModel.requestShizukuPermission() }
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Piccolo banner promemoria aggiornamento
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.refreshSnapshot() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.refresh_hint_banner),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item(key = "dashboard_cards") {
                Spacer(modifier = Modifier.height(16.dp))
                // Sezione Dashboard Principale
                Text(
                    text = stringResource(R.string.dashboard_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                DashboardCards(
                    snapshot = currentSnapshot,
                    onHealthInfoClick = { showHealthInfoDialog = true },
                    onCyclesInfoClick = { showCyclesInfoDialog = true },
                    onCapacityInfoClick = { showCapacityFluctuationDialog = true }
                )
            }

            item(key = "oplus_hardware_card") {
                Spacer(modifier = Modifier.height(12.dp))
                // Diagnostica Hardware & BMS Oplus (Punti 1, 2, 3, 4)
                OplusAdvancedHardwareCard(snapshot = currentSnapshot)
            }

            item(key = "trend_card") {
                Spacer(modifier = Modifier.height(12.dp))
                // Andamento Salute & Proiezione Cicli all'80% (Punto 4)
                BatteryHealthTrendCard(
                    history = history,
                    projection = viewModel.getBatteryProjection(),
                    currentCycles = currentSnapshot?.cycleCount
                )
            }

            item(key = "schedule_banner") {
                Spacer(modifier = Modifier.height(16.dp))
                // Info sul prossimo campionamento automatico
                NextScheduleBanner(nextScheduleTime = nextScheduleTime)
            }

            item(key = "history_header") {
                Spacer(modifier = Modifier.height(24.dp))
                // Header lista Storico
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.history_title, history.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (selectedRecordIds.isNotEmpty()) stringResource(R.string.history_tap_to_select) else stringResource(R.string.history_hold_to_select),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (history.isEmpty()) {
                item(key = "history_empty") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.history_empty_title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.history_empty_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(
                items = history,
                key = { it.id },
                contentType = { "history_record" }
            ) { record ->
                val isSelected = selectedRecordIds.contains(record.id)
                HistoryRecordCard(
                    record = record,
                    isSelected = isSelected,
                    onLongClick = {
                        selectedRecordIds = if (selectedRecordIds.contains(record.id)) {
                            selectedRecordIds - record.id
                        } else {
                            selectedRecordIds + record.id
                        }
                    },
                    onClick = {
                        if (selectedRecordIds.isNotEmpty()) {
                            selectedRecordIds = if (selectedRecordIds.contains(record.id)) {
                                selectedRecordIds - record.id
                            } else {
                                selectedRecordIds + record.id
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryRecordCard(
    record: BatteryData,
    isSelected: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val formattedDate = remember(record.timestamp) { historyDateFormat.format(Date(record.timestamp)) }
    val notAvail = stringResource(R.string.not_available)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        border = if (isSelected) BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.history_item_cycles, record.cycleCount?.toString() ?: notAvail),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.history_item_capacity, record.currentCapacityMah?.let { String.format(Locale.US, "%.0f mAh", it) } ?: notAvail),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.history_item_source, record.source),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Badge Salute %
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = record.healthPercentage?.let { "$it%" } ?: notAvail,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: BatteryViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val trashedItems by viewModel.trashedBatteryData.collectAsState()

    var showEmptyTrashConfirmDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onNavigateBack)

    if (showEmptyTrashConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirmDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.trash_empty_dialog_title)) },
            text = { Text(stringResource(R.string.trash_empty_dialog_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.emptyTrash()
                        showEmptyTrashConfirmDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.trash_snackbar_cleared))
                        }
                    }
                ) {
                    Text(stringResource(R.string.trash_empty_dialog_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
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
                title = { Text("${stringResource(R.string.title_trash)} (${trashedItems.size})", fontWeight = FontWeight.Bold) },
                actions = {
                    if (trashedItems.isNotEmpty()) {
                        IconButton(onClick = { showEmptyTrashConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = stringResource(R.string.trash_empty_btn),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
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
            if (trashedItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.trash_empty_state_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.trash_empty_state_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(trashedItems, key = { it.id }) { record ->
                    TrashedRecordCard(
                        record = record,
                        onRestore = {
                            viewModel.restoreFromTrash(record.id)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.trash_snackbar_restored))
                            }
                        },
                        onDeletePermanent = {
                            viewModel.deletePermanently(record.id)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.trash_snackbar_deleted_perm))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TrashedRecordCard(
    record: BatteryData,
    onRestore: () -> Unit,
    onDeletePermanent: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(record.timestamp))
    val notAvail = stringResource(R.string.not_available)
    val healthTitle = stringResource(R.string.card_health_title)
    val cyclesTitle = stringResource(R.string.card_cycles_title)
    val capacityTitle = stringResource(R.string.card_capacity_title)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$healthTitle: ${record.healthPercentage?.let { "$it%" } ?: notAvail} | $cyclesTitle: ${record.cycleCount?.toString() ?: notAvail}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "$capacityTitle: ${record.currentCapacityMah?.let { String.format(Locale.US, "%.0f mAh", it) } ?: notAvail} (${record.source})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = stringResource(R.string.trash_badge_in_trash),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDeletePermanent,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.trash_action_delete_perm))
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onRestore,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.trash_action_restore))
                }
            }
        }
    }
}

@Composable
fun ShizukuStatusCard(
    isAvailable: Boolean,
    isGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    if (isAvailable && isGranted) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.shizuku_connected_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = stringResource(R.string.shizuku_connected_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    } else if (isAvailable && !isGranted) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.shizuku_req_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.shizuku_req_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text(stringResource(R.string.shizuku_auth_btn), color = MaterialTheme.colorScheme.onTertiary)
                }
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.shizuku_fallback_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.shizuku_fallback_desc),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardCards(
    snapshot: BatterySnapshot?,
    onHealthInfoClick: () -> Unit = {},
    onCyclesInfoClick: () -> Unit = {},
    onCapacityInfoClick: () -> Unit = {}
) {
    val notAvail = stringResource(R.string.not_available)
    val isBatteryManager = snapshot?.isShizukuUsed != true
    val isHealthInaccurate = isBatteryManager || snapshot?.healthPercentage == null
    val isCyclesInaccurate = (snapshot?.cycleCount == null) || (isBatteryManager && snapshot.cycleCount == 0)

    val healthText = snapshot?.healthPercentage?.let { "$it%" } ?: notAvail
    val cyclesText = if (isCyclesInaccurate) notAvail else (snapshot?.cycleCount?.let { "$it" } ?: notAvail)
    val capacityText = snapshot?.currentCapacityMah?.let { String.format(Locale.US, "%.0f mAh", it) } ?: notAvail
    val levelText = snapshot?.batteryLevelPercentage?.let { "$it%" } ?: notAvail
    val sourceText = snapshot?.source ?: notAvail

    val capacitySubtitle = if (isBatteryManager) {
        snapshot?.designCapacityMah?.let {
            stringResource(R.string.card_capacity_sub_bm_with_design, it.toInt())
        } ?: stringResource(R.string.card_capacity_sub_bm)
    } else {
        snapshot?.designCapacityMah?.let {
            stringResource(R.string.card_capacity_sub_design, it.toInt())
        } ?: stringResource(R.string.card_capacity_sub_fcc)
    }

    val healthSubtitle = if (isHealthInaccurate) {
        stringResource(R.string.card_health_sub_bm)
    } else {
        stringResource(R.string.card_health_sub)
    }

    val cyclesSubtitle = if (isCyclesInaccurate) {
        stringResource(R.string.card_cycles_sub_bm)
    } else {
        stringResource(R.string.card_cycles_sub)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Riga 1: Salute % e Cicli
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = stringResource(R.string.card_health_title),
                value = healthText,
                subtitle = healthSubtitle,
                icon = Icons.Default.BatteryChargingFull,
                modifier = Modifier.weight(1f),
                isHighlighted = !isHealthInaccurate,
                showInfoIcon = true,
                showWarningIcon = isHealthInaccurate,
                onClick = onHealthInfoClick
            )
            MetricCard(
                title = stringResource(R.string.card_cycles_title),
                value = cyclesText,
                subtitle = cyclesSubtitle,
                icon = Icons.Default.Autorenew,
                modifier = Modifier.weight(1f),
                showInfoIcon = true,
                showWarningIcon = isCyclesInaccurate,
                onClick = onCyclesInfoClick
            )
        }

        // Riga 2: Capacità residua e Livello attuale
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = stringResource(R.string.card_capacity_title),
                value = capacityText,
                subtitle = capacitySubtitle,
                icon = Icons.Default.Speed,
                modifier = Modifier.weight(1f),
                showInfoIcon = true,
                showWarningIcon = isBatteryManager,
                onClick = onCapacityInfoClick
            )
            MetricCard(
                title = stringResource(R.string.card_level_title),
                value = levelText,
                subtitle = stringResource(R.string.card_level_sub),
                icon = Icons.Default.ElectricBolt,
                modifier = Modifier.weight(1f)
            )
        }

        // Chip sorgente
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.data_source_prefix),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = sourceText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (snapshot?.isShizukuUsed == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    showInfoIcon: Boolean = false,
    showWarningIcon: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (showWarningIcon) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (showInfoIcon) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = if (showWarningIcon) MaterialTheme.colorScheme.error else if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NextScheduleBanner(nextScheduleTime: Long?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            val text = if (nextScheduleTime != null && nextScheduleTime > System.currentTimeMillis()) {
                val formatted = historyDateFormat.format(Date(nextScheduleTime))
                stringResource(R.string.schedule_next, formatted)
            } else {
                stringResource(R.string.schedule_periodic_24h)
            }
            Text(text = text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

enum class DiagnosticInfoType {
    VOLTAGE,
    QMAX,
    RM,
    CUTOFF,
    RESISTANCE,
    CELL_BALANCE,
    BMS_SYNC,
    SATURATION,
    TEMP_COMPENSATION,
    SAFETY_FLAGS
}

@Composable
fun OplusAdvancedHardwareCard(snapshot: BatterySnapshot?) {
    val notAvail = stringResource(R.string.not_available)
    var activeInfoDialog by remember { mutableStateOf<DiagnosticInfoType?>(null) }

    if (activeInfoDialog != null) {
        val (titleRes, descRes) = when (activeInfoDialog) {
            DiagnosticInfoType.VOLTAGE -> Pair(R.string.dialog_voltage_info_title, R.string.dialog_voltage_info_desc)
            DiagnosticInfoType.QMAX -> Pair(R.string.dialog_qmax_info_title, R.string.dialog_qmax_info_desc)
            DiagnosticInfoType.RM -> Pair(R.string.dialog_rm_info_title, R.string.dialog_rm_info_desc)
            DiagnosticInfoType.CUTOFF -> Pair(R.string.dialog_cutoff_info_title, R.string.dialog_cutoff_info_desc)
            DiagnosticInfoType.RESISTANCE -> Pair(R.string.dialog_esr_info_title, R.string.dialog_esr_info_desc)
            DiagnosticInfoType.CELL_BALANCE -> Pair(R.string.dialog_cell_bal_info_title, R.string.dialog_cell_bal_info_desc)
            DiagnosticInfoType.BMS_SYNC -> Pair(R.string.dialog_bms_sync_title, R.string.dialog_bms_sync_desc)
            DiagnosticInfoType.SATURATION -> Pair(R.string.dialog_saturation_info_title, R.string.dialog_saturation_info_desc)
            DiagnosticInfoType.TEMP_COMPENSATION -> Pair(R.string.dialog_temp_comp_info_title, R.string.dialog_temp_comp_info_desc)
            DiagnosticInfoType.SAFETY_FLAGS -> Pair(R.string.dialog_safety_info_title, R.string.dialog_safety_info_desc)
            null -> Pair(0, 0)
        }

        AlertDialog(
            onDismissRequest = { activeInfoDialog = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(titleRes), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(descRes),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { activeInfoDialog = null }) {
                    Text(stringResource(R.string.dialog_rm_info_close), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header con Icona e Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.diag_hardware_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (snapshot?.isAuthentic == true) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = stringResource(R.string.diag_badge_oppo_auth),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    val dualText = when (snapshot?.isDualBattery) {
                        true -> stringResource(R.string.diag_badge_2_cells)
                        false -> stringResource(R.string.diag_badge_1_cell)
                        else -> null
                    }
                    if (dualText != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = dualText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Barra Potenza Istantanea & Temperatura (Punto 2)
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if ((snapshot?.chargingPowerWatts ?: 0.0) > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = snapshot?.chargingPowerWatts?.let {
                                if (it > 0) "+$it W" else "$it W"
                            } ?: notAvail,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val protocolLabel = when (snapshot?.chargingProtocol) {
                            "In Scarica", "DISCHARGING" -> stringResource(R.string.diag_discharging)
                            "Carica Standard", "STANDARD" -> stringResource(R.string.diag_standard_charging)
                            "Standby", "STANDBY" -> stringResource(R.string.diag_standby)
                            else -> snapshot?.chargingProtocol ?: stringResource(R.string.diag_standby)
                        }
                        Text(
                            text = "($protocolLabel)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Thermostat,
                            contentDescription = null,
                            tint = if ((snapshot?.batteryTemperatureCelsius ?: 0.0) >= 42.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = snapshot?.batteryTemperatureCelsius?.let { String.format(Locale.US, "%.1f°C", it) } ?: notAvail,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if ((snapshot?.batteryTemperatureCelsius ?: 0.0) >= 42.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Griglia 2x2 Compatta delle Tessere Diagnostiche (tutte cliccabili per dettagli)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tessera 1: Tensione Celle
                DiagnosticTile(
                    label = stringResource(R.string.diag_tile_circuit_volt),
                    value = if (snapshot?.isDualBattery == true && snapshot.cell0VoltageMv != null && snapshot.cell1VoltageMv != null) {
                        "${snapshot.cell0VoltageMv} / ${snapshot.cell1VoltageMv} mV"
                    } else if (snapshot?.cell0VoltageMv != null) {
                        "${snapshot.cell0VoltageMv} mV"
                    } else notAvail,
                    subtitle = if (snapshot?.isDualBattery == true && snapshot.cell0VoltageMv != null && snapshot.cell1VoltageMv != null) {
                        stringResource(R.string.diag_tile_circuit_volt_sub_dual, kotlin.math.abs(snapshot.cell0VoltageMv - snapshot.cell1VoltageMv))
                    } else stringResource(R.string.diag_tile_circuit_volt_sub_single),
                    modifier = Modifier.weight(1f),
                    showInfoIcon = true,
                    onClick = { activeInfoDialog = DiagnosticInfoType.VOLTAGE }
                )

                // Tessera 2: Capacità Chimica Qmax
                DiagnosticTile(
                    label = stringResource(R.string.diag_tile_qmax),
                    value = snapshot?.qMaxMah?.let { "$it mAh" } ?: notAvail,
                    subtitle = stringResource(R.string.diag_tile_qmax_sub),
                    modifier = Modifier.weight(1f),
                    showInfoIcon = true,
                    onClick = { activeInfoDialog = DiagnosticInfoType.QMAX }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tessera 3: Carica Residua RM
                DiagnosticTile(
                    label = stringResource(R.string.diag_tile_rm),
                    value = snapshot?.remainingCapacityMah?.let { "${it.toInt()} mAh" } ?: notAvail,
                    subtitle = stringResource(R.string.diag_tile_rm_sub),
                    modifier = Modifier.weight(1f),
                    showInfoIcon = true,
                    onClick = { activeInfoDialog = DiagnosticInfoType.RM }
                )

                // Tessera 4: Soglia Spegnimento Vbat UV
                DiagnosticTile(
                    label = stringResource(R.string.diag_tile_cutoff),
                    value = snapshot?.vbatUvMv?.let { "$it mV" } ?: notAvail,
                    subtitle = stringResource(R.string.diag_tile_cutoff_sub),
                    modifier = Modifier.weight(1f),
                    showInfoIcon = true,
                    onClick = { activeInfoDialog = DiagnosticInfoType.CUTOFF }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Riga 3: Resistenza Interna (ESR) & Bilanciamento Celle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tessera 5: Resistenza Interna ESR
                val esrSubtitle = when {
                    snapshot?.internalResistanceMohm == null -> stringResource(R.string.diag_tile_esr_sub)
                    snapshot.chargingProtocol in listOf("SuperVOOC", "USB-PD / PPS", "STANDARD", "Carica Standard") -> {
                        stringResource(R.string.diag_tile_esr_sub_charging)
                    }
                    snapshot.chargingPowerWatts != null && Math.abs(snapshot.chargingPowerWatts) > 2.2 -> {
                        stringResource(R.string.diag_tile_esr_sub_heavy)
                    }
                    snapshot.chargingPowerWatts != null -> {
                        stringResource(R.string.diag_tile_esr_sub_light)
                    }
                    else -> stringResource(R.string.diag_tile_esr_sub)
                }

                DiagnosticTile(
                    label = stringResource(R.string.diag_tile_esr),
                    value = snapshot?.internalResistanceMohm?.let { "$it mΩ" } ?: notAvail,
                    subtitle = esrSubtitle,
                    modifier = Modifier.weight(1f),
                    showInfoIcon = true,
                    onClick = { activeInfoDialog = DiagnosticInfoType.RESISTANCE }
                )

                // Tessera 6: Bilanciamento Celle
                val cellBalValue = if (snapshot?.isDualBattery == true && snapshot.cellBalanceDeltaMv != null) {
                    "Δ ${snapshot.cellBalanceDeltaMv} mV"
                } else if (snapshot?.cellBalanceStatus == "SingleCell") {
                    "1S • OK"
                } else notAvail

                val cellBalSubtitle = if (snapshot?.isDualBattery == true && snapshot.cellBalanceDeltaMv != null) {
                    stringResource(R.string.diag_tile_cell_bal_sub_dual, snapshot.cellBalanceDeltaMv)
                } else {
                    stringResource(R.string.diag_tile_cell_bal_sub_single)
                }

                DiagnosticTile(
                    label = stringResource(R.string.diag_tile_cell_bal),
                    value = cellBalValue,
                    subtitle = cellBalSubtitle,
                    modifier = Modifier.weight(1f),
                    showInfoIcon = true,
                    onClick = { activeInfoDialog = DiagnosticInfoType.CELL_BALANCE }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Riga 4: Saturazione Reale (Punto 4) & Capacità Normalizzata a 25°C (Punto 5)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tessera 7: Saturazione Reale (True Full vs Display 100%)
                val satValue = when (snapshot?.saturationStatus) {
                    "SATURATED" -> stringResource(R.string.diag_sat_value_saturated)
                    "CV_TAPERING" -> stringResource(R.string.diag_sat_value_tapering)
                    "CHARGING" -> snapshot.chipSoc?.let { "SOC $it%" } ?: stringResource(R.string.diag_standard_charging)
                    "DISCHARGING" -> snapshot.chipSoc?.let { "SOC $it%" } ?: stringResource(R.string.diag_discharging)
                    else -> notAvail
                }
                val satSubtitle = when (snapshot?.saturationStatus) {
                    "SATURATED" -> stringResource(R.string.diag_tile_saturation_sub_saturated)
                    "CV_TAPERING" -> stringResource(R.string.diag_tile_saturation_sub_tapering)
                    "CHARGING" -> stringResource(R.string.diag_tile_saturation_sub_charging, snapshot.chipSoc ?: 0)
                    "DISCHARGING" -> stringResource(R.string.diag_tile_saturation_sub_discharging, snapshot.chipSoc ?: 0)
                    else -> stringResource(R.string.diag_tile_saturation_sub_standby)
                }

                DiagnosticTile(
                    label = stringResource(R.string.diag_tile_saturation),
                    value = satValue,
                    subtitle = satSubtitle,
                    modifier = Modifier.weight(1f),
                    showInfoIcon = true,
                    onClick = { activeInfoDialog = DiagnosticInfoType.SATURATION }
                )

                // Tessera 8: Capacità Normalizzata a 25°C (Standard IEC 61960)
                DiagnosticTile(
                    label = stringResource(R.string.diag_tile_temp_comp),
                    value = snapshot?.tempCompensatedCapacityMah?.let { "${it.toInt()} mAh" } ?: notAvail,
                    subtitle = stringResource(R.string.diag_tile_temp_comp_sub),
                    modifier = Modifier.weight(1f),
                    showInfoIcon = true,
                    onClick = { activeInfoDialog = DiagnosticInfoType.TEMP_COMPENSATION }
                )
            }

            // Date di Produzione & Età Batteria (Punto 1)
            if (snapshot?.manuDate != null || snapshot?.firstUsageDate != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(
                                    R.string.diag_production_info,
                                    snapshot.manuDate ?: notAvail,
                                    snapshot.firstUsageDate ?: notAvail
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        snapshot.batteryAgeMonths?.let { months ->
                            Spacer(modifier = Modifier.height(2.dp))
                            val years = months / 12
                            val remMonths = months % 12
                            val ageDesc = if (years > 0) {
                                stringResource(R.string.diag_age_years_months, years, remMonths)
                            } else {
                                stringResource(R.string.diag_age_months, months)
                            }
                            Text(
                                text = stringResource(R.string.diag_age_info, ageDesc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Riquadro Silicio-Carbonio De-compensato (Punto 4 di PlusPlusBattery)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.diag_raw_silicon_carbon_title),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (snapshot?.rawSohPercentage != null) {
                                stringResource(R.string.diag_raw_silicon_carbon_sub_active)
                            } else {
                                stringResource(R.string.diag_raw_silicon_carbon_sub_locked)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (snapshot?.rawSohPercentage != null) {
                        Text(
                            text = "${snapshot.rawSohPercentage}%" + (snapshot.rawFccMah?.let { " ($it mAh)" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = stringResource(R.string.diag_root_required),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Stato Calibrazione & Deriva BMS (Punto 2)
            if (snapshot?.bmsSyncStatus != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val (syncColor, syncStatusText) = when (snapshot.bmsSyncStatus) {
                    BmsSyncStatus.SYNCED -> Pair(
                        MaterialTheme.colorScheme.primary,
                        stringResource(R.string.bms_sync_status_synced)
                    )
                    BmsSyncStatus.GOOD -> Pair(
                        MaterialTheme.colorScheme.tertiary,
                        stringResource(R.string.bms_sync_status_good, snapshot.cyclesSinceLastCalibration ?: 0)
                    )
                    BmsSyncStatus.CALIBRATION_RECOMMENDED -> Pair(
                        MaterialTheme.colorScheme.error,
                        stringResource(R.string.bms_sync_status_recal_rec, snapshot.cyclesSinceLastCalibration ?: 30)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeInfoDialog = DiagnosticInfoType.BMS_SYNC }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.bms_sync_title),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = syncStatusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = syncColor
                            )
                        }

                        TextButton(
                            onClick = { activeInfoDialog = DiagnosticInfoType.BMS_SYNC },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.bms_sync_action_guide),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Integrità e Sicurezza Hardware BMS (Punto 6)
            if (snapshot?.isHardwareSafe != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val isSafe = snapshot.isHardwareSafe == true
                val safetyColor = if (isSafe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeInfoDialog = DiagnosticInfoType.SAFETY_FLAGS }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isSafe) Icons.Default.VerifiedUser else Icons.Default.Warning,
                                contentDescription = null,
                                tint = safetyColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.diag_safety_title),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Info",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isSafe) stringResource(R.string.diag_safety_all_ok) else stringResource(R.string.diag_safety_fault_detected, snapshot.safetyFaultDetails ?: ""),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = safetyColor
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isSafe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = if (isSafe) stringResource(R.string.diag_safety_badge_safe) else stringResource(R.string.diag_safety_badge_warning),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSafe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticTile(
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    showInfoIcon: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (showInfoIcon) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun BatteryHealthTrendCard(
    history: List<BatteryData>,
    projection: BatteryViewModel.BatteryProjection?,
    currentCycles: Int?
) {
    var showProjectionDialog by remember { mutableStateOf(false) }

    if (showProjectionDialog) {
        AlertDialog(
            onDismissRequest = { showProjectionDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.dialog_projection_info_title), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(R.string.dialog_projection_info_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showProjectionDialog = false }) {
                    Text(stringResource(R.string.dialog_rm_info_close), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header con icona e titolo (Badge ridondante rimosso su richiesta utente)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.trend_card_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Proiezione Vita Utile (Punto 4) - Cliccabile con dialog esplicativo
            if (projection != null && projection.remainingCycles > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showProjectionDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(
                                    R.string.trend_projection_text,
                                    projection.remainingCycles,
                                    projection.totalCyclesAt80,
                                    currentCycles?.toString() ?: stringResource(R.string.not_available)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Grafico Lineare Canvas Particolareggiato
            val sortedHistory = remember(history) {
                history.filter { it.healthPercentage != null && it.healthPercentage > 0 }
                    .sortedBy { it.timestamp }
            }

            if (sortedHistory.size >= 2) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                val warningColor = MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
                val textColor = MaterialTheme.colorScheme.onSurfaceVariant
                val textMeasurer = rememberTextMeasurer()
                val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) }

                // Pre-misura etichette asse Y
                val label100Layout = remember(textMeasurer, textColor) {
                    textMeasurer.measure(
                        text = "100%",
                        style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Normal, color = textColor)
                    )
                }
                val label90Layout = remember(textMeasurer, textColor) {
                    textMeasurer.measure(
                        text = "90%",
                        style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Normal, color = textColor)
                    )
                }
                val label80Layout = remember(textMeasurer, warningColor) {
                    textMeasurer.measure(
                        text = "80% (Min)",
                        style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = warningColor)
                    )
                }

                // Pre-misura etichette percentuali sui punti e date su asse X
                val pointLabels = remember(sortedHistory, textMeasurer, primaryColor) {
                    sortedHistory.map { record ->
                        textMeasurer.measure(
                            text = "${record.healthPercentage}%",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        )
                    }
                }
                val dateLabels = remember(sortedHistory, textMeasurer, textColor) {
                    sortedHistory.mapIndexed { index, record ->
                        val shouldShow = (index == 0 || index == sortedHistory.size - 1 || (sortedHistory.size >= 5 && index == sortedHistory.size / 2))
                        if (shouldShow) {
                            textMeasurer.measure(
                                text = chartDateFormatter.format(Date(record.timestamp)),
                                style = TextStyle(
                                    fontSize = 9.sp,
                                    color = textColor
                                )
                            )
                        } else null
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(vertical = 4.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        val paddingLeft = 16.dp.toPx()
                        val paddingRight = 46.dp.toPx() // Spazio per le etichette Y (100%, 90%, 80%)
                        val paddingTop = 20.dp.toPx()    // Spazio per etichette percentuali sopra i punti
                        val paddingBottom = 24.dp.toPx() // Spazio per date asse X

                        val usableWidth = width - paddingLeft - paddingRight
                        val usableHeight = height - paddingTop - paddingBottom

                        val healthValues = sortedHistory.map { it.healthPercentage!! }
                        val actualMin = (healthValues.minOrNull() ?: 80).toFloat()
                        // Assicuriamo che l'intervallo mostri sempre almeno da 75% o min-2% fino al 100%
                        val minY = minOf(75f, actualMin - 2f)
                        val maxY = 100f
                        val healthRange = (maxY - minY).coerceAtLeast(10f)

                        fun getYForHealth(health: Float): Float {
                            val normY = (health - minY) / healthRange
                            return height - paddingBottom - (normY * usableHeight)
                        }

                        // 1. Griglia orizzontale di riferimento a 100%, 90%, 80%
                        val gridLevels = listOf(
                            Triple(100f, outlineColor, label100Layout),
                            Triple(90f, outlineColor, label90Layout),
                            Triple(80f, warningColor, label80Layout)
                        )

                        for ((lvl, lineColor, textResult) in gridLevels) {
                            if (lvl in minY..maxY) {
                                val yLevel = getYForHealth(lvl)
                                val is80 = (lvl == 80f)

                                drawLine(
                                    color = lineColor,
                                    start = Offset(paddingLeft, yLevel),
                                    end = Offset(width - paddingRight, yLevel),
                                    strokeWidth = if (is80) 1.5.dp.toPx() else 1.dp.toPx(),
                                    pathEffect = dashEffect
                                )

                                drawText(
                                    textLayoutResult = textResult,
                                    topLeft = Offset(
                                        x = width - paddingRight + 4.dp.toPx(),
                                        y = yLevel - (textResult.size.height / 2f)
                                    )
                                )
                            }
                        }

                        // 2. Coordinate dei punti
                        val points = sortedHistory.mapIndexed { index, record ->
                            val x = paddingLeft + (index.toFloat() / (sortedHistory.size - 1)) * usableWidth
                            val y = getYForHealth(record.healthPercentage!!.toFloat())
                            Offset(x, y)
                        }

                        // 3. Gradiente sotto la curva
                        val fillPath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                            lineTo(points.last().x, height - paddingBottom)
                            lineTo(points.first().x, height - paddingBottom)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.30f), Color.Transparent),
                                startY = paddingTop,
                                endY = height - paddingBottom
                            )
                        )

                        // 4. Linea congiungente
                        val strokePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }

                        drawPath(
                            path = strokePath,
                            color = primaryColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // 5. Punti ed etichette valori & date
                        points.forEachIndexed { index, pt ->
                            // Cerchio esterno con trasparenza
                            drawCircle(
                                color = primaryColor.copy(alpha = 0.25f),
                                radius = 6.dp.toPx(),
                                center = pt
                            )
                            // Cerchio interno
                            drawCircle(
                                color = primaryColor,
                                radius = 3.5.dp.toPx(),
                                center = pt
                            )

                            // Etichetta del valore sopra ogni punto
                            val valLayout = pointLabels[index]
                            drawText(
                                textLayoutResult = valLayout,
                                topLeft = Offset(
                                    x = (pt.x - valLayout.size.width / 2f).coerceIn(0f, width - valLayout.size.width),
                                    y = pt.y - valLayout.size.height - 3.dp.toPx()
                                )
                            )

                            // Etichette data su asse X (primo, ultimo o intermedio se molti punti)
                            val dateLayout = dateLabels[index]
                            if (dateLayout != null) {
                                val dateX = if (index == 0) {
                                    paddingLeft
                                } else if (index == points.size - 1) {
                                    (width - paddingRight - dateLayout.size.width)
                                } else {
                                    pt.x - dateLayout.size.width / 2f
                                }
                                drawText(
                                    textLayoutResult = dateLayout,
                                    topLeft = Offset(
                                        x = dateX,
                                        y = height - paddingBottom + 4.dp.toPx()
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.trend_empty_prompt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

