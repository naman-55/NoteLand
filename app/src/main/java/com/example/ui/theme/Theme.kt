package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val DarkColorScheme = darkColorScheme(
    primary = NotePrimaryDark,
    onPrimary = NoteOnPrimaryDark,
    primaryContainer = NotePrimaryContainerDark,
    onPrimaryContainer = NoteOnPrimaryContainerDark,
    secondary = NoteSecondaryDark,
    onSecondary = NoteOnSecondaryDark,
    secondaryContainer = NoteSecondaryContainerDark,
    onSecondaryContainer = NoteOnSecondaryContainerDark,
    tertiary = NoteTertiaryDark,
    onTertiary = NoteOnTertiaryDark,
    background = NoteBackgroundDark,
    onBackground = NoteOnBackgroundDark,
    surface = NoteSurfaceDark,
    onSurface = NoteOnSurfaceDark,
    surfaceVariant = NoteSurfaceVariantDark,
    onSurfaceVariant = NoteOnSurfaceVariantDark,
    surfaceContainerLowest = NoteBackgroundDark,
    surfaceContainerLow = NoteSurfaceContainerLowDark,
    surfaceContainer = NoteSurfaceContainerDark,
    surfaceContainerHigh = NoteSurfaceContainerHighDark,
    outline = NoteOutlineDark,
    outlineVariant = NoteOutlineVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = NotePrimaryLight,
    onPrimary = NoteOnPrimaryLight,
    primaryContainer = NotePrimaryContainerLight,
    onPrimaryContainer = NoteOnPrimaryContainerLight,
    secondary = NoteSecondaryLight,
    onSecondary = NoteOnSecondaryLight,
    secondaryContainer = NoteSecondaryContainerLight,
    onSecondaryContainer = NoteOnSecondaryContainerLight,
    tertiary = NoteTertiaryLight,
    onTertiary = NoteOnTertiaryLight,
    background = NoteBackgroundLight,
    onBackground = NoteOnBackgroundLight,
    surface = NoteSurfaceLight,
    onSurface = NoteOnSurfaceLight,
    surfaceVariant = NoteSurfaceVariantLight,
    onSurfaceVariant = NoteOnSurfaceVariantLight,
    surfaceContainerLowest = NoteSurfaceLight,
    surfaceContainerLow = NoteSurfaceContainerLowLight,
    surfaceContainer = NoteSurfaceContainerLight,
    surfaceContainerHigh = NoteSurfaceContainerHighLight,
    outline = NoteOutlineLight,
    outlineVariant = NoteOutlineVariantLight
)

private val IvoryColorScheme = lightColorScheme(
    primary = NotePrimaryIvory,
    onPrimary = NoteOnPrimaryIvory,
    primaryContainer = NotePrimaryContainerIvory,
    onPrimaryContainer = NoteOnPrimaryContainerIvory,
    secondary = NoteSecondaryIvory,
    onSecondary = NoteOnSecondaryIvory,
    secondaryContainer = NoteSecondaryContainerIvory,
    onSecondaryContainer = NoteOnSecondaryContainerIvory,
    tertiary = NoteTertiaryIvory,
    onTertiary = NoteOnTertiaryIvory,
    background = NoteBackgroundIvory,
    onBackground = NoteOnBackgroundIvory,
    surface = NoteSurfaceIvory,
    onSurface = NoteOnSurfaceIvory,
    surfaceVariant = NoteSurfaceVariantIvory,
    onSurfaceVariant = NoteOnSurfaceVariantIvory,
    surfaceContainerLowest = NoteSurfaceIvory,
    surfaceContainerLow = NoteSurfaceContainerLowIvory,
    surfaceContainer = NoteSurfaceContainerIvory,
    surfaceContainerHigh = NoteSurfaceContainerHighIvory,
    outline = NoteOutlineIvory,
    outlineVariant = NoteOutlineVariantIvory
)

enum class AppThemeMode(val key: String, val displayName: String) {
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    IVORY("ivory", "Ivory");

    companion object {
        fun fromKey(key: String?): AppThemeMode = when (key?.lowercase()) {
            "dark" -> DARK
            "ivory" -> IVORY
            else -> LIGHT
        }
    }
}

enum class ScreenBackdropTone {
    DEFAULT,
    FOLDERS_SKY,
    STUDIO_LAVENDER,
    PRODUCTIVITY_MINT,
    SETTINGS_EMERALD
}

val isAppInDarkTheme: Boolean
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface.luminance() < 0.5f

