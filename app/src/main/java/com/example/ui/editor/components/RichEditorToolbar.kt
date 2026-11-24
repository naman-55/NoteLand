package com.example.ui.editor.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertEmoticon
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.springPressScale
import com.example.ui.editor.model.InlineFormatStyle
import com.example.ui.editor.model.RichBlockKind
import com.example.ui.editor.model.RichTextAlignment
import com.example.ui.editor.model.UnderlineStyle
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.isAppInDarkTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ToolbarTray {
    NONE, THEME_STYLE, UNDERLINE, HIGHLIGHT, TEXT_COLOR, MORE_TOOLS
}

enum class MoreToolsCategory(val label: String, val icon: ImageVector) {
    INSERT("Insert", Icons.Default.Add),
    LAYOUT("Layout", Icons.Default.GridView),
    LIST("List", Icons.AutoMirrored.Filled.FormatListBulleted),
    BLOCK("Block", Icons.Default.Layers),
    OTHER("Other", Icons.Default.AutoAwesome)
}

/**
 * Compact bottom dock item with icon and label underneath.
 * Shows high-visibility green/primary pill container when selected (as seen in NoteLand UI).
 */
@Composable
fun DockTrayIconBtn(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme
    val interactionSource = remember { MutableInteractionSource() }

    val activeContainer = if (isDark) Color(0xFF163A2F) else Color(0xFFE6F4EA)
    val activeContent = if (isDark) Color(0xFF5EEAD4) else Color(0xFF137333)
    val inactiveContent = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)

    val containerColor by animateColorAsState(
        targetValue = if (isActive) activeContainer else Color.Transparent,
        animationSpec = tween(150),
        label = "dockItemContainer_$label"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isActive) activeContent else inactiveContent,
        animationSpec = tween(150),
        label = "dockItemContent_$label"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = modifier
            .springPressScale(interactionSource, pressedScale = 0.92f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .widthIn(min = 48.dp, max = 58.dp)
            .height(48.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Compact rounded card for expanded tools and contextual drawers.
 */
@Composable
fun CompactTrayPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme
    val interactionSource = remember { MutableInteractionSource() }
    val activeContainer = if (isDark) Color(0xFF173630) else Color(0xFFE8F5E9)
    val inactiveContainer = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color(0xFFF8F9FA)
    val activeContent = if (isDark) Color(0xFF5EEAD4) else Color(0xFF1B6B4C)
    val inactiveContent = MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isActive) activeContent.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

    Surface(
        shape = RoundedCornerShape(13.dp),
        color = if (isActive) activeContainer else inactiveContainer,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .springPressScale(interactionSource, pressedScale = 0.92f)
            .clip(RoundedCornerShape(13.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .width(62.dp)
            .height(58.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeContent else inactiveContent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isActive) activeContent else inactiveContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Enhanced Color Swatch Row with presets, recent colors, remove button, and custom picker.
 */
@Composable
fun ColorSwatchRow(
    selectedHex: String?,
    onSelectColor: (String) -> Unit,
    onClearColor: () -> Unit,
    presetColors: List<String>,
    recentColors: List<String> = emptyList(),
    onOpenCustomPicker: () -> Unit = {}
) {
    val isDark = isAppInDarkTheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clear / Default Color Button
        Surface(
            shape = CircleShape,
            color = if (selectedHex.isNullOrBlank()) {
                if (isDark) Color(0xFF263238) else Color(0xFFE2E8F0)
            } else Color.Transparent,
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable { onClearColor() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.FormatClear,
                    contentDescription = "Clear Color",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Custom Color Picker Button
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable { onOpenCustomPicker() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Custom Color",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Divider between actions and swatches
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(24.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )

        // Preset Colors
        presetColors.forEach { hex ->
            val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Transparent)
            val isSelected = selectedHex?.equals(hex, ignoreCase = true) == true
            val isLightColor = hex.equals("#FFFFFF", ignoreCase = true) || hex.equals("#FEF08A", ignoreCase = true) || hex.equals("#BBF7D0", ignoreCase = true)

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .clickable { onSelectColor(hex) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = if (isLightColor) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Final mobile note editor toolbar with strict layout order:
 * 1. Note Content
 * 2. Expanded Tools Panel / Contextual Tray (Above Main Toolbar)
 * 3. Main Formatting Toolbar (Fixed at bottom)
 * 4. Android Navigation Bar
 */
@Composable
fun RichEditorToolbar(
    plainTextForStats: String,
    activeStyle: InlineFormatStyle,
    activeBlockKind: RichBlockKind,
    activeAlignment: RichTextAlignment,
    activeIndentLevel: Int,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleUnderlineStandard: () -> Unit,
    onToggleUnderlineWavy: () -> Unit,
    onToggleStrikethrough: () -> Unit,
    onToggleInlineCode: () -> Unit,
    onClearFormatting: () -> Unit,
    onSelectTextColor: (String) -> Unit,
    onSelectTextBackgroundColor: (String) -> Unit,
    onSelectHighlightColor: (String) -> Unit,
    onSetAlignment: (RichTextAlignment) -> Unit,
    onIncreaseIndent: () -> Unit,
    onDecreaseIndent: () -> Unit,
    onSetBlockKind: (RichBlockKind) -> Unit,
    onInsertDivider: () -> Unit,
    onInsertTable: () -> Unit,
    onInsertSpreadsheet: () -> Unit,
    onOpenLinkDialog: () -> Unit,
    onAttachImage: () -> Unit,
    onAttachPdf: () -> Unit,
    onAttachDocument: () -> Unit,
    onPasteImage: () -> Unit,
    selectedNoteColorHex: String,
    onSelectNoteColorHex: (String) -> Unit,
    onOpenNoteColorSheet: () -> Unit,
    onOpenUtilities: () -> Unit,
    onOpenDrawing: () -> Unit,
    onOpenEraser: () -> Unit,
    onStartVoiceDictation: () -> Unit,
    isDictating: Boolean,
    onToggleVoiceRecording: () -> Unit,
    isRecordingAudio: Boolean,
    currentAppThemeMode: AppThemeMode,
    onSelectAppThemeMode: (AppThemeMode) -> Unit,
    hasInkStrokes: Boolean,
    applyNavigationBarPadding: Boolean = false,
    onSelectUnderlineStyle: (UnderlineStyle) -> Unit = { style ->
        if (style == UnderlineStyle.WAVY || style == UnderlineStyle.DOUBLE_WAVY) {
            onToggleUnderlineWavy()
        } else {
            onToggleUnderlineStandard()
        }
    }
) {
    var activeTray by remember { mutableStateOf(ToolbarTray.NONE) }
    var activeCategory by remember { mutableStateOf(MoreToolsCategory.INSERT) }
    val isDark = isAppInDarkTheme

    // Recent colors tracking
    val recentTextColors = remember { mutableStateListOf<String>() }
    val recentHighlightColors = remember { mutableStateListOf<String>() }

    // Custom Color Dialog states
    var showCustomTextColorDialog by remember { mutableStateOf(false) }
    var showCustomHighlightColorDialog by remember { mutableStateOf(false) }
    var customHexInput by remember { mutableStateOf("#") }

    val textColorPresets = remember {
        listOf(
            "#000000", "#1E293B", "#DC2626", "#EA580C",
            "#D97706", "#16A34A", "#0891B2", "#2563EB",
            "#7C3AED", "#DB2777", "#FFFFFF", "#64748B"
        )
    }

    val highlightColorPresets = remember {
        listOf(
            "#FEF08A", "#BBF7D0", "#BAE6FD", "#DDD6FE",
            "#FBCFE8", "#FED7AA", "#E2E8F0", "#FECDD3",
            "#99F6E4", "#FDE68A"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // TOP BORDER: 0.8dp crisp separation from note content
        HorizontalDivider(
            thickness = 0.8.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )

        // SECTION A: EXPANDED TOOLS PANEL / CONTEXTUAL FORMATTING TRAY (ABOVE MAIN TOOLBAR)
        AnimatedVisibility(
            visible = activeTray != ToolbarTray.NONE,
            enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(180)),
            exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(140))
        ) {
            Surface(
                color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color(0xFFF9FBFA),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(20.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    // Pull Handle Indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 6.dp)
                            .width(34.dp)
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    )

                    when (activeTray) {
                        // 1. MORE TOOLS EXPANDED PANEL
                        ToolbarTray.MORE_TOOLS -> {
                            // Category Tabs Row with Close Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MoreToolsCategory.values().forEach { category ->
                                        val isSelected = activeCategory == category
                                        val tabBg = if (isSelected) {
                                            if (isDark) Color(0xFF134E3E) else Color(0xFF1E6B52)
                                        } else Color.Transparent
                                        val tabContent = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = tabBg,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .clickable { activeCategory = category }
                                                .heightIn(min = 34.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = category.icon,
                                                    contentDescription = null,
                                                    tint = tabContent,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Text(
                                                    text = category.label,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 12.sp
                                                    ),
                                                    color = tabContent
                                                )
                                            }
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { activeTray = ToolbarTray.NONE },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Tool Cards Horizontally Scrollable Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                when (activeCategory) {
                                    MoreToolsCategory.INSERT -> {
                                        CompactTrayPill(Icons.Default.TableChart, "Table", onInsertTable)
                                        CompactTrayPill(Icons.Default.FormatQuote, "Quote", { onSetBlockKind(RichBlockKind.BLOCKQUOTE) })
                                        CompactTrayPill(Icons.Default.HorizontalRule, "Divider", onInsertDivider)
                                        CompactTrayPill(Icons.Default.Link, "Link", onOpenLinkDialog)
                                        CompactTrayPill(Icons.Default.AttachFile, "File", onAttachDocument)
                                        CompactTrayPill(Icons.Default.Functions, "Equation", { /* Equation insertion */ })
                                        CompactTrayPill(Icons.Default.DateRange, "Date", {
                                            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
                                            /* Insert date */
                                        })
                                        CompactTrayPill(Icons.Default.InsertEmoticon, "Emoji", { /* Emoji launcher */ })
                                    }
                                    MoreToolsCategory.LAYOUT -> {
                                        val alignIcon = when (activeAlignment) {
                                            RichTextAlignment.LEFT -> Icons.Default.FormatAlignLeft
                                            RichTextAlignment.CENTER -> Icons.Default.FormatAlignCenter
                                            RichTextAlignment.RIGHT -> Icons.Default.FormatAlignRight
                                        }
                                        CompactTrayPill(alignIcon, "Alignment", {
                                            val next = when (activeAlignment) {
                                                RichTextAlignment.LEFT -> RichTextAlignment.CENTER
                                                RichTextAlignment.CENTER -> RichTextAlignment.RIGHT
                                                RichTextAlignment.RIGHT -> RichTextAlignment.LEFT
                                            }
                                            onSetAlignment(next)
                                        }, isActive = activeAlignment != RichTextAlignment.LEFT)
                                        CompactTrayPill(Icons.AutoMirrored.Filled.FormatIndentIncrease, "Indent", onIncreaseIndent)
                                        CompactTrayPill(Icons.AutoMirrored.Filled.FormatIndentDecrease, "Outdent", onDecreaseIndent)
                                        CompactTrayPill(Icons.Default.FormatLineSpacing, "Line Spacing", { /* Line Spacing */ })
                                        CompactTrayPill(Icons.Default.Height, "Spacing", { /* Paragraph Spacing */ })
                                        CompactTrayPill(Icons.Default.ViewColumn, "Columns", { /* Columns */ })
                                    }
                                    MoreToolsCategory.LIST -> {
                                        CompactTrayPill(
                                            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                                            label = "Bullets",
                                            onClick = { onSetBlockKind(RichBlockKind.BULLET_LIST) },
                                            isActive = activeBlockKind == RichBlockKind.BULLET_LIST
                                        )
                                        CompactTrayPill(
                                            icon = Icons.Default.FormatListNumbered,
                                            label = "Numbered",
                                            onClick = { onSetBlockKind(RichBlockKind.NUMBERED_LIST) },
                                            isActive = activeBlockKind == RichBlockKind.NUMBERED_LIST
                                        )
                                        CompactTrayPill(
                                            icon = Icons.Default.CheckBox,
                                            label = "Checklist",
                                            onClick = { onSetBlockKind(RichBlockKind.CHECKLIST) },
                                            isActive = activeBlockKind == RichBlockKind.CHECKLIST
                                        )
                                        CompactTrayPill(
                                            icon = Icons.Default.AccountTree,
                                            label = "Nested",
                                            onClick = onIncreaseIndent
                                        )
                                    }
                                    MoreToolsCategory.BLOCK -> {
                                        CompactTrayPill(
                                            icon = Icons.Default.FormatQuote,
                                            label = "Quote Block",
                                            onClick = { onSetBlockKind(RichBlockKind.BLOCKQUOTE) },
                                            isActive = activeBlockKind == RichBlockKind.BLOCKQUOTE
                                        )
                                        CompactTrayPill(
                                            icon = Icons.Default.Info,
                                            label = "Callout",
                                            onClick = { onSetBlockKind(RichBlockKind.CALLOUT) },
                                            isActive = activeBlockKind == RichBlockKind.CALLOUT
                                        )
                                        CompactTrayPill(
                                            icon = Icons.Default.Code,
                                            label = "Code Block",
                                            onClick = onToggleInlineCode,
                                            isActive = activeStyle.inlineCode
                                        )
                                        CompactTrayPill(
                                            icon = Icons.Default.UnfoldMore,
                                            label = "Toggle",
                                            onClick = { /* Toggle Block */ }
                                        )
                                        CompactTrayPill(
                                            icon = Icons.Default.HorizontalRule,
                                            label = "Divider",
                                            onClick = onInsertDivider
                                        )
                                    }
                                    MoreToolsCategory.OTHER -> {
                                        CompactTrayPill(Icons.Default.AutoAwesome, "Special Chars", { /* Special symbols */ })
                                        CompactTrayPill(Icons.Default.Tune, "Utilities", onOpenUtilities)
                                        CompactTrayPill(
                                            icon = Icons.Default.Mic,
                                            label = "Voice Tools",
                                            onClick = onToggleVoiceRecording,
                                            isActive = isRecordingAudio
                                        )
                                        CompactTrayPill(
                                            icon = Icons.Default.Brush,
                                            label = "Drawing",
                                            onClick = onOpenDrawing
                                        )
                                        CompactTrayPill(
                                            icon = Icons.Default.AutoFixHigh,
                                            label = "Eraser",
                                            onClick = onOpenEraser
                                        )
                                    }
                                }
                            }
                        }

                        // 2. UNDERLINE CONTEXTUAL TRAY
                        ToolbarTray.UNDERLINE -> {
                            TrayHeader(
                                title = "UNDERLINE STYLES",
                                onClose = { activeTray = ToolbarTray.NONE }
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CompactTrayPill(
                                    icon = Icons.Default.FormatUnderlined,
                                    label = "Single",
                                    onClick = { onSelectUnderlineStyle(UnderlineStyle.STANDARD) },
                                    isActive = activeStyle.underline == UnderlineStyle.STANDARD
                                )
                                CompactTrayPill(
                                    icon = Icons.Default.FormatUnderlined,
                                    label = "Double",
                                    onClick = { onSelectUnderlineStyle(UnderlineStyle.DOUBLE) },
                                    isActive = activeStyle.underline == UnderlineStyle.DOUBLE
                                )
                                CompactTrayPill(
                                    icon = Icons.Default.HorizontalRule,
                                    label = "Wavy",
                                    onClick = { onSelectUnderlineStyle(UnderlineStyle.WAVY) },
                                    isActive = activeStyle.underline == UnderlineStyle.WAVY
                                )
                                CompactTrayPill(
                                    icon = Icons.Default.HorizontalRule,
                                    label = "2x Wavy",
                                    onClick = { onSelectUnderlineStyle(UnderlineStyle.DOUBLE_WAVY) },
                                    isActive = activeStyle.underline == UnderlineStyle.DOUBLE_WAVY
                                )
                                CompactTrayPill(
                                    icon = Icons.Default.MoreHoriz,
                                    label = "Dotted",
                                    onClick = { onSelectUnderlineStyle(UnderlineStyle.DOTTED) },
                                    isActive = activeStyle.underline == UnderlineStyle.DOTTED
                                )
                                CompactTrayPill(
                                    icon = Icons.Default.HorizontalRule,
                                    label = "Dashed",
                                    onClick = { onSelectUnderlineStyle(UnderlineStyle.DASHED) },
                                    isActive = activeStyle.underline == UnderlineStyle.DASHED
                                )
                                CompactTrayPill(
                                    icon = Icons.Default.MoreHoriz,
                                    label = "Dash-Dot",
                                    onClick = { onSelectUnderlineStyle(UnderlineStyle.DASH_DOT) },
                                    isActive = activeStyle.underline == UnderlineStyle.DASH_DOT
                                )
                                CompactTrayPill(
                                    icon = Icons.Default.FormatClear,
                                    label = "None",
                                    onClick = { onSelectUnderlineStyle(UnderlineStyle.NONE) },
                                    isActive = activeStyle.underline == UnderlineStyle.NONE
                                )
                            }
                        }

                        // 3. HIGHLIGHT CONTEXTUAL TRAY
                        ToolbarTray.HIGHLIGHT -> {
                            TrayHeader(
                                title = "HIGHLIGHT COLOR",
                                onClose = { activeTray = ToolbarTray.NONE }
                            )
                            ColorSwatchRow(
                                selectedHex = activeStyle.highlightColorHex,
                                onSelectColor = { hex ->
                                    if (hex !in recentHighlightColors) recentHighlightColors.add(0, hex)
                                    onSelectHighlightColor(hex)
                                },
                                onClearColor = { onSelectHighlightColor("") },
                                presetColors = highlightColorPresets,
                                recentColors = recentHighlightColors,
                                onOpenCustomPicker = { showCustomHighlightColorDialog = true }
                            )
                        }

                        // 4. TEXT COLOR CONTEXTUAL TRAY
                        ToolbarTray.TEXT_COLOR -> {
                            TrayHeader(
                                title = "TEXT COLOR",
                                onClose = { activeTray = ToolbarTray.NONE }
                            )
                            ColorSwatchRow(
                                selectedHex = activeStyle.textColorHex,
                                onSelectColor = { hex ->
                                    if (hex !in recentTextColors) recentTextColors.add(0, hex)
                                    onSelectTextColor(hex)
                                },
                                onClearColor = { onSelectTextColor("") },
                                presetColors = textColorPresets,
                                recentColors = recentTextColors,
                                onOpenCustomPicker = { showCustomTextColorDialog = true }
                            )
                        }

                        // 5. STYLE (THEME) CONTEXTUAL TRAY
                        ToolbarTray.THEME_STYLE -> {
                            TrayHeader(
                                title = "FORMATTING & STYLES",
                                onClose = { activeTray = ToolbarTray.NONE }
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CompactTrayPill(
                                    icon = Icons.Default.FormatClear,
                                    label = "Clear",
                                    onClick = onClearFormatting
                                )
                                CompactTrayPill(
                                    icon = Icons.Default.FormatStrikethrough,
                                    label = "Strike",
                                    onClick = onToggleStrikethrough,
                                    isActive = activeStyle.strikethrough
                                )
                                CompactTrayPill(
                                    icon = Icons.Default.Code,
                                    label = "Inline Code",
                                    onClick = onToggleInlineCode,
                                    isActive = activeStyle.inlineCode
                                )
                                CompactTrayPill(
                                    icon = Icons.Default.FormatSize,
                                    label = "Heading 1",
                                    onClick = { onSetBlockKind(RichBlockKind.HEADING_1) },
                                    isActive = activeBlockKind == RichBlockKind.HEADING_1
                                )
                                CompactTrayPill(
                                    icon = Icons.Default.FormatSize,
                                    label = "Heading 2",
                                    onClick = { onSetBlockKind(RichBlockKind.HEADING_2) },
                                    isActive = activeBlockKind == RichBlockKind.HEADING_2
                                )
                                CompactTrayPill(
                                    icon = Icons.Default.FormatSize,
                                    label = "Heading 3",
                                    onClick = { onSetBlockKind(RichBlockKind.HEADING_3) },
                                    isActive = activeBlockKind == RichBlockKind.HEADING_3
                                )
                            }
                        }
                        ToolbarTray.NONE -> {}
                    }
                }
            }
        }

        // SECTION B: MAIN FORMATTING TOOLBAR (FIXED AT BOTTOM)
        // Order: Style, Bold, Italic, Underline, Highlight, Text Color, More
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 3.dp)
                .let { if (applyNavigationBarPadding) it.navigationBarsPadding() else it },
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Style
            DockTrayIconBtn(
                icon = Icons.Default.FormatSize,
                label = "Style",
                onClick = {
                    activeTray = if (activeTray == ToolbarTray.THEME_STYLE) ToolbarTray.NONE else ToolbarTray.THEME_STYLE
                },
                isActive = activeTray == ToolbarTray.THEME_STYLE
            )

            // 2. Bold
            DockTrayIconBtn(
                icon = Icons.Default.FormatBold,
                label = "Bold",
                onClick = onToggleBold,
                isActive = activeStyle.bold
            )

            // 3. Italic
            DockTrayIconBtn(
                icon = Icons.Default.FormatItalic,
                label = "Italic",
                onClick = onToggleItalic,
                isActive = activeStyle.italic
            )

            // 4. Underline
            DockTrayIconBtn(
                icon = Icons.Default.FormatUnderlined,
                label = "Underline",
                onClick = {
                    activeTray = if (activeTray == ToolbarTray.UNDERLINE) ToolbarTray.NONE else ToolbarTray.UNDERLINE
                },
                isActive = activeTray == ToolbarTray.UNDERLINE || activeStyle.underline != UnderlineStyle.NONE
            )

            // 5. Highlight
            DockTrayIconBtn(
                icon = Icons.Default.BorderColor,
                label = "Highlight",
                onClick = {
                    activeTray = if (activeTray == ToolbarTray.HIGHLIGHT) ToolbarTray.NONE else ToolbarTray.HIGHLIGHT
                },
                isActive = activeTray == ToolbarTray.HIGHLIGHT || !activeStyle.highlightColorHex.isNullOrBlank()
            )

            // 6. Text Color
            DockTrayIconBtn(
                icon = Icons.Default.FormatColorText,
                label = "Color",
                onClick = {
                    activeTray = if (activeTray == ToolbarTray.TEXT_COLOR) ToolbarTray.NONE else ToolbarTray.TEXT_COLOR
                },
                isActive = activeTray == ToolbarTray.TEXT_COLOR || !activeStyle.textColorHex.isNullOrBlank()
            )

            // 7. More
            DockTrayIconBtn(
                icon = Icons.Default.MoreHoriz,
                label = "More",
                onClick = {
                    activeTray = if (activeTray == ToolbarTray.MORE_TOOLS) ToolbarTray.NONE else ToolbarTray.MORE_TOOLS
                },
                isActive = activeTray == ToolbarTray.MORE_TOOLS
            )
        }
    }

    // Custom Color Dialogs
    if (showCustomTextColorDialog) {
        CustomColorPickerDialog(
            title = "Custom Text Color",
            initialHex = activeStyle.textColorHex ?: "#1B6B4C",
            onApply = { hex ->
                onSelectTextColor(hex)
                showCustomTextColorDialog = false
            },
            onDismiss = { showCustomTextColorDialog = false }
        )
    }

    if (showCustomHighlightColorDialog) {
        CustomColorPickerDialog(
            title = "Custom Highlight Color",
            initialHex = activeStyle.highlightColorHex ?: "#FEF08A",
            onApply = { hex ->
                onSelectHighlightColor(hex)
                showCustomHighlightColorDialog = false
            },
            onDismiss = { showCustomHighlightColorDialog = false }
        )
    }
}

@Composable
private fun TrayHeader(
    title: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun CustomColorPickerDialog(
    title: String,
    initialHex: String,
    onApply: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var hexInput by remember { mutableStateOf(initialHex) }
    var isValid by remember { mutableStateOf(true) }

    val quickColors = listOf(
        "#E11D48", "#BE123C", "#C026D3", "#9333EA", "#4F46E5",
        "#2563EB", "#0284C7", "#0D9488", "#059669", "#16A34A",
        "#65A30D", "#CA8A04", "#D97706", "#EA580C", "#334155"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Color Preview Box
                val previewColor = runCatching {
                    Color(android.graphics.Color.parseColor(hexInput))
                }.getOrNull()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(previewColor ?: Color.Transparent)
                            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    )
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { input ->
                            hexInput = input
                            isValid = runCatching {
                                android.graphics.Color.parseColor(input)
                                true
                            }.getOrDefault(false)
                        },
                        label = { Text("Hex Code") },
                        isError = !isValid,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Quick Palette:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick Palette Grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickColors.forEach { colorHex ->
                        val color = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }.getOrDefault(Color.Transparent)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .clickable {
                                    hexInput = colorHex
                                    isValid = true
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (isValid) onApply(hexInput) },
                enabled = isValid
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
