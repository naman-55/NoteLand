package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CanvasEntity
import com.example.data.model.NoteType
import com.example.data.storage.AttachmentStorageManager
import com.example.ui.screens.mindmap.MindMapBoardSwitcherSheet
import com.example.ui.screens.mindmap.MindMapBottomStudioDock
import com.example.ui.screens.mindmap.MindMapDrawerTab
import com.example.ui.screens.mindmap.MindMapExportDialog
import com.example.ui.screens.mindmap.MindMapFindDialog
import com.example.ui.screens.mindmap.MindMapIconPickerDialog
import com.example.ui.screens.mindmap.MindMapLayersSheet
import com.example.ui.screens.mindmap.MindMapLayoutType
import com.example.ui.screens.mindmap.MindMapNode
import com.example.ui.screens.mindmap.MindMapNodeEditDialog
import com.example.ui.screens.mindmap.MindMapNodeShape
import com.example.ui.screens.mindmap.MindMapSettingsDialog
import com.example.ui.screens.mindmap.MindMapStyleConfig
import com.example.ui.screens.mindmap.MindMapTextOrStickyDialog
import com.example.ui.screens.mindmap.MindMapThemePreset
import com.example.ui.screens.mindmap.applyMindMapLayout
import com.example.ui.screens.mindmap.applyThemeToNodes
import com.example.ui.screens.mindmap.createAiInEducationMindMap
import com.example.ui.screens.mindmap.createStarterMindMap
import com.example.ui.screens.mindmap.drawMindMapBackdrop
import com.example.ui.screens.mindmap.drawMindMapBranches
import com.example.ui.screens.mindmap.drawMindMapNode
import com.example.ui.screens.mindmap.exportMindMap
import com.example.ui.screens.mindmap.hasChildNodes
import com.example.ui.screens.mindmap.hiddenDescendantCount
import com.example.ui.screens.mindmap.visibleMindMapNodes
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel
import java.util.UUID
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class MindMapHistorySnapshot(
    val nodes: List<MindMapNode>,
    val items: List<CanvasItem>,
    val config: MindMapStyleConfig
)

private val HeaderEmerald = Color(0xFF16A34A)
private val HeaderEmeraldDeep = Color(0xFF059669)