@Composable
@ReadOnlyComposable
fun editorialBackdropBrush(
    isDark: Boolean = isAppInDarkTheme,
    tone: ScreenBackdropTone = ScreenBackdropTone.DEFAULT
): Brush {
    val bottomBackground = MaterialTheme.colorScheme.background
    val isIvory = MaterialTheme.colorScheme.background == NoteBackgroundIvory
    return if (isDark) {
        val (topStop, midStop) = when (tone) {
            ScreenBackdropTone.FOLDERS_SKY -> EditorialBackdropFoldersDarkTop to EditorialBackdropFoldersDarkMid
            ScreenBackdropTone.STUDIO_LAVENDER -> EditorialBackdropStudioDarkTop to EditorialBackdropStudioDarkMid
            ScreenBackdropTone.PRODUCTIVITY_MINT -> EditorialBackdropProductivityDarkTop to EditorialBackdropProductivityDarkMid
            ScreenBackdropTone.SETTINGS_EMERALD -> EditorialBackdropSettingsDarkTop to EditorialBackdropSettingsDarkMid
            ScreenBackdropTone.DEFAULT -> EditorialBackdropDarkTop to EditorialBackdropDarkMid
        }
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to topStop,
                0.24f to midStop,
                0.52f to NoteBackgroundDark,
                1.00f to NoteBackgroundDark
            )
        )
    } else if (isIvory) {
        val (topStop, midStop) = when (tone) {
            ScreenBackdropTone.FOLDERS_SKY -> Color(0xFFEBE3D0) to Color(0xFFF2EBDC)
            ScreenBackdropTone.STUDIO_LAVENDER -> Color(0xFFEEE4E8) to Color(0xFFF4EDDF)
            ScreenBackdropTone.PRODUCTIVITY_MINT -> Color(0xFFE4EBDC) to Color(0xFFEFF0DC)
            ScreenBackdropTone.SETTINGS_EMERALD -> Color(0xFFE0EBDC) to Color(0xFFEDEFDB)
            ScreenBackdropTone.DEFAULT -> Color(0xFFEFE8DA) to Color(0xFFF5EFE0)
        }
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to topStop,
                0.26f to midStop,
                0.54f to bottomBackground,
                1.00f to bottomBackground
            )
        )
    } else {
        val (topStop, midStop) = when (tone) {
            ScreenBackdropTone.FOLDERS_SKY -> EditorialBackdropFoldersLightTop to EditorialBackdropFoldersLightMid
            ScreenBackdropTone.STUDIO_LAVENDER -> EditorialBackdropStudioLightTop to EditorialBackdropStudioLightMid
            ScreenBackdropTone.PRODUCTIVITY_MINT -> EditorialBackdropProductivityLightTop to EditorialBackdropProductivityLightMid
            ScreenBackdropTone.SETTINGS_EMERALD -> EditorialBackdropSettingsLightTop to EditorialBackdropSettingsLightMid
            ScreenBackdropTone.DEFAULT -> EditorialBackdropLightTop to EditorialBackdropLightMid
        }
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to topStop,
                0.26f to midStop,
                0.54f to bottomBackground,
                1.00f to bottomBackground
            )
        )
    }
}

@Composable
@ReadOnlyComposable
fun editorialHeaderTextColor(isDark: Boolean = isAppInDarkTheme): Color =
    MaterialTheme.colorScheme.onBackground

@Composable
@ReadOnlyComposable
fun editorialHeaderSubColor(isDark: Boolean = isAppInDarkTheme): Color =
    MaterialTheme.colorScheme.onSurfaceVariant

@Composable
@ReadOnlyComposable
fun editorialCardContainerColor(isDark: Boolean = isAppInDarkTheme): Color =
    MaterialTheme.colorScheme.surface

@Composable
@ReadOnlyComposable
fun editorialCardBorderColor(isDark: Boolean = isAppInDarkTheme): Color = when {
    isDark -> NoteOutlineDark
    MaterialTheme.colorScheme.background == NoteBackgroundIvory -> Color(0xFFCBBFA8)
    else -> Color(0xFFBACAC2)
}

val LocalAppTextScaled = compositionLocalOf { false }

@Composable
fun NoteLandTheme(
    darkTheme: Boolean = false,
    themeMode: AppThemeMode = if (darkTheme) AppThemeMode.DARK else AppThemeMode.LIGHT,
    dynamicColor: Boolean = true,
    textSizeSp: Float = 16.5f,
    content: @Composable () -> Unit
) {
    val isDark = themeMode == AppThemeMode.DARK
    val colorScheme = when {
        themeMode == AppThemeMode.IVORY -> IvoryColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) {
                val dyn = dynamicDarkColorScheme(context)
                DarkColorScheme.copy(
                    primary = dyn.primary,
                    onPrimary = dyn.onPrimary,
                    primaryContainer = dyn.primaryContainer,
                    onPrimaryContainer = dyn.onPrimaryContainer,
                    secondary = dyn.secondary,
                    onSecondary = dyn.onSecondary,
                    secondaryContainer = dyn.secondaryContainer,
                    onSecondaryContainer = dyn.onSecondaryContainer,
                    tertiary = dyn.tertiary,
                    onTertiary = dyn.onTertiary
                )
            } else {
                val dyn = dynamicLightColorScheme(context)
                LightColorScheme.copy(
                    primary = dyn.primary,
                    onPrimary = dyn.onPrimary,
                    primaryContainer = dyn.primaryContainer,
                    onPrimaryContainer = dyn.onPrimaryContainer,
                    secondary = dyn.secondary,
                    onSecondary = dyn.onSecondary,
                    secondaryContainer = dyn.secondaryContainer,
                    onSecondaryContainer = dyn.onSecondaryContainer,
                    tertiary = dyn.tertiary,
                    onTertiary = dyn.onTertiary
                )
            }
        }
        themeMode == AppThemeMode.DARK -> DarkColorScheme
        else -> LightColorScheme
    }

    val currentDensity = LocalDensity.current
    val textScale = (textSizeSp / 16.5f).coerceIn(0.82f, 1.25f)
    val scaledDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * textScale
    )

    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalAppTextScaled provides true
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
