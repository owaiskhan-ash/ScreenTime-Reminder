package online.justsimpleapps.screentime.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate

enum class ThemeMode { LIGHT, DARK, SYSTEM }

data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val remindersEnabled: Boolean = true,
    val sessionMinutes: Int = 45,
    val repeatWhileOn: Boolean = true,
    val dailyLimitMinutes: Int = 300,
    val backgroundMonitoring: Boolean = true,
    val vibrate: Boolean = true
)

data class AppUsage(
    val packageName: String,
    val label: String,
    val foregroundMs: Long
)

data class LiveSnapshot(
    val now: Long = 0L,
    val todayMs: Long = 0L,
    val continuousMs: Long = 0L,
    val nextBreakInMs: Long = 0L,
    val breakDue: Boolean = false,
    val sessionLimitMs: Long = 45 * 60_000L,
    val sessionProgress: Float = 0f,
    val dailyLimitMs: Long = 300 * 60_000L,
    val dailyProgress: Float = 0f,
    val overDailyLimit: Boolean = false,
    val remindersEnabled: Boolean = true,
    val snoozedUntilMs: Long = 0L,
    val unlocks: Int = 0,
    val breaks: Int = 0,
    val firstUseMs: Long = 0L,
    val hourlyMinutes: List<Float> = List(24) { 0f },
    val topApps: List<AppUsage> = emptyList(),
    val limitedMode: Boolean = false,
    val monitoring: Boolean = false,
    val accessGranted: Boolean = false,
    val notificationsAllowed: Boolean = true,
    val maxSessionMs: Long = 0L
)

data class SessionState(
    val sessionStart: Long = 0L,
    val lastActive: Long = 0L,
    val nextReminderAt: Long = 0L,
    val snoozeUntil: Long = 0L,
    val reminderFired: Boolean = false
)

data class DayPoint(
    val date: LocalDate,
    val ms: Long,
    val isToday: Boolean,
    val tracked: Boolean
)

/** Values used by the "insight" row on the week screen. */
data class WeekSummary(
    val points: List<DayPoint> = emptyList(),
    val averageMs: Long = 0L,
    val lightest: DayPoint? = null,
    val trackedDays: Int = 0,
    val breaks: Int = 0
)

class SettingsHolder {
    var settings by mutableStateOf(Settings())
    var loaded by mutableStateOf(false)
}
