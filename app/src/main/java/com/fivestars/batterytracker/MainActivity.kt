package com.fivestars.batterytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
    val latestData by viewModel.latestBatteryData.collectAsState(initial = null)
    val history by viewModel.allBatteryData.collectAsState(initial = emptyList())

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
            Text(
                text = "Current Status",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (latestData != null) {
                        Text(text = "Health: ${latestData!!.healthStatus}%")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Cycle Count: ${if (latestData!!.cycleCount >= 0) latestData!!.cycleCount else "N/A"}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Last Updated: ${formatTimestamp(latestData!!.timestamp)}")
                    } else {
                        Text(text = "No data yet. Waiting for first scheduled job.")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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
            Text(text = "Cycles: ${if (data.cycleCount >= 0) data.cycleCount else "N/A"} | Health: ${data.healthStatus}%")
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
