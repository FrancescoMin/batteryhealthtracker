package com.fivestars.batterytracker

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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

    var showEditDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onNavigateBack)

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
            isAuto = configState.isAutoDetection,
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

    Scaffold(
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
                            value = "1.0 - Oplus Edition (Oppo, OnePlus, Realme)"
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

    AlertDialog(
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
        "es" to stringResource(R.string.lang_es)
    )

    AlertDialog(
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
    isAuto: Boolean,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit,
    onResetAuto: () -> Unit
) {
    var textValue by remember { mutableStateOf(String.format(Locale.US, "%.0f", initialMah)) }
    var selectedLabel by remember { mutableStateOf(initialPresetLabel) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedBrandFilter by remember { mutableStateOf("Tutti") }

    val filteredPresets = remember(searchQuery, selectedBrandFilter) {
        OplusDevicePresets.ALL_PRESETS.filter { preset ->
            val matchesBrand = when (selectedBrandFilter) {
                "Oppo" -> preset.brand.equals("Oppo", ignoreCase = true)
                "OnePlus" -> preset.brand.equals("OnePlus", ignoreCase = true)
                "Realme" -> preset.brand.equals("Realme", ignoreCase = true)
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
                                val match = OplusDevicePresets.ALL_PRESETS.firstOrNull { it.ratedMah.toInt().toString() == input }
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val brandOptions = listOf(
                        "Tutti" to stringResource(R.string.brand_all),
                        "Oppo" to stringResource(R.string.brand_oppo),
                        "OnePlus" to stringResource(R.string.brand_oneplus),
                        "Realme" to stringResource(R.string.brand_realme)
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
