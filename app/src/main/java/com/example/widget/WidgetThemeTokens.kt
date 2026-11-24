package com.example.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider as GlanceColorProvider

object WidgetThemeTokens {
    val WidgetSurface: GlanceColorProvider = ColorProvider(
        day = Color(0xFFFAF7F2),
        night = Color(0xFF040708)
    )

    val CardSurface: GlanceColorProvider = ColorProvider(
        day = Color(0xFFFFFFFF),
        night = Color(0xFF0F1417)
    )

    val CardSurfaceAlt: GlanceColorProvider = ColorProvider(
        day = Color(0xFFF3ECE4),
        night = Color(0xFF151D21)
    )

    val TextPrimary: GlanceColorProvider = ColorProvider(
        day = Color(0xFF1F1C19),
        night = Color(0xFFF3F7F9)
    )

    val TextSecondary: GlanceColorProvider = ColorProvider(
        day = Color(0xFF645C55),
        night = Color(0xFF94A3AC)
    )

    val PrimaryAccent: GlanceColorProvider = ColorProvider(
        day = Color(0xFF4F46E5),
        night = Color(0xFF00D8F6)
    )

    val PrimaryContainer: GlanceColorProvider = ColorProvider(
        day = Color(0xFFEEF2FF),
        night = Color(0xFF0B353F)
    )

    val OnPrimaryAccent: GlanceColorProvider = ColorProvider(
        day = Color(0xFFFFFFFF),
        night = Color(0xFF001C22)
    )

    val VioletAccent: GlanceColorProvider = ColorProvider(
        day = Color(0xFF6E5AE0),
        night = Color(0xFFB39DFB)
    )

    val VioletContainer: GlanceColorProvider = ColorProvider(
        day = Color(0xFFEDE9FE),
        night = Color(0xFF342758)
    )

    val AmberAccent: GlanceColorProvider = ColorProvider(
        day = Color(0xFFD97706),
        night = Color(0xFFFBBF24)
    )

    val AmberContainer: GlanceColorProvider = ColorProvider(
        day = Color(0xFFFEF3C7),
        night = Color(0xFF3F2E14)
    )

    val EmeraldAccent: GlanceColorProvider = ColorProvider(
        day = Color(0xFF059669),
        night = Color(0xFF34D399)
    )

    val EmeraldContainer: GlanceColorProvider = ColorProvider(
        day = Color(0xFFECFDF5),
        night = Color(0xFF143127)
    )

    val CoralAccent: GlanceColorProvider = ColorProvider(
        day = Color(0xFFE11D48),
        night = Color(0xFFFB7185)
    )

    val CoralContainer: GlanceColorProvider = ColorProvider(
        day = Color(0xFFFFE4E6),
        night = Color(0xFF3E1824)
    )

    val SkyAccent: GlanceColorProvider = ColorProvider(
        day = Color(0xFF2563EB),
        night = Color(0xFF60A5FA)
    )

    val SkyContainer: GlanceColorProvider = ColorProvider(
        day = Color(0xFFEFF6FF),
        night = Color(0xFF172B47)
    )

    val TerracottaAccent: GlanceColorProvider = ColorProvider(
        day = Color(0xFFEA580C),
        night = Color(0xFFFB923C)
    )

    val TerracottaContainer: GlanceColorProvider = ColorProvider(
        day = Color(0xFFFFEDD5),
        night = Color(0xFF3E2214)
    )

    val ProgressTrack: GlanceColorProvider = ColorProvider(
        day = Color(0xFFE6E0D8),
        night = Color(0xFF3B322D)
    )
}
