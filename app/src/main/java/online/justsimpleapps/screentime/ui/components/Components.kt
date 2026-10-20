package online.justsimpleapps.screentime.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.justsimpleapps.screentime.data.DayPoint
import online.justsimpleapps.screentime.ui.theme.LocalPalette
import online.justsimpleapps.screentime.util.formatHmCompact
import online.justsimpleapps.screentime.util.shortDay

// ---------------------------------------------------------------------------
// Type
// ---------------------------------------------------------------------------

@Composable
fun Label(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.muted,
    size: TextUnit = 10.sp,
    weight: FontWeight = FontWeight.Medium,
    align: TextAlign? = null
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size,
        fontWeight = weight,
        letterSpacing = 1.6.sp,
        lineHeight = size * 1.5f,
        textAlign = align
    )
}

@Composable
fun Body(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.muted,
    size: TextUnit = 13.sp,
    lineHeight: TextUnit = 20.sp,
    weight: FontWeight = FontWeight.Normal
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size,
        fontWeight = weight,
        lineHeight = lineHeight
    )
}

@Composable
fun Headline(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.fg,
    size: TextUnit = 34.sp
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size,
        lineHeight = size * 1.1f,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.6).sp
    )
}

@Composable
fun Mono(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.fg,
    size: TextUnit = 18.sp,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign? = null
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size,
        fontWeight = weight,
        fontFamily = FontFamily.Monospace,
        letterSpacing = (-0.4).sp,
        textAlign = align
    )
}

// ---------------------------------------------------------------------------
// Rules, marks, buttons
// ---------------------------------------------------------------------------

@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = LocalPalette.current.line) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
fun AppMark(modifier: Modifier = Modifier, size: Dp = 22.dp, color: Color = LocalPalette.current.fg) {
    Canvas(modifier.size(size)) {
        val stroke = this.size.minDimension * 0.09f
        val radius = this.size.minDimension * 0.42f
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(color, radius = radius, center = c, style = Stroke(width = stroke))
        drawLine(
            color,
            start = c,
            end = Offset(c.x, c.y - radius * 0.62f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color,
            start = c,
            end = Offset(c.x + radius * 0.46f, c.y + radius * 0.34f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawCircle(color, radius = stroke * 0.9f, center = c)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dark: Boolean = false
) {
    val p = LocalPalette.current
    val bg = when {
        !enabled -> if (dark) p.cardMuted.copy(alpha = 0.4f) else p.faint.copy(alpha = 0.5f)
        else -> if (dark) p.cardFg else p.fg
    }
    val fg = if (dark) p.card else p.bg
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(bg)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.8.sp
        )
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dark: Boolean = false,
    colorOverride: Color? = null
) {
    val p = LocalPalette.current
    val line = if (dark) p.cardLine else p.line
    val fg = colorOverride ?: if (dark) p.cardFg else p.fg
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(2.dp))
            .border(1.dp, line, RoundedCornerShape(2.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.8.sp
        )
    }
}

@Composable
fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.fg
) {
    Text(
        text = text.uppercase(),
        modifier = modifier.clickable { onClick() }.padding(vertical = 6.dp),
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.5.sp
    )
}

@Composable
fun ToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val p = LocalPalette.current
    val t by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "toggle"
    )
    Box(
        modifier
            .size(width = 42.dp, height = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) lerp(p.line, p.fg, t) else p.line.copy(alpha = 0.6f))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .offset(x = (18f * t).dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(p.bg)
                .border(1.dp, lerp(p.muted.copy(alpha = 0.45f), p.bg, t), CircleShape)
        )
    }
}

// ---------------------------------------------------------------------------
// Rows
// ---------------------------------------------------------------------------

@Composable
fun SwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (enabled) p.fg else p.muted,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Normal
            )
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Body(subtitle, size = 11.5.sp, lineHeight = 16.sp, color = p.muted)
            }
        }
        Spacer(Modifier.width(16.dp))
        ToggleSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    accent: Color? = null
) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = p.fg, fontSize = 14.5.sp)
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Body(subtitle, size = 11.5.sp, lineHeight = 16.sp)
            }
        }
        if (value != null) {
            Spacer(Modifier.width(12.dp))
            Label(value, color = accent ?: p.muted)
        }
        if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            ChevronGlyph(color = p.faint)
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = LocalPalette.current.muted,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.8.sp
    )
}

