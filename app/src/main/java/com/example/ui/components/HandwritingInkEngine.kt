package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import com.example.util.InkSample
import com.example.util.InkStroke
import com.example.util.NoteInkTool
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

const val INK_ADAPTIVE_ARGB = 0

data class InkSwatchOption(
    val name: String,
    val colorArgb: Int,
    val previewColor: Color
)

val NoteInkPalette = listOf(
    InkSwatchOption("Ink", INK_ADAPTIVE_ARGB, Color(0xFF1E293B)),
    InkSwatchOption("Terracotta", Color(0xFFD85C37).toArgb(), Color(0xFFD85C37)),
    InkSwatchOption("Cobalt", Color(0xFF2563EB).toArgb(), Color(0xFF2563EB)),
    InkSwatchOption("Emerald", Color(0xFF059669).toArgb(), Color(0xFF059669)),
    InkSwatchOption("Crimson", Color(0xFFDC2626).toArgb(), Color(0xFFDC2626)),
    InkSwatchOption("Amber", Color(0xFFD97706).toArgb(), Color(0xFFD97706)),
    InkSwatchOption("Violet", Color(0xFF7C3AED).toArgb(), Color(0xFF7C3AED))
)

fun resolveInkColor(colorArgb: Int, isDark: Boolean): Color {
    return if (colorArgb == INK_ADAPTIVE_ARGB) {
        if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    } else {
        Color(colorArgb)
    }
}

/**
 * Speed-adaptive 1-Euro low-pass filter (Casiez et al.) tuned for stylus & handwriting accuracy.
 * Eliminates sub-pixel digitizer tremor during slow writing while keeping cutoff high during
 * rapid strokes and sharp corners so letters never feel sluggish or rounded-out.
 */
class OneEuroFilter1D(
    private val minCutoffHz: Float,
    private val beta: Float,
    private val dCutoffHz: Float = 1.0f
) {
    private var hasPrev = false
    private var xPrev = 0f
    private var dxPrev = 0f

    fun reset() {
        hasPrev = false
        xPrev = 0f
        dxPrev = 0f
    }

    fun filter(value: Float, dtSeconds: Float, preserveSharpTurn: Boolean = false): Float {
        if (!hasPrev || dtSeconds <= 0f) {
            hasPrev = true
            xPrev = value
            dxPrev = 0f
            return value
        }
        val clampedDt = dtSeconds.coerceIn(0.001f, 0.1f)
        val dx = (value - xPrev) / clampedDt
        val aD = smoothingFactor(clampedDt, dCutoffHz)
        val dxHat = aD * dx + (1f - aD) * dxPrev
        val cutoff = (minCutoffHz + beta * abs(dxHat)) * (if (preserveSharpTurn) 3.5f else 1f)
        val a = smoothingFactor(clampedDt, cutoff)
        val xHat = a * value + (1f - a) * xPrev
        xPrev = xHat
        dxPrev = dxHat
        return xHat
    }

    private fun smoothingFactor(dt: Float, cutoffHz: Float): Float {
        val r = 2f * PI.toFloat() * cutoffHz * dt
        return r / (r + 1f)
    }
}

