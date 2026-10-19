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
    val isDark: Boolean
)

/** Pure Light: white paper, black ink, one hairline. */
val LightPalette = Palette(
    bg = Color(0xFFFFFFFF),
    fg = Color(0xFF0B0B0C),
    muted = Color(0xFF6E6E73),
    faint = Color(0xFF9A9AA0),
    line = Color(0x1A000000),
    subtle = Color(0xFFF7F6F3),
    card = Color(0xFF0B0B0C),
    cardFg = Color(0xFFFAF9F6),
    cardMuted = Color(0x99FAF9F6),
    cardLine = Color(0x29FFFFFF),
    isDark = false
)

/** Deep Dark: the same two materials, swapped. */
val DarkPalette = Palette(
    bg = Color(0xFF0B0B0C),
    fg = Color(0xFFF4F3EF),
    muted = Color(0xFF9A9AA0),
    faint = Color(0xFF6E6E73),
    line = Color(0x24FFFFFF),
    subtle = Color(0xFF141416),
    card = Color(0xFFF4F3EF),
    cardFg = Color(0xFF0B0B0C),
    cardMuted = Color(0x990B0B0C),
    cardLine = Color(0x1F000000),
    isDark = true
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
            primary = palette.fg,
            onPrimary = palette.bg,
            background = palette.bg,
            onBackground = palette.fg,
            surface = palette.bg,
            onSurface = palette.fg
        )
    } else {
        lightColorScheme(
            primary = palette.fg,
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