@Composable
fun SectionHeader(title: String, trailing: String? = null) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Label(title, color = LocalPalette.current.fg)
        Spacer(Modifier.weight(1f))
        if (trailing != null) Label(trailing, color = LocalPalette.current.muted)
    }
}

@Composable
fun Segmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .border(1.dp, p.line, RoundedCornerShape(2.dp))
            .clip(RoundedCornerShape(2.dp))
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (selected) p.fg else Color.Transparent)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.uppercase(),
                    color = if (selected) p.bg else p.muted,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.Center
                )
            }
            if (index != options.lastIndex) {
                Box(Modifier.width(1.dp).fillMaxHeight().background(p.line))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Data display
// ---------------------------------------------------------------------------

@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    track: Color = LocalPalette.current.line,
    fill: Color = LocalPalette.current.fg
) {
    Box(modifier.fillMaxWidth().height(2.dp).background(track)) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(2.dp)
                .background(fill)
        )
    }
}

@Composable
fun StatCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = LocalPalette.current.fg,
    valueSize: TextUnit = 17.sp
) {
    Column(modifier) {
        Mono(value, size = valueSize, color = valueColor)
        Spacer(Modifier.height(5.dp))
        Label(label, size = 9.sp)
    }
}

@Composable
fun HourlyChart(
    hourly: List<Float>,
    highlightHour: Int,
    modifier: Modifier = Modifier
) {
    val p = LocalPalette.current
    val max = (hourly.maxOrNull() ?: 0f).coerceAtLeast(1f)
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(88.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            hourly.forEachIndexed { index, value ->
                val fraction = (value / max).coerceIn(0f, 1f)
                Box(
                    Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(if (fraction < 0.03f) 0.03f else fraction)
                            .background(
                                when {
                                    index == highlightHour -> p.fg
                                    value > 0f -> p.fg.copy(alpha = 0.20f)
                                    else -> p.line
                                }
                            )
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Hairline()
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Label("00", size = 8.5.sp, color = p.faint)
            Spacer(Modifier.weight(1f))
            Label("06", size = 8.5.sp, color = p.faint)
            Spacer(Modifier.weight(1f))
            Label("12", size = 8.5.sp, color = p.faint)
            Spacer(Modifier.weight(1f))
            Label("18", size = 8.5.sp, color = p.faint)
            Spacer(Modifier.weight(1f))
            Label("24", size = 8.5.sp, color = p.faint)
        }
    }
}

@Composable
fun WeeklyChart(points: List<DayPoint>, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val max = (points.maxOfOrNull { it.ms } ?: 0L).coerceAtLeast(1L)
    Row(
        modifier.fillMaxWidth().height(132.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        points.forEach { point ->
            val fraction = (point.ms.toFloat() / max).coerceIn(0f, 1f)
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (point.tracked) {
                    Mono(
                        formatHmCompact(point.ms),
                        size = 8.5.sp,
                        color = if (point.isToday) p.fg else p.muted
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(if (fraction < 0.02f) 0.02f else fraction * 0.8f)
                        .background(
                            when {
                                point.isToday -> p.fg
                                point.tracked -> p.fg.copy(alpha = 0.16f)
                                else -> p.line
                            }
                        )
                )
                Spacer(Modifier.height(8.dp))
                Label(
                    shortDay(point.date),
                    size = 8.5.sp,
                    color = if (point.isToday) p.fg else p.faint
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Line glyphs, drawn rather than imported
// ---------------------------------------------------------------------------

@Composable
fun CloseGlyph(
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.fg,
    size: Dp = 16.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val inset = s * 0.22f
        val stroke = s * 0.075f
        drawLine(color, Offset(inset, inset), Offset(s - inset, s - inset), stroke, StrokeCap.Round)
        drawLine(color, Offset(s - inset, inset), Offset(inset, s - inset), stroke, StrokeCap.Round)
    }
}

@Composable
fun ChevronGlyph(
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.faint,
    size: Dp = 12.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = s * 0.09f
        drawLine(color, Offset(s * 0.38f, s * 0.2f), Offset(s * 0.72f, s * 0.5f), stroke, StrokeCap.Round)
        drawLine(color, Offset(s * 0.72f, s * 0.5f), Offset(s * 0.38f, s * 0.8f), stroke, StrokeCap.Round)
    }
}

@Composable
fun BarsGlyph(
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.fg,
    size: Dp = 18.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = s * 0.09f
        val base = s * 0.82f
        val widths = listOf(0.32f, 0.56f, 0.44f)
        widths.forEachIndexed { index, height ->
            val x = s * (0.24f + index * 0.26f)
            drawLine(
                color,
                Offset(x, base),
                Offset(x, s * (0.9f - height)),
                stroke,
                StrokeCap.Round
            )
        }
    }
}

@Composable
fun TuneGlyph(
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.fg,
    size: Dp = 18.dp
) {
    val hole = LocalPalette.current.bg
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = s * 0.08f
        val xs = listOf(0.18f, 0.82f)
        val ys = listOf(0.3f, 0.7f)
        ys.forEachIndexed { row, y ->
            drawLine(
                color,
                Offset(s * xs[0], s * y),
                Offset(s * xs[1], s * y),
                stroke,
                StrokeCap.Round
            )
            val knobX = if (row == 0) s * 0.62f else s * 0.38f
            drawCircle(color, radius = stroke * 1.9f, center = Offset(knobX, s * y))
            drawCircle(hole, radius = stroke * 0.9f, center = Offset(knobX, s * y))
        }
    }
}

@Composable
fun PlayGlyph(
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.fg,
    size: Dp = 14.dp
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        drawLine(color, Offset(s * 0.3f, s * 0.2f), Offset(s * 0.3f, s * 0.8f), s * 0.08f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.3f, s * 0.2f), Offset(s * 0.8f, s * 0.5f), s * 0.08f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.8f, s * 0.5f), Offset(s * 0.3f, s * 0.8f), s * 0.08f, StrokeCap.Round)
    }
}