class StylusStrokeFilter(
    private val isStylus: Boolean
) {
    // Stylus hardware digitizers are cleaner than capacitive finger touch, so use higher minCutoff
    private val xFilter = OneEuroFilter1D(
        minCutoffHz = if (isStylus) 3.4f else 2.2f,
        beta = if (isStylus) 0.16f else 0.10f
    )
    private val yFilter = OneEuroFilter1D(
        minCutoffHz = if (isStylus) 3.4f else 2.2f,
        beta = if (isStylus) 0.16f else 0.10f
    )
    private var lastTimeMs = -1L
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var prevSegDx = 0f
    private var prevSegDy = 0f
    private var hasPrevSeg = false
    private var smoothedPressure = 0.5f
    private var hasPressure = false

    fun reset() {
        xFilter.reset()
        yFilter.reset()
        lastTimeMs = -1L
        hasPrevSeg = false
        hasPressure = false
        smoothedPressure = 0.5f
    }

    fun processSample(
        rawXDp: Float,
        rawYDp: Float,
        rawPressure: Float,
        timeMs: Long
    ): InkSample {
        val dtSec = if (lastTimeMs < 0L) 0f else ((timeMs - lastTimeMs).coerceAtLeast(1L) / 1000f)
        var sharpCorner = false
        if (lastTimeMs >= 0L) {
            val dx = rawXDp - lastRawX
            val dy = rawYDp - lastRawY
            val len = hypot(dx, dy)
            if (len > 0.35f) {
                if (hasPrevSeg) {
                    val prevLen = hypot(prevSegDx, prevSegDy)
                    if (prevLen > 0.35f) {
                        val cosTheta = (dx * prevSegDx + dy * prevSegDy) / (len * prevLen)
                        if (cosTheta < 0.35f) {
                            sharpCorner = true
                        }
                    }
                }
                prevSegDx = dx
                prevSegDy = dy
                hasPrevSeg = true
            }
        }
        lastTimeMs = timeMs
        lastRawX = rawXDp
        lastRawY = rawYDp

        val filteredX = if (sharpCorner) {
            xFilter.filter(rawXDp, dtSec, preserveSharpTurn = true)
        } else {
            xFilter.filter(rawXDp, dtSec, preserveSharpTurn = false)
        }
        val filteredY = if (sharpCorner) {
            yFilter.filter(rawYDp, dtSec, preserveSharpTurn = true)
        } else {
            yFilter.filter(rawYDp, dtSec, preserveSharpTurn = false)
        }

        val validPressure = rawPressure.takeIf { it > 0.01f }?.coerceIn(0.05f, 1f) ?: 0.5f
        smoothedPressure = if (!hasPressure) {
            hasPressure = true
            validPressure
        } else {
            smoothedPressure * 0.6f + validPressure * 0.4f
        }

        return InkSample(
            xDp = filteredX,
            yDp = filteredY,
            pressure = smoothedPressure
        )
    }
}

/**
 * Corner-preserving Ramer-Douglas-Peucker decimation with tiny sub-pixel tolerance (0.25dp)
 * to keep storage compact without sacrificing handwriting curves or sharp corners.
 */
fun simplifyInkSamples(
    points: List<InkSample>,
    epsilonDp: Float = 0.25f
): List<InkSample> {
    if (points.size <= 2) return points
    val keep = BooleanArray(points.size)
    keep[0] = true
    keep[points.size - 1] = true

    fun mark(start: Int, end: Int) {
        if (end - start <= 1) return
        val a = points[start]
        val b = points[end]
        var maxDist = -1f
        var maxIdx = -1
        for (i in (start + 1) until end) {
            val d = pointToSegmentDistance(
                px = points[i].xDp,
                py = points[i].yDp,
                ax = a.xDp,
                ay = a.yDp,
                bx = b.xDp,
                by = b.yDp
            )
            if (d > maxDist) {
                maxDist = d
                maxIdx = i
            }
        }
        if (maxDist > epsilonDp && maxIdx != -1) {
            keep[maxIdx] = true
            mark(start, maxIdx)
            mark(maxIdx, end)
        }
    }

    mark(0, points.size - 1)
    return points.filterIndexed { index, _ -> keep[index] }
}

fun isStraightLineCandidate(points: List<InkSample>): Boolean {
    if (points.size < 4) return false
    val start = points.first()
    val end = points.last()
    val chordLen = hypot(end.xDp - start.xDp, end.yDp - start.yDp)
    if (chordLen < 24f) return false
    val allowedDeviation = max(7.5f, chordLen * 0.075f)
    for (i in 1 until points.size - 1) {
        val d = pointToSegmentDistance(
            px = points[i].xDp,
            py = points[i].yDp,
            ax = start.xDp,
            ay = start.yDp,
            bx = end.xDp,
            by = end.yDp
        )
        if (d > allowedDeviation) return false
    }
    return true
}

fun snapToStraightLine(points: List<InkSample>): List<InkSample> {
    if (points.size < 2) return points
    val start = points.first()
    val end = points.last()
    val avgPressure = (points.sumOf { it.pressure.toDouble() } / points.size).toFloat().coerceIn(0.2f, 0.9f)
    val mid = InkSample(
        xDp = (start.xDp + end.xDp) * 0.5f,
        yDp = (start.yDp + end.yDp) * 0.5f,
        pressure = avgPressure
    )
    return listOf(
        start.copy(pressure = avgPressure),
        mid,
        end.copy(pressure = avgPressure)
    )
}

