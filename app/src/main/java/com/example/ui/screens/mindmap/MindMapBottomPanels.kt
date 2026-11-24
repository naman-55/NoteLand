package com.example.ui.screens.mindmap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.CanvasToolMode
import kotlin.math.roundToInt

private val StudioEmerald = Color(0xFF16A34A)
private val StudioEmeraldBright = Color(0xFF22C55E)
private val StudioEmeraldDeep = Color(0xFF047857)
private val StudioMintBg = Color(0xFFD8FCE3)
private val StudioMintBorder = Color(0xFF22C55E)
private val StudioRoseBg = Color(0xFFFFF1F2)
private val StudioRoseTint = Color(0xFFEF4444)

private enum class StyleSubTab(val label: String) {
    THEME("Theme"),
    NODE("Node"),
    FILL("Fill"),
    BORDER("Border"),
    TEXT("Text")
}

/**
 * Collapsible Mind Map Studio drawer and floating 5-tab emerald dock
 * precisely matching all 5 high-resolution reference screenshots.
 */
@Composable
fun MindMapBottomStudioDock(
    activeTab: MindMapDrawerTab,
    isDrawerOpen: Boolean,
    activeTool: CanvasToolMode,
    styleConfig: MindMapStyleConfig,
    selectedNode: MindMapNode?,
    isDark: Boolean,
    onSelectTab: (MindMapDrawerTab) -> Unit,
    onCloseDrawer: () -> Unit,
    onSelectTool: (CanvasToolMode) -> Unit,
    onAddTopic: () -> Unit,
    onAddSubtopic: () -> Unit,
    onAddSameLevel: () -> Unit,
    onAutoArrange: () -> Unit,
    onSelectLayout: (MindMapLayoutType) -> Unit,
    onPickImage: () -> Unit,
    onOpenIconPicker: () -> Unit,
    onOpenMoreSheet: () -> Unit,
    onSelectTheme: (MindMapThemePreset) -> Unit,
    onApplyNodeColor: (Int?) -> Unit,
    onSelectNodeShape: (MindMapNodeShape) -> Unit,
    onUpdateStyleConfig: (MindMapStyleConfig) -> Unit,
    onGroupSelected: () -> Unit,
    onUngroupSelected: () -> Unit,
    onLockSelected: () -> Unit,
    onUnlockSelected: () -> Unit,
    onOpenLayers: () -> Unit,
    onDuplicateSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onOpenFind: () -> Unit,
    onOpenSettings: () -> Unit,
    onLoadAiSample: () -> Unit = {},
    onResetStarter: () -> Unit = {}
) {
    val sheetBg = if (isDark) MaterialTheme.colorScheme.surfaceContainerLow else Color(0xFFF6FAF8)
    val cardBg = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isDrawerOpen) sheetBg else Color.Transparent,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
            )
            .navigationBarsPadding()
    ) {
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mindmap_drawer_sheet"),
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                color = sheetBg,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    // Centered pill handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(34.dp)
                            .height(3.5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f))
                    )
                    Spacer(Modifier.height(3.dp))

                    // Drawer title header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = tabIconFor(activeTab),
                                contentDescription = null,
                                tint = StudioEmerald,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = activeTab.label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = onCloseDrawer,
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("mindmap_drawer_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close panel",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(5.dp))

                    when (activeTab) {
                        MindMapDrawerTab.INSERT -> InsertToolPanel(
                            activeTool = activeTool,
                            cardBg = cardBg,
                            isDark = isDark,
                            onSelectTool = onSelectTool,
                            onAddTopic = onAddTopic,
                            onAddSubtopic = onAddSubtopic,
                            onPickImage = onPickImage,
                            onOpenIconPicker = onOpenIconPicker,
                            onOpenMoreSheet = onOpenMoreSheet,
                            onLoadAiSample = onLoadAiSample,
                            onResetStarter = onResetStarter,
                            onOpenSettings = onOpenSettings
                        )
                        MindMapDrawerTab.LAYOUT -> LayoutToolPanel(
                            currentLayout = styleConfig.layoutType,
                            cardBg = cardBg,
                            isDark = isDark,
                            onSelectLayout = onSelectLayout,
                            onAddTopic = onAddTopic,
                            onAddSubtopic = onAddSubtopic,
                            onAddSameLevel = onAddSameLevel,
                            onAutoArrange = onAutoArrange
                        )
                        MindMapDrawerTab.STYLE -> StyleToolPanel(
                            styleConfig = styleConfig,
                            selectedNode = selectedNode,
                            cardBg = cardBg,
                            isDark = isDark,
                            onSelectTheme = onSelectTheme,
                            onApplyNodeColor = onApplyNodeColor,
                            onSelectNodeShape = onSelectNodeShape,
                            onUpdateStyleConfig = onUpdateStyleConfig
                        )
                        MindMapDrawerTab.CONNECT -> ConnectToolPanel(
                            styleConfig = styleConfig,
                            cardBg = cardBg,
                            isDark = isDark,
                            onUpdateStyleConfig = onUpdateStyleConfig
                        )
                        MindMapDrawerTab.TOOLS -> ToolsActionPanel(
                            activeTool = activeTool,
                            cardBg = cardBg,
                            isDark = isDark,
                            onSelectTool = onSelectTool,
                            onGroupSelected = onGroupSelected,
                            onUngroupSelected = onUngroupSelected,
                            onLockSelected = onLockSelected,
                            onUnlockSelected = onUnlockSelected,
                            onOpenLayers = onOpenLayers,
                            onDuplicateSelected = onDuplicateSelected,
                            onDeleteSelected = onDeleteSelected,
                            onOpenFind = onOpenFind,
                            onOpenSettings = onOpenSettings,
                            onOpenMoreSheet = onOpenMoreSheet,
                            onLoadAiSample = onLoadAiSample,
                            onResetStarter = onResetStarter
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        // Floating white rounded 5-tab navigation dock
        MindMapBottomTabBar(
            activeTab = activeTab,
            isDrawerOpen = isDrawerOpen,
            isDark = isDark,
            onSelectTab = onSelectTab
        )
    }
}

