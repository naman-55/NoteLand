package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Rectangle
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnnotationEntity
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.SmartNoteUtils
import java.io.File
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import org.json.JSONArray
import org.json.JSONObject

enum class AnnotatorTool {
    PAN_ZOOM,
    PEN,
    HIGHLIGHTER,
    RECTANGLE,
    CIRCLE,
    ARROW,
    TEXT,
    ERASER
}

data class ImageAnnotationItem(
    val id: String = UUID.randomUUID().toString(),
    val type: String, // DRAWING, HIGHLIGHT, RECTANGLE, CIRCLE, ARROW, TEXT_NOTE
    val points: List<Offset> = emptyList(),
    val start: Offset = Offset.Zero,
    val end: Offset = Offset.Zero,
    val text: String = "",
    val colorArgb: Int = -65536,
    val strokeWidth: Float = 5f
) {
    fun toEntity(attachmentId: String, pageNumber: Int = 1): AnnotationEntity {
        val obj = JSONObject()
        obj.put("type", type)
        obj.put("color", colorArgb)
        obj.put("width", strokeWidth.toDouble())
        obj.put("text", text)
        obj.put("startX", start.x.toDouble())
        obj.put("startY", start.y.toDouble())
        obj.put("endX", end.x.toDouble())
        obj.put("endY", end.y.toDouble())

        val ptsArray = JSONArray()
        for (p in points) {
            val pObj = JSONObject()
            pObj.put("x", p.x.toDouble())
            pObj.put("y", p.y.toDouble())
            ptsArray.put(pObj)
        }
        obj.put("points", ptsArray)

        return AnnotationEntity(
            id = id,
            attachmentId = attachmentId,
            annotationType = type,
            pageNumber = pageNumber,
            coordinatesJson = obj.toString(),
            content = text,
            colorHex = String.format(java.util.Locale.US, "#%06X", (0xFFFFFF and colorArgb)),
            createdAtUtc = System.currentTimeMillis()
        )
    }

    fun containsPoint(point: Offset, tolerance: Float = 20f): Boolean {
        return when (type) {
            "DRAWING", "HIGHLIGHT" -> {
                for (i in 0 until points.size - 1) {
                    if (distToSegment(point, points[i], points[i + 1]) <= (strokeWidth / 2 + tolerance)) return true
                }
                false
            }
            "RECTANGLE" -> {
                val left = min(start.x, end.x) - tolerance
                val right = max(start.x, end.x) + tolerance
                val top = min(start.y, end.y) - tolerance
                val bottom = max(start.y, end.y) + tolerance
                point.x in left..right && point.y in top..bottom
            }
            "CIRCLE" -> {
                val center = Offset((start.x + end.x) / 2, (start.y + end.y) / 2)
                val radius = hypot(end.x - start.x, end.y - start.y) / 2 + tolerance
                hypot(point.x - center.x, point.y - center.y) <= radius
            }
            "ARROW" -> distToSegment(point, start, end) <= (strokeWidth / 2 + tolerance)
            "TEXT_NOTE" -> {
                val textPaint = android.graphics.Paint().apply {
                    textSize = 22f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                val textWidth = max(textPaint.measureText(text), 60f)
                val bounds = Rect(start.x - 10f, start.y - 30f, start.x + textWidth + 10f, start.y + 10f)
                bounds.contains(point)
            }
            else -> false
        }
    }

    private fun distToSegment(p: Offset, v: Offset, w: Offset): Float {
        val l2 = (v.x - w.x) * (v.x - w.x) + (v.y - w.y) * (v.y - w.y)
        if (l2 == 0f) return hypot(p.x - v.x, p.y - v.y)
        val t = max(0f, min(1f, ((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2))
        val proj = Offset(v.x + t * (w.x - v.x), v.y + t * (w.y - v.y))
        return hypot(p.x - proj.x, p.y - proj.y)
    }
}

fun parseAnnotationItem(entity: AnnotationEntity): ImageAnnotationItem? {
    return try {
        val obj = JSONObject(entity.coordinatesJson)
        val type = obj.optString("type", entity.annotationType)
        val color = obj.optInt("color", -65536)
        val width = obj.optDouble("width", 5.0).toFloat()
        val text = obj.optString("text", entity.content)
        val startX = obj.optDouble("startX", 0.0).toFloat()
        val startY = obj.optDouble("startY", 0.0).toFloat()
        val endX = obj.optDouble("endX", 0.0).toFloat()
        val endY = obj.optDouble("endY", 0.0).toFloat()

        val points = mutableListOf<Offset>()
        if (obj.has("points")) {
            val arr = obj.getJSONArray("points")
            for (i in 0 until arr.length()) {
                val p = arr.getJSONObject(i)
                points.add(Offset(p.getDouble("x").toFloat(), p.getDouble("y").toFloat()))
            }
        }

        ImageAnnotationItem(
            id = entity.id,
            type = type,
            points = points,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            text = text,
            colorArgb = color,
            strokeWidth = width
        )
    } catch (e: Exception) {
        null
    }
}

private data class DecodedAnnotatorImage(
    val bitmap: Bitmap,
    val logicalWidth: Int,
    val logicalHeight: Int
)

private fun decodeSampledOrientedBitmap(file: File, maxDimension: Int = 2048): DecodedAnnotatorImage? {
    if (!file.exists()) return null
    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
    val rawW = boundsOpts.outWidth
    val rawH = boundsOpts.outHeight
    if (rawW <= 0 || rawH <= 0) return null

    var sampleSize = 1
    while ((rawW / sampleSize) > maxDimension || (rawH / sampleSize) > maxDimension) {
        sampleSize *= 2
    }

    val decodeOpts = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val sampled = BitmapFactory.decodeFile(file.absolutePath, decodeOpts) ?: return null

    val orientation = try {
        ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } catch (_: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    val matrix = Matrix()
    var swappedDimensions = false
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> {
            matrix.postRotate(90f)
            swappedDimensions = true
        }
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> {
            matrix.postRotate(270f)
            swappedDimensions = true
        }
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.preScale(-1f, 1f)
            matrix.postRotate(270f)
            swappedDimensions = true
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.preScale(-1f, 1f)
            matrix.postRotate(90f)
            swappedDimensions = true
        }
    }

    val orientedBitmap = if (!matrix.isIdentity) {
        try {
            Bitmap.createBitmap(sampled, 0, 0, sampled.width, sampled.height, matrix, true).also {
                if (it !== sampled) sampled.recycle()
            }
        } catch (_: Exception) {
            sampled
        }
    } else {
        sampled
    }

    val logicalW = if (swappedDimensions) rawH else rawW
    val logicalH = if (swappedDimensions) rawW else rawH
    return DecodedAnnotatorImage(orientedBitmap, logicalW, logicalH)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageAnnotatorScreen(
    attachmentId: String,
    filePath: String,
    fileName: String,
    viewModel: NoteViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()

    // Load original bitmap asynchronously with vault decryption, bounded memory & EXIF orientation
    var decodedImage by remember(attachmentId, filePath) { mutableStateOf<DecodedAnnotatorImage?>(null) }
    var imageFile by remember(attachmentId, filePath) { mutableStateOf<File?>(null) }
    var isTempDecryptedImage by remember(attachmentId, filePath) { mutableStateOf(false) }
    var isLoadingImage by remember(attachmentId, filePath) { mutableStateOf(true) }
    var imageUnavailableReason by remember(attachmentId, filePath) { mutableStateOf<String?>(null) }
    var isNavigatingBack by remember { mutableStateOf(false) }

    LaunchedEffect(attachmentId, filePath, isVaultUnlocked) {
        isLoadingImage = true
        imageUnavailableReason = null
        val resolution = viewModel.resolveAttachmentForViewer(
            attachmentId = attachmentId,
            rawFilePath = filePath,
            defaultExtension = "jpg"
        )
        if (resolution.isLockedOut) {
            decodedImage = null
            imageFile = null
            isTempDecryptedImage = false
            imageUnavailableReason = "This image is in your Vault. Unlock the Vault to view it."
            isLoadingImage = false
            return@LaunchedEffect
        }
        val targetFile = resolution.file
        val decoded = if (targetFile != null && targetFile.exists()) {
            withContext(Dispatchers.IO) {
                runCatching { decodeSampledOrientedBitmap(targetFile) }.getOrNull()
            }
        } else null
        imageFile = targetFile
        isTempDecryptedImage = resolution.isTempDecrypted
        decodedImage = decoded
        if (decoded == null) {
            imageUnavailableReason = "Image file is unavailable or missing on device."
        }
        isLoadingImage = false
    }

    DisposableEffect(imageFile, isTempDecryptedImage) {
        val tempToClean = if (isTempDecryptedImage) imageFile else null
        onDispose {
            viewModel.attachmentStorageManager.deleteTempViewerFile(tempToClean)
        }
    }

    val originalBitmap = decodedImage?.bitmap
    val logicalWidth = decodedImage?.logicalWidth ?: 0
    val logicalHeight = decodedImage?.logicalHeight ?: 0

    // Annotations overlay list & undo/redo history
    val annotations = remember { mutableStateListOf<ImageAnnotationItem>() }
    val undoStack = remember { mutableStateListOf<List<ImageAnnotationItem>>() }
    val redoStack = remember { mutableStateListOf<List<ImageAnnotationItem>>() }
    var annotationsLoaded by remember { mutableStateOf(attachmentId.isBlank()) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }

    // Active Tool, Styling & Selection
    var toolMode by remember { mutableStateOf(AnnotatorTool.PEN) }
    var activeColor by remember { mutableStateOf(Color(0xFFEF4444)) }
    var strokeWidth by remember { mutableFloatStateOf(6f) }

    // Viewport Pan and Zoom
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var fitScale by remember { mutableFloatStateOf(1f) }
    var fitPanOffset by remember { mutableStateOf(Offset.Zero) }
    var hasInitializedViewport by remember(filePath) { mutableStateOf(false) }

    // Drawing in progress
    val currentPoints = remember { mutableStateListOf<Offset>() }
    var shapeStart by remember { mutableStateOf<Offset?>(null) }
    var shapeEnd by remember { mutableStateOf<Offset?>(null) }

    // Dialogs & Sheets
    var showMetadataSheet by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    var textTapPosition by remember { mutableStateOf(Offset.Zero) }
    var editingTextAnnotation by remember { mutableStateOf<ImageAnnotationItem?>(null) }

    fun screenToImage(screenOffset: Offset): Offset {
        return (screenOffset - panOffset) / zoomScale
    }

    fun pushUndo() {
        if (undoStack.size >= 30) undoStack.removeAt(0)
        undoStack.add(annotations.toList())
        redoStack.clear()
        hasUnsavedChanges = true
    }

    fun saveAnnotationsToDb(force: Boolean = false) {
        if (!annotationsLoaded && !force) return
        if (!hasUnsavedChanges && !force) return
        val entities = annotations.map { it.toEntity(attachmentId) }
        viewModel.saveAnnotations(
            attachmentId = attachmentId,
            annotations = entities,
            filePath = imageFile?.absolutePath ?: filePath,
            fileName = fileName
        )
        hasUnsavedChanges = false
    }

    fun flushAnnotationsSync(force: Boolean = false) {
        if (!annotationsLoaded && !force) return
        if (!hasUnsavedChanges && !force) return
        val entities = annotations.map { it.toEntity(attachmentId) }
        val res = viewModel.flushAnnotationsDirect(
            attachmentId = attachmentId,
            annotations = entities,
            filePath = imageFile?.absolutePath ?: filePath,
            fileName = fileName
        )
        if (res.isSuccess) {
            hasUnsavedChanges = false
        }
    }

    val latestFlushAnnotationsSync by rememberUpdatedState(::flushAnnotationsSync)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                latestFlushAnnotationsSync(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            latestFlushAnnotationsSync(false)
        }
    }

    fun saveAndNavigateBack(force: Boolean = false) {
        if (isNavigatingBack) return
        isNavigatingBack = true
        flushAnnotationsSync(force = force)
        onNavigateBack()
    }

    // Load existing annotations for this image on opening (reopen feature)
    LaunchedEffect(attachmentId) {
        if (attachmentId.isNotBlank()) {
            annotationsLoaded = false
            viewModel.loadAnnotations(attachmentId) { loadedEntities ->
                annotations.clear()
                undoStack.clear()
                redoStack.clear()
                loadedEntities.mapNotNull { parseAnnotationItem(it) }.forEach {
                    annotations.add(it)
                }
                hasUnsavedChanges = false
                annotationsLoaded = true
            }
        } else {
            annotationsLoaded = true
        }
    }

    BackHandler {
        saveAndNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            fileName.ifBlank { "Image Annotation" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            "Original untouched • ${annotations.size} annotations",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { saveAndNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Undo
                    IconButton(
                        onClick = {
                            if (undoStack.isNotEmpty()) {
                                redoStack.add(annotations.toList())
                                val prev = undoStack.removeAt(undoStack.lastIndex)
                                annotations.clear()
                                annotations.addAll(prev)
                                hasUnsavedChanges = true
                            }
                        },
                        enabled = undoStack.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }

                    // Redo
                    IconButton(
                        onClick = {
                            if (redoStack.isNotEmpty()) {
                                undoStack.add(annotations.toList())
                                val next = redoStack.removeAt(redoStack.lastIndex)
                                annotations.clear()
                                annotations.addAll(next)
                                hasUnsavedChanges = true
                            }
                        },
                        enabled = redoStack.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }

                    // Metadata info
                    IconButton(onClick = { showMetadataSheet = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Image Details")
                    }

                    // Save
                    IconButton(onClick = { saveAndNavigateBack(force = true) }) {
                        Icon(Icons.Default.Check, contentDescription = "Save Annotations", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Row 1: Tool Selection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ToolChip(icon = Icons.Default.PanTool, label = "Pan & Zoom", isSelected = toolMode == AnnotatorTool.PAN_ZOOM) {
                            toolMode = AnnotatorTool.PAN_ZOOM
                        }
                        ToolChip(icon = Icons.Default.Brush, label = "Pen", isSelected = toolMode == AnnotatorTool.PEN) {
                            toolMode = AnnotatorTool.PEN
                        }
                        ToolChip(icon = Icons.Default.AutoFixHigh, label = "Eraser", isSelected = toolMode == AnnotatorTool.ERASER) {
                            toolMode = AnnotatorTool.ERASER
                        }
                        ToolChip(icon = Icons.Default.Highlight, label = "Highlighter", isSelected = toolMode == AnnotatorTool.HIGHLIGHTER) {
                            toolMode = AnnotatorTool.HIGHLIGHTER
                        }
                        ToolChip(icon = Icons.Default.Rectangle, label = "Rectangle", isSelected = toolMode == AnnotatorTool.RECTANGLE) {
                            toolMode = AnnotatorTool.RECTANGLE
                        }
                        ToolChip(icon = Icons.Default.RadioButtonUnchecked, label = "Circle", isSelected = toolMode == AnnotatorTool.CIRCLE) {
                            toolMode = AnnotatorTool.CIRCLE
                        }
                        ToolChip(icon = Icons.AutoMirrored.Filled.ArrowForward, label = "Arrow", isSelected = toolMode == AnnotatorTool.ARROW) {
                            toolMode = AnnotatorTool.ARROW
                        }
                        ToolChip(icon = Icons.Default.TextFields, label = "Text Label", isSelected = toolMode == AnnotatorTool.TEXT) {
                            toolMode = AnnotatorTool.TEXT
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Row 2: Stroke Width and Colors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Width selection
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            listOf(3f, 6f, 12f, 20f).forEach { w ->
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            if (strokeWidth == w) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                                            CircleShape
                                        )
                                        .clickable { strokeWidth = w },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size((w / 2).coerceIn(4f, 14f).dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                }
                            }
                        }

                        // Colors
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Color(0xFFEF4444), // Crimson
                                Color(0xFFF59E0B), // Amber
                                Color(0xFF10B981), // Emerald
                                Color(0xFF2563EB), // Blue
                                Color(0xFF8B5CF6), // Purple
                                Color(0xFFFFFFFF), // White
                                Color(0xFF1E293B)  // Black
                            ).forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(color, CircleShape)
                                        .border(
                                            width = if (activeColor == color) 3.dp else 1.dp,
                                            color = if (activeColor == color) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            shape = CircleShape
                                        )
                                        .clickable { activeColor = color }
                                )
                            }
                        }

                        // Reset View button
                        IconButton(onClick = {
                            panOffset = fitPanOffset
                            zoomScale = fitScale
                        }) {
                            Icon(Icons.Default.NearMe, contentDescription = "Reset Zoom")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F172A))
        ) {
            val density = LocalDensity.current
            val viewportW = with(density) { maxWidth.toPx() }
            val viewportH = with(density) { maxHeight.toPx() }

            LaunchedEffect(logicalWidth, logicalHeight, viewportW, viewportH) {
                if (logicalWidth > 0 && logicalHeight > 0 && viewportW > 0f && viewportH > 0f) {
                    val computedFit = min(viewportW / logicalWidth.toFloat(), viewportH / logicalHeight.toFloat())
                        .coerceIn(0.1f, 3.0f)
                    val computedPan = Offset(
                        (viewportW - logicalWidth * computedFit) / 2f,
                        (viewportH - logicalHeight * computedFit) / 2f
                    )
                    fitScale = computedFit
                    fitPanOffset = computedPan
                    if (!hasInitializedViewport) {
                        zoomScale = computedFit
                        panOffset = computedPan
                        hasInitializedViewport = true
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(toolMode) {
                        if (toolMode == AnnotatorTool.PAN_ZOOM) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                panOffset += pan
                                zoomScale = (zoomScale * zoom).coerceIn(min(0.25f, fitScale * 0.5f), 5.0f)
                            }
                    } else if (toolMode == AnnotatorTool.ERASER) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val hitPoint = screenToImage(change.position)
                            val hit = annotations.lastOrNull { it.containsPoint(hitPoint) }
                            if (hit != null) {
                                pushUndo()
                                annotations.remove(hit)
                                saveAnnotationsToDb()
                            }
                        }
                    } else if (toolMode == AnnotatorTool.TEXT) {
                        detectTapGestures { tapOffset ->
                            val pt = screenToImage(tapOffset)
                            val existing = annotations.lastOrNull { it.type == "TEXT_NOTE" && it.containsPoint(pt) }
                            if (existing != null) {
                                editingTextAnnotation = existing
                                textInput = existing.text
                                showTextDialog = true
                            } else {
                                editingTextAnnotation = null
                                textTapPosition = pt
                                textInput = ""
                                showTextDialog = true
                            }
                        }
                    } else if (toolMode == AnnotatorTool.PEN || toolMode == AnnotatorTool.HIGHLIGHTER) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                currentPoints.clear()
                                currentPoints.add(screenToImage(startOffset))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPoints.add(screenToImage(change.position))
                            },
                            onDragEnd = {
                                if (currentPoints.isNotEmpty()) {
                                    pushUndo()
                                    val isHigh = toolMode == AnnotatorTool.HIGHLIGHTER
                                    annotations.add(
                                        ImageAnnotationItem(
                                            type = if (isHigh) "HIGHLIGHT" else "DRAWING",
                                            points = currentPoints.toList(),
                                            colorArgb = if (isHigh) activeColor.copy(alpha = 0.35f).toArgb() else activeColor.toArgb(),
                                            strokeWidth = if (isHigh) strokeWidth * 2.5f else strokeWidth
                                        )
                                    )
                                    currentPoints.clear()
                                    saveAnnotationsToDb()
                                }
                            }
                        )
                    } else {
                        // Shapes: RECTANGLE, CIRCLE, ARROW
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                shapeStart = screenToImage(startOffset)
                                shapeEnd = shapeStart
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                shapeEnd = screenToImage(change.position)
                            },
                            onDragEnd = {
                                val s = shapeStart
                                val e = shapeEnd
                                if (s != null && e != null) {
                                    pushUndo()
                                    val typeStr = when (toolMode) {
                                        AnnotatorTool.RECTANGLE -> "RECTANGLE"
                                        AnnotatorTool.CIRCLE -> "CIRCLE"
                                        AnnotatorTool.ARROW -> "ARROW"
                                        else -> "RECTANGLE"
                                    }
                                    annotations.add(
                                        ImageAnnotationItem(
                                            type = typeStr,
                                            start = s,
                                            end = e,
                                            colorArgb = activeColor.toArgb(),
                                            strokeWidth = strokeWidth
                                        )
                                    )
                                    shapeStart = null
                                    shapeEnd = null
                                    saveAnnotationsToDb()
                                }
                            }
                        )
                    }
                }
                .pointerInput(toolMode) {
                    if (toolMode == AnnotatorTool.ERASER) {
                        detectTapGestures { tapOffset ->
                            val pt = screenToImage(tapOffset)
                            val hit = annotations.lastOrNull { it.containsPoint(pt) }
                            if (hit != null) {
                                pushUndo()
                                annotations.remove(hit)
                                saveAnnotationsToDb()
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val bmp = originalBitmap
                translate(left = panOffset.x, top = panOffset.y) {
                    scale(scale = zoomScale, pivot = Offset.Zero) {
                        // 1. Draw original untouched image bitmap as foundation
                        if (bmp != null) {
                            val imgBmp = bmp.asImageBitmap()
                            val dstW = if (logicalWidth > 0) logicalWidth else bmp.width
                            val dstH = if (logicalHeight > 0) logicalHeight else bmp.height
                            drawImage(
                                image = imgBmp,
                                dstSize = IntSize(dstW, dstH)
                            )
                        }

                        // 2. Draw saved annotation overlays on top
                        for (item in annotations) {
                            drawImageAnnotation(item)
                        }

                        // 3. Draw active pen stroke in progress
                        if (currentPoints.size > 1) {
                            val path = Path().apply {
                                moveTo(currentPoints.first().x, currentPoints.first().y)
                                for (p in currentPoints.drop(1)) lineTo(p.x, p.y)
                            }
                            val isHigh = toolMode == AnnotatorTool.HIGHLIGHTER
                            drawPath(
                                path = path,
                                color = if (isHigh) activeColor.copy(alpha = 0.35f) else activeColor,
                                style = Stroke(
                                    width = if (isHigh) strokeWidth * 2.5f else strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        // 4. Draw active shape preview in progress
                        val s = shapeStart
                        val e = shapeEnd
                        if (s != null && e != null) {
                            val preview = ImageAnnotationItem(
                                type = when (toolMode) {
                                    AnnotatorTool.RECTANGLE -> "RECTANGLE"
                                    AnnotatorTool.CIRCLE -> "CIRCLE"
                                    AnnotatorTool.ARROW -> "ARROW"
                                    else -> "RECTANGLE"
                                },
                                start = s,
                                end = e,
                                colorArgb = activeColor.toArgb(),
                                strokeWidth = strokeWidth
                            )
                            drawImageAnnotation(preview)
                        }
                    }
                }
            }

            if (isLoadingImage) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (imageUnavailableReason != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = imageUnavailableReason ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }

            // Live Tool Status Hint Pill
            val imgHint = when (toolMode) {
                AnnotatorTool.PAN_ZOOM -> "Pan & Zoom • Pinch or drag"
                AnnotatorTool.PEN -> "Pen • Draw on photo"
                AnnotatorTool.HIGHLIGHTER -> "Highlighter • Highlight areas"
                AnnotatorTool.RECTANGLE -> "Rectangle • Drag to outline"
                AnnotatorTool.CIRCLE -> "Circle • Drag to encircle"
                AnnotatorTool.ARROW -> "Arrow • Drag to point"
                AnnotatorTool.TEXT -> "Text Label • Tap photo to add or edit"
                AnnotatorTool.ERASER -> "Eraser • Tap or drag to erase"
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        imgHint,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (zoomScale != fitScale || panOffset != fitPanOffset) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable {
                                zoomScale = fitScale
                                panOffset = fitPanOffset
                            }
                        ) {
                            Text(
                                "Reset ${(zoomScale * 100).toInt()}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            }
        }
    }

    // Text Label Add / Edit Dialog
    if (showTextDialog) {
        val editing = editingTextAnnotation
        AlertDialog(
            onDismissRequest = {
                editingTextAnnotation = null
                showTextDialog = false
            },
            title = { Text(if (editing != null) "Edit Label" else "Add Label") },
            text = {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Label text...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = textInput.trim()
                        if (trimmed.isNotEmpty()) {
                            pushUndo()
                            if (editing != null) {
                                val idx = annotations.indexOfFirst { it.id == editing.id }
                                if (idx >= 0) {
                                    annotations[idx] = editing.copy(text = trimmed, colorArgb = activeColor.toArgb())
                                }
                            } else {
                                annotations.add(
                                    ImageAnnotationItem(
                                        type = "TEXT_NOTE",
                                        start = textTapPosition,
                                        text = trimmed,
                                        colorArgb = activeColor.toArgb(),
                                        strokeWidth = strokeWidth
                                    )
                                )
                            }
                            saveAnnotationsToDb()
                        }
                        editingTextAnnotation = null
                        showTextDialog = false
                    }
                ) {
                    Text(if (editing != null) "Save" else "Add")
                }
            },
            dismissButton = {
                if (editing != null) {
                    TextButton(onClick = {
                        pushUndo()
                        annotations.removeAll { it.id == editing.id }
                        saveAnnotationsToDb()
                        editingTextAnnotation = null
                        showTextDialog = false
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    TextButton(onClick = {
                        editingTextAnnotation = null
                        showTextDialog = false
                    }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Metadata Details BottomSheet
    if (showMetadataSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMetadataSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Image Details & Integrity",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val displayFile = imageFile
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("File Name", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(displayFile?.name ?: fileName, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("File Size", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(SmartNoteUtils.formatFileSize(displayFile?.length() ?: 0L), fontWeight = FontWeight.Bold)
                        }
                        if (originalBitmap != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Resolution", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$logicalWidth × $logicalHeight px", fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Annotations Count", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${annotations.size} (stored separately)", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Original File Status", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Untouched / Read-Only", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                TextButton(
                    onClick = { showMetadataSheet = false },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

fun DrawScope.drawImageAnnotation(item: ImageAnnotationItem) {
    val color = Color(item.colorArgb)
    when (item.type) {
        "DRAWING", "HIGHLIGHT" -> {
            if (item.points.size > 1) {
                val path = Path().apply {
                    moveTo(item.points.first().x, item.points.first().y)
                    for (p in item.points.drop(1)) lineTo(p.x, p.y)
                }
                drawPath(path = path, color = color, style = Stroke(width = item.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            } else if (item.points.size == 1) {
                drawCircle(color = color, radius = item.strokeWidth / 2, center = item.points.first())
            }
        }
        "RECTANGLE" -> {
            val left = min(item.start.x, item.end.x)
            val top = min(item.start.y, item.end.y)
            val width = kotlin.math.abs(item.end.x - item.start.x)
            val height = kotlin.math.abs(item.end.y - item.start.y)
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = item.strokeWidth)
            )
        }
        "CIRCLE" -> {
            val center = Offset((item.start.x + item.end.x) / 2, (item.start.y + item.end.y) / 2)
            val radius = hypot(item.end.x - item.start.x, item.end.y - item.start.y) / 2
            drawCircle(color = color, radius = radius, center = center, style = Stroke(width = item.strokeWidth))
        }
        "ARROW" -> {
            drawLine(color = color, start = item.start, end = item.end, strokeWidth = item.strokeWidth, cap = StrokeCap.Round)
            val angle = atan2(item.end.y - item.start.y, item.end.x - item.start.x)
            val arrowSize = 22f
            val arrowAngle = Math.PI / 6
            val p1 = Offset(
                (item.end.x - arrowSize * cos(angle - arrowAngle)).toFloat(),
                (item.end.y - arrowSize * sin(angle - arrowAngle)).toFloat()
            )
            val p2 = Offset(
                (item.end.x - arrowSize * cos(angle + arrowAngle)).toFloat(),
                (item.end.y - arrowSize * sin(angle + arrowAngle)).toFloat()
            )
            drawLine(color = color, start = item.end, end = p1, strokeWidth = item.strokeWidth, cap = StrokeCap.Round)
            drawLine(color = color, start = item.end, end = p2, strokeWidth = item.strokeWidth, cap = StrokeCap.Round)
        }
        "TEXT_NOTE" -> {
            val paint = android.graphics.Paint().apply {
                this.color = if (color.luminance() > 0.6f) {
                    android.graphics.Color.rgb(15, 23, 42)
                } else {
                    android.graphics.Color.WHITE
                }
                textSize = 22f
                isAntiAlias = true
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val measuredWidth = max(paint.measureText(item.text), 60f)
            drawRoundRect(
                color = color,
                topLeft = Offset(item.start.x - 6f, item.start.y - 28f),
                size = Size(measuredWidth + 12f, 36f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawContext.canvas.nativeCanvas.apply {
                drawText(item.text, item.start.x, item.start.y - 4f, paint)
            }
        }
    }
}