@Composable
fun CanvasScreen(
    noteId: String,
    viewModel: NoteViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = isAppInDarkTheme
    val storageManager = remember(context) { AttachmentStorageManager(context) }
    val allCanvases by viewModel.canvases.collectAsStateWithLifecycle()

    // Active board metadata
    var activeCanvasId by remember(noteId) {
        mutableStateOf(if (noteId == "0" || noteId.isBlank()) UUID.randomUUID().toString() else noteId)
    }
    var isHydratingBoard by remember { mutableStateOf(true) }
    var canvasTitle by remember(activeCanvasId) {
        mutableStateOf(
            allCanvases.firstOrNull { it.id == activeCanvasId }?.title?.takeIf { it.isNotBlank() }
                ?: "Untitled Mind Map"
        )
    }
    LaunchedEffect(activeCanvasId, allCanvases) {
        if (isHydratingBoard) {
            val match = allCanvases.firstOrNull { it.id == activeCanvasId }
            if (match != null && match.title.isNotBlank()) {
                canvasTitle = match.title
            }
        }
    }
    var canvasBackgroundHex by remember { mutableStateOf("#FFFFFF") }
    var canvasCreatedAtUtc by remember { mutableStateOf(System.currentTimeMillis()) }
    var canvasWidth by remember { mutableFloatStateOf(2000f) }
    var canvasHeight by remember { mutableFloatStateOf(2000f) }
    var isLegacyCanvasNote by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var editRevision by remember { mutableIntStateOf(0) }

    // Mind Map + Freehand Canvas data state
    val mindMapNodes = remember { mutableStateListOf<MindMapNode>() }
    val elements = remember { mutableStateListOf<CanvasItem>() }
    var styleConfig by remember { mutableStateOf(MindMapStyleConfig()) }
    val imageBitmapCache = remember { mutableStateMapOf<String, ImageBitmap>() }

    // Selection & UI mode state
    var activeTab by remember { mutableStateOf(MindMapDrawerTab.INSERT) }
    var isDrawerOpen by remember { mutableStateOf(true) }
    var activeTool by remember { mutableStateOf(CanvasToolMode.SELECT) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    var multiSelectedNodeIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var searchHighlightNodeId by remember { mutableStateOf<String?>(null) }
    var activeStrokeColor by remember { mutableStateOf(Color(0xFF16A34A)) }

    // History stacks (bounded to 30 snapshots)
    val undoStack = remember { mutableStateListOf<MindMapHistorySnapshot>() }
    val redoStack = remember { mutableStateListOf<MindMapHistorySnapshot>() }

    // Viewport camera state
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val zoomAnim = remember { Animatable(1.0f) }

    // Transient drag/draw states
    var currentStrokePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var dragStartWorld by remember { mutableStateOf<Offset?>(null) }
    var dragCurrentWorld by remember { mutableStateOf<Offset?>(null) }
    var draggingNodeId by remember { mutableStateOf<String?>(null) }
    var draggingItemId by remember { mutableStateOf<String?>(null) }
    var hasPushedDragUndo by remember { mutableStateOf(false) }

    // Dialog / Sheet visibility
    var editingNode by remember { mutableStateOf<MindMapNode?>(null) }
    var showBoardSwitcher by remember { mutableStateOf(false) }
    var showLayersSheet by remember { mutableStateOf(false) }
    var showFindDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var pendingTextWorldPos by remember { mutableStateOf<Offset?>(null) }
    var pendingTextIsSticky by remember { mutableStateOf(false) }

    // Responsive base fit scale so the entire Mind Map sits beautifully inside any phone width at 100% zoom
    val baseFitScale = remember(viewportSize.width) {
        if (viewportSize.width <= 0) 1f else (viewportSize.width / 870f).coerceIn(0.62f, 1.15f)
    }

    fun screenToWorld(screenPt: Offset): Offset {
        val effScale = zoom * baseFitScale
        return Offset(
            (screenPt.x - viewportSize.width / 2f - pan.x) / effScale,
            (screenPt.y - viewportSize.height / 2f - pan.y) / effScale
        )
    }

    fun recordUndo() {
        if (undoStack.size >= 30) undoStack.removeAt(0)
        undoStack.add(
            MindMapHistorySnapshot(
                nodes = mindMapNodes.toList(),
                items = elements.toList(),
                config = styleConfig
            )
        )
        redoStack.clear()
        hasUnsavedChanges = true
        editRevision++
    }

    fun performUndo() {
        if (undoStack.isEmpty()) return
        redoStack.add(MindMapHistorySnapshot(mindMapNodes.toList(), elements.toList(), styleConfig))
        val prev = undoStack.removeAt(undoStack.lastIndex)
        mindMapNodes.clear(); mindMapNodes.addAll(prev.nodes)
        elements.clear(); elements.addAll(prev.items)
        styleConfig = prev.config
        hasUnsavedChanges = true
        editRevision++
    }

    fun performRedo() {
        if (redoStack.isEmpty()) return
        undoStack.add(MindMapHistorySnapshot(mindMapNodes.toList(), elements.toList(), styleConfig))
        val next = redoStack.removeAt(redoStack.lastIndex)
        mindMapNodes.clear(); mindMapNodes.addAll(next.nodes)
        elements.clear(); elements.addAll(next.items)
        styleConfig = next.config
        hasUnsavedChanges = true
        editRevision++
    }

    fun persistCurrentBoard(showErrorToast: Boolean = true) {
        val idToSave = activeCanvasId
        val fallbackTitle = if (!styleConfig.showMintWaves) "Untitled" else "Untitled Mind Map"
        val titleToSave = canvasTitle.trim().ifBlank { fallbackTitle }
        val bgHex = canvasBackgroundHex
        val createdUtc = canvasCreatedAtUtc
        val w = canvasWidth
        val h = canvasHeight
        val isLegacy = isLegacyCanvasNote

        val allEntities = buildList {
            add(styleConfig.toEntity(idToSave))
            mindMapNodes.forEachIndexed { idx, node ->
                add(node.toEntity(idToSave, zIndex = idx))
            }
            elements.forEachIndexed { idx, item ->
                add(item.copy(zIndex = mindMapNodes.size + idx).toEntity(idToSave))
            }
        }

        if (isLegacy) {
            val canvasJson = org.json.JSONArray().apply {
                elements.filter { it.type == "STROKE" || it.type == "HIGHLIGHTER" }.forEach { item ->
                    val obj = org.json.JSONObject().apply {
                        put("color", item.colorArgb)
                        put("width", item.strokeWidth.toDouble())
                        val pts = org.json.JSONArray()
                        item.points.forEach { p ->
                            pts.put(org.json.JSONObject().apply { put("x", p.x.toDouble()); put("y", p.y.toDouble()) })
                        }
                        put("points", pts)
                    }
                    put(obj)
                }
            }.toString()
            coroutineScope.launch {
                val existingNote = viewModel.getNoteByIdDirect(idToSave)
                if (existingNote != null) {
                    viewModel.saveNote(
                        existingNote.copy(
                            title = titleToSave,
                            canvasDataJson = canvasJson,
                            updatedAtUtc = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        val entity = CanvasEntity(
            id = idToSave,
            title = titleToSave,
            backgroundColorHex = bgHex,
            width = w,
            height = h,
            createdAtUtc = createdUtc,
            updatedAtUtc = System.currentTimeMillis()
        )
        viewModel.saveCanvasWithElements(entity, allEntities) { result ->
            if (result.isSuccess) {
                hasUnsavedChanges = false
            } else {
                hasUnsavedChanges = true
                if (showErrorToast) {
                    Toast.makeText(context, "Unable to save mind map", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun animateZoomTo(target: Float, resetPan: Boolean = false) {
        val clamped = target.coerceIn(0.4f, 3.0f)
        coroutineScope.launch {
            zoomAnim.snapTo(zoom)
            zoomAnim.animateTo(clamped, tween(180, easing = FastOutSlowInEasing)) {
                zoom = value
            }
            zoom = clamped
            if (resetPan) pan = Offset.Zero
        }
    }

    fun centerCameraOn(node: MindMapNode) {
        val effScale = zoom * baseFitScale
        pan = Offset(-node.x * effScale, -node.y * effScale)
    }

    fun deleteNodesWithDescendants(targetIds: Set<String>) {
        if (targetIds.isEmpty()) return
        recordUndo()
        val toRemove = targetIds.toMutableSet()
        var added = true
        while (added) {
            added = false
            mindMapNodes.forEach { n ->
                if (n.parentId in toRemove && toRemove.add(n.id)) added = true
            }
        }
        mindMapNodes.removeAll { it.id in toRemove }
        if (selectedNodeId in toRemove) selectedNodeId = null
        multiSelectedNodeIds = multiSelectedNodeIds - toRemove
    }

    fun addTopicNode(asSubtopic: Boolean, sameLevel: Boolean = false) {
        recordUndo()
        val root = mindMapNodes.firstOrNull { it.parentId == null } ?: mindMapNodes.firstOrNull()
        if (root == null) {
            val central = MindMapNode(
                parentId = null,
                title = "Central Topic",
                iconKey = "",
                level = 0,
                x = 0f,
                y = 0f,
                colorArgb = styleConfig.themePreset.rootStart.toArgb(),
                shape = MindMapNodeShape.PILL,
                side = 0
            )
            mindMapNodes.add(central)
            selectedNodeId = central.id
            multiSelectedNodeIds = setOf(central.id)
            return
        }
        val sel = mindMapNodes.firstOrNull { it.id == selectedNodeId } ?: root
        val parent = when {
            sameLevel && sel.parentId != null -> mindMapNodes.firstOrNull { it.id == sel.parentId } ?: root
            asSubtopic -> sel
            else -> root
        }
        if (parent.isCollapsed) {
            val pIdx = mindMapNodes.indexOfFirst { it.id == parent.id }
            if (pIdx >= 0) mindMapNodes[pIdx] = mindMapNodes[pIdx].copy(isCollapsed = false)
        }
        val newLevel = parent.level + 1
        val palette = styleConfig.themePreset.branchPalette
        val siblingsCount = mindMapNodes.count { it.parentId == parent.id }
        val colorArgb = if (newLevel == 1) {
            palette[siblingsCount % palette.size].toArgb()
        } else {
            parent.colorArgb
        }
        val side = when {
            newLevel == 1 -> if (siblingsCount % 2 == 0) -1 else 1
            parent.side != 0 -> parent.side
            parent.x < 0f -> -1
            else -> 1
        }
        val newLabel = when (newLevel) {
            1 -> "Topic ${siblingsCount + 1}"
            2 -> "Idea ${siblingsCount + 1}"
            else -> "Detail ${siblingsCount + 1}"
        }
        val (nx, ny) = when (styleConfig.layoutType) {
            MindMapLayoutType.HIERARCHICAL, MindMapLayoutType.ORG_CHART -> {
                val offsetIndex = (siblingsCount + 1) / 2
                val sign = if (siblingsCount % 2 == 0) 1f else -1f
                (parent.x + offsetIndex * 165f * sign) to (parent.y + 115f)
            }
            MindMapLayoutType.TIMELINE -> {
                if (newLevel == 1) {
                    val dirY = if (siblingsCount % 2 == 0) -1f else 1f
                    (parent.x + 190f + (siblingsCount / 2) * 160f) to (parent.y + dirY * 110f)
                } else {
                    val dirY = if (parent.y < 0f) -1f else 1f
                    (parent.x + (siblingsCount - 0.5f) * 145f) to (parent.y + dirY * 82f)
                }
            }
            else -> {
                val step = (siblingsCount + 1) / 2
                val sign = if (siblingsCount % 2 == 1) -1f else 1f
                val spreadY = if (siblingsCount == 0) 0f else step * 72f * sign
                (parent.x + side * 195f) to (parent.y + spreadY)
            }
        }
        val newNode = MindMapNode(
            parentId = parent.id,
            title = newLabel,
            iconKey = if (newLevel == 1) "spark" else "",
            level = newLevel,
            x = nx,
            y = ny,
            colorArgb = colorArgb,
            shape = sel.shape,
            side = side
        )
        mindMapNodes.add(newNode)
        selectedNodeId = newNode.id
        multiSelectedNodeIds = setOf(newNode.id)
    }

    fun addParentNodeFor(targetNode: MindMapNode) {
        recordUndo()
        val oldParent = targetNode.parentId?.let { pid -> mindMapNodes.firstOrNull { it.id == pid } }
        val newParentX = if (oldParent != null) (oldParent.x + targetNode.x) / 2f else targetNode.x - 180f
        val newParentY = if (oldParent != null) (oldParent.y + targetNode.y) / 2f else targetNode.y
        val newParentLevel = oldParent?.let { it.level + 1 } ?: 0
        val newParent = MindMapNode(
            parentId = oldParent?.id,
            title = "Parent Topic",
            iconKey = "",
            level = newParentLevel,
            x = newParentX,
            y = newParentY,
            colorArgb = if (newParentLevel == 0) styleConfig.themePreset.rootStart.toArgb() else targetNode.colorArgb,
            shape = targetNode.shape,
            side = targetNode.side
        )
        val idx = mindMapNodes.indexOfFirst { it.id == targetNode.id }
        if (idx >= 0) {
            mindMapNodes[idx] = mindMapNodes[idx].copy(
                parentId = newParent.id,
                level = newParentLevel + 1
            )
        }
        mindMapNodes.add(newParent)
        selectedNodeId = newParent.id
        multiSelectedNodeIds = setOf(newParent.id)
    }

    fun toggleCollapseNode(targetNode: MindMapNode) {
        if (!hasChildNodes(targetNode.id, mindMapNodes)) return
        recordUndo()
        val idx = mindMapNodes.indexOfFirst { it.id == targetNode.id }
        if (idx >= 0) {
            mindMapNodes[idx] = mindMapNodes[idx].copy(isCollapsed = !mindMapNodes[idx].isCollapsed)
        }
    }

    fun detachParentNode(targetNode: MindMapNode) {
        if (targetNode.parentId == null) return
        recordUndo()
        val idx = mindMapNodes.indexOfFirst { it.id == targetNode.id }
        if (idx >= 0) {
            mindMapNodes[idx] = mindMapNodes[idx].copy(parentId = null, level = 0, side = 0)
        }
    }

    fun removeBranchConnectionNear(worldPoint: Offset): Boolean {
        val visible = visibleMindMapNodes(mindMapNodes)
        val byId = visible.associateBy { it.id }
        val hitChild = visible.firstOrNull { child ->
            val parent = child.parentId?.let { byId[it] } ?: return@firstOrNull false
            val midX = (parent.x + child.x) / 2f
            val midY = (parent.y + child.y) / 2f
            hypot(worldPoint.x - midX, worldPoint.y - midY) <= 28f
        } ?: return false
        detachParentNode(hitChild)
        return true
    }

    fun exportAndOptionallyShare(shareNow: Boolean, format: String = "png") {
        coroutineScope.launch {
            val file = withContext(Dispatchers.IO) {
                exportMindMap(context, canvasTitle, mindMapNodes.toList(), styleConfig, format)
            }
            if (file == null) {
                Toast.makeText(context, "Unable to export file", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (shareNow) {
                try {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = when (format.lowercase()) {
                            "pdf" -> "application/pdf"
                            "jpg", "jpeg" -> "image/jpeg"
                            else -> "image/png"
                        }
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, canvasTitle)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Mind Map"))
                } catch (_: Exception) {
                    Toast.makeText(context, "File saved: ${file.name}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Saved: ${file.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Zero-permission Photo Picker for inserting images onto the Mind Map canvas
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val storedPath = withContext(Dispatchers.IO) {
                    storageManager.saveImageAttachmentFromUri(uri).getOrNull()
                }
                val finalPath = storedPath ?: uri.toString()
                recordUndo()
                val item = CanvasItem(
                    type = "IMAGE",
                    start = Offset(-90f, -65f),
                    end = Offset(90f, 65f),
                    imageUri = finalPath,
                    zIndex = elements.size
                )
                elements.add(item)
                selectedItemId = item.id
                selectedNodeId = null
            }
        }
    }

    // Decode any image items off the main thread
    LaunchedEffect(elements.size) {
        elements.filter { it.type == "IMAGE" && it.imageUri.isNotBlank() }.forEach { imgItem ->
            if (!imageBitmapCache.containsKey(imgItem.imageUri)) {
                val bmp = withContext(Dispatchers.IO) {
                    decodeCanvasImageThumbnail(context, storageManager, imgItem.imageUri)
                }
                if (bmp != null) imageBitmapCache[imgItem.imageUri] = bmp
            }
        }
    }

    // Hydrate active board from Room on cold start or board switch
    LaunchedEffect(activeCanvasId) {
        isHydratingBoard = true
        try {
            undoStack.clear()
            redoStack.clear()
            selectedNodeId = null
            selectedItemId = null
            multiSelectedNodeIds = emptySet()
            zoom = 1.0f
            pan = Offset.Zero

            val existingCanvas = viewModel.getCanvasByIdDirect(activeCanvasId)
            val rawEntities = withContext(Dispatchers.IO) {
                viewModel.repository.getElementsForCanvasDirect(activeCanvasId)
            }
            val loadedNodes = rawEntities.mapNotNull { MindMapNode.fromEntity(it) }
            val loadedConfig = rawEntities.firstNotNullOfOrNull { MindMapStyleConfig.fromEntity(it) }
            val loadedItems = rawEntities
                .filter {
                    it.elementType != MindMapNode.ELEMENT_TYPE_MINDMAP_NODE &&
                        it.elementType != MindMapStyleConfig.ELEMENT_TYPE_MINDMAP_CONFIG
                }
                .mapNotNull { parseCanvasItemFromEntity(it) }

            if (existingCanvas != null) {
                isLegacyCanvasNote = false
                val loadedStyle = loadedConfig ?: MindMapStyleConfig()
                val fallback = if (!loadedStyle.showMintWaves) "Untitled" else "Untitled Mind Map"
                canvasTitle = existingCanvas.title.ifBlank { fallback }
                canvasBackgroundHex = existingCanvas.backgroundColorHex
                canvasCreatedAtUtc = existingCanvas.createdAtUtc
                canvasWidth = existingCanvas.width
                canvasHeight = existingCanvas.height
                styleConfig = loadedStyle
                elements.clear()
                elements.addAll(loadedItems)
                mindMapNodes.clear()
                mindMapNodes.addAll(loadedNodes)
                hasUnsavedChanges = false
            } else {
                val legacyNote = viewModel.getNoteByIdDirect(activeCanvasId)
                if (legacyNote != null && legacyNote.noteType == NoteType.CANVAS.name) {
                    isLegacyCanvasNote = true
                    canvasTitle = legacyNote.title.ifBlank { "Untitled Mind Map" }
                    canvasCreatedAtUtc = legacyNote.createdAtUtc
                    styleConfig = loadedConfig ?: MindMapStyleConfig()
                    elements.clear()
                    elements.addAll(if (loadedItems.isNotEmpty()) loadedItems else parseLegacyCanvasNoteStrokes(legacyNote.canvasDataJson))
                    mindMapNodes.clear()
                    mindMapNodes.addAll(loadedNodes)
                    hasUnsavedChanges = false
                } else {
                    isLegacyCanvasNote = false
                    val now = System.currentTimeMillis()
                    canvasTitle = "Untitled Mind Map"
                    canvasBackgroundHex = "#FFFFFF"
                    canvasCreatedAtUtc = now
                    styleConfig = MindMapStyleConfig()
                    elements.clear()
                    mindMapNodes.clear()
                    hasUnsavedChanges = false
                    val blankEntity = CanvasEntity(
                        id = activeCanvasId,
                        title = "Untitled Mind Map",
                        backgroundColorHex = canvasBackgroundHex,
                        width = canvasWidth,
                        height = canvasHeight,
                        createdAtUtc = now,
                        updatedAtUtc = now
                    )
                    viewModel.saveCanvasWithElements(blankEntity, listOf(styleConfig.toEntity(activeCanvasId)))
                }
            }
        } finally {
            isHydratingBoard = false
        }
    }

    // Debounced automatic persistence on any edit, rename, node/branch change, or style update
    LaunchedEffect(editRevision, activeCanvasId) {
        if (!isHydratingBoard && hasUnsavedChanges && editRevision > 0) {
            delay(400L)
            persistCurrentBoard(showErrorToast = false)
        }
    }

    // Auto-save on lifecycle stop / screen exit / board switch
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, activeCanvasId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && hasUnsavedChanges) {
                persistCurrentBoard(showErrorToast = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (hasUnsavedChanges) {
                persistCurrentBoard(showErrorToast = false)
            }
        }
    }

    BackHandler {
        persistCurrentBoard()
        onNavigateBack()
    }

    val visibleNodes = remember(mindMapNodes.toList()) {
        visibleMindMapNodes(mindMapNodes)
    }
    val selectedNode = mindMapNodes.firstOrNull { it.id == selectedNodeId }

    val defaultBoardTitle = if (!styleConfig.showMintWaves) "Untitled" else "Untitled Mind Map"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MindMapTopHeaderBar(
                title = canvasTitle,
                defaultTitle = defaultBoardTitle,
                hasUnsavedChanges = hasUnsavedChanges,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                isDark = isDark,
                onBack = {
                    persistCurrentBoard()
                    onNavigateBack()
                },
                onTitleChange = { updatedTitle ->
                    canvasTitle = updatedTitle
                    hasUnsavedChanges = true
                    editRevision++
                },
                onCommitTitle = { committedTitle ->
                    canvasTitle = committedTitle.trim().ifBlank { defaultBoardTitle }
                    hasUnsavedChanges = true
                    editRevision++
                    persistCurrentBoard(showErrorToast = false)
                },
                onOpenBoardSwitcher = { showBoardSwitcher = true },
                onSaveClick = {
                    persistCurrentBoard()
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                },
                onUndo = { performUndo() },
                onRedo = { performRedo() },
                onShare = { exportAndOptionallyShare(shareNow = true) },
                onExport = { showExportDialog = true }
            )
        },
        bottomBar = {
            MindMapBottomStudioDock(
                activeTab = activeTab,
                isDrawerOpen = isDrawerOpen,
                activeTool = activeTool,
                styleConfig = styleConfig,
                selectedNode = selectedNode,
                isDark = isDark,
                onSelectTab = { tab ->
                    if (tab == activeTab && isDrawerOpen) {
                        isDrawerOpen = false
                    } else {
                        activeTab = tab
                        isDrawerOpen = true
                    }
                },
                onCloseDrawer = { isDrawerOpen = false },
                onSelectTool = { tool ->
                    activeTool = tool
                    if (tool == CanvasToolMode.TEXT || tool == CanvasToolMode.STICKY_NOTE) {
                        pendingTextIsSticky = (tool == CanvasToolMode.STICKY_NOTE)
                    }
                },
                onAddTopic = { addTopicNode(asSubtopic = false) },
                onAddSubtopic = { addTopicNode(asSubtopic = true) },
                onAddSameLevel = { addTopicNode(asSubtopic = false, sameLevel = true) },
                onAutoArrange = {
                    recordUndo()
                    val arranged = applyMindMapLayout(mindMapNodes.toList(), styleConfig.layoutType)
                    mindMapNodes.clear(); mindMapNodes.addAll(arranged)
                    animateZoomTo(1.0f, resetPan = true)
                },
                onSelectLayout = { layout ->
                    recordUndo()
                    styleConfig = styleConfig.copy(layoutType = layout)
                    val arranged = applyMindMapLayout(mindMapNodes.toList(), layout)
                    mindMapNodes.clear(); mindMapNodes.addAll(arranged)
                    animateZoomTo(1.0f, resetPan = true)
                },
                onPickImage = {
                    imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onOpenIconPicker = { showIconPicker = true },
                onOpenMoreSheet = { showExportDialog = true },
                onSelectTheme = { preset ->
                    recordUndo()
                    styleConfig = styleConfig.copy(themePreset = preset)
                    val recolored = applyThemeToNodes(mindMapNodes.toList(), preset)
                    mindMapNodes.clear(); mindMapNodes.addAll(recolored)
                },
                onApplyNodeColor = { argb ->
                    recordUndo()
                    if (argb == null) {
                        val recolored = applyThemeToNodes(mindMapNodes.toList(), MindMapThemePreset.SPECTRUM)
                        styleConfig = styleConfig.copy(themePreset = MindMapThemePreset.SPECTRUM)
                        mindMapNodes.clear(); mindMapNodes.addAll(recolored)
                    } else {
                        activeStrokeColor = Color(argb)
                        val target = selectedNode
                        if (target != null) {
                            val updated = mindMapNodes.map { n ->
                                if (n.id == target.id || n.parentId == target.id) n.copy(colorArgb = argb) else n
                            }
                            mindMapNodes.clear(); mindMapNodes.addAll(updated)
                        } else {
                            styleConfig = styleConfig.copy(overrideLineColorArgb = argb)
                        }
                    }
                },
                onSelectNodeShape = { shape ->
                    recordUndo()
                    val targetId = selectedNodeId
                    val updated = mindMapNodes.map { n ->
                        if (targetId == null || n.id == targetId) n.copy(shape = shape) else n
                    }
                    mindMapNodes.clear(); mindMapNodes.addAll(updated)
                },
                onUpdateStyleConfig = { updated ->
                    styleConfig = updated
                    hasUnsavedChanges = true
                    editRevision++
                },
                onGroupSelected = {
                    val selId = selectedNodeId
                    val targetIds = if (multiSelectedNodeIds.size >= 2) {
                        multiSelectedNodeIds
                    } else if (selId != null) {
                        mindMapNodes.filter { it.id == selId || it.parentId == selId }.map { it.id }.toSet()
                    } else {
                        emptySet()
                    }
                    if (targetIds.isNotEmpty()) {
                        recordUndo()
                        val gid = "grp_${UUID.randomUUID().toString().take(8)}"
                        val updated = mindMapNodes.map { if (it.id in targetIds) it.copy(groupId = gid) else it }
                        mindMapNodes.clear(); mindMapNodes.addAll(updated)
                        Toast.makeText(context, "Grouped ${targetIds.size} nodes", Toast.LENGTH_SHORT).show()
                    }
                },
                onUngroupSelected = {
                    val gid = selectedNode?.groupId
                    recordUndo()
                    val updated = mindMapNodes.map { n ->
                        if (gid == null || n.groupId == gid) n.copy(groupId = null) else n
                    }
                    mindMapNodes.clear(); mindMapNodes.addAll(updated)
                    Toast.makeText(context, "Ungrouped", Toast.LENGTH_SHORT).show()
                },
                onLockSelected = {
                    val selId = selectedNodeId ?: return@MindMapBottomStudioDock
                    recordUndo()
                    val updated = mindMapNodes.map { if (it.id == selId) it.copy(isLocked = true) else it }
                    mindMapNodes.clear(); mindMapNodes.addAll(updated)
                },
                onUnlockSelected = {
                    recordUndo()
                    val selId = selectedNodeId
                    val updated = mindMapNodes.map { n ->
                        if (selId == null || n.id == selId) n.copy(isLocked = false) else n
                    }
                    mindMapNodes.clear(); mindMapNodes.addAll(updated)
                },
                onOpenLayers = { showLayersSheet = true },
                onDuplicateSelected = {
                    val targets = if (multiSelectedNodeIds.isNotEmpty()) {
                        mindMapNodes.filter { it.id in multiSelectedNodeIds }
                    } else {
                        listOfNotNull(selectedNode)
                    }
                    if (targets.isNotEmpty()) {
                        recordUndo()
                        val newIds = mutableSetOf<String>()
                        targets.forEach { sel ->
                            val clone = sel.copy(
                                id = UUID.randomUUID().toString(),
                                title = "${sel.title} Copy",
                                x = sel.x + 28f,
                                y = sel.y + 44f,
                                isLocked = false
                            )
                            mindMapNodes.add(clone)
                            newIds.add(clone.id)
                        }
                        multiSelectedNodeIds = newIds
                        selectedNodeId = newIds.firstOrNull()
                    } else {
                        val item = elements.firstOrNull { it.id == selectedItemId }
                        if (item != null) {
                            recordUndo()
                            val clone = item.translatedBy(24f, 24f).copy(id = UUID.randomUUID().toString())
                            elements.add(clone)
                            selectedItemId = clone.id
                        }
                    }
                },
                onDeleteSelected = {
                    val targetIds = when {
                        multiSelectedNodeIds.isNotEmpty() -> multiSelectedNodeIds
                        selectedNodeId != null -> setOf(selectedNodeId!!)
                        else -> emptySet()
                    }
                    if (targetIds.isNotEmpty()) {
                        deleteNodesWithDescendants(targetIds)
                    } else if (selectedItemId != null) {
                        recordUndo()
                        elements.removeAll { it.id == selectedItemId }
                        selectedItemId = null
                    }
                },
                onOpenFind = { showFindDialog = true },
                onOpenSettings = { showSettingsDialog = true },
                onLoadAiSample = {
                    recordUndo()
                    canvasTitle = "AI in Education"
                    mindMapNodes.clear()
                    mindMapNodes.addAll(createAiInEducationMindMap(styleConfig.themePreset))
                    animateZoomTo(1.0f, resetPan = true)
                },
                onResetStarter = {
                    recordUndo()
                    mindMapNodes.clear()
                    mindMapNodes.addAll(createStarterMindMap(styleConfig.themePreset))
                    animateZoomTo(1.0f, resetPan = true)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .onSizeChanged { viewportSize = it }
        ) {
            val surfaceCol = MaterialTheme.colorScheme.surface
            val backgroundCol = MaterialTheme.colorScheme.background
            val lassoAccent = if (isDark) Color(0xFF34D399) else HeaderEmerald

            // Interactive Mind Map + Drawing Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("mindmap_canvas")
                    .pointerInput(activeTool, viewportSize, baseFitScale) {
                        detectTapGestures(
                            onTap = { tapOffset ->
                                val world = screenToWorld(tapOffset)
                                searchHighlightNodeId = null
                                when (activeTool) {
                                    CanvasToolMode.TEXT -> {
                                        pendingTextWorldPos = world
                                        pendingTextIsSticky = false
                                    }
                                    CanvasToolMode.STICKY_NOTE -> {
                                        pendingTextWorldPos = world
                                        pendingTextIsSticky = true
                                    }
                                    CanvasToolMode.ERASER -> {
                                        val hitItem = elements.lastOrNull { it.containsPoint(world, 20f) }
                                        if (hitItem != null) {
                                            recordUndo()
                                            elements.remove(hitItem)
                                        } else {
                                            removeBranchConnectionNear(world)
                                        }
                                    }
                                    else -> {
                                        val hitNode = visibleNodes.lastOrNull { it.containsPoint(world) }
                                        if (hitNode != null) {
                                            if (hitNode.isCollapsed && world.x >= hitNode.bounds().right - 16f) {
                                                toggleCollapseNode(hitNode)
                                            }
                                            selectedNodeId = hitNode.id
                                            selectedItemId = null
                                            multiSelectedNodeIds = setOf(hitNode.id)
                                        } else {
                                            val hitItem = elements.lastOrNull { it.containsPoint(world) }
                                            selectedItemId = hitItem?.id
                                            selectedNodeId = null
                                            multiSelectedNodeIds = emptySet()
                                        }
                                    }
                                }
                            },
                            onDoubleTap = { tapOffset ->
                                val world = screenToWorld(tapOffset)
                                val hitNode = visibleNodes.lastOrNull { it.containsPoint(world) }
                                if (hitNode != null) {
                                    selectedNodeId = hitNode.id
                                    editingNode = hitNode
                                } else if (activeTool == CanvasToolMode.SELECT) {
                                    recordUndo()
                                    val root = mindMapNodes.firstOrNull { it.parentId == null }
                                    val parent = selectedNode ?: root
                                    val isRoot = parent == null
                                    val newLevel = if (isRoot) 0 else parent!!.level + 1
                                    val palette = styleConfig.themePreset.branchPalette
                                    val siblingsCount = if (isRoot) 0 else mindMapNodes.count { it.parentId == parent!!.id }
                                    val colorArgb = when {
                                        isRoot -> styleConfig.themePreset.rootStart.toArgb()
                                        newLevel == 1 -> palette[siblingsCount % palette.size].toArgb()
                                        else -> parent!!.colorArgb
                                    }
                                    val side = if (isRoot) 0 else if (world.x < parent!!.x) -1 else 1
                                    val newNode = MindMapNode(
                                        parentId = parent?.id,
                                        title = if (isRoot) "Central Topic" else "Topic ${siblingsCount + 1}",
                                        iconKey = "",
                                        level = newLevel,
                                        x = world.x,
                                        y = world.y,
                                        colorArgb = colorArgb,
                                        shape = parent?.shape ?: MindMapNodeShape.PILL,
                                        side = side
                                    )
                                    mindMapNodes.add(newNode)
                                    selectedNodeId = newNode.id
                                    multiSelectedNodeIds = setOf(newNode.id)
                                }
                            }
                        )
                    }
                    .pointerInput(activeTool, baseFitScale) {
                        if (activeTool == CanvasToolMode.PAN_ZOOM) {
                            detectTransformGestures { _, panChange, zoomChange, _ ->
                                zoom = (zoom * zoomChange).coerceIn(0.4f, 3.0f)
                                pan += panChange
                            }
                        } else {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    val world = screenToWorld(startOffset)
                                    dragStartWorld = world
                                    dragCurrentWorld = world
                                    hasPushedDragUndo = false
                                    when (activeTool) {
                                        CanvasToolMode.SELECT -> {
                                            val hitNode = visibleNodes.lastOrNull { it.containsPoint(world) }
                                            if (hitNode != null) {
                                                selectedNodeId = hitNode.id
                                                selectedItemId = null
                                                if (hitNode.id !in multiSelectedNodeIds) {
                                                    multiSelectedNodeIds = setOf(hitNode.id)
                                                }
                                                draggingNodeId = if (!hitNode.isLocked) hitNode.id else null
                                                draggingItemId = null
                                            } else {
                                                val hitItem = elements.lastOrNull { it.containsPoint(world) }
                                                if (hitItem != null) {
                                                    selectedItemId = hitItem.id
                                                    selectedNodeId = null
                                                    draggingItemId = hitItem.id
                                                    draggingNodeId = null
                                                } else {
                                                    draggingNodeId = null
                                                    draggingItemId = null
                                                }
                                            }
                                        }
                                        CanvasToolMode.PEN, CanvasToolMode.HIGHLIGHTER -> {
                                            currentStrokePoints = listOf(world)
                                        }
                                        else -> Unit
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val worldPos = screenToWorld(change.position)
                                    dragCurrentWorld = worldPos
                                    val effScale = (zoom * baseFitScale).coerceAtLeast(0.1f)
                                    val worldDx = dragAmount.x / effScale
                                    val worldDy = dragAmount.y / effScale

                                    when (activeTool) {
                                        CanvasToolMode.SELECT -> {
                                            val dNodeId = draggingNodeId
                                            val dItemId = draggingItemId
                                            if (dNodeId != null) {
                                                if (!hasPushedDragUndo) {
                                                    recordUndo()
                                                    hasPushedDragUndo = true
                                                }
                                                val targetGroup = mindMapNodes.firstOrNull { it.id == dNodeId }?.groupId
                                                val movingMulti = dNodeId in multiSelectedNodeIds && multiSelectedNodeIds.size > 1
                                                for (i in mindMapNodes.indices) {
                                                    val n = mindMapNodes[i]
                                                    val shouldMove = !n.isLocked && (
                                                        n.id == dNodeId ||
                                                            (targetGroup != null && n.groupId == targetGroup) ||
                                                            (movingMulti && n.id in multiSelectedNodeIds)
                                                        )
                                                    if (shouldMove) {
                                                        mindMapNodes[i] = n.copy(x = n.x + worldDx, y = n.y + worldDy)
                                                    }
                                                }
                                            } else if (dItemId != null) {
                                                if (!hasPushedDragUndo) {
                                                    recordUndo()
                                                    hasPushedDragUndo = true
                                                }
                                                val idx = elements.indexOfFirst { it.id == dItemId }
                                                if (idx >= 0) {
                                                    elements[idx] = elements[idx].translatedBy(worldDx, worldDy)
                                                }
                                            } else {
                                                // Smoothly pan when dragging empty canvas background
                                                pan += dragAmount
                                            }
                                        }
                                        CanvasToolMode.PEN, CanvasToolMode.HIGHLIGHTER -> {
                                            currentStrokePoints = currentStrokePoints + worldPos
                                        }
                                        CanvasToolMode.ERASER -> {
                                            val hits = elements.filter { it.containsPoint(worldPos, 20f) }
                                            if (hits.isNotEmpty()) {
                                                if (!hasPushedDragUndo) {
                                                    recordUndo()
                                                    hasPushedDragUndo = true
                                                }
                                                elements.removeAll(hits)
                                            }
                                        }
                                        else -> Unit
                                    }
                                },
                                onDragEnd = {
                                    val start = dragStartWorld
                                    val end = dragCurrentWorld
                                    if (hasPushedDragUndo) {
                                        editRevision++
                                    }
                                    when (activeTool) {
                                        CanvasToolMode.PEN, CanvasToolMode.HIGHLIGHTER -> {
                                            if (currentStrokePoints.isNotEmpty()) {
                                                recordUndo()
                                                val isHl = activeTool == CanvasToolMode.HIGHLIGHTER
                                                elements.add(
                                                    CanvasItem(
                                                        type = if (isHl) "HIGHLIGHTER" else "STROKE",
                                                        points = currentStrokePoints,
                                                        colorArgb = if (isHl) Color(0x66FACC15).toArgb() else activeStrokeColor.toArgb(),
                                                        strokeWidth = if (isHl) 16f else 4.5f,
                                                        zIndex = elements.size
                                                    )
                                                )
                                            }
                                        }
                                        CanvasToolMode.SHAPE_RECT,
                                        CanvasToolMode.SHAPE_CIRCLE,
                                        CanvasToolMode.SHAPE_LINE,
                                        CanvasToolMode.SHAPE_ARROW,
                                        CanvasToolMode.SHAPE_CONNECTOR -> {
                                            if (start != null && end != null) {
                                                recordUndo()
                                                val typeName = when (activeTool) {
                                                    CanvasToolMode.SHAPE_RECT -> "RECTANGLE"
                                                    CanvasToolMode.SHAPE_CIRCLE -> "CIRCLE"
                                                    CanvasToolMode.SHAPE_LINE -> "LINE"
                                                    CanvasToolMode.SHAPE_ARROW -> "ARROW"
                                                    else -> "CONNECTOR"
                                                }
                                                val snappedStart = if (typeName == "ARROW" || typeName == "CONNECTOR") {
                                                    visibleNodes.lastOrNull { it.containsPoint(start) }?.let { Offset(it.x, it.y) } ?: start
                                                } else {
                                                    start
                                                }
                                                val snappedEnd = if (typeName == "ARROW" || typeName == "CONNECTOR") {
                                                    visibleNodes.lastOrNull { it.containsPoint(end) }?.let { Offset(it.x, it.y) } ?: end
                                                } else {
                                                    end
                                                }
                                                elements.add(
                                                    CanvasItem(
                                                        type = typeName,
                                                        start = snappedStart,
                                                        end = snappedEnd,
                                                        colorArgb = activeStrokeColor.toArgb(),
                                                        strokeWidth = styleConfig.lineWidthPx,
                                                        zIndex = elements.size
                                                    )
                                                )
                                            }
                                        }
                                        CanvasToolMode.LASSO -> {
                                            if (start != null && end != null) {
                                                val rect = Rect(
                                                    min(start.x, end.x),
                                                    min(start.y, end.y),
                                                    max(start.x, end.x),
                                                    max(start.y, end.y)
                                                )
                                                val inside = visibleNodes.filter { rect.contains(Offset(it.x, it.y)) }.map { it.id }.toSet()
                                                multiSelectedNodeIds = inside
                                                selectedNodeId = inside.firstOrNull()
                                            }
                                        }
                                        else -> Unit
                                    }
                                    currentStrokePoints = emptyList()
                                    dragStartWorld = null
                                    dragCurrentWorld = null
                                    draggingNodeId = null
                                    draggingItemId = null
                                    hasPushedDragUndo = false
                                }
                            )
                        }
                    }
            ) {
                // 1. Studio backdrop synchronized with active app theme
                drawMindMapBackdrop(
                    theme = styleConfig.themePreset,
                    isDark = isDark,
                    showMintWaves = styleConfig.showMintWaves,
                    surfaceColor = surfaceCol,
                    backgroundColor = backgroundCol
                )

                val effScale = zoom * baseFitScale
                val originX = size.width / 2f + pan.x
                val originY = size.height / 2f + pan.y

                // 2. World-transformed content
                translate(left = originX, top = originY) {
                    scale(scale = effScale, pivot = Offset.Zero) {
                        if (styleConfig.showGrid) {
                            drawInfiniteGrid(
                                pan = Offset(originX, originY),
                                zoom = effScale,
                                viewportSize = size,
                                isDark = isDark
                            )
                        }

                        // Branches first, then visible nodes on top
                        drawMindMapBranches(
                            nodes = visibleNodes,
                            config = styleConfig,
                            isDark = isDark
                        )
                        visibleNodes.forEach { node ->
                            val hiddenCount = if (node.isCollapsed) {
                                hiddenDescendantCount(node.id, mindMapNodes)
                            } else {
                                0
                            }
                            drawMindMapNode(
                                node = node,
                                config = styleConfig,
                                isSelected = node.id == selectedNodeId || node.id in multiSelectedNodeIds,
                                isSearchMatch = node.id == searchHighlightNodeId,
                                isDark = isDark,
                                collapsedChildCount = hiddenCount
                            )
                        }

                        // Freehand strokes, shapes, sticky notes, images
                        elements.forEach { item ->
                            drawCanvasItem(
                                item = item,
                                isSelected = item.id == selectedItemId,
                                cachedImage = if (item.type == "IMAGE") imageBitmapCache[item.imageUri] else null,
                                isDark = isDark
                            )
                        }

                        // Active live stroke preview
                        if (currentStrokePoints.size > 1) {
                            val isHl = activeTool == CanvasToolMode.HIGHLIGHTER
                            drawCanvasItem(
                                item = CanvasItem(
                                    type = if (isHl) "HIGHLIGHTER" else "STROKE",
                                    points = currentStrokePoints,
                                    colorArgb = if (isHl) Color(0x66FACC15).toArgb() else activeStrokeColor.toArgb(),
                                    strokeWidth = if (isHl) 16f else 4.5f
                                ),
                                isSelected = false,
                                isDark = isDark
                            )
                        }

                        // Active live shape or lasso preview
                        val dStart = dragStartWorld
                        val dEnd = dragCurrentWorld
                        if (dStart != null && dEnd != null) {
                            if (activeTool == CanvasToolMode.LASSO) {
                                drawRect(
                                    color = lassoAccent.copy(alpha = 0.12f),
                                    topLeft = Offset(min(dStart.x, dEnd.x), min(dStart.y, dEnd.y)),
                                    size = Size(kotlin.math.abs(dEnd.x - dStart.x), kotlin.math.abs(dEnd.y - dStart.y))
                                )
                                drawRect(
                                    color = lassoAccent,
                                    topLeft = Offset(min(dStart.x, dEnd.x), min(dStart.y, dEnd.y)),
                                    size = Size(kotlin.math.abs(dEnd.x - dStart.x), kotlin.math.abs(dEnd.y - dStart.y)),
                                    style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                                )
                            } else if (activeTool in setOf(
                                    CanvasToolMode.SHAPE_RECT,
                                    CanvasToolMode.SHAPE_CIRCLE,
                                    CanvasToolMode.SHAPE_LINE,
                                    CanvasToolMode.SHAPE_ARROW,
                                    CanvasToolMode.SHAPE_CONNECTOR
                                )
                            ) {
                                val previewType = when (activeTool) {
                                    CanvasToolMode.SHAPE_RECT -> "RECTANGLE"
                                    CanvasToolMode.SHAPE_CIRCLE -> "CIRCLE"
                                    CanvasToolMode.SHAPE_LINE -> "LINE"
                                    CanvasToolMode.SHAPE_ARROW -> "ARROW"
                                    else -> "CONNECTOR"
                                }
                                drawCanvasItem(
                                    item = CanvasItem(
                                        type = previewType,
                                        start = dStart,
                                        end = dEnd,
                                        colorArgb = activeStrokeColor.toArgb(),
                                        strokeWidth = styleConfig.lineWidthPx
                                    ),
                                    isSelected = false,
                                    isDark = isDark
                                )
                            }
                        }
                    }
                }
            }

            // Floating Bottom-Left Circular Layers Button
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, bottom = 14.dp)
                    .size(44.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .testTag("canvas_floating_layers_btn")
                    .clickable { showLayersSheet = true },
                shape = CircleShape,
                color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Layers",
                        tint = HeaderEmerald,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            // Floating Right Vertical Zoom Capsule (Fit, +, 100%, -)
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .width(42.dp)
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(21.dp)),
                shape = RoundedCornerShape(21.dp),
                color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .testTag("canvas_zoom_reset_btn")
                            .clickable { animateZoomTo(1.0f, resetPan = true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusWeak,
                            contentDescription = "Reset zoom and fit",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), thickness = 0.5.dp)
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .testTag("canvas_zoom_in_btn")
                            .clickable { animateZoomTo(zoom + 0.25f) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Zoom in",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "${(zoom * 100).roundToInt()}%",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .testTag("canvas_zoom_out_btn")
                            .clickable { animateZoomTo(zoom - 0.25f) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Zoom out",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    // Dialogs & Sheets
    editingNode?.let { nodeToEdit ->
        MindMapNodeEditDialog(
            node = nodeToEdit,
            hasChildren = hasChildNodes(nodeToEdit.id, mindMapNodes),
            onDismiss = { editingNode = null },
            onSave = { newTitle, newIcon, newLocked ->
                recordUndo()
                val updated = mindMapNodes.map { n ->
                    if (n.id == nodeToEdit.id) n.copy(title = newTitle, iconKey = newIcon, isLocked = newLocked) else n
                }
                mindMapNodes.clear(); mindMapNodes.addAll(updated)
                editingNode = null
            },
            onAddChild = {
                selectedNodeId = nodeToEdit.id
                addTopicNode(asSubtopic = true)
            },
            onAddSibling = {
                selectedNodeId = nodeToEdit.id
                addTopicNode(asSubtopic = false, sameLevel = true)
            },
            onAddParent = {
                addParentNodeFor(nodeToEdit)
                editingNode = null
            },
            onToggleCollapse = {
                toggleCollapseNode(nodeToEdit)
                editingNode = null
            },
            onDetachParent = {
                detachParentNode(nodeToEdit)
                editingNode = null
            },
            onDelete = {
                deleteNodesWithDescendants(setOf(nodeToEdit.id))
                editingNode = null
            }
        )
    }

    if (showIconPicker) {
        val target = selectedNode ?: mindMapNodes.firstOrNull()
        MindMapIconPickerDialog(
            currentIconKey = target?.iconKey.orEmpty(),
            onDismiss = { showIconPicker = false },
            onPickIcon = { chosenKey ->
                if (target != null) {
                    recordUndo()
                    val updated = mindMapNodes.map { if (it.id == target.id) it.copy(iconKey = chosenKey) else it }
                    mindMapNodes.clear(); mindMapNodes.addAll(updated)
                }
                showIconPicker = false
            }
        )
    }

    if (showBoardSwitcher) {
        MindMapBoardSwitcherSheet(
            currentCanvasId = activeCanvasId,
            canvases = allCanvases,
            onDismiss = { showBoardSwitcher = false },
            onSelectCanvas = { chosen ->
                persistCurrentBoard(showErrorToast = false)
                activeCanvasId = chosen.id
            },
            onCreateNewBoard = { newTitle ->
                persistCurrentBoard(showErrorToast = false)
                val newId = UUID.randomUUID().toString()
                val newConfig = MindMapStyleConfig()
                val now = System.currentTimeMillis()
                val entity = CanvasEntity(
                    id = newId,
                    title = newTitle.trim().ifBlank { "Untitled Mind Map" },
                    createdAtUtc = now,
                    updatedAtUtc = now
                )
                viewModel.saveCanvasWithElements(entity, listOf(newConfig.toEntity(newId)))
                activeCanvasId = newId
            },
            onDeleteCanvas = { target ->
                viewModel.deleteCanvas(target.id)
            }
        )
    }

    if (showLayersSheet) {
        MindMapLayersSheet(
            nodes = mindMapNodes.toList(),
            canvasItems = elements.toList(),
            selectedNodeId = selectedNodeId,
            selectedItemId = selectedItemId,
            onDismiss = { showLayersSheet = false },
            onSelectNode = { node ->
                selectedNodeId = node.id
                selectedItemId = null
                centerCameraOn(node)
            },
            onToggleNodeLock = { node ->
                recordUndo()
                val updated = mindMapNodes.map { if (it.id == node.id) it.copy(isLocked = !it.isLocked) else it }
                mindMapNodes.clear(); mindMapNodes.addAll(updated)
            },
            onDeleteNode = { node ->
                deleteNodesWithDescendants(setOf(node.id))
            },
            onSelectCanvasItem = { item ->
                selectedItemId = item.id
                selectedNodeId = null
            },
            onDeleteCanvasItem = { item ->
                recordUndo()
                elements.removeAll { it.id == item.id }
                if (selectedItemId == item.id) selectedItemId = null
            }
        )
    }

    if (showFindDialog) {
        MindMapFindDialog(
            nodes = mindMapNodes.toList(),
            onDismiss = { showFindDialog = false },
            onSelectMatch = { matched ->
                selectedNodeId = matched.id
                searchHighlightNodeId = matched.id
                centerCameraOn(matched)
            }
        )
    }

    if (showSettingsDialog) {
        MindMapSettingsDialog(
            styleConfig = styleConfig,
            onDismiss = { showSettingsDialog = false },
            onUpdateStyleConfig = {
                styleConfig = it
                hasUnsavedChanges = true
            },
            onResetStarterMap = {
                recordUndo()
                mindMapNodes.clear()
                mindMapNodes.addAll(createStarterMindMap(styleConfig.themePreset))
                animateZoomTo(1.0f, resetPan = true)
            },
            onClearFreehand = {
                recordUndo()
                elements.clear()
                selectedItemId = null
            }
        )
    }

    if (showExportDialog) {
        MindMapExportDialog(
            title = canvasTitle,
            onDismiss = { showExportDialog = false },
            onSaveNow = {
                persistCurrentBoard()
                Toast.makeText(context, "Board saved", Toast.LENGTH_SHORT).show()
            },
            onExport = { shareNow, format -> exportAndOptionallyShare(shareNow, format) }
        )
    }

    pendingTextWorldPos?.let { pos ->
        MindMapTextOrStickyDialog(
            isSticky = pendingTextIsSticky,
            onDismiss = { pendingTextWorldPos = null },
            onConfirm = { enteredText ->
                recordUndo()
                if (pendingTextIsSticky) {
                    elements.add(
                        CanvasItem(
                            type = "STICKY",
                            start = Offset(pos.x - 65f, pos.y - 45f),
                            end = Offset(pos.x + 65f, pos.y + 45f),
                            text = enteredText,
                            fillColorArgb = Color(0xFFFEF3C7).toArgb(),
                            colorArgb = Color(0xFFF59E0B).toArgb(),
                            zIndex = elements.size
                        )
                    )
                } else {
                    elements.add(
                        CanvasItem(
                            type = "TEXT",
                            start = pos,
                            text = enteredText,
                            colorArgb = activeStrokeColor.toArgb(),
                            zIndex = elements.size
                        )
                    )
                }
                pendingTextWorldPos = null
                activeTool = CanvasToolMode.SELECT
            }
        )
    }
}

@Composable
private fun MindMapTopHeaderBar(
    title: String,
    defaultTitle: String,
    hasUnsavedChanges: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    isDark: Boolean,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onCommitTitle: (String) -> Unit,
    onOpenBoardSwitcher: () -> Unit,
    onSaveClick: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit
) {
    var isEditingTitle by remember { mutableStateOf(false) }
    var hasEnteredTitleFocus by remember { mutableStateOf(false) }
    val titleFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isEditingTitle) {
        if (isEditingTitle) {
            hasEnteredTitleFocus = false
            titleFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDark) MaterialTheme.colorScheme.surfaceContainerLowest else Color(0xFFEDFBF3))
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(26.dp)),
            shape = RoundedCornerShape(26.dp),
            color = if (isDark) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (isEditingTitle) {
                            isEditingTitle = false
                            focusManager.clearFocus()
                        }
                        onBack()
                    },
                    modifier = Modifier.size(36.dp).testTag("canvas_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Inline editable title + board switcher chevron
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(10.dp))
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditingTitle) {
                        BasicTextField(
                            value = title,
                            onValueChange = onTitleChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(HeaderEmerald),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    isEditingTitle = false
                                    focusManager.clearFocus()
                                    onCommitTitle(title)
                                }
                            ),
                            modifier = Modifier
                                .widthIn(min = 72.dp, max = 128.dp)
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .focusRequester(titleFocusRequester)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        hasEnteredTitleFocus = true
                                    } else if (hasEnteredTitleFocus && isEditingTitle) {
                                        isEditingTitle = false
                                        onCommitTitle(title)
                                    }
                                }
                                .testTag("canvas_title_input"),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (title.isEmpty()) {
                                        Text(
                                            text = defaultTitle,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .testTag("canvas_title_btn")
                                .clickable { isEditingTitle = true }
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = title.ifBlank { defaultTitle },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 112.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .testTag("canvas_switcher_pill")
                            .clickable(onClick = onOpenBoardSwitcher),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Switch board",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.width(2.dp))

                // Cloud Saved status pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .testTag("canvas_save_btn")
                        .clickable(onClick = onSaveClick)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (hasUnsavedChanges) Icons.Default.CloudUpload else Icons.Default.CloudDone,
                        contentDescription = if (hasUnsavedChanges) "Save board" else "Saved",
                        tint = HeaderEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = if (hasUnsavedChanges) "Save" else "Saved",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Spacer(Modifier.weight(1f))

                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.size(32.dp).testTag("canvas_undo_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(17.dp)
                    )
                }
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.size(32.dp).testTag("canvas_redo_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(17.dp)
                    )
                }
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(32.dp).testTag("canvas_share_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Mind Map",
                        tint = HeaderEmerald,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(Modifier.width(3.dp))

                // Glossy Emerald Export Pill Button
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF22C55E), HeaderEmerald, HeaderEmeraldDeep)
                            )
                        )
                        .testTag("canvas_export_btn")
                        .clickable(onClick = onExport)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Export",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