@Composable
private fun MindMapBottomTabBar(
    activeTab: MindMapDrawerTab,
    isDrawerOpen: Boolean,
    isDark: Boolean,
    onSelectTab: (MindMapDrawerTab) -> Unit
) {
    val dockColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        color = dockColor,
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MindMapDrawerTab.entries.forEach { tab ->
                val isSelected = tab == activeTab && isDrawerOpen
                val pillModifier = if (isSelected) {
                    Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(StudioEmeraldBright, StudioEmerald, StudioEmeraldDeep)
                            )
                        )
                } else {
                    Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(20.dp))
                }

                Box(
                    modifier = pillModifier
                        .testTag("mindmap_tab_${tab.name.lowercase()}")
                        .clickable { onSelectTab(tab) }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tabIconFor(tab),
                            contentDescription = tab.label,
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = tab.label,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun tabIconFor(tab: MindMapDrawerTab): ImageVector = when (tab) {
    MindMapDrawerTab.INSERT -> Icons.Default.AddBox
    MindMapDrawerTab.LAYOUT -> Icons.Default.AccountTree
    MindMapDrawerTab.STYLE -> Icons.Default.Palette
    MindMapDrawerTab.CONNECT -> Icons.Default.Share
    MindMapDrawerTab.TOOLS -> Icons.Default.Layers
}

@Composable
private fun PaginationDots(
    count: Int,
    activeIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 3.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until count) {
            val active = i == activeIndex
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.5.dp)
                    .size(if (active) 6.5.dp else 5.5.dp)
                    .clip(CircleShape)
                    .background(if (active) StudioEmerald else MaterialTheme.colorScheme.outlineVariant)
                    .clickable { onSelect(i) }
            )
        }
    }
}

// =========================================================================
// TAB 1: INSERT PANEL (Matches Screenshot 4 — 2x5 Square Cards + Paging)
// =========================================================================

@Composable
private fun InsertToolPanel(
    activeTool: CanvasToolMode,
    cardBg: Color,
    isDark: Boolean,
    onSelectTool: (CanvasToolMode) -> Unit,
    onAddTopic: () -> Unit,
    onAddSubtopic: () -> Unit,
    onPickImage: () -> Unit,
    onOpenIconPicker: () -> Unit,
    onOpenMoreSheet: () -> Unit,
    onLoadAiSample: () -> Unit,
    onResetStarter: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var page by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mindmap_panel_insert"),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (page == 0) {
            // Row 1: Select, Pen, Highlight, Eraser, Text (Exact Screenshot 4 order)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                StudioSquareCard(
                    label = "Select",
                    icon = Icons.Default.NearMe,
                    isSelected = activeTool == CanvasToolMode.SELECT,
                    iconTint = if (activeTool == CanvasToolMode.SELECT) {
                        if (isDark) Color(0xFF86EFAC) else StudioEmeraldDeep
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    cardBg = cardBg,
                    isDark = isDark,
                    modifier = Modifier.weight(1f).testTag("insert_tool_select"),
                    onClick = { onSelectTool(CanvasToolMode.SELECT) }
                )
                StudioSquareCard(
                    label = "Pen",
                    icon = Icons.Default.Edit,
                    isSelected = activeTool == CanvasToolMode.PEN,
                    cardBg = cardBg,
                    isDark = isDark,
                    modifier = Modifier.weight(1f).testTag("insert_tool_pen"),
                    onClick = { onSelectTool(CanvasToolMode.PEN) }
                )
                StudioSquareCard(
                    label = "Highlight",
                    icon = Icons.Default.Highlight,
                    isSelected = activeTool == CanvasToolMode.HIGHLIGHTER,
                    iconTint = Color(0xFFF59E0B),
                    cardBg = cardBg,
                    isDark = isDark,
                    modifier = Modifier.weight(1f).testTag("insert_tool_highlight"),
                    onClick = { onSelectTool(CanvasToolMode.HIGHLIGHTER) }
                )
                StudioSquareCard(
                    label = "Eraser",
                    icon = Icons.Default.AutoFixHigh,
                    isSelected = activeTool == CanvasToolMode.ERASER,
                    cardBg = cardBg,
                    isDark = isDark,
                    modifier = Modifier.weight(1f).testTag("insert_tool_eraser"),
                    onClick = { onSelectTool(CanvasToolMode.ERASER) }
                )
                StudioSquareCard(
                    label = "Text",
                    icon = Icons.Default.TextFields,
                    isSelected = activeTool == CanvasToolMode.TEXT,
                    cardBg = cardBg,
                    isDark = isDark,
                    modifier = Modifier.weight(1f).testTag("insert_tool_text"),
                    onClick = { onSelectTool(CanvasToolMode.TEXT) }
                )
            }

            // Row 2: Shape, Arrow, Topic Box, Subtopic, Sticky Note (Exact Screenshot 4 order)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                StudioSquareCard(
                    label = "Shape",
                    icon = Icons.Default.CropSquare,
                    isSelected = activeTool == CanvasToolMode.SHAPE_RECT || activeTool == CanvasToolMode.SHAPE_CIRCLE,
                    cardBg = cardBg,
                    isDark = isDark,
                    modifier = Modifier.weight(1f).testTag("insert_tool_shape"),
                    onClick = {
                        onSelectTool(
                            if (activeTool == CanvasToolMode.SHAPE_RECT) CanvasToolMode.SHAPE_CIRCLE else CanvasToolMode.SHAPE_RECT
                        )
                    }
                )
                StudioSquareCard(
                    label = "Arrow",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    isSelected = activeTool == CanvasToolMode.SHAPE_ARROW,
                    cardBg = cardBg,
                    isDark = isDark,
                    modifier = Modifier.weight(1f).testTag("insert_tool_arrow"),
                    onClick = { onSelectTool(CanvasToolMode.SHAPE_ARROW) }
                )
                StudioSquareCard(
                    label = "Topic Box",
                    icon = Icons.Default.AddBox,
                    isSelected = false,
                    cardBg = cardBg,
                    isDark = isDark,
                    modifier = Modifier.weight(1f).testTag("insert_tool_topic_box"),
                    onClick = onAddTopic
                )
                StudioSquareCard(
                    label = "Subtopic",
                    icon = Icons.Default.AccountTree,
                    isSelected = false,
                    iconTint = StudioEmerald,
                    cardBg = cardBg,
                    isDark = isDark,
                    modifier = Modifier.weight(1f).testTag("insert_tool_subtopic"),
                    onClick = onAddSubtopic
                )
                StudioSquareCard(
                    label = "Sticky Note",
                    icon = Icons.AutoMirrored.Filled.StickyNote2,
                    isSelected = activeTool == CanvasToolMode.STICKY_NOTE,
                    iconTint = Color(0xFFF59E0B),
                    cardBg = cardBg,
                    isDark = isDark,
                    modifier = Modifier.weight(1f).testTag("insert_tool_sticky"),
                    onClick = { onSelectTool(CanvasToolMode.STICKY_NOTE) }
                )
            }
        } else {
            // Page 2: Image, Icon, Circle, Line, Connector / AI Template, Reset, Export, Settings, More
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                StudioSquareCard("Image", Icons.Default.Image, false, cardBg, isDark, Modifier.weight(1f).testTag("insert_tool_image"), onClick = onPickImage)
                StudioSquareCard("Icon", Icons.Default.Mood, false, cardBg, isDark, Modifier.weight(1f).testTag("insert_tool_icon"), onClick = onOpenIconPicker)
                StudioSquareCard("Circle", Icons.Default.RadioButtonUnchecked, activeTool == CanvasToolMode.SHAPE_CIRCLE, cardBg, isDark, Modifier.weight(1f), onClick = { onSelectTool(CanvasToolMode.SHAPE_CIRCLE) })
                StudioSquareCard("Line", Icons.Default.Timeline, activeTool == CanvasToolMode.SHAPE_LINE, cardBg, isDark, Modifier.weight(1f), onClick = { onSelectTool(CanvasToolMode.SHAPE_LINE) })
                StudioSquareCard("Connector", Icons.Default.Share, activeTool == CanvasToolMode.SHAPE_CONNECTOR, cardBg, isDark, Modifier.weight(1f), onClick = { onSelectTool(CanvasToolMode.SHAPE_CONNECTOR) })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                StudioSquareCard("AI Map", Icons.Default.AutoAwesome, false, cardBg, isDark, Modifier.weight(1f), iconTint = StudioEmerald, onClick = onLoadAiSample)
                StudioSquareCard("Starter", Icons.Default.Refresh, false, cardBg, isDark, Modifier.weight(1f), onClick = onResetStarter)
                StudioSquareCard("Export", Icons.Default.Download, false, cardBg, isDark, Modifier.weight(1f), onClick = onOpenMoreSheet)
                StudioSquareCard("Settings", Icons.Default.Settings, false, cardBg, isDark, Modifier.weight(1f), onClick = onOpenSettings)
                Spacer(Modifier.weight(1f))
            }
        }

        PaginationDots(count = 2, activeIndex = page, onSelect = { page = it })
    }
}

