package online.justsimpleapps.screentime.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import online.justsimpleapps.screentime.data.ScreenTimeStore
import online.justsimpleapps.screentime.data.SessionEngine
import online.justsimpleapps.screentime.data.UsageRepository
import online.justsimpleapps.screentime.notifications.Notifiers
import online.justsimpleapps.screentime.widget.ScreenTimeWidgetProvider
import online.justsimpleapps.screentime.util.formatHm
import online.justsimpleapps.screentime.util.hasUsageAccess
import online.justsimpleapps.screentime.util.minutesToMs

/**
 * Watches the phone's screen state while the app is in the background.
 * One lightweight check every twenty seconds is all it takes; the numbers
 * themselves come from Android's own usage record.
 */
class ScreenTimeService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var store: ScreenTimeStore
    private var loop: Job? = null
    private var lastMonitorUpdate = 0L

    override fun onCreate() {
        super.onCreate()
        store = ScreenTimeStore.get(this)
        isRunning = true
        Notifiers.ensureChannels(this)
        val text = Notifiers.monitorText(
            todayMs = store.snapshot.todayMs,
            remindersEnabled = store.settings.remindersEnabled,
            sessionActive = false,
            nextReminderAt = 0L,
            now = System.currentTimeMillis()
        )
        val notification = Notifiers.buildMonitor(this, "Screen time reminder", text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(Notifiers.ID_MONITOR, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(Notifiers.ID_MONITOR, notification)
        }
        loop = scope.launch {
            while (isActive) {
                runCatching { evaluate() }
                delay(20_000L)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        loop?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun evaluate() {
        val now = System.currentTimeMillis()
        val granted = hasUsageAccess(this)
        val repo = UsageRepository.get(this)
        val screenOn = repo.isScreenOn()
        val foreground = if (granted && screenOn) repo.foregroundPackage(now) else null
        val active = granted && screenOn && foreground != null

        val session = SessionEngine.tick(store, now, active)
        val total = if (granted) repo.todayMs(now) else 0L
        store.recordTodayTotal(now, total)

        val settings = store.settings
        if (settings.remindersEnabled) {
            val due = session.sessionStart > 0L &&
                session.nextReminderAt > 0L &&
                session.nextReminderAt <= now
            if (active && due) {
                Notifiers.postBreak(this, settings, now - session.sessionStart)
                SessionEngine.markReminded(store, session, now)
            }
            val limit = settings.dailyLimitMinutes
            if (limit > 0 && total >= minutesToMs(limit) && !store.limitNotifiedToday()) {
                Notifiers.postDailyLimit(this, settings, total)
                store.markLimitNotified()
            }
        }

        if (now - lastMonitorUpdate >= 30_000L) {
            lastMonitorUpdate = now
            val text = Notifiers.monitorText(
                todayMs = total,
                remindersEnabled = settings.remindersEnabled,
                sessionActive = session.sessionStart > 0L,
                nextReminderAt = session.nextReminderAt,
                now = now
            )
            val notification = Notifiers.buildMonitor(this, "Screen time reminder", text)
            runCatching {
                NotificationManagerCompat.from(this).notify(Notifiers.ID_MONITOR, notification)
            }
        }

        // Keep any home-screen widget in step with the same numbers the
        // notification shows. No-op and self-throttling when none is placed.
        runCatching { ScreenTimeWidgetProvider.update(this) }
    }

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, ScreenTimeService::class.java)
                )
            }
        }
    }
}
