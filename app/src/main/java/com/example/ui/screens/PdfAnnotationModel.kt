package com.example.ui.screens

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.data.model.AnnotationEntity
import java.util.UUID
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import org.json.JSONArray
import org.json.JSONObject

enum class PdfTool {
    PAN_ZOOM,
    SELECT,         // Select, move, resize & edit annotations
    NOTE,           // Multi-color sticky note badge
    HIGHLIGHT,      // Text/Area highlighter
    STRIKETHROUGH,  // Strikethrough
    UNDERLINE,      // Underline
    PEN,            // Freehand handwriting / pen drawing
    SHAPE,          // Rect, Circle, Arrows, Curve Arrows, Star
    TEXT_BOX,       // Custom text / handwritten label
    ERASER          // Delete annotations
}

enum class ShapeType {
    RECT,
    CIRCLE,
    ARROW,
    CURVE_ARROW,
    S_CURVE_ARROW,
    DOUBLE_ARROW,
    ELBOW_ARROW,
    STAR
}

enum class PdfSaveStatus {
    SAVED,
    SAVING,
    UNSAVED,
    ERROR
}

enum class ReadingTheme(
    val displayName: String,
    val bgTint: Color,
    val deskColor: Color,
    val swatchColor: Color,
    val isDark: Boolean
) {
    WHITE(
        displayName = "Daylight",
        bgTint = Color.Transparent,
        deskColor = Color(0xFFF1F5F9),
        swatchColor = Color(0xFFFFFFFF),
        isDark = false
    ),
    DARK(
        displayName = "Night",
        bgTint = Color(0x450F172A),
        deskColor = Color(0xFF0B1120),
        swatchColor = Color(0xFF1E293B),
        isDark = true
    ),
    SEPIA(
        displayName = "Sepia",
        bgTint = Color(0x42FBF0D9),
        deskColor = Color(0xFFEDE4D3),
        swatchColor = Color(0xFFFBF0D9),
        isDark = false
    ),
    MINT(
        displayName = "Mint",
        bgTint = Color(0x3AE8F5E9),
        deskColor = Color(0xFFE0EFE5),
        swatchColor = Color(0xFFE8F5E9),
        isDark = false
    )
}

internal data class PdfStickyPalette(
    val name: String,
    val fill: Color,
    val accent: Color,
    val ink: Color
)

internal val PdfStickyColors = listOf(
    PdfStickyPalette("Sun", Color(0xFFFEF08A), Color(0xFFEAB308), Color(0xFF713F12)),
    PdfStickyPalette("Sky", Color(0xFFBAE6FD), Color(0xFF0284C7), Color(0xFF0C4A6E)),
    PdfStickyPalette("Mint", Color(0xFFBBF7D0), Color(0xFF16A34A), Color(0xFF14532D)),
    PdfStickyPalette("Rose", Color(0xFFFBCFE8), Color(0xFFDB2777), Color(0xFF831843)),
    PdfStickyPalette("Lavender", Color(0xFFDDD6FE), Color(0xFF7C3AED), Color(0xFF4C1D95)),
    PdfStickyPalette("Peach", Color(0xFFFED7AA), Color(0xFFEA580C), Color(0xFF7C2D12))
)

internal fun findStickyPalette(colorArgb: Int): PdfStickyPalette {
    val matched = PdfStickyColors.firstOrNull {
        (it.fill.toArgb() and 0xFFFFFF) == (colorArgb and 0xFFFFFF) ||
            (it.accent.toArgb() and 0xFFFFFF) == (colorArgb and 0xFFFFFF)
    }
    if (matched != null) return matched
    val fill = Color(colorArgb).copy(alpha = 1f)
    val luminance = 0.299f * fill.red + 0.587f * fill.green + 0.114f * fill.blue
    val accent = Color(
        red = (fill.red * 0.72f).coerceIn(0f, 1f),
        green = (fill.green * 0.72f).coerceIn(0f, 1f),
        blue = (fill.blue * 0.72f).coerceIn(0f, 1f),
        alpha = 1f
    )
    val ink = if (luminance > 0.56f) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    return PdfStickyPalette("Custom", fill, accent, ink)
}