@Composable
private fun StudioSquareCard(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    cardBg: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    tintOverride: Color? = null,
    borderOverride: Color? = null,
    onClick: () -> Unit
) {
    val bg = when {
        isSelected && isDark -> StudioEmeraldDeep.copy(alpha = 0.45f)
        isSelected -> StudioMintBg
        else -> cardBg
    }
    val activeContentTint = if (isDark) Color(0xFF86EFAC) else StudioEmeraldDeep
    val finalTint = tintOverride ?: if (isSelected && iconTint == MaterialTheme.colorScheme.onSurface) activeContentTint else iconTint
    val borderColor = borderOverride ?: if (isSelected) (if (isDark) Color(0xFF34D399) else StudioMintBorder) else Color.Transparent

    Surface(
        modifier = modifier
            .height(54.dp)
            .shadow(elevation = if (isSelected) 3.dp else 2.dp, shape = RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = if (borderColor != Color.Transparent) BorderStroke(1.4.dp, borderColor) else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 3.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = finalTint,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = label,
                fontSize = 9.5.sp,
                fontWeight = if (isSelected || tintOverride != null) FontWeight.Bold else FontWeight.Medium,
                color = tintOverride ?: if (isSelected) (if (isDark) Color(0xFF86EFAC) else StudioEmeraldDeep) else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// =========================================================================
// TAB 2: LAYOUT PANEL (Matches Screenshot 2 — 5 Cards + Quick Actions Row)
// =========================================================================

@Composable
private fun LayoutToolPanel(
    currentLayout: MindMapLayoutType,
    cardBg: Color,
    isDark: Boolean,
    onSelectLayout: (MindMapLayoutType) -> Unit,
    onAddTopic: () -> Unit,
    onAddSubtopic: () -> Unit,
    onAddSameLevel: () -> Unit,
    onAutoArrange: () -> Unit
) {
    var page by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mindmap_panel_layout"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row of 5 layout cards matching Screenshot 2
        if (page == 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    MindMapLayoutType.RADIAL,
                    MindMapLayoutType.HIERARCHICAL,
                    MindMapLayoutType.TREE,
                    MindMapLayoutType.CONCEPT,
                    MindMapLayoutType.TIMELINE
                ).forEach { layout ->
                    LayoutPreviewTile(
                        layout = layout,
                        isSelected = currentLayout == layout,
                        cardBg = cardBg,
                        isDark = isDark,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("layout_tile_${layout.name.lowercase()}"),
                        onClick = { onSelectLayout(layout) }
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LayoutPreviewTile(
                    layout = MindMapLayoutType.ORG_CHART,
                    isSelected = currentLayout == MindMapLayoutType.ORG_CHART,
                    cardBg = cardBg,
                    isDark = isDark,
                    modifier = Modifier.weight(1f).testTag("layout_tile_org_chart"),
                    onClick = { onSelectLayout(MindMapLayoutType.ORG_CHART) }
                )
                LayoutPreviewTile(
                    layout = MindMapLayoutType.RADIAL,
                    isSelected = currentLayout == MindMapLayoutType.RADIAL,
                    cardBg = cardBg,
                    isDark = isDark,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectLayout(MindMapLayoutType.RADIAL) }
                )
                Spacer(Modifier.weight(3f))
            }
        }

        // ⚡ Quick Actions header with subtle horizontal divider line matching Screenshot 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = StudioEmerald,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Quick Actions",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFF6EE7B7) else StudioEmeraldDeep
            )
            Spacer(Modifier.width(8.dp))
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 0.8.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }

        // Single sleek horizontal row of 4 quick action pill chips matching Screenshot 2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuickActionChip(
                label = "Add Topic",
                icon = Icons.Default.Add,
                hasMintBadge = true,
                cardBg = cardBg,
                modifier = Modifier.testTag("quick_action_add_topic"),
                onClick = onAddTopic
            )
            QuickActionChip(
                label = "Add Subtopic",
                icon = Icons.Default.Add,
                hasMintBadge = true,
                cardBg = cardBg,
                modifier = Modifier.testTag("quick_action_add_subtopic"),
                onClick = onAddSubtopic
            )
            QuickActionChip(
                label = "Same Level",
                icon = Icons.Default.AccountTree,
                hasMintBadge = false,
                cardBg = cardBg,
                modifier = Modifier.testTag("quick_action_same_level"),
                onClick = onAddSameLevel
            )
            QuickActionChip(
                label = "Auto Arrange",
                icon = Icons.Default.AutoFixHigh,
                hasMintBadge = false,
                cardBg = cardBg,
                modifier = Modifier.testTag("quick_action_auto_arrange"),
                onClick = onAutoArrange
            )
        }

        PaginationDots(count = 2, activeIndex = page, onSelect = { page = it })
    }
}

