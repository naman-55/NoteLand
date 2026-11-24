package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.StickyNoteEntity
import com.example.ui.theme.NoteColorPresets
import com.example.ui.theme.editorialBackdropBrush
import com.example.ui.theme.editorialHeaderSubColor
import com.example.ui.theme.editorialHeaderTextColor
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.ui.viewmodel.StickyLayout
import com.example.util.SmartNoteUtils
import java.util.UUID
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private data class StickyAxisBox(
    val cx: Float,
    val cy: Float,
    val hw: Float,
    val hh: Float,
    val uxX: Float,
    val uxY: Float,
    val uyX: Float,
    val uyY: Float
) {
    companion object {
        fun from(layout: StickyLayout): StickyAxisBox {
            val rad = layout.rotationDeg * (Math.PI / 180.0)
            val cosR = cos(rad).toFloat()
            val sinR = sin(rad).toFloat()
            return StickyAxisBox(
                cx = layout.x + layout.width * 0.5f,
                cy = layout.y + layout.height * 0.5f,
                hw = layout.width * 0.5f,
                hh = layout.height * 0.5f,
                uxX = cosR,
                uxY = sinR,
                uyX = -sinR,
                uyY = cosR
            )
        }
    }
}

fun doStickyNotesOverlap(a: StickyLayout, b: StickyLayout): Boolean {
    if (a.id == b.id) return false
    val boxA = StickyAxisBox.from(a)
    val boxB = StickyAxisBox.from(b)
    val dx = boxB.cx - boxA.cx
    val dy = boxB.cy - boxA.cy
    val axes = arrayOf(
        boxA.uxX to boxA.uxY,
        boxA.uyX to boxA.uyY,
        boxB.uxX to boxB.uxY,
        boxB.uyX to boxB.uyY
    )
    for ((ax, ay) in axes) {
        val centerProj = abs(dx * ax + dy * ay)
        val radiusA = boxA.hw * abs(boxA.uxX * ax + boxA.uxY * ay) +
            boxA.hh * abs(boxA.uyX * ax + boxA.uyY * ay)
        val radiusB = boxB.hw * abs(boxB.uxX * ax + boxB.uxY * ay) +
            boxB.hh * abs(boxB.uyX * ax + boxB.uyY * ay)
        if (centerProj >= radiusA + radiusB - 0.5f) {
            return false
        }
    }
    return true
}

