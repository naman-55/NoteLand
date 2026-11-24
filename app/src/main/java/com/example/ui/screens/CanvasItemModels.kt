package com.example.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import com.example.data.model.CanvasElementEntity
import com.example.data.storage.AttachmentStorageManager
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import org.json.JSONArray
import org.json.JSONObject

enum class CanvasToolMode {
    PAN_ZOOM,
    PEN,
    HIGHLIGHTER,
    SHAPE_RECT,
    SHAPE_CIRCLE,
    SHAPE_LINE,
    SHAPE_ARROW,
    SHAPE_CONNECTOR,
    TEXT,
    STICKY_NOTE,
    SELECT,
    LASSO,
    ERASER
}

/**
 * Domain model for freehand / shape / annotation canvas elements persisted in Room.
 */
data class CanvasItem(
    val id: String = UUID.randomUUID().toString(),
    val type: String, // STROKE, HIGHLIGHTER, RECTANGLE, CIRCLE, LINE, ARROW, CONNECTOR, TEXT, STICKY, IMAGE
    val points: List<Offset> = emptyList(),
    val start: Offset = Offset.Zero,
    val end: Offset = Offset.Zero,
    val text: String = "",
    val imageUri: String = "",
    val colorArgb: Int = -16777216,
    val fillColorArgb: Int? = null,
    val strokeWidth: Float = 5f,
    val zIndex: Int = 0,
    val rotationDeg: Float = 0f
) {
    fun toEntity(canvasId: String): CanvasElementEntity {
        val obj = JSONObject().apply {
            put("type", type)
            put("color", colorArgb)
            if (fillColorArgb != null) put("fillColor", fillColorArgb)
            put("width", strokeWidth.toDouble())
            put("rotation", rotationDeg.toDouble())
            put("text", text)
            put("imageUri", imageUri)
            put("startX", start.x.toDouble())
            put("startY", start.y.toDouble())
            put("endX", end.x.toDouble())
            put("endY", end.y.toDouble())

            val ptsArray = JSONArray()
            for (p in points) {
                ptsArray.put(
                    JSONObject().apply {
                        put("x", p.x.toDouble())
                        put("y", p.y.toDouble())
                    }
                )
            }
            put("points", ptsArray)
        }

        return CanvasElementEntity(
            id = id,
            canvasId = canvasId,
            elementType = type,
            elementDataJson = obj.toString(),
            zIndex = zIndex,
            createdAtUtc = System.currentTimeMillis()
        )
    }

    fun containsPoint(point: Offset, tolerance: Float = 16f): Boolean {
        return when (type) {
            "STROKE", "HIGHLIGHTER" -> {
                for (i in 0 until points.size - 1) {
                    if (distToSegment(point, points[i], points[i + 1]) <= (strokeWidth / 2f + tolerance)) {
                        return true
                    }
                }
                points.size == 1 && hypot(point.x - points[0].x, point.y - points[0].y) <= (strokeWidth + tolerance)
            }
            "RECTANGLE", "STICKY" -> {
                val left = min(start.x, end.x) - tolerance
                val right = max(start.x, end.x) + tolerance
                val top = min(start.y, end.y) - tolerance
                val bottom = max(start.y, end.y) + tolerance
                point.x in left..right && point.y in top..bottom
            }
            "CIRCLE" -> {
                val center = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
                val radius = hypot(end.x - start.x, end.y - start.y) / 2f + tolerance
                hypot(point.x - center.x, point.y - center.y) <= radius
            }
            "LINE", "ARROW", "CONNECTOR" -> {
                distToSegment(point, start, end) <= (strokeWidth / 2f + tolerance)
            }
            "TEXT" -> {
                val bounds = Rect(start.x, start.y - 30f, start.x + max(text.length * 16f, 60f), start.y + 20f)
                bounds.contains(point)
            }
            "IMAGE" -> {
                val left = min(start.x, end.x)
                val right = max(start.x, end.x)
                val top = min(start.y, end.y)
                val bottom = max(start.y, end.y)
                point.x in left..right && point.y in top..bottom
            }
            else -> false
        }
    }

    fun translatedBy(dx: Float, dy: Float): CanvasItem {
        return copy(
            points = points.map { Offset(it.x + dx, it.y + dy) },
            start = Offset(start.x + dx, start.y + dy),
            end = Offset(end.x + dx, end.y + dy)
        )
    }

    private fun distToSegment(p: Offset, v: Offset, w: Offset): Float {
        val l2 = (v.x - w.x) * (v.x - w.x) + (v.y - w.y) * (v.y - w.y)
        if (l2 == 0f) return hypot(p.x - v.x, p.y - v.y)
        val t = max(0f, min(1f, ((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2))
        val proj = Offset(v.x + t * (w.x - v.x), v.y + t * (w.y - v.y))
        return hypot(p.x - proj.x, p.y - proj.y)
    }
}

fun parseCanvasItemFromEntity(entity: CanvasElementEntity): CanvasItem? {
    return try {
        val obj = JSONObject(entity.elementDataJson)
        val type = obj.optString("type", entity.elementType)
        val color = obj.optInt("color", -16777216)
        val fillColor = if (obj.has("fillColor")) obj.getInt("fillColor") else null
        val width = obj.optDouble("width", 5.0).toFloat()
        val rotation = obj.optDouble("rotation", 0.0).toFloat()
        val text = obj.optString("text", "")
        val imageUri = obj.optString("imageUri", "")
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

        CanvasItem(
            id = entity.id,
            type = type,
            points = points,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            text = text,
            imageUri = imageUri,
            colorArgb = color,
            fillColorArgb = fillColor,
            strokeWidth = width,
            zIndex = entity.zIndex,
            rotationDeg = rotation
        )
    } catch (_: Exception) {
        null
    }
}

fun parseLegacyCanvasNoteStrokes(canvasDataJson: String): List<CanvasItem> {
    if (canvasDataJson.isBlank()) return emptyList()
    return try {
        val trimmed = canvasDataJson.trim()
        if (!trimmed.startsWith("[")) return emptyList()
        val arr = JSONArray(trimmed)
        val items = mutableListOf<CanvasItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val color = obj.optInt("color", -16777216)
            val width = obj.optDouble("width", 5.0).toFloat()
            val ptsArr = obj.optJSONArray("points") ?: continue
            val pts = mutableListOf<Offset>()
            for (j in 0 until ptsArr.length()) {
                val p = ptsArr.optJSONObject(j) ?: continue
                pts.add(Offset(p.optDouble("x", 0.0).toFloat(), p.optDouble("y", 0.0).toFloat()))
            }
            if (pts.isNotEmpty()) {
                items.add(
                    CanvasItem(
                        type = "STROKE",
                        points = pts,
                        colorArgb = color,
                        strokeWidth = width,
                        zIndex = i
                    )
                )
            }
        }
        items
    } catch (_: Exception) {
        emptyList()
    }
}

fun pushUndoSnapshot(
    elements: List<CanvasItem>,
    undoStack: MutableList<List<CanvasItem>>,
    redoStack: MutableList<List<CanvasItem>>
) {
    if (undoStack.size >= 30) {
        undoStack.removeAt(0)
    }
    undoStack.add(elements.toList())
    redoStack.clear()
}

internal fun decodeCanvasImageThumbnail(
    context: Context,
    storageManager: AttachmentStorageManager,
    pathOrUri: String,
    maxDimension: Int = 1024
): ImageBitmap? {
    return try {
        val optionsBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val file = if (!pathOrUri.startsWith("content://") && !pathOrUri.startsWith("file://")) {
            storageManager.getFileFromRelativePath(pathOrUri)
        } else {
            null
        }
        if (file != null && file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath, optionsBounds)
            val w = optionsBounds.outWidth
            val h = optionsBounds.outHeight
            if (w <= 0 || h <= 0) return null
            var sampleSize = 1
            while ((w / sampleSize) > maxDimension || (h / sampleSize) > maxDimension) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeFile(file.absolutePath, decodeOpts)?.asImageBitmap()
        } else if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
            val uri = pathOrUri.toUri()
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, optionsBounds)
            }
            val w = optionsBounds.outWidth
            val h = optionsBounds.outHeight
            if (w <= 0 || h <= 0) return null
            var sampleSize = 1
            while ((w / sampleSize) > maxDimension || (h / sampleSize) > maxDimension) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)?.asImageBitmap()
            }
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}

