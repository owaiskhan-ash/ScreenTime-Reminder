package online.justsimpleapps.screentime.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import online.justsimpleapps.screentime.service.ScreenTimeService
import online.justsimpleapps.screentime.util.hasUsageAccess
import online.justsimpleapps.screentime.util.minutesToMs
import online.justsimpleapps.screentime.util.notificationsAllowed
import online.justsimpleapps.screentime.util.todayKey
import java.time.LocalDate

/**
 * One small store for everything the app remembers: settings, today's
 * counters, and up to 30 days of daily totals. All of it stays in
 * SharedPreferences on this phone.
 */
class ScreenTimeStore private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var settings by mutableStateOf(loadSettings())
        private set
    var snapshot by mutableStateOf(LiveSnapshot())
        private set
    var accessGranted by mutableStateOf(hasUsageAccess(appContext))
        private set
    var onboardingDone by mutableStateOf(prefs.getBoolean(KEY_ONBOARDING_DONE, false))
        private set
    var notificationAsked by mutableStateOf(prefs.getBoolean(KEY_NOTIF_ASKED, false))
        private set
    var history by mutableStateOf(loadHistory())
        private set
    var lastResetAt by mutableStateOf(prefs.getLong(KEY_LAST_RESET, 0L))
        private set
    var breakScreenRequest by mutableStateOf(false)
        private set

    private var lastTotalWrite = 0L
    private var lastArchivedDay = ""

    companion object {
        private const val PREFS = "screen_time_reminder"

        private const val KEY_THEME = "theme_mode"
        private const val KEY_REMINDERS = "reminders_enabled"
        private const val KEY_SESSION_MINUTES = "session_minutes"
        private const val KEY_REPEAT = "repeat_while_on"
        private const val KEY_DAILY_LIMIT = "daily_limit_minutes"
        private const val KEY_BACKGROUND = "background_monitoring"
        private const val KEY_VIBRATE = "vibrate"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_NOTIF_ASKED = "notifications_asked"
        private const val KEY_DAY_DATE = "day_date"
        private const val KEY_DAY_BREAKS = "day_breaks"
        private const val KEY_DAY_TOTAL = "day_total"
        private const val KEY_DAY_MAX_SESSION = "day_max_session"
        private const val KEY_DAY_LIMIT_NOTIFIED = "day_limit_notified"
        private const val KEY_DAY_FIRST_USE = "day_first_use"
        private const val KEY_SESSION_START = "session_start"
        private const val KEY_SESSION_LAST = "session_last_active"
        private const val KEY_NEXT_REMINDER = "session_next_reminder"
        private const val KEY_SNOOZE = "session_snooze"
        private const val KEY_REMINDER_FIRED = "session_reminder_fired"
        private const val KEY_HISTORY = "history"
        private const val KEY_LAST_RESET = "last_reset"

        @Volatile
        private var instance: ScreenTimeStore? = null

        fun get(context: Context): ScreenTimeStore =
            instance ?: synchronized(this) {
                instance ?: ScreenTimeStore(context.applicationContext).also { instance = it }
            }
    }

    // ----- Settings -----------------------------------------------------

    private fun loadSettings(): Settings {
        val theme = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
        return Settings(
            themeMode = theme,
            remindersEnabled = prefs.getBoolean(KEY_REMINDERS, true),
            sessionMinutes = prefs.getInt(KEY_SESSION_MINUTES, 45),
            repeatWhileOn = prefs.getBoolean(KEY_REPEAT, true),
            dailyLimitMinutes = prefs.getInt(KEY_DAILY_LIMIT, 300),
            backgroundMonitoring = prefs.getBoolean(KEY_BACKGROUND, true),
            vibrate = prefs.getBoolean(KEY_VIBRATE, true)
        )
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        val old = settings
        val next = transform(old)
        if (next == old) return
        settings = next
        prefs.edit()
            .putString(KEY_THEME, next.themeMode.name)
            .putBoolean(KEY_REMINDERS, next.remindersEnabled)
            .putInt(KEY_SESSION_MINUTES, next.sessionMinutes)
            .putBoolean(KEY_REPEAT, next.repeatWhileOn)
            .putInt(KEY_DAILY_LIMIT, next.dailyLimitMinutes)
            .putBoolean(KEY_BACKGROUND, next.backgroundMonitoring)
            .putBoolean(KEY_VIBRATE, next.vibrate)
            .apply()

        if (old.sessionMinutes != next.sessionMinutes || old.remindersEnabled != next.remindersEnabled) {
            val session = readSession()
            if (session.sessionStart > 0L) {
                val now = System.currentTimeMillis()
                val nextAt = if (next.remindersEnabled) {
                    maxOf(now, session.sessionStart + minutesToMs(next.sessionMinutes))
                } else {
                    0L
                }
                saveSession(
                    session.copy(nextReminderAt = nextAt, reminderFired = false, snoozeUntil = 0L)
                )
            }
        }
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
        onboardingDone = true
    }

    fun markNotificationAsked() {
        prefs.edit().putBoolean(KEY_NOTIF_ASKED, true).apply()
        notificationAsked = true
    }

    fun requestBreakScreen() {
        breakScreenRequest = true
    }

    fun consumeBreakScreenRequest() {
        breakScreenRequest = false
    }

    // ----- Session ------------------------------------------------------

    fun readSession(): SessionState = SessionState(
        sessionStart = prefs.getLong(KEY_SESSION_START, 0L),
        lastActive = prefs.getLong(KEY_SESSION_LAST, 0L),
        nextReminderAt = prefs.getLong(KEY_NEXT_REMINDER, 0L),
        snoozeUntil = prefs.getLong(KEY_SNOOZE, 0L),
        reminderFired = prefs.getBoolean(KEY_REMINDER_FIRED, false)
    )

    fun saveSession(session: SessionState) {
        prefs.edit()
            .putLong(KEY_SESSION_START, session.sessionStart)
            .putLong(KEY_SESSION_LAST, session.lastActive)
            .putLong(KEY_NEXT_REMINDER, session.nextReminderAt)
            .putLong(KEY_SNOOZE, session.snoozeUntil)
            .putBoolean(KEY_REMINDER_FIRED, session.reminderFired)
            .apply()
    }

    fun noteFirstUse(now: Long) {
        if (prefs.getLong(KEY_DAY_FIRST_USE, 0L) == 0L) {
            prefs.edit().putLong(KEY_DAY_FIRST_USE, now).apply()
        }
    }

    fun recordSessionDuration(start: Long, end: Long) {
        if (end <= start) return
        val current = prefs.getLong(KEY_DAY_MAX_SESSION, 0L)
        val duration = end - start
        if (duration > current) {
            prefs.edit().putLong(KEY_DAY_MAX_SESSION, duration).apply()
        }
    }

    fun incrementBreaks(): Int {
        ensureDay(System.currentTimeMillis())
        val next = prefs.getInt(KEY_DAY_BREAKS, 0) + 1
        prefs.edit().putInt(KEY_DAY_BREAKS, next).apply()
        return next
    }

    fun todayBreaks(): Int {
        ensureDay(System.currentTimeMillis())
        return prefs.getInt(KEY_DAY_BREAKS, 0)
    }

    fun todayMaxSession(): Long = prefs.getLong(KEY_DAY_MAX_SESSION, 0L)

    fun limitNotifiedToday(): Boolean {
        ensureDay(System.currentTimeMillis())
        return prefs.getBoolean(KEY_DAY_LIMIT_NOTIFIED, false)
    }

    fun markLimitNotified() {
        prefs.edit().putBoolean(KEY_DAY_LIMIT_NOTIFIED, true).apply()
    }

    fun recordTodayTotal(now: Long, totalMs: Long) {
        if (now - lastTotalWrite < 15_000L) return
        lastTotalWrite = now
        prefs.edit().putLong(KEY_DAY_TOTAL, totalMs).apply()
    }

    // ----- Refresh ------------------------------------------------------

    fun refresh(now: Long = System.currentTimeMillis(), tickSession: Boolean = true) {
        ensureDay(now)
        if (tickSession) SessionEngine.tick(this, now, active = true)

        accessGranted = hasUsageAccess(appContext)
        val allowed = notificationsAllowed(appContext)
        val usage = if (accessGranted) {
            UsageRepository.get(appContext).snapshot(now)
        } else {
            UsageData()
        }
        val session = readSession()
        val continuous = if (session.sessionStart > 0L) {
            (now - session.sessionStart).coerceAtLeast(0L)
        } else {
            0L
        }
        val total = if (accessGranted) maxOf(usage.todayMs, 0L) else continuous
        recordTodayTotal(now, total)
        history = loadHistory()

        val s = settings
        val limitMs = minutesToMs(s.sessionMinutes)
        val dailyLimitMs = if (s.dailyLimitMinutes > 0) minutesToMs(s.dailyLimitMinutes) else 0L
        val sessionActive = session.sessionStart > 0L
        val due = s.remindersEnabled && sessionActive && (
            (session.nextReminderAt > 0L && session.nextReminderAt <= now) ||
                (session.nextReminderAt == 0L && session.reminderFired)
            )
        val nextIn = when {
            !s.remindersEnabled || !sessionActive -> 0L
            session.nextReminderAt > now -> session.nextReminderAt - now
            else -> 0L
        }
        val progress = if (limitMs > 0L) (continuous.toFloat() / limitMs).coerceIn(0f, 1f) else 0f
        val dailyProgress = if (dailyLimitMs > 0L) (total.toFloat() / dailyLimitMs).coerceIn(0f, 1f) else 0f
        val firstUse = when {
            usage.firstUseMs > 0L -> usage.firstUseMs
            session.sessionStart > 0L -> session.sessionStart
            else -> prefs.getLong(KEY_DAY_FIRST_USE, 0L)
        }

        snapshot = LiveSnapshot(
            now = now,
            todayMs = total,
            continuousMs = continuous,
            nextBreakInMs = nextIn,
            breakDue = due,
            sessionLimitMs = limitMs,
            sessionProgress = progress,
            dailyLimitMs = dailyLimitMs,
            dailyProgress = dailyProgress,
            overDailyLimit = dailyLimitMs > 0L && total >= dailyLimitMs,
            remindersEnabled = s.remindersEnabled,
            snoozedUntilMs = if (session.snoozeUntil > now) session.snoozeUntil else 0L,
            unlocks = usage.unlocks,
            breaks = todayBreaks(),
            firstUseMs = firstUse,
            hourlyMinutes = usage.hourly,
            topApps = usage.topApps,
            limitedMode = !accessGranted,
            monitoring = ScreenTimeService.isRunning,
            accessGranted = accessGranted,
            notificationsAllowed = allowed,
            maxSessionMs = maxOf(usage.maxSessionMs, todayMaxSession())
        )
    }

    // ----- Day rollover & history --------------------------------------

    private fun ensureDay(now: Long) {
        val today = todayKey()
        val stored = prefs.getString(KEY_DAY_DATE, null)
        if (stored == today) return
        val total = prefs.getLong(KEY_DAY_TOTAL, 0L)
        if (!stored.isNullOrEmpty() && total / 60_000L > 0L && stored != lastArchivedDay) {
            archive(stored, total / 60_000L, prefs.getInt(KEY_DAY_BREAKS, 0))
            lastArchivedDay = stored
        }
        prefs.edit()
            .putString(KEY_DAY_DATE, today)
            .putInt(KEY_DAY_BREAKS, 0)
            .putLong(KEY_DAY_TOTAL, 0L)
            .putLong(KEY_DAY_MAX_SESSION, 0L)
            .putBoolean(KEY_DAY_LIMIT_NOTIFIED, false)
            .putLong(KEY_DAY_FIRST_USE, 0L)
            .apply()
        saveSession(SessionState())
        lastTotalWrite = 0L
        history = loadHistory()
    }

    private fun archive(date: String, minutes: Long, breaks: Int) {
        val entries = loadHistoryEntries().toMutableMap()
        entries[date] = HistoryEntry(minutes, breaks)
        val cutoff = LocalDate.now().minusDays(29).toString()
        val trimmed = entries.filterKeys { it >= cutoff }
        writeHistory(trimmed)
    }

    private fun loadHistoryEntries(): Map<String, HistoryEntry> {
        val raw = prefs.getString(KEY_HISTORY, "") ?: return emptyMap()
        if (raw.isBlank()) return emptyMap()
        val result = HashMap<String, HistoryEntry>()
        raw.split(';').forEach { part ->
            if (part.isBlank()) return@forEach
            val fields = part.split(':')
            if (fields.size >= 2) {
                val date = fields[0]
                val minutes = fields[1].toLongOrNull() ?: 0L
                val breaks = fields.getOrNull(2)?.toIntOrNull() ?: 0
                result[date] = HistoryEntry(minutes, breaks)
            }
        }
        return result
    }

    private fun loadHistory(): Map<String, HistoryEntry> = loadHistoryEntries()

    private fun writeHistory(entries: Map<String, HistoryEntry>) {
        val raw = entries.entries
            .sortedBy { it.key }
            .joinToString(";") { "${it.key}:${it.value.minutes}:${it.value.breaks}" }
        prefs.edit().putString(KEY_HISTORY, raw).apply()
        history = entries
    }

    fun weekSummary(todayMs: Long, todayBreaks: Int): WeekSummary {
        val entries = loadHistory()
        val today = LocalDate.now()
        val points = (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val key = date.toString()
            val isToday = offset == 0
            val entry = entries[key]
            val tracked = isToday || entry != null
            val ms = if (isToday) {
                maxOf(todayMs, (entry?.minutes ?: 0L) * 60_000L)
            } else {
                (entry?.minutes ?: 0L) * 60_000L
            }
            DayPoint(date = date, ms = ms, isToday = isToday, tracked = tracked)
        }
        val trackedPoints = points.filter { it.tracked }
        val average = if (trackedPoints.isEmpty()) 0L else trackedPoints.sumOf { it.ms } / trackedPoints.size
        val lightest = trackedPoints.filter { it.tracked }.minByOrNull { it.ms }
        val breaks = points.sumOf { point ->
            if (point.isToday) todayBreaks else entries[point.date.toString()]?.breaks ?: 0
        }
        return WeekSummary(
            points = points,
            averageMs = average,
            lightest = lightest,
            trackedDays = trackedPoints.size,
            breaks = breaks
        )
    }

    // ----- Reset --------------------------------------------------------

    fun resetToday(now: Long = System.currentTimeMillis()) {
        SessionEngine.reset(this)
        prefs.edit()
            .putInt(KEY_DAY_BREAKS, 0)
            .putLong(KEY_DAY_TOTAL, 0L)
            .putLong(KEY_DAY_MAX_SESSION, 0L)
            .putBoolean(KEY_DAY_LIMIT_NOTIFIED, false)
            .putLong(KEY_DAY_FIRST_USE, 0L)
            .putLong(KEY_LAST_RESET, now)
            .apply()
        lastResetAt = now
        lastTotalWrite = 0L
        UsageRepository.get(appContext).invalidate()
        refresh(now, tickSession = false)
    }

    fun appContext(): Context = appContext
}

data class HistoryEntry(val minutes: Long, val breaks: Int)
