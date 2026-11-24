package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
internal fun PdfFloatingTopBar(
    fileName: String,
    currentPageIndex: Int,
    totalPages: Int,
    pageAnnotationCount: Int,
    stickyCount: Int,
    stickyAccent: Color,
    isSearchVisible: Boolean,
    saveStatus: PdfSaveStatus,
    isAutoSaveEnabled: Boolean,
    hasUnsavedChanges: Boolean,
    isTtsPlaying: Boolean,
    onBack: () -> Unit,
    onJumpPageClick: () -> Unit,
    onOpenStickyWall: () -> Unit,
    onToggleSearch: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onManualSave: () -> Unit,
    onToggleAutoSave: (Boolean) -> Unit,
    onToggleTts: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onSharePdf: () -> Unit,
    onCreateNoteFromPage: () -> Unit = {},
    onClearCurrentPage: () -> Unit,
    onOpenReaderSettings: () -> Unit
) {
    var showOverflowMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("pdf_back_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onJumpPageClick)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = fileName.ifBlank { "PDF Reader" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "Page ${currentPageIndex + 1}/$totalPages",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    val (statusText, statusColor) = when (saveStatus) {
                        PdfSaveStatus.SAVING -> "• Saving…" to MaterialTheme.colorScheme.primary
                        PdfSaveStatus.UNSAVED -> "• Unsaved" to Color(0xFFD97706)
                        PdfSaveStatus.ERROR -> "• Retry save" to MaterialTheme.colorScheme.error
                        PdfSaveStatus.SAVED -> "• Saved" to Color(0xFF16A34A)
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = statusColor,
                        maxLines = 1
                    )
                    if (pageAnnotationCount > 0) {
                        Text(
                            text = "• $pageAnnotationCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            // Sticky Wall Pill with badge
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = stickyAccent.copy(alpha = 0.14f),
                border = BorderStroke(0.7.dp, stickyAccent.copy(alpha = 0.45f)),
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenStickyWall)
                    .testTag("pdf_sticky_wall_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.StickyNote2,
                        contentDescription = "Sticky Wall",
                        tint = stickyAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    if (stickyCount > 0) {
                        Text(
                            text = "$stickyCount",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.width(2.dp))

            IconButton(
                onClick = onToggleSearch,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search Annotations",
                    tint = if (isSearchVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onToggleFullscreen,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    Icons.Default.Fullscreen,
                    contentDescription = "Focus Reading Mode",
                    modifier = Modifier.size(20.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { showOverflowMenu = true },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("pdf_more_options_button")
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Reader Menu & Save Options",
                        tint = if (hasUnsavedChanges) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = if (hasUnsavedChanges) "Save Changes" else "Save Now",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = when (saveStatus) {
                                        PdfSaveStatus.SAVING -> "Saving changes..."
                                        PdfSaveStatus.UNSAVED -> "Unsaved edits • Tap to save"
                                        PdfSaveStatus.ERROR -> "Last save failed • Tap to retry"
                                        PdfSaveStatus.SAVED -> "All changes saved"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasUnsavedChanges) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                if (hasUnsavedChanges) Icons.Default.Save else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (hasUnsavedChanges) MaterialTheme.colorScheme.primary else Color(0xFF16A34A)
                            )
                        },
                        modifier = Modifier.testTag("pdf_menu_manual_save"),
                        onClick = {
                            showOverflowMenu = false
                            onManualSave()
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("Auto-save", fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (isAutoSaveEnabled) "Saves edits automatically" else "Manual save mode",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = null,
                                tint = if (isAutoSaveEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            Switch(
                                checked = isAutoSaveEnabled,
                                onCheckedChange = { checked ->
                                    onToggleAutoSave(checked)
                                },
                                modifier = Modifier.testTag("pdf_menu_autosave_switch")
                            )
                        },
                        onClick = {
                            onToggleAutoSave(!isAutoSaveEnabled)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    DropdownMenuItem(
                        text = { Text("Themes & Reader Studio") },
                        leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                        onClick = {
                            showOverflowMenu = false
                            onOpenReaderSettings()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text(if (isTtsPlaying) "Pause Read Aloud" else "Read Aloud") },
                        leadingIcon = {
                            Icon(
                                if (isTtsPlaying) Icons.Default.Pause else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            onToggleTts()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Bookmarks") },
                        leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFFD97706)) },
                        onClick = {
                            showOverflowMenu = false
                            onOpenBookmarks()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Create Note from Page") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.testTag("pdf_menu_create_note"),
                        onClick = {
                            showOverflowMenu = false
                            onCreateNoteFromPage()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Export & Share") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        modifier = Modifier.testTag("pdf_menu_export_share"),
                        onClick = {
                            showOverflowMenu = false
                            onSharePdf()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Clear Current Page", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showOverflowMenu = false
                            onClearCurrentPage()
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun PdfSearchGlassBar(
    query: String,
    matchCount: Int,
    activeMatchIdx: Int,
    activeMatchBadge: String? = null,
    onQueryChange: (String) -> Unit,
    onPrevMatch: () -> Unit,
    onNextMatch: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp)
            .heightIn(min = 46.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            "Search PDF text & annotations...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotBlank()) {
                Text(
                    text = if (matchCount == 0) "0" else "${activeMatchIdx + 1}/$matchCount",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (matchCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onPrevMatch, enabled = matchCount > 0, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Match", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onNextMatch, enabled = matchCount > 0, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Match", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(15.dp))
                }
            }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close Search", modifier = Modifier.size(17.dp))
                }
            }
            if (!activeMatchBadge.isNullOrBlank()) {
                Text(
                    text = activeMatchBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 38.dp, end = 12.dp, bottom = 6.dp)
                )
            }
        }
    }
}

@Composable
internal fun PdfTtsPlayerPill(
    isPlaying: Boolean,
    ttsSpeed: Float,
    currentPageNumber: Int,
    onTogglePlayPause: () -> Unit,
    onCycleSpeed: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.96f),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        shadowElevation = 6.dp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                "Reading P.$currentPageNumber",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onCycleSpeed)
            ) {
                Text(
                    "${ttsSpeed}x",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
            IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(30.dp)) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Resume",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onStop, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
internal fun PdfBottomStudioDock(
    toolMode: PdfTool,
    selectedShape: ShapeType,
    highlightColor: Color,
    inkColor: Color,
    textColor: Color,
    activeColor: Color,
    penStrokeWidth: Float,
    textFontSize: Float,
    selectedStickyPalette: PdfStickyPalette,
    stickyAsCard: Boolean,
    selectedAnnotation: PdfPageAnnotation?,
    currentPageIndex: Int,
    totalPages: Int,
    isBookmarked: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    zoomScale: Float,
    hasPanOffset: Boolean,
    onSelectTool: (PdfTool) -> Unit,
    onSelectStickyPalette: (PdfStickyPalette) -> Unit,
    onToggleStickyStyle: (Boolean) -> Unit,
    onOpenStickyWall: () -> Unit,
    onSelectHighlightColor: (Color) -> Unit,
    onSelectPenWidth: (Float) -> Unit,
    onSelectInkColor: (Color) -> Unit,
    onSelectTextSize: (Float) -> Unit,
    onSelectTextColor: (Color) -> Unit,
    onSelectShape: (ShapeType) -> Unit,
    onEditSelectedText: () -> Unit,
    onUpdateSelectedColor: (Color) -> Unit,
    onAdjustSelectedSize: (Float) -> Unit,
    onFlipSelectedCurve: () -> Unit,
    onDuplicateSelected: () -> Unit,
    onBringSelectedToFront: () -> Unit,
    onDeleteSelected: () -> Unit,
    onClearCurrentPage: () -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onJumpPage: () -> Unit,
    onOpenThumbnails: () -> Unit,
    onToggleBookmark: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onResetZoom: () -> Unit,
    onOpenColorPicker: () -> Unit = {},
    onRotateSelected: (Float) -> Unit = {},
    onRotateSelectedTo: (Float) -> Unit = {},
    onResetRotationSelected: () -> Unit = {},
    hasSelectableOnPage: Boolean = false,
    onSelectLatestOnPage: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    linkedNoteTitle: String? = null,
    onLinkNoteToSelected: () -> Unit = {},
    onOpenLinkedNote: () -> Unit = {},
    onCreateNoteFromSelected: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Context-Sensitive Active Tool Options Pill / Card
        AnimatedVisibility(
            visible = toolMode != PdfTool.PAN_ZOOM,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)) + expandVertically(),
            exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) + shrinkVertically()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (toolMode != PdfTool.SELECT && toolMode != PdfTool.ERASER) {
                    ModernToolRotateStrip(
                        selectedAnnotation = selectedAnnotation,
                        hasSelectableOnPage = hasSelectableOnPage,
                        onRotateDelta = onRotateSelected,
                        onRotateTo = onRotateSelectedTo,
                        onResetRotation = onResetRotationSelected,
                        onSelectLatestOnPage = onSelectLatestOnPage,
                        onClearSelection = onClearSelection
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)),
                    shadowElevation = 6.dp,
                    modifier = if (toolMode == PdfTool.SHAPE || (toolMode == PdfTool.SELECT && selectedAnnotation != null)) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier
                    }
                ) {
                when {
                    toolMode == PdfTool.SHAPE -> {
                        val shapePresets = listOf(
                            Color(0xFFDC2626),
                            Color(0xFF2563EB),
                            Color(0xFF16A34A),
                            Color(0xFFF97316),
                            Color(0xFF7C3AED),
                            Color(0xFF1E293B)
                        )
                        val isCustomShapeColor = shapePresets.none { (it.toArgb() and 0xFFFFFF) == (activeColor.toArgb() and 0xFFFFFF) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            // Line 1: All shapes on one screen-fitted row (no sliding needed)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("shape_panel_row_shapes"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ShapeSelectButton(
                                    label = "Rect",
                                    isSelected = selectedShape == ShapeType.RECT,
                                    horizontalPadding = 7.dp,
                                    testTag = "shape_rect_button"
                                ) { onSelectShape(ShapeType.RECT) }
                                ShapeSelectButton(
                                    label = "Circle",
                                    isSelected = selectedShape == ShapeType.CIRCLE,
                                    horizontalPadding = 7.dp,
                                    testTag = "shape_circle_button"
                                ) { onSelectShape(ShapeType.CIRCLE) }
                                ShapeSelectButton(
                                    label = "Star",
                                    isSelected = selectedShape == ShapeType.STAR,
                                    horizontalPadding = 7.dp,
                                    testTag = "shape_star_button"
                                ) { onSelectShape(ShapeType.STAR) }
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(18.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                                ShapeGraphicButton(
                                    shapeType = ShapeType.ARROW,
                                    label = "Arrow",
                                    isSelected = selectedShape == ShapeType.ARROW,
                                    testTag = "shape_arrow_button",
                                    modifier = Modifier.weight(1f)
                                ) { onSelectShape(ShapeType.ARROW) }
                                ShapeGraphicButton(
                                    shapeType = ShapeType.CURVE_ARROW,
                                    label = "Curve Arrow",
                                    isSelected = selectedShape == ShapeType.CURVE_ARROW,
                                    testTag = "shape_curve_arrow_button",
                                    modifier = Modifier.weight(1f)
                                ) { onSelectShape(ShapeType.CURVE_ARROW) }
                                ShapeGraphicButton(
                                    shapeType = ShapeType.S_CURVE_ARROW,
                                    label = "S-Curve Arrow",
                                    isSelected = selectedShape == ShapeType.S_CURVE_ARROW,
                                    testTag = "shape_s_curve_arrow_button",
                                    modifier = Modifier.weight(1f)
                                ) { onSelectShape(ShapeType.S_CURVE_ARROW) }
                                ShapeGraphicButton(
                                    shapeType = ShapeType.DOUBLE_ARROW,
                                    label = "Double Arrow",
                                    isSelected = selectedShape == ShapeType.DOUBLE_ARROW,
                                    testTag = "shape_double_arrow_button",
                                    modifier = Modifier.weight(1f)
                                ) { onSelectShape(ShapeType.DOUBLE_ARROW) }
                                ShapeGraphicButton(
                                    shapeType = ShapeType.ELBOW_ARROW,
                                    label = "Elbow Arrow",
                                    isSelected = selectedShape == ShapeType.ELBOW_ARROW,
                                    testTag = "shape_elbow_arrow_button",
                                    modifier = Modifier.weight(1f)
                                ) { onSelectShape(ShapeType.ELBOW_ARROW) }
                            }

                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )

                            // Line 2: Proportional point weights + all shape color swatches + color grades/gradients button
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("shape_panel_row_style")
                            ) {
                                val weightBtnSize = when {
                                    maxWidth < 260.dp -> 24.dp
                                    maxWidth < 310.dp -> 27.dp
                                    else -> 30.dp
                                }
                                val swatchSize = when {
                                    maxWidth < 260.dp -> 19.dp
                                    maxWidth < 310.dp -> 22.dp
                                    else -> 24.dp
                                }
                                val gradientSize = when {
                                    maxWidth < 260.dp -> 21.dp
                                    maxWidth < 310.dp -> 24.dp
                                    else -> 26.dp
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    listOf(
                                        StrokeDotOption("Thin stroke", 3f, 5.dp, "shape_weight_thin"),
                                        StrokeDotOption("Medium stroke", 5f, 9.dp, "shape_weight_med"),
                                        StrokeDotOption("Bold stroke", 9f, 13.dp, "shape_weight_bold")
                                    ).forEach { opt ->
                                        val isWeightSelected =
                                            penStrokeWidth == opt.weight ||
                                                (opt.weight == 5f && penStrokeWidth == 6f) ||
                                                (opt.weight == 9f && penStrokeWidth >= 11f)
                                        StrokeWeightDotButton(
                                            label = opt.label,
                                            dotSize = opt.dotSize,
                                            dotColor = activeColor,
                                            isSelected = isWeightSelected,
                                            buttonSize = weightBtnSize,
                                            testTag = opt.testTag,
                                            onClick = { onSelectPenWidth(opt.weight) }
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(18.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    )
                                    shapePresets.forEachIndexed { idx, c ->
                                        ColorDotSwatch(
                                            color = c,
                                            size = swatchSize,
                                            isSelected = (activeColor.toArgb() and 0xFFFFFF) == (c.toArgb() and 0xFFFFFF),
                                            testTag = "shape_color_swatch_$idx",
                                            onClick = { onSelectInkColor(c) }
                                        )
                                    }
                                    PdfColorGradientButton(
                                        activeColor = activeColor,
                                        isCustomSelected = isCustomShapeColor,
                                        size = gradientSize,
                                        testTag = "shape_color_gradient_picker",
                                        onClick = onOpenColorPicker
                                    )
                                }
                            }
                        }
                    }

                    toolMode == PdfTool.SELECT && selectedAnnotation != null -> {
                        val sel = selectedAnnotation
                        val paletteColors = when (sel.type) {
                            "COMMENT" -> PdfStickyColors.map { it.fill }
                            "HIGHLIGHT" -> listOf(
                                Color(0xFFFEF08A),
                                Color(0xFF93C5FD),
                                Color(0xFFA7F3D0),
                                Color(0xFFFBCFE8),
                                Color(0xFFFED7AA)
                            )
                            else -> listOf(
                                Color(0xFFDC2626),
                                Color(0xFF2563EB),
                                Color(0xFF16A34A),
                                Color(0xFFF97316),
                                Color(0xFF7C3AED),
                                Color(0xFF1E293B)
                            )
                        }
                        val isCustomSelectedColor = paletteColors.none {
                            (sel.colorArgb and 0xFFFFFF) == (it.toArgb() and 0xFFFFFF)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            // Line 1: Selection label + edit/flip/size/copy/front/delete actions
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .testTag("select_panel_row_actions"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        sel.selectionDisplayLabel(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                    )
                                }
                                if (sel.hasEditableText()) {
                                    DockEditActionChip(
                                        icon = Icons.Default.Edit,
                                        label = "Edit",
                                        testTag = "select_edit_button",
                                        onClick = onEditSelectedText
                                    )
                                }
                                if (linkedNoteTitle != null) {
                                    DockEditActionChip(
                                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                                        label = linkedNoteTitle.take(14),
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        testTag = "select_open_linked_note_button",
                                        onClick = onOpenLinkedNote
                                    )
                                }
                                DockEditActionChip(
                                    icon = Icons.Default.Link,
                                    label = if (linkedNoteTitle != null) "Relink" else "Link Note",
                                    testTag = "select_link_note_button",
                                    onClick = onLinkNoteToSelected
                                )
                                DockEditActionChip(
                                    icon = Icons.AutoMirrored.Filled.NoteAdd,
                                    label = "To Note",
                                    testTag = "select_create_note_button",
                                    onClick = onCreateNoteFromSelected
                                )
                                if (sel.type in setOf("SHAPE_CURVE_ARROW", "SHAPE_S_CURVE_ARROW", "SHAPE_DOUBLE_ARROW", "SHAPE_ELBOW_ARROW")) {
                                    DockEditActionChip(
                                        icon = Icons.Default.SwapHoriz,
                                        label = "Flip",
                                        testTag = "select_flip_button",
                                        onClick = onFlipSelectedCurve
                                    )
                                }
                                // Size - / + controls (resizes sticky notes, text, strokes & shapes)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .testTag("select_size_down")
                                            .clickable { onAdjustSelectedSize(-1f) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Remove, contentDescription = "Smaller", modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Text("Size", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .testTag("select_size_up")
                                            .clickable { onAdjustSelectedSize(1f) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Add, contentDescription = "Larger", modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                                // Rotate -15° / angle reset / +15° controls for lines, curves, shapes & sticky notes
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .testTag("select_rotate_left")
                                            .clickable { onRotateSelected(-15f) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Rotate Left 15°", modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    if (abs(sel.rotationDeg) >= 0.5f) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .testTag("select_rotate_reset")
                                                .clickable { onResetRotationSelected() }
                                        ) {
                                            Text(
                                                "${sel.rotationDeg.roundToInt()}°",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                                            )
                                        }
                                    } else {
                                        Text("Rotate", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .testTag("select_rotate_right")
                                            .clickable { onRotateSelected(15f) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate Right 15°", modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                                DockEditActionChip(
                                    icon = Icons.Default.ContentCopy,
                                    label = "Copy",
                                    testTag = "select_copy_button",
                                    onClick = onDuplicateSelected
                                )
                                DockEditActionChip(
                                    icon = Icons.Default.Layers,
                                    label = "Front",
                                    testTag = "select_front_button",
                                    onClick = onBringSelectedToFront
                                )
                                DockEditActionChip(
                                    icon = Icons.Default.Delete,
                                    label = "Delete",
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    testTag = "select_delete_button",
                                    onClick = onDeleteSelected
                                )
                            }

                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )

                            // Line 2: Color dots + Color Grades & Gradients picker
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("select_panel_row_colors")
                            ) {
                                val swatchSize = if (maxWidth < 270.dp) 21.dp else 24.dp
                                val gradientSize = if (maxWidth < 270.dp) 23.dp else 26.dp
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Color:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    paletteColors.forEach { c ->
                                        ColorDotSwatch(
                                            color = c,
                                            size = swatchSize,
                                            isSelected = (sel.colorArgb and 0xFFFFFF) == (c.toArgb() and 0xFFFFFF),
                                            onClick = { onUpdateSelectedColor(c) }
                                        )
                                    }
                                    PdfColorGradientButton(
                                        activeColor = Color(sel.colorArgb).copy(alpha = 1f),
                                        isCustomSelected = isCustomSelectedColor,
                                        size = gradientSize,
                                        testTag = "select_color_gradient_picker",
                                        onClick = onOpenColorPicker
                                    )
                                }
                            }

                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )

                            // Line 3: Smooth modern 360° Rotation Slider + quick angle presets
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("select_rotate_slider_row"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Angle", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onRotateSelectedTo(-45f) }
                                ) {
                                    Text("-45°", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                }
                                Slider(
                                    value = sel.rotationDeg.coerceIn(-180f, 180f),
                                    onValueChange = { onRotateSelectedTo(it) },
                                    valueRange = -180f..180f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF0284C7),
                                        activeTrackColor = Color(0xFF0284C7)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(26.dp)
                                        .testTag("select_rotate_slider")
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onRotateSelectedTo(45f) }
                                ) {
                                    Text("+45°", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (abs(sel.rotationDeg) >= 0.5f) Color(0xFF0284C7) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onResetRotationSelected() }
                                ) {
                                    Text(
                                        "${sel.rotationDeg.roundToInt()}°",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (abs(sel.rotationDeg) >= 0.5f) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (toolMode) {
                                PdfTool.NOTE -> {
                                    ShapeSelectButton("Card", stickyAsCard) { onToggleStickyStyle(true) }
                                    ShapeSelectButton("Pin", !stickyAsCard) { onToggleStickyStyle(false) }
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(18.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    )
                                    PdfStickyColors.forEach { palette ->
                                        ColorDotSwatch(
                                            color = palette.fill,
                                            isSelected = selectedStickyPalette.name == palette.name,
                                            ringColor = palette.accent,
                                            checkTint = palette.ink,
                                            showCheck = selectedStickyPalette.name == palette.name,
                                            onClick = { onSelectStickyPalette(palette) }
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(onClick = onOpenStickyWall)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.StickyNote2,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                "Wall",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }

                                PdfTool.PEN -> {
                                    listOf(
                                        StrokeDotOption("Fine stroke", 3f, 5.dp, "pen_weight_fine"),
                                        StrokeDotOption("Medium stroke", 6f, 9.dp, "pen_weight_med"),
                                        StrokeDotOption("Bold stroke", 11f, 14.dp, "pen_weight_bold"),
                                        StrokeDotOption("Marker stroke", 18f, 20.dp, "pen_weight_marker")
                                    ).forEach { opt ->
                                        val isWeightSelected =
                                            penStrokeWidth == opt.weight ||
                                                (opt.weight == 6f && penStrokeWidth == 5f) ||
                                                (opt.weight == 11f && penStrokeWidth == 9f)
                                        StrokeWeightDotButton(
                                            label = opt.label,
                                            dotSize = opt.dotSize,
                                            dotColor = inkColor,
                                            isSelected = isWeightSelected,
                                            testTag = opt.testTag,
                                            onClick = { onSelectPenWidth(opt.weight) }
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(18.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    )
                                    val penPresets = listOf(
                                        Color(0xFFDC2626),
                                        Color(0xFF1E293B),
                                        Color(0xFF2563EB),
                                        Color(0xFF16A34A),
                                        Color(0xFFF97316),
                                        Color(0xFF7C3AED),
                                        Color(0xFFDB2777)
                                    )
                                    penPresets.forEach { c ->
                                        ColorDotSwatch(
                                            color = c,
                                            isSelected = inkColor == c,
                                            onClick = { onSelectInkColor(c) }
                                        )
                                    }
                                    PdfColorGradientButton(
                                        activeColor = inkColor,
                                        isCustomSelected = penPresets.none { it == inkColor },
                                        testTag = "pen_color_gradient_picker",
                                        onClick = onOpenColorPicker
                                    )
                                }

                                PdfTool.TEXT_BOX -> {
                                    Text("Text:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    listOf("S" to 26f, "M" to 36f, "L" to 48f).forEach { (label, sz) ->
                                        ShapeSelectButton(label, textFontSize == sz) { onSelectTextSize(sz) }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(18.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    )
                                    val textPresets = listOf(
                                        Color(0xFF1E293B),
                                        Color(0xFFDC2626),
                                        Color(0xFF2563EB),
                                        Color(0xFF16A34A),
                                        Color(0xFF7C3AED),
                                        Color(0xFFF97316)
                                    )
                                    textPresets.forEach { c ->
                                        ColorDotSwatch(
                                            color = c,
                                            isSelected = textColor == c,
                                            onClick = { onSelectTextColor(c) }
                                        )
                                    }
                                    PdfColorGradientButton(
                                        activeColor = textColor,
                                        isCustomSelected = textPresets.none { it == textColor },
                                        testTag = "text_color_gradient_picker",
                                        onClick = onOpenColorPicker
                                    )
                                }

                                PdfTool.HIGHLIGHT -> {
                                    Text("Text Highlight:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    val hlPresets = listOf(
                                        Color(0xFFFEF08A),
                                        Color(0xFF93C5FD),
                                        Color(0xFFA7F3D0),
                                        Color(0xFFFBCFE8),
                                        Color(0xFFFED7AA)
                                    )
                                    hlPresets.forEach { c ->
                                        ColorDotSwatch(
                                            color = c,
                                            isSelected = highlightColor == c,
                                            onClick = { onSelectHighlightColor(c) }
                                        )
                                    }
                                    PdfColorGradientButton(
                                        activeColor = highlightColor,
                                        isCustomSelected = hlPresets.none { it == highlightColor },
                                        testTag = "highlight_color_gradient_picker",
                                        onClick = onOpenColorPicker
                                    )
                                }

                                PdfTool.SELECT -> {
                                    Text(
                                        "Tap any line, curve, shape, or sticky note to move, resize, rotate, or edit",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                PdfTool.UNDERLINE, PdfTool.STRIKETHROUGH -> {
                                    Text(
                                        if (toolMode == PdfTool.UNDERLINE) "Underline:" else "Strike:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val markupPresets = listOf(
                                        Color(0xFFDC2626),
                                        Color(0xFF2563EB),
                                        Color(0xFF16A34A),
                                        Color(0xFFF97316),
                                        Color(0xFF7C3AED),
                                        Color(0xFF1E293B)
                                    )
                                    markupPresets.forEach { c ->
                                        ColorDotSwatch(
                                            color = c,
                                            isSelected = activeColor == c,
                                            onClick = { onSelectInkColor(c) }
                                        )
                                    }
                                    PdfColorGradientButton(
                                        activeColor = activeColor,
                                        isCustomSelected = markupPresets.none { it == activeColor },
                                        testTag = "markup_color_gradient_picker",
                                        onClick = onOpenColorPicker
                                    )
                                }

                                PdfTool.ERASER -> {
                                    Text(
                                        "Tap or drag marks to erase",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(onClick = onClearCurrentPage)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteSweep,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                "Clear Page",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }

                                PdfTool.SHAPE, PdfTool.PAN_ZOOM -> Unit
                            }
                        }
                    }
                }
                }
            }
        }

        // Main Floating Translucent Glass Studio Dock
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // Row 1: Page Scrubber, Undo/Redo, Bookmark, Thumbnails, Zoom Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onPrevPage,
                            enabled = currentPageIndex > 0,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Page", modifier = Modifier.size(20.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onJumpPage)
                        ) {
                            Text(
                                "${currentPageIndex + 1} / $totalPages",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        IconButton(
                            onClick = onNextPage,
                            enabled = currentPageIndex < totalPages - 1,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Page", modifier = Modifier.size(20.dp))
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (zoomScale != 1f || hasPanOffset) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(onClick = onResetZoom)
                            ) {
                                Text(
                                    "${(zoomScale * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onToggleBookmark, modifier = Modifier.size(34.dp)) {
                            Icon(
                                if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark Page",
                                tint = if (isBookmarked) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onOpenThumbnails, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.GridView, contentDescription = "Page Thumbnails", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = 0.6.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )

                // Two-Row Grid of Annotation Tools (All tools visible at once)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModernDockToolPill(
                            icon = Icons.Default.PanTool,
                            label = "Pan",
                            isSelected = toolMode == PdfTool.PAN_ZOOM,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectTool(PdfTool.PAN_ZOOM) }
                        )
                        ModernDockToolPill(
                            icon = Icons.Default.Highlight,
                            label = "Highlight",
                            isSelected = toolMode == PdfTool.HIGHLIGHT,
                            dotColor = highlightColor,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onSelectTool(if (toolMode == PdfTool.HIGHLIGHT) PdfTool.PAN_ZOOM else PdfTool.HIGHLIGHT)
                            }
                        )
                        ModernDockToolPill(
                            icon = Icons.Default.Edit,
                            label = "Pen",
                            isSelected = toolMode == PdfTool.PEN,
                            dotColor = inkColor,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onSelectTool(if (toolMode == PdfTool.PEN) PdfTool.PAN_ZOOM else PdfTool.PEN)
                            }
                        )
                        ModernDockToolPill(
                            icon = Icons.Default.AutoFixHigh,
                            label = "Eraser",
                            isSelected = toolMode == PdfTool.ERASER,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onSelectTool(if (toolMode == PdfTool.ERASER) PdfTool.PAN_ZOOM else PdfTool.ERASER)
                            }
                        )
                        ModernDockToolPill(
                            icon = Icons.Default.TextFields,
                            label = "Text",
                            isSelected = toolMode == PdfTool.TEXT_BOX,
                            dotColor = textColor,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onSelectTool(if (toolMode == PdfTool.TEXT_BOX) PdfTool.PAN_ZOOM else PdfTool.TEXT_BOX)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModernDockToolPill(
                            icon = Icons.Default.NearMe,
                            label = "Select",
                            isSelected = toolMode == PdfTool.SELECT,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onSelectTool(if (toolMode == PdfTool.SELECT) PdfTool.PAN_ZOOM else PdfTool.SELECT)
                            }
                        )
                        ModernDockToolPill(
                            icon = Icons.Default.FormatUnderlined,
                            label = "Underline",
                            isSelected = toolMode == PdfTool.UNDERLINE,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onSelectTool(if (toolMode == PdfTool.UNDERLINE) PdfTool.PAN_ZOOM else PdfTool.UNDERLINE)
                            }
                        )
                        ModernDockToolPill(
                            icon = Icons.Default.FormatStrikethrough,
                            label = "Strike",
                            isSelected = toolMode == PdfTool.STRIKETHROUGH,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onSelectTool(if (toolMode == PdfTool.STRIKETHROUGH) PdfTool.PAN_ZOOM else PdfTool.STRIKETHROUGH)
                            }
                        )
                        ModernDockToolPill(
                            icon = Icons.Default.Category,
                            label = "Shapes",
                            isSelected = toolMode == PdfTool.SHAPE,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onSelectTool(if (toolMode == PdfTool.SHAPE) PdfTool.PAN_ZOOM else PdfTool.SHAPE)
                            }
                        )
                        ModernDockToolPill(
                            icon = Icons.Default.AddComment,
                            label = "Sticky",
                            isSelected = toolMode == PdfTool.NOTE,
                            dotColor = selectedStickyPalette.fill,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onSelectTool(if (toolMode == PdfTool.NOTE) PdfTool.PAN_ZOOM else PdfTool.NOTE)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernToolRotateStrip(
    selectedAnnotation: PdfPageAnnotation?,
    hasSelectableOnPage: Boolean,
    onRotateDelta: (Float) -> Unit,
    onRotateTo: (Float) -> Unit,
    onResetRotation: () -> Unit,
    onSelectLatestOnPage: () -> Unit,
    onClearSelection: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(
            width = 0.8.dp,
            color = if (selectedAnnotation != null) Color(0xFF0284C7).copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        ),
        shadowElevation = 5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tool_rotate_bar")
    ) {
        if (selectedAnnotation != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = selectedAnnotation.selectionDisplayLabel(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .testTag("tool_rotate_left")
                        .clickable { onRotateDelta(-15f) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.RotateLeft,
                            contentDescription = "Rotate Left 15°",
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Slider(
                    value = selectedAnnotation.rotationDeg.coerceIn(-180f, 180f),
                    onValueChange = { onRotateTo(it) },
                    valueRange = -180f..180f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF0284C7),
                        activeTrackColor = Color(0xFF0284C7)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(26.dp)
                        .testTag("tool_rotate_slider")
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (abs(selectedAnnotation.rotationDeg) >= 0.5f) Color(0xFF0284C7) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .testTag("tool_rotate_reset")
                        .clickable { onResetRotation() }
                ) {
                    Text(
                        text = "${selectedAnnotation.rotationDeg.roundToInt()}°",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (abs(selectedAnnotation.rotationDeg) >= 0.5f) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .testTag("tool_rotate_right")
                        .clickable { onRotateDelta(15f) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = "Rotate Right 15°",
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .testTag("tool_rotate_deselect")
                        .clickable { onClearSelection() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear Selection",
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.RotateRight,
                        contentDescription = null,
                        tint = if (hasSelectableOnPage) Color(0xFF0284C7) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (hasSelectableOnPage) "Tap mark or use controls to rotate" else "Draw or place mark to rotate",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (hasSelectableOnPage) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.14f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .testTag("tool_rotate_select_last")
                                .clickable { onSelectLatestOnPage() }
                        ) {
                            Text(
                                "Select Last",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (hasSelectableOnPage) 1f else 0.45f),
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .testTag("tool_rotate_left")
                            .clickable(enabled = hasSelectableOnPage) { onRotateDelta(-15f) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.RotateLeft,
                                contentDescription = "Rotate Left 15°",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (hasSelectableOnPage) 1f else 0.4f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (hasSelectableOnPage) 1f else 0.45f),
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .testTag("tool_rotate_right")
                            .clickable(enabled = hasSelectableOnPage) { onRotateDelta(15f) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.RotateRight,
                                contentDescription = "Rotate Right 15°",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (hasSelectableOnPage) 1f else 0.4f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DockEditActionChip(
    icon: ImageVector,
    label: String,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    testTag: String? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(13.dp))
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ModernDockToolPill(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "tool_pill_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tool_pill_fg"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = if (isSelected) BorderStroke(0.9.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)) else null,
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(18.dp)
                )
                if (dotColor != null) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                            .border(0.6.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                }
            }
            Text(
                text = label,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ColorDotSwatch(
    color: Color,
    isSelected: Boolean,
    size: Dp = 28.dp,
    ringColor: Color = MaterialTheme.colorScheme.primary,
    checkTint: Color = Color.Black,
    showCheck: Boolean = false,
    testTag: String? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) ringColor else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (showCheck) {
            Icon(Icons.Default.Check, contentDescription = null, tint = checkTint, modifier = Modifier.size(14.dp))
        }
    }
}

private data class StrokeDotOption(
    val label: String,
    val weight: Float,
    val dotSize: Dp,
    val testTag: String
)

@Composable
private fun StrokeWeightDotButton(
    label: String,
    dotSize: Dp,
    dotColor: Color,
    isSelected: Boolean,
    testTag: String,
    buttonSize: Dp = 32.dp,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "stroke_dot_bg"
    )
    Surface(
        shape = CircleShape,
        color = bgColor,
        border = BorderStroke(
            width = if (isSelected) 1.8.dp else 0.8.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .size(buttonSize)
            .clip(CircleShape)
            .testTag(testTag)
            .semantics {
                contentDescription = label
                selected = isSelected
            }
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(
                        width = 0.6.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun ShapeGraphicButton(
    shapeType: ShapeType,
    label: String,
    isSelected: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "shape_graphic_bg"
    )
    val fgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        label = "shape_graphic_fg"
    )
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(10.dp))
            .testTag(testTag)
            .semantics {
                contentDescription = label
                selected = isSelected
            }
            .clickable(onClick = onClick)
    ) {
        Canvas(
            modifier = Modifier
                .padding(horizontal = 5.dp, vertical = 5.dp)
        ) {
            val w = size.width
            val h = size.height
            val strokePx = 1.9.dp.toPx()
            val headLen = 5.2.dp.toPx()

            when (shapeType) {
                ShapeType.ARROW -> {
                    val start = Offset(1.dp.toPx(), h - 1.dp.toPx())
                    val tip = Offset(w - 1.dp.toPx(), 1.dp.toPx())
                    val angle = atan2(tip.y - start.y, tip.x - start.x)
                    val shaftEnd = Offset(
                        tip.x - headLen * 0.55f * cos(angle),
                        tip.y - headLen * 0.55f * sin(angle)
                    )
                    drawLine(
                        color = fgColor,
                        start = start,
                        end = shaftEnd,
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                    drawMiniArrowHead(tip, angle, headLen, fgColor)
                }

                ShapeType.CURVE_ARROW -> {
                    val start = Offset(1.dp.toPx(), h * 0.82f)
                    val ctrl = Offset(w * 0.36f, -h * 0.15f)
                    val tip = Offset(w - 1.dp.toPx(), h * 0.45f)
                    val angle = atan2(tip.y - ctrl.y, tip.x - ctrl.x)
                    val shaftEnd = Offset(
                        tip.x - headLen * 0.5f * cos(angle),
                        tip.y - headLen * 0.5f * sin(angle)
                    )
                    val path = Path().apply {
                        moveTo(start.x, start.y)
                        quadraticTo(ctrl.x, ctrl.y, shaftEnd.x, shaftEnd.y)
                    }
                    drawPath(
                        path = path,
                        color = fgColor,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawMiniArrowHead(tip, angle, headLen, fgColor)
                }

                ShapeType.S_CURVE_ARROW -> {
                    val start = Offset(1.dp.toPx(), h * 0.80f)
                    val c1 = Offset(w * 0.38f, -h * 0.20f)
                    val c2 = Offset(w * 0.62f, h * 1.20f)
                    val tip = Offset(w - 1.dp.toPx(), h * 0.22f)
                    val angle = atan2(tip.y - c2.y, tip.x - c2.x)
                    val shaftEnd = Offset(
                        tip.x - headLen * 0.5f * cos(angle),
                        tip.y - headLen * 0.5f * sin(angle)
                    )
                    val path = Path().apply {
                        moveTo(start.x, start.y)
                        cubicTo(c1.x, c1.y, c2.x, c2.y, shaftEnd.x, shaftEnd.y)
                    }
                    drawPath(
                        path = path,
                        color = fgColor,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawMiniArrowHead(tip, angle, headLen, fgColor)
                }

                ShapeType.DOUBLE_ARROW -> {
                    val leftTip = Offset(0.5.dp.toPx(), h * 0.5f)
                    val rightTip = Offset(w - 0.5.dp.toPx(), h * 0.5f)
                    val inset = headLen * 0.55f
                    drawLine(
                        color = fgColor,
                        start = Offset(leftTip.x + inset, leftTip.y),
                        end = Offset(rightTip.x - inset, rightTip.y),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                    drawMiniArrowHead(rightTip, 0f, headLen, fgColor)
                    drawMiniArrowHead(leftTip, PI.toFloat(), headLen, fgColor)
                }

                ShapeType.ELBOW_ARROW -> {
                    val start = Offset(1.dp.toPx(), h * 0.24f)
                    val cornerX = w * 0.74f
                    val cornerY = h * 0.24f
                    val radius = (h * 0.32f).coerceAtMost(cornerX * 0.4f)
                    val tip = Offset(cornerX, h - 0.5.dp.toPx())
                    val path = Path().apply {
                        moveTo(start.x, start.y)
                        lineTo(cornerX - radius, cornerY)
                        quadraticTo(cornerX, cornerY, cornerX, cornerY + radius)
                        lineTo(tip.x, tip.y - headLen * 0.55f)
                    }
                    drawPath(
                        path = path,
                        color = fgColor,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawMiniArrowHead(tip, (PI / 2.0).toFloat(), headLen, fgColor)
                }

                else -> Unit
            }
        }
    }
}

private fun DrawScope.drawMiniArrowHead(
    tip: Offset,
    angleRad: Float,
    headLen: Float,
    color: Color
) {
    val spread = 0.56f
    val p1 = Offset(
        tip.x - headLen * cos(angleRad - spread),
        tip.y - headLen * sin(angleRad - spread)
    )
    val p2 = Offset(
        tip.x - headLen * cos(angleRad + spread),
        tip.y - headLen * sin(angleRad + spread)
    )
    val head = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        close()
    }
    drawPath(head, color = color, style = Fill)
}

@Composable
fun ShapeSelectButton(
    label: String,
    isSelected: Boolean,
    horizontalPadding: Dp = 9.dp,
    testTag: String? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable { onClick() }
    ) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 5.dp)
        )
    }
}
