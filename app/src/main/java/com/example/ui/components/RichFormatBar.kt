package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.NoteColorPresets
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.SmartNoteUtils

@Composable
fun RichFormatBar(
    content: String,
    selection: TextRange = TextRange.Zero,
    onInsertMarkdown: (prefix: String, suffix: String) -> Unit,
    onOpenLinkDialog: () -> Unit = {},
    onAddImage: () -> Unit = {},
    onPasteImage: (() -> Unit)? = null,
    onOpenUtilities: (() -> Unit)? = null,
    selectedColorHex: String = "",
    onSelectColorHex: ((String) -> Unit)? = null,
    onOpenColorSheet: (() -> Unit)? = null,
    onOpenDrawing: (() -> Unit)? = null,
    onOpenEraser: (() -> Unit)? = null,
    onStartVoiceDictation: (() -> Unit)? = null,
    isDictating: Boolean = false,
    onToggleVoiceRecording: (() -> Unit)? = null,
    isRecordingAudio: Boolean = false,
    currentAppThemeMode: AppThemeMode = AppThemeMode.LIGHT,
    onSelectAppThemeMode: ((AppThemeMode) -> Unit)? = null,
    applyNavigationBarPadding: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showToolsTray by remember { mutableStateOf(false) }
    val isDark = isAppInDarkTheme

    BackHandler(enabled = showToolsTray) {
        showToolsTray = false
    }
    val activeStyles = remember(content, selection) {
        detectActiveEditorialStyles(content, selection)
    }
    val wordCount = SmartNoteUtils.countWords(content)
    val readTime = SmartNoteUtils.calculateReadingTimeMinutes(content)

    val dockBg = when {
        isDark -> Color(0xFF141B20).copy(alpha = 0.97f)
        currentAppThemeMode == AppThemeMode.IVORY -> Color(0xFFFDF8F0).copy(alpha = 0.98f)
        else -> Color(0xFFFFFFFF).copy(alpha = 0.98f)
    }
    val dockBorder = if (isDark) {
        Color.White.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
    }
    val contentFg = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1F242D)
    val mutedFg = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val chipBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    val accentColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFFD85C37)
    val onAccent = if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (applyNavigationBarPadding) Modifier.navigationBarsPadding() else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Expandable tray for extra formatting, color swatches, and Smart Utilities
        AnimatedVisibility(
            visible = showToolsTray,
            enter = fadeIn() + slideInVertically { it / 3 },
            exit = fadeOut() + slideOutVertically { it / 3 }
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = dockBg,
                border = BorderStroke(0.9.dp, dockBorder),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onSelectAppThemeMode != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Theme",
                                color = mutedFg,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    Triple("Light", Icons.Default.LightMode, AppThemeMode.LIGHT),
                                    Triple("Ivory", Icons.Default.WbTwilight, AppThemeMode.IVORY),
                                    Triple("Dark", Icons.Default.DarkMode, AppThemeMode.DARK)
                                ).forEach { (label, icon, mode) ->
                                    val selected = currentAppThemeMode == mode
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) accentColor else chipBg,
                                        border = BorderStroke(0.7.dp, if (selected) accentColor else dockBorder),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onSelectAppThemeMode(mode) }
                                            .testTag("theme_mode_${mode.key}_btn")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Icon(icon, contentDescription = null, tint = if (selected) onAccent else contentFg, modifier = Modifier.size(13.dp))
                                            Text(label, color = if (selected) onAccent else contentFg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Color Swatches row
                    if (onSelectColorHex != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NoteColorPresets.forEach { preset ->
                                val isSelected = selectedColorHex.equals(preset.hexCode, ignoreCase = true)
                                val swatchColor = if (preset.hexCode.isEmpty()) {
                                    Color(0xFFFAF9F6)
                                } else {
                                    preset.resolve(isDark)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(
                                            width = if (isSelected) 2.2.dp else 1.dp,
                                            color = if (isSelected) accentColor else dockBorder,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            onSelectColorHex(preset.hexCode)
                                        }
                                )
                            }
                            if (onOpenColorSheet != null) {
                                Surface(
                                    color = accentColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(0.7.dp, accentColor.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onOpenColorSheet() }
                                ) {
                                    Text(
                                        text = "Backgrounds & Textures",
                                        color = accentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Extended Markdown & Utilities row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TrayTextButton("H2", contentFg) { onInsertMarkdown("## ", "") }
                        TrayTextButton("H3", contentFg) { onInsertMarkdown("### ", "") }
                        TrayIconButton(Icons.Default.FormatStrikethrough, "Strikethrough", contentFg) {
                            onInsertMarkdown("~~", "~~")
                        }
                        TrayIconButton(Icons.Default.Highlight, "Highlight", contentFg) {
                            onInsertMarkdown("==", "==")
                        }
                        TrayIconButton(Icons.AutoMirrored.Filled.FormatListBulleted, "Bullet List", contentFg) {
                            onInsertMarkdown("\n• ", "")
                        }
                        TrayIconButton(Icons.Default.FormatListNumbered, "Numbered List", contentFg) {
                            onInsertMarkdown("\n1. ", "")
                        }
                        TrayIconButton(Icons.Default.CheckBox, "Task Checklist", contentFg) {
                            onInsertMarkdown("\n- [ ] ", "")
                        }
                        TrayIconButton(Icons.Default.FormatQuote, "Quote", contentFg) {
                            onInsertMarkdown("\n> ", "")
                        }
                        TrayIconButton(Icons.Default.Code, "Code Block", contentFg) {
                            onInsertMarkdown("\n```\n", "\n```\n")
                        }
                        TrayIconButton(Icons.Default.Link, "Hyperlink", contentFg) {
                            onOpenLinkDialog()
                        }

                        if (onOpenUtilities != null) {
                            Spacer(Modifier.width(4.dp))
                            Surface(
                                color = accentColor.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showToolsTray = false
                                        onOpenUtilities()
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = "Smart Utilities",
                                        tint = accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (readTime > 0) "${wordCount}w · ${readTime}m" else "${wordCount}w",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Primary Floating Editorial Dock
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = dockBg,
            border = BorderStroke(0.9.dp, dockBorder),
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxWidth()
                .height(54.dp)
                .shadow(
                    elevation = if (isDark) 10.dp else 8.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color.Black.copy(alpha = 0.14f),
                    spotColor = Color.Black.copy(alpha = 0.16f)
                )
                .testTag("floating_format_dock")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = onAddImage,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("format_add_image_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Insert Image",
                        tint = contentFg,
                        modifier = Modifier.size(21.dp)
                    )
                }

                if (onPasteImage != null) {
                    IconButton(
                        onClick = onPasteImage,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("format_paste_image_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste Image from Clipboard",
                            tint = contentFg,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { onInsertMarkdown("# ", "") },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("format_heading_btn")
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (activeStyles.isHeading) accentColor else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "T",
                            color = if (activeStyles.isHeading) onAccent else contentFg,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                IconButton(
                    onClick = { onInsertMarkdown("**", "**") },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("format_bold_btn")
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (activeStyles.isBold) accentColor else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "B",
                            color = if (activeStyles.isBold) onAccent else contentFg,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                IconButton(
                    onClick = { onInsertMarkdown("*", "*") },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("format_italic_btn")
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (activeStyles.isItalic) accentColor else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "I",
                            color = if (activeStyles.isItalic) onAccent else contentFg,
                            fontSize = 19.sp,
                            fontStyle = FontStyle.Italic,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }

                IconButton(
                    onClick = { onInsertMarkdown("<u>", "</u>") },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("format_underline_btn")
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (activeStyles.isUnderline) accentColor else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "U",
                            color = if (activeStyles.isUnderline) onAccent else contentFg,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textDecoration = TextDecoration.Underline,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                if (onOpenDrawing != null) {
                    IconButton(
                        onClick = {
                            showToolsTray = false
                            onOpenDrawing()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("format_draw_overlay_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Draw,
                            contentDescription = "Handwriting & Stylus Overlay",
                            tint = contentFg,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (onOpenEraser != null) {
                    IconButton(
                        onClick = {
                            showToolsTray = false
                            onOpenEraser()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("format_eraser_overlay_btn")
                    ) {
                        Icon(
                            imageVector = NotelandEraserIcon,
                            contentDescription = "Eraser",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (onStartVoiceDictation != null) {
                    IconButton(
                        onClick = {
                            showToolsTray = false
                            onStartVoiceDictation()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("format_mic_dictate_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDictating) accentColor else Color.Transparent
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = if (isDictating) "Stop Voice Dictation" else "Dictate with Microphone",
                                tint = if (isDictating) onAccent else contentFg,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (onToggleVoiceRecording != null) {
                    IconButton(
                        onClick = {
                            showToolsTray = false
                            onToggleVoiceRecording()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("format_record_voice_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRecordingAudio) Color(0xFFEF4444) else chipBg
                                )
                                .border(
                                    width = 1.4.dp,
                                    color = if (isRecordingAudio) Color.White else contentFg.copy(alpha = 0.4f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRecordingAudio) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                contentDescription = if (isRecordingAudio) "Stop & Save Voice Recording" else "Record Voice Audio",
                                tint = if (isRecordingAudio) Color.White else Color(0xFFEF4444),
                                modifier = Modifier.size(if (isRecordingAudio) 17.dp else 15.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        if (onSelectColorHex != null || onOpenUtilities != null || onSelectAppThemeMode != null) {
                            showToolsTray = !showToolsTray
                        } else {
                            onOpenColorSheet?.invoke()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("format_palette_btn")
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (showToolsTray) accentColor else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Colors and Formatting",
                            tint = if (showToolsTray) onAccent else contentFg,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrayIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint.copy(alpha = 0.88f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun TrayTextButton(
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 38.dp, height = 36.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = tint.copy(alpha = 0.9f),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
