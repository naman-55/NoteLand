package com.example.ui.screens

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal const val STICKY_CARD_MIN_WIDTH = 156f
internal const val STICKY_CARD_MAX_WIDTH = 560f
internal const val STICKY_CARD_MIN_HEIGHT = 96f
internal const val STICKY_CARD_MAX_HEIGHT = 560f
internal const val STICKY_BADGE_DEFAULT_SIZE = 50f
internal const val STICKY_BADGE_MIN_SIZE = 36f
internal const val STICKY_BADGE_MAX_SIZE = 132f

enum class SelectionDragHandle {
    NONE,
    MOVE,
    RESIZE_BR,
    RESIZE_R,
    RESIZE_B,
    ENDPOINT_START,
    ENDPOINT_END,
    CURVE_CONTROL,
    ROTATE
}

internal fun PdfPageAnnotation.stickyWidth(): Float {
    return if (isStickyCard()) {
        if (end.x >= STICKY_CARD_MIN_WIDTH) {
            end.x.coerceIn(STICKY_CARD_MIN_WIDTH, STICKY_CARD_MAX_WIDTH)
        } else {
            STICKY_CARD_WIDTH
        }
    } else {
        if (end.x >= STICKY_BADGE_MIN_SIZE) {
            end.x.coerceIn(STICKY_BADGE_MIN_SIZE, STICKY_BADGE_MAX_SIZE)
        } else {
            STICKY_BADGE_DEFAULT_SIZE
        }
    }
}

internal fun PdfPageAnnotation.stickyHeight(): Float {
    return if (isStickyCard()) {
        val cardW = stickyWidth()
        if (end.y >= STICKY_CARD_MIN_HEIGHT) {
            end.y.coerceIn(STICKY_CARD_MIN_HEIGHT, STICKY_CARD_MAX_HEIGHT)
        } else {
            estimateStickyCardHeightForWidth(cardW)
        }
    } else {
        stickyWidth()
    }
}

internal fun PdfPageAnnotation.estimateStickyCardHeightForWidth(cardWidth: Float): Float {
    val maxChars = maxCharsForStickyWidth(cardWidth)
    val lines = wrapStickyTextLines(content, maxCharsPerLine = maxChars, maxLines = 10)
    return (54f + lines.size * 27f + 22f).coerceIn(116f, 340f)
}

internal fun maxCharsForStickyWidth(cardWidth: Float): Int {
    return ((cardWidth - 32f) / 11.2f).toInt().coerceIn(9, 48)
}

internal fun maxLinesForStickyHeight(cardHeight: Float): Int {
    return ((cardHeight - 66f) / 27f).toInt().coerceIn(2, 18)
}

internal fun PdfPageAnnotation.isArrowOrCurve(): Boolean {
    return type in setOf(
        "SHAPE_ARROW",
        "SHAPE_CURVE_ARROW",
        "SHAPE_S_CURVE_ARROW",
        "SHAPE_DOUBLE_ARROW",
        "SHAPE_ELBOW_ARROW"
    )
}

internal fun PdfPageAnnotation.hasEditableText(): Boolean {
    return type == "COMMENT" || type == "TEXT_BOX"
}

internal fun PdfPageAnnotation.selectionDisplayLabel(): String = when (type) {
    "COMMENT" -> if (isStickyCard()) "Sticky Card" else "Sticky Pin"
    "HIGHLIGHT" -> "Highlight"
    "DRAWING" -> "Pen Stroke"
    "TEXT_BOX" -> "Text Box"
    "UNDERLINE" -> "Underline"
    "STRIKETHROUGH" -> "Strike"
    "SHAPE_RECT" -> "Rectangle"
    "SHAPE_CIRCLE" -> "Circle"
    "SHAPE_STAR" -> "Star"
    "SHAPE_ARROW" -> "Arrow"
    "SHAPE_CURVE_ARROW" -> "Curve Arrow"
    "SHAPE_S_CURVE_ARROW" -> "S-Curve"
    "SHAPE_DOUBLE_ARROW" -> "Double Arrow"
    "SHAPE_ELBOW_ARROW" -> "Elbow Arrow"
    else -> "Annotation"
}