/**
 * Builds a C1-continuous Centripetal Catmull-Rom Cubic Bezier Path that passes directly
 * through every captured sample point without overshooting or self-intersecting.
 */
fun buildCentripetalCatmullRomPath(
    points: List<InkSample>,
    densityScale: Float
): Path {
    val path = Path()
    if (points.isEmpty()) return path
    val p0Px = Offset(points[0].xDp * densityScale, points[0].yDp * densityScale)
    path.moveTo(p0Px.x, p0Px.y)
    if (points.size == 1) return path
    if (points.size == 2) {
        val p1Px = Offset(points[1].xDp * densityScale, points[1].yDp * densityScale)
        path.lineTo(p1Px.x, p1Px.y)
        return path
    }

    val pts = ArrayList<Offset>(points.size + 2)
    val first = Offset(points[0].xDp * densityScale, points[0].yDp * densityScale)
    val second = Offset(points[1].xDp * densityScale, points[1].yDp * densityScale)
    val phantomStart = Offset(first.x * 2f - second.x, first.y * 2f - second.y)
    pts.add(phantomStart)
    for (s in points) {
        val off = Offset(s.xDp * densityScale, s.yDp * densityScale)
        if (pts.size == 1 || hypot(off.x - pts.last().x, off.y - pts.last().y) >= 0.25f) {
            pts.add(off)
        }
    }
    if (pts.size < 3) {
        val lastPx = Offset(points.last().xDp * densityScale, points.last().yDp * densityScale)
        path.lineTo(lastPx.x, lastPx.y)
        return path
    }
    val last = pts.last()
    val prev = pts[pts.size - 2]
    val phantomEnd = Offset(last.x * 2f - prev.x, last.y * 2f - prev.y)
    pts.add(phantomEnd)

    for (i in 1 until pts.size - 2) {
        val p0 = pts[i - 1]
        val p1 = pts[i]
        val p2 = pts[i + 1]
        val p3 = pts[i + 2]

        val d1 = sqrt(max(hypot(p1.x - p0.x, p1.y - p0.y), 0.1f))
        val d2 = sqrt(max(hypot(p2.x - p1.x, p2.y - p1.y), 0.1f))
        val d3 = sqrt(max(hypot(p3.x - p2.x, p3.y - p2.y), 0.1f))

        val d1Sq = d1 * d1
        val d2Sq = d2 * d2
        val d3Sq = d3 * d3

        val denom1 = 3f * d1 * (d1 + d2)
        val denom2 = 3f * d3 * (d3 + d2)

        val c1x = (d1Sq * p2.x - d2Sq * p0.x + (2f * d1Sq + 3f * d1 * d2 + d2Sq) * p1.x) / denom1
        val c1y = (d1Sq * p2.y - d2Sq * p0.y + (2f * d1Sq + 3f * d1 * d2 + d2Sq) * p1.y) / denom1

        val c2x = (d3Sq * p1.x - d2Sq * p3.x + (2f * d3Sq + 3f * d3 * d2 + d2Sq) * p2.x) / denom2
        val c2y = (d3Sq * p1.y - d2Sq * p3.y + (2f * d3Sq + 3f * d3 * d2 + d2Sq) * p2.y) / denom2

        path.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
    }
    return path
}

data class PressureInkSegment(
    val start: Offset,
    val end: Offset,
    val widthPx: Float
)

/**
 * Generates dense, smooth variable-width capsule segments along a centripetal Catmull-Rom spline
 * modulated by real stylus pressure, local stroke speed, and subtle entry/exit nib taper.
 */
