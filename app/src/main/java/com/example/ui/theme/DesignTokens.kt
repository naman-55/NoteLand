package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Semantic spacing tokens aligned to the 4dp / 8dp Material Design 3 grid.
 */
object AppSpacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val section: Dp = 32.dp
}

/**
 * Consistent corner radius hierarchy across all cards, sheets, and controls.
 */
object AppRadius {
    val sm: Dp = 10.dp
    val md: Dp = 14.dp
    val lg: Dp = 18.dp
    val xl: Dp = 22.dp
    val pill: Dp = 999.dp
}

/**
 * Restrained Material You elevation levels (light mode subtle shadows, dark mode border + tonal step).
 */
object AppElevation {
    val none: Dp = 0.dp
    val low: Dp = 2.dp
    val card: Dp = 4.dp
    val elevated: Dp = 8.dp
    val navBarLight: Dp = 10.dp
    val navBarDark: Dp = 16.dp
}

/**
 * Primary navigation sections, each carrying its dedicated Light & Dark visual identity.
 */
enum class AppSection {
    HOME,
    FOLDERS,
    STUDIO,
    PRODUCTIVITY,
    SETTINGS
}

@Immutable
data class SectionIdentityColors(
    val section: AppSection,
    val accent: Color,
    val onAccent: Color,
    val tonalContainer: Color,
    val onTonalContainer: Color,
    val secondaryTint: Color,
    val deepGradientStop: Color
)

object SectionColorPalette {
    fun forSection(section: AppSection, isDark: Boolean): SectionIdentityColors = when (section) {
        AppSection.HOME -> if (isDark) {
            SectionIdentityColors(
                section = AppSection.HOME,
                accent = DarkHomeAccent,
                onAccent = Color(0xFF072110),
                tonalContainer = Color(0xFF173623),
                onTonalContainer = Color(0xFFB3E68D),
                secondaryTint = Color(0xFFA3E635),
                deepGradientStop = Color(0xFF1B7A43)
            )
        } else {
            SectionIdentityColors(
                section = AppSection.HOME,
                accent = LightHomeAccent,
                onAccent = Color.White,
                tonalContainer = Color(0xFFDCFCE7),
                onTonalContainer = Color(0xFF0D4A27),
                secondaryTint = Color(0xFF84CC16),
                deepGradientStop = Color(0xFF0F6D39)
            )
        }

        AppSection.FOLDERS -> if (isDark) {
            SectionIdentityColors(
                section = AppSection.FOLDERS,
                accent = DarkFoldersAccent,
                onAccent = Color(0xFF082247),
                tonalContainer = Color(0xFF162944),
                onTonalContainer = Color(0xFFBFDBFE),
                secondaryTint = Color(0xFF38BDF8),
                deepGradientStop = Color(0xFF2563EB)
            )
        } else {
            SectionIdentityColors(
                section = AppSection.FOLDERS,
                accent = LightFoldersAccent,
                onAccent = Color.White,
                tonalContainer = Color(0xFFDBEAFE),
                onTonalContainer = Color(0xFF1E3A8A),
                secondaryTint = Color(0xFF0EA5E9),
                deepGradientStop = Color(0xFF1D4ED8)
            )
        }

        AppSection.STUDIO -> if (isDark) {
            SectionIdentityColors(
                section = AppSection.STUDIO,
                accent = DarkStudioAccent,
                onAccent = Color(0xFF211042),
                tonalContainer = Color(0xFF271D40),
                onTonalContainer = Color(0xFFE9D5FF),
                secondaryTint = Color(0xFFC4B5FD),
                deepGradientStop = Color(0xFF6D28D9)
            )
        } else {
            SectionIdentityColors(
                section = AppSection.STUDIO,
                accent = LightStudioAccent,
                onAccent = Color.White,
                tonalContainer = Color(0xFFEDE9FE),
                onTonalContainer = Color(0xFF4C1D95),
                secondaryTint = Color(0xFFA78BFA),
                deepGradientStop = Color(0xFF6D28D9)
            )
        }

        AppSection.PRODUCTIVITY -> if (isDark) {
            SectionIdentityColors(
                section = AppSection.PRODUCTIVITY,
                accent = DarkProductivityAccent,
                onAccent = Color(0xFF062110),
                tonalContainer = Color(0xFF173322),
                onTonalContainer = Color(0xFFBBF7D0),
                secondaryTint = Color(0xFF2DD4BF),
                deepGradientStop = Color(0xFF247A44)
            )
        } else {
            SectionIdentityColors(
                section = AppSection.PRODUCTIVITY,
                accent = LightProductivityAccent,
                onAccent = Color.White,
                tonalContainer = Color(0xFFDCFCE7),
                onTonalContainer = Color(0xFF14532D),
                secondaryTint = Color(0xFF0D9488),
                deepGradientStop = Color(0xFF116631)
            )
        }

        AppSection.SETTINGS -> if (isDark) {
            SectionIdentityColors(
                section = AppSection.SETTINGS,
                accent = DarkSettingsAccent,
                onAccent = Color(0xFF05241F),
                tonalContainer = Color(0xFF14332E),
                onTonalContainer = Color(0xFF99F6E4),
                secondaryTint = Color(0xFF5EEAD4),
                deepGradientStop = Color(0xFF1A7869)
            )
        } else {
            SectionIdentityColors(
                section = AppSection.SETTINGS,
                accent = LightSettingsAccent,
                onAccent = Color.White,
                tonalContainer = Color(0xFFCCFBF1),
                onTonalContainer = Color(0xFF115E59),
                secondaryTint = Color(0xFF0F766E),
                deepGradientStop = Color(0xFF0A7D6C)
            )
        }
    }
}

val LocalSectionColors = staticCompositionLocalOf {
    SectionColorPalette.forSection(AppSection.HOME, isDark = false)
}

val currentSectionColors: SectionIdentityColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSectionColors.current

@Composable
fun ProvideSectionTheme(
    section: AppSection,
    isDark: Boolean = isAppInDarkTheme,
    content: @Composable () -> Unit
) {
    val sectionColors = SectionColorPalette.forSection(section, isDark)
    CompositionLocalProvider(LocalSectionColors provides sectionColors, content = content)
}
