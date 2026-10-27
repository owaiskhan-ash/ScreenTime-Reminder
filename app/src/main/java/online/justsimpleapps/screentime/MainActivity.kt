package online.justsimpleapps.screentime

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import online.justsimpleapps.screentime.data.ScreenTimeStore
import online.justsimpleapps.screentime.notifications.Notifiers
import online.justsimpleapps.screentime.service.Monitor
import online.justsimpleapps.screentime.ui.ScreenTimeApp
import online.justsimpleapps.screentime.widget.ScreenTimeWidgetProvider
import online.justsimpleapps.screentime.util.openAppInfo
import online.justsimpleapps.screentime.util.openNotificationSettings
import online.justsimpleapps.screentime.util.openUsageAccessSettings

class MainActivity : ComponentActivity() {

    private lateinit var store: ScreenTimeStore

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        store.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        store = ScreenTimeStore.get(this)
        Notifiers.ensureChannels(this)
        handleIntent(intent)
        setContent {
            ScreenTimeApp(
                store = store,
                onRequestNotifications = { requestNotifications(auto = true) },
                onOpenAccessSettings = { openUsageAccessSettings(this) },
                onOpenAppInfo = { openAppInfo(this) },
                onOpenNotificationSettings = { requestNotifications(auto = false) },
                onOpenWebsite = { openWebsite() }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        store.refresh()
        Monitor.sync(this)
        // The repository cache is warm after refresh(), so this is cheap.
        ScreenTimeWidgetProvider.update(applicationContext, force = true)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_BREAK, false) == true) {
            store.requestBreakScreen()
        }
    }

    private fun requestNotifications(auto: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            when {
                granted -> if (!auto) openNotificationSettings(this)
                auto && store.notificationAsked -> Unit
                !auto && store.notificationAsked -> openNotificationSettings(this)
                else -> {
                    store.markNotificationAsked()
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            store.markNotificationAsked()
            if (!auto) openNotificationSettings(this)
        }
    }

    private fun openWebsite() {
        runCatching {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.justsimpleapps.online/"))
            )
        }
    }

    companion object {
        const val EXTRA_OPEN_BREAK = "extra_open_break"
    }
}
