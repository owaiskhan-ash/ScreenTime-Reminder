package online.justsimpleapps.screentime.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import online.justsimpleapps.screentime.MainActivity
import online.justsimpleapps.screentime.R
import online.justsimpleapps.screentime.data.Settings
import online.justsimpleapps.screentime.util.formatHm
import online.justsimpleapps.screentime.util.formatTimer

object Notifiers {

    const val CHANNEL_BREAK = "break_reminders"
    const val CHANNEL_BREAK_QUIET = "break_reminders_quiet"
    const val CHANNEL_MONITOR = "monitor"

    const val ID_MONITOR = 41
    const val ID_BREAK = 42
    const val ID_LIMIT = 43
    const val ID_TEST = 44

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val monitor = NotificationChannel(
            CHANNEL_MONITOR,
            "Background monitor",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "A quiet, ongoing notice while screen time is being watched."
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        val loud = NotificationChannel(
            CHANNEL_BREAK,
            "Break reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "A nudge when a screen session runs long."
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 220L, 140L, 220L)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        val quiet = NotificationChannel(
            CHANNEL_BREAK_QUIET,
            "Break reminders without vibration",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "A nudge when a screen session runs long, with sound only."
            enableVibration(false)
            vibrationPattern = null
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        manager.createNotificationChannels(listOf(monitor, loud, quiet))
    }

    private fun channelFor(settings: Settings): String =
        if (settings.vibrate) CHANNEL_BREAK else CHANNEL_BREAK_QUIET

    private fun openApp(context: Context, openBreak: Boolean): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_OPEN_BREAK, openBreak)
        }
        return PendingIntent.getActivity(
            context,
            if (openBreak) 1 else 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun snoozeIntent(context: Context): PendingIntent {
        val intent = Intent(context, SnoozeReceiver::class.java).apply {
            action = SnoozeReceiver.ACTION_SNOOZE
        }
        return PendingIntent.getBroadcast(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun postBreak(context: Context, settings: Settings, sessionMs: Long) {
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, channelFor(settings))
            .setSmallIcon(R.drawable.ic_stat_break)
            .setContentTitle("Time to look up")
            .setContentText("${formatHm(sessionMs)} on screen. Take a few minutes away.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "You have been looking at a screen for ${formatHm(sessionMs)}. " +
                        "Rest your eyes, stand up, and come back in a few minutes."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp(context, openBreak = true))
            .addAction(0, "Snooze 15 min", snoozeIntent(context))
            .build()
        notify(context, ID_BREAK, notification)
    }

    fun postDailyLimit(context: Context, settings: Settings, totalMs: Long) {
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, channelFor(settings))
            .setSmallIcon(R.drawable.ic_stat_break)
            .setContentTitle("Daily limit reached")
            .setContentText("${formatHm(totalMs)} on screen today. The rest of the day is yours.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "${formatHm(totalMs)} on screen today. That is past the limit you set. " +
                        "Put the phone down for a little while."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp(context, openBreak = true))
            .build()
        notify(context, ID_LIMIT, notification)
    }

    fun postTest(context: Context, settings: Settings) {
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, channelFor(settings))
            .setSmallIcon(R.drawable.ic_stat_break)
            .setContentTitle("This is what a nudge looks like")
            .setContentText("You are all set. Reminders will arrive like this.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp(context, openBreak = false))
            .build()
        notify(context, ID_TEST, notification)
    }

    fun buildMonitor(
        context: Context,
        title: String,
        text: String
    ): Notification {
        ensureChannels(context)
        return NotificationCompat.Builder(context, CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_stat_break)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setOngoing(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp(context, openBreak = false))
            .build()
    }

    fun monitorText(
        todayMs: Long,
        remindersEnabled: Boolean,
        sessionActive: Boolean,
        nextReminderAt: Long,
        now: Long
    ): String = when {
        !remindersEnabled -> "Reminders are paused"
        sessionActive && nextReminderAt > now ->
            "Today ${formatHm(todayMs)} · next nudge in ${formatTimer(nextReminderAt - now)}"
        sessionActive -> "Today ${formatHm(todayMs)} · time for a break"
        else -> "Today ${formatHm(todayMs)} · watching quietly"
    }

    fun cancel(context: Context, id: Int) {
        runCatching { NotificationManagerCompat.from(context).cancel(id) }
    }

    private fun notify(context: Context, id: Int, notification: Notification) {
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }
}
