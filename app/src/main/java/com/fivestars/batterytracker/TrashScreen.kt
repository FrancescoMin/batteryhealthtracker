package com.fivestars.batterytracker

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        AppAlertDialog(
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
