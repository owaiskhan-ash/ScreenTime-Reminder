package online.justsimpleapps.screentime.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_LABEL = DateTimeFormatter.ofPattern("EEE, d MMMM", Locale.US)
private val SHORT_DAY = DateTimeFormatter.ofPattern("EEE", Locale.US)
private val CLOCK = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

fun todayKey(): String = LocalDate.now().toString()

fun dateLabel(date: LocalDate = LocalDate.now()): String =
    date.format(DATE_LABEL).uppercase(Locale.US)

fun shortDay(date: LocalDate): String =
    date.format(SHORT_DAY).uppercase(Locale.US)

fun clockLabel(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(CLOCK)

/** 2:14:36 — hours are not padded, good for a running total. */
fun formatHms(ms: Long): String {
    val total = ms.coerceAtLeast(0L) / 1000L
    val h = total / 3600L
    val m = (total % 3600L) / 60L
    val s = total % 60L
    return if (h > 0L) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}

/** 00:24:18 — padded, for countdowns. */
fun formatTimer(ms: Long): String {
    val total = ms.coerceAtLeast(0L) / 1000L
    val h = total / 3600L
    val m = (total % 3600L) / 60L
    val s = total % 60L
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

/** 2h 14m */
fun formatHm(ms: Long): String {
    val total = ms.coerceAtLeast(0L) / 60000L
    val h = total / 60L
    val m = total % 60L
    return when {
        h > 0L && m > 0L -> "${h}h ${m}m"
        h > 0L -> "${h}h"
        else -> "${m}m"
    }
}

/** 2:14 — compact, for chart labels. */
fun formatHmCompact(ms: Long): String {
    val total = ms.coerceAtLeast(0L) / 60000L
    return String.format(Locale.US, "%d:%02d", total / 60L, total % 60L)
}

fun minutesToMs(minutes: Int): Long = minutes * 60_000L

fun startOfTodayMs(): Long =
    LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
