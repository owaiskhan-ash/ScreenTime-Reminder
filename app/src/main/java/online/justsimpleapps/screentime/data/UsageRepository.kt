package online.justsimpleapps.screentime.data

import android.app.KeyguardManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import online.justsimpleapps.screentime.util.startOfTodayMs
import java.time.Instant
import java.time.ZoneId

data class UsageData(
    val todayMs: Long = 0L,
    val hourly: List<Float> = List(24) { 0f },
    val topApps: List<AppUsage> = emptyList(),
    val unlocks: Int = 0,
    val firstUseMs: Long = 0L,
    val maxSessionMs: Long = 0L,
    val activeNow: Boolean = false
)

/**
 * Reads the phone's own screen-time record. Nothing leaves the device.
 *
 * The day is rebuilt from the usage *event* stream — the same source
 * Android's Digital Wellbeing uses — and an app is credited only while the
 * screen is actually on and unlocked. Time with the screen off (background
 * playback, notifications arriving in the dark) counts for zero. We never
 * read content, messages or app data — only which app was in front, and
 * for how long the screen was on.
 */
class UsageRepository private constructor(private val context: Context) {

    private val lock = Any()
    private val usm: UsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private val power: PowerManager? =
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val keyguard: KeyguardManager? =
        context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

    private var excluded: Set<String> = emptySet()
    private var excludedComputed = false
    private val labels = HashMap<String, String>()

    private var cacheDay: String = ""
    private var cached: Cached = Cached()
    private val cacheTtl = 15_000L

    private class Cached(
        val data: UsageData = UsageData(),
        val accruingNow: Boolean = false,
        val at: Long = 0L
    )

    companion object {
        /**
         * How far before midnight the event walk starts, so a session that
         * crosses midnight keeps its state (screen off? which app in front?)
         * instead of starting the day blind.
         */
        private const val SEED_WINDOW_MS = 12 * 60 * 60_000L

        @Volatile
        private var instance: UsageRepository? = null

        fun get(context: Context): UsageRepository =
            instance ?: synchronized(this) {
                instance ?: UsageRepository(context.applicationContext).also { instance = it }
            }
    }

    fun isScreenOn(): Boolean = power?.isInteractive == true && keyguard?.isKeyguardLocked != true

    /** Forget cached numbers so the next read is fresh. */
    fun invalidate() {
        synchronized(lock) {
            cacheDay = ""
            cached = Cached()
        }
    }

    fun snapshot(now: Long): UsageData = synchronized(lock) {
        expireIfNeeded(now)
        val c = cached
        val total = (c.data.todayMs + liveDelta(now))
            .coerceIn(0L, (now - startOfTodayMs()).coerceAtLeast(0L))
        c.data.copy(todayMs = total)
    }

    fun todayMs(now: Long): Long = synchronized(lock) {
        expireIfNeeded(now)
        (cached.data.todayMs + liveDelta(now)).coerceAtLeast(0L)
    }