@Composable
private fun LayoutPreviewTile(
    layout: MindMapLayoutType,
    isSelected: Boolean,
    cardBg: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = when {
        isSelected && isDark -> StudioEmeraldDeep.copy(alpha = 0.45f)
        isSelected -> StudioMintBg
        else -> cardBg
    }
    val strokeColor = if (isSelected) (if (isDark) Color(0xFF86EFAC) else StudioEmeraldDeep) else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier
            .height(58.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(
            if (isSelected) 1.5.dp else 0.5.dp,
            if (isSelected) StudioMintBorder else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 3.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Canvas(modifier = Modifier.size(width = 28.dp, height = 22.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                when (layout) {
                    MindMapLayoutType.RADIAL -> {
                        val pts = listOf(
                            Offset(4f, 4f), Offset(size.width - 4f, 4f),
                            Offset(4f, size.height - 4f), Offset(size.width - 4f, size.height - 4f)
                        )
                        pts.forEach { p ->
                            drawLine(strokeColor, Offset(cx, cy), p, strokeWidth = 2f)
                            drawCircle(strokeColor, radius = 2.6f, center = p, style = Stroke(1.8f))
                        }
                        drawCircle(strokeColor, radius = 3.6f, center = Offset(cx, cy), style = Stroke(2f))
                    }
                    MindMapLayoutType.HIERARCHICAL -> {
                        drawRoundRect(strokeColor, Offset(cx - 5f, 1f), Size(10f, 5f), CornerRadius(2f), style = Stroke(1.8f))
                        drawLine(strokeColor, Offset(cx, 6f), Offset(cx, 11f), 1.8f)
                        drawLine(strokeColor, Offset(6f, 11f), Offset(size.width - 6f, 11f), 1.8f)
                        drawLine(strokeColor, Offset(6f, 11f), Offset(6f, 15f), 1.8f)
                        drawLine(strokeColor, Offset(size.width - 6f, 11f), Offset(size.width - 6f, 15f), 1.8f)
                        drawRoundRect(strokeColor, Offset(2f, 15f), Size(8f, 5f), CornerRadius(2f), style = Stroke(1.6f))
                        drawRoundRect(strokeColor, Offset(size.width - 10f, 15f), Size(8f, 5f), CornerRadius(2f), style = Stroke(1.6f))
                    }
                    MindMapLayoutType.TREE -> {
                        drawRoundRect(strokeColor, Offset(cx - 4f, 1f), Size(8f, 5f), CornerRadius(2f), style = Stroke(1.8f))
                        drawLine(strokeColor, Offset(cx, 6f), Offset(6f, 15f), 1.8f)
                        drawLine(strokeColor, Offset(cx, 6f), Offset(size.width - 6f, 15f), 1.8f)
                        drawRoundRect(strokeColor, Offset(2f, 15f), Size(8f, 5f), CornerRadius(2f), style = Stroke(1.6f))
                        drawRoundRect(strokeColor, Offset(size.width - 10f, 15f), Size(8f, 5f), CornerRadius(2f), style = Stroke(1.6f))
                    }
                    MindMapLayoutType.CONCEPT -> {
                        val a = Offset(5f, 5f)
                        val b = Offset(size.width - 5f, 5f)
                        val c = Offset(cx, size.height - 4f)
                        drawLine(strokeColor, a, c, 1.8f)
                        drawLine(strokeColor, b, c, 1.8f)
                        drawCircle(strokeColor, 3f, a, style = Stroke(1.8f))
                        drawCircle(strokeColor, 3f, b, style = Stroke(1.8f))
                        drawCircle(strokeColor, 3f, c, style = Stroke(1.8f))
                    }
                    MindMapLayoutType.TIMELINE -> {
                        drawLine(strokeColor, Offset(4f, cy), Offset(size.width - 4f, cy), 2f)
                        drawCircle(strokeColor, 3f, Offset(5f, cy), style = Stroke(1.8f))
                        drawCircle(strokeColor, 3f, Offset(size.width - 5f, cy), style = Stroke(1.8f))
                    }
                    MindMapLayoutType.ORG_CHART -> {
                        drawRoundRect(strokeColor, Offset(cx - 5f, 1f), Size(10f, 5f), CornerRadius(2f), style = Stroke(1.8f))
                        drawLine(strokeColor, Offset(cx, 6f), Offset(cx, 15f), 1.6f)
                        drawLine(strokeColor, Offset(5f, 11f), Offset(size.width - 5f, 11f), 1.6f)
                        drawLine(strokeColor, Offset(5f, 11f), Offset(5f, 15f), 1.6f)
                        drawLine(strokeColor, Offset(size.width - 5f, 11f), Offset(size.width - 5f, 15f), 1.6f)
                        listOf(2f, cx - 3f, size.width - 8f).forEach { rx ->
                            drawRoundRect(strokeColor, Offset(rx, 15f), Size(6f, 5f), CornerRadius(1.5f), style = Stroke(1.5f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = layout.label,
                fontSize = 9.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) (if (isDark) Color(0xFF86EFAC) else StudioEmeraldDeep) else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickActionChip(
    label: String,
    icon: ImageVector,
    hasMintBadge: Boolean,
    cardBg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(38.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasMintBadge) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(StudioMintBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = StudioEmeraldDeep, modifier = Modifier.size(14.dp))
                }
            } else {
                Icon(icon, contentDescription = null, tint = StudioEmerald, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

// =========================================================================
// TAB 3: STYLE PANEL (Matches Screenshot 1 — 5 Icon Cards + Themes + 8 Presets)
// =========================================================================

@Composable
private fun StyleToolPanel(
    styleConfig: MindMapStyleConfig,
    selectedNode: MindMapNode?,
    cardBg: Color,
    isDark: Boolean,
    onSelectTheme: (MindMapThemePreset) -> Unit,
    onApplyNodeColor: (Int?) -> Unit,
    onSelectNodeShape: (MindMapNodeShape) -> Unit,
    onUpdateStyleConfig: (MindMapStyleConfig) -> Unit
) {
    var subTab by remember { mutableStateOf(StyleSubTab.THEME) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mindmap_panel_style"),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        // Row of 5 tall square icon+label cards matching Screenshot 1 (Theme, Node, Fill, Border, Text)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StyleSubTab.entries.forEach { tab ->
                val active = tab == subTab
                val bg = if (active) (if (isDark) StudioEmeraldDeep.copy(alpha = 0.45f) else StudioMintBg) else cardBg
                val tint = if (active) (if (isDark) Color(0xFF86EFAC) else StudioEmeraldDeep) else MaterialTheme.colorScheme.onSurface
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(13.dp))
                        .clip(RoundedCornerShape(13.dp))
                        .testTag("style_subtab_${tab.name.lowercase()}")
                        .clickable { subTab = tab },
                    shape = RoundedCornerShape(13.dp),
                    color = bg,
                    border = BorderStroke(if (active) 1.4.dp else 0.5.dp, if (active) StudioMintBorder else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        when (tab) {
                            StyleSubTab.THEME -> Canvas(Modifier.size(18.dp)) {
                                val c = if (active) StudioEmerald else tint
                                drawCircle(c.copy(alpha = 0.85f), 4.8f, Offset(size.width / 2f, 5.5f))
                                drawCircle(c.copy(alpha = 0.65f), 4.8f, Offset(5.5f, size.height - 5.5f))
                                drawCircle(c.copy(alpha = 0.65f), 4.8f, Offset(size.width - 5.5f, size.height - 5.5f))
                            }
                            StyleSubTab.NODE -> Canvas(Modifier.size(width = 22.dp, height = 14.dp)) {
                                drawRoundRect(tint, Offset(1f, 1f), Size(size.width - 2f, size.height - 2f), CornerRadius(7f), style = Stroke(2f))
                            }
                            StyleSubTab.FILL -> Icon(Icons.Default.WaterDrop, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
                            StyleSubTab.BORDER -> Canvas(Modifier.size(width = 22.dp, height = 14.dp)) {
                                drawRoundRect(
                                    tint, Offset(1f, 1f), Size(size.width - 2f, size.height - 2f), CornerRadius(7f),
                                    style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f)))
                                )
                            }
                            StyleSubTab.TEXT -> Text("Aa", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = tint)
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            fontSize = 9.5.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = tint,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        when (subTab) {
            StyleSubTab.THEME -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Themes", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "View All →",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StudioEmerald,
                        modifier = Modifier.clickable {
                            val nextIdx = (styleConfig.themePreset.ordinal + 1) % MindMapThemePreset.entries.size
                            onSelectTheme(MindMapThemePreset.entries[nextIdx])
                        }
                    )
                }

                // 5 Mini-diagram theme preview cards matching Screenshot 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MindMapThemePreset.entries.forEach { preset ->
                        val active = styleConfig.themePreset == preset
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .testTag("theme_card_${preset.name.lowercase()}")
                                .clickable { onSelectTheme(preset) },
                            shape = RoundedCornerShape(12.dp),
                            color = cardBg,
                            border = BorderStroke(
                                width = if (active) 1.8.dp else 0.6.dp,
                                color = if (active) StudioMintBorder else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(7.dp)) {
                                val midY = size.height / 2f
                                val c1 = preset.rootStart
                                val c2 = preset.branchPalette.getOrElse(3) { c1 }
                                val c3 = preset.branchPalette.getOrElse(0) { c1 }
                                drawRoundRect(c1, Offset(0f, midY - 5f), Size(size.width * 0.35f, 10f), CornerRadius(5f), style = Stroke(2f))
                                drawLine(c2, Offset(size.width * 0.35f, midY - 2f), Offset(size.width * 0.62f, 5f), 1.8f)
                                drawLine(c3, Offset(size.width * 0.35f, midY + 2f), Offset(size.width * 0.62f, size.height - 5f), 1.8f)
                                drawRoundRect(c2, Offset(size.width * 0.62f, 1f), Size(size.width * 0.36f, 9f), CornerRadius(4.5f), style = Stroke(1.8f))
                                drawRoundRect(c3, Offset(size.width * 0.62f, size.height - 10f), Size(size.width * 0.36f, 9f), CornerRadius(4.5f), style = Stroke(1.8f))
                            }
                        }
                    }
                }

                // Color Presets (8 circular swatches matching Screenshot 1!)
                Text("Color Presets", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                val swatches = listOf(
                    Color(0xFF16A34A), // Emerald
                    Color(0xFF3B82F6), // Sky Blue
                    Color(0xFFA855F7), // Soft Purple
                    Color(0xFFEC4899), // Pink
                    Color(0xFFF97316), // Orange
                    Color(0xFFFACC15), // Yellow
                    Color(0xFF2DD4BF)  // Teal
                )
                val activeColorArgb = selectedNode?.colorArgb ?: styleConfig.themePreset.rootStart.toArgb()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    swatches.forEachIndexed { idx, color ->
                        val isPicked = activeColorArgb == color.toArgb()
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isPicked) 1.8.dp else 0.dp,
                                    color = if (isPicked) StudioEmerald else Color.Transparent,
                                    shape = CircleShape
                                )
                                .padding(if (isPicked) 3.dp else 1.dp)
                                .clip(CircleShape)
                                .background(color)
                                .testTag("style_color_swatch_$idx")
                                .clickable { onApplyNodeColor(color.toArgb()) }
                        )
                    }
                    // 8th swatch: Rainbow spectrum inside elevated white ring
                    Surface(
                        modifier = Modifier
                            .size(32.dp)
                            .shadow(elevation = 2.dp, shape = CircleShape)
                            .clip(CircleShape)
                            .testTag("style_color_rainbow")
                            .clickable { onApplyNodeColor(null) },
                        shape = CircleShape,
                        color = cardBg
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.5.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF22C55E),
                                            Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF9333EA), Color(0xFFEF4444)
                                        )
                                    )
                                )
                        )
                    }
                }
            }

            StyleSubTab.NODE -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val activeShape = selectedNode?.shape ?: MindMapNodeShape.PILL
                    MindMapNodeShape.entries.forEach { shape ->
                        val active = shape == activeShape
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .clickable { onSelectNodeShape(shape) },
                            shape = RoundedCornerShape(13.dp),
                            color = if (active) (if (isDark) StudioEmeraldDeep.copy(alpha = 0.42f) else StudioMintBg) else cardBg,
                            border = BorderStroke(if (active) 1.5.dp else 0.7.dp, if (active) StudioMintBorder else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(shape.label, fontSize = 11.5.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            StyleSubTab.FILL -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MindMapFillStyle.entries.forEach { fill ->
                        val active = styleConfig.fillStyle == fill
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .clickable { onUpdateStyleConfig(styleConfig.copy(fillStyle = fill)) },
                            shape = RoundedCornerShape(13.dp),
                            color = if (active) (if (isDark) StudioEmeraldDeep.copy(alpha = 0.42f) else StudioMintBg) else cardBg,
                            border = BorderStroke(if (active) 1.5.dp else 0.7.dp, if (active) StudioMintBorder else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                                Text(fill.label.replace(" ", "\n"), fontSize = 10.5.sp, lineHeight = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            StyleSubTab.BORDER -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Border Width", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    Text("${styleConfig.borderWidthPx.roundToInt()} px", fontSize = 12.sp, color = StudioEmerald, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = styleConfig.borderWidthPx,
                    onValueChange = { onUpdateStyleConfig(styleConfig.copy(borderWidthPx = it)) },
                    valueRange = 0f..5f,
                    colors = SliderDefaults.colors(thumbColor = StudioEmerald, activeTrackColor = StudioEmerald)
                )
            }

            StyleSubTab.TEXT -> {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Text Scale (${(styleConfig.textScale * 100).roundToInt()}%)", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Bold", fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Switch(checked = styleConfig.boldText, onCheckedChange = { onUpdateStyleConfig(styleConfig.copy(boldText = it)) })
                    }
                }
                Slider(
                    value = styleConfig.textScale,
                    onValueChange = { onUpdateStyleConfig(styleConfig.copy(textScale = it)) },
                    valueRange = 0.8f..1.35f,
                    colors = SliderDefaults.colors(thumbColor = StudioEmerald, activeTrackColor = StudioEmerald)
                )
            }
        }
    }
}