fun findOverlappingStickyIds(targetId: String, layouts: Map<String, StickyLayout>): Set<String> {
    val target = layouts[targetId] ?: return emptySet()
    if (layouts.size < 2) return emptySet()
    return layouts.values
        .asSequence()
        .filter { it.id != targetId && doStickyNotesOverlap(target, it) }
        .map { it.id }
        .toSet()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickyBoardScreen(
    viewModel: NoteViewModel,
    onNavigateBack: () -> Unit
) {
    val stickyNotes by viewModel.stickyNotes.collectAsStateWithLifecycle()
    val layouts by viewModel.stickyLayouts.collectAsStateWithLifecycle()
    val currentStickyBoard by viewModel.currentStickyBoard.collectAsStateWithLifecycle()
    val stickyBoards by viewModel.stickyBoards.collectAsStateWithLifecycle()

    var showBoardDropdown by remember { mutableStateOf(false) }
    var showAddBoardDialog by remember { mutableStateOf(false) }
    var showRenameBoardDialog by remember { mutableStateOf<String?>(null) }
    var newBoardName by remember { mutableStateOf("") }

    var selectedNoteId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<StickyNoteEntity?>(null) }
    var showNudgeSheet by remember { mutableStateOf(false) }

    var dialogTitle by remember { mutableStateOf("") }
    var dialogContent by remember { mutableStateOf("") }
    var dialogColorHex by remember { mutableStateOf("#FEF3C7") }

    var viewportWidthDp by remember { mutableFloatStateOf(380f) }
    var viewportHeightDp by remember { mutableFloatStateOf(640f) }
    val density = LocalDensity.current.density

    val hScrollState = rememberScrollState(0)
    val vScrollState = rememberScrollState(0)

    val effectiveLayouts = remember(stickyNotes, layouts) {
        stickyNotes.mapIndexed { index, note ->
            note.id to (layouts[note.id] ?: StickyLayout(
                id = note.id,
                x = 24f + (index % 2) * 190f,
                y = 24f + (index / 2) * 200f,
                zIndex = index
            ))
        }.toMap()
    }
    val orderedBoardIds = remember(stickyNotes) { stickyNotes.map { it.id } }
    val selectedOverlappingIds = remember(selectedNoteId, effectiveLayouts, stickyNotes) {
        val selId = selectedNoteId
        if (selId != null && stickyNotes.size > 1) {
            findOverlappingStickyIds(selId, effectiveLayouts)
        } else {
            emptySet()
        }
    }

    BackHandler {
        onNavigateBack()
    }

    // Default positioning for notes that don't have stored layout coordinates yet
    LaunchedEffect(stickyNotes) {
        stickyNotes.forEachIndexed { index, note ->
            if (!layouts.containsKey(note.id)) {
                val col = index % 2
                val row = index / 2
                val defaultX = 24f + col * 190f
                val defaultY = 24f + row * 200f
                viewModel.updateStickyPosition(note.id, defaultX, defaultY, zIndex = index)
            }
        }
    }

    val isDark = isAppInDarkTheme
    val headerTextColor = editorialHeaderTextColor(isDark)
    val headerSubColor = editorialHeaderSubColor(isDark)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(editorialBackdropBrush(isDark))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFEF3C7),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.StickyNote2,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.clickable { showBoardDropdown = true }) {
                                Text(
                                    currentStickyBoard,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = headerTextColor
                                )
                                Text(
                                    "${stickyNotes.size} stickies • Drag, Resize & Rotate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = headerSubColor
                                )
                                
                                DropdownMenu(
                                    expanded = showBoardDropdown,
                                    onDismissRequest = { showBoardDropdown = false }
                                ) {
                                    stickyBoards.forEach { boardName ->
                                        DropdownMenuItem(
                                            text = { Text(boardName) },
                                            onClick = {
                                                viewModel.setCurrentStickyBoard(boardName)
                                                showBoardDropdown = false
                                            },
                                            trailingIcon = {
                                                if (stickyBoards.size > 1) {
                                                    IconButton(onClick = { 
                                                        viewModel.deleteStickyBoard(boardName) 
                                                        showBoardDropdown = false
                                                    }) {
                                                        Icon(Icons.Default.Delete, "Delete Board")
                                                    }
                                                }
                                            }
                                        )
                                    }
                                    androidx.compose.material3.HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Add New Wall...") },
                                        onClick = {
                                            newBoardName = ""
                                            showAddBoardDialog = true
                                            showBoardDropdown = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.Add, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Rename Current...") },
                                        onClick = {
                                            newBoardName = currentStickyBoard
                                            showRenameBoardDialog = currentStickyBoard
                                            showBoardDropdown = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = headerTextColor)
                        }
                    },
                    actions = {
                        // Accessible Snap to Grid action
                        IconButton(
                            onClick = { viewModel.alignStickiesToGrid(gridSize = 30f) },
                            modifier = Modifier.testTag("snap_grid_button")
                        ) {
                            Icon(Icons.Default.GridOn, contentDescription = "Snap to Grid", tint = MaterialTheme.colorScheme.primary)
                        }

                        // Open Nudge / Position Controls if a sticky note is selected
                        if (selectedNoteId != null) {
                            IconButton(
                                onClick = { showNudgeSheet = true },
                                modifier = Modifier.testTag("nudge_controls_button")
                            ) {
                                Icon(Icons.Default.OpenWith, contentDescription = "Nudge & Resize", tint = Color(0xFFD97706))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    noteToEdit = null
                    dialogTitle = ""
                    dialogContent = ""
                    dialogColorHex = "#FEF3C7"
                    showCreateDialog = true
                },
                containerColor = Color(0xFFD97706),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("create_sticky_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Sticky Note")
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val liveW = maxWidth.value
            val liveH = maxHeight.value
            LaunchedEffect(liveW, liveH) {
                if (viewportWidthDp != liveW) viewportWidthDp = liveW
                if (viewportHeightDp != liveH) viewportHeightDp = liveH
            }

            if (stickyNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.StickyNote2,
                                    contentDescription = null,
                                    tint = Color(0xFF92400E),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Text(
                            "Your Sticky Wall is empty",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "Tap + to add sticky notes to the wall, then drag, resize, rotate, or layer overlapping notes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Interactive 2D Workspace with subtle dot-grid background over editorial backdrop
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(hScrollState)
                        .verticalScroll(vScrollState)
                        .clickable { selectedNoteId = null }
                ) {
                    // Large 2000 x 2000 canvas area for free positioning
                    Box(
                        modifier = Modifier
                            .size(2000.dp, 2000.dp)
                            .drawBehind {
                                val dotColor = Color(0xFF94A3B8).copy(alpha = 0.25f)
                                val spacing = 30.dp.toPx()
                                var x = spacing
                                while (x < size.width) {
                                    var y = spacing
                                    while (y < size.height) {
                                        drawCircle(
                                            color = dotColor,
                                            radius = 1.8f,
                                            center = Offset(x, y)
                                        )
                                        y += spacing
                                    }
                                    x += spacing
                                }
                            }
                    ) {
                        val orderedStickies = remember(stickyNotes, effectiveLayouts) {
                            stickyNotes.sortedWith(
                                compareBy<StickyNoteEntity> { effectiveLayouts[it.id]?.zIndex ?: 0 }
                                    .thenBy { it.createdAtUtc }
                            )
                        }
                        orderedStickies.forEach { note ->
                            val layout = effectiveLayouts[note.id] ?: StickyLayout(note.id, 24f, 24f)
                            val isSelected = selectedNoteId == note.id

                            StickyNoteCanvasItem(
                                note = note,
                                layout = layout,
                                isSelected = isSelected,
                                onSelect = { selectedNoteId = note.id },
                                onPositionCommit = { x, y ->
                                    viewModel.updateStickyPosition(note.id, x, y)
                                },
                                onSizeCommit = { w, h ->
                                    viewModel.updateStickySize(note.id, w, h)
                                },
                                onRotationCommit = { deg ->
                                    viewModel.updateStickyRotation(note.id, deg)
                                },
                                onTogglePin = { viewModel.toggleStickyPin(note) },
                                onDuplicate = { viewModel.duplicateStickyNote(note) },
                                onEdit = {
                                    noteToEdit = note
                                    dialogTitle = note.title
                                    dialogContent = note.content
                                    dialogColorHex = note.colorHex
                                    showCreateDialog = true
                                },
                                onDelete = {
                                    if (selectedNoteId == note.id) selectedNoteId = null
                                    viewModel.deleteStickyNote(note.id)
                                },
                                modifier = Modifier.zIndex(layout.zIndex.toFloat())
                            )
                        }
                    }
                }
            }

            // Overlap Layer Controls ("Move to Front" / "Move to Back") visible only when 2+ notes overlap
            selectedNoteId?.let { selId ->
                if (selectedOverlappingIds.isNotEmpty()) {
                    val selZ = effectiveLayouts[selId]?.zIndex ?: 0
                    val maxOverlapZ = selectedOverlappingIds.maxOfOrNull { effectiveLayouts[it]?.zIndex ?: 0 } ?: selZ
                    val minOverlapZ = selectedOverlappingIds.minOfOrNull { effectiveLayouts[it]?.zIndex ?: 0 } ?: selZ
                    val isAlreadyFront = selZ > maxOverlapZ
                    val isAlreadyBack = selZ < minOverlapZ

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .navigationBarsPadding()
                            .padding(start = 16.dp, bottom = 16.dp, end = 88.dp)
                            .testTag("sticky_overlap_layer_bar")
                    ) {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isAlreadyFront) Color(0xFFD97706) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .heightIn(min = 38.dp)
                                    .clickable {
                                        viewModel.moveStickyToFront(selId, orderedBoardIds)
                                    }
                                    .testTag("sticky_move_to_front")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FlipToFront,
                                        contentDescription = null,
                                        tint = if (isAlreadyFront) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Move to Front",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAlreadyFront) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isAlreadyBack) Color(0xFFD97706) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .heightIn(min = 38.dp)
                                    .clickable {
                                        viewModel.moveStickyToBack(selId, orderedBoardIds)
                                    }
                                    .testTag("sticky_move_to_back")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FlipToBack,
                                        contentDescription = null,
                                        tint = if (isAlreadyBack) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Move to Back",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAlreadyBack) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    // Accessible Alternative to Drag & Resize (Nudge & Dimensions Sheet)
    if (showNudgeSheet) {
        selectedNoteId?.let { selectedId ->
            val currentLayout = layouts[selectedId] ?: StickyLayout(selectedId, 20f, 20f)
            val selectedNote = stickyNotes.firstOrNull { it.id == selectedId }

        ModalBottomSheet(
            onDismissRequest = { showNudgeSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Position & Size: ${selectedNote?.title?.ifBlank { "Sticky Note" } ?: "Sticky Note"}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Position, Size & Rotation Display
                Text(
                    text = "Pos: (${currentLayout.x.roundToInt()}dp, ${currentLayout.y.roundToInt()}dp) • Size: ${currentLayout.width.roundToInt()}×${currentLayout.height.roundToInt()}dp • Angle: ${currentLayout.rotationDeg.roundToInt()}°",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Overlapping note layer order ("Move to Front" / "Move to Back")
                val sheetOverlappingIds = if (stickyNotes.size > 1) {
                    findOverlappingStickyIds(selectedId, effectiveLayouts)
                } else {
                    emptySet()
                }
                if (sheetOverlappingIds.isNotEmpty()) {
                    val selZ = currentLayout.zIndex
                    val maxOverlapZ = sheetOverlappingIds.maxOfOrNull { effectiveLayouts[it]?.zIndex ?: 0 } ?: selZ
                    val minOverlapZ = sheetOverlappingIds.minOfOrNull { effectiveLayouts[it]?.zIndex ?: 0 } ?: selZ
                    val isAlreadyFront = selZ > maxOverlapZ
                    val isAlreadyBack = selZ < minOverlapZ

                    Text("Layer Order (Overlapping):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isAlreadyFront) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isAlreadyFront) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp)
                                .clickable {
                                    viewModel.moveStickyToFront(selectedId, orderedBoardIds)
                                }
                                .testTag("nudge_sheet_move_to_front")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.FlipToFront,
                                    contentDescription = null,
                                    tint = if (isAlreadyFront) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Move to Front",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAlreadyFront) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isAlreadyBack) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isAlreadyBack) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp)
                                .clickable {
                                    viewModel.moveStickyToBack(selectedId, orderedBoardIds)
                                }
                                .testTag("nudge_sheet_move_to_back")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.FlipToBack,
                                    contentDescription = null,
                                    tint = if (isAlreadyBack) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Move to Back",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAlreadyBack) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // D-Pad Nudge Buttons for accessibility
                Text("Nudge Position:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.nudgeSticky(selectedId, deltaX = 0f, deltaY = -30f) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.nudgeSticky(selectedId, deltaX = -30f, deltaY = 0f) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Move Left")
                    }
                    Spacer(Modifier.width(24.dp))
                    IconButton(
                        onClick = { viewModel.nudgeSticky(selectedId, deltaX = 30f, deltaY = 0f) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Move Right")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.nudgeSticky(selectedId, deltaX = 0f, deltaY = 30f) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
                    }
                }

                // Dimension controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Resize Dimensions:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(
                        onClick = {
                            viewModel.updateStickySize(
                                selectedId,
                                StickyLayout.DEFAULT_WIDTH,
                                StickyLayout.DEFAULT_HEIGHT
                            )
                        }
                    ) {
                        Text("Reset (${StickyLayout.DEFAULT_WIDTH.roundToInt()}×${StickyLayout.DEFAULT_HEIGHT.roundToInt()})")
                    }
                }

                // Quick size presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        "Compact" to (130f to 120f),
                        "Standard" to (170f to 180f),
                        "Wide" to (280f to 180f),
                        "Tall" to (180f to 310f),
                        "Large" to (320f to 320f)
                    )
                    presets.forEach { (label, dims) ->
                        val isMatch = kotlin.math.abs(currentLayout.width - dims.first) < 8f &&
                            kotlin.math.abs(currentLayout.height - dims.second) < 8f
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isMatch) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isMatch) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.clickable {
                                viewModel.updateStickySize(selectedId, dims.first, dims.second)
                            }
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isMatch) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                // Smooth Width & Height Sliders
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Width: ${currentLayout.width.roundToInt()} dp",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = currentLayout.width.coerceIn(StickyLayout.MIN_WIDTH, StickyLayout.MAX_WIDTH),
                        onValueChange = { newWidth ->
                            viewModel.updateStickySize(selectedId, newWidth, currentLayout.height)
                        },
                        valueRange = StickyLayout.MIN_WIDTH..StickyLayout.MAX_WIDTH
                    )
                    Text(
                        "Height: ${currentLayout.height.roundToInt()} dp",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = currentLayout.height.coerceIn(StickyLayout.MIN_HEIGHT, StickyLayout.MAX_HEIGHT),
                        onValueChange = { newHeight ->
                            viewModel.updateStickySize(selectedId, currentLayout.width, newHeight)
                        },
                        valueRange = StickyLayout.MIN_HEIGHT..StickyLayout.MAX_HEIGHT
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.updateStickySize(selectedId, currentLayout.width - 20f, currentLayout.height) }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
                            Text("- Width", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.updateStickySize(selectedId, currentLayout.width + 20f, currentLayout.height) }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
                            Text("+ Width", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.updateStickySize(selectedId, currentLayout.width, currentLayout.height - 20f) }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
                            Text("- Height", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.updateStickySize(selectedId, currentLayout.width, currentLayout.height + 20f) }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
                            Text("+ Height", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Rotation Controls
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Rotation: ${currentLayout.rotationDeg.roundToInt()}°",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    viewModel.updateStickyRotation(selectedId, currentLayout.rotationDeg - 15f)
                                }
                            ) {
                                Text("-15°", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.clickable {
                                    viewModel.updateStickyRotation(selectedId, 0f)
                                }
                            ) {
                                Text(
                                    "0°",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    viewModel.updateStickyRotation(selectedId, currentLayout.rotationDeg + 15f)
                                }
                            ) {
                                Text("+15°", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                    Slider(
                        value = currentLayout.rotationDeg.coerceIn(-180f, 180f),
                        onValueChange = { angle ->
                            viewModel.updateStickyRotation(selectedId, angle)
                        },
                        valueRange = -180f..180f
                    )
                }

                // Snap to Grid & Done button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            viewModel.alignStickiesToGrid(gridSize = 30f)
                        }
                    ) {
                        Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Align All to Grid")
                    }

                    TextButton(onClick = { showNudgeSheet = false }) {
                        Text("Done")
                    }
                }

                Spacer(Modifier.height(10.dp))
            }
        }
        }
    }

    // Create / Edit Sticky Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(if (noteToEdit == null) "New Sticky Note" else "Edit Sticky Note")
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = dialogTitle,
                        onValueChange = { dialogTitle = it },
                        label = { Text("Title (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dialogContent,
                        onValueChange = { dialogContent = it },
                        label = { Text("Note content...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Pick Color:", style = MaterialTheme.typography.labelMedium)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("#FEF3C7", "#DCFCE7", "#E0F2FE", "#FFE4E6", "#EDE9FE", "#FFEDD5").forEach { hex ->
                            val color = NoteColorPresets.firstOrNull { it.hexCode == hex }?.lightBackground ?: Color.Yellow
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(color, shape = CircleShape)
                                    .border(
                                        width = if (dialogColorHex.equals(hex, ignoreCase = true)) 3.dp else 1.dp,
                                        color = if (dialogColorHex.equals(hex, ignoreCase = true)) MaterialTheme.colorScheme.primary else Color(0x33000000),
                                        shape = CircleShape
                                    )
                                    .clickable { dialogColorHex = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (dialogContent.isNotBlank() || dialogTitle.isNotBlank()) {
                            val id = noteToEdit?.id ?: UUID.randomUUID().toString()
                            val entity = (noteToEdit ?: StickyNoteEntity(id = id, boardName = currentStickyBoard)).copy(
                                title = dialogTitle.trim(),
                                content = dialogContent.trim(),
                                colorHex = dialogColorHex,
                                updatedAtUtc = System.currentTimeMillis()
                            )
                            viewModel.saveStickyNote(entity)

                            if (noteToEdit == null) {
                                val scrollX_dp = hScrollState.value / density
                                val scrollY_dp = vScrollState.value / density
                                val stagger = (stickyNotes.size % 4) * 24f
                                val maxX = (2000f - StickyLayout.DEFAULT_WIDTH).coerceAtLeast(12f)
                                val maxY = (2000f - StickyLayout.DEFAULT_HEIGHT).coerceAtLeast(12f)
                                val centerX = (scrollX_dp + (viewportWidthDp - StickyLayout.DEFAULT_WIDTH) / 2f - 36f + stagger).coerceIn(12f, maxX)
                                val centerY = (scrollY_dp + (viewportHeightDp - StickyLayout.DEFAULT_HEIGHT) / 2f - 36f + stagger).coerceIn(12f, maxY)
                                viewModel.updateStickyPosition(id, centerX, centerY)
                            }
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddBoardDialog) {
        AlertDialog(
            onDismissRequest = { showAddBoardDialog = false },
            title = { Text("New Sticky Wall") },
            text = {
                OutlinedTextField(
                    value = newBoardName,
                    onValueChange = { newBoardName = it },
                    label = { Text("Wall Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newBoardName.isNotBlank()) {
                        viewModel.addStickyBoard(newBoardName)
                    }
                    showAddBoardDialog = false
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBoardDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRenameBoardDialog != null) {
        AlertDialog(
            onDismissRequest = { showRenameBoardDialog = null },
            title = { Text("Rename Sticky Wall") },
            text = {
                OutlinedTextField(
                    value = newBoardName,
                    onValueChange = { newBoardName = it },
                    label = { Text("Wall Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newBoardName.isNotBlank()) {
                        viewModel.renameStickyBoard(showRenameBoardDialog!!, newBoardName)
                    }
                    showRenameBoardDialog = null
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameBoardDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun StickyNoteCanvasItem(
    note: StickyNoteEntity,
    layout: StickyLayout,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPositionCommit: (Float, Float) -> Unit,
    onSizeCommit: (Float, Float) -> Unit,
    onRotationCommit: (Float) -> Unit = {},
    onTogglePin: () -> Unit,
    onDuplicate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density

    val cardBg = remember(note.colorHex) {
        val preset = NoteColorPresets.firstOrNull { it.hexCode.equals(note.colorHex, ignoreCase = true) }
        preset?.lightBackground ?: Color(0xFFFEF3C7)
    }

    var dragOffsetX by remember(note.id) { mutableFloatStateOf(layout.x) }
    var dragOffsetY by remember(note.id) { mutableFloatStateOf(layout.y) }
    var isDraggingPosition by remember(note.id) { mutableStateOf(false) }

    var liveWidth by remember(note.id) { mutableFloatStateOf(layout.width) }
    var liveHeight by remember(note.id) { mutableFloatStateOf(layout.height) }
    var isResizing by remember(note.id) { mutableStateOf(false) }

    var liveRotation by remember(note.id) { mutableFloatStateOf(layout.rotationDeg) }
    var isRotating by remember(note.id) { mutableStateOf(false) }

    LaunchedEffect(layout.x, layout.y, isDraggingPosition) {
        if (!isDraggingPosition) {
            dragOffsetX = layout.x
            dragOffsetY = layout.y
        }
    }

    LaunchedEffect(layout.width, layout.height, isResizing) {
        if (!isResizing) {
            liveWidth = layout.width
            liveHeight = layout.height
        }
    }

    LaunchedEffect(layout.rotationDeg, isRotating) {
        if (!isRotating) {
            liveRotation = layout.rotationDeg
        }
    }

    val isCompactWidth = liveWidth < 150f
    val isCompactHeight = liveHeight < 140f
    val contentPadding = if (isCompactWidth || isCompactHeight) 8.dp else 10.dp
    val headerBtnSize = if (isCompactWidth) 20.dp else 24.dp
    val headerIconSize = if (isCompactWidth) 12.dp else 14.dp
    val bodyFontSize = if (liveWidth >= 260f && liveHeight >= 220f) 13.sp else if (isCompactWidth) 11.sp else 12.sp
    val bodyLineHeight = if (liveWidth >= 260f && liveHeight >= 220f) 19.sp else if (isCompactWidth) 15.sp else 17.sp
    val accentColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .offset { IntOffset(dragOffsetX.dp.roundToPx(), dragOffsetY.dp.roundToPx()) }
            .size(liveWidth.dp, liveHeight.dp)
            .graphicsLayer { rotationZ = liveRotation }
            .testTag("sticky_note_container_${note.id}")
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(note.id, density) {
                    detectDragGestures(
                        onDragStart = {
                            isDraggingPosition = true
                            onSelect()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val maxX = (2000f - liveWidth).coerceAtLeast(10f)
                            val maxY = (2000f - liveHeight).coerceAtLeast(10f)
                            val rad = liveRotation * (Math.PI / 180.0)
                            val cosR = cos(rad).toFloat()
                            val sinR = sin(rad).toFloat()
                            val screenDx = dragAmount.x * cosR - dragAmount.y * sinR
                            val screenDy = dragAmount.x * sinR + dragAmount.y * cosR
                            dragOffsetX = (dragOffsetX + screenDx / density).coerceIn(8f, maxX)
                            dragOffsetY = (dragOffsetY + screenDy / density).coerceIn(8f, maxY)
                        },
                        onDragEnd = {
                            isDraggingPosition = false
                            onPositionCommit(dragOffsetX, dragOffsetY)
                        },
                        onDragCancel = {
                            isDraggingPosition = false
                            onPositionCommit(dragOffsetX, dragOffsetY)
                        }
                    )
                }
                .clickable { onSelect() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(
                width = if (isSelected || isResizing || isRotating) 2.dp else 1.dp,
                color = if (isSelected || isResizing || isRotating) accentColor else Color(0x22000000)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected || isResizing || isRotating) 8.dp else 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                // Sticky Header: Drag icon, Pin, Duplicate, Edit, Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = "Drag Sticky",
                            tint = Color.Gray.copy(alpha = 0.7f),
                            modifier = Modifier.size(if (isCompactWidth) 14.dp else 16.dp)
                        )
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier.size(headerBtnSize)
                        ) {
                            Icon(
                                if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin",
                                tint = if (note.isPinned) Color(0xFFB45309) else Color.Gray,
                                modifier = Modifier.size(headerIconSize)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onDuplicate,
                            modifier = Modifier.size(headerBtnSize)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Duplicate",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(headerIconSize)
                            )
                        }

                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(headerBtnSize)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(headerIconSize)
                            )
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(headerBtnSize)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(headerIconSize)
                            )
                        }
                    }
                }

                if (note.title.isNotBlank()) {
                    Text(
                        text = note.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isCompactWidth) 12.sp else 14.sp,
                        color = Color(0xFF1E293B),
                        maxLines = if (liveHeight >= 220f) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(if (isCompactHeight) 2.dp else 4.dp))
                }

                Text(
                    text = note.content,
                    fontSize = bodyFontSize,
                    lineHeight = bodyLineHeight,
                    color = Color(0xFF334155),
                    modifier = Modifier.weight(1f),
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                // Footer date padded clear of bottom handles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp, end = 30.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = SmartNoteUtils.formatShortDate(note.updatedAtUtc),
                        fontSize = 9.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Live dimension indicator badge while stretching/shrinking
        if (isResizing) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xD90F172A),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 36.dp)
                    .testTag("sticky_size_badge_${note.id}")
            ) {
                Text(
                    text = "${liveWidth.roundToInt()} × ${liveHeight.roundToInt()}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Live rotation angle badge when rotating or rotated
        if (isRotating || (isSelected && abs(liveRotation) >= 0.5f)) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xD90EA5E9),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 36.dp, bottom = 8.dp)
                    .testTag("sticky_angle_badge_${note.id}")
            ) {
                Text(
                    text = "${liveRotation.roundToInt()}°",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Bottom-Left Interactive Rotate Handle (always available when selected or rotating)
        if (isSelected || isRotating) {
            val handlePx = 32f * density
            var prevScreenAngle = 0f
            var unclampedRotation = 0f
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(32.dp)
                    .testTag("sticky_rotate_handle_${note.id}")
                    .pointerInput(note.id, density, liveWidth, liveHeight) {
                        fun computeScreenAngle(localHandlePos: Offset): Float? {
                            val cardW = liveWidth * density
                            val cardH = liveHeight * density
                            val cardPt = Offset(localHandlePos.x, cardH - handlePx + localHandlePos.y)
                            val relX = cardPt.x - cardW * 0.5f
                            val relY = cardPt.y - cardH * 0.5f
                            if (hypot(relX, relY) < 8f) return null
                            val localDeg = (atan2(relY, relX) * (180.0 / Math.PI)).toFloat()
                            return liveRotation + localDeg
                        }
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                isRotating = true
                                onSelect()
                                unclampedRotation = liveRotation
                                prevScreenAngle = computeScreenAngle(startOffset) ?: liveRotation
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val currScreenAngle = computeScreenAngle(change.position) ?: return@detectDragGestures
                                var delta = currScreenAngle - prevScreenAngle
                                while (delta > 180f) delta -= 360f
                                while (delta <= -180f) delta += 360f
                                prevScreenAngle = currScreenAngle
                                unclampedRotation = StickyLayout.normalizeAngle(unclampedRotation + delta)
                                liveRotation = snapAngleDeg(unclampedRotation)
                            },
                            onDragEnd = {
                                isRotating = false
                                onRotationCommit(liveRotation)
                            },
                            onDragCancel = {
                                isRotating = false
                                onRotationCommit(liveRotation)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0EA5E9),
                    border = BorderStroke(1.5.dp, Color.White),
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = "Rotate Sticky Note",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        // Right-Edge Horizontal Resize Handle (Stretch/Shrink Width)
        if (isSelected || isResizing) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(width = 18.dp, height = if (liveHeight < 150f) 34.dp else 48.dp)
                    .testTag("sticky_resize_width_${note.id}")
                    .pointerInput(note.id, density) {
                        detectDragGestures(
                            onDragStart = {
                                isResizing = true
                                onSelect()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                liveWidth = StickyLayout.clampWidth(liveWidth + dragAmount.x / density)
                            },
                            onDragEnd = {
                                isResizing = false
                                onSizeCommit(liveWidth, liveHeight)
                            },
                            onDragCancel = {
                                isResizing = false
                                onSizeCommit(liveWidth, liveHeight)
                            }
                        )
                    },
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(width = 4.dp, height = 26.dp)
                        .background(accentColor.copy(alpha = 0.85f), RoundedCornerShape(2.dp))
                )
            }

            // Bottom-Edge Vertical Resize Handle (Stretch/Shrink Height)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(width = if (liveWidth < 150f) 34.dp else 48.dp, height = 18.dp)
                    .testTag("sticky_resize_height_${note.id}")
                    .pointerInput(note.id, density) {
                        detectDragGestures(
                            onDragStart = {
                                isResizing = true
                                onSelect()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                liveHeight = StickyLayout.clampHeight(liveHeight + dragAmount.y / density)
                            },
                            onDragEnd = {
                                isResizing = false
                                onSizeCommit(liveWidth, liveHeight)
                            },
                            onDragCancel = {
                                isResizing = false
                                onSizeCommit(liveWidth, liveHeight)
                            }
                        )
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .size(width = 26.dp, height = 4.dp)
                        .background(accentColor.copy(alpha = 0.85f), RoundedCornerShape(2.dp))
                )
            }
        }

        // Bottom-Right Corner Diagonal Resize Handle (Always Accessible)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(38.dp)
                .testTag("sticky_resize_corner_${note.id}")
                .pointerInput(note.id, density) {
                    detectDragGestures(
                        onDragStart = {
                            isResizing = true
                            onSelect()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            liveWidth = StickyLayout.clampWidth(liveWidth + dragAmount.x / density)
                            liveHeight = StickyLayout.clampHeight(liveHeight + dragAmount.y / density)
                        },
                        onDragEnd = {
                            isResizing = false
                            onSizeCommit(liveWidth, liveHeight)
                        },
                        onDragCancel = {
                            isResizing = false
                            onSizeCommit(liveWidth, liveHeight)
                        }
                    )
                },
            contentAlignment = Alignment.BottomEnd
        ) {
            Canvas(
                modifier = Modifier
                    .padding(6.dp)
                    .size(16.dp)
            ) {
                val gripColor = if (isSelected || isResizing) accentColor else Color(0xFF475569).copy(alpha = 0.72f)
                val strokePx = 1.8.dp.toPx()
                // Crisp 3-line diagonal corner resize grip
                drawLine(
                    color = gripColor,
                    start = Offset(size.width * 0.25f, size.height),
                    end = Offset(size.width, size.height * 0.25f),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = gripColor,
                    start = Offset(size.width * 0.55f, size.height),
                    end = Offset(size.width, size.height * 0.55f),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = gripColor,
                    start = Offset(size.width * 0.82f, size.height),
                    end = Offset(size.width, size.height * 0.82f),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