@Composable
fun TopBar(right: String? = null, rightColor: Color? = null) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppMark(size = 18.dp)
            Spacer(Modifier.width(11.dp))
            Label("SCREEN TIME REMINDER", color = p.fg, size = 10.sp)
            Spacer(Modifier.weight(1f))
            if (right != null) {
                Label(right, color = rightColor ?: p.muted, size = 9.sp)
            }
        }
        Hairline()
    }
}

@Composable
fun AccessNoticeCard(onOpenAccess: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Column(
        modifier
            .fillMaxWidth()
            .border(1.dp, p.line, RoundedCornerShape(3.dp))
            .padding(18.dp)
    ) {
        Label("SCREEN TIME ACCESS NEEDED", color = p.fg, size = 9.sp)
        Spacer(Modifier.height(9.dp))
        Body(
            "Android keeps the screen-time record. Allow usage access and this fills with " +
                "your real day. Nothing leaves the phone.",
            size = 12.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(12.dp))
        TextAction("OPEN ACCESS SETTINGS", onOpenAccess)
    }
}

@Composable
fun RestrictedSettingsCard(
    onOpenAppInfo: () -> Unit,
    onOpenAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalPalette.current
    Column(
        modifier
            .fillMaxWidth()
            .border(1.dp, p.line, RoundedCornerShape(3.dp))
            .background(p.subtle)
            .padding(18.dp)
    ) {
        Label("IF ANDROID BLOCKS THE TOGGLE", color = p.fg, size = 9.sp)
        Spacer(Modifier.height(9.dp))
        Body(
            "Android 13+ blocks usage access for apps you installed from a file (not Play Store). " +
                "That warning — \"personal and financial data at risk\" — is generic, even for screen-time. " +
                "Our app has no internet permission and cannot send anything anywhere; it only reads \"which app was in front and for how long.\"",
            size = 11.sp,
            lineHeight = 17.sp
        )
        Spacer(Modifier.height(10.dp))
        Body(
            "Fix in 15 seconds:",
            size = 11.sp,
            lineHeight = 17.sp,
            color = p.fg,
            weight = androidx.compose.ui.text.font.FontWeight.Medium
        )
        Spacer(Modifier.height(6.dp))
        Body(
            "1) Tap OPEN APP INFO below → 2) Tap the 3-dot menu (top-right) → 3) Allow restricted settings → 4) Go back → OPEN ACCESS SETTINGS → toggle Screen Time Reminder ON.",
            size = 11.sp,
            lineHeight = 17.sp
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton(text = "Open app info", onClick = onOpenAppInfo, modifier = Modifier.weight(1f))
            PrimaryButton(text = "Open access", onClick = onOpenAccess, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Label(
            "Path: Settings → Apps → Screen Time Reminder → ⋮ → Allow restricted settings",
            size = 7.5.sp,
            color = p.faint
        )
    }
}
