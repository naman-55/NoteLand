package com.example.ui.editor.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.editor.model.EditorColorPalette
import com.example.ui.editor.model.EditorColorSwatch
import com.example.ui.editor.model.QuickEditConfigReducer
import com.example.ui.editor.model.QuickEditTool
import com.example.ui.editor.model.QuickEditToolbarConfig
import com.example.ui.editor.model.QuickEditToolbarPreset
import com.example.ui.editor.model.UnderlineStyle
import kotlin.math.PI
import kotlin.math.sin

/**
 * Clean, easy-to-use Manage Toolbar bottom sheet:
 * - 1-tap Presets (Simple, Writing, Full) to simplify or expand instantly
 * - Calm, low-clutter Active Tools list for toggling, starring, and reordering
 * - Compact FlowRow chip grid for adding more tools
 * - Focused Default Colours & Underline Style card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickEditCustomizationSheet(
    config: QuickEditToolbarConfig,
    accentColor: Color,
    isDark: Boolean,
    onUpdateConfig: (QuickEditToolbarConfig) -> Unit,
    onRestoreDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color(0xFFFAF8F4),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("quick_edit_customization_sheet")
    ) {
        QuickEditCustomizationSheetContent(
            config = config,
            accentColor = accentColor,
            isDark = isDark,
            onUpdateConfig = onUpdateConfig,
            onRestoreDefaults = onRestoreDefaults,
            onDismiss = onDismiss
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickEditCustomizationSheetContent(
    config: QuickEditToolbarConfig,
    accentColor: Color,
    isDark: Boolean,
    onUpdateConfig: (QuickEditToolbarConfig) -> Unit,
    onRestoreDefaults: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTools = config.orderedEnabledTools()
    val availableTools = config.availableToAddTools()
    val cardBg = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White
    val borderCol = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.48f else 0.65f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Manage Toolbar",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${activeTools.size} active · Pick a preset or customize",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onRestoreDefaults,
                    modifier = Modifier.testTag("restore_toolbar_defaults_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Restore Default Toolbar",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Reset", color = accentColor, fontWeight = FontWeight.SemiBold)
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("close_customize_sheet_btn")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Customization")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 530.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section 1: 1-Tap Quick Presets (Effortless simplification)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(0.8.dp, borderCol),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "QUICK LAYOUT PRESETS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        ),
                        color = accentColor
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickEditToolbarPreset.entries.forEach { preset ->
                            val presetTools = if (preset == QuickEditToolbarPreset.FULL) {
                                QuickEditToolbarConfig.DEFAULT_TOOLS
                            } else {
                                preset.tools
                            }
                            val isMatchingPreset = activeTools == presetTools
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isMatchingPreset) {
                                    accentColor.copy(alpha = if (isDark) 0.22f else 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                },
                                border = BorderStroke(
                                    width = if (isMatchingPreset) 1.3.dp else 0.8.dp,
                                    color = if (isMatchingPreset) accentColor else borderCol
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onUpdateConfig(QuickEditConfigReducer.applyPreset(config, preset))
                                    }
                                    .testTag("preset_${preset.id}_btn")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp)
                                ) {
                                    Text(
                                        text = preset.title,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (isMatchingPreset) FontWeight.Bold else FontWeight.SemiBold
                                        ),
                                        color = if (isMatchingPreset) accentColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = preset.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Clean Active Tools List (Divider-separated, zero visual clutter)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(0.8.dp, borderCol),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SHOWN IN TOOLBAR (${activeTools.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            ),
                            color = accentColor
                        )
                        Text(
                            text = "Star · Reorder · Hide",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    activeTools.forEachIndexed { index, tool ->
                        val isFrequent = tool in config.frequentTools
                        if (index > 0) {
                            HorizontalDivider(color = borderCol.copy(alpha = 0.4f))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .testTag("active_tool_row_${tool.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.width(22.dp)
                            )

                            Text(
                                text = tool.label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isFrequent) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = {
                                    onUpdateConfig(QuickEditConfigReducer.toggleFrequentTool(config, tool))
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("star_tool_${tool.id}")
                            ) {
                                Icon(
                                    imageVector = if (isFrequent) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = if (isFrequent) "Unstar ${tool.label}" else "Star ${tool.label}",
                                    tint = if (isFrequent) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    onUpdateConfig(QuickEditConfigReducer.moveTool(config, index, index - 1))
                                },
                                enabled = index > 0,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("move_up_tool_${tool.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Move ${tool.label} left",
                                    tint = if (index > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    onUpdateConfig(QuickEditConfigReducer.moveTool(config, index, index + 1))
                                },
                                enabled = index < activeTools.lastIndex,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("move_down_tool_${tool.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Move ${tool.label} right",
                                    tint = if (index < activeTools.lastIndex) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    onUpdateConfig(QuickEditConfigReducer.removeTool(config, tool))
                                },
                                enabled = activeTools.size > 1,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("remove_tool_${tool.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove ${tool.label}",
                                    tint = if (activeTools.size > 1) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                                    },
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Compact Pill Grid for Adding Tools (Scannable in seconds)
            if (availableTools.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(0.8.dp, borderCol),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "TAP TO ADD (${availableTools.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            ),
                            color = accentColor
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            availableTools.forEach { tool ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                                    border = BorderStroke(0.7.dp, borderCol),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onUpdateConfig(QuickEditConfigReducer.addTool(config, tool))
                                        }
                                        .testTag("add_tool_${tool.id}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add ${tool.label}",
                                            tint = accentColor,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = tool.label,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: Default Colours & Underline Style
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(0.8.dp, borderCol),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "DEFAULT COLOURS & UNDERLINE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        ),
                        color = accentColor
                    )

                    DefaultColorSwatchRow(
                        label = "Text",
                        swatches = EditorColorPalette.textColors.filter { it.hex != null },
                        selectedHex = config.defaultTextColorHex,
                        isDark = isDark,
                        accentColor = accentColor,
                        tagPrefix = "default_text_color",
                        onSelect = { hex ->
                            onUpdateConfig(QuickEditConfigReducer.setDefaultTextColor(config, hex))
                        }
                    )

                    DefaultColorSwatchRow(
                        label = "Background",
                        swatches = EditorColorPalette.textBackgroundColors.filter { it.hex != null },
                        selectedHex = config.defaultBackgroundColorHex,
                        isDark = isDark,
                        accentColor = accentColor,
                        tagPrefix = "default_bg_color",
                        onSelect = { hex ->
                            onUpdateConfig(QuickEditConfigReducer.setDefaultBackgroundColor(config, hex))
                        }
                    )

                    DefaultColorSwatchRow(
                        label = "Highlight",
                        swatches = EditorColorPalette.highlightColors.filter { it.hex != null },
                        selectedHex = config.defaultHighlightColorHex,
                        isDark = isDark,
                        accentColor = accentColor,
                        tagPrefix = "default_hl_color",
                        onSelect = { hex ->
                            onUpdateConfig(QuickEditConfigReducer.setDefaultHighlightColor(config, hex))
                        }
                    )

                    HorizontalDivider(color = borderCol.copy(alpha = 0.45f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Underline Style",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Quick toggle default",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            UnderlineStyleOptionChip(
                                label = "Standard",
                                isWavy = false,
                                selected = config.preferredUnderlineStyle == UnderlineStyle.STANDARD,
                                accentColor = accentColor,
                                testTag = "underline_style_standard_opt",
                                onClick = {
                                    onUpdateConfig(
                                        QuickEditConfigReducer.setPreferredUnderlineStyle(config, UnderlineStyle.STANDARD)
                                    )
                                }
                            )
                            UnderlineStyleOptionChip(
                                label = "Wavy",
                                isWavy = true,
                                selected = config.preferredUnderlineStyle == UnderlineStyle.WAVY,
                                accentColor = accentColor,
                                testTag = "underline_style_wavy_opt",
                                onClick = {
                                    onUpdateConfig(
                                        QuickEditConfigReducer.setPreferredUnderlineStyle(config, UnderlineStyle.WAVY)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultColorSwatchRow(
    label: String,
    swatches: List<EditorColorSwatch>,
    selectedHex: String?,
    isDark: Boolean,
    accentColor: Color,
    tagPrefix: String,
    onSelect: (String?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(78.dp)
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            swatches.forEach { swatch ->
                val isSelected = selectedHex.equals(swatch.hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(swatch.resolve(isDark))
                        .border(
                            width = if (isSelected) 2.2.dp else 0.9.dp,
                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        )
                        .clickable { onSelect(swatch.hex) }
                        .testTag("${tagPrefix}_${swatch.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnderlineStyleOptionChip(
    label: String,
    isWavy: Boolean,
    selected: Boolean,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accentColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            width = if (selected) 1.3.dp else 0.8.dp,
            color = if (selected) accentColor else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    textDecoration = if (!isWavy) TextDecoration.Underline else TextDecoration.None
                ),
                color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface
            )
            if (isWavy) {
                Canvas(
                    modifier = Modifier
                        .width(26.dp)
                        .height(5.dp)
                ) {
                    val path = Path()
                    val midY = size.height / 2f
                    val amp = size.height * 0.42f
                    var x = 0f
                    path.moveTo(0f, midY)
                    while (x <= size.width) {
                        val y = midY + sin((x / 7f) * 2.0 * PI).toFloat() * amp
                        path.lineTo(x, y)
                        x += 1.5f
                    }
                    drawPath(
                        path = path,
                        color = if (selected) accentColor else Color.Gray,
                        style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}
