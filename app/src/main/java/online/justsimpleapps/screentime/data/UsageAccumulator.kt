package online.justsimpleapps.screentime.data

import java.time.Instant
import java.time.ZoneId

/**
 * Rebuilds screen time from the usage event stream.
 *
 * Android's own `totalTimeInForeground` keeps counting for whatever app was
 * last in front even while the screen is off — so background playback
 * (YouTube or music with the screen off) is billed as screen time. This
 * accumulator credits an app only while the screen is actually on and
 * unlocked: screen-off, keyguard and shutdown events close the running
 * interval, so a phone playing audio in the dark counts for zero.
 *
 * Pure Kotlin with no Android types, so the rules are unit-testable.
 * [UsageRepository] feeds it the device's usage events.
 */
class UsageAccumulator(
    /** Only time at or after this instant counts toward the day. */
    private val windowStart: Long,
    /** A pause in use at least this long starts a new session. */
    private val awayResetMs: Long = SessionEngine.AWAY_RESET_MS
) {

    private var screenInteractive = true
    private var keyguardUp = false
    private var front: String? = null
    private var spanStart = windowStart

    private var clusterStart = NO_CLUSTER
    private var lastUseEnd = 0L

    private val hours = FloatArray(24)
    private val perApp = HashMap<String, Long>()

    /** Credited screen time inside the window. */
    var totalMs = 0L
        private set

    /** Keyguard-hidden events inside the window. */
    var unlocks = 0
        private set

    /** Longest continuous stretch of use (brief gaps under [awayResetMs] are absorbed). */
    var maxSessionMs = 0L
        private set

    /** True while the screen is on, unlocked and showing something. */
    val isAccruing: Boolean
        get() = screenInteractive && !keyguardUp && front != null

    /** Credited time per package (unfiltered — the caller decides what to show). */
    val perAppMs: Map<String, Long>
        get() = perApp

    /** Hourly buckets in minutes, indexed 0–23 in the local timezone. */
    fun hourlyMinutes(): List<Float> = hours.toList()

    // ----- Event intake -------------------------------------------------
    // Each call first settles credited time up to [ts] under the *old*
    // state, then applies the state change. That way an interval always
    // ends exactly at the event that ended it.

    fun onForeground(pkg: String, ts: Long) {
        settle(ts)
        front = pkg
    }

    fun onBackground(pkg: String?, ts: Long) {
        settle(ts)
        if (pkg == null || pkg == front) front = null
    }

    fun onScreenInteractive(ts: Long) {
        settle(ts)
        screenInteractive = true
    }

    fun onScreenOff(ts: Long) {
        settle(ts)
        screenInteractive = false
    }

    fun onKeyguardShown(ts: Long) {
        settle(ts)
        keyguardUp = true
    }

    fun onKeyguardHidden(ts: Long) {
        settle(ts)
        keyguardUp = false
        if (ts >= windowStart) unlocks++
    }

    fun onShutdown(ts: Long) {
        settle(ts)
        screenInteractive = false
        front = null
    }

    /** Reboot: the screen state is unknown, so start clean and permissive. */
    fun onStartup(ts: Long) {
        settle(ts)
        screenInteractive = true
        keyguardUp = false
        front = null
    }

    /** Settle any open span up to [now] (the end of the query window). */
    fun finish(now: Long) {
        settle(now)
    }

    // ----- Accounting ---------------------------------------------------

    private fun settle(ts: Long) {
        val to = maxOf(ts, spanStart)
        val from = maxOf(spanStart, windowStart)
        val accrued = to - from
        if (isAccruing && accrued > 0L) {
            totalMs += accrued
            addHours(from, to)
            val pkg = front
            if (pkg != null) perApp[pkg] = (perApp[pkg] ?: 0L) + accrued
            if (clusterStart == NO_CLUSTER || from - lastUseEnd >= awayResetMs) {
                clusterStart = from
            }
            lastUseEnd = to
            maxSessionMs = maxOf(maxSessionMs, to - clusterStart)
        }
        spanStart = to
    }

    private fun addHours(from: Long, to: Long) {
        var cursor = from
        var guard = 0
        while (cursor < to && guard < 48) {
            guard++
            val zoned = Instant.ofEpochMilli(cursor).atZone(ZoneId.systemDefault())
            val nextHour = zoned.withMinute(0).withSecond(0).withNano(0).plusHours(1)
                .toInstant().toEpochMilli()
            val end = minOf(to, nextHour)
            hours[zoned.hour] += (end - cursor) / 60_000f
            cursor = end
        }
    }

    private companion object {
        /** "No session open". Never a real timestamp, so clusters may start at 0. */
        const val NO_CLUSTER = Long.MIN_VALUE
    }
}