internal fun defaultCurveControlPoint(start: Offset, end: Offset): Offset {
    val midX = (start.x + end.x) * 0.5f
    val midY = (start.y + end.y) * 0.5f
    val dx = end.x - start.x
    val dy = end.y - start.y
    val len = hypot(dx, dy).coerceAtLeast(1f)
    // Perpendicular unit vector
    var nx = -dy / len
    var ny = dx / len
    // Prefer an intuitive upward/leftward arch by default
    if (ny > 0f) {
        nx = -nx
        ny = -ny
    }
    val arcOffset = (len * 0.28f).coerceIn(32f, 220f)
    return Offset(midX + nx * arcOffset, midY + ny * arcOffset)
}

internal fun resolveCurveControlPoint(start: Offset, end: Offset, points: List<Offset>): Offset {
    return points.firstOrNull() ?: defaultCurveControlPoint(start, end)
}

internal fun resolveSCurveControlPoints(
    start: Offset,
    end: Offset,
    points: List<Offset>
): Pair<Offset, Offset> {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val midX = (start.x + end.x) * 0.5f
    val midY = (start.y + end.y) * 0.5f
    val handle = points.firstOrNull()
    val horizontalDominant = abs(dx) >= abs(dy)
    return if (horizontalDominant) {
        val tensionShift = if (handle != null) (handle.x - midX) else 0f
        val yBias = if (handle != null) (handle.y - midY) * 0.45f else 0f
        val c1 = Offset(midX + tensionShift * 0.5f, start.y + yBias)
        val c2 = Offset(midX - tensionShift * 0.5f, end.y - yBias)
        c1 to c2
    } else {
        val tensionShift = if (handle != null) (handle.y - midY) else 0f
        val xBias = if (handle != null) (handle.x - midX) * 0.45f else 0f
        val c1 = Offset(start.x + xBias, midY + tensionShift * 0.5f)
        val c2 = Offset(end.x - xBias, midY - tensionShift * 0.5f)
        c1 to c2
    }
}

internal fun resolveElbowCornerPoint(
    start: Offset,
    end: Offset,
    points: List<Offset>
): Offset {
    val handle = points.firstOrNull()
    if (handle != null) {
        val dTop = hypot(handle.x - end.x, handle.y - start.y)
        val dBottom = hypot(handle.x - start.x, handle.y - end.y)
        return if (dTop <= dBottom) Offset(end.x, start.y) else Offset(start.x, end.y)
    }
    return if (abs(end.x - start.x) >= abs(end.y - start.y)) {
        Offset(end.x, start.y)
    } else {
        Offset(start.x, end.y)
    }
}

internal fun DrawScope.drawArrowhead(
    tip: Offset,
    from: Offset,
    color: Color,
    strokeWidth: Float
) {
    val dx = tip.x - from.x
    val dy = tip.y - from.y
    if (abs(dx) < 0.01f && abs(dy) < 0.01f) return
    val angle = atan2(dy, dx)
    val arrowLen = max(26f, strokeWidth * 4.4f)
    val wingAngle = Math.PI / 6.2

    val p1 = Offset(
        (tip.x - arrowLen * cos(angle - wingAngle)).toFloat(),
        (tip.y - arrowLen * sin(angle - wingAngle)).toFloat()
    )
    val p2 = Offset(
        (tip.x - arrowLen * cos(angle + wingAngle)).toFloat(),
        (tip.y - arrowLen * sin(angle + wingAngle)).toFloat()
    )

    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        close()
    }
    drawPath(path = path, color = color, style = Fill)
}

