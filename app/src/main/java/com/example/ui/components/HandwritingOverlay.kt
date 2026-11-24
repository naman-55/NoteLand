package com.example.ui.components

import android.os.Build
import android.view.InputDevice
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoNotTouch
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.InkSample
import com.example.util.InkStroke
import com.example.util.NoteInkTool
import java.util.UUID
import kotlin.math.hypot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val NotelandEraserIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "NotelandEraser",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Solid eraser tip half
        path(fill = SolidColor(Color.Black)) {
            moveTo(4f, 13f)
            lineTo(9f, 8f)
            lineTo(16f, 15f)
            lineTo(11f, 20f)
            close()
        }
        // Outlined body + baseline
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4f, 13f)
            lineTo(14f, 3f)
            lineTo(21f, 10f)
            lineTo(11f, 20f)
            close()
            moveTo(9f, 8f)
            lineTo(16f, 15f)
            moveTo(11f, 20f)
            lineTo(21f, 20f)
        }
    }.build()
}

private data class CachedDryStroke(
    val stroke: InkStroke,
    val path: Path?,
    val pressureSegments: List<PressureInkSegment>?
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HandwritingOverlayLayer(
    strokes: List<InkStroke>,
    scrollState: ScrollState,
    isDrawingMode: Boolean,
    isInkVisible: Boolean,
    isDark: Boolean,
    activeTool: NoteInkTool,
    activeColorArgb: Int,
    activeWidthDp: Float,
    stylusOnlyMode: Boolean,
    onStrokeCompleted: (InkStroke) -> Unit,
    onEraseAt: (pointDp: Offset, radiusDp: Float, startSweep: Boolean) -> Unit,
    onCancelEraseSweep: () -> Unit,
    onStylusDetected: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val densityScale = density.density
    val view = LocalView.current
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val currentTool by rememberUpdatedState(activeTool)
    val currentColorArgb by rememberUpdatedState(activeColorArgb)
    val currentWidthDp by rememberUpdatedState(activeWidthDp)
    val currentStylusOnly by rememberUpdatedState(stylusOnlyMode)
    val currentOnStrokeCompleted by rememberUpdatedState(onStrokeCompleted)
    val currentOnEraseAt by rememberUpdatedState(onEraseAt)
    val currentOnCancelEraseSweep by rememberUpdatedState(onCancelEraseSweep)
    val currentOnStylusDetected by rememberUpdatedState(onStylusDetected)

    // Active wet stroke points & version tick for immediate 1-frame live rendering
    val activeSamples = remember { mutableStateListOf<InkSample>() }
    var activeWetTool by remember { mutableStateOf(activeTool) }
    var activeWetColorArgb by remember { mutableIntStateOf(activeColorArgb) }
    var activeWetWidthDp by remember { mutableFloatStateOf(activeWidthDp) }
    var wetRevision by remember { mutableIntStateOf(0) }

    // Hover & eraser reticle feedback
    var hoverPosPx by remember { mutableStateOf<Offset?>(null) }
    var eraserReticlePx by remember { mutableStateOf<Offset?>(null) }
    var lastStylusActivityMs by remember { mutableLongStateOf(0L) }

    // Request unbuffered hardware-rate dispatch for stylus & touchscreen on Android 12+
    DisposableEffect(isDrawingMode, view) {
        if (isDrawingMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                view.requestUnbufferedDispatch(InputDevice.SOURCE_CLASS_POINTER)
            } catch (_: Exception) {
            }
        }
        onDispose {}
    }

    // Precomputed dry stroke cache (recomputed only when stroke list or density changes)
    val cachedDryStrokes = remember(strokes.toList(), densityScale) {
        val highlighters = ArrayList<CachedDryStroke>()
        val inks = ArrayList<CachedDryStroke>()
        strokes.forEach { stroke ->
            val cached = if (stroke.tool == NoteInkTool.PRESSURE_PEN.name) {
                CachedDryStroke(
                    stroke = stroke,
                    path = null,
                    pressureSegments = buildPressureSegments(stroke.points, stroke.baseWidthDp, densityScale)
                )
            } else {
                CachedDryStroke(
                    stroke = stroke,
                    path = buildCentripetalCatmullRomPath(stroke.points, densityScale),
                    pressureSegments = null
                )
            }
            if (stroke.tool == NoteInkTool.HIGHLIGHTER.name) {
                highlighters.add(cached)
            } else {
                inks.add(cached)
            }
        }
        highlighters + inks
    }

    val inputModifier = if (isDrawingMode) {
        Modifier.pointerInput(isDrawingMode) {
            var activePointerId: PointerId? = null
            var activeStrokeStartedMs = 0L
            var isErasingGesture = false
            var eraseSweepStarted = false
            var isScrollingGesture = false
            var lastScrollY = 0f
            var scrollAccumDy = 0f
            var straightSnapJob: Job? = null
            var didSnapStraight = false
            var strokeFilter: StylusStrokeFilter? = null

            fun cancelWetState() {
                straightSnapJob?.cancel()
                straightSnapJob = null
                activeSamples.clear()
                activePointerId = null
                strokeFilter = null
                didSnapStraight = false
                eraserReticlePx = null
                if (eraseSweepStarted) {
                    currentOnCancelEraseSweep()
                    eraseSweepStarted = false
                }
                isErasingGesture = false
                isScrollingGesture = false
                wetRevision++
            }

            fun scheduleStraightLineCheck() {
                straightSnapJob?.cancel()
                if (activeSamples.size < 4 || didSnapStraight) return
                val capturedSamples = activeSamples.toList()
                straightSnapJob = coroutineScope.launch {
                    delay(380L)
                    if (activePointerId != null && !isErasingGesture && isStraightLineCandidate(capturedSamples)) {
                        val snapped = snapToStraightLine(capturedSamples)
                        activeSamples.clear()
                        activeSamples.addAll(snapped)
                        didSnapStraight = true
                        wetRevision++
                        try {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        } catch (_: Exception) {
                        }
                    }
                }
            }

            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val nowMs = System.currentTimeMillis()

                    // Handle stylus hover reticle when stylus is hovering above the note
                    if (event.type == PointerEventType.Move && event.changes.none { it.pressed }) {
                        val hoveringStylus = event.changes.firstOrNull {
                            it.type == PointerType.Stylus || it.type == PointerType.Eraser
                        }
                        if (hoveringStylus != null) {
                            currentOnStylusDetected()
                            lastStylusActivityMs = nowMs
                            hoverPosPx = hoveringStylus.position
                        } else {
                            hoverPosPx = null
                        }
                        continue
                    }
                    if (event.type == PointerEventType.Exit) {
                        hoverPosPx = null
                        continue
                    }

                    val pressedChanges = event.changes.filter { it.pressed }
                    if (pressedChanges.isEmpty()) {
                        // Pointer lifted: finalize active stroke or erase sweep
                        if (activePointerId != null) {
                            straightSnapJob?.cancel()
                            straightSnapJob = null
                            if (!isErasingGesture && activeSamples.isNotEmpty()) {
                                val rawList = activeSamples.toList()
                                val finalPoints = if (didSnapStraight) rawList else simplifyInkSamples(rawList, 0.25f)
                                val completed = InkStroke(
                                    id = UUID.randomUUID().toString(),
                                    tool = activeWetTool.name,
                                    colorArgb = activeWetColorArgb,
                                    baseWidthDp = activeWetWidthDp,
                                    points = finalPoints
                                )
                                activeSamples.clear()
                                currentOnStrokeCompleted(completed)
                            }
                            activePointerId = null
                            strokeFilter = null
                            didSnapStraight = false
                            isErasingGesture = false
                            eraseSweepStarted = false
                            isScrollingGesture = false
                            eraserReticlePx = null
                            wetRevision++
                        } else if (isScrollingGesture) {
                            isScrollingGesture = false
                        }
                        event.changes.forEach { if (!it.pressed) it.consume() }
                        continue
                    }

                    // Multi-touch handling: 2 fingers scroll smoothly, or reject resting palm during stylus use
                    if (pressedChanges.size >= 2) {
                        val stylusChange = pressedChanges.firstOrNull {
                            it.type == PointerType.Stylus || it.type == PointerType.Eraser
                        }
                        if (stylusChange != null) {
                            // Stylus + resting palm: if a palm touched first, cancel its nascent stroke
                            // and hand over tracking immediately to the stylus
                            if (activePointerId != null && activePointerId != stylusChange.id) {
                                cancelWetState()
                            }
                            currentOnStylusDetected()
                            lastStylusActivityMs = nowMs
                        } else {
                            // 2-finger vertical scroll gesture: roll back any accidental nascent dot
                            val totalTraveledDp = if (activeSamples.size >= 2) {
                                hypot(
                                    activeSamples.last().xDp - activeSamples.first().xDp,
                                    activeSamples.last().yDp - activeSamples.first().yDp
                                )
                            } else 0f
                            if (activePointerId != null &&
                                (nowMs - activeStrokeStartedMs < 220L || totalTraveledDp < 16f)
                            ) {
                                cancelWetState()
                            }
                            val avgY = pressedChanges.take(2).map { it.position.y }.average().toFloat()
                            if (!isScrollingGesture) {
                                isScrollingGesture = true
                                lastScrollY = avgY
                            } else {
                                val dy = avgY - lastScrollY
                                lastScrollY = avgY
                                if (kotlin.math.abs(dy) > 0.2f) {
                                    coroutineScope.launch { scrollState.scrollBy(-dy) }
                                }
                            }
                            pressedChanges.forEach { it.consume() }
                            continue
                        }
                    }

                    // Pick the primary drawing pointer (always prioritize stylus/eraser over finger touch)
                    val preferredPointer = pressedChanges.firstOrNull {
                        it.type == PointerType.Stylus || it.type == PointerType.Eraser
                    } ?: pressedChanges.first()

                    val isStylusOrEraser = preferredPointer.type == PointerType.Stylus ||
                        preferredPointer.type == PointerType.Eraser
                    if (isStylusOrEraser) {
                        currentOnStylusDetected()
                        lastStylusActivityMs = nowMs
                    } else if (currentStylusOnly) {
                        // In Stylus-Only mode, reject touches near stylus use (palm rejection),
                        // and allow clean 1-finger vertical scrolling
                        val nearStylusUse = (nowMs - lastStylusActivityMs) < 650L
                        if (!nearStylusUse) {
                            val currentY = preferredPointer.position.y
                            if (!isScrollingGesture) {
                                isScrollingGesture = true
                                lastScrollY = currentY
                                scrollAccumDy = 0f
                            } else {
                                val dy = currentY - lastScrollY
                                lastScrollY = currentY
                                scrollAccumDy += kotlin.math.abs(dy)
                                if (scrollAccumDy > 3f * densityScale) {
                                    coroutineScope.launch { scrollState.scrollBy(-dy) }
                                }
                            }
                            preferredPointer.consume()
                        }
                        continue
                    }

                    hoverPosPx = null

                    // Begin new stroke or erase sweep on pointer down
                    if (activePointerId == null) {
                        activePointerId = preferredPointer.id
                        activeStrokeStartedMs = nowMs
                        didSnapStraight = false
                        val useHardwareEraser = preferredPointer.type == PointerType.Eraser ||
                            (preferredPointer.type == PointerType.Stylus && event.buttons.isSecondaryPressed)
                        isErasingGesture = useHardwareEraser || currentTool == NoteInkTool.ERASER

                        val effectiveWidth = if (currentTool == NoteInkTool.HIGHLIGHTER && currentWidthDp < 8f) {
                            12f
                        } else {
                            currentWidthDp
                        }

                        if (isErasingGesture) {
                            eraseSweepStarted = true
                            val eraserRadius = (effectiveWidth * 2.4f).coerceIn(10f, 24f)
                            val docPtDp = Offset(
                                preferredPointer.position.x / densityScale,
                                (preferredPointer.position.y + scrollState.value) / densityScale
                            )
                            eraserReticlePx = preferredPointer.position
                            currentOnEraseAt(docPtDp, eraserRadius, true)
                        } else {
                            activeWetTool = currentTool
                            activeWetColorArgb = currentColorArgb
                            activeWetWidthDp = effectiveWidth
                            val filter = StylusStrokeFilter(isStylus = isStylusOrEraser)
                            strokeFilter = filter
                            activeSamples.clear()
                            val firstSample = filter.processSample(
                                rawXDp = preferredPointer.position.x / densityScale,
                                rawYDp = (preferredPointer.position.y + scrollState.value) / densityScale,
                                rawPressure = preferredPointer.pressure,
                                timeMs = preferredPointer.uptimeMillis
                            )
                            activeSamples.add(firstSample)
                            wetRevision++
                        }
                        preferredPointer.consume()
                        continue
                    }

                    // Continue active stroke or erase sweep
                    val tracked = pressedChanges.firstOrNull { it.id == activePointerId } ?: preferredPointer
                    val scrollPx = scrollState.value.toFloat()

                    if (isErasingGesture) {
                        val eraserRadius = (currentWidthDp * 2.4f).coerceIn(10f, 24f)
                        eraserReticlePx = tracked.position
                        tracked.historical.forEach { hist ->
                            val histDocDp = Offset(
                                hist.position.x / densityScale,
                                (hist.position.y + scrollPx) / densityScale
                            )
                            currentOnEraseAt(histDocDp, eraserRadius, false)
                        }
                        val curDocDp = Offset(
                            tracked.position.x / densityScale,
                            (tracked.position.y + scrollPx) / densityScale
                        )
                        currentOnEraseAt(curDocDp, eraserRadius, false)
                    } else {
                        val filter = strokeFilter ?: StylusStrokeFilter(isStylus = isStylusOrEraser).also {
                            strokeFilter = it
                        }
                        var addedAny = false
                        tracked.historical.forEach { hist ->
                            val sample = filter.processSample(
                                rawXDp = hist.position.x / densityScale,
                                rawYDp = (hist.position.y + scrollPx) / densityScale,
                                rawPressure = tracked.pressure,
                                timeMs = hist.uptimeMillis
                            )
                            val last = activeSamples.lastOrNull()
                            if (last == null || hypot(sample.xDp - last.xDp, sample.yDp - last.yDp) >= 0.35f) {
                                activeSamples.add(sample)
                                addedAny = true
                            }
                        }

                        val currentSample = filter.processSample(
                            rawXDp = tracked.position.x / densityScale,
                            rawYDp = (tracked.position.y + scrollPx) / densityScale,
                            rawPressure = tracked.pressure,
                            timeMs = tracked.uptimeMillis
                        )
                        val prevLast = activeSamples.lastOrNull()
                        val moveDist = if (prevLast == null) 99f else hypot(
                            currentSample.xDp - prevLast.xDp,
                            currentSample.yDp - prevLast.yDp
                        )
                        if (moveDist >= 0.35f) {
                            if (didSnapStraight && moveDist > 3.5f) {
                                didSnapStraight = false
                            }
                            activeSamples.add(currentSample)
                            addedAny = true
                            scheduleStraightLineCheck()
                        } else if (addedAny) {
                            scheduleStraightLineCheck()
                        }

                        if (addedAny) {
                            wetRevision++
                        }
                    }

                    tracked.consume()
                }
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .then(inputModifier)
            .testTag("handwriting_overlay_layer")
    ) {
        if (isInkVisible || isDrawingMode) {
            Canvas(modifier = Modifier.matchParentSize()) {
                // Consume wetRevision so active wet ink redraws at full refresh rate
                @Suppress("UNUSED_EXPRESSION")
                wetRevision
                val scrollPx = scrollState.value.toFloat()

                translate(left = 0f, top = -scrollPx) {
                    // 1. Dry strokes (highlighters already ordered before pen strokes)
                    for (i in cachedDryStrokes.indices) {
                        val item = cachedDryStrokes[i]
                        drawInkStroke(
                            stroke = item.stroke,
                            isDark = isDark,
                            densityScale = densityScale,
                            cachedPath = item.path,
                            cachedPressureSegments = item.pressureSegments
                        )
                    }

                    // 2. Active wet stroke
                    if (activeSamples.isNotEmpty()) {
                        val wetStroke = InkStroke(
                            id = "wet_active",
                            tool = activeWetTool.name,
                            colorArgb = activeWetColorArgb,
                            baseWidthDp = activeWetWidthDp,
                            points = activeSamples
                        )
                        drawInkStroke(
                            stroke = wetStroke,
                            isDark = isDark,
                            densityScale = densityScale
                        )
                    }
                }

                // 3. Stylus hover nib indicator
                hoverPosPx?.let { hover ->
                    val nibColor = if (activeTool == NoteInkTool.ERASER) {
                        Color(0xFFEF4444)
                    } else {
                        resolveInkColor(activeColorArgb, isDark)
                    }
                    val radiusPx = (activeWidthDp * densityScale * 0.65f).coerceAtLeast(3.dp.toPx())
                    drawCircle(
                        color = nibColor.copy(alpha = 0.22f),
                        radius = radiusPx + 3.dp.toPx(),
                        center = hover
                    )
                    drawCircle(
                        color = nibColor,
                        radius = radiusPx,
                        center = hover,
                        style = Stroke(width = 1.4.dp.toPx())
                    )
                }

                // 4. Precision eraser reticle while dragging eraser
                eraserReticlePx?.let { pos ->
                    val radiusPx = (activeWidthDp * 2.4f).coerceIn(10f, 24f) * densityScale
                    drawCircle(
                        color = Color(0xFFEF4444).copy(alpha = 0.14f),
                        radius = radiusPx,
                        center = pos
                    )
                    drawCircle(
                        color = Color(0xFFEF4444).copy(alpha = 0.85f),
                        radius = radiusPx,
                        center = pos,
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun HandwritingOverlayToolbar(
    activeTool: NoteInkTool,
    activeColorArgb: Int,
    activeWidthDp: Float,
    stylusOnlyMode: Boolean,
    stylusDetected: Boolean,
    hasStrokes: Boolean,
    onSelectTool: (NoteInkTool) -> Unit,
    onSelectColorArgb: (Int) -> Unit,
    onSelectWidthDp: (Float) -> Unit,
    onToggleStylusOnly: () -> Unit,
    onClearAll: () -> Unit,
    onDone: () -> Unit,
    applyNavigationBarPadding: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showStyleTray by remember { mutableStateOf(false) }
    val isDark = isAppInDarkTheme
    val dockBg = if (isDark) Color(0xFF0F1518) else Color(0xFF18181A)
    val dockBorder = if (isDark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.08f)
    val accentColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFFD85C37)
    val onAccentColor = if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (applyNavigationBarPadding) Modifier.navigationBarsPadding() else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Expandable Tray for Ink Swatches, Nib Size Presets, and Palm Rejection Controls
        AnimatedVisibility(
            visible = showStyleTray,
            enter = fadeIn() + slideInVertically { it / 3 },
            exit = fadeOut() + slideOutVertically { it / 3 }
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = dockBg.copy(alpha = 0.96f),
                border = BorderStroke(0.8.dp, dockBorder),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Color Swatches Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NoteInkPalette.forEach { swatch ->
                            val isSelected = activeColorArgb == swatch.colorArgb
                            val displayColor = if (swatch.colorArgb == INK_ADAPTIVE_ARGB) {
                                Color(0xFFF8FAFC)
                            } else {
                                swatch.previewColor
                            }
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(displayColor)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) accentColor else Color.White.copy(alpha = 0.45f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onSelectColorArgb(swatch.colorArgb)
                                        if (activeTool == NoteInkTool.ERASER) {
                                            onSelectTool(NoteInkTool.PRESSURE_PEN)
                                        }
                                    }
                                    .testTag("ink_color_${swatch.name.lowercase()}")
                            )
                        }

                        if (hasStrokes) {
                            Spacer(Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.18f),
                                border = BorderStroke(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        showStyleTray = false
                                        onClearAll()
                                    }
                                    .testTag("ink_clear_all_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteSweep,
                                        contentDescription = "Clear All Ink",
                                        tint = Color(0xFFFCA5A5),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Clear",
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Nib Width Presets + Stylus Palm Rejection Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                1.5f to "Fine",
                                2.8f to "Med",
                                5.0f to "Bold",
                                12.0f to "Wide"
                            ).forEach { (wDp, label) ->
                                val isSelected = kotlin.math.abs(activeWidthDp - wDp) < 0.5f
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) accentColor else Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onSelectWidthDp(wDp) }
                                        .testTag("ink_width_${label.lowercase()}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size((wDp * 1.15f).coerceIn(4f, 12f).dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                        Text(
                                            text = label,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = if (stylusOnlyMode) {
                            "Palm Rejection Active: Stylus writes • Finger scrolls"
                        } else if (stylusDetected) {
                            "Stylus Detected • Hold stroke end to snap straight line"
                        } else {
                            "Hold stroke end for ruler-straight lines • Two fingers scroll"
                        },
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Primary Floating Ink Studio Dock
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = dockBg,
            border = BorderStroke(0.8.dp, dockBorder),
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .height(58.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color.Black.copy(alpha = 0.28f),
                    spotColor = Color.Black.copy(alpha = 0.28f)
                )
                .testTag("handwriting_toolbar_dock")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                InkToolIconBtn(
                    icon = Icons.Default.Edit,
                    label = "Fine Pen",
                    selected = activeTool == NoteInkTool.FINE_PEN,
                    accentColor = accentColor,
                    testTag = "ink_tool_fine_pen",
                    onClick = { onSelectTool(NoteInkTool.FINE_PEN) }
                )

                InkToolIconBtn(
                    icon = Icons.Default.Gesture,
                    label = "Stylus Calligraphy Pen",
                    selected = activeTool == NoteInkTool.PRESSURE_PEN,
                    accentColor = accentColor,
                    testTag = "ink_tool_pressure_pen",
                    onClick = { onSelectTool(NoteInkTool.PRESSURE_PEN) }
                )

                InkToolIconBtn(
                    icon = NotelandEraserIcon,
                    label = "Eraser",
                    selected = activeTool == NoteInkTool.ERASER,
                    accentColor = Color(0xFFEF4444),
                    testTag = "ink_tool_eraser",
                    onClick = { onSelectTool(NoteInkTool.ERASER) }
                )

                InkToolIconBtn(
                    icon = Icons.Default.Highlight,
                    label = "Highlighter",
                    selected = activeTool == NoteInkTool.HIGHLIGHTER,
                    accentColor = accentColor,
                    testTag = "ink_tool_highlighter",
                    onClick = { onSelectTool(NoteInkTool.HIGHLIGHTER) }
                )

                InkToolIconBtn(
                    icon = Icons.Default.DoNotTouch,
                    label = if (stylusOnlyMode) "Stylus Only (Palm Rejection On)" else "Stylus Only Mode",
                    selected = stylusOnlyMode,
                    accentColor = Color(0xFF38BDF8),
                    testTag = "ink_stylus_only_toggle",
                    onClick = onToggleStylusOnly
                )

                IconButton(
                    onClick = { showStyleTray = !showStyleTray },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(44.dp)
                        .testTag("ink_palette_tray_btn")
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (showStyleTray) accentColor else Color.White.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "Ink Colors & Width",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.width(4.dp))

                Surface(
                    shape = CircleShape,
                    color = accentColor,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .clickable(onClick = onDone)
                        .testTag("ink_done_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Done Drawing",
                            tint = onAccentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Done",
                            color = onAccentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InkToolIconBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(44.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (selected) accentColor else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}
