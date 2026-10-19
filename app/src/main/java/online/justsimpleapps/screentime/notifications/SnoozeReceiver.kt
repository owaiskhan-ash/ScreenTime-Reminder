package online.justsimpleapps.screentime.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import online.justsimpleapps.screentime.data.ScreenTimeStore
import online.justsimpleapps.screentime.data.SessionEngine

/** The "Snooze 15 min" button on a break notification. */
class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_SNOOZE) return
        val store = ScreenTimeStore.get(context)
        SessionEngine.snooze(store, System.currentTimeMillis(), 15)
        Notifiers.cancel(context, Notifiers.ID_BREAK)
    }

    companion object {
        const val ACTION_SNOOZE = "online.justsimpleapps.screentime.action.SNOOZE"
    }
}
