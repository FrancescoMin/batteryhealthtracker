package com.fivestars.batterytracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Date
import kotlin.math.roundToInt

@Composable
fun BatteryHealthTrendCard(
    history: List<BatteryData>,
    projection: BatteryViewModel.BatteryProjection?,
    currentCycles: Int?,
    daysSinceFirstBoot: Int? = null
) {
    var showProjectionDialog by remember { mutableStateOf(false) }
    var showUsageStatsDialog by remember { mutableStateOf(false) }

    if (showUsageStatsDialog) {
        AppAlertDialog(
            onDismissRequest = { showUsageStatsDialog = false },
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
                    Text(stringResource(R.string.dialog_usage_stats_title), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(R.string.dialog_usage_stats_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showUsageStatsDialog = false }) {
                    Text(stringResource(R.string.dialog_rm_info_close), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showProjectionDialog) {
        AppAlertDialog(
            onDismissRequest = { showProjectionDialog = false },
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
                    Text(stringResource(R.string.dialog_projection_info_title), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(R.string.dialog_projection_info_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showProjectionDialog = false }) {
                    Text(stringResource(R.string.dialog_rm_info_close), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header con icona e titolo (Badge ridondante rimosso su richiesta utente)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.trend_card_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Proiezione Vita Utile (Punto 4) - Cliccabile con dialog esplicativo
            if (projection != null && projection.remainingCycles > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showProjectionDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(
                                    R.string.trend_projection_text,
                                    projection.remainingCycles,
                                    projection.totalCyclesAt80,
                                    currentCycles?.toString() ?: stringResource(R.string.not_available)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Box Statistiche Utilizzo: Giorni dal 1° Avvio & Rapporto Cicli
            if (daysSinceFirstBoot != null && daysSinceFirstBoot in 1..BatteryTelemetryParser.MAX_VALID_BOOT_DAYS) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showUsageStatsDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.trend_days_since_first_boot, daysSinceFirstBoot),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (currentCycles != null && currentCycles > 0) {
                                    val daysPerCycle = daysSinceFirstBoot.toDouble() / currentCycles.toDouble()
                                    val cyclesPerDay = currentCycles.toDouble() / daysSinceFirstBoot.toDouble()
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.trend_usage_ratio, daysPerCycle, cyclesPerDay),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Grafico Lineare Canvas Particolareggiato
            val sortedHistory = remember(history) {
                history.filter { it.healthPercentage != null && it.healthPercentage > 0 }
                    .sortedBy { it.timestamp }
            }

            if (sortedHistory.size >= 2) {
                var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
                val haptic = LocalHapticFeedback.current

                val firstRecord = sortedHistory.first()
                val latestRecord = sortedHistory.last()
                val firstHealth = firstRecord.healthPercentage ?: 100
                val latestHealth = latestRecord.healthPercentage ?: 100
                val delta = latestHealth - firstHealth

                // 1. Scheda di riepilogo sintetico (Iniziale, Attuale, Delta)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Iniziale
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.trend_stat_initial),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$firstHealth%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Attuale
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.trend_stat_current),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$latestHealth%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Variazione
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.trend_stat_change),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val deltaColor = when {
                                delta < 0 -> MaterialTheme.colorScheme.error
                                delta > 0 -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            val deltaStr = when {
                                delta > 0 -> "+$delta%"
                                delta < 0 -> "$delta%"
                                else -> "0%"
                            }
                            Text(
                                text = deltaStr,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = deltaColor
                            )
                        }
                    }
                }

                // 2. Banner dinamico di ispezione interattiva al tocco (Scrubber Snapshot Detail)
                AnimatedVisibility(visible = selectedPointIndex != null) {
                    val selIdx = selectedPointIndex
                    if (selIdx != null && selIdx in sortedHistory.indices) {
                        val record = sortedHistory[selIdx]
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = historyDateFormat.format(Date(record.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val details = buildList {
                                        record.cycleCount?.let { add(stringResource(R.string.history_item_cycles, it)) }
                                        record.currentCapacityMah?.let { add(stringResource(R.string.history_item_capacity, "%.0f mAh".format(it))) }
                                    }.joinToString(" • ")
                                    if (details.isNotEmpty()) {
                                        Text(
                                            text = details,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${record.healthPercentage}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { selectedPointIndex = null },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                val primaryColor = MaterialTheme.colorScheme.primary
                val surfaceColor = MaterialTheme.colorScheme.surface
                val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                val warningColor = MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
                val textColor = MaterialTheme.colorScheme.onSurfaceVariant
                val textMeasurer = rememberTextMeasurer()
                val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) }
                val scrubberDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f) }

                // Pre-misura etichette asse Y
                val label100Layout = remember(textMeasurer, textColor) {
                    textMeasurer.measure(
                        text = "100%",
                        style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Normal, color = textColor)
                    )
                }
                val label90Layout = remember(textMeasurer, textColor) {
                    textMeasurer.measure(
                        text = "90%",
                        style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Normal, color = textColor)
                    )
                }
                val label80Layout = remember(textMeasurer, warningColor) {
                    textMeasurer.measure(
                        text = "80% (Min)",
                        style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = warningColor)
                    )
                }

                // Pre-misura date su asse X (estremi e centro per leggibilità ottimale)
                val dateLabels = remember(sortedHistory, textMeasurer, textColor) {
                    sortedHistory.mapIndexed { index, record ->
                        val shouldShow = (index == 0 || index == sortedHistory.size - 1 || (sortedHistory.size >= 5 && index == sortedHistory.size / 2))
                        if (shouldShow) {
                            textMeasurer.measure(
                                text = chartDateFormatter.format(Date(record.timestamp)),
                                style = TextStyle(
                                    fontSize = 9.sp,
                                    color = textColor
                                )
                            )
                        } else null
                    }
                }

                val actualMin = remember(sortedHistory) {
                    (sortedHistory.mapNotNull { it.healthPercentage }.minOrNull() ?: 80).toFloat()
                }
                val minY = remember(actualMin) { minOf(75f, actualMin - 2f) }
                val maxY = 100f
                val healthRange = remember(minY) { (maxY - minY).coerceAtLeast(10f) }
                val gridLevels = remember(outlineColor, warningColor, label100Layout, label90Layout, label80Layout) {
                    listOf(
                        Triple(100f, outlineColor, label100Layout),
                        Triple(90f, outlineColor, label90Layout),
                        Triple(80f, warningColor, label80Layout)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(vertical = 4.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(sortedHistory) {
                                detectTapGestures { offset ->
                                    val paddingLeft = 16.dp.toPx()
                                    val paddingRight = 46.dp.toPx()
                                    val usableWidth = size.width.toFloat() - paddingLeft - paddingRight
                                    if (usableWidth > 0f && sortedHistory.size > 1) {
                                        val normX = ((offset.x - paddingLeft) / usableWidth).coerceIn(0f, 1f)
                                        val clickedIdx = (normX * (sortedHistory.size - 1)).roundToInt()
                                        if (selectedPointIndex == clickedIdx) {
                                            selectedPointIndex = null
                                        } else {
                                            selectedPointIndex = clickedIdx
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                }
                            }
                            .pointerInput(sortedHistory) {
                                detectHorizontalDragGestures(
                                    onDragStart = { offset ->
                                        val paddingLeft = 16.dp.toPx()
                                        val paddingRight = 46.dp.toPx()
                                        val usableWidth = size.width.toFloat() - paddingLeft - paddingRight
                                        if (usableWidth > 0f && sortedHistory.size > 1) {
                                            val normX = ((offset.x - paddingLeft) / usableWidth).coerceIn(0f, 1f)
                                            val newIdx = (normX * (sortedHistory.size - 1)).roundToInt()
                                            if (selectedPointIndex != newIdx) {
                                                selectedPointIndex = newIdx
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                    },
                                    onHorizontalDrag = { change, _ ->
                                        change.consume()
                                        val paddingLeft = 16.dp.toPx()
                                        val paddingRight = 46.dp.toPx()
                                        val usableWidth = size.width.toFloat() - paddingLeft - paddingRight
                                        if (usableWidth > 0f && sortedHistory.size > 1) {
                                            val normX = ((change.position.x - paddingLeft) / usableWidth).coerceIn(0f, 1f)
                                            val newIdx = (normX * (sortedHistory.size - 1)).roundToInt()
                                            if (selectedPointIndex != newIdx) {
                                                selectedPointIndex = newIdx
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        val width = size.width
                        val height = size.height

                        val paddingLeft = 16.dp.toPx()
                        val paddingRight = 46.dp.toPx() // Spazio per le etichette Y (100%, 90%, 80%)
                        val paddingTop = 14.dp.toPx()
                        val paddingBottom = 22.dp.toPx() // Spazio per date asse X

                        val usableWidth = width - paddingLeft - paddingRight
                        val usableHeight = height - paddingTop - paddingBottom

                        fun getYForHealth(health: Float): Float {
                            val normY = (health - minY) / healthRange
                            return height - paddingBottom - (normY * usableHeight)
                        }

                        // 1. Griglia orizzontale di riferimento a 100%, 90%, 80%
                        for ((lvl, lineColor, textResult) in gridLevels) {
                            if (lvl in minY..maxY) {
                                val yLevel = getYForHealth(lvl)
                                val is80 = (lvl == 80f)

                                drawLine(
                                    color = lineColor,
                                    start = Offset(paddingLeft, yLevel),
                                    end = Offset(width - paddingRight, yLevel),
                                    strokeWidth = if (is80) 1.5.dp.toPx() else 1.dp.toPx(),
                                    pathEffect = dashEffect
                                )

                                drawText(
                                    textLayoutResult = textResult,
                                    topLeft = Offset(
                                        x = width - paddingRight + 4.dp.toPx(),
                                        y = yLevel - (textResult.size.height / 2f)
                                    )
                                )
                            }
                        }

                        // 2. Coordinate dei punti
                        val points = sortedHistory.mapIndexed { index, record ->
                            val x = paddingLeft + (index.toFloat() / (sortedHistory.size - 1)) * usableWidth
                            val y = getYForHealth(record.healthPercentage!!.toFloat())
                            Offset(x, y)
                        }

                        // Helper per costruire la curva Catmull-Rom Bézier fluida
                        fun Path.traceSmoothCurve() {
                            moveTo(points.first().x, points.first().y)
                            if (points.size == 2) {
                                lineTo(points[1].x, points[1].y)
                                return
                            }
                            for (i in 0 until points.size - 1) {
                                val p0 = points[maxOf(0, i - 1)]
                                val p1 = points[i]
                                val p2 = points[i + 1]
                                val p3 = points[minOf(points.size - 1, i + 2)]

                                val c1x = p1.x + (p2.x - p0.x) / 6f
                                val c1y = p1.y + (p2.y - p0.y) / 6f
                                val c2x = p2.x - (p3.x - p1.x) / 6f
                                val c2y = p2.y - (p3.y - p1.y) / 6f

                                cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
                            }
                        }

                        // 3. Gradiente morbido sotto la curva
                        val fillPath = Path().apply {
                            traceSmoothCurve()
                            lineTo(points.last().x, height - paddingBottom)
                            lineTo(points.first().x, height - paddingBottom)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.28f),
                                    primaryColor.copy(alpha = 0.04f),
                                    Color.Transparent
                                ),
                                startY = paddingTop,
                                endY = height - paddingBottom
                            )
                        )

                        // 4. Linea congiungente smussata
                        val strokePath = Path().apply {
                            traceSmoothCurve()
                        }

                        drawPath(
                            path = strokePath,
                            color = primaryColor,
                            style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // 5. Linea guida verticale dello Scrubber al tocco
                        val currentSelIdx = selectedPointIndex
                        if (currentSelIdx != null && currentSelIdx in points.indices) {
                            val selPt = points[currentSelIdx]
                            drawLine(
                                color = primaryColor.copy(alpha = 0.55f),
                                start = Offset(selPt.x, paddingTop),
                                end = Offset(selPt.x, height - paddingBottom),
                                strokeWidth = 1.2.dp.toPx(),
                                pathEffect = scrubberDashEffect
                            )
                        }

                        // 6. Disegno punti
                        points.forEachIndexed { index, pt ->
                            val isSelected = (index == currentSelIdx)

                            if (isSelected) {
                                // Punto attivo selezionato con alone espanso e cerchio a contrasto
                                drawCircle(
                                    color = primaryColor.copy(alpha = 0.25f),
                                    radius = 8.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = surfaceColor,
                                    radius = 5.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = primaryColor,
                                    radius = 3.5.dp.toPx(),
                                    center = pt
                                )
                            } else {
                                // Punti normali discreti (nessun sovraffollamento)
                                val isEndpoint = (index == 0 || index == points.size - 1)
                                val dotRadius = if (isEndpoint) 3.dp.toPx() else 2.dp.toPx()
                                val dotAlpha = if (isEndpoint) 0.85f else 0.45f

                                drawCircle(
                                    color = primaryColor.copy(alpha = dotAlpha),
                                    radius = dotRadius,
                                    center = pt
                                )
                            }

                            // Etichette data su asse X (primo, ultimo o intermedio se molti punti)
                            val dateLayout = dateLabels[index]
                            if (dateLayout != null) {
                                val dateX = if (index == 0) {
                                    paddingLeft
                                } else if (index == points.size - 1) {
                                    (width - paddingRight - dateLayout.size.width)
                                } else {
                                    pt.x - dateLayout.size.width / 2f
                                }
                                drawText(
                                    textLayoutResult = dateLayout,
                                    topLeft = Offset(
                                        x = dateX,
                                        y = height - paddingBottom + 4.dp.toPx()
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.trend_empty_prompt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}