internal const val STICKY_STYLE_BADGE = 1f
internal const val STICKY_STYLE_CARD = 2f
internal const val STICKY_CARD_WIDTH = 256f

internal fun PdfPageAnnotation.isStickyCard(): Boolean =
    type == "COMMENT" && strokeWidth == STICKY_STYLE_CARD

internal fun PdfPageAnnotation.estimateStickyCardHeight(): Float {
    val lines = wrapStickyTextLines(content, maxCharsPerLine = 20, maxLines = 5)
    return (54f + lines.size * 27f + 22f).coerceIn(116f, 224f)
}

internal fun wrapStickyTextLines(text: String, maxCharsPerLine: Int = 20, maxLines: Int = 5): List<String> {
    val words = text.trim().replace("\n", " ").split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return listOf("Sticky note")
    val lines = mutableListOf<String>()
    var current = StringBuilder()
    for (word in words) {
        val candidate = if (current.isEmpty()) word else "${current} $word"
        if (candidate.length <= maxCharsPerLine) {
            current = StringBuilder(candidate)
        } else {
            if (current.isNotEmpty()) {
                lines.add(current.toString())
                if (lines.size >= maxLines) break
            }
            current = StringBuilder(word.take(maxCharsPerLine))
        }
    }
    if (lines.size < maxLines && current.isNotEmpty()) {
        lines.add(current.toString())
    } else if (lines.size == maxLines && words.joinToString(" ").length > lines.sumOf { it.length + 1 }) {
        val last = lines.last()
        lines[lines.lastIndex] = if (last.length > 3) last.dropLast(1) + "…" else "$last…"
    }
    return lines
}

internal fun normalizeAngleDeg(angleDeg: Float): Float {
    if (!angleDeg.isFinite()) return 0f
    var normalized = ((angleDeg % 360f) + 360f) % 360f
    if (normalized > 180f) normalized -= 360f
    return if (abs(normalized) < 0.05f) 0f else normalized
}

internal fun snapAngleDeg(angleDeg: Float, snapToleranceDeg: Float = 3.5f): Float {
    val norm = normalizeAngleDeg(angleDeg)
    val targets = floatArrayOf(0f, 45f, 90f, 135f, 180f, -45f, -90f, -135f, -180f)
    for (target in targets) {
        if (abs(norm - target) <= snapToleranceDeg) {
            return if (target == -180f) 180f else target
        }
    }
    return norm
}

internal fun Offset.rotatedAround(pivot: Offset, angleDeg: Float): Offset {
    if (angleDeg == 0f) return this
    val rad = angleDeg * (Math.PI / 180.0)
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()
    val dx = x - pivot.x
    val dy = y - pivot.y
    return Offset(
        pivot.x + dx * cosA - dy * sinA,
        pivot.y + dy * cosA + dx * sinA
    )
}

