package com.fivestars.batterytracker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Date
import java.util.Locale

@Composable
fun DashboardCards(
    snapshot: BatterySnapshot?,
    onHealthInfoClick: () -> Unit = {},
    onCyclesInfoClick: () -> Unit = {},
    onCapacityInfoClick: () -> Unit = {},
    onOpenConsole: () -> Unit = {}
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
            if (snapshot.isCapacityEstimated) {
                stringResource(R.string.card_capacity_sub_design_estimated, it.toInt())
            } else {
                stringResource(R.string.card_capacity_sub_design, it.toInt())
            }
        } ?: stringResource(R.string.card_capacity_sub_fcc)
    }

    val healthSubtitle = when {
        isHealthInaccurate -> stringResource(R.string.card_health_sub_bm)
        snapshot?.isHealthCalculated == true -> stringResource(R.string.card_health_sub)
        else -> stringResource(R.string.card_health_sub_bms)
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

        // Chip sorgente (cliccabile per aprire la Console Diagnostica)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable { onOpenConsole() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = if (snapshot?.isShizukuUsed == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
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
fun NextScheduleBanner(nextScheduleTime: Long?, isAutoSamplingEnabled: Boolean = true) {
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
                imageVector = if (isAutoSamplingEnabled) Icons.Default.Info else Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isAutoSamplingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            val text = if (!isAutoSamplingEnabled) {
                stringResource(R.string.schedule_disabled)
            } else if (nextScheduleTime != null && nextScheduleTime > System.currentTimeMillis()) {
                val formatted = historyDateFormat.format(Date(nextScheduleTime))
                stringResource(R.string.schedule_next, formatted)
            } else {
                stringResource(R.string.schedule_periodic_active)
            }
            Text(text = text, style = MaterialTheme.typography.bodySmall)
        }
    }
}
