package com.fivestars.batterytracker

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

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
        enforceHighRefreshRate(window, this)

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
        enforceHighRefreshRate(window, this)
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