data class PdfPageAnnotation(
    val id: String = UUID.randomUUID().toString(),
    val type: String, // HIGHLIGHT, UNDERLINE, STRIKETHROUGH, DRAWING, SHAPE_RECT, SHAPE_CIRCLE, SHAPE_ARROW, SHAPE_STAR, TEXT_BOX, STAMP, COMMENT
    val pageNumber: Int,
    val points: List<Offset> = emptyList(),
    val start: Offset = Offset.Zero,
    val end: Offset = Offset.Zero,
    val content: String = "",
    val colorArgb: Int = -256,
    val strokeWidth: Float = 4f,
    val segments: List<PdfHighlightSegment> = emptyList(),
    val rotationDeg: Float = 0f,
    val linkedNoteId: String? = null
) {
    fun toEntity(attachmentId: String): AnnotationEntity {
        val obj = JSONObject()
        obj.put("type", type)
        obj.put("color", colorArgb)
        obj.put("width", strokeWidth.toDouble())
        obj.put("content", content)
        obj.put("startX", start.x.toDouble())
        obj.put("startY", start.y.toDouble())
        obj.put("endX", end.x.toDouble())
        obj.put("endY", end.y.toDouble())
        obj.put("rotation", rotationDeg.toDouble())
        if (!linkedNoteId.isNullOrBlank()) {
            obj.put("linkedNoteId", linkedNoteId)
        }

        val ptsArray = JSONArray()
        for (p in points) {
            val pObj = JSONObject()
            pObj.put("x", p.x.toDouble())
            pObj.put("y", p.y.toDouble())
            ptsArray.put(pObj)
        }
        obj.put("points", ptsArray)

        if (segments.isNotEmpty()) {
            val segArray = JSONArray()
            for (seg in segments) {
                val sObj = JSONObject()
                sObj.put("l", seg.left.toDouble())
                sObj.put("t", seg.top.toDouble())
                sObj.put("r", seg.right.toDouble())
                sObj.put("b", seg.bottom.toDouble())
                segArray.put(sObj)
            }
            obj.put("segments", segArray)
        }

        return AnnotationEntity(
            id = id,
            attachmentId = attachmentId,
            annotationType = type,
            pageNumber = pageNumber,
            coordinatesJson = obj.toString(),
            content = content,
            colorHex = String.format(java.util.Locale.US, "#%06X", (0xFFFFFF and colorArgb)),
            createdAtUtc = System.currentTimeMillis()
        )
    }

    fun containsPoint(point: Offset, tolerance: Float = 26f): Boolean {
        val localPoint = if (rotationDeg != 0f) {
            point.rotatedAround(rotationPivot(), -rotationDeg)
        } else {
            point
        }
        return containsLocalPoint(localPoint, tolerance)
    }

    internal fun containsLocalPoint(point: Offset, tolerance: Float = 26f): Boolean {
        return when (type) {
            "HIGHLIGHT" -> {
                if (segments.isNotEmpty()) {
                    segments.any { it.contains(point, tolerance) }
                } else {
                    val left = min(start.x, end.x) - tolerance
                    val right = max(start.x, end.x) + tolerance
                    val top = min(start.y, end.y) - tolerance
                    val bottom = max(start.y, end.y) + tolerance
                    point.x in left..right && point.y in top..bottom
                }
            }
            "UNDERLINE", "STRIKETHROUGH", "SHAPE_RECT", "SHAPE_CIRCLE", "SHAPE_STAR", "STAMP" -> {
                val left = min(start.x, end.x) - tolerance
                val right = max(start.x, end.x) + tolerance
                val top = min(start.y, end.y) - tolerance
                val bottom = max(start.y, end.y) + tolerance
                point.x in left..right && point.y in top..bottom
            }
            "SHAPE_ARROW" -> {
                distanceToSegment(point, start, end) <= max(strokeWidth / 2f + tolerance, tolerance)
            }
            "SHAPE_CURVE_ARROW" -> {
                val ctrl = resolveCurveControlPoint(start, end, points)
                distanceToQuadraticCurve(point, start, ctrl, end) <= max(strokeWidth / 2f + tolerance, tolerance)
            }
            "SHAPE_S_CURVE_ARROW" -> {
                val (c1, c2) = resolveSCurveControlPoints(start, end, points)
                distanceToCubicCurve(point, start, c1, c2, end) <= max(strokeWidth / 2f + tolerance, tolerance)
            }
            "SHAPE_DOUBLE_ARROW" -> {
                val ctrl = points.firstOrNull()
                val dist = if (ctrl != null) {
                    distanceToQuadraticCurve(point, start, ctrl, end)
                } else {
                    distanceToSegment(point, start, end)
                }
                dist <= max(strokeWidth / 2f + tolerance, tolerance)
            }
            "SHAPE_ELBOW_ARROW" -> {
                val corner = resolveElbowCornerPoint(start, end, points)
                val d1 = distanceToSegment(point, start, corner)
                val d2 = distanceToSegment(point, corner, end)
                min(d1, d2) <= max(strokeWidth / 2f + tolerance, tolerance)
            }
            "TEXT_BOX" -> {
                val estimatedWidth = max(content.length * (strokeWidth.coerceAtLeast(24f) * 0.55f), 110f)
                val fontHeight = strokeWidth.coerceAtLeast(24f)
                val left = start.x - tolerance
                val right = start.x + estimatedWidth + tolerance
                val top = start.y - fontHeight - tolerance
                val bottom = start.y + tolerance
                point.x in left..right && point.y in top..bottom
            }
            "COMMENT" -> {
                val cardW = stickyWidth()
                val cardH = stickyHeight()
                val left = start.x - tolerance
                val right = start.x + cardW + tolerance
                val top = start.y - tolerance
                val bottom = start.y + cardH + tolerance
                point.x in left..right && point.y in top..bottom
            }
            "DRAWING" -> {
                if (points.size == 1) {
                    return hypot(point.x - points[0].x, point.y - points[0].y) <= (strokeWidth / 2 + tolerance)
                }
                for (i in 0 until points.size - 1) {
                    if (distanceToSegment(point, points[i], points[i + 1]) <= (strokeWidth / 2 + tolerance)) {
                        return true
                    }
                }
                false
            }
            else -> false
        }
    }

    private fun distanceToSegment(p: Offset, v: Offset, w: Offset): Float {
        val l2 = (v.x - w.x) * (v.x - w.x) + (v.y - w.y) * (v.y - w.y)
        if (l2 == 0f) return hypot(p.x - v.x, p.y - v.y)
        val t = max(0f, min(1f, ((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2))
        val proj = Offset(v.x + t * (w.x - v.x), v.y + t * (w.y - v.y))
        return hypot(p.x - proj.x, p.y - proj.y)
    }
}

fun parsePdfAnnotation(entity: AnnotationEntity): PdfPageAnnotation? {
    return try {
        val obj = JSONObject(entity.coordinatesJson)
        val type = obj.optString("type", entity.annotationType)
        val color = obj.optInt("color", -256)
        val width = obj.optDouble("width", 4.0).toFloat()
        val content = obj.optString("content", entity.content)
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

        val segments = mutableListOf<PdfHighlightSegment>()
        if (obj.has("segments")) {
            val segArr = obj.getJSONArray("segments")
            for (i in 0 until segArr.length()) {
                val sObj = segArr.getJSONObject(i)
                segments.add(
                    PdfHighlightSegment(
                        left = sObj.getDouble("l").toFloat(),
                        top = sObj.getDouble("t").toFloat(),
                        right = sObj.getDouble("r").toFloat(),
                        bottom = sObj.getDouble("b").toFloat()
                    )
                )
            }
        }

        val rotation = normalizeAngleDeg(obj.optDouble("rotation", 0.0).toFloat())
        val linkedNoteId = obj.optString("linkedNoteId", "").takeIf { it.isNotBlank() }

        PdfPageAnnotation(
            id = entity.id,
            type = type,
            pageNumber = entity.pageNumber,
            points = points,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            content = content,
            colorArgb = color,
            strokeWidth = width,
            segments = segments,
            rotationDeg = rotation,
            linkedNoteId = linkedNoteId
        )
    } catch (e: Exception) {
        null
    }
}

fun DrawScope.drawHighlightSelectionPreview(
    selection: PdfHighlightSelection,
    color: Color
) {
    val translucentColor = if (color.alpha > 0.72f) color.copy(alpha = 0.46f) else color
    for (seg in selection.segments) {
        drawRoundRect(
            color = translucentColor,
            topLeft = Offset(min(seg.left, seg.right), min(seg.top, seg.bottom)),
            size = Size(seg.width, seg.height),
            cornerRadius = CornerRadius(5f, 5f)
        )
    }
    // Render crisp text-selection carets at start & end of the selected phrase
    val caretColor = color.copy(alpha = 0.95f)
    val startTop = selection.startCaretTop
    val startBot = selection.startCaretBottom
    if (startTop != null && startBot != null) {
        drawLine(
            color = caretColor,
            start = startTop,
            end = startBot,
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = caretColor,
            radius = 5.5f,
            center = Offset(startTop.x, startTop.y - 3f)
        )
    }
    val endTop = selection.endCaretTop
    val endBot = selection.endCaretBottom
    if (endTop != null && endBot != null) {
        drawLine(
            color = caretColor,
            start = endTop,
            end = endBot,
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = caretColor,
            radius = 5.5f,
            center = Offset(endBot.x, endBot.y + 3f)
        )
    }
}

fun DrawScope.drawPdfAnnotation(item: PdfPageAnnotation) {
    if (item.rotationDeg != 0f) {
        rotate(degrees = item.rotationDeg, pivot = item.rotationPivot()) {
            drawUnrotatedPdfAnnotation(item)
        }
    } else {
        drawUnrotatedPdfAnnotation(item)
    }
}

private fun DrawScope.drawUnrotatedPdfAnnotation(item: PdfPageAnnotation) {
    val color = Color(item.colorArgb)
    when (item.type) {
        "HIGHLIGHT" -> {
            val translucentColor = if (color.alpha > 0.72f) color.copy(alpha = 0.46f) else color
            if (item.segments.isNotEmpty()) {
                for (seg in item.segments) {
                    val left = min(seg.left, seg.right)
                    val top = min(seg.top, seg.bottom)
                    drawRoundRect(
                        color = translucentColor,
                        topLeft = Offset(left, top),
                        size = Size(seg.width, seg.height),
                        cornerRadius = CornerRadius(5f, 5f)
                    )
                }
            } else {
                val left = min(item.start.x, item.end.x)
                val top = min(item.start.y, item.end.y)
                val w = kotlin.math.abs(item.end.x - item.start.x)
                val h = kotlin.math.abs(item.end.y - item.start.y)
                drawRoundRect(
                    color = translucentColor,
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(5f, 5f)
                )
            }
        }
        "UNDERLINE" -> {
            val left = min(item.start.x, item.end.x)
            val right = max(item.start.x, item.end.x)
            val y = max(item.start.y, item.end.y)
            drawLine(
                color = color,
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = item.strokeWidth.coerceAtLeast(3.5f),
                cap = StrokeCap.Round
            )
        }
        "STRIKETHROUGH" -> {
            val left = min(item.start.x, item.end.x)
            val right = max(item.start.x, item.end.x)
            val midY = (item.start.y + item.end.y) / 2f
            drawLine(
                color = color,
                start = Offset(left, midY),
                end = Offset(right, midY),
                strokeWidth = item.strokeWidth.coerceAtLeast(3.5f),
                cap = StrokeCap.Round
            )
        }
        "DRAWING" -> {
            if (item.points.size > 1) {
                val path = Path().apply {
                    moveTo(item.points.first().x, item.points.first().y)
                    for (p in item.points.drop(1)) lineTo(p.x, p.y)
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = item.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            } else if (item.points.size == 1) {
                drawCircle(color = color, radius = item.strokeWidth / 2f, center = item.points.first())
            }
        }
        "SHAPE_RECT" -> {
            val left = min(item.start.x, item.end.x)
            val top = min(item.start.y, item.end.y)
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(kotlin.math.abs(item.end.x - item.start.x), kotlin.math.abs(item.end.y - item.start.y)),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = item.strokeWidth, join = StrokeJoin.Round)
            )
        }
        "SHAPE_CIRCLE" -> {
            val left = min(item.start.x, item.end.x)
            val top = min(item.start.y, item.end.y)
            drawOval(
                color = color,
                topLeft = Offset(left, top),
                size = Size(kotlin.math.abs(item.end.x - item.start.x), kotlin.math.abs(item.end.y - item.start.y)),
                style = Stroke(width = item.strokeWidth)
            )
        }
        "SHAPE_ARROW" -> {
            drawArrow(item.start, item.end, color, item.strokeWidth)
        }
        "SHAPE_CURVE_ARROW" -> {
            val ctrl = resolveCurveControlPoint(item.start, item.end, item.points)
            drawCurveArrow(item.start, item.end, ctrl, color, item.strokeWidth)
        }
        "SHAPE_S_CURVE_ARROW" -> {
            drawSCurveArrow(item.start, item.end, item.points, color, item.strokeWidth)
        }
        "SHAPE_DOUBLE_ARROW" -> {
            drawDoubleArrow(item.start, item.end, item.points, color, item.strokeWidth)
        }
        "SHAPE_ELBOW_ARROW" -> {
            drawElbowArrow(item.start, item.end, item.points, color, item.strokeWidth)
        }
        "SHAPE_STAR" -> {
            drawFivePointStar(item.start, item.end, color, item.strokeWidth)
        }
        "TEXT_BOX" -> {
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    this.color = item.colorArgb
                    textSize = item.strokeWidth.coerceAtLeast(24f)
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT,
                        android.graphics.Typeface.BOLD_ITALIC
                    )
                }
                drawText(item.content, item.start.x, item.start.y, paint)
            }
        }
        "STAMP" -> {
            val textLen = max(item.content.length * 20f, 160f)
            val left = item.start.x
            val top = item.start.y
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(textLen + 40f, 60f),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = 4f)
            )
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    this.color = item.colorArgb
                    textSize = 28f
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                drawText(item.content, left + 20f, top + 42f, paint)
            }
        }
        "COMMENT" -> {
            val palette = findStickyPalette(item.colorArgb)
            val left = item.start.x
            val top = item.start.y

            if (item.isStickyCard()) {
                val cardW = item.stickyWidth()
                val cardH = item.stickyHeight()
                val lines = wrapStickyTextLines(
                    text = item.content,
                    maxCharsPerLine = maxCharsForStickyWidth(cardW),
                    maxLines = maxLinesForStickyHeight(cardH)
                )
                val fold = (min(cardW, cardH) * 0.11f).coerceIn(14f, 26f)

                // Soft layered drop shadow
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.16f),
                    topLeft = Offset(left + 5f, top + 7f),
                    size = Size(cardW, cardH),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                // Main pastel paper card fill
                drawRoundRect(
                    color = palette.fill,
                    topLeft = Offset(left, top),
                    size = Size(cardW, cardH),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                // Top sticky tape / pin header strip
                drawRoundRect(
                    color = palette.accent.copy(alpha = 0.18f),
                    topLeft = Offset(left + 2f, top + 2f),
                    size = Size(cardW - 4f, 28f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                // Center pushpin dot & inner highlight
                drawCircle(
                    color = palette.accent,
                    radius = 7f,
                    center = Offset(left + cardW / 2f, top + 16f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.65f),
                    radius = 2.4f,
                    center = Offset(left + cardW / 2f - 2f, top + 14f)
                )
                // Card outline
                drawRoundRect(
                    color = palette.accent.copy(alpha = 0.85f),
                    topLeft = Offset(left, top),
                    size = Size(cardW, cardH),
                    cornerRadius = CornerRadius(14f, 14f),
                    style = Stroke(width = 2.5f)
                )
                // Folded bottom-right corner curl
                val cornerCurl = Path().apply {
                    moveTo(left + cardW - fold, top + cardH)
                    lineTo(left + cardW, top + cardH - fold)
                    lineTo(left + cardW - fold, top + cardH - fold)
                    close()
                }
                drawPath(cornerCurl, color = palette.accent.copy(alpha = 0.35f))

                // Wrapped readable note text scaled smoothly inside resized card
                drawContext.canvas.nativeCanvas.apply {
                    val fontSize = if (cardW < 190f) 18.5f else 21f
                    val lineStep = if (cardW < 190f) 24f else 27f
                    val textPaint = android.graphics.Paint().apply {
                        this.color = palette.ink.toArgb()
                        textSize = fontSize
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create(
                            android.graphics.Typeface.DEFAULT,
                            android.graphics.Typeface.BOLD
                        )
                    }
                    var yPos = top + 54f
                    for (line in lines) {
                        if (yPos > top + cardH - 12f) break
                        drawText(line, left + 16f, yPos, textPaint)
                        yPos += lineStep
                    }
                }
            } else {
                val boxSize = item.stickyWidth()
                val scale = boxSize / STICKY_BADGE_DEFAULT_SIZE
                val fold = 12f * scale

                // Subtle drop shadow under sticky badge
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.14f),
                    topLeft = Offset(left + 3f, top + 4f),
                    size = Size(boxSize, boxSize),
                    cornerRadius = CornerRadius(10f * scale, 10f * scale)
                )
                // Main pastel note fill
                drawRoundRect(
                    color = palette.fill,
                    topLeft = Offset(left, top),
                    size = Size(boxSize, boxSize),
                    cornerRadius = CornerRadius(10f * scale, 10f * scale)
                )
                // Crisp border
                drawRoundRect(
                    color = palette.accent,
                    topLeft = Offset(left, top),
                    size = Size(boxSize, boxSize),
                    cornerRadius = CornerRadius(10f * scale, 10f * scale),
                    style = Stroke(width = 2.5f)
                )
                // Folded corner triangle accent at top-right
                val foldPath = Path().apply {
                    moveTo(left + boxSize - fold, top + 2f)
                    lineTo(left + boxSize - 2f, top + fold)
                    lineTo(left + boxSize - fold, top + fold)
                    close()
                }
                drawPath(foldPath, color = palette.accent.copy(alpha = 0.45f))

                // Note ink lines
                val strokeW = (2.6f * scale).coerceIn(2f, 5f)
                drawLine(palette.ink, Offset(left + 11f * scale, top + 18f * scale), Offset(left + 34f * scale, top + 18f * scale), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(palette.ink, Offset(left + 11f * scale, top + 27f * scale), Offset(left + 38f * scale, top + 27f * scale), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(palette.ink, Offset(left + 11f * scale, top + 36f * scale), Offset(left + 27f * scale, top + 36f * scale), strokeWidth = strokeW, cap = StrokeCap.Round)
            }
        }
    }
}

