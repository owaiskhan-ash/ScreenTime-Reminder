package online.justsimpleapps.screentime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules that keep screen-off time out of the totals.
 *
 * The headline case: an app that keeps "running" (audio, downloads) while
 * the screen is off must not earn a single millisecond of screen time.
 */
class UsageAccumulatorTest {

    private val min = 60_000L

    @Test
    fun screenOffStopsCounting_backgroundPlaybackIsFree() {
        val acc = UsageAccumulator(windowStart = 0L)
        acc.onForeground("com.google.android.youtube", 0L)
        acc.onScreenOff(5 * min)   // locked the phone; YouTube keeps playing audio
        acc.finish(8 * 60 * min)   // eight hours later, nothing else happened

        assertEquals(5 * min, acc.totalMs)
        assertEquals(5 * min, acc.perAppMs["com.google.android.youtube"])
        assertFalse(acc.isAccruing)
    }

    @Test
    fun screenOnUseWithoutFurtherEventsStillCounts() {
        val acc = UsageAccumulator(windowStart = 0L)
        acc.onForeground("com.google.android.youtube", 0L)
        acc.finish(30 * min)       // one long, quiet video — no other events

        assertEquals(30 * min, acc.totalMs)
        assertTrue(acc.isAccruing)
    }

    @Test
    fun timeAfterAppGoesBackgroundIsNotCounted() {
        val acc = UsageAccumulator(windowStart = 0L)
        acc.onForeground("a", 0L)
        acc.onBackground("a", 4 * min)
        acc.finish(10 * min)

        assertEquals(4 * min, acc.totalMs)
        assertFalse(acc.isAccruing)
    }

    @Test
    fun lockscreenDoesNotCount() {
        val acc = UsageAccumulator(windowStart = 0L)
        acc.onScreenInteractive(0L)
        acc.onKeyguardShown(0L)                 // wake to the lockscreen
        acc.onForeground("com.android.systemui", 0L)
        acc.onKeyguardHidden(2 * min)           // unlocked
        acc.onForeground("a", 2 * min)
        acc.onScreenOff(5 * min)

        assertEquals(3 * min, acc.totalMs)      // two lockscreen minutes are free
        assertEquals(1, acc.unlocks)
    }

    @Test
    fun shutdownEndsEverything_startupStartsClean() {
        val acc = UsageAccumulator(windowStart = 0L)
        acc.onForeground("a", 0L)
        acc.onShutdown(7 * min)
        acc.onStartup(50 * min)                 // rebooted later
        acc.onForeground("a", 51 * min)
        acc.finish(60 * min)

        assertEquals(7 * min + 9 * min, acc.totalMs)
    }

    @Test
    fun appSwitchSplitsAttribution() {
        val acc = UsageAccumulator(windowStart = 0L)
        acc.onForeground("a", 0L)
        acc.onForeground("b", 10 * min)         // straight switch, no background event
        acc.onScreenOff(15 * min)

        assertEquals(10 * min, acc.perAppMs["a"])
        assertEquals(5 * min, acc.perAppMs["b"])
        assertEquals(15 * min, acc.totalMs)
    }

    @Test
    fun usageBeforeMidnightDoesNotLeakIntoToday() {
        val windowStart = 1_000 * min           // "midnight"
        val acc = UsageAccumulator(windowStart = windowStart)
        acc.onForeground("a", windowStart - min) // started just before midnight
        acc.finish(windowStart + 2 * min)

        assertEquals(2 * min, acc.totalMs)
    }

    @Test
    fun sessionCrossingMidnightIsNotCutShort() {
        val windowStart = 1_000 * min
        val acc = UsageAccumulator(windowStart = windowStart)
        acc.onScreenOff(windowStart - 6 * min)   // seeded state from before midnight
        acc.onScreenInteractive(windowStart - min)
        acc.onForeground("a", windowStart - min)
        acc.finish(windowStart + 9 * min)

        // Ten visible minutes, but the one before midnight stays yesterday's.
        assertEquals(9 * min, acc.totalMs)
    }

    @Test
    fun briefPauseKeepsSession_longPauseStartsANewOne() {
        val acc = UsageAccumulator(windowStart = 0L, awayResetMs = 2 * min)
        acc.onForeground("a", 0L)
        acc.onScreenOff(10 * min)
        acc.onScreenInteractive(10 * min + 30_000L)  // 30 s away — same session
        acc.onForeground("a", 10 * min + 30_000L)
        acc.onScreenOff(20 * min + 30_000L)

        assertEquals(20 * min, acc.totalMs)
        assertEquals(20 * min + 30_000L, acc.maxSessionMs) // the gap is absorbed

        acc.onScreenInteractive(30 * min)             // five-minute break — new session
        acc.onForeground("a", 30 * min)
        acc.onScreenOff(40 * min)

        // The second session is only ten minutes; it must not merge into
        // one giant forty-minute stretch across the break.
        assertEquals(20 * min + 30_000L, acc.maxSessionMs)
    }

    @Test
    fun hourlyBucketsSumToTheTotal() {
        val acc = UsageAccumulator(windowStart = 0L)
        acc.onForeground("a", 0L)
        acc.onScreenOff(45 * min)

        assertEquals(45.0f, acc.hourlyMinutes().sum(), 0.01f)
    }
}
