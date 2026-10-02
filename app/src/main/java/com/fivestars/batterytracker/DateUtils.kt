package com.fivestars.batterytracker

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val historyDateFormatLock = Any()
private val chartDateFormatterLock = Any()

private val internalHistoryDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
private val internalChartDateFormatter = SimpleDateFormat("dd/MM", Locale.getDefault())

/**
 * Thread-safe date formatting helpers for battery history and charts.
 * Prevents concurrency issues in SimpleDateFormat when called from multiple Composables or Coroutines.
 */
fun formatHistoryDate(timestamp: Long): String {
    return synchronized(historyDateFormatLock) {
        internalHistoryDateFormat.format(Date(timestamp))
    }
}

fun formatChartDate(timestamp: Long): String {
    return synchronized(chartDateFormatterLock) {
        internalChartDateFormatter.format(Date(timestamp))
    }
}

internal object historyDateFormat {
    fun format(date: Date): String = synchronized(historyDateFormatLock) {
        internalHistoryDateFormat.format(date)
    }
    fun format(timestamp: Long): String = formatHistoryDate(timestamp)
}

internal object chartDateFormatter {
    fun format(date: Date): String = synchronized(chartDateFormatterLock) {
        internalChartDateFormatter.format(date)
    }
    fun format(timestamp: Long): String = formatChartDate(timestamp)
}