fun buildPressureSegments(
    points: List<InkSample>,
    baseWidthDp: Float,
    densityScale: Float
): List<PressureInkSegment> {
    if (points.size < 2) return emptyList()
    val pts = ArrayList<InkSample>(points.size + 2)
    val pFirst = points[0]
    val pSecond = points[1]
    pts.add(
        InkSample(
            xDp = pFirst.xDp * 2f - pSecond.xDp,
            yDp = pFirst.yDp * 2f - pSecond.yDp,
            pressure = pFirst.pressure
        )
    )
    for (s in points) {
        if (pts.size == 1 || hypot(s.xDp - pts.last().xDp, s.yDp - pts.last().yDp) >= 0.15f) {
            pts.add(s)
        }
    }
    if (pts.size < 3) {
        val a = points.first()
        val b = points.last()
        return listOf(
            PressureInkSegment(
                start = Offset(a.xDp * densityScale, a.yDp * densityScale),
                end = Offset(b.xDp * densityScale, b.yDp * densityScale),
                widthPx = baseWidthDp * densityScale * pressureWidthMultiplier((a.pressure + b.pressure) * 0.5f)
            )
        )
    }
    val pLast = pts.last()
    val pPrev = pts[pts.size - 2]
    pts.add(
        InkSample(
            xDp = pLast.xDp * 2f - pPrev.xDp,
            yDp = pLast.yDp * 2f - pPrev.yDp,
            pressure = pLast.pressure
        )
    )

    val out = ArrayList<PressureInkSegment>(points.size * 3)
    val totalRealSegs = pts.size - 3
    for (i in 1 until pts.size - 2) {
        val s0 = pts[i - 1]
        val s1 = pts[i]
        val s2 = pts[i + 1]
        val s3 = pts[i + 2]

        val p0 = Offset(s0.xDp * densityScale, s0.yDp * densityScale)
        val p1 = Offset(s1.xDp * densityScale, s1.yDp * densityScale)
        val p2 = Offset(s2.xDp * densityScale, s2.yDp * densityScale)
        val p3 = Offset(s3.xDp * densityScale, s3.yDp * densityScale)

        val segDistPx = hypot(p2.x - p1.x, p2.y - p1.y)
        val steps = (segDistPx / (2.5f * densityScale)).toInt().coerceIn(2, 8)

        val d1 = sqrt(max(hypot(p1.x - p0.x, p1.y - p0.y), 0.1f))
        val d2 = sqrt(max(hypot(p2.x - p1.x, p2.y - p1.y), 0.1f))
        val d3 = sqrt(max(hypot(p3.x - p2.x, p3.y - p2.y), 0.1f))
        val d1Sq = d1 * d1
        val d2Sq = d2 * d2
        val d3Sq = d3 * d3
        val denom1 = 3f * d1 * (d1 + d2)
        val denom2 = 3f * d3 * (d3 + d2)
        val c1 = Offset(
            (d1Sq * p2.x - d2Sq * p0.x + (2f * d1Sq + 3f * d1 * d2 + d2Sq) * p1.x) / denom1,
            (d1Sq * p2.y - d2Sq * p0.y + (2f * d1Sq + 3f * d1 * d2 + d2Sq) * p1.y) / denom1
        )
        val c2 = Offset(
            (d3Sq * p1.x - d2Sq * p3.x + (2f * d3Sq + 3f * d3 * d2 + d2Sq) * p2.x) / denom2,
            (d3Sq * p1.y - d2Sq * p3.y + (2f * d3Sq + 3f * d3 * d2 + d2Sq) * p2.y) / denom2
        )

        var prevPt = p1
        for (step in 1..steps) {
            val t = step.toFloat() / steps.toFloat()
            val curPt = evalCubicBezier(p1, c1, c2, p2, t)
            val interpPressure = s1.pressure + (s2.pressure - s1.pressure) * (t - 0.5f / steps)
            val globalProgress = ((i - 1) + t) / max(1, totalRealSegs).toFloat()
            val edgeTaper = when {
                totalRealSegs <= 1 -> 1f
                globalProgress < 0.06f -> 0.72f + 0.28f * (globalProgress / 0.06f)
                globalProgress > 0.94f -> 0.72f + 0.28f * ((1f - globalProgress) / 0.06f)
                else -> 1f
            }
            val wPx = baseWidthDp * densityScale * pressureWidthMultiplier(interpPressure) * edgeTaper
            out.add(PressureInkSegment(prevPt, curPt, wPx.coerceAtLeast(0.8f * densityScale)))
            prevPt = curPt
        }
    }
    return out
}

private fun pressureWidthMultiplier(pressure: Float): Float {
    val p = pressure.coerceIn(0.05f, 1.0f)
    // Centered around p=0.5 -> ~1.0x; light touch -> ~0.48x; firm press -> ~1.58x
    return 0.44f + 1.16f * p.pow(0.85f)
}

