package online.justsimpleapps.screentime.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import online.justsimpleapps.screentime.data.ScreenTimeStore
import online.justsimpleapps.screentime.util.hasUsageAccess

/**
 * Starts or stops the background monitor to match the user's settings.
 * Monitoring needs usage access; without it there is nothing to watch.
 */
object Monitor {

    fun sync(context: Context) {
        val store = ScreenTimeStore.get(context)
        val shouldRun = store.settings.backgroundMonitoring &&
            store.settings.remindersEnabled &&
            hasUsageAccess(context)
        val intent = Intent(context, ScreenTimeService::class.java)
        if (shouldRun) {
            runCatching { ContextCompat.startForegroundService(context, intent) }
        } else {
            runCatching { context.stopService(intent) }
        }
    }

    fun stop(context: Context) {
        runCatching { context.stopService(Intent(context, ScreenTimeService::class.java)) }
    }
}
