package com.fivestars.batterytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: BatteryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BatteryTrackerScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryTrackerScreen(viewModel: BatteryViewModel) {
    val history by viewModel.allBatteryData.collectAsState(initial = emptyList())
    val nextScheduleTime by viewModel.nextWorkScheduleTime.collectAsState(initial = null)

    val context = LocalContext.current

    // Auto-refresh real-time stats state
    var realTimeStats by remember { mutableStateOf(viewModel.getCurrentBatteryStats(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Battery Health Tracker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            // Real-Time View
            Text(
                text = "Real-Time Status",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val isPermissionMissing = realTimeStats.first == -1 && realTimeStats.second == -1

            if (isPermissionMissing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Permission Required",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To read battery stats, please connect your device to PC and run the following ADB command:\n\n" +
                                   "adb shell pm grant com.fivestars.batterytracker android.permission.BATTERY_STATS\n\n" +
                                   "Then click 'Refresh' below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { realTimeStats = viewModel.getCurrentBatteryStats(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Refresh", color = MaterialTheme.colorScheme.onError)
                        }
                    }
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Current Health: ${if (realTimeStats.first > 0) "${realTimeStats.first}%" else "Unsupported"}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Current Cycle Count: ${if (realTimeStats.second >= 0) realTimeStats.second else "Unsupported"}")

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.saveCurrentBatteryData(context)
                                realTimeStats = viewModel.getCurrentBatteryStats(context) // Refresh
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Save Current Status")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Next Scheduled Time View
            if (nextScheduleTime != null) {
                Text(
                    text = "Next Scheduled Check: ${formatTimestamp(nextScheduleTime!!)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            } else {
                Text(
                    text = "No scheduled background task running.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            // History View
            Text(
                text = "History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (history.isEmpty()) {
                Text(text = "No history available.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(history) { data ->
                        HistoryItem(data)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(data: BatteryData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Date: ${formatTimestamp(data.timestamp)}")
            Text(text = "Cycles: ${if (data.cycleCount >= 0) data.cycleCount else "Unsupported"} | Health: ${if (data.healthStatus > 0) "${data.healthStatus}%" else "Unsupported"}")
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
