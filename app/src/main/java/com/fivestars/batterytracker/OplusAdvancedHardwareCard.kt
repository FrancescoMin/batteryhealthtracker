package com.fivestars.batterytracker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.abs

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
private fun DiagnosticHardwareDialog(
    dialogType: DiagnosticInfoType?,
    onDismiss: () -> Unit
) {
    if (dialogType == null) return
    val (titleRes, descRes) = when (dialogType) {
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
    }

    AppAlertDialog(
        onDismissRequest = onDismiss,
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
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_rm_info_close), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun OplusAdvancedHardwareCard(
    snapshot: BatterySnapshot?,
    onOpenConsole: () -> Unit = {}
) {
    val notAvail = stringResource(R.string.not_available)
    var activeInfoDialog by remember { mutableStateOf<DiagnosticInfoType?>(null) }

    DiagnosticHardwareDialog(
        dialogType = activeInfoDialog,
        onDismiss = { activeInfoDialog = null }
    )

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
                        stringResource(R.string.diag_tile_circuit_volt_sub_dual, abs(snapshot.cell0VoltageMv - snapshot.cell1VoltageMv))
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

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenConsole() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.console_card_title),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.console_card_desc),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
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