fun DrawScope.drawCanvasItem(
    item: CanvasItem,
    isSelected: Boolean,
    cachedImage: ImageBitmap? = null,
    isDark: Boolean = false
) {
    val rawColor = Color(item.colorArgb)
    val elementColor = if (isDark && item.colorArgb == android.graphics.Color.BLACK) {
        Color(0xFFF1F5F9)
    } else {
        rawColor
    }

    rotate(degrees = item.rotationDeg, pivot = if (item.points.isNotEmpty()) item.points.first() else item.start) {
        when (item.type) {
            "STROKE", "HIGHLIGHTER" -> {
                if (item.points.size > 1) {
                    val path = Path().apply {
                        moveTo(item.points.first().x, item.points.first().y)
                        for (p in item.points.drop(1)) {
                            lineTo(p.x, p.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = elementColor,
                        style = Stroke(
                            width = item.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                } else if (item.points.size == 1) {
                    drawCircle(
                        color = elementColor,
                        radius = item.strokeWidth / 2f,
                        center = item.points.first()
                    )
                }
            }
            "RECTANGLE" -> {
                val left = min(item.start.x, item.end.x)
                val top = min(item.start.y, item.end.y)
                val width = kotlin.math.abs(item.end.x - item.start.x)
                val height = kotlin.math.abs(item.end.y - item.start.y)
                if (item.fillColorArgb != null) {
                    drawRoundRect(
                        color = Color(item.fillColorArgb),
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }
                drawRoundRect(
                    color = elementColor,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(12f, 12f),
                    style = Stroke(width = item.strokeWidth)
                )
            }
            "STICKY" -> {
                val left = min(item.start.x, item.end.x)
                val top = min(item.start.y, item.end.y)
                val width = kotlin.math.abs(item.end.x - item.start.x).coerceAtLeast(120f)
                val height = kotlin.math.abs(item.end.y - item.start.y).coerceAtLeast(88f)
                val defaultLightSticky = Color(0xFFFEF3C7).toArgb()
                val fill = when {
                    isDark && (item.fillColorArgb == null || item.fillColorArgb == defaultLightSticky) -> Color(0xFF362C14)
                    item.fillColorArgb != null -> Color(item.fillColorArgb)
                    else -> Color(0xFFFEF3C7)
                }
                val stickyTextColor = if (isDark && (item.fillColorArgb == null || item.fillColorArgb == defaultLightSticky)) {
                    android.graphics.Color.rgb(254, 243, 199)
                } else {
                    android.graphics.Color.rgb(51, 35, 10)
                }
                // Soft drop shadow
                drawRoundRect(
                    color = Color(0x260F172A),
                    topLeft = Offset(left + 2f, top + 4f),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                drawRoundRect(
                    color = fill,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                drawRoundRect(
                    color = Color(0xFFF59E0B).copy(alpha = if (isDark) 0.55f else 0.65f),
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(14f, 14f),
                    style = Stroke(width = 1.8f)
                )
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = stickyTextColor
                        textSize = 15f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    val lines = item.text.ifBlank { "Sticky Note" }.chunked(18).take(4)
                    lines.forEachIndexed { idx, line ->
                        drawText(line, left + 12f, top + 28f + idx * 19f, paint)
                    }
                }
            }
            "CIRCLE" -> {
                val center = Offset((item.start.x + item.end.x) / 2f, (item.start.y + item.end.y) / 2f)
                val radius = hypot(item.end.x - item.start.x, item.end.y - item.start.y) / 2f
                drawCircle(
                    color = elementColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = item.strokeWidth)
                )
            }
            "LINE" -> {
                drawLine(
                    color = elementColor,
                    start = item.start,
                    end = item.end,
                    strokeWidth = item.strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            "ARROW" -> {
                drawLine(
                    color = elementColor,
                    start = item.start,
                    end = item.end,
                    strokeWidth = item.strokeWidth,
                    cap = StrokeCap.Round
                )
                val angle = atan2(item.end.y - item.start.y, item.end.x - item.start.x)
                val arrowSize = 20f
                val arrowAngle = Math.PI / 6
                val p1 = Offset(
                    (item.end.x - arrowSize * cos(angle - arrowAngle)).toFloat(),
                    (item.end.y - arrowSize * sin(angle - arrowAngle)).toFloat()
                )
                val p2 = Offset(
                    (item.end.x - arrowSize * cos(angle + arrowAngle)).toFloat(),
                    (item.end.y - arrowSize * sin(angle + arrowAngle)).toFloat()
                )
                drawLine(color = elementColor, start = item.end, end = p1, strokeWidth = item.strokeWidth, cap = StrokeCap.Round)
                drawLine(color = elementColor, start = item.end, end = p2, strokeWidth = item.strokeWidth, cap = StrokeCap.Round)
            }
            "CONNECTOR" -> {
                val midX = (item.start.x + item.end.x) / 2f
                val path = Path().apply {
                    moveTo(item.start.x, item.start.y)
                    lineTo(midX, item.start.y)
                    lineTo(midX, item.end.y)
                    lineTo(item.end.x, item.end.y)
                }
                drawPath(
                    path = path,
                    color = elementColor,
                    style = Stroke(width = item.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawCircle(color = elementColor, radius = item.strokeWidth * 1.4f, center = item.end)
            }
            "TEXT" -> {
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = elementColor.toArgb()
                        textSize = 24f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    drawText(item.text, item.start.x, item.start.y, paint)
                }
            }
            "IMAGE" -> {
                val left = min(item.start.x, item.end.x)
                val top = min(item.start.y, item.end.y)
                val width = kotlin.math.abs(item.end.x - item.start.x).coerceAtLeast(60f)
                val height = kotlin.math.abs(item.end.y - item.start.y).coerceAtLeast(60f)

                if (cachedImage != null) {
                    drawImage(
                        image = cachedImage,
                        dstOffset = androidx.compose.ui.unit.IntOffset(left.roundToInt(), top.roundToInt()),
                        dstSize = androidx.compose.ui.unit.IntSize(width.roundToInt(), height.roundToInt())
                    )
                    drawRoundRect(
                        color = Color(0xFF94A3B8),
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(width = 2f)
                    )
                } else {
                    drawRoundRect(
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    drawRoundRect(
                        color = if (isDark) Color(0xFF475569) else Color(0xFF94A3B8),
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(12f, 12f),
                        style = Stroke(width = 2f)
                    )
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = if (isDark) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
                            textSize = 20f
                            isAntiAlias = true
                        }
                        drawText("Image", left + 18f, top + 36f, paint)
                    }
                }
            }
        }

        if (isSelected) {
            val bounds = when (item.type) {
                "RECTANGLE", "IMAGE", "STICKY" -> Rect(
                    min(item.start.x, item.end.x),
                    min(item.start.y, item.end.y),
                    max(item.start.x, item.end.x),
                    max(item.start.y, item.end.y)
                )
                "CIRCLE" -> {
                    val c = Offset((item.start.x + item.end.x) / 2f, (item.start.y + item.end.y) / 2f)
                    val r = hypot(item.end.x - item.start.x, item.end.y - item.start.y) / 2f
                    Rect(c.x - r, c.y - r, c.x + r, c.y + r)
                }
                "TEXT" -> Rect(item.start.x - 6f, item.start.y - 28f, item.start.x + max(item.text.length * 14f, 60f) + 6f, item.start.y + 10f)
                else -> Rect(
                    min(item.start.x, item.end.x) - 10f,
                    min(item.start.y, item.end.y) - 10f,
                    max(item.start.x, item.end.x) + 10f,
                    max(item.start.y, item.end.y) + 10f
                )
            }

            drawRoundRect(
                color = if (isDark) Color(0xFF34D399) else Color(0xFF16A34A),
                topLeft = bounds.topLeft,
                size = bounds.size,
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 2.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            )
        }
    }
}

fun DrawScope.drawInfiniteGrid(
    pan: Offset,
    zoom: Float,
    viewportSize: Size,
    isDark: Boolean = false
) {
    val dotColor = if (isDark) Color(0xFF94A3B8).copy(alpha = 0.22f) else Color(0xFF64748B).copy(alpha = 0.16f)
    val spacing = 36f

    val left = (-pan.x / zoom) - spacing
    val top = (-pan.y / zoom) - spacing
    val right = ((-pan.x + viewportSize.width) / zoom) + spacing
    val bottom = ((-pan.y + viewportSize.height) / zoom) + spacing

    val startX = (left / spacing).toInt() * spacing
    val startY = (top / spacing).toInt() * spacing

    var x = startX
    while (x <= right) {
        var y = startY
        while (y <= bottom) {
            drawCircle(
                color = dotColor,
                radius = 1.4f / zoom.coerceAtLeast(0.5f),
                center = Offset(x, y)
            )
            y += spacing
        }
        x += spacing
    }
}

@Composable
fun ToolChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = if (isSelected) 1.2.dp else 0.7.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

