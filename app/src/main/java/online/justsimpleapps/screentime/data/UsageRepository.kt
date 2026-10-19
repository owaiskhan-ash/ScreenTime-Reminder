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

data class Aggregate(
    val totalMs: Long = 0L,
    val topApps: List<AppUsage> = emptyList(),
    val at: Long = 0L
)

data class Timeline(
    val hourly: List<Float> = List(24) { 0f },
    val unlocks: Int = 0,
    val firstUseMs: Long = 0L,
    val maxSessionMs: Long = 0L,
    val lastActivityAt: Long = 0L,
    val activeNow: Boolean = false
)

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
 * Screen time comes from UsageStatsManager: the same numbers Android's
 * Digital Wellbeing uses. We never read content, messages or app data —
 * only which app was in front, and for how long.
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
    private var aggregate: Aggregate = Aggregate()
    private var aggregateAt = 0L
    private var timeline: Timeline = Timeline()
    private var timelineAt = 0L

    private val aggregateTtl = 15_000L
    private val timelineTtl = 20_000L

    companion object {
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
            aggregateAt = 0L
            timelineAt = 0L
        }
    }

    fun snapshot(now: Long): UsageData = synchronized(lock) {
        val day = LocalDay.key(now)
        if (day != cacheDay) {
            cacheDay = day
            aggregate = Aggregate()
            aggregateAt = 0L
            timeline = Timeline()
            timelineAt = 0L
        }
        expireIfNeeded(now)
        val total = aggregate.totalMs + liveDelta(now)
        UsageData(
            todayMs = total.coerceIn(0L, (now - startOfTodayMs()).coerceAtLeast(0L)),
            hourly = timeline.hourly,
            topApps = aggregate.topApps,
            unlocks = timeline.unlocks,
            firstUseMs = timeline.firstUseMs,
            maxSessionMs = timeline.maxSessionMs,
            activeNow = timeline.activeNow || isScreenOn()
        )
    }

    fun todayMs(now: Long): Long = synchronized(lock) {
        expireIfNeeded(now)
        (aggregate.totalMs + liveDelta(now)).coerceAtLeast(0L)
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
                    if (!isNoise(pkg, forTotals = false)) last = pkg
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
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
        if (now - aggregateAt > aggregateTtl || aggregateAt == 0L) {
            aggregate = queryAggregate(now)
            aggregateAt = now
        }
        if (now - timelineAt > timelineTtl || timelineAt == 0L) {
            timeline = queryTimeline(now)
            timelineAt = now
        }
    }

    /** Between the 15-second aggregate polls, screen-on time keeps ticking. */
    private fun liveDelta(now: Long): Long {
        if (aggregateAt == 0L) return 0L
        val elapsed = now - aggregateAt
        if (elapsed <= 0L || elapsed > 120_000L) return 0L
        if (!isScreenOn()) return 0L
        return elapsed
    }

    private fun queryAggregate(now: Long): Aggregate {
        val manager = usm ?: return Aggregate(at = now)
        val start = startOfTodayMs()
        val stats = runCatching { manager.queryAndAggregateUsageStats(start, now) }
            .getOrNull()
            ?: return Aggregate(at = now)

        var total = 0L
        val ranked = ArrayList<Pair<String, Long>>()
        for ((pkg, value) in stats) {
            val ms = value.totalTimeInForeground
            if (ms <= 0L) continue
            if (isNoise(pkg, forTotals = true)) continue
            total += ms
            ranked += pkg to ms
        }
        total = total.coerceAtMost((now - start).coerceAtLeast(0L))
        val top = ranked
            .sortedByDescending { it.second }
            .take(5)
            .map { (pkg, ms) -> AppUsage(pkg, labelFor(pkg), ms) }
        return Aggregate(totalMs = total, topApps = top, at = now)
    }

    private fun queryTimeline(now: Long): Timeline {
        val manager = usm ?: return Timeline()
        val start = startOfTodayMs()
        val events = runCatching { manager.queryEvents(start, now) }.getOrNull()
            ?: return Timeline()
        val hourly = FloatArray(24)
        var unlocks = 0
        var firstUse = 0L
        var maxSession = 0L
        var sessionStart = 0L
        var lastActive = 0L
        var lastPkg: String? = null
        var lastTs = start
        val event = UsageEvents.Event()

        fun closeInterval(until: Long) {
            if (lastPkg == null) return
            if (until > lastTs) addToHours(hourly, lastTs, until)
            if (sessionStart > 0L) maxSession = maxOf(maxSession, until - sessionStart)
            lastPkg = null
            lastTs = until
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val ts = event.timeStamp
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    val pkg = event.packageName ?: continue
                    if (lastPkg == null) {
                        lastPkg = pkg
                        lastTs = ts
                        if (firstUse == 0L && !isNoise(pkg, forTotals = true)) firstUse = ts
                        if (sessionStart == 0L || ts - lastActive > SessionEngine.AWAY_RESET_MS) {
                            sessionStart = ts
                        }
                    } else if (lastPkg != pkg) {
                        closeInterval(ts)
                        lastPkg = pkg
                        lastTs = ts
                    }
                    lastActive = ts
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (event.packageName == lastPkg) closeInterval(ts)
                    lastActive = ts
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.KEYGUARD_SHOWN,
                UsageEvents.Event.DEVICE_SHUTDOWN -> {
                    closeInterval(ts)
                    lastTs = ts
                }
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    lastTs = ts
                }
                UsageEvents.Event.KEYGUARD_HIDDEN -> {
                    unlocks++
                    lastTs = ts
                    lastActive = ts
                }
            }
        }
        if (lastPkg != null) {
            if (now > lastTs) addToHours(hourly, lastTs, now)
            if (sessionStart > 0L) maxSession = maxOf(maxSession, now - sessionStart)
        }
        return Timeline(
            hourly = hourly.toList(),
            unlocks = unlocks,
            firstUseMs = firstUse,
            maxSessionMs = maxSession,
            lastActivityAt = lastActive,
            activeNow = lastPkg != null
        )
    }

    private fun addToHours(hourly: FloatArray, from: Long, to: Long) {
        if (to <= from) return
        var cursor = from
        var guard = 0
        while (cursor < to && guard < 48) {
            guard++
            val zoned = Instant.ofEpochMilli(cursor).atZone(ZoneId.systemDefault())
            val nextHour = zoned.withMinute(0).withSecond(0).withNano(0).plusHours(1)
                .toInstant().toEpochMilli()
            val end = minOf(to, nextHour)
            hourly[zoned.hour] += (end - cursor) / 60_000f
            cursor = end
        }
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

    private fun isNoise(pkg: String, forTotals: Boolean): Boolean {
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
        if (pkg in excluded) return true
        // The monitor still counts our own app as "in use": the user is looking at it.
        return forTotals && pkg == context.packageName
    }
}

private object LocalDay {
    fun key(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate().toString()
}
