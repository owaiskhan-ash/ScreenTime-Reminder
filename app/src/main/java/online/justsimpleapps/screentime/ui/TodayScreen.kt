package online.justsimpleapps.screentime.ui

import online.justsimpleapps.screentime.ui.theme.textile
import online.justsimpleapps.screentime.ui.theme.Textile
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.justsimpleapps.screentime.data.LiveSnapshot
import online.justsimpleapps.screentime.data.ScreenTimeStore
import online.justsimpleapps.screentime.data.Settings
import online.justsimpleapps.screentime.ui.components.AccessNoticeCard
import online.justsimpleapps.screentime.ui.components.Body
import online.justsimpleapps.screentime.ui.components.GhostButton
import online.justsimpleapps.screentime.ui.components.Hairline
import online.justsimpleapps.screentime.ui.components.Headline
import online.justsimpleapps.screentime.ui.components.HourlyChart
import online.justsimpleapps.screentime.ui.components.Label
import online.justsimpleapps.screentime.ui.components.Mono
import online.justsimpleapps.screentime.ui.components.ProgressBar
import online.justsimpleapps.screentime.ui.components.RestrictedSettingsCard
import online.justsimpleapps.screentime.ui.components.SectionHeader
import online.justsimpleapps.screentime.ui.components.StatCell
import online.justsimpleapps.screentime.ui.components.TopBar
import online.justsimpleapps.screentime.ui.theme.LocalPalette
import online.justsimpleapps.screentime.util.clockLabel
import online.justsimpleapps.screentime.util.dateLabel
import online.justsimpleapps.screentime.util.formatHm
import online.justsimpleapps.screentime.util.formatHms
import online.justsimpleapps.screentime.util.formatTimer
import java.time.LocalTime
import kotlin.math.roundToInt

