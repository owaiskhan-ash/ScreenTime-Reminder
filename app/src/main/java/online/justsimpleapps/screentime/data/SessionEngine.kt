package online.justsimpleapps.screentime.data

import online.justsimpleapps.screentime.util.minutesToMs

/**
 * The session engine decides when a screen session has gone on long enough
 * to deserve a nudge. It is shared by the in-app UI and the background
 * monitor, so both always agree on the same session.
 */
object SessionEngine {

    /** Two minutes away from the phone counts as a real break. */
    const val AWAY_RESET_MS = 2 * 60_000L

    @Synchronized
    fun tick(store: ScreenTimeStore, now: Long, active: Boolean): SessionState {
        val settings = store.settings
        var session = store.readSession()
        var changed = false

        // A session that is more than six hours stale belongs to a previous
        // run of the app (reboot, force stop). Do not count it as continuous.
        if (session.sessionStart > 0L && now - session.lastActive > 6 * 60 * 60_000L) {
            session = SessionState()
            changed = true
        }

        // Away for long enough: a real break happened.
        if (session.sessionStart > 0L && now - session.lastActive >= AWAY_RESET_MS) {
            store.recordSessionDuration(session.sessionStart, session.lastActive)
            store.incrementBreaks()
            session = session.copy(
                sessionStart = 0L,
                nextReminderAt = 0L,
                snoozeUntil = 0L,
                reminderFired = false
            )
            changed = true
        }

        if (active) {
            if (session.sessionStart == 0L) {
                session = session.copy(
                    sessionStart = now,
                    lastActive = now,
                    nextReminderAt = if (settings.remindersEnabled) now + minutesToMs(settings.sessionMinutes) else 0L,
                    snoozeUntil = 0L,
                    reminderFired = false
                )
                store.noteFirstUse(now)
                changed = true
            } else {
                // Persist the heartbeat every ten seconds, not every second.
                if (now - session.lastActive >= 10_000L) {
                    session = session.copy(lastActive = now)
                    changed = true
                }
                if (settings.remindersEnabled && !session.reminderFired && session.nextReminderAt == 0L) {
                    session = session.copy(
                        nextReminderAt = maxOf(now, session.sessionStart + minutesToMs(settings.sessionMinutes))
                    )
                    changed = true
                }
            }
        }

        if (changed) store.saveSession(session)
        return session
    }

    /** The person tapped "start break". Count it, and start fresh afterwards. */
    @Synchronized
    fun startBreak(store: ScreenTimeStore, now: Long) {
        val session = store.readSession()
        if (session.sessionStart > 0L) {
            store.recordSessionDuration(session.sessionStart, now)
            store.incrementBreaks()
        }
        store.saveSession(SessionState())
    }

    /** Notification action: nudge again in [minutes]. */
    @Synchronized
    fun snooze(store: ScreenTimeStore, now: Long, minutes: Int = 15) {
        val session = store.readSession()
        val until = now + minutesToMs(minutes)
        store.saveSession(
            session.copy(
                nextReminderAt = until,
                snoozeUntil = until,
                reminderFired = false
            )
        )
    }

    @Synchronized
    fun reset(store: ScreenTimeStore) {
        store.saveSession(SessionState())
    }

    /** Called by the monitor right after it posts a break notification. */
    @Synchronized
    fun markReminded(store: ScreenTimeStore, session: SessionState, now: Long): SessionState {
        val settings = store.settings
        val next = if (settings.repeatWhileOn && settings.remindersEnabled) {
            now + minutesToMs(settings.sessionMinutes)
        } else {
            0L
        }
        val updated = session.copy(nextReminderAt = next, reminderFired = true)
        store.saveSession(updated)
        return updated
    }
}
