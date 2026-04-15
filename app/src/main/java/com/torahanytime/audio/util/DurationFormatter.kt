package com.torahanytime.audio.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%d:%02d", m, s)
}

fun formatDurationLong(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

fun formatRelativeDate(dateString: String?): String? {
    if (dateString.isNullOrBlank()) return null
    val date = parseDate(dateString) ?: return null
    val now = System.currentTimeMillis()
    val diffMs = now - date.time
    val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)

    return when {
        diffDays < 0 -> null
        diffDays == 0L -> "Today"
        diffDays == 1L -> "Yesterday"
        diffDays < 7 -> "$diffDays days ago"
        diffDays < 30 -> {
            val weeks = diffDays / 7
            if (weeks == 1L) "1 week ago" else "$weeks weeks ago"
        }
        else -> {
            val cal = Calendar.getInstance().apply { time = date }
            val nowCal = Calendar.getInstance()
            if (cal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)) {
                SimpleDateFormat("MMM d", Locale.US).format(date)
            } else {
                SimpleDateFormat("MMM yyyy", Locale.US).format(date)
            }
        }
    }
}

private fun parseDate(dateString: String): Date? {
    val formats = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd"
    )
    for (fmt in formats) {
        try {
            return SimpleDateFormat(fmt, Locale.US).parse(dateString)
        } catch (_: Exception) {}
    }
    return null
}
