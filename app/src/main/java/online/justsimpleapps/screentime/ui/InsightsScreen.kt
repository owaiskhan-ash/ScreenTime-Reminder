package online.justsimpleapps.screentime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.justsimpleapps.screentime.data.ScreenTimeStore
import online.justsimpleapps.screentime.data.WeekSummary
import online.justsimpleapps.screentime.ui.components.Body
import online.justsimpleapps.screentime.ui.components.Hairline
import online.justsimpleapps.screentime.ui.components.Headline
import online.justsimpleapps.screentime.ui.components.Label
import online.justsimpleapps.screentime.ui.components.Mono
import online.justsimpleapps.screentime.ui.components.SectionHeader
import online.justsimpleapps.screentime.ui.components.TopBar
import online.justsimpleapps.screentime.ui.components.WeeklyChart
import online.justsimpleapps.screentime.ui.theme.LocalPalette
import online.justsimpleapps.screentime.util.formatHm
import online.justsimpleapps.screentime.util.shortDay
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(store: ScreenTimeStore) {
    val p = LocalPalette.current
    val snapshot = store.snapshot
    val week = store.weekSummary(snapshot.todayMs, snapshot.breaks)

    Column(Modifier.fillMaxSize()) {
        TopBar(right = "LAST 7 DAYS")
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(Modifier.padding(horizontal = 22.dp)) {
                Spacer(Modifier.height(22.dp))
                Label("YOUR WEEK", size = 9.sp)
                Spacer(Modifier.height(10.dp))
                Headline("Seven days.")
                Spacer(Modifier.height(7.dp))
                Body(
                    "The same record, looked at from further back.",
                    size = 12.5.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Label("DAILY AVERAGE", size = 9.sp)
                        Spacer(Modifier.height(8.dp))
                        Mono(
                            if (week.trackedDays > 0) formatHm(week.averageMs) else "—",
                            size = 30.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Label("DAYS TRACKED", size = 9.sp)
                        Spacer(Modifier.height(8.dp))
                        Mono("${week.trackedDays} / 7", size = 16.sp)
                    }
                }
                Spacer(Modifier.height(24.dp))
                WeeklyChart(week.points)
                Spacer(Modifier.height(20.dp))

                SectionHeader("DAY BY DAY")
                Spacer(Modifier.height(4.dp))
                week.points.reversed().forEach { point ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Label(
                                    shortDay(point.date),
                                    color = if (point.isToday) p.fg else p.muted,
                                    size = 9.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                Mono(
                                    point.date.dayOfMonth.toString().padStart(2, '0'),
                                    size = 11.sp,
                                    color = if (point.isToday) p.fg else p.muted
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(p.line)
                            ) {
                                val max = (week.points.maxOfOrNull { it.ms } ?: 0L).coerceAtLeast(1L)
                                val fraction = (point.ms.toFloat() / max).coerceIn(0.02f, 1f)
                                Box(
                                    Modifier
                                        .fillMaxWidth(fraction)
                                        .height(1.dp)
                                        .background(
                                            if (point.isToday) p.fg else p.fg.copy(alpha = 0.4f)
                                        )
                                )
                            }
                        }
                        Spacer(Modifier.width(18.dp))
                        Mono(
                            if (point.tracked) formatHm(point.ms) else "—",
                            size = 12.sp,
                            color = if (point.isToday) p.fg else p.muted
                        )
                        if (point.isToday) {
                            Spacer(Modifier.width(10.dp))
                            Label("TODAY", size = 7.5.sp, color = p.faint)
                        }
                    }
                    Hairline()
                }

                Spacer(Modifier.height(30.dp))
                SectionHeader("PATTERNS")
                Spacer(Modifier.height(6.dp))
                PatternRow(
                    "LIGHTEST DAY",
                    week.lightest?.takeIf { it.ms > 0L }?.let {
                        "${shortDay(it.date)} · ${formatHm(it.ms)}"
                    } ?: "—"
                )
                Hairline()
                PatternRow(
                    "LONGEST SESSION",
                    if (snapshot.maxSessionMs > 0L) formatHm(snapshot.maxSessionMs) else "—"
                )
                Hairline()
                PatternRow("BREAKS THIS WEEK", week.breaks.toString())
                Hairline()
                PatternRow("TODAY VS AVERAGE", paceLine(week, snapshot.todayMs))
                Hairline()

                Spacer(Modifier.height(26.dp))
                Body(
                    "History begins the first day the app runs. Android does not share " +
                        "usage from before an app is installed.",
                    size = 11.sp,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun PatternRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text14(label)
        Spacer(Modifier.weight(1f))
        Mono(value, size = 12.sp)
    }
}

@Composable
private fun Text14(text: String) {
    androidx.compose.material3.Text(
        text = text.uppercase(),
        color = LocalPalette.current.muted,
        fontSize = 9.5.sp,
        letterSpacing = 1.3.sp
    )
}

private fun paceLine(week: WeekSummary, todayMs: Long): String {
    if (week.averageMs <= 0L) return "—"
    if (week.trackedDays <= 1) return "First day"
    val diff = todayMs - week.averageMs
    if (diff.absoluteValue < 5 * 60_000L) return "About average"
    val percent = (diff.toDouble() / week.averageMs * 100).roundToInt().absoluteValue
    return if (diff < 0) "$percent% below average" else "$percent% above average"
}