internal fun DrawScope.drawCurveArrow(
    start: Offset,
    end: Offset,
    control: Offset,
    color: Color,
    strokeWidth: Float
) {
    val path = Path().apply {
        moveTo(start.x, start.y)
        quadraticTo(control.x, control.y, end.x, end.y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    val tangentFrom = if (hypot(end.x - control.x, end.y - control.y) > 4f) control else start
    drawArrowhead(tip = end, from = tangentFrom, color = color, strokeWidth = strokeWidth)
}

internal fun DrawScope.drawSCurveArrow(
    start: Offset,
    end: Offset,
    points: List<Offset>,
    color: Color,
    strokeWidth: Float
) {
    val (c1, c2) = resolveSCurveControlPoints(start, end, points)
    val path = Path().apply {
        moveTo(start.x, start.y)
        cubicTo(c1.x, c1.y, c2.x, c2.y, end.x, end.y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    val tangentFrom = if (hypot(end.x - c2.x, end.y - c2.y) > 4f) c2 else start
    drawArrowhead(tip = end, from = tangentFrom, color = color, strokeWidth = strokeWidth)
}

internal fun DrawScope.drawDoubleArrow(
    start: Offset,
    end: Offset,
    points: List<Offset>,
    color: Color,
    strokeWidth: Float
) {
    val ctrl = points.firstOrNull()
    if (ctrl != null) {
        val path = Path().apply {
            moveTo(start.x, start.y)
            quadraticTo(ctrl.x, ctrl.y, end.x, end.y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawArrowhead(tip = end, from = ctrl, color = color, strokeWidth = strokeWidth)
        drawArrowhead(tip = start, from = ctrl, color = color, strokeWidth = strokeWidth)
    } else {
        drawLine(color = color, start = start, end = end, strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawArrowhead(tip = end, from = start, color = color, strokeWidth = strokeWidth)
        drawArrowhead(tip = start, from = end, color = color, strokeWidth = strokeWidth)
    }
}

internal fun DrawScope.drawElbowArrow(
    start: Offset,
    end: Offset,
    points: List<Offset>,
    color: Color,
    strokeWidth: Float
) {
    val corner = resolveElbowCornerPoint(start, end, points)
    val seg1Len = hypot(corner.x - start.x, corner.y - start.y)
    val seg2Len = hypot(end.x - corner.x, end.y - corner.y)
    val cornerR = min(24f, min(seg1Len, seg2Len) * 0.45f)

    val path = Path().apply {
        moveTo(start.x, start.y)
        if (cornerR > 2f && seg1Len > 1f && seg2Len > 1f) {
            val u1x = (corner.x - start.x) / seg1Len
            val u1y = (corner.y - start.y) / seg1Len
            val u2x = (end.x - corner.x) / seg2Len
            val u2y = (end.y - corner.y) / seg2Len
            val preCorner = Offset(corner.x - u1x * cornerR, corner.y - u1y * cornerR)
            val postCorner = Offset(corner.x + u2x * cornerR, corner.y + u2y * cornerR)
            lineTo(preCorner.x, preCorner.y)
            quadraticTo(corner.x, corner.y, postCorner.x, postCorner.y)
            lineTo(end.x, end.y)
        } else {
            lineTo(corner.x, corner.y)
            lineTo(end.x, end.y)
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    val tangentFrom = if (seg2Len > 4f) corner else start
    drawArrowhead(tip = end, from = tangentFrom, color = color, strokeWidth = strokeWidth)
}

internal fun evalQuadraticBezier(p0: Offset, p1: Offset, p2: Offset, t: Float): Offset {
    val u = 1f - t
    return Offset(
        u * u * p0.x + 2f * u * t * p1.x + t * t * p2.x,
        u * u * p0.y + 2f * u * t * p1.y + t * t * p2.y
    )
}

internal fun evalCubicBezier(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
    val u = 1f - t
    val tt = t * t
    val uu = u * u
    val uuu = uu * u
    val ttt = tt * t
    return Offset(
        uuu * p0.x + 3f * uu * t * p1.x + 3f * u * tt * p2.x + ttt * p3.x,
        uuu * p0.y + 3f * uu * t * p1.y + 3f * u * tt * p2.y + ttt * p3.y
    )
}

internal fun distancePointToSegment(p: Offset, v: Offset, w: Offset): Float {
    val l2 = (v.x - w.x) * (v.x - w.x) + (v.y - w.y) * (v.y - w.y)
    if (l2 == 0f) return hypot(p.x - v.x, p.y - v.y)
    val t = max(0f, min(1f, ((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2))
    val proj = Offset(v.x + t * (w.x - v.x), v.y + t * (w.y - v.y))
    return hypot(p.x - proj.x, p.y - proj.y)
}

internal fun distanceToQuadraticCurve(
    point: Offset,
    start: Offset,
    control: Offset,
    end: Offset,
    steps: Int = 18
): Float {
    var minDist = Float.MAX_VALUE
    var prev = start
    for (i in 1..steps) {
        val curr = evalQuadraticBezier(start, control, end, i.toFloat() / steps)
        minDist = min(minDist, distancePointToSegment(point, prev, curr))
        prev = curr
    }
    return minDist
}

internal fun distanceToCubicCurve(
    point: Offset,
    start: Offset,
    c1: Offset,
    c2: Offset,
    end: Offset,
    steps: Int = 20
): Float {
    var minDist = Float.MAX_VALUE
    var prev = start
    for (i in 1..steps) {
        val curr = evalCubicBezier(start, c1, c2, end, i.toFloat() / steps)
        minDist = min(minDist, distancePointToSegment(point, prev, curr))
        prev = curr
    }
    return minDist
}

internal fun PdfPageAnnotation.boundingBox(): PdfHighlightSegment {
    return when (type) {
        "COMMENT" -> PdfHighlightSegment(
            left = start.x,
            top = start.y,
            right = start.x + stickyWidth(),
            bottom = start.y + stickyHeight()
        )
        "TEXT_BOX" -> {
            val fontH = strokeWidth.coerceAtLeast(24f)
            val estimatedWidth = max(content.length * (fontH * 0.56f), 110f)
            PdfHighlightSegment(
                left = start.x,
                top = start.y - fontH,
                right = start.x + estimatedWidth,
                bottom = start.y + 8f
            )
        }
        "STAMP" -> {
            val textLen = max(content.length * 20f, 160f)
            PdfHighlightSegment(
                left = start.x,
                top = start.y,
                right = start.x + textLen + 40f,
                bottom = start.y + 60f
            )
        }
        "DRAWING" -> {
            if (points.isEmpty()) {
                PdfHighlightSegment(start.x, start.y, start.x + 20f, start.y + 20f)
            } else {
                val pad = max(8f, strokeWidth)
                val minX = points.minOf { it.x } - pad
                val maxX = points.maxOf { it.x } + pad
                val minY = points.minOf { it.y } - pad
                val maxY = points.maxOf { it.y } + pad
                PdfHighlightSegment(minX, minY, maxX, maxY)
            }
        }
        "HIGHLIGHT" -> {
            if (segments.isNotEmpty()) {
                PdfHighlightSegment(
                    left = segments.minOf { min(it.left, it.right) },
                    top = segments.minOf { min(it.top, it.bottom) },
                    right = segments.maxOf { max(it.left, it.right) },
                    bottom = segments.maxOf { max(it.top, it.bottom) }
                )
            } else {
                PdfHighlightSegment(
                    left = min(start.x, end.x),
                    top = min(start.y, end.y),
                    right = max(start.x, end.x),
                    bottom = max(start.y, end.y)
                )
            }
        }
        else -> {
            val allPts = buildList {
                add(start)
                add(end)
                addAll(points)
            }
            val pad = if (type == "UNDERLINE" || type == "STRIKETHROUGH") 10f else 0f
            PdfHighlightSegment(
                left = allPts.minOf { it.x } - pad,
                top = allPts.minOf { it.y } - pad,
                right = allPts.maxOf { it.x } + pad,
                bottom = allPts.maxOf { it.y } + pad
            )
        }
    }
}

internal fun PdfPageAnnotation.translatedBy(dx: Float, dy: Float): PdfPageAnnotation {
    val delta = Offset(dx, dy)
    val usesEndAsCoordinate = type != "COMMENT" && type != "TEXT_BOX" && type != "STAMP"
    return copy(
        start = start + delta,
        end = if (usesEndAsCoordinate) end + delta else end,
        points = points.map { it + delta },
        segments = segments.map {
            it.copy(
                left = it.left + dx,
                top = it.top + dy,
                right = it.right + dx,
                bottom = it.bottom + dy
            )
        }
    )
}

internal fun PdfPageAnnotation.rotationPivot(): Offset {
    val bbox = boundingBox()
    return Offset(
        (bbox.left + bbox.right) * 0.5f,
        (bbox.top + bbox.bottom) * 0.5f
    )
}

internal fun PdfPageAnnotation.rotateHandlePosition(zoomScale: Float): Offset {
    val inv = 1f / zoomScale.coerceAtLeast(0.4f)
    val pad = (6f * inv).coerceIn(4f, 14f)
    val stemLen = (48f * inv).coerceIn(34f, 88f)
    val bbox = boundingBox()
    val centerX = (bbox.left + bbox.right) * 0.5f
    return Offset(centerX, bbox.top - pad - stemLen)
}

internal fun PdfPageAnnotation.rotatedByDelta(deltaDeg: Float): PdfPageAnnotation {
    return copy(rotationDeg = normalizeAngleDeg(rotationDeg + deltaDeg))
}

internal fun PdfPageAnnotation.flippedCurve(): PdfPageAnnotation {
    if (type != "SHAPE_CURVE_ARROW" && type != "SHAPE_S_CURVE_ARROW" &&
        type != "SHAPE_DOUBLE_ARROW" && type != "SHAPE_ELBOW_ARROW"
    ) {
        return this
    }
    val mid = Offset((start.x + end.x) * 0.5f, (start.y + end.y) * 0.5f)
    val currentCtrl = resolveCurveControlPoint(start, end, points)
    val reflected = Offset(2f * mid.x - currentCtrl.x, 2f * mid.y - currentCtrl.y)
    return copy(points = listOf(reflected))
}

internal fun PdfPageAnnotation.resizedFromHandle(
    handle: SelectionDragHandle,
    targetPoint: Offset
): PdfPageAnnotation {
    val oldPivot = rotationPivot()
    val oldBbox = boundingBox()
    val localTarget = if (rotationDeg != 0f) {
        targetPoint.rotatedAround(oldPivot, -rotationDeg)
    } else {
        targetPoint
    }

    val updated = when (handle) {
        SelectionDragHandle.CURVE_CONTROL -> {
            if (isArrowOrCurve()) copy(points = listOf(localTarget)) else this
        }
        SelectionDragHandle.ENDPOINT_START -> {
            if (isArrowOrCurve()) copy(start = localTarget) else this
        }
        SelectionDragHandle.ENDPOINT_END -> {
            if (isArrowOrCurve()) copy(end = localTarget) else this
        }
        SelectionDragHandle.RESIZE_BR,
        SelectionDragHandle.RESIZE_R,
        SelectionDragHandle.RESIZE_B -> {
            when (type) {
                "COMMENT" -> {
                    if (isStickyCard()) {
                        val curW = stickyWidth()
                        val curH = stickyHeight()
                        val newW = if (handle == SelectionDragHandle.RESIZE_B) {
                            curW
                        } else {
                            (localTarget.x - start.x).coerceIn(STICKY_CARD_MIN_WIDTH, STICKY_CARD_MAX_WIDTH)
                        }
                        val newH = if (handle == SelectionDragHandle.RESIZE_R) {
                            curH
                        } else {
                            (localTarget.y - start.y).coerceIn(STICKY_CARD_MIN_HEIGHT, STICKY_CARD_MAX_HEIGHT)
                        }
                        copy(end = Offset(newW, newH))
                    } else {
                        val side = max(localTarget.x - start.x, localTarget.y - start.y)
                            .coerceIn(STICKY_BADGE_MIN_SIZE, STICKY_BADGE_MAX_SIZE)
                        copy(end = Offset(side, side))
                    }
                }
                "TEXT_BOX" -> {
                    val desiredW = (localTarget.x - oldBbox.left).coerceAtLeast(60f)
                    val curW = oldBbox.width.coerceAtLeast(60f)
                    val ratio = desiredW / curW
                    val newSize = (strokeWidth.coerceAtLeast(24f) * ratio).coerceIn(18f, 96f)
                    copy(strokeWidth = newSize)
                }
                "DRAWING" -> {
                    if (points.size < 2) return this
                    val anchor = Offset(oldBbox.left, oldBbox.top)
                    val curW = oldBbox.width.coerceAtLeast(24f)
                    val curH = oldBbox.height.coerceAtLeast(24f)
                    val scaleX = ((localTarget.x - anchor.x).coerceAtLeast(24f)) / curW
                    val scaleY = ((localTarget.y - anchor.y).coerceAtLeast(24f)) / curH
                    copy(
                        points = points.map { pt ->
                            Offset(
                                anchor.x + (pt.x - anchor.x) * scaleX,
                                anchor.y + (pt.y - anchor.y) * scaleY
                            )
                        }
                    )
                }
                "HIGHLIGHT" -> {
                    val anchor = Offset(oldBbox.left, oldBbox.top)
                    val curW = oldBbox.width.coerceAtLeast(24f)
                    val curH = oldBbox.height.coerceAtLeast(20f)
                    val scaleX = ((localTarget.x - anchor.x).coerceAtLeast(24f)) / curW
                    val scaleY = ((localTarget.y - anchor.y).coerceAtLeast(20f)) / curH
                    val scaledSegs = segments.map { seg ->
                        PdfHighlightSegment(
                            left = anchor.x + (seg.left - anchor.x) * scaleX,
                            top = anchor.y + (seg.top - anchor.y) * scaleY,
                            right = anchor.x + (seg.right - anchor.x) * scaleX,
                            bottom = anchor.y + (seg.bottom - anchor.y) * scaleY
                        )
                    }
                    copy(
                        start = Offset(
                            anchor.x + (start.x - anchor.x) * scaleX,
                            anchor.y + (start.y - anchor.y) * scaleY
                        ),
                        end = Offset(
                            anchor.x + (end.x - anchor.x) * scaleX,
                            anchor.y + (end.y - anchor.y) * scaleY
                        ),
                        segments = scaledSegs
                    )
                }
                "STAMP" -> this
                else -> {
                    val minX = min(start.x, end.x)
                    val minY = min(start.y, end.y)
                    val newRight = max(minX + 24f, localTarget.x)
                    val newBottom = max(minY + 20f, localTarget.y)
                    val newStart = Offset(
                        if (start.x <= end.x) minX else newRight,
                        if (start.y <= end.y) minY else newBottom
                    )
                    val newEnd = Offset(
                        if (start.x <= end.x) newRight else minX,
                        if (start.y <= end.y) newBottom else minY
                    )
                    copy(start = newStart, end = newEnd)
                }
            }
        }
        else -> this
    }

    if (rotationDeg == 0f || updated === this) return updated

    // Keep opposite anchor point fixed in world space as the bbox center shifts
    val (oldAnchor, newAnchor) = when (handle) {
        SelectionDragHandle.ENDPOINT_START -> end to updated.end
        SelectionDragHandle.ENDPOINT_END,
        SelectionDragHandle.CURVE_CONTROL -> start to updated.start
        else -> {
            val oa = if (type == "TEXT_BOX") start else Offset(oldBbox.left, oldBbox.top)
            val newBbox = updated.boundingBox()
            val na = if (type == "TEXT_BOX") updated.start else Offset(newBbox.left, newBbox.top)
            oa to na
        }
    }
    val newPivot = updated.rotationPivot()
    val oldWorldAnchor = oldAnchor.rotatedAround(oldPivot, rotationDeg)
    val newWorldAnchor = newAnchor.rotatedAround(newPivot, rotationDeg)
    val drift = oldWorldAnchor - newWorldAnchor
    return updated.translatedBy(drift.x, drift.y)
}

internal fun hitTestSelectionHandle(
    item: PdfPageAnnotation,
    point: Offset,
    zoomScale: Float
): SelectionDragHandle {
    val inv = 1f / zoomScale.coerceAtLeast(0.4f)
    val hitRadius = (36f * inv).coerceIn(28f, 64f)
    val rotHitRadius = (46f * inv).coerceIn(34f, 76f)
    val pad = (6f * inv).coerceIn(4f, 14f)
    val localPoint = if (item.rotationDeg != 0f) {
        point.rotatedAround(item.rotationPivot(), -item.rotationDeg)
    } else {
        point
    }

    val rotPos = item.rotateHandlePosition(zoomScale)
    if (hypot(localPoint.x - rotPos.x, localPoint.y - rotPos.y) <= rotHitRadius) {
        return SelectionDragHandle.ROTATE
    }

    if (item.isArrowOrCurve()) {
        if (item.type == "SHAPE_CURVE_ARROW" || item.type == "SHAPE_S_CURVE_ARROW" ||
            item.type == "SHAPE_DOUBLE_ARROW" || item.type == "SHAPE_ELBOW_ARROW"
        ) {
            val ctrl = resolveCurveControlPoint(item.start, item.end, item.points)
            if (hypot(localPoint.x - ctrl.x, localPoint.y - ctrl.y) <= hitRadius) {
                return SelectionDragHandle.CURVE_CONTROL
            }
        }
        if (hypot(localPoint.x - item.end.x, localPoint.y - item.end.y) <= hitRadius) {
            return SelectionDragHandle.ENDPOINT_END
        }
        if (hypot(localPoint.x - item.start.x, localPoint.y - item.start.y) <= hitRadius) {
            return SelectionDragHandle.ENDPOINT_START
        }
    } else if (item.type != "STAMP") {
        val bbox = item.boundingBox()
        val brVisual = Offset(bbox.right + pad, bbox.bottom + pad)
        val brCorner = Offset(bbox.right, bbox.bottom)
        if (hypot(localPoint.x - brVisual.x, localPoint.y - brVisual.y) <= hitRadius ||
            hypot(localPoint.x - brCorner.x, localPoint.y - brCorner.y) <= hitRadius
        ) {
            return SelectionDragHandle.RESIZE_BR
        }
        if (item.type == "COMMENT" && item.isStickyCard()) {
            val rightVisual = Offset(bbox.right + pad, bbox.centerY)
            val rightCorner = Offset(bbox.right, bbox.centerY)
            if (hypot(localPoint.x - rightVisual.x, localPoint.y - rightVisual.y) <= hitRadius ||
                hypot(localPoint.x - rightCorner.x, localPoint.y - rightCorner.y) <= hitRadius
            ) {
                return SelectionDragHandle.RESIZE_R
            }
            val bottomVisual = Offset((bbox.left + bbox.right) * 0.5f, bbox.bottom + pad)
            val bottomCorner = Offset((bbox.left + bbox.right) * 0.5f, bbox.bottom)
            if (hypot(localPoint.x - bottomVisual.x, localPoint.y - bottomVisual.y) <= hitRadius ||
                hypot(localPoint.x - bottomCorner.x, localPoint.y - bottomCorner.y) <= hitRadius
            ) {
                return SelectionDragHandle.RESIZE_B
            }
        }
    }

    val bbox = item.boundingBox()
    val bboxTolerance = (16f * inv).coerceIn(10f, 30f)
    if (item.containsLocalPoint(localPoint, tolerance = bboxTolerance) || bbox.contains(localPoint, bboxTolerance * 0.5f)) {
        return SelectionDragHandle.MOVE
    }
    return SelectionDragHandle.NONE
}

internal fun DrawScope.drawSelectionOverlay(
    item: PdfPageAnnotation,
    zoomScale: Float,
    accentColor: Color
) {
    if (item.rotationDeg != 0f) {
        rotate(degrees = item.rotationDeg, pivot = item.rotationPivot()) {
            drawUnrotatedSelectionOverlay(item, zoomScale, accentColor)
        }
    } else {
        drawUnrotatedSelectionOverlay(item, zoomScale, accentColor)
    }
}

private fun DrawScope.drawUnrotatedSelectionOverlay(
    item: PdfPageAnnotation,
    zoomScale: Float,
    accentColor: Color
) {
    val inv = 1f / zoomScale.coerceAtLeast(0.4f)
    val dashLen = (10f * inv).coerceIn(6f, 22f)
    val borderW = (2.2f * inv).coerceIn(1.5f, 4.5f)
    val handleR = (10f * inv).coerceIn(7.5f, 18f)
    val rotKnobR = handleR * 1.35f
    val pad = (6f * inv).coerceIn(4f, 14f)
    val bbox = item.boundingBox()
    val topCenterX = (bbox.left + bbox.right) * 0.5f
    val centerPivot = item.rotationPivot()
    val rotPos = item.rotateHandlePosition(zoomScale)
    val rotateHandleColor = Color(0xFF0284C7)

    if (abs(item.rotationDeg) >= 0.5f) {
        drawCircle(
            color = rotateHandleColor.copy(alpha = 0.25f),
            radius = handleR * 0.85f,
            center = centerPivot
        )
        drawCircle(
            color = rotateHandleColor,
            radius = handleR * 0.35f,
            center = centerPivot
        )
    }

    if (item.isArrowOrCurve()) {
        val stemBase = Offset(topCenterX, bbox.top - pad * 0.5f)
        drawLine(
            color = rotateHandleColor.copy(alpha = 0.78f),
            start = stemBase,
            end = rotPos,
            strokeWidth = borderW,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLen * 0.6f, dashLen * 0.45f))
        )
        drawRotateHandleKnob(center = rotPos, radius = rotKnobR, fill = rotateHandleColor, borderWidth = borderW)

        val showCurveControl = item.type == "SHAPE_CURVE_ARROW" ||
            item.type == "SHAPE_S_CURVE_ARROW" ||
            item.type == "SHAPE_DOUBLE_ARROW" ||
            item.type == "SHAPE_ELBOW_ARROW"
        if (showCurveControl) {
            val ctrl = resolveCurveControlPoint(item.start, item.end, item.points)
            drawLine(
                color = accentColor.copy(alpha = 0.45f),
                start = item.start,
                end = ctrl,
                strokeWidth = borderW * 0.8f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLen, dashLen))
            )
            drawLine(
                color = accentColor.copy(alpha = 0.45f),
                start = ctrl,
                end = item.end,
                strokeWidth = borderW * 0.8f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLen, dashLen))
            )
            drawHandleKnob(center = ctrl, radius = handleR * 1.05f, fill = Color(0xFFF59E0B), border = Color.White, borderWidth = borderW)
        }
        drawHandleKnob(center = item.start, radius = handleR, fill = accentColor, border = Color.White, borderWidth = borderW)
        drawHandleKnob(center = item.end, radius = handleR, fill = accentColor, border = Color.White, borderWidth = borderW)
        return
    }

    val topLeft = Offset(bbox.left - pad, bbox.top - pad)
    val size = Size(bbox.width + pad * 2f, bbox.height + pad * 2f)

    drawRoundRect(
        color = accentColor.copy(alpha = 0.07f),
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(10f * inv, 10f * inv)
    )
    drawRoundRect(
        color = accentColor.copy(alpha = 0.90f),
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(10f * inv, 10f * inv),
        style = Stroke(
            width = borderW,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLen, dashLen * 0.65f))
        )
    )

    // Top-center rotation stem & knob
    drawLine(
        color = rotateHandleColor.copy(alpha = 0.88f),
        start = Offset(topCenterX, bbox.top - pad),
        end = rotPos,
        strokeWidth = borderW
    )
    drawRotateHandleKnob(center = rotPos, radius = rotKnobR, fill = rotateHandleColor, borderWidth = borderW)

    // Bottom-right corner resize grip (for sticky notes, shapes, strokes, text)
    if (item.type != "STAMP") {
        val br = Offset(bbox.right + pad, bbox.bottom + pad)
        drawHandleKnob(center = br, radius = handleR * 1.15f, fill = accentColor, border = Color.White, borderWidth = borderW)
        // Edge width/height pills for sticky card resize
        if (item.type == "COMMENT" && item.isStickyCard()) {
            val rightEdge = Offset(bbox.right + pad, (bbox.top + bbox.bottom) * 0.5f)
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(rightEdge.x - handleR * 0.45f, rightEdge.y - handleR * 1.3f),
                size = Size(handleR * 0.9f, handleR * 2.6f),
                cornerRadius = CornerRadius(handleR, handleR)
            )
            val bottomEdge = Offset((bbox.left + bbox.right) * 0.5f, bbox.bottom + pad)
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(bottomEdge.x - handleR * 1.3f, bottomEdge.y - handleR * 0.45f),
                size = Size(handleR * 2.6f, handleR * 0.9f),
                cornerRadius = CornerRadius(handleR, handleR)
            )
        }
    }
}

private fun DrawScope.drawHandleKnob(
    center: Offset,
    radius: Float,
    fill: Color,
    border: Color,
    borderWidth: Float
) {
    drawCircle(
        color = Color.Black.copy(alpha = 0.20f),
        radius = radius + borderWidth * 0.6f,
        center = Offset(center.x + 1.5f, center.y + 2f)
    )
    drawCircle(color = border, radius = radius, center = center)
    drawCircle(color = fill, radius = (radius - borderWidth).coerceAtLeast(radius * 0.65f), center = center)
}

private fun DrawScope.drawRotateHandleKnob(
    center: Offset,
    radius: Float,
    fill: Color,
    borderWidth: Float
) {
    drawCircle(
        color = fill.copy(alpha = 0.22f),
        radius = radius * 1.42f,
        center = center
    )
    drawHandleKnob(center = center, radius = radius, fill = fill, border = Color.White, borderWidth = borderWidth)
    val arcR = radius * 0.46f
    drawArc(
        color = Color.White,
        startAngle = -35f,
        sweepAngle = 255f,
        useCenter = false,
        topLeft = Offset(center.x - arcR, center.y - arcR),
        size = Size(arcR * 2f, arcR * 2f),
        style = Stroke(width = (borderWidth * 0.85f).coerceAtLeast(1.6f), cap = StrokeCap.Round)
    )
    val tipRad = -35.0 * (Math.PI / 180.0)
    val tip = Offset(
        center.x + (arcR * cos(tipRad)).toFloat(),
        center.y + (arcR * sin(tipRad)).toFloat()
    )
    drawCircle(color = Color.White, radius = (borderWidth * 0.9f).coerceAtLeast(1.8f), center = tip)
}
