package online.justsimpleapps.screentime.ui

import online.justsimpleapps.screentime.ui.theme.textile
import online.justsimpleapps.screentime.ui.theme.Textile
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import online.justsimpleapps.screentime.data.ScreenTimeStore
import online.justsimpleapps.screentime.data.ThemeMode
import online.justsimpleapps.screentime.ui.components.AppMark
import online.justsimpleapps.screentime.ui.components.Body
import online.justsimpleapps.screentime.ui.components.GhostButton
import online.justsimpleapps.screentime.ui.components.Hairline
import online.justsimpleapps.screentime.ui.components.Headline
import online.justsimpleapps.screentime.ui.components.Label
import online.justsimpleapps.screentime.ui.components.PrimaryButton
import online.justsimpleapps.screentime.ui.components.RestrictedSettingsCard
import online.justsimpleapps.screentime.ui.components.SectionLabel
import online.justsimpleapps.screentime.ui.components.Segmented
import online.justsimpleapps.screentime.ui.components.SettingsRow
import online.justsimpleapps.screentime.ui.components.SwitchRow
import online.justsimpleapps.screentime.ui.components.TextAction
import online.justsimpleapps.screentime.ui.components.TopBar
import online.justsimpleapps.screentime.ui.theme.LocalPalette

@Composable
fun SettingsScreen(
    store: ScreenTimeStore,
    onOpenAccess: () -> Unit,
    onOpenAppInfo: () -> Unit = {},
    onOpenNotifications: () -> Unit,
    onSendTest: () -> Unit,
    onSettingsChanged: () -> Unit,
    onOpenWebsite: () -> Unit
) {
    val p = LocalPalette.current
    val settings = store.settings
    val snapshot = store.snapshot
    var showReset by remember { mutableStateOf(false) }
    var showRestrictedHelp by remember { mutableStateOf(false) }

    val sessionOptions = listOf(30, 45, 60, 90)
    val dailyOptions = listOf(120, 180, 300, 360, 0)
    val themes = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)

    Column(Modifier.fillMaxSize()) {
        TopBar(right = "1.0.0")
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(Modifier.padding(horizontal = 22.dp)) {
                Spacer(Modifier.height(22.dp))
                Headline("Settings")
                Spacer(Modifier.height(7.dp))
                Body(
                    "A few switches. Nothing buried, nothing extra.",
                    size = 12.5.sp,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(30.dp))
                SectionLabel("SESSIONS & REMINDERS")
                Spacer(Modifier.height(4.dp))
                SwitchRow(
                    title = "Reminders",
                    subtitle = "A calm nudge when a screen session runs long.",
                    checked = settings.remindersEnabled,
                    onCheckedChange = {
                        store.updateSettings { s -> s.copy(remindersEnabled = it) }
                        onSettingsChanged()
                    }
                )
                Hairline()
                Column(Modifier.padding(vertical = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Reminder after",
                            color = p.fg,
                            fontSize = 14.5.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Label("${settings.sessionMinutes} MIN", color = p.muted, size = 9.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Segmented(
                        options = sessionOptions.map { "${it}m" },
                        selectedIndex = sessionOptions.indexOf(settings.sessionMinutes).coerceAtLeast(0),
                        onSelect = { index ->
                            store.updateSettings { s -> s.copy(sessionMinutes = sessionOptions[index]) }
                            onSettingsChanged()
                        }
                    )
                }
                Hairline()
                SwitchRow(
                    title = "Repeat while still on screen",
                    subtitle = "Nudge again every ${settings.sessionMinutes} minutes if you have not stepped away.",
                    checked = settings.repeatWhileOn,
                    onCheckedChange = {
                        store.updateSettings { s -> s.copy(repeatWhileOn = it) }
                        onSettingsChanged()
                    }
                )
                Hairline()
                SwitchRow(
                    title = "Vibrate on reminder",
                    subtitle = "Sound comes from your notification settings.",
                    checked = settings.vibrate,
                    onCheckedChange = {
                        store.updateSettings { s -> s.copy(vibrate = it) }
                        onSettingsChanged()
                    }
                )

                Spacer(Modifier.height(28.dp))
                SectionLabel("DAILY LIMIT")
                Spacer(Modifier.height(4.dp))
                Column(Modifier.padding(vertical = 16.dp)) {
                    Text("Stop nudging me after", color = p.fg, fontSize = 14.5.sp)
                    Spacer(Modifier.height(4.dp))
                    Body(
                        "A second, firmer reminder when the day's total passes this line.",
                        size = 11.5.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Segmented(
                        options = listOf("2h", "3h", "5h", "6h", "OFF"),
                        selectedIndex = dailyOptions.indexOf(settings.dailyLimitMinutes).coerceAtLeast(0),
                        onSelect = { index ->
                            store.updateSettings { s -> s.copy(dailyLimitMinutes = dailyOptions[index]) }
                            onSettingsChanged()
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    Label(
                        if (snapshot.dailyLimitMs > 0L) {
                            "TODAY ${snapshot.todayMs / 60_000L / 60}H ${snapshot.todayMs / 60_000L % 60}M OF ${settings.dailyLimitMinutes / 60}H"
                        } else {
                            "NO DAILY LIMIT SET"
                        },
                        size = 8.5.sp
                    )
                }
                Hairline()

                Spacer(Modifier.height(28.dp))
                SectionLabel("MONITORING")
                Spacer(Modifier.height(4.dp))
                SwitchRow(
                    title = "Background monitoring",
                    subtitle = "Keeps watching while the app is closed. A quiet, ongoing notice stays in your shade.",
                    checked = settings.backgroundMonitoring,
                    onCheckedChange = {
                        store.updateSettings { s -> s.copy(backgroundMonitoring = it) }
                        onSettingsChanged()
                    }
                )
                if (!store.accessGranted) {
                    Spacer(Modifier.height(4.dp))
                    Body(
                        "Needs screen time access to run.",
                        size = 11.sp,
                        lineHeight = 15.sp,
                        color = p.muted
                    )
                }
                Hairline()

                Spacer(Modifier.height(28.dp))
                SectionLabel("ACCESS")
                Spacer(Modifier.height(4.dp))
                SettingsRow(
                    title = "Screen time access",
                    subtitle = "The phone's own usage record. Read on this device only.",
                    value = if (store.accessGranted) "ALLOWED" else "NEEDED",
                    accent = if (store.accessGranted) p.fg else p.muted,
                    onClick = onOpenAccess
                )
                if (!store.accessGranted) {
                    Spacer(Modifier.height(8.dp))
                    GhostButton(
                        text = if (showRestrictedHelp) "Hide blocked-toggle help" else "Toggle blocked or greyed out?",
                        onClick = { showRestrictedHelp = !showRestrictedHelp }
                    )
                    if (showRestrictedHelp) {
                        Spacer(Modifier.height(12.dp))
                        RestrictedSettingsCard(onOpenAppInfo = onOpenAppInfo, onOpenAccess = onOpenAccess)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Hairline()
                SettingsRow(
                    title = "Notifications",
                    subtitle = "Reminders and the daily limit notice.",
                    value = if (snapshot.notificationsAllowed) "ON" else "OFF",
                    accent = if (snapshot.notificationsAllowed) p.fg else p.muted,
                    onClick = onOpenNotifications
                )
                Hairline()

                Spacer(Modifier.height(28.dp))
                SectionLabel("APPEARANCE")
                Spacer(Modifier.height(12.dp))
                Segmented(
                    options = listOf("LIGHT", "DARK", "SYSTEM"),
                    selectedIndex = themes.indexOf(settings.themeMode).coerceAtLeast(0),
                    onSelect = { index ->
                        store.updateSettings { s -> s.copy(themeMode = themes[index]) }
                    }
                )
                Spacer(Modifier.height(10.dp))
                Body("Sand felt, charcoal felt, or follow the phone.", size = 11.sp)

                Spacer(Modifier.height(28.dp))
                SectionLabel("TEST & DATA")
                Spacer(Modifier.height(4.dp))
                SettingsRow(
                    title = "Send a test reminder",
                    subtitle = "See exactly what a nudge looks like.",
                    value = "SEND",
                    onClick = onSendTest
                )
                Hairline()
                SettingsRow(
                    title = "Reset today's tracking",
                    subtitle = "Start the day fresh inside the app.",
                    value = "RESET",
                    onClick = { showReset = true }
                )
                Hairline()

                Spacer(Modifier.height(30.dp))
                SectionLabel("ABOUT")
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppMark(size = 26.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Screen Time Reminder",
                            color = p.fg,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(3.dp))
                        Label("VERSION 1.1.0 · ANDROID", size = 8.5.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Body(
                    "Free to use. No ads, no tracking, no account. Screen-time numbers, " +
                        "settings and daily totals stay on this phone.",
                    size = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(12.dp))
                TextAction("JUSTSIMPLEAPPS.ONLINE", onOpenWebsite)
                Spacer(Modifier.height(18.dp))
                Label("MADE FOR ONE THING · NOT FOR ATTENTION", color = p.faint, size = 8.sp)
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showReset) {
        Dialog(onDismissRequest = { showReset = false }) {
            val shape = RoundedCornerShape(22.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .textile(p.bg, Textile.SUEDE, stitched = true)
                    .border(1.dp, p.line, shape)
                    .padding(22.dp)
            ) {
                Label("RESET TODAY", color = p.muted, size = 9.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Start today fresh?",
                    color = p.fg,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.3).sp
                )
                Spacer(Modifier.height(8.dp))
                Body(
                    "Clears today's session, break count and totals inside this app. " +
                        "Android's own usage record is untouched.",
                    size = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton(
                        text = "Cancel",
                        onClick = { showReset = false },
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = "Reset",
                        onClick = {
                            store.resetToday()
                            showReset = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
