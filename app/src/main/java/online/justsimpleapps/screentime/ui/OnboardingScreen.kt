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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.justsimpleapps.screentime.data.ScreenTimeStore
import online.justsimpleapps.screentime.ui.components.AppMark
import online.justsimpleapps.screentime.ui.components.Body
import online.justsimpleapps.screentime.ui.components.GhostButton
import online.justsimpleapps.screentime.ui.components.Hairline
import online.justsimpleapps.screentime.ui.components.Headline
import online.justsimpleapps.screentime.ui.components.Label
import online.justsimpleapps.screentime.ui.components.Mono
import online.justsimpleapps.screentime.ui.components.PrimaryButton
import online.justsimpleapps.screentime.ui.theme.LocalPalette

@Composable
fun OnboardingScreen(
    store: ScreenTimeStore,
    onAllowAccess: () -> Unit,
    onDone: () -> Unit
) {
    val p = LocalPalette.current
    val granted = store.accessGranted

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
    ) {
        Column(Modifier.padding(horizontal = 24.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppMark(size = 20.dp)
                Spacer(Modifier.width(11.dp))
                Label("SCREEN TIME REMINDER", color = p.fg, size = 10.sp)
                Spacer(Modifier.weight(1f))
                Label("JUST SIMPLE APPS", size = 8.5.sp, color = p.faint)
            }

            Spacer(Modifier.height(44.dp))
            Label("WELCOME TO A SIMPLER APP", size = 9.sp)
            Spacer(Modifier.height(14.dp))
            Headline("See where\nyour time goes.", size = 38.sp)
            Spacer(Modifier.height(16.dp))
            Body(
                "Screen Time Reminder reads the screen-time record already on this phone. " +
                    "It adds nothing to your feed, and nothing leaves the device.",
                size = 13.sp,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(30.dp))
            Step("01", "ALLOW SCREEN TIME ACCESS", "Android keeps the record. We only read it, on this phone.")
            Hairline()
            Step("02", "WE WATCH THE LONG SESSIONS", "A quiet count of continuous screen time, session by session.")
            Hairline()
            Step("03", "YOU GET ONE CALM NUDGE", "Step away for five minutes. Your day keeps its own tally.")

            Spacer(Modifier.height(34.dp))
            if (granted) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(p.fg))
                    Spacer(Modifier.width(8.dp))
                    Label("ACCESS GRANTED", color = p.fg, size = 9.sp)
                    Spacer(Modifier.weight(1f))
                    Mono("READY", size = 11.sp, color = p.muted)
                }
                Spacer(Modifier.height(16.dp))
                PrimaryButton(text = "Continue", onClick = onDone)
            } else {
                PrimaryButton(text = "Allow screen time access", onClick = onAllowAccess)
                Spacer(Modifier.height(10.dp))
                GhostButton(text = "Not now", onClick = onDone)
                Spacer(Modifier.height(14.dp))
                Body(
                    "Android opens its settings. Find Screen Time Reminder in the list and allow " +
                        "usage access. You can change this later in Settings.",
                    size = 11.sp,
                    lineHeight = 16.sp
                )
            }
            Spacer(Modifier.height(36.dp))
            Label("NO ADS · NO TRACKING · NO ACCOUNT", color = p.faint, size = 8.5.sp)
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun Step(number: String, title: String, copy: String) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Mono(number, size = 10.sp, color = p.faint)
        Spacer(Modifier.width(18.dp))
        Column {
            Label(title, color = p.fg, size = 9.sp)
            Spacer(Modifier.height(6.dp))
            Body(copy, size = 11.5.sp, lineHeight = 17.sp)
        }
    }
}
