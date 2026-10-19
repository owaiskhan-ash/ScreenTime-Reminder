package online.justsimpleapps.screentime.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import online.justsimpleapps.screentime.data.ScreenTimeStore
import online.justsimpleapps.screentime.data.SessionEngine
import online.justsimpleapps.screentime.util.hasUsageAccess

/** A reboot should never look like one very long screen session. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        val store = ScreenTimeStore.get(context)
        SessionEngine.reset(store)
        if (store.settings.backgroundMonitoring &&
            store.settings.remindersEnabled &&
            hasUsageAccess(context)
        ) {
            Notifiers.ensureChannels(context)
            runCatching {
                val service = Intent(context, online.justsimpleapps.screentime.service.ScreenTimeService::class.java)
                context.startForegroundService(service)
            }
        }
    }
}