private fun evalCubicBezier(p0: Offset, c1: Offset, c2: Offset, p3: Offset, t: Float): Offset {
    val u = 1f - t
    val tt = t * t
    val uu = u * u
    val uuu = uu * u
    val ttt = tt * t
    return Offset(
        x = uuu * p0.x + 3f * uu * t * c1.x + 3f * u * tt * c2.x + ttt * p3.x,
        y = uuu * p0.y + 3f * uu * t * c1.y + 3f * u * tt * c2.y + ttt * p3.y
    )
}

fun DrawScope.drawInkStroke(
    stroke: InkStroke,
    isDark: Boolean,
    densityScale: Float,
    cachedPath: Path? = null,
    cachedPressureSegments: List<PressureInkSegment>? = null
) {
    val pts = stroke.points
    if (pts.isEmpty()) return
    val baseColor = resolveInkColor(stroke.colorArgb, isDark)
    val isHighlighter = stroke.tool == NoteInkTool.HIGHLIGHTER.name
    val renderColor = if (isHighlighter) baseColor.copy(alpha = 0.34f) else baseColor
    val widthPx = stroke.baseWidthDp * densityScale

    if (pts.size == 1) {
        val center = Offset(pts[0].xDp * densityScale, pts[0].yDp * densityScale)
        val dotRadius = if (stroke.tool == NoteInkTool.PRESSURE_PEN.name) {
            (widthPx * pressureWidthMultiplier(pts[0].pressure) * 0.5f)
        } else {
            widthPx * 0.5f
        }
        drawCircle(
            color = renderColor,
            radius = dotRadius.coerceAtLeast(1f),
            center = center
        )
        return
    }

    if (stroke.tool == NoteInkTool.PRESSURE_PEN.name) {
        val segs = cachedPressureSegments ?: buildPressureSegments(pts, stroke.baseWidthDp, densityScale)
        for (i in segs.indices) {
            val seg = segs[i]
            drawLine(
                color = renderColor,
                start = seg.start,
                end = seg.end,
                strokeWidth = seg.widthPx,
                cap = StrokeCap.Round
            )
        }
    } else {
        val path = cachedPath ?: buildCentripetalCatmullRomPath(pts, densityScale)
        drawPath(
            path = path,
            color = renderColor,
            style = Stroke(
                width = widthPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

fun inkStrokeContainsPoint(
    stroke: InkStroke,
    pointDp: Offset,
    eraserRadiusDp: Float
): Boolean {
    val pts = stroke.points
    if (pts.isEmpty()) return false
    val hitRadius = eraserRadiusDp + stroke.baseWidthDp * 0.6f

    // Fast AABB rejection
    var minX = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    for (p in pts) {
        if (p.xDp < minX) minX = p.xDp
        if (p.xDp > maxX) maxX = p.xDp
        if (p.yDp < minY) minY = p.yDp
        if (p.yDp > maxY) maxY = p.yDp
    }
    if (pointDp.x < minX - hitRadius || pointDp.x > maxX + hitRadius ||
        pointDp.y < minY - hitRadius || pointDp.y > maxY + hitRadius
    ) {
        return false
    }

    if (pts.size == 1) {
        return hypot(pointDp.x - pts[0].xDp, pointDp.y - pts[0].yDp) <= hitRadius
    }
    for (i in 0 until pts.size - 1) {
        val a = pts[i]
        val b = pts[i + 1]
        val d = pointToSegmentDistance(pointDp.x, pointDp.y, a.xDp, a.yDp, b.xDp, b.yDp)
        if (d <= hitRadius) return true
    }
    return false
}

fun pointToSegmentDistance(
    px: Float,
    py: Float,
    ax: Float,
    ay: Float,
    bx: Float,
    by: Float
): Float {
    val dx = bx - ax
    val dy = by - ay
    val l2 = dx * dx + dy * dy
    if (l2 <= 1e-6f) return hypot(px - ax, py - ay)
    val t = ((px - ax) * dx + (py - ay) * dy) / l2
    val clampedT = min(1f, max(0f, t))
    val projX = ax + clampedT * dx
    val projY = ay + clampedT * dy
    return hypot(px - projX, py - projY)
}
