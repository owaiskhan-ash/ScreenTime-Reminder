package online.justsimpleapps.screentime.ui

import online.justsimpleapps.screentime.ui.theme.textile
import online.justsimpleapps.screentime.ui.theme.Textile
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import online.justsimpleapps.screentime.data.ScreenTimeStore
import online.justsimpleapps.screentime.data.SessionEngine
import online.justsimpleapps.screentime.notifications.Notifiers
import online.justsimpleapps.screentime.service.Monitor
import online.justsimpleapps.screentime.ui.components.Hairline
import online.justsimpleapps.screentime.ui.theme.LocalPalette
import online.justsimpleapps.screentime.ui.theme.ScreenTimeTheme

@Composable
fun ScreenTimeApp(
    store: ScreenTimeStore,
    onRequestNotifications: () -> Unit,
    onOpenAccessSettings: () -> Unit,
    onOpenAppInfo: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit,
    onOpenWebsite: () -> Unit
) {
    ScreenTimeTheme(store.settings.themeMode) {
        val p = LocalPalette.current
        val context = LocalContext.current
        val owner = LocalLifecycleOwner.current
        var tab by remember { mutableIntStateOf(0) }
        var showBreak by remember { mutableStateOf(false) }
        var dismissedDue by remember { mutableStateOf(false) }
        val snapshot = store.snapshot

        LaunchedEffect(owner) {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    withContext(Dispatchers.IO) {
                        store.refresh(System.currentTimeMillis(), tickSession = true)
                    }
                    delay(1000L)
                }
            }
        }

        LaunchedEffect(snapshot.breakDue) {
            if (snapshot.breakDue && !dismissedDue) {
                showBreak = true
            }
            if (!snapshot.breakDue) {
                dismissedDue = false
            }
        }

        LaunchedEffect(store.breakScreenRequest) {
            if (store.breakScreenRequest) {
                showBreak = true
                store.consumeBreakScreenRequest()
            }
        }

        LaunchedEffect(store.onboardingDone) {
            if (store.onboardingDone && !store.notificationAsked) {
                onRequestNotifications()
            }
        }

        val startBreak: () -> Unit = {
            SessionEngine.startBreak(store, System.currentTimeMillis())
            Notifiers.cancel(context, Notifiers.ID_BREAK)
            dismissedDue = true
            store.refresh(System.currentTimeMillis(), tickSession = false)
        }
        val snooze: () -> Unit = {
            SessionEngine.snooze(store, System.currentTimeMillis(), 15)
            Notifiers.cancel(context, Notifiers.ID_BREAK)
            dismissedDue = true
            showBreak = false
            store.refresh(System.currentTimeMillis(), tickSession = false)
        }

        Box(Modifier.fillMaxSize().textile(p.bg, Textile.LEATHER)) {
            if (!store.onboardingDone) {
                OnboardingScreen(
                    store = store,
                    onAllowAccess = onOpenAccessSettings,
                    onOpenAppInfo = onOpenAppInfo,
                    onDone = {
                        store.completeOnboarding()
                        store.refresh()
                    }
                )
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    Box(Modifier.weight(1f)) {
                        when (tab) {
                            0 -> TodayScreen(
                                store = store,
                                onOpenBreak = { showBreak = true },
                                onOpenAccessSettings = onOpenAccessSettings,
                                onOpenAppInfo = onOpenAppInfo
                            )
                            1 -> InsightsScreen(store = store)
                            else -> SettingsScreen(
                                store = store,
                                onOpenAccess = onOpenAccessSettings,
                                onOpenAppInfo = onOpenAppInfo,
                                onOpenNotifications = onOpenNotificationSettings,
                                onSendTest = { Notifiers.postTest(context, store.settings) },
                                onSettingsChanged = { Monitor.sync(context) },
                                onOpenWebsite = onOpenWebsite
                            )
                        }
                    }
                    BottomNav(selected = tab, onSelect = { tab = it })
                }
            }

            if (showBreak) {
                BreakOverlay(
                    store = store,
                    onClose = {
                        showBreak = false
                        if (store.snapshot.breakDue) dismissedDue = true
                    },
                    onStartBreak = startBreak,
                    onSnooze = snooze
                )
            }
        }
    }
}

@Composable
private fun BottomNav(selected: Int, onSelect: (Int) -> Unit) {
    val p = LocalPalette.current
    val items = listOf("TODAY", "WEEK", "SETTINGS")
    Column(Modifier.fillMaxWidth().textile(p.subtle, Textile.FELT)) {
        Hairline()
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, label ->
                Column(
                    Modifier
                        .clickable { onSelect(index) }
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label,
                        color = if (index == selected) p.fg else p.muted,
                        fontSize = 9.5.sp,
                        fontWeight = if (index == selected) FontWeight.Medium else FontWeight.Normal,
                        letterSpacing = 1.7.sp
                    )
                    Spacer(Modifier.height(7.dp))
                    Box(
                        Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(if (index == selected) p.lime else Color.Transparent)
                    )
                }
            }
        }
    }
}
