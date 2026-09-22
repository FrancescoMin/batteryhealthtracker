package com.fivestars.batterytracker

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatteryConfigState(
    val customRatedCapacityMah: Double?,
    val presetLabel: String,
    val isAutoDetection: Boolean
)

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED
}

class BatteryPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _configState = MutableStateFlow(loadCurrentState())
    val configState: StateFlow<BatteryConfigState> = _configState.asStateFlow()

    private fun loadCurrentState(): BatteryConfigState {
        val isAuto = prefs.getBoolean(KEY_IS_AUTO, true)
        val mahStr = prefs.getString(KEY_RATED_MAH, null)
        val mah = mahStr?.toDoubleOrNull()
        val detected = OplusDevicePresets.detectDevicePreset()
        val defaultLabel = detected?.displayName ?: "Rilevamento Automatico (BMS Chip)"
        val label = prefs.getString(KEY_PRESET_LABEL, defaultLabel) ?: defaultLabel

        return BatteryConfigState(
            customRatedCapacityMah = if (isAuto) null else mah,
            presetLabel = label,
            isAutoDetection = isAuto
        )
    }

    fun getCustomRatedCapacity(): Double? {
        if (prefs.getBoolean(KEY_IS_AUTO, true)) return null
        return prefs.getString(KEY_RATED_MAH, null)?.toDoubleOrNull()
    }

    fun getPresetLabel(): String {
        val detected = OplusDevicePresets.detectDevicePreset()
        val defaultLabel = detected?.displayName ?: "Rilevamento Automatico (BMS Chip)"
        return prefs.getString(KEY_PRESET_LABEL, defaultLabel) ?: defaultLabel
    }

    fun isAutoDetection(): Boolean {
        return prefs.getBoolean(KEY_IS_AUTO, true)
    }

    fun setCustomRatedCapacity(mah: Double, label: String) {
        prefs.edit()
            .putBoolean(KEY_IS_AUTO, false)
            .putString(KEY_RATED_MAH, mah.toString())
            .putString(KEY_PRESET_LABEL, label)
            .apply()
        _configState.value = BatteryConfigState(
            customRatedCapacityMah = mah,
            presetLabel = label,
            isAutoDetection = false
        )
    }

    fun resetToAutoDetection() {
        val detected = OplusDevicePresets.detectDevicePreset()
        val defaultLabel = detected?.let { "${it.displayName} (Auto)" } ?: "Rilevamento Automatico (BMS Chip)"
        prefs.edit()
            .putBoolean(KEY_IS_AUTO, true)
            .remove(KEY_RATED_MAH)
            .putString(KEY_PRESET_LABEL, defaultLabel)
            .apply()
        _configState.value = BatteryConfigState(
            customRatedCapacityMah = null,
            presetLabel = defaultLabel,
            isAutoDetection = true
        )
    }

    companion object {
        private const val PREFS_NAME = "battery_tracker_preferences"
        private const val KEY_IS_AUTO = "key_is_auto_detection"
        private const val KEY_RATED_MAH = "key_custom_rated_mah"
        private const val KEY_PRESET_LABEL = "key_selected_preset_label"
        private const val KEY_APP_LANGUAGE = "key_app_language"
        private const val KEY_LAST_CALIBRATION_CYCLE = "key_last_calibration_cycle"
        private const val KEY_APP_THEME_MODE = "key_app_theme_mode"
    }

    private val _appThemeMode = MutableStateFlow(getAppThemeMode())
    val appThemeMode: StateFlow<AppThemeMode> = _appThemeMode.asStateFlow()

    fun getAppThemeMode(): AppThemeMode {
        val name = prefs.getString(KEY_APP_THEME_MODE, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        return try {
            AppThemeMode.valueOf(name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    fun setAppThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_APP_THEME_MODE, mode.name).apply()
        _appThemeMode.value = mode
    }

    private val _appLanguage = MutableStateFlow(getAppLanguage())
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    fun getAppLanguage(): String {
        return prefs.getString(KEY_APP_LANGUAGE, "system") ?: "system"
    }

    fun setAppLanguage(langCode: String) {
        prefs.edit().putString(KEY_APP_LANGUAGE, langCode).apply()
        _appLanguage.value = langCode
    }

    fun getLastCalibrationCycle(): Int? {
        val v = prefs.getInt(KEY_LAST_CALIBRATION_CYCLE, -1)
        return if (v >= 0) v else null
    }

    fun setLastCalibrationCycle(cycles: Int) {
        prefs.edit().putInt(KEY_LAST_CALIBRATION_CYCLE, cycles).apply()
    }
}
