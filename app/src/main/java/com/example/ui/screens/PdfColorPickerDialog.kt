package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal data class StudioGradientPreset(
    val name: String,
    val stops: List<Color>
)

internal val PdfStudioGradientPresets = listOf(
    StudioGradientPreset(
        name = "Sunset",
        stops = listOf(Color(0xFFFEF08A), Color(0xFFFB923C), Color(0xFFF43F5E), Color(0xFF9333EA))
    ),
    StudioGradientPreset(
        name = "Ocean",
        stops = listOf(Color(0xFFCCFBF1), Color(0xFF38BDF8), Color(0xFF2563EB), Color(0xFF1E3A8A))
    ),
    StudioGradientPreset(
        name = "Emerald",
        stops = listOf(Color(0xFFDCFCE7), Color(0xFF4ADE80), Color(0xFF16A34A), Color(0xFF14532D))
    ),
    StudioGradientPreset(
        name = "Berry",
        stops = listOf(Color(0xFFFCE7F3), Color(0xFFF472B6), Color(0xFFDB2777), Color(0xFF831843))
    )
)

internal fun rgbToHsv(color: Color): FloatArray {
    val r = color.red.coerceIn(0f, 1f)
    val g = color.green.coerceIn(0f, 1f)
    val b = color.blue.coerceIn(0f, 1f)
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC

    val hue = when {
        delta <= 1e-5f -> 0f
        maxC == r -> 60f * (((g - b) / delta) % 6f)
        maxC == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }

    val sat = if (maxC <= 1e-5f) 0f else (delta / maxC).coerceIn(0f, 1f)
    return floatArrayOf(hue.coerceIn(0f, 360f), sat, maxC.coerceIn(0f, 1f))
}

internal fun generateHueGrades(hue: Float): List<Color> {
    val safeHue = ((hue % 360f) + 360f) % 360f
    val gradeSpecs = listOf(
        0.16f to 0.99f, // Mist tint
        0.34f to 0.98f, // Soft pastel
        0.58f to 0.96f, // Bright tone
        0.84f to 0.94f, // Vivid pure
        0.92f to 0.78f, // Rich medium
        0.95f to 0.58f, // Deep grade
        0.96f to 0.36f  // Dark shade
    )
    return gradeSpecs.map { (sat, value) -> Color.hsv(safeHue, sat, value) }
}

internal fun interpolateGradientColor(stops: List<Color>, fraction: Float): Color {
    if (stops.isEmpty()) return Color.Black
    if (stops.size == 1) return stops.first()
    val f = fraction.coerceIn(0f, 1f)
    val scaled = f * (stops.size - 1)
    val idx = scaled.toInt().coerceIn(0, stops.lastIndex - 1)
    val localT = (scaled - idx).coerceIn(0f, 1f)
    return lerp(stops[idx], stops[idx + 1], localT)
}

internal fun Color.toHexRgbString(): String {
    val rgb = toArgb() and 0xFFFFFF
    return "#%06X".format(rgb)
}

@Composable
internal fun PdfColorGradientButton(
    activeColor: Color,
    isCustomSelected: Boolean,
    size: Dp = 28.dp,
    testTag: String = "color_gradient_picker_button",
    onClick: () -> Unit
) {
    val spectrumColors = remember {
        listOf(
            Color(0xFFEF4444),
            Color(0xFFF59E0B),
            Color(0xFF10B981),
            Color(0xFF06B6D4),
            Color(0xFF3B82F6),
            Color(0xFF8B5CF6),
            Color(0xFFEC4899),
            Color(0xFFEF4444)
        )
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.sweepGradient(spectrumColors))
            .border(
                width = if (isCustomSelected) 2.2.dp else 1.dp,
                color = if (isCustomSelected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.85f),
                shape = CircleShape
            )
            .testTag(testTag)
            .semantics {
                contentDescription = "Open Color Grades and Gradients Picker"
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.5f)
                .clip(CircleShape)
                .background(if (isCustomSelected) activeColor else MaterialTheme.colorScheme.surface)
                .border(1.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!isCustomSelected) {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(size * 0.32f)
                )
            }
        }
    }
}

