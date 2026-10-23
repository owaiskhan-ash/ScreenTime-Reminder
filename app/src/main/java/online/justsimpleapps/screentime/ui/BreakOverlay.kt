package online.justsimpleapps.screentime.ui

import online.justsimpleapps.screentime.ui.theme.textile
import online.justsimpleapps.screentime.ui.theme.Textile
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import online.justsimpleapps.screentime.data.ScreenTimeStore
import online.justsimpleapps.screentime.ui.components.AppMark
import online.justsimpleapps.screentime.ui.components.Body
import online.justsimpleapps.screentime.ui.components.CloseGlyph
import online.justsimpleapps.screentime.ui.components.GhostButton
import online.justsimpleapps.screentime.ui.components.Headline
import online.justsimpleapps.screentime.ui.components.Label
import online.justsimpleapps.screentime.ui.components.Mono
import online.justsimpleapps.screentime.ui.components.PrimaryButton
import online.justsimpleapps.screentime.ui.theme.LocalPalette
import online.justsimpleapps.screentime.util.formatHm
import online.justsimpleapps.screentime.util.formatTimer

private const val BREAK_MS = 5 * 60_000L

@Composable
fun BreakOverlay(
    store: ScreenTimeStore,
    onClose: () -> Unit,
    onStartBreak: () -> Unit,
    onSnooze: () -> Unit
) {
    val p = LocalPalette.current
    val sessionBefore = remember { store.snapshot.continuousMs }
    val breaksToday = store.snapshot.breaks
    var running by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var remaining by remember { mutableStateOf(BREAK_MS) }

    LaunchedEffect(running, finished) {
        if (running && !finished) {
            while (remaining > 0L) {
                delay(200L)
                remaining -= 200L
            }
            remaining = 0L
            running = false
            finished = true
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .textile(p.bg, Textile.SUEDE)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
    ) {
        Column(Modifier.padding(horizontal = 24.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppMark(size = 18.dp)
                Spacer(Modifier.width(11.dp))
                Label("BREAK", color = p.fg, size = 10.sp)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, p.line, CircleShape)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    CloseGlyph(size = 14.dp, color = p.fg)
                }
            }

            Spacer(Modifier.height(26.dp))
            Label(if (finished) "BREAK COMPLETE" else "STEP AWAY", size = 9.sp)
            Spacer(Modifier.height(12.dp))
            Headline(
                if (finished) "Welcome\nback." else "Look up.",
                size = 40.sp
            )
            Spacer(Modifier.height(12.dp))
            Body(
                when {
                    finished -> "Five minutes for your eyes and your attention. That is the whole idea."
                    sessionBefore > 0L -> "You have been on screen for ${formatHm(sessionBefore)}. " +
                        "Give your eyes five minutes of distance."
                    else -> "Stand up, stretch, and look at something far away. " +
                        "Five minutes is enough to reset."
                },
                size = 13.sp,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(30.dp))
            Box(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                BreakDial(
                    progress = when {
                        finished -> 1f
                        running -> 1f - remaining.toFloat() / BREAK_MS
                        else -> 0f
                    },
                    remainingMs = if (!running && !finished) BREAK_MS else remaining,
                    label = when {
                        finished -> "DONE"
                        running -> "RESTING"
                        else -> "5 MINUTES"
                    }
                )
            }
            Spacer(Modifier.height(30.dp))

            when {
                finished -> {
                    PrimaryButton(text = "Back to today", onClick = onClose)
                }
                running -> {
                    PrimaryButton(text = "I'm back early", onClick = onClose)
                    Spacer(Modifier.height(10.dp))
                    Body(
                        "The break already counts. Leaving early is your call.",
                        size = 11.sp,
                        color = p.muted
                    )
                }
                else -> {
                    PrimaryButton(
                        text = "Start a 5-min break",
                        onClick = {
                            onStartBreak()
                            running = true
                            remaining = BREAK_MS
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    GhostButton(text = "Snooze 15 minutes", onClick = onSnooze)
                }
            }

            Spacer(Modifier.height(28.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .textile(p.card, Textile.LEATHER, stitched = true)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Mono("20·20·20", size = 14.sp, color = p.cardFg)
                Spacer(Modifier.width(16.dp))
                Body(
                    "Every 20 minutes, look 20 feet away for 20 seconds.",
                    color = p.cardMuted,
                    size = 11.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Label(
                    "$breaksToday BREAKS TODAY · NOTHING LEAVES THIS PHONE",
                    size = 8.sp,
                    color = p.faint
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun BreakDial(progress: Float, remainingMs: Long, label: String) {
    val p = LocalPalette.current
    Box(Modifier.size(196.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 2.dp.toPx()
            val inset = 2.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(
                color = p.line,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke)
            )
            drawArc(
                color = p.fg,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Mono(formatTimer(remainingMs), size = 34.sp, color = p.fg)
            Spacer(Modifier.height(10.dp))
            Label(label, size = 8.5.sp, color = p.muted)
        }
    }
}
