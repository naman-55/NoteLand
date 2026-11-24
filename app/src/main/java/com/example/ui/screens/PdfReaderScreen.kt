package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.createBitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.PdfDocumentEngine
import com.example.util.PdfTextMatch
import com.example.util.PdfValidationResult
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PdfReaderScreen(
    attachmentId: String,
    filePath: String,
    fileName: String,
    viewModel: NoteViewModel,
    initialPage: Int = 1,
    onNavigateToNote: (String, String) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val isAutoSaveEnabled by viewModel.pdfAutoSaveEnabled.collectAsStateWithLifecycle()
    val effectiveAttachmentId = remember(attachmentId) { attachmentId.ifBlank { "demo_pdf" } }

    // Page & Document State
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(1) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var outgoingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var lastRenderedPageIndex by remember { mutableIntStateOf(0) }
    var pageTurnDirection by remember { mutableIntStateOf(0) } // 1 next, -1 prev, 0 initial reveal
    val pageAnimProgress = remember { Animatable(0f) }

    // Annotations, History & Save State
    val allPdfAnnotations = remember { mutableStateListOf<PdfPageAnnotation>() }
    val undoStack = remember { mutableStateListOf<List<PdfPageAnnotation>>() }
    val redoStack = remember { mutableStateListOf<List<PdfPageAnnotation>>() }
    var annotationsLoaded by remember(effectiveAttachmentId) { mutableStateOf(false) }
    var annotationRevision by remember(effectiveAttachmentId) { mutableIntStateOf(0) }
    var hasUnsavedChanges by remember(effectiveAttachmentId) { mutableStateOf(false) }
    var saveStatus by remember(effectiveAttachmentId) { mutableStateOf(PdfSaveStatus.SAVED) }
    var showUnsavedExitDialog by remember { mutableStateOf(false) }

    // Active Tool & Styling
    var toolMode by remember { mutableStateOf(PdfTool.PAN_ZOOM) }
    var selectedShape by remember { mutableStateOf(ShapeType.CIRCLE) }
    var highlightColor by remember { mutableStateOf(Color(0xFFFEF08A)) }
    var inkColor by remember { mutableStateOf(Color(0xFFDC2626)) }
    var textColor by remember { mutableStateOf(Color(0xFF1E293B)) }
    var activeColor by remember { mutableStateOf(Color(0xFFFEF08A)) }
    var penStrokeWidth by remember { mutableFloatStateOf(5f) }
    var textFontSize by remember { mutableFloatStateOf(36f) }
    var selectedStickyPalette by remember { mutableStateOf(PdfStickyColors.first()) }
    var stickyAsCard by remember { mutableStateOf(true) }
    var activeTheme by remember { mutableStateOf(ReadingTheme.WHITE) }
    var isFocusMode by remember { mutableStateOf(false) }

    // Viewport Pan & Zoom
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var fitScale by remember { mutableFloatStateOf(1f) }
    var fitPanOffset by remember { mutableStateOf(Offset.Zero) }
    var zoomAnimJob by remember { mutableStateOf<Job?>(null) }

    fun animateViewportTo(targetZoom: Float, targetPan: Offset) {
        zoomAnimJob?.cancel()
        val startZoom = zoomScale
        val startPan = panOffset
        zoomAnimJob = coroutineScope.launch {
            val anim = Animatable(0f)
            anim.animateTo(1f, tween(durationMillis = 240, easing = FastOutSlowInEasing)) {
                val t = value
                zoomScale = startZoom + (targetZoom - startZoom) * t
                panOffset = Offset(
                    startPan.x + (targetPan.x - startPan.x) * t,
                    startPan.y + (targetPan.y - startPan.y) * t
                )
            }
        }
    }

    // Active Gesture & Selection State
    var gestureStart by remember { mutableStateOf<Offset?>(null) }
    var gestureEnd by remember { mutableStateOf<Offset?>(null) }
    val currentPoints = remember { mutableStateListOf<Offset>() }
    var currentPageTextLayout by remember { mutableStateOf<PdfPageTextLayout?>(null) }
    var activeHighlightSelection by remember { mutableStateOf<PdfHighlightSelection?>(null) }
    var selectedAnnotationId by remember { mutableStateOf<String?>(null) }
    var activeRotatingAngleDeg by remember { mutableStateOf<Float?>(null) }
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()
    val latestPageTextLayout by rememberUpdatedState(currentPageTextLayout)
    val latestHighlightColor by rememberUpdatedState(highlightColor)
    val latestInkColor by rememberUpdatedState(inkColor)
    val latestActiveColor by rememberUpdatedState(activeColor)
    val latestPenStrokeWidth by rememberUpdatedState(penStrokeWidth)
    val latestSelectedShape by rememberUpdatedState(selectedShape)
    val latestSelectedStickyPalette by rememberUpdatedState(selectedStickyPalette)
    val latestStickyAsCard by rememberUpdatedState(stickyAsCard)

    LaunchedEffect(toolMode, currentPageIndex) {
        gestureStart = null
        gestureEnd = null
        activeHighlightSelection = null
        activeRotatingAngleDeg = null
        val curSel = allPdfAnnotations.firstOrNull { it.id == selectedAnnotationId }
        if (toolMode == PdfTool.PAN_ZOOM || toolMode == PdfTool.ERASER || curSel?.pageNumber != (currentPageIndex + 1)) {
            selectedAnnotationId = null
        }
    }

    val selectedAnnotation = remember(selectedAnnotationId, allPdfAnnotations.toList(), currentPageIndex) {
        allPdfAnnotations.firstOrNull {
            it.id == selectedAnnotationId && it.pageNumber == (currentPageIndex + 1)
        }
    }

    // Dialogs & Sheets
    var showReaderSettingsSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showStickyWallSheet by remember { mutableStateOf(false) }
    var showThumbnailsSheet by remember { mutableStateOf(false) }
    var showJumpPageDialog by remember { mutableStateOf(false) }
    var jumpPageInput by remember { mutableStateOf("") }
    var showTextBoxDialog by remember { mutableStateOf(false) }
    var pendingTextPosition by remember { mutableStateOf(Offset.Zero) }
    var activeTextToEdit by remember { mutableStateOf<PdfPageAnnotation?>(null) }
    var activeCommentToView by remember { mutableStateOf<PdfPageAnnotation?>(null) }
    var showNewCommentDialog by remember { mutableStateOf(false) }
    var pendingCommentPosition by remember { mutableStateOf(Offset.Zero) }
    var pendingStickyWallDraft by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var pendingRelocateStickyId by remember { mutableStateOf<String?>(null) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var showExportShareDialog by remember { mutableStateOf(false) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }
    var createNoteInitialBody by remember { mutableStateOf("") }
    var pendingCreateNoteAnnotationId by remember { mutableStateOf<String?>(null) }
    var linkPickerTargetAnnotationId by remember { mutableStateOf<String?>(null) }
    var pendingExportAnnotatedMode by remember { mutableStateOf(true) }

    val allActiveNotes by viewModel.allActiveNotes.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()

    // Search State (searches real PDF page text + annotations across pages)
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeSearchMatchIndex by remember { mutableIntStateOf(0) }
    var extractedPageTexts by remember(effectiveAttachmentId, filePath) { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var searchMatches by remember { mutableStateOf<List<PdfTextMatch>>(emptyList()) }

    // Text-to-Speech (TTS)
    var ttsEngine: TextToSpeech? by remember { mutableStateOf(null) }
    var isTtsPlaying by remember { mutableStateOf(false) }
    var ttsSpeed by remember { mutableFloatStateOf(1.0f) }

    val appContext = context.applicationContext
    DisposableEffect(appContext) {
        var speech: TextToSpeech? = null
        try {
            speech = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    try {
                        speech?.language = Locale.getDefault()
                    } catch (_: Exception) {
                    }
                }
            }
            ttsEngine = speech
        } catch (_: Exception) {
        }
        onDispose {
            try {
                speech?.stop()
                speech?.shutdown()
            } catch (_: Exception) {
            }
        }
    }

    val currentBookmarkTargetId = "$effectiveAttachmentId#${currentPageIndex + 1}"
    val isCurrentPageBookmarked = remember(bookmarks, currentBookmarkTargetId) {
        bookmarks.any { it.targetType == "PDF_PAGE" && it.targetId == currentBookmarkTargetId }
    }

    var resolvedPdfFile by remember(effectiveAttachmentId, filePath) { mutableStateOf<File?>(null) }
    var isTempDecryptedPdf by remember(effectiveAttachmentId, filePath) { mutableStateOf(false) }
    var isResolvingPdf by remember(effectiveAttachmentId, filePath) { mutableStateOf(true) }
    var pdfUnavailableReason by remember(effectiveAttachmentId, filePath) { mutableStateOf<String?>(null) }

    var appliedInitialPage by remember(effectiveAttachmentId, filePath, initialPage) { mutableStateOf(false) }

    LaunchedEffect(effectiveAttachmentId, filePath, isVaultUnlocked) {
        isResolvingPdf = true
        pdfUnavailableReason = null
        val resolution = viewModel.resolveAttachmentForViewer(
            attachmentId = effectiveAttachmentId,
            rawFilePath = filePath,
            defaultExtension = "pdf"
        )
        if (resolution.isLockedOut) {
            resolvedPdfFile = null
            isTempDecryptedPdf = false
            pdfUnavailableReason = "This PDF is in your Vault. Unlock the Vault to view it."
            isResolvingPdf = false
            return@LaunchedEffect
        }
        val candidate = resolution.file
        val finalFile = withContext(Dispatchers.IO) {
            if (candidate != null && candidate.exists()) {
                candidate
            } else if (filePath.isNotBlank()) {
                val f = File(filePath)
                if (f.exists()) f else null
            } else if (effectiveAttachmentId.startsWith("demo")) {
                val demoTarget = candidate ?: File(context.filesDir, "demo_guide.pdf")
                demoTarget.parentFile?.mkdirs()
                if (!demoTarget.exists()) {
                    createSamplePdf(demoTarget, fileName.ifBlank { "Quick Start Guide" }, pageCount = 3)
                }
                demoTarget
            } else {
                null
            }
        }
        if (finalFile == null) {
            resolvedPdfFile = null
            isTempDecryptedPdf = false
            pdfUnavailableReason = "PDF file is unavailable or missing on device."
            isResolvingPdf = false
            return@LaunchedEffect
        }

        val validation = withContext(Dispatchers.IO) { PdfDocumentEngine.validatePdfFile(finalFile) }
        when (validation) {
            is PdfValidationResult.Missing -> {
                resolvedPdfFile = null
                isTempDecryptedPdf = false
                pdfUnavailableReason = "PDF file is unavailable or missing on device."
            }
            is PdfValidationResult.Corrupted -> {
                resolvedPdfFile = null
                isTempDecryptedPdf = resolution.isTempDecrypted
                pdfUnavailableReason = validation.reason
            }
            is PdfValidationResult.Valid -> {
                resolvedPdfFile = finalFile
                isTempDecryptedPdf = resolution.isTempDecrypted
                totalPages = validation.pageCount.coerceAtLeast(1)
                if (!appliedInitialPage && initialPage > 1) {
                    currentPageIndex = (initialPage - 1).coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                    appliedInitialPage = true
                }
            }
        }
        isResolvingPdf = false
    }

    LaunchedEffect(resolvedPdfFile) {
        val target = resolvedPdfFile
        extractedPageTexts = if (target != null && target.exists()) {
            withContext(Dispatchers.IO) { PdfDocumentEngine.extractPdfPageTexts(target) }
        } else {
            emptyMap()
        }
    }

    LaunchedEffect(searchQuery, allPdfAnnotations.toList(), extractedPageTexts, resolvedPdfFile) {
        val q = searchQuery.trim()
        val pdfFile = resolvedPdfFile
        if (q.isEmpty() || pdfFile == null || !pdfFile.exists()) {
            searchMatches = emptyList()
            activeSearchMatchIndex = 0
        } else {
            val found = withContext(Dispatchers.IO) {
                PdfDocumentEngine.searchPdfDocumentAndAnnotations(
                    pdfFile = pdfFile,
                    query = q,
                    annotations = allPdfAnnotations.toList(),
                    preExtractedPages = extractedPageTexts
                )
            }
            searchMatches = found
            if (activeSearchMatchIndex >= found.size) {
                activeSearchMatchIndex = 0
            }
            val first = found.firstOrNull()
            if (first != null && activeSearchMatchIndex == 0) {
                currentPageIndex = (first.pageNumber - 1).coerceIn(0, (totalPages - 1).coerceAtLeast(0))
            }
        }
    }

    DisposableEffect(resolvedPdfFile, isTempDecryptedPdf) {
        val fileToClean = if (isTempDecryptedPdf) resolvedPdfFile else null
        onDispose {
            viewModel.attachmentStorageManager.deleteTempViewerFile(fileToClean)
        }
    }

    suspend fun renderPageBitmap(targetFile: File, pageIdx: Int): Pair<Bitmap?, Int> = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        try {
            pfd = ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val count = max(renderer.pageCount, 1)
            val safeIdx = pageIdx.coerceIn(0, count - 1)
            page = renderer.openPage(safeIdx)
            val maxSide = max(page.width, page.height).coerceAtLeast(1)
            val scale = (2400f / maxSide).coerceAtMost(2f)
            val targetWidth = (page.width * scale).toInt().coerceAtLeast(1)
            val targetHeight = (page.height * scale).toInt().coerceAtLeast(1)
            val bmp = createBitmap(targetWidth, targetHeight)
            val canvas = android.graphics.Canvas(bmp)
            canvas.drawColor(android.graphics.Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bmp to count
        } catch (_: Throwable) {
            null to 0
        } finally {
            try { page?.close() } catch (_: Throwable) {}
            try { renderer?.close() } catch (_: Throwable) {}
            try { pfd?.close() } catch (_: Throwable) {}
        }
    }

    LaunchedEffect(currentPageIndex, resolvedPdfFile) {
        val targetFile = resolvedPdfFile
        if (targetFile == null) {
            currentBitmap = null
            outgoingBitmap = null
            currentPageTextLayout = null
            return@LaunchedEffect
        }
        val oldBitmap = currentBitmap
        val dir = when {
            oldBitmap == null -> 0
            currentPageIndex > lastRenderedPageIndex -> 1
            currentPageIndex < lastRenderedPageIndex -> -1
            else -> 0
        }
        val (bmp, count) = renderPageBitmap(targetFile, currentPageIndex)
        if (bmp == null) {
            currentBitmap = null
            outgoingBitmap = null
            currentPageTextLayout = null
            pdfUnavailableReason = "Unable to render PDF page. Document may be corrupted or encrypted."
            return@LaunchedEffect
        }
        val analyzedLayout = withContext(Dispatchers.Default) { analyzePdfPageBitmap(bmp) }
        totalPages = count
        outgoingBitmap = if (oldBitmap != null && dir != 0) oldBitmap else null
        pageTurnDirection = dir
        currentBitmap = bmp
        currentPageTextLayout = analyzedLayout
        lastRenderedPageIndex = currentPageIndex
        pageAnimProgress.snapTo(0f)
        pageAnimProgress.animateTo(
            targetValue = 1f,
            animationSpec = if (dir == 0) {
                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
            } else {
                tween(durationMillis = 280, easing = FastOutSlowInEasing)
            }
        )
        outgoingBitmap = null
    }

    fun seedMockupAnnotationsIfEmpty() {
        if (allPdfAnnotations.isEmpty()) {
            allPdfAnnotations.add(
                PdfPageAnnotation(
                    type = "HIGHLIGHT",
                    pageNumber = 1,
                    start = Offset(95f, 310f),
                    end = Offset(360f, 345f),
                    colorArgb = Color(0xFF93C5FD).copy(alpha = 0.52f).toArgb()
                )
            )
            allPdfAnnotations.add(
                PdfPageAnnotation(
                    type = "COMMENT",
                    pageNumber = 1,
                    start = Offset(860f, 150f),
                    content = "Tips: Tap any sticky card to edit, or drag to move anywhere!",
                    colorArgb = PdfStickyColors[0].fill.toArgb(),
                    strokeWidth = STICKY_STYLE_CARD
                )
            )
            allPdfAnnotations.add(
                PdfPageAnnotation(
                    type = "SHAPE_STAR",
                    pageNumber = 1,
                    start = Offset(70f, 430f),
                    end = Offset(130f, 490f),
                    colorArgb = Color(0xFFEF4444).toArgb(),
                    strokeWidth = 5f
                )
            )
            allPdfAnnotations.add(
                PdfPageAnnotation(
                    type = "HIGHLIGHT",
                    pageNumber = 1,
                    start = Offset(95f, 430f),
                    end = Offset(430f, 465f),
                    colorArgb = Color(0xFFFEF08A).copy(alpha = 0.58f).toArgb()
                )
            )
            allPdfAnnotations.add(
                PdfPageAnnotation(
                    type = "SHAPE_CIRCLE",
                    pageNumber = 1,
                    start = Offset(490f, 545f),
                    end = Offset(930f, 615f),
                    colorArgb = Color(0xFFF97316).toArgb(),
                    strokeWidth = 6f
                )
            )
            allPdfAnnotations.add(
                PdfPageAnnotation(
                    type = "UNDERLINE",
                    pageNumber = 1,
                    start = Offset(95f, 705f),
                    end = Offset(970f, 735f),
                    colorArgb = Color(0xFFDC2626).toArgb(),
                    strokeWidth = 4f
                )
            )
            allPdfAnnotations.add(
                PdfPageAnnotation(
                    type = "SHAPE_ARROW",
                    pageNumber = 1,
                    start = Offset(1070f, 820f),
                    end = Offset(1020f, 710f),
                    colorArgb = Color(0xFFDC2626).toArgb(),
                    strokeWidth = 5f
                )
            )
            allPdfAnnotations.add(
                PdfPageAnnotation(
                    type = "TEXT_BOX",
                    pageNumber = 1,
                    start = Offset(890f, 850f),
                    content = "Handwrite here",
                    colorArgb = Color(0xFFDC2626).toArgb(),
                    strokeWidth = 36f
                )
            )
        }
    }

    LaunchedEffect(effectiveAttachmentId) {
        annotationsLoaded = false
        hasUnsavedChanges = false
        selectedAnnotationId = null
        activeRotatingAngleDeg = null
        saveStatus = PdfSaveStatus.SAVED
        viewModel.loadAnnotations(effectiveAttachmentId) { loadedList ->
            allPdfAnnotations.clear()
            undoStack.clear()
            redoStack.clear()
            loadedList.mapNotNull { parsePdfAnnotation(it) }.forEach {
                allPdfAnnotations.add(it)
            }
            if (
                allPdfAnnotations.isEmpty() &&
                effectiveAttachmentId.startsWith("demo") &&
                !viewModel.hasSeededPdfDemo(effectiveAttachmentId)
            ) {
                seedMockupAnnotationsIfEmpty()
                viewModel.markSeededPdfDemo(effectiveAttachmentId)
                val seededEntities = allPdfAnnotations.map { it.toEntity(effectiveAttachmentId) }
                viewModel.saveAnnotations(
                    attachmentId = effectiveAttachmentId,
                    annotations = seededEntities,
                    filePath = resolvedPdfFile?.absolutePath ?: filePath,
                    fileName = fileName
                )
            }
            annotationsLoaded = true
        }
    }

    fun buildAnnotationEntities() = allPdfAnnotations.map { it.toEntity(effectiveAttachmentId) }

    fun markAnnotationsModified() {
        annotationRevision++
        hasUnsavedChanges = true
        saveStatus = if (isAutoSaveEnabled) PdfSaveStatus.SAVING else PdfSaveStatus.UNSAVED
    }

    fun triggerManualSave(showFeedback: Boolean = true, onComplete: (Boolean) -> Unit = {}) {
        val snapshotRev = annotationRevision
        val entities = buildAnnotationEntities()
        saveStatus = PdfSaveStatus.SAVING
        coroutineScope.launch {
            val result = viewModel.saveAnnotationsDirect(
                attachmentId = effectiveAttachmentId,
                annotations = entities,
                filePath = resolvedPdfFile?.absolutePath ?: filePath,
                fileName = fileName
            )
            if (result.isSuccess) {
                if (annotationRevision == snapshotRev) {
                    hasUnsavedChanges = false
                    saveStatus = PdfSaveStatus.SAVED
                }
                if (showFeedback) {
                    Toast.makeText(context, "PDF changes saved", Toast.LENGTH_SHORT).show()
                }
                onComplete(true)
            } else {
                saveStatus = PdfSaveStatus.ERROR
                if (showFeedback) {
                    Toast.makeText(context, "Could not save PDF changes", Toast.LENGTH_SHORT).show()
                }
                onComplete(false)
            }
        }
    }

    fun flushPendingAutoSaveSync() {
        if (annotationsLoaded && hasUnsavedChanges && isAutoSaveEnabled) {
            val res = viewModel.flushAnnotationsDirect(
                attachmentId = effectiveAttachmentId,
                annotations = buildAnnotationEntities(),
                filePath = resolvedPdfFile?.absolutePath ?: filePath,
                fileName = fileName
            )
            if (res.isSuccess) {
                hasUnsavedChanges = false
                saveStatus = PdfSaveStatus.SAVED
            }
        }
    }

    val latestFlushPendingAutoSave by rememberUpdatedState(::flushPendingAutoSaveSync)

    // Debounced automatic persistence when Auto-save is enabled
    LaunchedEffect(annotationRevision, isAutoSaveEnabled, annotationsLoaded) {
        if (annotationsLoaded && hasUnsavedChanges && isAutoSaveEnabled) {
            saveStatus = PdfSaveStatus.SAVING
            delay(350)
            val snapshotRev = annotationRevision
            val result = viewModel.saveAnnotationsDirect(
                attachmentId = effectiveAttachmentId,
                annotations = buildAnnotationEntities(),
                filePath = resolvedPdfFile?.absolutePath ?: filePath,
                fileName = fileName
            )
            if (result.isSuccess) {
                if (annotationRevision == snapshotRev) {
                    hasUnsavedChanges = false
                    saveStatus = PdfSaveStatus.SAVED
                }
            } else {
                saveStatus = PdfSaveStatus.ERROR
            }
        }
    }

    // Flush pending autosave on backgrounding or disposal
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                latestFlushPendingAutoSave()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            latestFlushPendingAutoSave()
        }
    }

    fun handleToggleAutoSave(enabled: Boolean) {
        viewModel.setPdfAutoSaveEnabled(enabled)
        if (enabled && hasUnsavedChanges) {
            triggerManualSave(showFeedback = false)
        } else if (!enabled && hasUnsavedChanges) {
            saveStatus = PdfSaveStatus.UNSAVED
        }
        Toast.makeText(
            context,
            if (enabled) "Auto-save enabled" else "Manual save mode enabled",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun pushUndo() {
        if (undoStack.size >= 30) undoStack.removeAt(0)
        undoStack.add(allPdfAnnotations.toList())
        redoStack.clear()
    }

    fun performUndo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(allPdfAnnotations.toList())
            val prev = undoStack.removeAt(undoStack.lastIndex)
            allPdfAnnotations.clear()
            allPdfAnnotations.addAll(prev)
            markAnnotationsModified()
        }
    }

    fun performRedo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(allPdfAnnotations.toList())
            val next = redoStack.removeAt(redoStack.lastIndex)
            allPdfAnnotations.clear()
            allPdfAnnotations.addAll(next)
            markAnnotationsModified()
        }
    }

    fun screenToPdf(screenOffset: Offset): Offset = (screenOffset - panOffset) / zoomScale

    fun startTtsReading(speed: Float = ttsSpeed) {
        val currentPageNum = currentPageIndex + 1
        val pageAnnotations = allPdfAnnotations.filter { it.pageNumber == currentPageNum }
        val spokenNotes = pageAnnotations
            .filter { (it.type == "TEXT_BOX" || it.type == "COMMENT") && it.content.isNotBlank() }
            .joinToString(separator = ". ") { it.content.trim() }
        val docLabel = fileName.ifBlank { "PDF Document" }
        val pageSummary = buildString {
            append("$docLabel, Page $currentPageNum of $totalPages. ")
            if (spokenNotes.isNotBlank()) {
                append(spokenNotes)
            } else if (pageAnnotations.isNotEmpty()) {
                append("${pageAnnotations.size} annotation markups on this page.")
            } else {
                append("No text notes on this page.")
            }
        }
        ttsEngine?.setSpeechRate(speed)
        ttsEngine?.speak(pageSummary, TextToSpeech.QUEUE_FLUSH, null, "PDF_PAGE_$currentPageNum")
        isTtsPlaying = true
    }

    fun stopTtsReading() {
        ttsEngine?.stop()
        isTtsPlaying = false
    }

    val savePdfCopyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri: Uri? ->
        val srcFile = resolvedPdfFile
        if (targetUri == null || srcFile == null || !srcFile.exists()) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            if (pendingExportAnnotatedMode && allPdfAnnotations.isNotEmpty()) {
                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val base = fileName.substringBeforeLast('.').ifBlank { "Document" }
                val tmpAnnotated = File(exportDir, "${base}_annotated_${System.currentTimeMillis()}.pdf")
                val renderRes = withContext(Dispatchers.IO) {
                    PdfDocumentEngine.exportAnnotatedPdfToFile(
                        pdfFile = srcFile,
                        annotations = allPdfAnnotations.toList(),
                        destFile = tmpAnnotated
                    )
                }
                renderRes.onSuccess { renderedFile ->
                    viewModel.exportFileToUri(renderedFile, targetUri) { exportRes ->
                        runCatching { renderedFile.delete() }
                        exportRes.onSuccess {
                            Toast.makeText(context, "Saved annotated PDF copy", Toast.LENGTH_SHORT).show()
                        }.onFailure { err ->
                            Toast.makeText(context, "Export failed: ${err.localizedMessage ?: "Error"}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.onFailure { err ->
                    Toast.makeText(context, "Export failed: ${err.localizedMessage ?: "Error"}", Toast.LENGTH_SHORT).show()
                }
            } else {
                viewModel.exportFileToUri(srcFile, targetUri) { exportRes ->
                    exportRes.onSuccess {
                        Toast.makeText(context, "Saved original PDF copy", Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        Toast.makeText(context, "Export failed: ${err.localizedMessage ?: "Error"}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun executeSharePdf(annotated: Boolean) {
        val targetFile = resolvedPdfFile
        if (pdfUnavailableReason != null || targetFile == null || !targetFile.exists()) {
            Toast.makeText(context, pdfUnavailableReason ?: "PDF file not found on device", Toast.LENGTH_SHORT).show()
            return
        }
        if (hasUnsavedChanges) {
            viewModel.flushAnnotationsDirect(
                attachmentId = effectiveAttachmentId,
                annotations = buildAnnotationEntities(),
                filePath = targetFile.absolutePath,
                fileName = fileName
            )
            hasUnsavedChanges = false
            saveStatus = PdfSaveStatus.SAVED
        }
        coroutineScope.launch {
            val fileToShare = if (annotated && allPdfAnnotations.isNotEmpty()) {
                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val base = fileName.substringBeforeLast('.').ifBlank { "Document" }
                val tmpAnnotated = File(exportDir, "${base}_annotated.pdf")
                val exportRes = withContext(Dispatchers.IO) {
                    PdfDocumentEngine.exportAnnotatedPdfToFile(
                        pdfFile = targetFile,
                        annotations = allPdfAnnotations.toList(),
                        destFile = tmpAnnotated
                    )
                }
                exportRes.getOrElse { err ->
                    Toast.makeText(context, "Failed to render annotated PDF: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                    return@launch
                }
            } else {
                targetFile
            }
            try {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fileToShare)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Export & Share PDF"))
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to share PDF: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareCurrentPdf() {
        val targetFile = resolvedPdfFile
        if (pdfUnavailableReason != null || targetFile == null || !targetFile.exists()) {
            Toast.makeText(context, pdfUnavailableReason ?: "PDF file not found on device", Toast.LENGTH_SHORT).show()
            return
        }
        showExportShareDialog = true
    }

    fun openCreateNoteForPageOrSelection(ann: PdfPageAnnotation? = null) {
        val pageNum = currentPageIndex + 1
        val pageExtract = extractedPageTexts[pageNum].orEmpty().trim()
        val draft = buildString {
            if (ann != null && ann.content.isNotBlank()) {
                append(ann.content.trim())
                if (pageExtract.isNotBlank()) {
                    append("\n\n")
                }
            }
            if (pageExtract.isNotBlank()) {
                append(pageExtract.take(1500))
            }
        }
        createNoteInitialBody = draft
        pendingCreateNoteAnnotationId = ann?.id
        showCreateNoteDialog = true
    }

    fun handleNavigateBack() {
        if (hasUnsavedChanges && !isAutoSaveEnabled) {
            showUnsavedExitDialog = true
        } else {
            stopTtsReading()
            flushPendingAutoSaveSync()
            onNavigateBack()
        }
    }

    BackHandler {
        if (isFocusMode) {
            isFocusMode = false
        } else {
            handleNavigateBack()
        }
    }

    val animatedDeskColor by animateColorAsState(
        targetValue = activeTheme.deskColor,
        animationSpec = tween(durationMillis = 280),
        label = "pdfDeskColor"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedDeskColor)
    ) {
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val viewportHeightPx = with(density) { maxHeight.toPx() }

        LaunchedEffect(currentBitmap, currentPageIndex, viewportWidthPx, viewportHeightPx) {
            val bmp = currentBitmap
            if (bmp != null && viewportWidthPx > 0f && viewportHeightPx > 0f) {
                val padX = with(density) { 28.dp.toPx() }
                val padY = with(density) { 250.dp.toPx() }
                val newFit = min(
                    (viewportWidthPx - padX) / bmp.width.toFloat(),
                    (viewportHeightPx - padY).coerceAtLeast(220f) / bmp.height.toFloat()
                ).coerceIn(0.35f, 2.2f)
                val newPan = Offset(
                    (viewportWidthPx - bmp.width * newFit) / 2f,
                    ((viewportHeightPx - bmp.height * newFit) / 2f - with(density) { 46.dp.toPx() }).coerceAtLeast(with(density) { 68.dp.toPx() })
                )
                fitScale = newFit
                fitPanOffset = newPan
                zoomScale = newFit
                panOffset = newPan
            }
        }

        // Interactive PDF Document Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(toolMode, currentPageIndex) {
                    if (toolMode == PdfTool.PAN_ZOOM) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            zoomAnimJob?.cancel()
                            val oldZoom = zoomScale
                            val newZoom = (oldZoom * zoom).coerceIn(0.3f, 5.0f)
                            val zoomFactor = newZoom / oldZoom
                            panOffset = centroid - (centroid - panOffset) * zoomFactor + pan
                            zoomScale = newZoom
                        }
                    } else if (toolMode == PdfTool.ERASER) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val pt = screenToPdf(change.position)
                            val hit = allPdfAnnotations.lastOrNull {
                                it.pageNumber == (currentPageIndex + 1) && it.containsPoint(pt)
                            }
                            if (hit != null) {
                                pushUndo()
                                allPdfAnnotations.remove(hit)
                                markAnnotationsModified()
                            }
                        }
                    } else {
                        var activeEditId: String? = null
                        var activeEditHandle = SelectionDragHandle.NONE
                        var lastEditPdfPos = Offset.Zero
                        var rotatePrevPointerAngleDeg = 0f
                        var rotateAccumulatedDeg = 0f
                        var editUndoSnapshot: List<PdfPageAnnotation>? = null
                        var drawingNewItem = false

                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val pt = screenToPdf(startOffset)
                                val pageNum = currentPageIndex + 1
                                val currentSelected = allPdfAnnotations.firstOrNull {
                                    it.id == selectedAnnotationId && it.pageNumber == pageNum
                                }
                                val selHandle = if (currentSelected != null) {
                                    hitTestSelectionHandle(currentSelected, pt, zoomScale)
                                } else {
                                    SelectionDragHandle.NONE
                                }
                                val isExplicitHandle = selHandle == SelectionDragHandle.ROTATE ||
                                    selHandle == SelectionDragHandle.RESIZE_BR ||
                                    selHandle == SelectionDragHandle.RESIZE_R ||
                                    selHandle == SelectionDragHandle.RESIZE_B ||
                                    selHandle == SelectionDragHandle.ENDPOINT_START ||
                                    selHandle == SelectionDragHandle.ENDPOINT_END ||
                                    selHandle == SelectionDragHandle.CURVE_CONTROL

                                val allowSelectedBodyMove = selHandle == SelectionDragHandle.MOVE &&
                                    (toolMode == PdfTool.SELECT || toolMode == PdfTool.NOTE || toolMode == PdfTool.TEXT_BOX)

                                if (currentSelected != null && (isExplicitHandle || allowSelectedBodyMove)) {
                                    activeEditId = currentSelected.id
                                    activeEditHandle = selHandle
                                    lastEditPdfPos = pt
                                    editUndoSnapshot = allPdfAnnotations.toList()
                                    if (selHandle == SelectionDragHandle.ROTATE) {
                                        val pivot = currentSelected.rotationPivot()
                                        rotatePrevPointerAngleDeg = (atan2(pt.y - pivot.y, pt.x - pivot.x) * (180.0 / Math.PI)).toFloat()
                                        rotateAccumulatedDeg = currentSelected.rotationDeg
                                        activeRotatingAngleDeg = currentSelected.rotationDeg
                                    }
                                    drawingNewItem = false
                                    return@detectDragGestures
                                }

                                if (toolMode == PdfTool.SELECT) {
                                    val hit = allPdfAnnotations.lastOrNull {
                                        it.pageNumber == pageNum &&
                                            hitTestSelectionHandle(it, pt, zoomScale) != SelectionDragHandle.NONE
                                    }
                                    if (hit != null) {
                                        val h = hitTestSelectionHandle(hit, pt, zoomScale)
                                        selectedAnnotationId = hit.id
                                        activeEditId = hit.id
                                        activeEditHandle = h
                                        lastEditPdfPos = pt
                                        editUndoSnapshot = allPdfAnnotations.toList()
                                        if (h == SelectionDragHandle.ROTATE) {
                                            val pivot = hit.rotationPivot()
                                            rotatePrevPointerAngleDeg = (atan2(pt.y - pivot.y, pt.x - pivot.x) * (180.0 / Math.PI)).toFloat()
                                            rotateAccumulatedDeg = hit.rotationDeg
                                            activeRotatingAngleDeg = hit.rotationDeg
                                        }
                                    } else {
                                        selectedAnnotationId = null
                                    }
                                    drawingNewItem = false
                                    return@detectDragGestures
                                }

                                if (toolMode == PdfTool.NOTE || toolMode == PdfTool.TEXT_BOX) {
                                    val targetType = if (toolMode == PdfTool.NOTE) "COMMENT" else "TEXT_BOX"
                                    val hit = allPdfAnnotations.lastOrNull {
                                        it.pageNumber == pageNum && it.type == targetType &&
                                            hitTestSelectionHandle(it, pt, zoomScale) != SelectionDragHandle.NONE
                                    }
                                    if (hit != null) {
                                        val h = hitTestSelectionHandle(hit, pt, zoomScale)
                                        selectedAnnotationId = hit.id
                                        activeEditId = hit.id
                                        activeEditHandle = h
                                        lastEditPdfPos = pt
                                        editUndoSnapshot = allPdfAnnotations.toList()
                                        if (h == SelectionDragHandle.ROTATE) {
                                            val pivot = hit.rotationPivot()
                                            rotatePrevPointerAngleDeg = (atan2(pt.y - pivot.y, pt.x - pivot.x) * (180.0 / Math.PI)).toFloat()
                                            rotateAccumulatedDeg = hit.rotationDeg
                                            activeRotatingAngleDeg = hit.rotationDeg
                                        }
                                    }
                                    drawingNewItem = false
                                    return@detectDragGestures
                                }

                                drawingNewItem = true
                                when (toolMode) {
                                    PdfTool.PEN -> {
                                        currentPoints.clear()
                                        currentPoints.add(pt)
                                    }
                                    PdfTool.HIGHLIGHT -> {
                                        gestureStart = pt
                                        gestureEnd = pt
                                        activeHighlightSelection = computeTextHighlightSelection(
                                            layout = latestPageTextLayout,
                                            anchor = pt,
                                            focus = pt
                                        )
                                    }
                                    PdfTool.UNDERLINE, PdfTool.STRIKETHROUGH, PdfTool.SHAPE -> {
                                        gestureStart = pt
                                        gestureEnd = pt
                                    }
                                    else -> Unit
                                }
                            },
                            onDrag = { change, _ ->
                                val editId = activeEditId
                                if (editId != null) {
                                    change.consume()
                                    val pt = screenToPdf(change.position)
                                    val idx = allPdfAnnotations.indexOfFirst { it.id == editId }
                                    if (idx >= 0) {
                                        val item = allPdfAnnotations[idx]
                                        allPdfAnnotations[idx] = when (activeEditHandle) {
                                            SelectionDragHandle.MOVE -> {
                                                val delta = pt - lastEditPdfPos
                                                item.translatedBy(delta.x, delta.y)
                                            }
                                            SelectionDragHandle.ROTATE -> {
                                                val pivot = item.rotationPivot()
                                                if (hypot(pt.x - pivot.x, pt.y - pivot.y) > 4f) {
                                                    val currDeg = (atan2(pt.y - pivot.y, pt.x - pivot.x) * (180.0 / Math.PI)).toFloat()
                                                    val stepDelta = normalizeAngleDeg(currDeg - rotatePrevPointerAngleDeg)
                                                    rotatePrevPointerAngleDeg = currDeg
                                                    rotateAccumulatedDeg = normalizeAngleDeg(rotateAccumulatedDeg + stepDelta)
                                                    val snapped = snapAngleDeg(rotateAccumulatedDeg)
                                                    activeRotatingAngleDeg = snapped
                                                    item.copy(rotationDeg = snapped)
                                                } else {
                                                    item
                                                }
                                            }
                                            else -> item.resizedFromHandle(activeEditHandle, pt)
                                        }
                                    }
                                    lastEditPdfPos = pt
                                    return@detectDragGestures
                                }

                                if (!drawingNewItem) return@detectDragGestures
                                change.consume()
                                val pt = screenToPdf(change.position)
                                when (toolMode) {
                                    PdfTool.PEN -> currentPoints.add(pt)
                                    PdfTool.HIGHLIGHT -> {
                                        gestureEnd = pt
                                        val anchor = gestureStart ?: pt
                                        activeHighlightSelection = computeTextHighlightSelection(
                                            layout = latestPageTextLayout,
                                            anchor = anchor,
                                            focus = pt
                                        )
                                    }
                                    PdfTool.UNDERLINE, PdfTool.STRIKETHROUGH, PdfTool.SHAPE -> {
                                        gestureEnd = pt
                                    }
                                    else -> Unit
                                }
                            },
                            onDragEnd = {
                                if (activeEditId != null) {
                                    editUndoSnapshot?.let { snap ->
                                        if (undoStack.size >= 30) undoStack.removeAt(0)
                                        undoStack.add(snap)
                                        redoStack.clear()
                                    }
                                    markAnnotationsModified()
                                } else if (drawingNewItem) {
                                    when (toolMode) {
                                        PdfTool.PEN -> {
                                            if (currentPoints.isNotEmpty()) {
                                                pushUndo()
                                                val created = PdfPageAnnotation(
                                                    type = "DRAWING",
                                                    pageNumber = currentPageIndex + 1,
                                                    points = currentPoints.toList(),
                                                    colorArgb = latestInkColor.toArgb(),
                                                    strokeWidth = latestPenStrokeWidth
                                                )
                                                allPdfAnnotations.add(created)
                                                selectedAnnotationId = created.id
                                                currentPoints.clear()
                                                markAnnotationsModified()
                                            }
                                        }
                                        PdfTool.HIGHLIGHT -> {
                                            val sel = activeHighlightSelection
                                            if (sel != null && sel.segments.isNotEmpty() && (sel.snappedToText || sel.segments.maxOf { it.width } >= 10f)) {
                                                val minLeft = sel.segments.minOf { it.left }
                                                val minTop = sel.segments.minOf { it.top }
                                                val maxRight = sel.segments.maxOf { it.right }
                                                val maxBottom = sel.segments.maxOf { it.bottom }
                                                pushUndo()
                                                val created = PdfPageAnnotation(
                                                    type = "HIGHLIGHT",
                                                    pageNumber = currentPageIndex + 1,
                                                    start = Offset(minLeft, minTop),
                                                    end = Offset(maxRight, maxBottom),
                                                    colorArgb = latestHighlightColor.copy(alpha = 0.48f).toArgb(),
                                                    strokeWidth = 2f,
                                                    segments = sel.segments
                                                )
                                                allPdfAnnotations.add(created)
                                                selectedAnnotationId = created.id
                                                markAnnotationsModified()
                                            }
                                            gestureStart = null
                                            gestureEnd = null
                                            activeHighlightSelection = null
                                        }
                                        PdfTool.UNDERLINE, PdfTool.STRIKETHROUGH, PdfTool.SHAPE -> {
                                            val s = gestureStart
                                            val e = gestureEnd
                                            if (s != null && e != null && hypot(e.x - s.x, e.y - s.y) >= 6f) {
                                                pushUndo()
                                                val annType = when (toolMode) {
                                                    PdfTool.UNDERLINE -> "UNDERLINE"
                                                    PdfTool.STRIKETHROUGH -> "STRIKETHROUGH"
                                                    PdfTool.SHAPE -> when (latestSelectedShape) {
                                                        ShapeType.RECT -> "SHAPE_RECT"
                                                        ShapeType.CIRCLE -> "SHAPE_CIRCLE"
                                                        ShapeType.ARROW -> "SHAPE_ARROW"
                                                        ShapeType.CURVE_ARROW -> "SHAPE_CURVE_ARROW"
                                                        ShapeType.S_CURVE_ARROW -> "SHAPE_S_CURVE_ARROW"
                                                        ShapeType.DOUBLE_ARROW -> "SHAPE_DOUBLE_ARROW"
                                                        ShapeType.ELBOW_ARROW -> "SHAPE_ELBOW_ARROW"
                                                        ShapeType.STAR -> "SHAPE_STAR"
                                                    }
                                                    else -> "UNDERLINE"
                                                }
                                                val initialPoints = if (annType == "SHAPE_CURVE_ARROW") {
                                                    listOf(defaultCurveControlPoint(s, e))
                                                } else {
                                                    emptyList()
                                                }
                                                val created = PdfPageAnnotation(
                                                    type = annType,
                                                    pageNumber = currentPageIndex + 1,
                                                    points = initialPoints,
                                                    start = s,
                                                    end = e,
                                                    colorArgb = latestActiveColor.toArgb(),
                                                    strokeWidth = latestPenStrokeWidth
                                                )
                                                allPdfAnnotations.add(created)
                                                selectedAnnotationId = created.id
                                                markAnnotationsModified()
                                            }
                                            gestureStart = null
                                            gestureEnd = null
                                        }
                                        else -> Unit
                                    }
                                }
                                activeEditId = null
                                activeEditHandle = SelectionDragHandle.NONE
                                activeRotatingAngleDeg = null
                                editUndoSnapshot = null
                                drawingNewItem = false
                            },
                            onDragCancel = {
                                activeEditId = null
                                activeEditHandle = SelectionDragHandle.NONE
                                activeRotatingAngleDeg = null
                                editUndoSnapshot = null
                                drawingNewItem = false
                                gestureStart = null
                                gestureEnd = null
                                activeHighlightSelection = null
                                currentPoints.clear()
                            }
                        )
                    }
                }
                .pointerInput(toolMode, currentPageIndex) {
                    val handleTap: (Offset) -> Unit = { tapOffset ->
                        val pt = screenToPdf(tapOffset)
                        val pageNum = currentPageIndex + 1
                        val draft = pendingStickyWallDraft
                        val relocateId = pendingRelocateStickyId
                        val currentSelected = allPdfAnnotations.firstOrNull {
                            it.id == selectedAnnotationId && it.pageNumber == pageNum
                        }
                        if (draft != null) {
                            val (draftText, asCard) = draft
                            val halfW = if (asCard) STICKY_CARD_WIDTH / 2f else 25f
                            pushUndo()
                            val created = PdfPageAnnotation(
                                type = "COMMENT",
                                pageNumber = pageNum,
                                start = Offset((pt.x - halfW).coerceAtLeast(20f), (pt.y - 24f).coerceAtLeast(20f)),
                                content = draftText,
                                colorArgb = latestSelectedStickyPalette.fill.toArgb(),
                                strokeWidth = if (asCard) STICKY_STYLE_CARD else STICKY_STYLE_BADGE
                            )
                            allPdfAnnotations.add(created)
                            selectedAnnotationId = created.id
                            markAnnotationsModified()
                            pendingStickyWallDraft = null
                            Toast.makeText(context, "Sticky note placed on Page $pageNum", Toast.LENGTH_SHORT).show()
                        } else if (relocateId != null) {
                            val idx = allPdfAnnotations.indexOfFirst { it.id == relocateId }
                            if (idx >= 0) {
                                val existing = allPdfAnnotations[idx]
                                val halfW = if (latestStickyAsCard) STICKY_CARD_WIDTH / 2f else 25f
                                pushUndo()
                                allPdfAnnotations[idx] = existing.copy(
                                    pageNumber = pageNum,
                                    start = Offset((pt.x - halfW).coerceAtLeast(20f), (pt.y - 24f).coerceAtLeast(20f)),
                                    strokeWidth = if (latestStickyAsCard) STICKY_STYLE_CARD else STICKY_STYLE_BADGE
                                )
                                selectedAnnotationId = existing.id
                                markAnnotationsModified()
                            }
                            pendingRelocateStickyId = null
                            Toast.makeText(context, "Sticky note placed on Page $pageNum", Toast.LENGTH_SHORT).show()
                        } else if (toolMode == PdfTool.ERASER) {
                            val hit = allPdfAnnotations.lastOrNull {
                                it.pageNumber == pageNum && it.containsPoint(pt)
                            }
                            if (hit != null) {
                                pushUndo()
                                allPdfAnnotations.remove(hit)
                                if (selectedAnnotationId == hit.id) selectedAnnotationId = null
                                markAnnotationsModified()
                            }
                        } else if (toolMode == PdfTool.SELECT) {
                            val selectedHit = currentSelected?.takeIf {
                                hitTestSelectionHandle(it, pt, zoomScale) != SelectionDragHandle.NONE
                            }
                            val hit = selectedHit ?: allPdfAnnotations.lastOrNull {
                                it.pageNumber == pageNum &&
                                    hitTestSelectionHandle(it, pt, zoomScale) != SelectionDragHandle.NONE
                            }
                            if (hit != null) {
                                if (hit.id == selectedAnnotationId && hit.hasEditableText()) {
                                    if (hit.type == "COMMENT") activeCommentToView = hit
                                    else if (hit.type == "TEXT_BOX") activeTextToEdit = hit
                                } else {
                                    selectedAnnotationId = hit.id
                                }
                            } else {
                                selectedAnnotationId = null
                            }
                        } else if (currentSelected != null &&
                            toolMode != PdfTool.PAN_ZOOM &&
                            hitTestSelectionHandle(currentSelected, pt, zoomScale) in setOf(
                                SelectionDragHandle.ROTATE,
                                SelectionDragHandle.RESIZE_BR,
                                SelectionDragHandle.RESIZE_R,
                                SelectionDragHandle.RESIZE_B,
                                SelectionDragHandle.ENDPOINT_START,
                                SelectionDragHandle.ENDPOINT_END,
                                SelectionDragHandle.CURVE_CONTROL
                            )
                        ) {
                            // Keep active selection intact if user taps one of its handles
                            selectedAnnotationId = currentSelected.id
                        } else {
                            val tappedComment = allPdfAnnotations.lastOrNull {
                                it.pageNumber == pageNum && it.type == "COMMENT" && it.containsPoint(pt)
                            }
                            if (tappedComment != null) {
                                if (toolMode == PdfTool.NOTE && tappedComment.id != selectedAnnotationId) {
                                    selectedAnnotationId = tappedComment.id
                                } else {
                                    selectedAnnotationId = tappedComment.id
                                    activeCommentToView = tappedComment
                                }
                            } else if (toolMode == PdfTool.TEXT_BOX) {
                                val tappedText = allPdfAnnotations.lastOrNull {
                                    it.pageNumber == pageNum && it.type == "TEXT_BOX" && it.containsPoint(pt)
                                }
                                if (tappedText != null) {
                                    if (tappedText.id != selectedAnnotationId) {
                                        selectedAnnotationId = tappedText.id
                                    } else {
                                        activeTextToEdit = tappedText
                                    }
                                } else {
                                    pendingTextPosition = pt
                                    showTextBoxDialog = true
                                }
                            } else if (toolMode == PdfTool.NOTE) {
                                val halfW = if (latestStickyAsCard) STICKY_CARD_WIDTH / 2f else 25f
                                pendingCommentPosition = Offset((pt.x - halfW).coerceAtLeast(20f), (pt.y - 24f).coerceAtLeast(20f))
                                showNewCommentDialog = true
                            } else if (toolMode == PdfTool.HIGHLIGHT) {
                                val tappedHighlight = allPdfAnnotations.lastOrNull {
                                    it.pageNumber == pageNum && it.type == "HIGHLIGHT" && it.containsPoint(pt)
                                }
                                if (tappedHighlight != null) {
                                    selectedAnnotationId = tappedHighlight.id
                                } else {
                                    val tappedWord = findTappedWordHighlight(latestPageTextLayout, pt)
                                    if (tappedWord != null && tappedWord.segments.isNotEmpty()) {
                                        val seg = tappedWord.segments.first()
                                        pushUndo()
                                        val created = PdfPageAnnotation(
                                            type = "HIGHLIGHT",
                                            pageNumber = pageNum,
                                            start = Offset(seg.left, seg.top),
                                            end = Offset(seg.right, seg.bottom),
                                            colorArgb = latestHighlightColor.copy(alpha = 0.48f).toArgb(),
                                            strokeWidth = 2f,
                                            segments = tappedWord.segments
                                        )
                                        allPdfAnnotations.add(created)
                                        selectedAnnotationId = created.id
                                        markAnnotationsModified()
                                    }
                                }
                            } else {
                                val tappedMark = allPdfAnnotations.lastOrNull {
                                    it.pageNumber == pageNum && it.containsPoint(pt)
                                }
                                if (tappedMark != null) {
                                    if (tappedMark.type == "TEXT_BOX" && tappedMark.id == selectedAnnotationId) {
                                        activeTextToEdit = tappedMark
                                    } else {
                                        selectedAnnotationId = tappedMark.id
                                    }
                                } else if (toolMode == PdfTool.PAN_ZOOM) {
                                    // Keep Pan/Zoom tap quiet when not hitting a sticky or text
                                } else {
                                    selectedAnnotationId = null
                                }
                            }
                        }
                    }

                    if (toolMode == PdfTool.PAN_ZOOM) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                if (zoomScale > fitScale * 1.15f) {
                                    animateViewportTo(fitScale, fitPanOffset)
                                } else {
                                    val newScale = (fitScale * 1.9f).coerceAtMost(4.0f)
                                    val pdfPt = screenToPdf(tapOffset)
                                    animateViewportTo(newScale, tapOffset - (pdfPt * newScale))
                                }
                            },
                            onTap = handleTap
                        )
                    } else {
                        detectTapGestures(onTap = handleTap)
                    }
                }
        ) {
            val selectionAccentColor = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.fillMaxSize()) {
                val p = pageAnimProgress.value.coerceIn(0f, 1f)
                val outBmp = outgoingBitmap
                if (outBmp != null && pageTurnDirection != 0 && p < 1f) {
                    val outAlpha = (1f - p).coerceIn(0f, 1f)
                    val outSlideX = -pageTurnDirection * (outBmp.width * 0.14f) * p
                    val outScale = 1f - 0.04f * p
                    val outW = outBmp.width.toFloat()
                    val outH = outBmp.height.toFloat()
                    val shrinkOffsetX = (outW * zoomScale * (1f - outScale)) * 0.5f
                    val shrinkOffsetY = (outH * zoomScale * (1f - outScale)) * 0.5f
                    translate(
                        left = panOffset.x + outSlideX * zoomScale + shrinkOffsetX,
                        top = panOffset.y + shrinkOffsetY
                    ) {
                        scale(
                            scale = zoomScale * outScale,
                            pivot = Offset.Zero
                        ) {
                            drawRoundRect(
                                color = Color.Black.copy(alpha = (if (activeTheme.isDark) 0.45f else 0.12f) * outAlpha),
                                topLeft = Offset(8f, 14f),
                                size = Size(outW, outH),
                                cornerRadius = CornerRadius(12f, 12f)
                            )
                            drawImage(outBmp.asImageBitmap(), topLeft = Offset.Zero, alpha = outAlpha)
                        }
                    }
                }

                val bmp = currentBitmap
                val inAlpha = when {
                    bmp == null -> 0f
                    pageTurnDirection == 0 -> p
                    p < 1f -> (0.25f + 0.75f * p).coerceIn(0f, 1f)
                    else -> 1f
                }
                val inSlideX = if (bmp != null && pageTurnDirection != 0) {
                    pageTurnDirection * (bmp.width * 0.16f) * (1f - p)
                } else 0f
                val inSlideY = if (pageTurnDirection == 0) 28f * (1f - p) else 0f
                val inPageScale = 0.96f + 0.04f * p

                translate(
                    left = panOffset.x + inSlideX * zoomScale,
                    top = panOffset.y + inSlideY
                ) {
                    scale(scale = zoomScale * inPageScale, pivot = Offset.Zero) {
                        if (bmp != null) {
                            val bw = bmp.width.toFloat()
                            val bh = bmp.height.toFloat()
                            // Elevated studio paper drop shadow
                            drawRoundRect(
                                color = Color.Black.copy(alpha = (if (activeTheme.isDark) 0.45f else 0.12f) * inAlpha),
                                topLeft = Offset(8f, 14f),
                                size = Size(bw, bh),
                                cornerRadius = CornerRadius(12f, 12f)
                            )
                            drawImage(bmp.asImageBitmap(), topLeft = Offset.Zero, alpha = inAlpha)
                            if (activeTheme.bgTint != Color.Transparent) {
                                drawRect(
                                    color = activeTheme.bgTint.copy(alpha = activeTheme.bgTint.alpha * inAlpha),
                                    topLeft = Offset.Zero,
                                    size = Size(bw, bh)
                                )
                            }
                            drawRect(
                                color = Color.Black.copy(alpha = 0.08f * inAlpha),
                                topLeft = Offset.Zero,
                                size = Size(bw, bh),
                                style = Stroke(width = 2f)
                            )
                        }

                        // Saved page annotations
                        allPdfAnnotations
                            .filter { it.pageNumber == (currentPageIndex + 1) }
                            .forEach { drawPdfAnnotation(it) }

                        // Selection outline & resize/curve/rotate handles (visible across all annotation tools)
                        val activeSelection = selectedAnnotation
                        if (activeSelection != null && toolMode != PdfTool.PAN_ZOOM && toolMode != PdfTool.ERASER) {
                            drawSelectionOverlay(
                                item = activeSelection,
                                zoomScale = zoomScale,
                                accentColor = selectionAccentColor
                            )
                        }

                        // Active gesture preview
                        val previewHighlight = activeHighlightSelection
                        if (toolMode == PdfTool.HIGHLIGHT && previewHighlight != null) {
                            drawHighlightSelectionPreview(
                                selection = previewHighlight,
                                color = activeColor
                            )
                        }

                        val gs = gestureStart
                        val ge = gestureEnd
                        if (gs != null && ge != null) {
                            when (toolMode) {
                                PdfTool.HIGHLIGHT -> Unit
                                PdfTool.UNDERLINE -> {
                                    drawLine(
                                        color = activeColor,
                                        start = Offset(min(gs.x, ge.x), max(gs.y, ge.y)),
                                        end = Offset(max(gs.x, ge.x), max(gs.y, ge.y)),
                                        strokeWidth = 4f,
                                        cap = StrokeCap.Round
                                    )
                                }
                                PdfTool.STRIKETHROUGH -> {
                                    val midY = (gs.y + ge.y) / 2f
                                    drawLine(
                                        color = activeColor,
                                        start = Offset(min(gs.x, ge.x), midY),
                                        end = Offset(max(gs.x, ge.x), midY),
                                        strokeWidth = 4f,
                                        cap = StrokeCap.Round
                                    )
                                }
                                PdfTool.SHAPE -> {
                                    when (selectedShape) {
                                        ShapeType.RECT -> drawRoundRect(
                                            color = activeColor,
                                            topLeft = Offset(min(gs.x, ge.x), min(gs.y, ge.y)),
                                            size = Size(kotlin.math.abs(ge.x - gs.x), kotlin.math.abs(ge.y - gs.y)),
                                            cornerRadius = CornerRadius(10f, 10f),
                                            style = Stroke(width = penStrokeWidth)
                                        )
                                        ShapeType.CIRCLE -> drawOval(
                                            color = activeColor,
                                            topLeft = Offset(min(gs.x, ge.x), min(gs.y, ge.y)),
                                            size = Size(kotlin.math.abs(ge.x - gs.x), kotlin.math.abs(ge.y - gs.y)),
                                            style = Stroke(width = penStrokeWidth)
                                        )
                                        ShapeType.ARROW -> drawArrow(gs, ge, activeColor, penStrokeWidth)
                                        ShapeType.CURVE_ARROW -> drawCurveArrow(
                                            start = gs,
                                            end = ge,
                                            control = defaultCurveControlPoint(gs, ge),
                                            color = activeColor,
                                            strokeWidth = penStrokeWidth
                                        )
                                        ShapeType.S_CURVE_ARROW -> drawSCurveArrow(gs, ge, emptyList(), activeColor, penStrokeWidth)
                                        ShapeType.DOUBLE_ARROW -> drawDoubleArrow(gs, ge, emptyList(), activeColor, penStrokeWidth)
                                        ShapeType.ELBOW_ARROW -> drawElbowArrow(gs, ge, emptyList(), activeColor, penStrokeWidth)
                                        ShapeType.STAR -> drawFivePointStar(gs, ge, activeColor, penStrokeWidth)
                                    }
                                }
                                else -> {}
                            }
                        }

                        // Active pen stroke
                        if (currentPoints.size > 1) {
                            val path = Path().apply {
                                moveTo(currentPoints.first().x, currentPoints.first().y)
                                for (p in currentPoints.drop(1)) lineTo(p.x, p.y)
                            }
                            drawPath(
                                path = path,
                                color = activeColor,
                                style = Stroke(width = penStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }
            }
            if (isResolvingPdf) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (pdfUnavailableReason != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = pdfUnavailableReason ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }

        // Top Floating Studio Header + Search + TTS Mini-Player
        AnimatedVisibility(
            visible = !isFocusMode,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PdfFloatingTopBar(
                    fileName = fileName,
                    currentPageIndex = currentPageIndex,
                    totalPages = totalPages,
                    pageAnnotationCount = allPdfAnnotations.count { it.pageNumber == (currentPageIndex + 1) },
                    stickyCount = allPdfAnnotations.count { it.type == "COMMENT" },
                    stickyAccent = selectedStickyPalette.accent,
                    isSearchVisible = showSearchBar,
                    saveStatus = saveStatus,
                    isAutoSaveEnabled = isAutoSaveEnabled,
                    hasUnsavedChanges = hasUnsavedChanges,
                    isTtsPlaying = isTtsPlaying,
                    onBack = { handleNavigateBack() },
                    onJumpPageClick = {
                        jumpPageInput = "${currentPageIndex + 1}"
                        showJumpPageDialog = true
                    },
                    onOpenStickyWall = { showStickyWallSheet = true },
                    onToggleSearch = { showSearchBar = !showSearchBar },
                    onToggleFullscreen = { isFocusMode = true },
                    onManualSave = { triggerManualSave(showFeedback = true) },
                    onToggleAutoSave = { handleToggleAutoSave(it) },
                    onToggleTts = { if (isTtsPlaying) stopTtsReading() else startTtsReading() },
                    onOpenBookmarks = { showBookmarksSheet = true },
                    onSharePdf = { shareCurrentPdf() },
                    onCreateNoteFromPage = { openCreateNoteForPageOrSelection() },
                    onClearCurrentPage = {
                        pushUndo()
                        allPdfAnnotations.removeAll { it.pageNumber == (currentPageIndex + 1) }
                        markAnnotationsModified()
                    },
                    onOpenReaderSettings = { showReaderSettingsSheet = true }
                )

                AnimatedVisibility(visible = showSearchBar) {
                    val activeMatch = searchMatches.getOrNull(activeSearchMatchIndex)
                    val matchBadge = activeMatch?.let { m ->
                        val src = if (m.sourceType == "PAGE_TEXT") "PDF text" else "Annotation"
                        "P.${m.pageNumber} ($src): ${m.snippet}"
                    }
                    PdfSearchGlassBar(
                        query = searchQuery,
                        matchCount = searchMatches.size,
                        activeMatchIdx = activeSearchMatchIndex.coerceIn(0, (searchMatches.size - 1).coerceAtLeast(0)),
                        activeMatchBadge = matchBadge,
                        onQueryChange = {
                            searchQuery = it
                            activeSearchMatchIndex = 0
                        },
                        onPrevMatch = {
                            if (searchMatches.isNotEmpty()) {
                                val nextIdx = (activeSearchMatchIndex - 1 + searchMatches.size) % searchMatches.size
                                activeSearchMatchIndex = nextIdx
                                currentPageIndex = (searchMatches[nextIdx].pageNumber - 1).coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                            }
                        },
                        onNextMatch = {
                            if (searchMatches.isNotEmpty()) {
                                val nextIdx = (activeSearchMatchIndex + 1) % searchMatches.size
                                activeSearchMatchIndex = nextIdx
                                currentPageIndex = (searchMatches[nextIdx].pageNumber - 1).coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                            }
                        },
                        onClose = {
                            showSearchBar = false
                            searchQuery = ""
                        }
                    )
                }

                AnimatedVisibility(visible = isTtsPlaying) {
                    PdfTtsPlayerPill(
                        isPlaying = isTtsPlaying,
                        ttsSpeed = ttsSpeed,
                        currentPageNumber = currentPageIndex + 1,
                        onTogglePlayPause = {
                            if (isTtsPlaying) stopTtsReading() else startTtsReading()
                        },
                        onCycleSpeed = {
                            ttsSpeed = when (ttsSpeed) {
                                1.0f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 0.8f
                                else -> 1.0f
                            }
                            startTtsReading(ttsSpeed)
                        },
                        onStop = { stopTtsReading() }
                    )
                }

                AnimatedVisibility(visible = pendingStickyWallDraft != null || pendingRelocateStickyId != null) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = selectedStickyPalette.fill,
                        border = BorderStroke(1.dp, selectedStickyPalette.accent),
                        shadowElevation = 6.dp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Tap anywhere on the PDF page to place sticky note",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = selectedStickyPalette.ink,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            TextButton(
                                onClick = {
                                    pendingStickyWallDraft = null
                                    pendingRelocateStickyId = null
                                }
                            ) {
                                Text("Cancel", color = selectedStickyPalette.accent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = activeRotatingAngleDeg != null) {
                    val deg = activeRotatingAngleDeg ?: 0f
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xEC0F172A),
                        border = BorderStroke(1.2.dp, Color(0xFF38BDF8)),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .testTag("pdf_rotation_hud")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.RotateRight,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${deg.roundToInt()}°",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Bottom Floating Glass Studio Dock
        AnimatedVisibility(
            visible = !isFocusMode,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PdfBottomStudioDock(
                toolMode = toolMode,
                selectedShape = selectedShape,
                highlightColor = highlightColor,
                inkColor = inkColor,
                textColor = textColor,
                activeColor = activeColor,
                penStrokeWidth = penStrokeWidth,
                textFontSize = textFontSize,
                selectedStickyPalette = selectedStickyPalette,
                stickyAsCard = stickyAsCard,
                selectedAnnotation = selectedAnnotation,
                currentPageIndex = currentPageIndex,
                totalPages = totalPages,
                isBookmarked = isCurrentPageBookmarked,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                zoomScale = zoomScale / fitScale.coerceAtLeast(0.1f),
                hasPanOffset = panOffset != fitPanOffset,
                onSelectTool = { mode ->
                    toolMode = mode
                    activeColor = when (mode) {
                        PdfTool.HIGHLIGHT -> highlightColor
                        PdfTool.TEXT_BOX -> textColor
                        PdfTool.PEN, PdfTool.UNDERLINE, PdfTool.STRIKETHROUGH, PdfTool.SHAPE -> inkColor
                        else -> activeColor
                    }
                },
                onSelectStickyPalette = { selectedStickyPalette = it },
                onToggleStickyStyle = { stickyAsCard = it },
                onOpenStickyWall = { showStickyWallSheet = true },
                onSelectHighlightColor = {
                    highlightColor = it
                    activeColor = it
                },
                onSelectPenWidth = { penStrokeWidth = it },
                onSelectInkColor = {
                    inkColor = it
                    activeColor = it
                },
                onSelectTextSize = { textFontSize = it },
                onSelectTextColor = {
                    textColor = it
                    activeColor = it
                },
                onSelectShape = { selectedShape = it },
                onEditSelectedText = {
                    val sel = selectedAnnotation
                    if (sel != null) {
                        when (sel.type) {
                            "COMMENT" -> activeCommentToView = sel
                            "TEXT_BOX" -> activeTextToEdit = sel
                        }
                    }
                },
                onUpdateSelectedColor = { newColor ->
                    val idx = allPdfAnnotations.indexOfFirst { it.id == selectedAnnotationId }
                    if (idx >= 0) {
                        val item = allPdfAnnotations[idx]
                        pushUndo()
                        val updatedArgb = if (item.type == "HIGHLIGHT") {
                            newColor.copy(alpha = 0.48f).toArgb()
                        } else {
                            newColor.toArgb()
                        }
                        allPdfAnnotations[idx] = item.copy(colorArgb = updatedArgb)
                        markAnnotationsModified()
                    }
                },
                onAdjustSelectedSize = { dir ->
                    val idx = allPdfAnnotations.indexOfFirst { it.id == selectedAnnotationId }
                    if (idx >= 0) {
                        val item = allPdfAnnotations[idx]
                        pushUndo()
                        val updated = when (item.type) {
                            "COMMENT" -> {
                                if (item.isStickyCard()) {
                                    val nextW = (item.stickyWidth() + dir * 28f).coerceIn(STICKY_CARD_MIN_WIDTH, STICKY_CARD_MAX_WIDTH)
                                    val nextH = (item.stickyHeight() + dir * 20f).coerceIn(STICKY_CARD_MIN_HEIGHT, STICKY_CARD_MAX_HEIGHT)
                                    item.copy(end = Offset(nextW, nextH))
                                } else {
                                    val nextSize = (item.stickyWidth() + dir * 10f).coerceIn(STICKY_BADGE_MIN_SIZE, STICKY_BADGE_MAX_SIZE)
                                    item.copy(end = Offset(nextSize, nextSize))
                                }
                            }
                            "TEXT_BOX" -> {
                                val nextFont = (item.strokeWidth.coerceAtLeast(24f) + dir * 5f).coerceIn(18f, 96f)
                                item.copy(strokeWidth = nextFont)
                            }
                            "HIGHLIGHT" -> {
                                val bbox = item.boundingBox()
                                val scale = if (dir > 0f) 1.12f else 0.89f
                                val localTargetBR = Offset(
                                    bbox.left + (bbox.width * scale).coerceAtLeast(24f),
                                    bbox.top + (bbox.height * scale).coerceAtLeast(18f)
                                )
                                val targetBR = if (item.rotationDeg != 0f) {
                                    localTargetBR.rotatedAround(item.rotationPivot(), item.rotationDeg)
                                } else {
                                    localTargetBR
                                }
                                item.resizedFromHandle(SelectionDragHandle.RESIZE_BR, targetBR)
                            }
                            else -> {
                                val nextStroke = (item.strokeWidth + dir * 2.5f).coerceIn(2f, 24f)
                                item.copy(strokeWidth = nextStroke)
                            }
                        }
                        allPdfAnnotations[idx] = updated
                        markAnnotationsModified()
                    }
                },
                onFlipSelectedCurve = {
                    val idx = allPdfAnnotations.indexOfFirst { it.id == selectedAnnotationId }
                    if (idx >= 0) {
                        pushUndo()
                        allPdfAnnotations[idx] = allPdfAnnotations[idx].flippedCurve()
                        markAnnotationsModified()
                    }
                },
                onDuplicateSelected = {
                    val sel = selectedAnnotation
                    if (sel != null) {
                        pushUndo()
                        val copy = sel.translatedBy(32f, 32f).copy(id = UUID.randomUUID().toString())
                        allPdfAnnotations.add(copy)
                        selectedAnnotationId = copy.id
                        markAnnotationsModified()
                    }
                },
                onBringSelectedToFront = {
                    val idx = allPdfAnnotations.indexOfFirst { it.id == selectedAnnotationId }
                    if (idx >= 0 && idx < allPdfAnnotations.lastIndex) {
                        pushUndo()
                        val item = allPdfAnnotations.removeAt(idx)
                        allPdfAnnotations.add(item)
                        markAnnotationsModified()
                    }
                },
                onDeleteSelected = {
                    val selId = selectedAnnotationId
                    if (selId != null) {
                        pushUndo()
                        allPdfAnnotations.removeAll { it.id == selId }
                        selectedAnnotationId = null
                        markAnnotationsModified()
                    }
                },
                onClearCurrentPage = {
                    pushUndo()
                    allPdfAnnotations.removeAll { it.pageNumber == (currentPageIndex + 1) }
                    selectedAnnotationId = null
                    markAnnotationsModified()
                },
                onPrevPage = {
                    if (currentPageIndex > 0) currentPageIndex--
                },
                onNextPage = {
                    if (currentPageIndex < totalPages - 1) currentPageIndex++
                },
                onJumpPage = {
                    jumpPageInput = "${currentPageIndex + 1}"
                    showJumpPageDialog = true
                },
                onOpenThumbnails = { showThumbnailsSheet = true },
                onToggleBookmark = {
                    viewModel.toggleBookmark(
                        targetType = "PDF_PAGE",
                        targetId = currentBookmarkTargetId,
                        label = "Page ${currentPageIndex + 1} • $fileName"
                    )
                },
                onUndo = { performUndo() },
                onRedo = { performRedo() },
                onResetZoom = {
                    animateViewportTo(fitScale, fitPanOffset)
                },
                onOpenColorPicker = { showColorPickerDialog = true },
                onRotateSelected = { deltaDeg ->
                    val targetIdx = if (selectedAnnotationId != null) {
                        allPdfAnnotations.indexOfFirst { it.id == selectedAnnotationId && it.pageNumber == (currentPageIndex + 1) }
                    } else {
                        allPdfAnnotations.indexOfLast { it.pageNumber == (currentPageIndex + 1) }
                    }
                    if (targetIdx >= 0) {
                        pushUndo()
                        val updated = allPdfAnnotations[targetIdx].rotatedByDelta(deltaDeg)
                        allPdfAnnotations[targetIdx] = updated
                        selectedAnnotationId = updated.id
                        markAnnotationsModified()
                    }
                },
                onRotateSelectedTo = { targetDeg ->
                    val targetIdx = if (selectedAnnotationId != null) {
                        allPdfAnnotations.indexOfFirst { it.id == selectedAnnotationId && it.pageNumber == (currentPageIndex + 1) }
                    } else {
                        allPdfAnnotations.indexOfLast { it.pageNumber == (currentPageIndex + 1) }
                    }
                    if (targetIdx >= 0) {
                        val snapped = snapAngleDeg(targetDeg, snapToleranceDeg = 2.0f)
                        allPdfAnnotations[targetIdx] = allPdfAnnotations[targetIdx].copy(rotationDeg = snapped)
                        selectedAnnotationId = allPdfAnnotations[targetIdx].id
                        markAnnotationsModified()
                    }
                },
                onResetRotationSelected = {
                    val idx = allPdfAnnotations.indexOfFirst { it.id == selectedAnnotationId }
                    if (idx >= 0 && allPdfAnnotations[idx].rotationDeg != 0f) {
                        pushUndo()
                        allPdfAnnotations[idx] = allPdfAnnotations[idx].copy(rotationDeg = 0f)
                        markAnnotationsModified()
                    }
                },
                hasSelectableOnPage = allPdfAnnotations.any { it.pageNumber == (currentPageIndex + 1) },
                onSelectLatestOnPage = {
                    val lastOnPage = allPdfAnnotations.lastOrNull { it.pageNumber == (currentPageIndex + 1) }
                    if (lastOnPage != null) selectedAnnotationId = lastOnPage.id
                },
                onClearSelection = { selectedAnnotationId = null },
                linkedNoteTitle = selectedAnnotation?.linkedNoteId?.let { linkedId ->
                    allActiveNotes.firstOrNull { !it.isLocked && it.id == linkedId }?.title?.ifBlank { "Untitled Note" }
                },
                onLinkNoteToSelected = {
                    selectedAnnotation?.let { linkPickerTargetAnnotationId = it.id }
                },
                onOpenLinkedNote = {
                    val linkedId = selectedAnnotation?.linkedNoteId
                    val target = allActiveNotes.firstOrNull { it.id == linkedId && !it.isLocked }
                    if (target != null) {
                        flushPendingAutoSaveSync()
                        onNavigateToNote(target.id, target.noteType)
                    } else if (linkedId != null) {
                        Toast.makeText(context, "Linked note is locked or unavailable.", Toast.LENGTH_SHORT).show()
                    }
                },
                onCreateNoteFromSelected = {
                    openCreateNoteForPageOrSelection(selectedAnnotation)
                }
            )
        }

        // Exit Focus Mode Pill
        AnimatedVisibility(
            visible = isFocusMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { isFocusMode = false }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Focus Mode", modifier = Modifier.size(18.dp))
                    Text("Controls • ${currentPageIndex + 1}/$totalPages", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Sheets & Dialogs
    if (showReaderSettingsSheet) {
        PdfReaderSettingsSheet(
            fileName = fileName,
            totalPages = totalPages,
            allAnnotations = allPdfAnnotations,
            activeTheme = activeTheme,
            isTtsPlaying = isTtsPlaying,
            bookmarkCount = bookmarks.count { it.targetId.startsWith("$effectiveAttachmentId#") },
            saveStatus = saveStatus,
            isAutoSaveEnabled = isAutoSaveEnabled,
            hasUnsavedChanges = hasUnsavedChanges,
            onManualSave = { triggerManualSave(showFeedback = true) },
            onToggleAutoSave = { handleToggleAutoSave(it) },
            onSelectTheme = { activeTheme = it },
            onToggleTts = { if (isTtsPlaying) stopTtsReading() else startTtsReading() },
            onOpenBookmarks = { showBookmarksSheet = true },
            onSharePdf = { shareCurrentPdf() },
            onClearCurrentPage = {
                pushUndo()
                allPdfAnnotations.removeAll { it.pageNumber == (currentPageIndex + 1) }
                markAnnotationsModified()
            },
            onDismiss = { showReaderSettingsSheet = false }
        )
    }

    if (showThumbnailsSheet) {
        resolvedPdfFile?.let { pdfFile ->
            val bookmarkedPages = bookmarks
                .filter { it.targetType == "PDF_PAGE" && it.targetId.startsWith("$effectiveAttachmentId#") }
                .mapNotNull { it.targetId.substringAfter("#").toIntOrNull() }
                .toSet()
            PdfThumbnailsSheet(
                pdfFile = pdfFile,
                totalPages = totalPages,
                currentPageIndex = currentPageIndex,
                allAnnotations = allPdfAnnotations,
                bookmarkedPages = bookmarkedPages,
                onSelectPage = { currentPageIndex = it },
                onDismiss = { showThumbnailsSheet = false }
            )
        }
    }

    if (showBookmarksSheet) {
        PdfBookmarksSheet(
            fileName = fileName,
            pdfBookmarks = bookmarks.filter { it.targetType == "PDF_PAGE" && it.targetId.startsWith("$effectiveAttachmentId#") },
            totalPages = totalPages,
            onSelectPage = { currentPageIndex = it },
            onRenameBookmark = { bookmark, newLabel ->
                viewModel.updateBookmarkLabel(bookmark, newLabel)
            },
            onDeleteBookmark = { bookmark ->
                viewModel.deleteBookmark(bookmark)
            },
            onDismiss = { showBookmarksSheet = false }
        )
    }

    if (showStickyWallSheet) {
        PdfStickyWallSheet(
            allStickyNotes = allPdfAnnotations.filter { it.type == "COMMENT" },
            currentPageIndex = currentPageIndex,
            selectedStickyPalette = selectedStickyPalette,
            stickyAsCard = stickyAsCard,
            onSelectStickyPalette = { selectedStickyPalette = it },
            onToggleStickyStyle = { stickyAsCard = it },
            onPinNote = { text, asCard ->
                val pageStickies = allPdfAnnotations.count { it.type == "COMMENT" && it.pageNumber == (currentPageIndex + 1) }
                val yPos = 170f + (pageStickies % 5) * 200f
                val xPos = if (pageStickies % 2 == 0) 860f else 560f
                pushUndo()
                val created = PdfPageAnnotation(
                    type = "COMMENT",
                    pageNumber = currentPageIndex + 1,
                    start = Offset(xPos, yPos),
                    content = text,
                    colorArgb = selectedStickyPalette.fill.toArgb(),
                    strokeWidth = if (asCard) STICKY_STYLE_CARD else STICKY_STYLE_BADGE
                )
                allPdfAnnotations.add(created)
                selectedAnnotationId = created.id
                markAnnotationsModified()
            },
            onPlaceDraftOnPdf = { text, asCard ->
                toolMode = PdfTool.NOTE
                pendingRelocateStickyId = null
                pendingStickyWallDraft = text to asCard
            },
            onPlaceExistingOnPdf = { sticky ->
                toolMode = PdfTool.NOTE
                pendingStickyWallDraft = null
                pendingRelocateStickyId = sticky.id
            },
            onJumpToStickyPage = { pNum ->
                currentPageIndex = (pNum - 1).coerceIn(0, totalPages - 1)
            },
            onEditSticky = { activeCommentToView = it },
            onDeleteSticky = { sticky ->
                pushUndo()
                allPdfAnnotations.removeAll { it.id == sticky.id }
                markAnnotationsModified()
            },
            onDismiss = { showStickyWallSheet = false }
        )
    }

    activeCommentToView?.let { comment ->
        val linkedTitle = comment.linkedNoteId?.let { lid ->
            allActiveNotes.firstOrNull { it.id == lid && !it.isLocked }?.title?.ifBlank { "Untitled Note" }
        }
        PdfStickyNoteEditDialog(
            comment = comment,
            linkedNoteTitle = linkedTitle,
            onLinkNote = { linkPickerTargetAnnotationId = comment.id },
            onOpenLinkedNote = {
                val target = allActiveNotes.firstOrNull { it.id == comment.linkedNoteId && !it.isLocked }
                if (target != null) {
                    activeCommentToView = null
                    flushPendingAutoSaveSync()
                    onNavigateToNote(target.id, target.noteType)
                }
            },
            onSave = { newContent, newFill, asCard, newWidth, newHeight ->
                val idx = allPdfAnnotations.indexOfFirst { it.id == comment.id }
                if (idx >= 0) {
                    pushUndo()
                    allPdfAnnotations[idx] = allPdfAnnotations[idx].copy(
                        content = newContent,
                        colorArgb = newFill.toArgb(),
                        strokeWidth = if (asCard) STICKY_STYLE_CARD else STICKY_STYLE_BADGE,
                        end = Offset(newWidth, newHeight)
                    )
                    markAnnotationsModified()
                }
                activeCommentToView = null
            },
            onDelete = {
                pushUndo()
                allPdfAnnotations.removeAll { it.id == comment.id }
                if (selectedAnnotationId == comment.id) selectedAnnotationId = null
                markAnnotationsModified()
                activeCommentToView = null
            },
            onDismiss = { activeCommentToView = null }
        )
    }

    if (showNewCommentDialog) {
        PdfStickyNoteCreateDialog(
            pageNumber = currentPageIndex + 1,
            selectedStickyPalette = selectedStickyPalette,
            initialAsCard = stickyAsCard,
            onSelectPalette = { selectedStickyPalette = it },
            onCreate = { text, asCard, w, h ->
                stickyAsCard = asCard
                pushUndo()
                val created = PdfPageAnnotation(
                    type = "COMMENT",
                    pageNumber = currentPageIndex + 1,
                    start = pendingCommentPosition,
                    end = Offset(w, h),
                    content = text,
                    colorArgb = selectedStickyPalette.fill.toArgb(),
                    strokeWidth = if (asCard) STICKY_STYLE_CARD else STICKY_STYLE_BADGE
                )
                allPdfAnnotations.add(created)
                selectedAnnotationId = created.id
                markAnnotationsModified()
                showNewCommentDialog = false
            },
            onDismiss = { showNewCommentDialog = false }
        )
    }

    if (showColorPickerDialog) {
        val sel = selectedAnnotation
        val dialogTitle = when {
            toolMode == PdfTool.SELECT && sel != null -> "Selection Color Grades & Gradients"
            toolMode == PdfTool.HIGHLIGHT -> "Highlight Color Grades & Gradients"
            toolMode == PdfTool.TEXT_BOX -> "Text Color Grades & Gradients"
            else -> "Ink & Shape Color Grades & Gradients"
        }
        val initialPickedColor = when {
            toolMode == PdfTool.SELECT && sel != null -> Color(sel.colorArgb).copy(alpha = 1f)
            toolMode == PdfTool.HIGHLIGHT -> highlightColor
            toolMode == PdfTool.TEXT_BOX -> textColor
            else -> inkColor
        }
        PdfColorPickerDialog(
            title = dialogTitle,
            initialColor = initialPickedColor,
            onSelectColor = { chosen ->
                if (toolMode == PdfTool.SELECT && sel != null) {
                    val idx = allPdfAnnotations.indexOfFirst { it.id == sel.id }
                    if (idx >= 0) {
                        val item = allPdfAnnotations[idx]
                        pushUndo()
                        val updatedArgb = if (item.type == "HIGHLIGHT") {
                            chosen.copy(alpha = 0.48f).toArgb()
                        } else {
                            chosen.toArgb()
                        }
                        allPdfAnnotations[idx] = item.copy(colorArgb = updatedArgb)
                        markAnnotationsModified()
                    }
                } else {
                    when (toolMode) {
                        PdfTool.HIGHLIGHT -> {
                            highlightColor = chosen
                            activeColor = chosen
                        }
                        PdfTool.TEXT_BOX -> {
                            textColor = chosen
                            activeColor = chosen
                        }
                        else -> {
                            inkColor = chosen
                            activeColor = chosen
                        }
                    }
                }
                showColorPickerDialog = false
            },
            onDismiss = { showColorPickerDialog = false }
        )
    }

    if (showTextBoxDialog) {
        PdfTextAnnotationCreateDialog(
            pageNumber = currentPageIndex + 1,
            textFontSize = textFontSize,
            onSelectSize = { textFontSize = it },
            onCreate = { text ->
                pushUndo()
                val created = PdfPageAnnotation(
                    type = "TEXT_BOX",
                    pageNumber = currentPageIndex + 1,
                    start = pendingTextPosition,
                    content = text,
                    colorArgb = textColor.toArgb(),
                    strokeWidth = textFontSize
                )
                allPdfAnnotations.add(created)
                selectedAnnotationId = created.id
                markAnnotationsModified()
                showTextBoxDialog = false
            },
            onDismiss = { showTextBoxDialog = false }
        )
    }

    activeTextToEdit?.let { textAnn ->
        val linkedTitle = textAnn.linkedNoteId?.let { lid ->
            allActiveNotes.firstOrNull { it.id == lid && !it.isLocked }?.title?.ifBlank { "Untitled Note" }
        }
        PdfTextAnnotationEditDialog(
            textAnn = textAnn,
            linkedNoteTitle = linkedTitle,
            onLinkNote = { linkPickerTargetAnnotationId = textAnn.id },
            onOpenLinkedNote = {
                val target = allActiveNotes.firstOrNull { it.id == textAnn.linkedNoteId && !it.isLocked }
                if (target != null) {
                    activeTextToEdit = null
                    flushPendingAutoSaveSync()
                    onNavigateToNote(target.id, target.noteType)
                }
            },
            onSave = { newText, newColorArgb, newSize ->
                val idx = allPdfAnnotations.indexOfFirst { it.id == textAnn.id }
                if (idx >= 0) {
                    pushUndo()
                    allPdfAnnotations[idx] = allPdfAnnotations[idx].copy(
                        content = newText,
                        colorArgb = newColorArgb,
                        strokeWidth = newSize
                    )
                    markAnnotationsModified()
                }
                activeTextToEdit = null
            },
            onDelete = {
                pushUndo()
                allPdfAnnotations.removeAll { it.id == textAnn.id }
                markAnnotationsModified()
                activeTextToEdit = null
            },
            onDismiss = { activeTextToEdit = null }
        )
    }

    if (showExportShareDialog) {
        PdfExportAndShareDialog(
            fileName = fileName.ifBlank { "Document.pdf" },
            annotationCount = allPdfAnnotations.size,
            onShareAnnotated = { executeSharePdf(annotated = true) },
            onSaveAnnotatedCopy = {
                pendingExportAnnotatedMode = true
                val base = fileName.substringBeforeLast('.').ifBlank { "Document" }
                savePdfCopyLauncher.launch("${base}_annotated.pdf")
            },
            onShareOriginal = { executeSharePdf(annotated = false) },
            onSaveOriginalCopy = {
                pendingExportAnnotatedMode = false
                savePdfCopyLauncher.launch(fileName.ifBlank { "Document.pdf" })
            },
            onDismiss = { showExportShareDialog = false }
        )
    }

    if (showCreateNoteDialog) {
        PdfCreateNoteFromPageDialog(
            fileName = fileName.ifBlank { "Document.pdf" },
            pageNumber = currentPageIndex + 1,
            initialBodyText = createNoteInitialBody,
            folders = folders,
            onCreateNote = { noteTitle, noteBody, folderId, openAfterCreate ->
                val linkTargetAnnId = pendingCreateNoteAnnotationId
                val pageNum = currentPageIndex + 1
                showCreateNoteDialog = false
                pendingCreateNoteAnnotationId = null
                viewModel.createNoteFromPdfSelection(
                    title = noteTitle,
                    selectedText = noteBody,
                    pdfAttachmentId = effectiveAttachmentId,
                    pdfFileName = fileName.ifBlank { "Document.pdf" },
                    pageNumber = pageNum,
                    folderId = folderId
                ) { createdNoteId ->
                    if (linkTargetAnnId != null) {
                        val idx = allPdfAnnotations.indexOfFirst { it.id == linkTargetAnnId }
                        if (idx >= 0) {
                            pushUndo()
                            allPdfAnnotations[idx] = allPdfAnnotations[idx].copy(linkedNoteId = createdNoteId)
                            markAnnotationsModified()
                        }
                    }
                    Toast.makeText(context, "Created note from Page $pageNum", Toast.LENGTH_SHORT).show()
                    if (openAfterCreate) {
                        flushPendingAutoSaveSync()
                        onNavigateToNote(createdNoteId, "STANDARD")
                    }
                }
            },
            onDismiss = {
                showCreateNoteDialog = false
                pendingCreateNoteAnnotationId = null
            }
        )
    }

    linkPickerTargetAnnotationId?.let { targetAnnId ->
        val currentAnn = allPdfAnnotations.firstOrNull { it.id == targetAnnId }
        if (currentAnn != null) {
            PdfLinkNotePickerDialog(
                currentLinkedNoteId = currentAnn.linkedNoteId,
                availableNotes = allActiveNotes,
                onSelectNote = { pickedNote ->
                    val idx = allPdfAnnotations.indexOfFirst { it.id == targetAnnId }
                    if (idx >= 0) {
                        pushUndo()
                        allPdfAnnotations[idx] = allPdfAnnotations[idx].copy(linkedNoteId = pickedNote?.id)
                        markAnnotationsModified()
                    }
                    linkPickerTargetAnnotationId = null
                },
                onCreateAndLinkNote = {
                    linkPickerTargetAnnotationId = null
                    openCreateNoteForPageOrSelection(currentAnn)
                },
                onDismiss = { linkPickerTargetAnnotationId = null }
            )
        } else {
            linkPickerTargetAnnotationId = null
        }
    }

    if (showJumpPageDialog) {
        AlertDialog(
            onDismissRequest = { showJumpPageDialog = false },
            title = { Text("Jump to Page (1 - $totalPages)") },
            text = {
                OutlinedTextField(
                    value = jumpPageInput,
                    onValueChange = { jumpPageInput = it },
                    label = { Text("Page number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val num = jumpPageInput.toIntOrNull()
                        if (num != null && num in 1..totalPages) {
                            currentPageIndex = num - 1
                        }
                        showJumpPageDialog = false
                    }
                ) { Text("Go") }
            },
            dismissButton = {
                TextButton(onClick = { showJumpPageDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showUnsavedExitDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedExitDialog = false },
            title = { Text("Save PDF Changes?") },
            text = { Text("Auto-save is off and you have unsaved edits on this PDF. Save before exiting?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        triggerManualSave(showFeedback = true) {
                            showUnsavedExitDialog = false
                            stopTtsReading()
                            onNavigateBack()
                        }
                    }
                ) { Text("Save & Exit") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showUnsavedExitDialog = false
                            stopTtsReading()
                            onNavigateBack()
                        }
                    ) { Text("Discard", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { showUnsavedExitDialog = false }) { Text("Cancel") }
                }
            }
        )
    }
}