@Composable
internal fun PdfColorPickerDialog(
    title: String,
    initialColor: Color,
    onSelectColor: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val initialHsv = remember(initialColor) { rgbToHsv(initialColor.copy(alpha = 1f)) }
    var hue by remember(initialColor) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(initialColor) { mutableFloatStateOf(initialHsv[1].coerceAtLeast(0.05f)) }
    var brightness by remember(initialColor) { mutableFloatStateOf(initialHsv[2].coerceAtLeast(0.15f)) }

    val pickedColor = Color.hsv(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), brightness.coerceIn(0f, 1f))
    val huePureColor = Color.hsv(hue.coerceIn(0f, 360f), 1f, 1f)
    val tonalGrades = remember(hue) { generateHueGrades(hue) }
    val spectrumHues = remember {
        (0..6).map { Color.hsv((it * 60f).coerceAtMost(359.9f), 1f, 1f) } + Color.hsv(0f, 1f, 1f)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pdf_color_picker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header & Live Before/After Swatch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${pickedColor.toHexRgbString()} • Hue ${hue.roundToInt()}°",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("color_picker_hex_label")
                        )
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 30.dp, height = 32.dp)
                                .background(initialColor.copy(alpha = 1f))
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 32.dp)
                                .background(pickedColor)
                        )
                    }
                }

                // 2D Saturation × Brightness Grade Pad
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(118.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .testTag("color_grade_pad")
                        .pointerInput(hue) {
                            detectTapGestures { pos ->
                                if (size.width > 0 && size.height > 0) {
                                    saturation = (pos.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    brightness = (1f - (pos.y / size.height.toFloat())).coerceIn(0f, 1f)
                                }
                            }
                        }
                        .pointerInput(hue) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                if (size.width > 0 && size.height > 0) {
                                    saturation = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    brightness = (1f - (change.position.y / size.height.toFloat())).coerceIn(0f, 1f)
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(listOf(Color.White, huePureColor)),
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                        )
                        drawRoundRect(
                            brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)),
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                        )
                        val cx = (saturation * size.width).coerceIn(10.dp.toPx(), size.width - 10.dp.toPx())
                        val cy = ((1f - brightness) * size.height).coerceIn(10.dp.toPx(), size.height - 10.dp.toPx())
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.35f),
                            radius = 10.dp.toPx(),
                            center = Offset(cx, cy),
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 9.dp.toPx(),
                            center = Offset(cx, cy),
                            style = Stroke(width = 2.2.dp.toPx())
                        )
                        drawCircle(
                            color = pickedColor,
                            radius = 6.5.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                    }
                }

                // Continuous Hue Rainbow Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Spectrum Hue",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), RoundedCornerShape(13.dp))
                            .testTag("hue_spectrum_bar")
                            .pointerInput(Unit) {
                                detectTapGestures { pos ->
                                    if (size.width > 0) {
                                        hue = ((pos.x / size.width.toFloat()).coerceIn(0f, 1f) * 359.5f)
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    if (size.width > 0) {
                                        hue = ((change.position.x / size.width.toFloat()).coerceIn(0f, 1f) * 359.5f)
                                    }
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRoundRect(
                                brush = Brush.horizontalGradient(spectrumHues),
                                cornerRadius = CornerRadius(13.dp.toPx(), 13.dp.toPx())
                            )
                            val tx = ((hue / 360f) * size.width).coerceIn(11.dp.toPx(), size.width - 11.dp.toPx())
                            val ty = size.height / 2f
                            drawCircle(
                                color = Color.White,
                                radius = 10.dp.toPx(),
                                center = Offset(tx, ty),
                                style = Stroke(width = 3.dp.toPx())
                            )
                            drawCircle(
                                color = huePureColor,
                                radius = 7.5.dp.toPx(),
                                center = Offset(tx, ty)
                            )
                        }
                    }
                }

                // Tonal Color Grades (Light tint to Deep shade of current hue)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Tonal Color Grades",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tonalGrades.forEachIndexed { idx, gradeColor ->
                            val isSelected = abs(pickedColor.red - gradeColor.red) < 0.04f &&
                                abs(pickedColor.green - gradeColor.green) < 0.04f &&
                                abs(pickedColor.blue - gradeColor.blue) < 0.04f
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(gradeColor)
                                    .border(
                                        width = if (isSelected) 2.2.dp else 0.8.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .testTag("grade_swatch_$idx")
                                    .clickable {
                                        val gHsv = rgbToHsv(gradeColor)
                                        hue = gHsv[0]
                                        saturation = gHsv[1]
                                        brightness = gHsv[2]
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected grade",
                                        tint = if (idx < 3) Color.Black else Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Interactive Gradient Strips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Gradient Swatches (Tap or slide)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    PdfStudioGradientPresets.forEach { preset ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                preset.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(54.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(Brush.horizontalGradient(preset.stops))
                                    .border(
                                        0.8.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(11.dp)
                                    )
                                    .testTag("gradient_bar_${preset.name.lowercase()}")
                                    .pointerInput(preset) {
                                        detectTapGestures { pos ->
                                            if (size.width > 0) {
                                                val chosen = interpolateGradientColor(
                                                    preset.stops,
                                                    pos.x / size.width.toFloat()
                                                )
                                                val hsv = rgbToHsv(chosen)
                                                hue = hsv[0]
                                                saturation = hsv[1]
                                                brightness = hsv[2]
                                            }
                                        }
                                    }
                                    .pointerInput(preset) {
                                        detectDragGestures { change, _ ->
                                            change.consume()
                                            if (size.width > 0) {
                                                val chosen = interpolateGradientColor(
                                                    preset.stops,
                                                    change.position.x / size.width.toFloat()
                                                )
                                                val hsv = rgbToHsv(chosen)
                                                hue = hsv[0]
                                                saturation = hsv[1]
                                                brightness = hsv[2]
                                            }
                                        }
                                    }
                            )
                        }
                    }
                }

                // Dialog Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSelectColor(pickedColor) },
                        modifier = Modifier.testTag("apply_picked_color_button")
                    ) {
                        Text("Apply Color")
                    }
                }
            }
        }
    }
}
