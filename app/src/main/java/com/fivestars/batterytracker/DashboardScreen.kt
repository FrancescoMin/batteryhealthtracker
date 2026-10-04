package com.fivestars.batterytracker

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val samplingConfig by viewModel.samplingConfig.collectAsState()

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

    // Launcher per importazione CSV via Storage Access Framework
    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val result = viewModel.importCsvFromUri(uri)
                val message = if (result.success) {
                    if (result.importedCount > 0) {
                        if (result.duplicateCount > 0) {
                            context.getString(R.string.csv_import_success_snackbar_with_duplicates, result.importedCount, result.duplicateCount)
                        } else {
                            context.getString(R.string.csv_import_success_snackbar, result.importedCount)
                        }
                    } else {
                        context.getString(R.string.csv_import_all_duplicates, result.duplicateCount)
                    }
                } else {
                    result.errorMessage ?: context.getString(R.string.csv_import_error_msg)
                }
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    // Dialog per la spiegazione della salute della batteria (SOH)
    var showHealthInfoDialog by remember { mutableStateOf(false) }

    if (showHealthInfoDialog) {
        val isBm = currentSnapshot?.isShizukuUsed != true
        AppAlertDialog(
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
        AppAlertDialog(
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
        AppAlertDialog(
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
        AppAlertDialog(
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

    var showDiagnosticConsole by remember { mutableStateOf(false) }

    if (showDiagnosticConsole) {
        DiagnosticConsoleDialog(
            snapshot = currentSnapshot,
            onDismiss = { showDiagnosticConsole = false }
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
                var showOverflowMenu by remember { mutableStateOf(false) }

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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDiagnosticConsole = true }) {
                            Icon(Icons.Default.Terminal, contentDescription = stringResource(R.string.action_diagnostic_console))
                        }

                        IconButton(onClick = { viewModel.refreshSnapshot(resetProbe = true) }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh))
                        }

                        Box {
                            BadgedBox(
                                badge = {
                                    if (trashedItems.isNotEmpty()) {
                                        Badge { Text("${trashedItems.size}") }
                                    }
                                }
                            ) {
                                IconButton(onClick = { showOverflowMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = stringResource(R.string.action_more_options)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_export_csv)) },
                                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        val defaultFileName = "battery_history_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.csv"
                                        exportCsvLauncher.launch(defaultFileName)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_import_csv)) },
                                    leadingIcon = { Icon(Icons.Default.Upload, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        importCsvLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "application/csv", "text/plain", "*/*"))
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (trashedItems.isNotEmpty()) "${stringResource(R.string.title_trash)} (${trashedItems.size})"
                                            else stringResource(R.string.title_trash)
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onNavigateToTrash()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_settings)) },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onNavigateToSettings()
                                    }
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
                        .clickable { viewModel.refreshSnapshot(resetProbe = true) }
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
                    onCapacityInfoClick = { showCapacityFluctuationDialog = true },
                    onOpenConsole = { showDiagnosticConsole = true }
                )
            }

            item(key = "oplus_hardware_card") {
                Spacer(modifier = Modifier.height(12.dp))
                // Diagnostica Hardware & BMS Oplus (Punti 1, 2, 3, 4)
                OplusAdvancedHardwareCard(
                    snapshot = currentSnapshot,
                    onOpenConsole = { showDiagnosticConsole = true }
                )
            }

            item(key = "trend_card") {
                Spacer(modifier = Modifier.height(12.dp))
                // Andamento Salute & Proiezione Cicli all'80% (Punto 4)
                BatteryHealthTrendCard(
                    history = history,
                    projection = viewModel.getBatteryProjection(),
                    currentCycles = currentSnapshot?.cycleCount,
                    daysSinceFirstBoot = currentSnapshot?.daysSinceFirstUsage
                )
            }

            item(key = "schedule_banner") {
                Spacer(modifier = Modifier.height(16.dp))
                // Info sul prossimo campionamento automatico
                NextScheduleBanner(
                    nextScheduleTime = nextScheduleTime,
                    isAutoSamplingEnabled = samplingConfig.isEnabled
                )
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