    /** Which app is in front right now? Used by the monitor. */
    fun foregroundPackage(now: Long): String? {
        val manager = usm ?: return null
        if (!isScreenOn()) return null
        val events = runCatching { manager.queryEvents(now - 10 * 60_000L, now) }.getOrNull() ?: return null
        val event = UsageEvents.Event()
        var last: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    val pkg = event.packageName ?: continue
                    last = pkg
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    if (event.packageName == last) last = null
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.KEYGUARD_SHOWN,
                UsageEvents.Event.DEVICE_SHUTDOWN -> last = null
            }
        }
        return last
    }

    private fun expireIfNeeded(now: Long) {
        val day = LocalDay.key(now)
        if (day != cacheDay) {
            cacheDay = day
            cached = Cached()
        }
        val c = cached
        if (c.at == 0L || now - c.at > cacheTtl) {
            cached = queryUsage(now)
        }
    }

    /** Between the 15-second polls, screen-on time keeps ticking. */
    private fun liveDelta(now: Long): Long {
        val c = cached
        if (c.at == 0L) return 0L
        val elapsed = now - c.at
        if (elapsed <= 0L || elapsed > 120_000L) return 0L
        if (!c.accruingNow || !isScreenOn()) return 0L
        return elapsed
    }

    private fun queryUsage(now: Long): Cached {
        val manager = usm ?: return Cached(at = now)
        val start = startOfTodayMs()
        val events = runCatching { manager.queryEvents(start - SEED_WINDOW_MS, now) }.getOrNull()
            ?: return fallbackUsage(manager, start, now)

        val acc = UsageAccumulator(windowStart = start)
        var firstUse = 0L
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val ts = event.timeStamp
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    val pkg = event.packageName ?: continue
                    if (firstUse == 0L && ts >= start && !isNoise(pkg)) firstUse = ts
                    acc.onForeground(pkg, ts)
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> acc.onBackground(event.packageName, ts)
                UsageEvents.Event.ACTIVITY_STOPPED -> acc.onBackground(event.packageName, ts)
                UsageEvents.Event.SCREEN_INTERACTIVE -> acc.onScreenInteractive(ts)
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> acc.onScreenOff(ts)
                UsageEvents.Event.KEYGUARD_SHOWN -> acc.onKeyguardShown(ts)
                UsageEvents.Event.KEYGUARD_HIDDEN -> acc.onKeyguardHidden(ts)
                UsageEvents.Event.DEVICE_SHUTDOWN -> acc.onShutdown(ts)
                UsageEvents.Event.DEVICE_STARTUP -> acc.onStartup(ts)
            }
        }
        acc.finish(now)

        val top = acc.perAppMs.asSequence()
            .filterNot { isNoise(it.key) }
            .sortedByDescending { it.value }
            .take(5)
            .map { (pkg, ms) -> AppUsage(pkg, labelFor(pkg), ms) }
            .toList()

        val data = UsageData(
            todayMs = acc.totalMs,
            hourly = acc.hourlyMinutes(),
            topApps = top,
            unlocks = acc.unlocks,
            firstUseMs = firstUse,
            maxSessionMs = acc.maxSessionMs,
            activeNow = acc.isAccruing
        )
        return Cached(data = data, accruingNow = acc.isAccruing, at = now)
    }

    /**
     * Only used when the event stream is unavailable: the raw per-app
     * totals from UsageStatsManager. These can include screen-off time,
     * but a roughly right number beats a blank screen.
     */
    private fun fallbackUsage(manager: UsageStatsManager, start: Long, now: Long): Cached {
        val stats = runCatching { manager.queryAndAggregateUsageStats(start, now) }
            .getOrNull()
            ?: return Cached(at = now)
        var total = 0L
        val ranked = ArrayList<Pair<String, Long>>()
        for ((pkg, value) in stats) {
            val ms = value.totalTimeInForeground
            if (ms <= 0L) continue
            if (isNoise(pkg)) continue
            total += ms
            ranked += pkg to ms
        }
        total = total.coerceAtMost((now - start).coerceAtLeast(0L))
        val top = ranked
            .sortedByDescending { it.second }
            .take(5)
            .map { (pkg, ms) -> AppUsage(pkg, labelFor(pkg), ms) }
        return Cached(
            data = UsageData(todayMs = total, topApps = top),
            accruingNow = false,
            at = now
        )
    }

    private fun labelFor(pkg: String): String {
        labels[pkg]?.let { return it }
        val label = runCatching {
            val info = context.packageManager.getApplicationInfo(pkg, 0)
            context.packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(pkg.substringAfterLast('.'))
        labels[pkg] = label
        return label
    }

    /** System chrome and home screens: never listed as an app "you used". */
    private fun isNoise(pkg: String): Boolean {
        if (!excludedComputed) {
            val set = HashSet<String>()
            set += "com.android.systemui"
            set += "android"
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            runCatching {
                context.packageManager.queryIntentActivities(home, 0).forEach {
                    it.activityInfo?.packageName?.let(set::add)
                }
            }
            excluded = set
            excludedComputed = true
        }
        return pkg in excluded
    }
}

private object LocalDay {
    fun key(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate().toString()
}
