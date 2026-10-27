package online.justsimpleapps.screentime.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import online.justsimpleapps.screentime.data.ThemeMode

@Immutable
data class Palette(
    val bg: Color,
    val fg: Color,
    val muted: Color,
    val faint: Color,
    val line: Color,
    val subtle: Color,
    val card: Color,
    val cardFg: Color,
    val cardMuted: Color,
    val cardLine: Color,
    val accent: Color,
    val lime: Color,
    val isDark: Boolean
)

val IconBlue = Color(0xFF03B1C2)

/** Warm felt with a teal leather card in either appearance. */
val LightPalette = Palette(
    bg = Color(0xFFF0E3CC), fg = Color(0xFF272D27),
    muted = Color(0xFF625B4E), faint = Color(0xFF756B5B),
    line = Color(0x302D352D), subtle = Color(0xFFE1D0B3),
    card = IconBlue, cardFg = Color(0xFF102C2C),
    cardMuted = Color(0xFF173D40), cardLine = Color(0x40102C2C),
    accent = IconBlue, lime = IconBlue, isDark = false
)

val DarkPalette = Palette(
    bg = Color(0xFF171B19), fg = Color(0xFFF7F2E8),
    muted = Color(0xFFBEB7A8), faint = Color(0xFF9D9688),
    line = Color(0x30F7F2E8), subtle = Color(0xFF282F29),
    card = IconBlue, cardFg = Color(0xFF102C2C),
    cardMuted = Color(0xFF173D40), cardLine = Color(0x40102C2C),
    accent = IconBlue, lime = IconBlue, isDark = true
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

@Composable
fun ScreenTimeTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }
    val palette = if (dark) DarkPalette else LightPalette
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    val scheme = if (dark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.bg,
            background = palette.bg,
            onBackground = palette.fg,
            surface = palette.bg,
            onSurface = palette.fg
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.bg,
            background = palette.bg,
            onBackground = palette.fg,
            surface = palette.bg,
            onSurface = palette.fg
        )
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme) {
            content()
        }
    }
}
