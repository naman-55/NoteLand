package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.NoteColorPresets
import com.example.ui.theme.NoteGradientPresets
import com.example.ui.theme.resolveNoteBackgroundBrush
import java.io.File

data class NoteTextureOption(
    val key: String,
    val label: String
)

val NoteTextureOptions = listOf(
    NoteTextureOption("none", "Clean"),
    NoteTextureOption("dot_grid", "Dot Grid"),
    NoteTextureOption("ruled", "Ruled"),
    NoteTextureOption("graph_grid", "Graph"),
    NoteTextureOption("linen", "Paper Grain"),
    NoteTextureOption("diagonal", "Diagonal"),
    NoteTextureOption("soft_glow", "Soft Glow")
)

fun Modifier.drawNoteTexture(
    textureKey: String,
    isDark: Boolean,
    subtle: Boolean = false
): Modifier {
    val key = textureKey.lowercase().trim()
    if (key.isBlank() || key == "none") return this
    return this.drawBehind {
        val baseInk = if (isDark) Color.White else Color(0xFF0F172A)
        val factor = if (subtle) 0.65f else 1f
        when (key) {
            "dot_grid" -> {
                val step = 18.dp.toPx()
                val radius = 1.25.dp.toPx()
                val dotColor = baseInk.copy(alpha = (if (isDark) 0.14f else 0.13f) * factor)
                var y = step * 0.6f
                while (y < size.height) {
                    var x = step * 0.6f
                    while (x < size.width) {
                        drawCircle(color = dotColor, radius = radius, center = Offset(x, y))
                        x += step
                    }
                    y += step
                }
            }
            "ruled" -> {
                val step = 28.dp.toPx()
                val lineColor = baseInk.copy(alpha = (if (isDark) 0.11f else 0.10f) * factor)
                val stroke = 0.85.dp.toPx()
                var y = step
                while (y < size.height) {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = stroke
                    )
                    y += step
                }
                if (!subtle) {
                    val marginX = 28.dp.toPx()
                    drawLine(
                        color = Color(0xFFEF4444).copy(alpha = if (isDark) 0.16f else 0.14f),
                        start = Offset(marginX, 0f),
                        end = Offset(marginX, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            "graph_grid" -> {
                val step = 22.dp.toPx()
                val gridColor = baseInk.copy(alpha = (if (isDark) 0.10f else 0.085f) * factor)
                val stroke = 0.75.dp.toPx()
                var x = step
                while (x < size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), stroke)
                    x += step
                }
                var y = step
                while (y < size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), stroke)
                    y += step
                }
            }
            "linen" -> {
                val step = 8.dp.toPx()
                val weaveA = baseInk.copy(alpha = (if (isDark) 0.06f else 0.055f) * factor)
                val weaveB = baseInk.copy(alpha = (if (isDark) 0.045f else 0.04f) * factor)
                val stroke = 0.6.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(weaveA, Offset(x, 0f), Offset(x, size.height), stroke)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(weaveB, Offset(0f, y), Offset(size.width, y), stroke)
                    y += step
                }
            }
            "diagonal" -> {
                val step = 18.dp.toPx()
                val stripeColor = baseInk.copy(alpha = (if (isDark) 0.09f else 0.075f) * factor)
                val stroke = 0.9.dp.toPx()
                var startX = -size.height
                while (startX < size.width + size.height) {
                    drawLine(
                        color = stripeColor,
                        start = Offset(startX, size.height),
                        end = Offset(startX + size.height, 0f),
                        strokeWidth = stroke
                    )
                    startX += step
                }
            }
            "soft_glow" -> {
                val glow1 = (if (isDark) Color(0xFF818CF8) else Color(0xFF6366F1))
                    .copy(alpha = (if (isDark) 0.16f else 0.12f) * factor)
                val glow2 = (if (isDark) Color(0xFFF472B6) else Color(0xFFEC4899))
                    .copy(alpha = (if (isDark) 0.12f else 0.09f) * factor)
                val r1 = (size.minDimension * 0.58f).coerceAtLeast(60f)
                val r2 = (size.minDimension * 0.48f).coerceAtLeast(48f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glow1, Color.Transparent),
                        center = Offset(size.width * 0.86f, size.height * 0.14f),
                        radius = r1
                    ),
                    radius = r1,
                    center = Offset(size.width * 0.86f, size.height * 0.14f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glow2, Color.Transparent),
                        center = Offset(size.width * 0.14f, size.height * 0.86f),
                        radius = r2
                    ),
                    radius = r2,
                    center = Offset(size.width * 0.14f, size.height * 0.86f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteStyleStudioSheet(
    selectedColorHex: String,
    selectedTextureKey: String,
    selectedBgImagePath: String = "",
    isDark: Boolean,
    accentColor: Color,
    onSelectColorHex: (String) -> Unit,
    onSelectTextureKey: (String) -> Unit,
    onUploadGalleryBackground: () -> Unit = {},
    onClearGalleryBackground: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val defaultSurface = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFFAF9F6)
    val previewBrush = resolveNoteBackgroundBrush(selectedColorHex, isDark, defaultSurface)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Colors & Textures",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Customize tint, gradient backdrop, and paper feel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(
                    onClick = {
                        onSelectColorHex("")
                        onSelectTextureKey("none")
                        onClearGalleryBackground()
                    }
                ) {
                    Icon(
                        Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Reset")
                }
            }

            // Live Mini Preview Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(previewBrush)
                ) {
                    if (selectedBgImagePath.isNotBlank()) {
                        AsyncImage(
                            model = if (selectedBgImagePath.startsWith("/")) File(selectedBgImagePath) else selectedBgImagePath,
                            contentDescription = "Selected gallery background preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    if (isDark) Color(0xFF040708).copy(alpha = 0.58f)
                                    else Color.White.copy(alpha = 0.66f)
                                )
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawNoteTexture(selectedTextureKey, isDark = isDark, subtle = false)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val activeTextureName = NoteTextureOptions
                            .firstOrNull { it.key.equals(selectedTextureKey, ignoreCase = true) }
                            ?.label ?: "Clean"
                        val bgLabel = if (selectedBgImagePath.isNotBlank()) "Gallery Photo" else activeTextureName
                        Text(
                            text = "Live Preview • $bgLabel",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = accentColor
                        )
                        Text(
                            text = "Ideas look sharper on tactile paper",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Combine pastel tints, gradient backdrops, gallery photos, and stationery textures.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 1. Solid Paper Tints
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Solid Colors",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NoteColorPresets.forEach { preset ->
                        val isSelected = selectedBgImagePath.isBlank() &&
                            selectedColorHex.equals(preset.hexCode, ignoreCase = true)
                        val swatchColor = if (preset.hexCode.isEmpty()) defaultSurface else preset.resolve(isDark)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onClearGalleryBackground()
                                    onSelectColorHex(preset.hexCode)
                                }
                                .padding(2.dp)
                                .testTag("color_swatch_${preset.name.lowercase()}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected ${preset.name}",
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. Gradient Backgrounds
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Gradient Backgrounds",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NoteGradientPresets.forEach { preset ->
                        val isSelected = selectedBgImagePath.isBlank() &&
                            selectedColorHex.equals(preset.hexCode, ignoreCase = true)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onClearGalleryBackground()
                                    onSelectColorHex(preset.hexCode)
                                }
                                .testTag("gradient_swatch_${preset.hexCode}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 68.dp, height = 46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(preset.resolveBrush(isDark, defaultSurface))
                                    .border(
                                        width = if (isSelected) 2.2.dp else 1.dp,
                                        color = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = accentColor,
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected ${preset.name}",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. Paper & Surface Textures
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Paper & Surface Textures",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NoteTextureOptions.forEach { tex ->
                        val effectiveCurrent = selectedTextureKey.ifBlank { "none" }
                        val isSelected = effectiveCurrent.equals(tex.key, ignoreCase = true)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectTextureKey(tex.key) }
                                .testTag("texture_option_${tex.key}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 76.dp, height = 56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(previewBrush)
                                    .drawNoteTexture(tex.key, isDark = isDark, subtle = false)
                                    .border(
                                        width = if (isSelected) 2.2.dp else 1.dp,
                                        color = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = accentColor,
                                        modifier = Modifier
                                            .padding(5.dp)
                                            .size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected ${tex.label}",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = tex.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 4. Custom Photo Background from Gallery
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Photo Background",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onUploadGalleryBackground,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("upload_gallery_background_button")
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (selectedBgImagePath.isNotBlank()) "Change Photo" else "Upload from Gallery",
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (selectedBgImagePath.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, accentColor),
                            modifier = Modifier.size(48.dp)
                        ) {
                            AsyncImage(
                                model = if (selectedBgImagePath.startsWith("/")) File(selectedBgImagePath) else selectedBgImagePath,
                                contentDescription = "Current background photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                            modifier = Modifier
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onClearGalleryBackground() }
                                .testTag("remove_gallery_background_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove photo background",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Remove",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("style_sheet_done_button")
            ) {
                Text("Apply Style", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
