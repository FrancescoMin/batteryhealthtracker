package com.fivestars.batterytracker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

data class DiagnosticLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val command: String,
    val result: String?,
    val isSuccess: Boolean
) {
    fun formatTime(): String {
        return SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
    }
}

object DiagnosticLogger {
    private const val MAX_LOGS = 250
    private val logs = ConcurrentLinkedDeque<DiagnosticLogEntry>()
    const val GITHUB_ISSUES_URL = "https://github.com/FrancescoMin/batteryhealthtracker/issues"
    const val GITHUB_COMPATIBILITY_FORM_URL = "https://github.com/FrancescoMin/batteryhealthtracker/issues/new?template=device_compatibility.yml"

    fun log(tag: String, command: String, result: String?, isSuccess: Boolean = true) {
        val entry = DiagnosticLogEntry(
            tag = tag,
            command = command.trim(),
            result = result?.trim(),
            isSuccess = isSuccess
        )
        logs.addLast(entry)
        while (logs.size > MAX_LOGS) {
            logs.pollFirst()
        }
    }

    fun getLogs(): List<DiagnosticLogEntry> {
        return logs.toList()
    }

    fun clear() {
        logs.clear()
    }

    fun buildMarkdownReport(snapshot: BatterySnapshot?): String {
        val sb = StringBuilder()
        sb.append("### 📱 Battery Health Tracker - Device Diagnostic Report\n\n")
        sb.append("#### ⚙️ Device Environment\n")
        sb.append("- **Manufacturer:** `${Build.MANUFACTURER}`\n")
        sb.append("- **Model:** `${Build.MODEL}`\n")
        sb.append("- **Device / Product:** `${Build.DEVICE}` / `${Build.PRODUCT}`\n")
        sb.append("- **Hardware / Board:** `${Build.HARDWARE}` / `${Build.BOARD}`\n")
        sb.append("- **Android Version:** Android `${Build.VERSION.RELEASE}` (SDK ${Build.VERSION.SDK_INT})\n")
        sb.append("- **Build Fingerprint:** `${Build.FINGERPRINT}`\n\n")

        val preset = OplusDevicePresets.detectDevicePreset()
        sb.append("#### 🔋 Battery Telemetry & State\n")
        sb.append("- **Detected Preset:** ${preset?.displayName ?: "None / Unmapped"}\n")
        if (preset != null) {
            sb.append("  - Typical Capacity: `${preset.typicalMah} mAh`\n")
            sb.append("  - Rated Capacity (IEC 61960): `${preset.ratedMah} mAh`\n")
        }
        sb.append("- **Telemetry Source:** `${snapshot?.source ?: "N/A"}`\n")
        sb.append("- **Battery Health (%):** `${snapshot?.healthPercentage ?: "N/A"}%` (Calculated: `${snapshot?.isHealthCalculated ?: false}`)\n")
        sb.append("- **Current / FCC Capacity:** `${snapshot?.currentCapacityMah ?: "N/A"} mAh`\n")
        sb.append("- **Design Capacity (Base):** `${snapshot?.designCapacityMah ?: "N/A"} mAh`\n")
        sb.append("- **Battery Level (%):** `${snapshot?.batteryLevelPercentage ?: "N/A"}%`\n")
        sb.append("- **Charge Cycles:** `${snapshot?.cycleCount ?: "N/A"}`\n")
        sb.append("- **Voltage:** `${snapshot?.cell0VoltageMv ?: "N/A"} mV`\n")
        sb.append("- **Temperature:** `${snapshot?.batteryTemperatureCelsius ?: "N/A"} °C`\n")
        sb.append("- **Raw RM (Remaining):** `${snapshot?.remainingCapacityMah ?: "N/A"} mAh`\n")
        sb.append("- **Charging Power:** `${snapshot?.chargingPowerWatts ?: "N/A"} W (${snapshot?.chargingProtocol ?: "N/A"})`\n\n")

        sb.append("#### 📜 Executed Commands & Log Output\n")
        sb.append("```\n")
        val currentLogs = getLogs()
        if (currentLogs.isEmpty()) {
            sb.append("(No command logs recorded yet. Tap refresh in dashboard to execute commands)\n")
        } else {
            currentLogs.forEach { entry ->
                val status = if (entry.isSuccess) "OK" else "FAIL"
                val res = entry.result?.let {
                    if (it.length > 300) it.take(300) + "... [truncated]" else it
                } ?: "null"
                sb.append("[${entry.formatTime()}] [${entry.tag}] [$status] ${entry.command}\n")
                sb.append("  ↳ OUTPUT: $res\n")
            }
        }
        sb.append("```\n")
        return sb.toString()
    }

    fun copyReportToClipboard(context: Context, snapshot: BatterySnapshot?) {
        val report = buildMarkdownReport(snapshot)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("BatteryHealthTracker Diagnostic Report", report)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, R.string.console_copied_toast, Toast.LENGTH_SHORT).show()
    }

    fun openGitHubIssues(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_COMPATIBILITY_FORM_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open browser", Toast.LENGTH_SHORT).show()
        }
    }
}