@Composable
fun TodayScreen(
    store: ScreenTimeStore,
    onOpenBreak: () -> Unit,
    onOpenAccessSettings: () -> Unit,
    onOpenAppInfo: () -> Unit = {}
) {
    val p = LocalPalette.current
    val s = store.snapshot
    val settings = store.settings
    val hour = LocalTime.now().hour
    var showRestrictedHelp by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar(
            right = when {
                s.limitedMode -> "LIMITED MODE"
                s.monitoring -> "LIVE"
                else -> "MONITOR PAUSED"
            },
            rightColor = if (!s.limitedMode && s.monitoring) p.fg else p.muted
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(Modifier.padding(horizontal = 22.dp)) {
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Label(dateLabel(), size = 9.sp)
                    Spacer(Modifier.weight(1f))
                    Label("THIS PHONE", size = 9.sp, color = p.faint)
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Headline("Today")
                    Spacer(Modifier.weight(1f))
                    if (s.overDailyLimit) {
                        Label("LIMIT REACHED", color = p.fg, size = 9.sp)
                    }
                }
                Spacer(Modifier.height(7.dp))
                Body(
                    if (s.limitedMode) {
                        "Limited mode. This counts only the time you spend inside this app."
                    } else {
                        "A live count of the time you spend looking."
                    },
                    size = 12.5.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(20.dp))

                HeroCard(
                    snapshot = s,
                    settings = settings,
                    onOpenBreak = onOpenBreak,
                    onOpenAccessSettings = onOpenAccessSettings
                )

                Spacer(Modifier.height(20.dp))
                StatsRow(snapshot = s)
                Spacer(Modifier.height(28.dp))

                SectionHeader("YOUR RHYTHM", "24 HOURS")
                Spacer(Modifier.height(16.dp))
                if (s.limitedMode) {
                    AccessNoticeCard(onOpenAccessSettings)
                    Spacer(Modifier.height(10.dp))
                    GhostButton(text = if (showRestrictedHelp) "Hide blocked-toggle help" else "Toggle greyed out? Fix it", onClick = { showRestrictedHelp = !showRestrictedHelp })
                    if (showRestrictedHelp) {
                        Spacer(Modifier.height(10.dp))
                        RestrictedSettingsCard(onOpenAppInfo = onOpenAppInfo, onOpenAccess = onOpenAccessSettings)
                    }
                } else {
                    HourlyChart(hourly = s.hourlyMinutes, highlightHour = hour)
                    Spacer(Modifier.height(12.dp))
                    Body(
                        "Screen-on minutes by hour, since midnight.",
                        size = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                Spacer(Modifier.height(30.dp))
                SectionHeader("TOP APPS", if (s.topApps.isEmpty()) null else "TODAY")
                Spacer(Modifier.height(4.dp))
                when {
                    s.limitedMode -> {
                        Spacer(Modifier.height(10.dp))
                        AccessNoticeCard(onOpenAccessSettings)
                    }
                    s.topApps.isEmpty() -> {
                        Spacer(Modifier.height(14.dp))
                        Body("No app activity recorded yet today.", size = 12.5.sp)
                    }
                    else -> {
                        Spacer(Modifier.height(6.dp))
                        s.topApps.forEachIndexed { index, app ->
                            TopAppRow(
                                rank = index + 1,
                                name = app.label,
                                duration = formatHm(app.foregroundMs),
                                fraction = if (s.topApps.first().foregroundMs > 0L) {
                                    app.foregroundMs.toFloat() / s.topApps.first().foregroundMs
                                } else {
                                    0f
                                }
                            )
                            if (index != s.topApps.lastIndex) Hairline()
                        }
                    }
                }

                Spacer(Modifier.height(30.dp))
                BreakCard(
                    breakDue = s.breakDue,
                    breaksToday = s.breaks,
                    onOpenBreak = onOpenBreak
                )

                Spacer(Modifier.height(34.dp))
                Hairline()
                Spacer(Modifier.height(16.dp))
                Label("NO ADS · NO TRACKING · NO ACCOUNT", color = p.faint, size = 8.5.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "justsimpleapps.online",
                    color = p.muted,
                    fontSize = 10.sp,
                    letterSpacing = 0.4.sp
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun HeroCard(
    snapshot: LiveSnapshot,
    settings: Settings,
    onOpenBreak: () -> Unit,
    onOpenAccessSettings: () -> Unit
) {
    val p = LocalPalette.current
    val total = if (snapshot.limitedMode) snapshot.continuousMs else snapshot.todayMs
    val showDaily = snapshot.dailyLimitMs > 0L
    val progress = if (showDaily) snapshot.dailyProgress else snapshot.sessionProgress
    val percent = (progress * 100).roundToInt()
    val progressLabel = when {
        showDaily && snapshot.overDailyLimit -> "LIMIT REACHED"
        showDaily -> "${formatHm(snapshot.dailyLimitMs).uppercase()} DAILY LIMIT"
        else -> "${settings.sessionMinutes}-MINUTE SESSIONS"
    }
    val subtitle = when {
        snapshot.limitedMode -> "Allow screen time access to see every app, not just this one."
        snapshot.overDailyLimit -> "Past your daily limit. The rest of the day is yours."
        snapshot.breaks == 0 -> "Since midnight. No breaks counted yet."
        else -> "Since midnight, across every app."
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .textile(p.card, Textile.LEATHER, stitched = true)
            .padding(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label(
                if (snapshot.limitedMode) "THIS SESSION" else "TIME ON SCREEN TODAY",
                color = p.cardMuted
            )
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(5.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color(0xFFD0FF00)))
            Spacer(Modifier.width(6.dp))
            Label(
                if (snapshot.limitedMode) "LIMITED" else "LIVE",
                color = p.cardMuted,
                size = 9.sp
            )
        }
        Spacer(Modifier.height(16.dp))
        Mono(formatHms(total), size = 42.sp, color = p.cardFg)
        Spacer(Modifier.height(8.dp))
        Body(subtitle, color = p.cardMuted, size = 11.5.sp, lineHeight = 17.sp)
        Spacer(Modifier.height(22.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.cardLine))
        Spacer(Modifier.height(16.dp))
        Row {
            Column(Modifier.weight(1f)) {
                Label("CONTINUOUS", color = p.cardMuted, size = 9.sp)
                Spacer(Modifier.height(7.dp))
                Mono(formatTimer(snapshot.continuousMs), size = 16.sp, color = p.cardFg)
            }
            Column(Modifier.weight(1f)) {
                Label(
                    when {
                        !snapshot.remindersEnabled -> "REMINDERS"
                        snapshot.breakDue -> "STEP AWAY"
                        else -> "NEXT NUDGE IN"
                    },
                    color = p.cardMuted,
                    size = 9.sp
                )
                Spacer(Modifier.height(7.dp))
                Mono(
                    when {
                        !snapshot.remindersEnabled -> "PAUSED"
                        snapshot.breakDue -> "NOW"
                        snapshot.nextBreakInMs > 0L -> formatTimer(snapshot.nextBreakInMs)
                        else -> "—"
                    },
                    size = 16.sp,
                    color = p.cardFg
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        ProgressBar(progress = progress, track = p.cardLine, fill = androidx.compose.ui.graphics.Color(0xFF00BCC8))
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label(progressLabel, color = p.cardMuted, size = 8.5.sp)
            Spacer(Modifier.weight(1f))
            Label("$percent%", color = p.cardMuted, size = 8.5.sp)
        }
        if (snapshot.breakDue || snapshot.limitedMode) {
            Spacer(Modifier.height(18.dp))
            GhostButton(
                text = if (snapshot.limitedMode) "Turn on screen time access" else "Take a break now",
                onClick = if (snapshot.limitedMode) onOpenAccessSettings else onOpenBreak,
                dark = true
            )
        }
    }
}

@Composable
private fun StatsRow(snapshot: LiveSnapshot) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatCell(
            value = if (snapshot.firstUseMs > 0L) clockLabel(snapshot.firstUseMs) else "—",
            label = "FIRST UNLOCK",
            modifier = Modifier.weight(1f),
            valueSize = 15.sp
        )
        VerticalRule()
        StatCell(
            value = if (snapshot.limitedMode) "—" else snapshot.unlocks.toString(),
            label = "UNLOCKS",
            modifier = Modifier.weight(1f),
            valueSize = 17.sp
        )
        VerticalRule()
        StatCell(
            value = snapshot.breaks.toString(),
            label = "BREAKS",
            modifier = Modifier.weight(1f),
            valueSize = 17.sp
        )
    }
}

@Composable
private fun VerticalRule() {
    Box(Modifier.width(1.dp).height(30.dp).background(LocalPalette.current.line))
}

@Composable
private fun TopAppRow(rank: Int, name: String, duration: String, fraction: Float) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(vertical = 13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Mono(rank.toString().padStart(2, '0'), size = 9.5.sp, color = p.accent)
            Spacer(Modifier.width(14.dp))
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                color = p.fg,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(12.dp))
            Mono(duration, size = 11.5.sp, color = p.muted)
        }
        Spacer(Modifier.height(9.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.line)) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .height(1.dp)
                    .background(p.fg.copy(alpha = 0.55f))
            )
        }
    }
}

@Composable
private fun BreakCard(breakDue: Boolean, breaksToday: Int, onOpenBreak: () -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .textile(p.subtle, Textile.SUEDE, stitched = true)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label("STEP AWAY", color = p.fg)
            Spacer(Modifier.weight(1f))
            if (breaksToday > 0) Label("$breaksToday TODAY", color = p.muted, size = 8.5.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (breakDue) "Time to look up." else "Look up for a minute.",
            color = p.fg,
            fontSize = 21.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.3).sp
        )
        Spacer(Modifier.height(7.dp))
        Body(
            "Stand, stretch, and look at something far away. " +
                "Twenty seconds of distance does more than twenty minutes of scrolling.",
            size = 12.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(16.dp))
        GhostButton(
            text = if (breakDue) "Take a 5-min break" else "Start a 5-min break",
            onClick = onOpenBreak
        )
    }
}