// =========================================================================
// TAB 4: CONNECT PANEL (Matches Screenshot 5 Exactly!)
// =========================================================================

@Composable
private fun ConnectToolPanel(
    styleConfig: MindMapStyleConfig,
    cardBg: Color,
    isDark: Boolean,
    onUpdateStyleConfig: (MindMapStyleConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mindmap_panel_connect"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section 1: Connector Type — 5 tall cards with icon + label (Straight, Elbow, Curved, Spline, Double)
        Text("Connector Type", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MindMapConnectorType.entries.forEach { type ->
                val active = styleConfig.connectorType == type
                val bg = if (active) (if (isDark) StudioEmeraldDeep.copy(alpha = 0.45f) else StudioMintBg) else cardBg
                val strokeColor = if (active) (if (isDark) Color(0xFF86EFAC) else StudioEmeraldDeep) else MaterialTheme.colorScheme.onSurface
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(13.dp))
                        .clip(RoundedCornerShape(13.dp))
                        .testTag("connector_type_${type.name.lowercase()}")
                        .clickable { onUpdateStyleConfig(styleConfig.copy(connectorType = type)) },
                    shape = RoundedCornerShape(13.dp),
                    color = bg,
                    border = BorderStroke(if (active) 1.4.dp else 0.5.dp, if (active) StudioMintBorder else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Canvas(modifier = Modifier.size(width = 28.dp, height = 16.dp)) {
                            val cy = size.height / 2f
                            when (type) {
                                MindMapConnectorType.STRAIGHT -> {
                                    drawLine(strokeColor, Offset(2f, cy), Offset(size.width - 2f, cy), 2f, cap = StrokeCap.Round)
                                    drawLine(strokeColor, Offset(size.width - 7f, cy - 4f), Offset(size.width - 2f, cy), 2f, cap = StrokeCap.Round)
                                    drawLine(strokeColor, Offset(size.width - 7f, cy + 4f), Offset(size.width - 2f, cy), 2f, cap = StrokeCap.Round)
                                }
                                MindMapConnectorType.ORTHOGONAL -> {
                                    val p = Path().apply {
                                        moveTo(2f, 3f); lineTo(size.width * 0.5f, 3f)
                                        lineTo(size.width * 0.5f, size.height - 3f); lineTo(size.width - 2f, size.height - 3f)
                                    }
                                    drawPath(p, strokeColor, style = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                }
                                MindMapConnectorType.CURVED -> {
                                    val p = Path().apply {
                                        moveTo(2f, size.height - 3f)
                                        cubicTo(size.width * 0.25f, 1f, size.width * 0.75f, 1f, size.width - 2f, size.height - 3f)
                                    }
                                    drawPath(p, strokeColor, style = Stroke(2f, cap = StrokeCap.Round))
                                }
                                MindMapConnectorType.SPLINE -> {
                                    val p = Path().apply {
                                        moveTo(2f, size.height * 0.45f)
                                        cubicTo(size.width * 0.35f, size.height + 2f, size.width * 0.65f, -2f, size.width - 2f, size.height * 0.55f)
                                    }
                                    drawPath(p, strokeColor, style = Stroke(2f, cap = StrokeCap.Round))
                                }
                                MindMapConnectorType.BIDIRECTIONAL -> {
                                    drawLine(strokeColor, Offset(3f, cy), Offset(size.width - 3f, cy), 2f, cap = StrokeCap.Round)
                                    drawLine(strokeColor, Offset(7f, cy - 3.5f), Offset(3f, cy), 1.9f)
                                    drawLine(strokeColor, Offset(7f, cy + 3.5f), Offset(3f, cy), 1.9f)
                                    drawLine(strokeColor, Offset(size.width - 7f, cy - 3.5f), Offset(size.width - 3f, cy), 1.9f)
                                    drawLine(strokeColor, Offset(size.width - 7f, cy + 3.5f), Offset(size.width - 3f, cy), 1.9f)
                                }
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = type.label,
                            fontSize = 9.5.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = strokeColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Section 2: Line Style (3 pills) | Line Color (white capsule with 5 dots)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1.05f)) {
                Text("Line Style", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    MindMapLineStyle.entries.forEach { style ->
                        val active = styleConfig.lineStyle == style
                        ConnectMiniChip(
                            isSelected = active,
                            cardBg = cardBg,
                            isDark = isDark,
                            modifier = Modifier.weight(1f).testTag("line_style_${style.name.lowercase()}"),
                            onClick = { onUpdateStyleConfig(styleConfig.copy(lineStyle = style)) }
                        ) { color ->
                            val effect = when (style) {
                                MindMapLineStyle.SOLID -> null
                                MindMapLineStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf(5f, 4f))
                                MindMapLineStyle.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(2f, 4f))
                            }
                            drawLine(color, Offset(2f, size.height / 2f), Offset(size.width - 2f, size.height / 2f), 2.3f, cap = StrokeCap.Round, pathEffect = effect)
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Line Color", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                val lineColors = listOf(Color(0xFF16A34A), Color(0xFF3B82F6), Color(0xFFA855F7), Color(0xFFF97316))
                Surface(
                    modifier = Modifier.fillMaxWidth().height(34.dp),
                    shape = RoundedCornerShape(17.dp),
                    color = cardBg,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        lineColors.forEach { c ->
                            val picked = styleConfig.overrideLineColorArgb == c.toArgb()
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .border(if (picked) 1.8.dp else 0.dp, if (picked) StudioEmerald else Color.Transparent, CircleShape)
                                    .padding(if (picked) 2.5.dp else 0.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .clickable { onUpdateStyleConfig(styleConfig.copy(overrideLineColorArgb = c.toArgb())) }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF22C55E), Color(0xFF3B82F6), Color(0xFF9333EA), Color(0xFFEF4444))
                                    )
                                )
                                .border(if (styleConfig.overrideLineColorArgb == null) 1.8.dp else 0.dp, StudioEmerald, CircleShape)
                                .clickable { onUpdateStyleConfig(styleConfig.copy(overrideLineColorArgb = null)) }
                        )
                    }
                }
            }
        }

        // Section 3: Arrowhead (5 square buttons including Diamond) | Line Width (slider + px)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1.18f)) {
                Text("Arrowhead", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        MindMapArrowhead.OPEN_ARROW,
                        MindMapArrowhead.DOT,
                        MindMapArrowhead.FILLED_ARROW,
                        MindMapArrowhead.HOLLOW_TRIANGLE,
                        MindMapArrowhead.DIAMOND
                    ).forEach { arrow ->
                        val active = styleConfig.arrowhead == arrow
                        ConnectMiniChip(
                            isSelected = active,
                            cardBg = cardBg,
                            isDark = isDark,
                            modifier = Modifier.weight(1f).testTag("arrowhead_${arrow.name.lowercase()}"),
                            onClick = {
                                val next = if (active) MindMapArrowhead.NONE else arrow
                                onUpdateStyleConfig(styleConfig.copy(arrowhead = next))
                            }
                        ) { color ->
                            val cy = size.height / 2f
                            when (arrow) {
                                MindMapArrowhead.OPEN_ARROW -> {
                                    drawLine(color, Offset(2f, cy), Offset(size.width - 2f, cy), 2f)
                                    drawLine(color, Offset(size.width - 7f, cy - 3.5f), Offset(size.width - 2f, cy), 2f)
                                    drawLine(color, Offset(size.width - 7f, cy + 3.5f), Offset(size.width - 2f, cy), 2f)
                                }
                                MindMapArrowhead.DOT -> {
                                    drawLine(color, Offset(2f, cy), Offset(size.width - 5f, cy), 2f)
                                    drawCircle(color, 3f, Offset(size.width - 4f, cy))
                                }
                                MindMapArrowhead.FILLED_ARROW -> {
                                    drawLine(color, Offset(2f, cy), Offset(size.width - 5f, cy), 2f)
                                    val tri = Path().apply {
                                        moveTo(size.width - 1f, cy); lineTo(size.width - 7f, cy - 3.5f); lineTo(size.width - 7f, cy + 3.5f); close()
                                    }
                                    drawPath(tri, color)
                                }
                                MindMapArrowhead.HOLLOW_TRIANGLE -> {
                                    val tri = Path().apply {
                                        moveTo(size.width - 2f, cy); lineTo(3f, cy - 4.5f); lineTo(3f, cy + 4.5f); close()
                                    }
                                    drawPath(tri, color, style = Stroke(1.8f))
                                }
                                MindMapArrowhead.DIAMOND -> {
                                    drawLine(color, Offset(2f, cy), Offset(size.width - 7f, cy), 2f)
                                    val dm = Path().apply {
                                        moveTo(size.width - 1f, cy)
                                        lineTo(size.width - 5f, cy - 3.5f)
                                        lineTo(size.width - 9f, cy)
                                        lineTo(size.width - 5f, cy + 3.5f)
                                        close()
                                    }
                                    drawPath(dm, color)
                                }
                                MindMapArrowhead.NONE -> Unit
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(0.9f)) {
                Text("Line Width", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = styleConfig.lineWidthPx,
                        onValueChange = { onUpdateStyleConfig(styleConfig.copy(lineWidthPx = it)) },
                        valueRange = 1.5f..7f,
                        modifier = Modifier.weight(1f).height(34.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = StudioEmerald,
                            activeTrackColor = StudioEmerald
                        )
                    )
                    Text(
                        text = "${styleConfig.lineWidthPx.roundToInt()} px",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectMiniChip(
    isSelected: Boolean,
    cardBg: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    drawContent: DrawScope.(Color) -> Unit
) {
    val bg = when {
        isSelected && isDark -> StudioEmeraldDeep.copy(alpha = 0.45f)
        isSelected -> StudioMintBg
        else -> cardBg
    }
    val strokeColor = if (isSelected) (if (isDark) Color(0xFF86EFAC) else StudioEmeraldDeep) else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier
            .height(34.dp)
            .shadow(elevation = 1.5.dp, shape = RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = BorderStroke(if (isSelected) 1.4.dp else 0.5.dp, if (isSelected) StudioMintBorder else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 7.dp)) {
            drawContent(strokeColor)
        }
    }
}

// =========================================================================
// TAB 5: TOOLS PANEL (Matches Screenshot 3 — 2x5 Square Cards + Paging)
// =========================================================================

@Composable
private fun ToolsActionPanel(
    activeTool: CanvasToolMode,
    cardBg: Color,
    isDark: Boolean,
    onSelectTool: (CanvasToolMode) -> Unit,
    onGroupSelected: () -> Unit,
    onUngroupSelected: () -> Unit,
    onLockSelected: () -> Unit,
    onUnlockSelected: () -> Unit,
    onOpenLayers: () -> Unit,
    onDuplicateSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onOpenFind: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMoreSheet: () -> Unit,
    onLoadAiSample: () -> Unit,
    onResetStarter: () -> Unit
) {
    var page by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mindmap_panel_tools"),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (page == 0) {
            // Row 1: Pan, Lasso, Group, Ungroup, Lock (Exact Screenshot 3 order)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                StudioSquareCard("Pan", Icons.Default.PanTool, activeTool == CanvasToolMode.PAN_ZOOM, cardBg, isDark, Modifier.weight(1f).testTag("tools_pan"), onClick = { onSelectTool(CanvasToolMode.PAN_ZOOM) })
                StudioSquareCard("Lasso", Icons.Default.Hub, activeTool == CanvasToolMode.LASSO, cardBg, isDark, Modifier.weight(1f).testTag("tools_lasso"), onClick = { onSelectTool(CanvasToolMode.LASSO) })
                StudioSquareCard("Group", Icons.Default.GroupWork, false, cardBg, isDark, Modifier.weight(1f).testTag("tools_group"), onClick = onGroupSelected)
                StudioSquareCard("Ungroup", Icons.Default.GridView, false, cardBg, isDark, Modifier.weight(1f).testTag("tools_ungroup"), onClick = onUngroupSelected)
                StudioSquareCard("Lock", Icons.Default.Lock, false, cardBg, isDark, Modifier.weight(1f).testTag("tools_lock"), onClick = onLockSelected)
            }

            // Row 2: Unlock, Layers, Duplicate, Delete (soft rose), Find (Exact Screenshot 3 order)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                StudioSquareCard("Unlock", Icons.Default.LockOpen, false, cardBg, isDark, Modifier.weight(1f).testTag("tools_unlock"), onClick = onUnlockSelected)
                StudioSquareCard("Layers", Icons.Default.Layers, false, cardBg, isDark, Modifier.weight(1f).testTag("tools_layers"), onClick = onOpenLayers)
                StudioSquareCard("Duplicate", Icons.Default.ContentCopy, false, cardBg, isDark, Modifier.weight(1f).testTag("tools_duplicate"), onClick = onDuplicateSelected)
                StudioSquareCard(
                    label = "Delete",
                    icon = Icons.Default.DeleteOutline,
                    isSelected = false,
                    cardBg = if (isDark) Color(0xFF3B181E) else StudioRoseBg,
                    isDark = isDark,
                    modifier = Modifier.weight(1f).testTag("tools_delete"),
                    tintOverride = if (isDark) Color(0xFFFCA5A5) else StudioRoseTint,
                    borderOverride = (if (isDark) Color(0xFFFCA5A5) else StudioRoseTint).copy(alpha = 0.28f),
                    onClick = onDeleteSelected
                )
                StudioSquareCard("Find", Icons.Default.Search, false, cardBg, isDark, Modifier.weight(1f).testTag("tools_find"), onClick = onOpenFind)
            }
        } else {
            // Page 2: Settings, Export, AI Map, Starter Map, Select
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                StudioSquareCard("Settings", Icons.Default.Settings, false, cardBg, isDark, Modifier.weight(1f).testTag("tools_settings"), onClick = onOpenSettings)
                StudioSquareCard("Export", Icons.Default.Download, false, cardBg, isDark, Modifier.weight(1f).testTag("tools_more"), onClick = onOpenMoreSheet)
                StudioSquareCard("AI Map", Icons.Default.AutoAwesome, false, cardBg, isDark, Modifier.weight(1f), iconTint = StudioEmerald, onClick = onLoadAiSample)
                StudioSquareCard("Reset", Icons.Default.Refresh, false, cardBg, isDark, Modifier.weight(1f), onClick = onResetStarter)
                StudioSquareCard("Select", Icons.Default.NearMe, activeTool == CanvasToolMode.SELECT, cardBg, isDark, Modifier.weight(1f), onClick = { onSelectTool(CanvasToolMode.SELECT) })
            }
            Spacer(Modifier.height(54.dp))
        }

        PaginationDots(count = 2, activeIndex = page, onSelect = { page = it })
    }
}
