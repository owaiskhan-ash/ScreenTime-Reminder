package online.justsimpleapps.screentime.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import online.justsimpleapps.screentime.MainActivity
import online.justsimpleapps.screentime.R
import online.justsimpleapps.screentime.data.UsageRepository
import online.justsimpleapps.screentime.util.hasUsageAccess

/**
 * The small 2x1 home-screen widget: a white eye and today's screen time
 * in hours and minutes, and nothing else — no background, no outline,
 * so the wallpaper is the only surface. Tapping it opens the app.
 *
 * The numbers come from the same [UsageRepository] event stream as the
 * rest of the app, so the widget always agrees with the Today screen.
 */
class ScreenTimeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // Reading the usage event stream can take a moment; keep it off
        // the main thread. goAsync() gives us a short window to finish.
        val pending = goAsync()
        Thread {
            try {
                render(context.applicationContext, manager, ids)
            } finally {
                pending.finish()
            }
        }.start()
    }

    override fun onEnabled(context: Context) {
        val appContext = context.applicationContext
        Thread { update(appContext, force = true) }.start()
    }

    companion object {
        @Volatile
        private var lastPushAt = 0L

        /** The background loop ticks every 20 s; the widget need not. */
        private const val MIN_PUSH_INTERVAL_MS = 30_000L

        /**
         * Render fresh data onto every placed widget. A cheap no-op when
         * no widget is on a home screen, and throttled so the 20-second
         * monitor loop cannot spam AppWidgetManager.
         */
        fun update(context: Context, force: Boolean = false) {
            val now = System.currentTimeMillis()
            if (!force && now - lastPushAt < MIN_PUSH_INTERVAL_MS) return
            lastPushAt = now
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(
                ComponentName(context, ScreenTimeWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return
            render(context.applicationContext, manager, ids)
        }

        private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val now = System.currentTimeMillis()
            val granted = hasUsageAccess(context)
            val data = if (granted) UsageRepository.get(context).snapshot(now) else null
            // Always explicit hours and minutes: "0h 0m", "2h 14m".
            val totalMinutes = (data?.todayMs ?: 0L).coerceAtLeast(0L) / 60_000L
            val time = "${totalMinutes / 60L}h ${totalMinutes % 60L}m"

            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )

            for (id in ids) {
                val views = RemoteViews(context.packageName, R.layout.widget_screen_time)
                views.setTextViewText(R.id.widget_time, time)
                views.setOnClickPendingIntent(R.id.widget_root, openApp)
                manager.updateAppWidget(id, views)
            }
        }
    }
}