fun DrawScope.drawArrow(start: Offset, end: Offset, color: Color, strokeWidth: Float) {
    drawLine(color = color, start = start, end = end, strokeWidth = strokeWidth, cap = StrokeCap.Round)
    val dx = end.x - start.x
    val dy = end.y - start.y
    val angle = atan2(dy, dx)
    val arrowLen = max(28f, strokeWidth * 4.5f)
    val wingAngle = Math.PI / 6.0

    val p1 = Offset(
        (end.x - arrowLen * cos(angle - wingAngle)).toFloat(),
        (end.y - arrowLen * sin(angle - wingAngle)).toFloat()
    )
    val p2 = Offset(
        (end.x - arrowLen * cos(angle + wingAngle)).toFloat(),
        (end.y - arrowLen * sin(angle + wingAngle)).toFloat()
    )

    val path = Path().apply {
        moveTo(end.x, end.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        close()
    }
    drawPath(path = path, color = color, style = Fill)
}

fun DrawScope.drawFivePointStar(start: Offset, end: Offset, color: Color, strokeWidth: Float) {
    val cx = (start.x + end.x) / 2f
    val cy = (start.y + end.y) / 2f
    val rOuter = max(kotlin.math.abs(end.x - start.x), kotlin.math.abs(end.y - start.y)) / 2f
    val rInner = rOuter * 0.42f

    val path = Path()
    val step = Math.PI / 5.0
    var rot = -Math.PI / 2.0

    for (i in 0 until 5) {
        val xOut = (cx + cos(rot) * rOuter).toFloat()
        val yOut = (cy + sin(rot) * rOuter).toFloat()
        if (i == 0) path.moveTo(xOut, yOut) else path.lineTo(xOut, yOut)
        rot += step

        val xIn = (cx + cos(rot) * rInner).toFloat()
        val yIn = (cy + sin(rot) * rInner).toFloat()
        path.lineTo(xIn, yIn)
        rot += step
    }
    path.close()
    drawPath(path = path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
