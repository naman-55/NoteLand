package com.example.ui.screens.mindmap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withScale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Draws the luminous studio backdrop matching the high-res reference screenshots:
 * subtle mint-kissed zenith, pure clean center, and soft organic mint waves along the edges.
 */
fun DrawScope.drawMindMapBackdrop(
    theme: MindMapThemePreset,
    isDark: Boolean,
    showMintWaves: Boolean,
    surfaceColor: Color = Color.Unspecified,
    backgroundColor: Color = Color.Unspecified
) {
    val bgColors = if (isDark) {
        val baseBg = if (backgroundColor != Color.Unspecified) backgroundColor else Color(0xFF0A0D0C)
        val baseSurface = if (surfaceColor != Color.Unspecified) surfaceColor else Color(0xFF101513)
        listOf(
            lerp(baseBg, theme.rootStart, 0.05f),
            lerp(baseSurface, theme.rootStart, 0.04f),
            lerp(baseSurface, theme.rootEnd, 0.08f)
        )
    } else {
        val baseSurface = if (surfaceColor != Color.Unspecified) surfaceColor else Color(0xFFFFFFFF)
        val baseBg = if (backgroundColor != Color.Unspecified) backgroundColor else theme.canvasTint
        listOf(
            lerp(baseSurface, theme.rootStart, 0.06f),
            baseSurface,
            lerp(baseBg, theme.canvasTint, 0.65f)
        )
    }
    drawRect(
        brush = Brush.verticalGradient(colors = bgColors),
        size = size
    )

    if (!showMintWaves) return

    val waveTint1 = if (isDark) theme.rootStart.copy(alpha = 0.11f) else theme.rootStart.copy(alpha = 0.08f)
    val waveTint2 = if (isDark) theme.rootEnd.copy(alpha = 0.09f) else Color(0xFF34D399).copy(alpha = 0.10f)

    val wavePath1 = Path().apply {
        moveTo(0f, size.height * 0.80f)
        cubicTo(
            size.width * 0.28f, size.height * 0.72f,
            size.width * 0.58f, size.height * 0.94f,
            size.width, size.height * 0.78f
        )
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(path = wavePath1, color = waveTint1)

    val wavePath2 = Path().apply {
        moveTo(0f, size.height * 0.88f)
        cubicTo(
            size.width * 0.36f, size.height * 0.96f,
            size.width * 0.74f, size.height * 0.76f,
            size.width, size.height * 0.86f
        )
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(path = wavePath2, color = waveTint2)
}

/**
 * Draws all Mind Map branch connectors between parent and child nodes.
 */
fun DrawScope.drawMindMapBranches(
    nodes: List<MindMapNode>,
    config: MindMapStyleConfig,
    isDark: Boolean = false
) {
    val byId = nodes.associateBy { it.id }
    val pathEffect = when (config.lineStyle) {
        MindMapLineStyle.SOLID -> null
        MindMapLineStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
        MindMapLineStyle.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(3.5f, 7f))
    }

    nodes.forEach { child ->
        val parent = child.parentId?.let { byId[it] } ?: return@forEach
        val branchColor = config.overrideLineColorArgb?.let { Color(it) } ?: Color(child.colorArgb)
        val strokeW = (if (child.level <= 1) config.lineWidthPx * 1.45f else config.lineWidthPx * 0.76f)
            .coerceIn(1.8f, 11f)

        val pBounds = parent.bounds()
        val cBounds = child.bounds()
        val (startPt, endPt) = computeAnchorPoints(parent, child, pBounds, cBounds, config.layoutType)

        val effectiveConnector = if (config.layoutType == MindMapLayoutType.ORG_CHART &&
            config.connectorType == MindMapConnectorType.CURVED
        ) {
            MindMapConnectorType.ORTHOGONAL
        } else {
            config.connectorType
        }

        val path = Path()
        var tangentFrom = startPt
        val isVerticalFlow = config.layoutType == MindMapLayoutType.HIERARCHICAL ||
            config.layoutType == MindMapLayoutType.ORG_CHART ||
            child.side == 2

        when (effectiveConnector) {
            MindMapConnectorType.STRAIGHT, MindMapConnectorType.BIDIRECTIONAL -> {
                path.moveTo(startPt.x, startPt.y)
                path.lineTo(endPt.x, endPt.y)
                tangentFrom = startPt
            }
            MindMapConnectorType.ORTHOGONAL -> {
                if (isVerticalFlow) {
                    val midY = (startPt.y + endPt.y) / 2f
                    path.moveTo(startPt.x, startPt.y)
                    path.lineTo(startPt.x, midY)
                    path.lineTo(endPt.x, midY)
                    path.lineTo(endPt.x, endPt.y)
                    tangentFrom = Offset(endPt.x, midY)
                } else {
                    val midX = (startPt.x + endPt.x) / 2f
                    path.moveTo(startPt.x, startPt.y)
                    path.lineTo(midX, startPt.y)
                    path.lineTo(midX, endPt.y)
                    path.lineTo(endPt.x, endPt.y)
                    tangentFrom = Offset(midX, endPt.y)
                }
            }
            MindMapConnectorType.SPLINE -> {
                path.moveTo(startPt.x, startPt.y)
                if (isVerticalFlow) {
                    val dy = (endPt.y - startPt.y) * 0.65f
                    val cp1 = Offset(startPt.x, startPt.y + dy)
                    val cp2 = Offset(endPt.x, endPt.y - dy)
                    path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, endPt.x, endPt.y)
                    tangentFrom = cp2
                } else {
                    val dx = (endPt.x - startPt.x) * 0.68f
                    val cp1 = Offset(startPt.x + dx, startPt.y + (endPt.y - startPt.y) * 0.12f)
                    val cp2 = Offset(endPt.x - dx, endPt.y - (endPt.y - startPt.y) * 0.12f)
                    path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, endPt.x, endPt.y)
                    tangentFrom = cp2
                }
            }
            MindMapConnectorType.CURVED -> {
                path.moveTo(startPt.x, startPt.y)
                if (isVerticalFlow) {
                    val midY = (startPt.y + endPt.y) / 2f
                    path.cubicTo(startPt.x, midY, endPt.x, midY, endPt.x, endPt.y)
                    tangentFrom = Offset(endPt.x, midY)
                } else if (parent.level == 0) {
                    // Organic graceful arch out of the root pill matching screenshots
                    val cp1 = Offset(startPt.x, endPt.y)
                    val cp2 = Offset((startPt.x + endPt.x) * 0.5f, endPt.y)
                    path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, endPt.x, endPt.y)
                    tangentFrom = cp2
                } else {
                    val dx = (endPt.x - startPt.x) * 0.52f
                    val cp1 = Offset(startPt.x + dx, startPt.y)
                    val cp2 = Offset(endPt.x - dx, endPt.y)
                    path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, endPt.x, endPt.y)
                    tangentFrom = cp2
                }
            }
        }

        drawPath(
            path = path,
            color = branchColor,
            style = Stroke(
                width = strokeW,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = pathEffect
            )
        )

        val resolvedArrow = if (effectiveConnector == MindMapConnectorType.BIDIRECTIONAL &&
            config.arrowhead == MindMapArrowhead.NONE
        ) {
            MindMapArrowhead.OPEN_ARROW
        } else {
            config.arrowhead
        }

        if (resolvedArrow != MindMapArrowhead.NONE) {
            val hollowFill = if (isDark) Color(0xFF101513) else Color.White
            drawArrowHead(tangentFrom, endPt, branchColor, strokeW, resolvedArrow, hollowFill)
            if (effectiveConnector == MindMapConnectorType.BIDIRECTIONAL) {
                drawArrowHead(endPt, startPt, branchColor, strokeW, resolvedArrow, hollowFill)
            }
        }
    }
}

private fun computeAnchorPoints(
    parent: MindMapNode,
    child: MindMapNode,
    pBounds: Rect,
    cBounds: Rect,
    layout: MindMapLayoutType
): Pair<Offset, Offset> {
    if (child.side == 2 && layout == MindMapLayoutType.RADIAL) {
        return Offset(parent.x, pBounds.bottom) to Offset(child.x, cBounds.top)
    }
    return when (layout) {
        MindMapLayoutType.HIERARCHICAL, MindMapLayoutType.ORG_CHART -> {
            Offset(parent.x, pBounds.bottom) to Offset(child.x, cBounds.top)
        }
        MindMapLayoutType.TIMELINE -> {
            if (parent.level == 0) {
                Offset(pBounds.right, parent.y) to Offset(child.x, if (child.y < parent.y) cBounds.bottom else cBounds.top)
            } else {
                Offset(parent.x, if (child.y < parent.y) pBounds.top else pBounds.bottom) to
                    Offset(child.x, if (child.y < parent.y) cBounds.bottom else cBounds.top)
            }
        }
        else -> {
            val childRightOfParent = child.x >= parent.x
            val startX = if (parent.level == 0) {
                if (childRightOfParent) parent.x + 18f else parent.x - 18f
            } else {
                if (childRightOfParent) pBounds.right else pBounds.left
            }
            val startY = if (parent.level == 0) {
                if (child.y < parent.y - 10f) pBounds.top + 4f
                else if (child.y > parent.y + 10f) pBounds.bottom - 4f
                else parent.y
            } else {
                parent.y
            }
            val endX = if (childRightOfParent) cBounds.left else cBounds.right
            Offset(startX, startY) to Offset(endX, child.y)
        }
    }
}

private fun DrawScope.drawArrowHead(
    from: Offset,
    tip: Offset,
    color: Color,
    strokeWidth: Float,
    style: MindMapArrowhead,
    hollowFillColor: Color = Color.White
) {
    val angle = atan2(tip.y - from.y, tip.x - from.x)
    val len = max(11f, strokeWidth * 3.1f)
    val spread = PI / 6.5
    val p1 = Offset(
        (tip.x - len * cos(angle - spread)).toFloat(),
        (tip.y - len * sin(angle - spread)).toFloat()
    )
    val p2 = Offset(
        (tip.x - len * cos(angle + spread)).toFloat(),
        (tip.y - len * sin(angle + spread)).toFloat()
    )

    when (style) {
        MindMapArrowhead.NONE -> Unit
        MindMapArrowhead.DOT -> {
            drawCircle(color = color, radius = max(4.5f, strokeWidth * 1.35f), center = tip)
        }
        MindMapArrowhead.OPEN_ARROW -> {
            drawLine(color, tip, p1, strokeWidth = strokeWidth, cap = StrokeCap.Round)
            drawLine(color, tip, p2, strokeWidth = strokeWidth, cap = StrokeCap.Round)
        }
        MindMapArrowhead.FILLED_ARROW -> {
            val tri = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                close()
            }
            drawPath(tri, color = color)
        }
        MindMapArrowhead.HOLLOW_TRIANGLE -> {
            val tri = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                close()
            }
            drawPath(tri, color = hollowFillColor)
            drawPath(tri, color = color, style = Stroke(width = strokeWidth))
        }
        MindMapArrowhead.DIAMOND -> {
            val back = Offset(
                (tip.x - len * 1.4f * cos(angle)).toFloat(),
                (tip.y - len * 1.4f * sin(angle)).toFloat()
            )
            val d1 = Offset(
                (tip.x - len * 0.7f * cos(angle - PI / 4.5)).toFloat(),
                (tip.y - len * 0.7f * sin(angle - PI / 4.5)).toFloat()
            )
            val d2 = Offset(
                (tip.x - len * 0.7f * cos(angle + PI / 4.5)).toFloat(),
                (tip.y - len * 0.7f * sin(angle + PI / 4.5)).toFloat()
            )
            val diamond = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(d1.x, d1.y)
                lineTo(back.x, back.y)
                lineTo(d2.x, d2.y)
                close()
            }
            drawPath(diamond, color = color)
        }
    }
}

/**
 * Draws a single glossy/pastel Mind Map node with crisp vector icons, luminous root halo,
 * and 3D pastel pill polish matching the 5 reference screenshots.
 */
fun DrawScope.drawMindMapNode(
    node: MindMapNode,
    config: MindMapStyleConfig,
    isSelected: Boolean,
    isSearchMatch: Boolean,
    isDark: Boolean,
    collapsedChildCount: Int = 0
) {
    val b = node.bounds()
    val accent = Color(node.colorArgb)
    val radius = when (node.shape) {
        MindMapNodeShape.PILL -> b.height / 2f
        MindMapNodeShape.ROUNDED -> 14f
        MindMapNodeShape.DIAMOND -> 12f
        MindMapNodeShape.UNDERLINE -> 8f
    }

    // Luminous soft outer glow & 3D tactile depth shadow
    if (node.shape != MindMapNodeShape.UNDERLINE) {
        if (node.level == 0) {
            // Soft cyan-emerald aura around Main Topic (matching screenshots)
            drawRoundRect(
                color = Color(0xFF34D399).copy(alpha = 0.22f),
                topLeft = Offset(b.left - 7f, b.top - 5f),
                size = Size(b.width + 14f, b.height + 14f),
                cornerRadius = CornerRadius(radius + 7f, radius + 7f)
            )
            drawRoundRect(
                color = accent.copy(alpha = 0.20f),
                topLeft = Offset(b.left - 3f, b.top + 5f),
                size = Size(b.width + 6f, b.height + 6f),
                cornerRadius = CornerRadius(radius + 3f, radius + 3f)
            )
        } else {
            // Soft tinted drop glow
            drawRoundRect(
                color = accent.copy(alpha = if (node.level == 1) 0.22f else 0.13f),
                topLeft = Offset(b.left - 1f, b.top + if (node.level == 1) 4.5f else 2.5f),
                size = Size(b.width + 2f, b.height + 2f),
                cornerRadius = CornerRadius(radius + 1f, radius + 1f)
            )
            // Subtle 3D bottom lip on primary Topic pills
            if (node.level == 1 && config.fillStyle != MindMapFillStyle.OUTLINE) {
                drawRoundRect(
                    color = accent.copy(alpha = 0.82f),
                    topLeft = Offset(b.left, b.top + 2.5f),
                    size = Size(b.width, b.height),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
        }
    }

    val fillBrush = when {
        node.level == 0 -> Brush.verticalGradient(
            colors = listOf(
                lerp(config.themePreset.rootStart, Color(0xFF34D399), 0.18f),
                config.themePreset.rootStart,
                config.themePreset.rootEnd
            ),
            startY = b.top,
            endY = b.bottom
        )
        config.fillStyle == MindMapFillStyle.SOLID -> Brush.verticalGradient(
            colors = listOf(accent, lerp(accent, Color.Black, 0.14f)),
            startY = b.top,
            endY = b.bottom
        )
        config.fillStyle == MindMapFillStyle.OUTLINE -> Brush.verticalGradient(
            colors = listOf(
                if (isDark) Color(0xFF16221D) else Color.White,
                if (isDark) Color(0xFF16221D) else Color.White
            )
        )
        config.fillStyle == MindMapFillStyle.PASTEL -> {
            val tint = if (isDark) lerp(Color(0xFF17221E), accent, 0.28f) else lerp(Color.White, accent, 0.16f)
            Brush.verticalGradient(listOf(tint, tint))
        }
        else -> {
            val topMix = if (node.level == 1) 0.10f else 0.08f
            val bottomMix = if (node.level == 1) 0.28f else 0.20f
            val base = if (isDark) Color(0xFF16221D) else Color.White
            Brush.verticalGradient(
                colors = listOf(lerp(base, accent, topMix), lerp(base, accent, bottomMix)),
                startY = b.top,
                endY = b.bottom
            )
        }
    }

    when (node.shape) {
        MindMapNodeShape.DIAMOND -> {
            val diamond = Path().apply {
                moveTo(node.x, b.top - 8f)
                lineTo(b.right + 12f, node.y)
                lineTo(node.x, b.bottom + 8f)
                lineTo(b.left - 12f, node.y)
                close()
            }
            drawPath(diamond, brush = fillBrush)
            if (config.borderWidthPx > 0f) {
                drawPath(
                    diamond,
                    color = if (node.level == 0) Color.White else accent.copy(alpha = 0.78f),
                    style = Stroke(width = if (node.level == 0) 3.2f else config.borderWidthPx)
                )
            }
        }
        MindMapNodeShape.UNDERLINE -> {
            drawLine(
                color = accent,
                start = Offset(b.left + 4f, b.bottom),
                end = Offset(b.right - 4f, b.bottom),
                strokeWidth = max(2.5f, config.borderWidthPx * 1.2f),
                cap = StrokeCap.Round
            )
        }
        else -> {
            drawRoundRect(
                brush = fillBrush,
                topLeft = b.topLeft,
                size = Size(b.width, b.height),
                cornerRadius = CornerRadius(radius, radius)
            )
            // Soft glossy sheen on Root pill
            if (node.level == 0) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                        startY = b.top,
                        endY = b.top + b.height * 0.48f
                    ),
                    topLeft = Offset(b.left + 5f, b.top + 3f),
                    size = Size(b.width - 10f, b.height * 0.44f),
                    cornerRadius = CornerRadius(radius, radius)
                )
                // Signature crisp outer ring around Main Topic
                drawRoundRect(
                    color = if (isDark) Color.White.copy(alpha = 0.85f) else Color.White,
                    topLeft = b.topLeft,
                    size = Size(b.width, b.height),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = 3.2f)
                )
            } else if (config.borderWidthPx > 0f) {
                val borderColor = if (node.level == 1) accent.copy(alpha = 0.82f) else accent.copy(alpha = 0.55f)
                drawRoundRect(
                    color = borderColor,
                    topLeft = b.topLeft,
                    size = Size(b.width, b.height),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = if (node.level == 1) max(1.6f, config.borderWidthPx * 0.85f) else config.borderWidthPx * 0.65f)
                )
            }
        }
    }

    // Selection or Search highlight halo
    if (isSelected || isSearchMatch) {
        val ringColor = when {
            isSearchMatch -> if (isDark) Color(0xFFFBBF24) else Color(0xFFF59E0B)
            isDark -> Color(0xFF34D399)
            else -> Color(0xFF16A34A)
        }
        drawRoundRect(
            color = ringColor,
            topLeft = Offset(b.left - 5f, b.top - 5f),
            size = Size(b.width + 10f, b.height + 10f),
            cornerRadius = CornerRadius(radius + 5f, radius + 5f),
            style = Stroke(
                width = 2.6f,
                pathEffect = if (isSelected) PathEffect.dashPathEffect(floatArrayOf(8f, 6f)) else null
            )
        )
    }

    // Crisp vector icon + centered typography
    val textColorArgb = when {
        node.level == 0 -> android.graphics.Color.WHITE
        config.fillStyle == MindMapFillStyle.SOLID -> android.graphics.Color.WHITE
        isDark -> android.graphics.Color.rgb(241, 245, 249)
        else -> android.graphics.Color.rgb(20, 29, 43)
    }
    val vectorIconTint = when {
        node.level == 0 -> Color.White
        config.fillStyle == MindMapFillStyle.SOLID -> Color.White
        else -> accent
    }
    val hasKnownVector = node.iconKey in setOf("brain", "target", "bulb", "book", "chart", "rocket", "check")

    if (node.level == 0 && hasKnownVector) {
        drawNodeVectorIcon(node.iconKey, Offset(node.x, node.y - 14f), 22f, vectorIconTint)
    }

    drawContext.canvas.nativeCanvas.apply {
        val paint = Paint().apply {
            color = textColorArgb
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = if (config.boldText || node.level <= 1) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            textSize = when (node.level) {
                0 -> 21.5f * config.textScale
                1 -> 15.5f * config.textScale
                else -> 13f * config.textScale
            }
        }

        if (node.level == 0) {
            if (hasKnownVector) {
                drawText(node.title, node.x, node.y + 21f, paint)
            } else {
                val glyph = resolveMindMapIconGlyph(node.iconKey)
                if (glyph.isNotBlank()) {
                    val iconPaint = Paint(paint).apply { textSize = 19f * config.textScale }
                    drawText(glyph, node.x, node.y - 6f, iconPaint)
                    drawText(node.title, node.x, node.y + 20f, paint)
                } else {
                    val fm = paint.fontMetrics
                    drawText(node.title, node.x, node.y - (fm.ascent + fm.descent) / 2f, paint)
                }
            }
        } else if (hasKnownVector) {
            val textWidth = paint.measureText(node.title)
            val iconW = 18f
            val gap = 7f
            val totalW = iconW + gap + textWidth
            val iconCenterX = node.x - totalW / 2f + iconW / 2f
            val textCenterX = iconCenterX + iconW / 2f + gap + textWidth / 2f
            val fm = paint.fontMetrics
            val baseline = node.y - (fm.ascent + fm.descent) / 2f
            drawText(node.title, textCenterX, baseline, paint)
            // Draw crisp vector icon to the left of title
            drawNodeVectorIcon(node.iconKey, Offset(iconCenterX, node.y), 16f, vectorIconTint)
        } else {
            val glyph = resolveMindMapIconGlyph(node.iconKey)
            val displayText = if (glyph.isNotBlank()) "$glyph ${node.title}" else node.title
            val fm = paint.fontMetrics
            val baseline = node.y - (fm.ascent + fm.descent) / 2f
            drawText(displayText, node.x, baseline, paint)
        }

        if (node.isLocked) {
            val lockPaint = Paint().apply {
                isAntiAlias = true
                textSize = 11f
                textAlign = Paint.Align.RIGHT
            }
            drawText("🔒", b.right - 6f, b.top + 13f, lockPaint)
        }
    }

    // Collapsed branch badge (+N)
    if (node.isCollapsed && collapsedChildCount > 0) {
        val badgeX = if (node.side < 0) b.left - 12f else b.right + 12f
        val badgeY = node.y
        val badgeBg = if (isDark) Color(0xFF1A2822) else Color.White
        drawCircle(color = badgeBg, radius = 11f, center = Offset(badgeX, badgeY))
        drawCircle(color = accent, radius = 11f, center = Offset(badgeX, badgeY), style = Stroke(width = 1.8f))
        drawContext.canvas.nativeCanvas.apply {
            val countPaint = Paint().apply {
                color = if (isDark) android.graphics.Color.rgb(241, 245, 249) else accent.toArgb()
                textSize = 10.5f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            val fm = countPaint.fontMetrics
            drawText("+$collapsedChildCount", badgeX, badgeY - (fm.ascent + fm.descent) / 2f, countPaint)
        }
    }
}

/**
 * Draws clean, resolution-independent vector icons inside Mind Map nodes matching the screenshots
 * (white brain silhouette, purple bullseye target, amber bulb, blue open book, pink bar chart, rocket, check).
 */
private fun DrawScope.drawNodeVectorIcon(
    iconKey: String,
    center: Offset,
    sizePx: Float,
    color: Color
) {
    val r = sizePx / 2f
    val stroke = Stroke(width = max(1.8f, sizePx * 0.11f), cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (iconKey) {
        "brain" -> {
            // Left outline lobe + right filled/crisp lobe matching screenshot Main Topic brain emblem
            val leftPath = Path().apply {
                moveTo(center.x - 1f, center.y - r * 0.72f)
                cubicTo(center.x - r * 0.9f, center.y - r * 0.95f, center.x - r * 1.15f, center.y - r * 0.1f, center.x - r * 0.65f, center.y + r * 0.1f)
                cubicTo(center.x - r * 1.05f, center.y + r * 0.45f, center.x - r * 0.65f, center.y + r * 0.98f, center.x - 1f, center.y + r * 0.72f)
                close()
            }
            val rightPath = Path().apply {
                moveTo(center.x + 1f, center.y - r * 0.72f)
                cubicTo(center.x + r * 0.9f, center.y - r * 0.95f, center.x + r * 1.15f, center.y - r * 0.1f, center.x + r * 0.65f, center.y + r * 0.1f)
                cubicTo(center.x + r * 1.05f, center.y + r * 0.45f, center.x + r * 0.65f, center.y + r * 0.98f, center.x + 1f, center.y + r * 0.72f)
                close()
            }
            drawPath(leftPath, color = color, style = stroke)
            drawPath(rightPath, color = color)
        }
        "target" -> {
            drawCircle(color = color, radius = r * 0.92f, center = center, style = stroke)
            drawCircle(color = color, radius = r * 0.50f, center = center, style = stroke)
            drawCircle(color = color, radius = r * 0.18f, center = center)
            drawLine(color, Offset(center.x + r * 0.2f, center.y - r * 0.2f), Offset(center.x + r * 0.95f, center.y - r * 0.95f), strokeWidth = 1.8f, cap = StrokeCap.Round)
        }
        "bulb" -> {
            drawCircle(color = color, radius = r * 0.62f, center = Offset(center.x, center.y - r * 0.2f), style = stroke)
            drawLine(color, Offset(center.x - r * 0.34f, center.y + r * 0.56f), Offset(center.x + r * 0.34f, center.y + r * 0.56f), strokeWidth = 1.9f, cap = StrokeCap.Round)
            drawLine(color, Offset(center.x - r * 0.24f, center.y + r * 0.85f), Offset(center.x + r * 0.24f, center.y + r * 0.85f), strokeWidth = 1.9f, cap = StrokeCap.Round)
        }
        "book" -> {
            val leftPage = Path().apply {
                moveTo(center.x, center.y - r * 0.45f)
                quadraticTo(center.x - r * 0.5f, center.y - r * 0.75f, center.x - r * 0.95f, center.y - r * 0.45f)
                lineTo(center.x - r * 0.95f, center.y + r * 0.55f)
                quadraticTo(center.x - r * 0.5f, center.y + r * 0.3f, center.x, center.y + r * 0.58f)
                close()
            }
            val rightPage = Path().apply {
                moveTo(center.x, center.y - r * 0.45f)
                quadraticTo(center.x + r * 0.5f, center.y - r * 0.75f, center.x + r * 0.95f, center.y - r * 0.45f)
                lineTo(center.x + r * 0.95f, center.y + r * 0.55f)
                quadraticTo(center.x + r * 0.5f, center.y + r * 0.3f, center.x, center.y + r * 0.58f)
                close()
            }
            drawPath(leftPage, color, style = stroke)
            drawPath(rightPage, color, style = stroke)
        }
        "chart" -> {
            val barW = r * 0.42f
            drawRoundRect(color, Offset(center.x - r * 0.9f, center.y + r * 0.05f), Size(barW, r * 0.75f), CornerRadius(2f), style = stroke)
            drawRoundRect(color, Offset(center.x - barW / 2f, center.y - r * 0.35f), Size(barW, r * 1.15f), CornerRadius(2f), style = stroke)
            drawRoundRect(color, Offset(center.x + r * 0.48f, center.y - r * 0.75f), Size(barW, r * 1.55f), CornerRadius(2f), style = stroke)
        }
        "rocket" -> {
            val body = Path().apply {
                moveTo(center.x + r * 0.8f, center.y - r * 0.8f)
                quadraticTo(center.x + r * 0.1f, center.y - r * 0.7f, center.x - r * 0.55f, center.y + r * 0.1f)
                lineTo(center.x - r * 0.1f, center.y + r * 0.55f)
                quadraticTo(center.x + r * 0.7f, center.y - r * 0.1f, center.x + r * 0.8f, center.y - r * 0.8f)
                close()
            }
            drawPath(body, color, style = stroke)
            drawCircle(color, radius = r * 0.18f, center = Offset(center.x + r * 0.18f, center.y - r * 0.18f))
        }
        "check" -> {
            drawCircle(color, radius = r * 0.85f, center = center, style = stroke)
            val tick = Path().apply {
                moveTo(center.x - r * 0.38f, center.y + r * 0.02f)
                lineTo(center.x - r * 0.08f, center.y + r * 0.34f)
                lineTo(center.x + r * 0.44f, center.y - r * 0.28f)
            }
            drawPath(tick, color, style = stroke)
        }
    }
}

/**
 * Exports the current Mind Map to a crisp high-resolution file in app cache for saving/sharing.
 */
fun exportMindMap(
    context: Context,
    title: String,
    nodes: List<MindMapNode>,
    config: MindMapStyleConfig,
    format: String = "png"
): File? {
    return try {
        val bounds = computeNodesBoundingBox(nodes) ?: Rect(-320f, -220f, 320f, 220f)
        val margin = 96f
        val worldW = (bounds.width + margin * 2f).coerceAtLeast(640f)
        val worldH = (bounds.height + margin * 2f).coerceAtLeast(420f)
        val scale = (1800f / max(worldW, worldH)).coerceIn(1.5f, 3.0f)
        val bmpW = (worldW * scale).toInt().coerceIn(800, 2400)
        val bmpH = (worldH * scale).toInt().coerceIn(600, 2400)

        val bitmap = createBitmap(bmpW, bmpH)
        val canvas = AndroidCanvas(bitmap)

        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, bmpH.toFloat(),
                android.graphics.Color.WHITE,
                config.themePreset.canvasTint.toArgb(),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, bmpW.toFloat(), bmpH.toFloat(), bgPaint)

        canvas.withScale(scale, scale) {
        translate(-bounds.left + margin, -bounds.top + margin)

        val byId = nodes.associateBy { it.id }
        val linePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        nodes.forEach { child ->
            val parent = child.parentId?.let { byId[it] } ?: return@forEach
            val (s, e) = computeAnchorPoints(parent, child, parent.bounds(), child.bounds(), config.layoutType)
            linePaint.color = config.overrideLineColorArgb ?: child.colorArgb
            linePaint.strokeWidth = if (child.level <= 1) config.lineWidthPx * 1.4f else config.lineWidthPx * 0.78f
            val p = AndroidPath().apply {
                moveTo(s.x, s.y)
                val dx = (e.x - s.x) * 0.52f
                cubicTo(s.x + dx, s.y, e.x - dx, e.y, e.x, e.y)
            }
            canvas.drawPath(p, linePaint)
        }

        val fillPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
        val strokePaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE }
        val textPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        nodes.forEach { node ->
            val b = node.bounds()
            val rectF = RectF(b.left, b.top, b.right, b.bottom)
            val rad = b.height / 2f
            val accent = Color(node.colorArgb)
            if (node.level == 0) {
                fillPaint.shader = LinearGradient(
                    b.left, b.top, b.left, b.bottom,
                    config.themePreset.rootStart.toArgb(),
                    config.themePreset.rootEnd.toArgb(),
                    Shader.TileMode.CLAMP
                )
            } else {
                fillPaint.shader = LinearGradient(
                    b.left, b.top, b.left, b.bottom,
                    lerp(Color.White, accent, 0.12f).toArgb(),
                    lerp(Color.White, accent, 0.28f).toArgb(),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(rectF, rad, rad, fillPaint)
            fillPaint.shader = null

            strokePaint.color = if (node.level == 0) android.graphics.Color.WHITE else accent.copy(alpha = 0.78f).toArgb()
            strokePaint.strokeWidth = if (node.level == 0) 3.2f else max(1.5f, config.borderWidthPx)
            canvas.drawRoundRect(rectF, rad, rad, strokePaint)

            textPaint.color = if (node.level == 0) android.graphics.Color.WHITE else android.graphics.Color.rgb(20, 29, 43)
            textPaint.textSize = when (node.level) {
                0 -> 21f
                1 -> 15.5f
                else -> 13f
            }
            val glyph = resolveMindMapIconGlyph(node.iconKey)
            if (node.level == 0 && glyph.isNotBlank()) {
                canvas.drawText(glyph, node.x, node.y - 6f, textPaint)
                canvas.drawText(node.title, node.x, node.y + 20f, textPaint)
            } else {
                val label = if (glyph.isNotBlank()) "$glyph ${node.title}" else node.title
                val fm = textPaint.fontMetrics
                canvas.drawText(label, node.x, node.y - (fm.ascent + fm.descent) / 2f, textPaint)
            }
        }
        }

        val exportDir = File(context.cacheDir, "mindmap_exports").apply { mkdirs() }
        val safeName = title.replace(Regex("[^A-Za-z0-9_\\-]"), "_").ifBlank { "MindMap" }
        
        val outFile = when (format.lowercase()) {
            "jpg", "jpeg" -> {
                val file = File(exportDir, "${safeName}_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
                }
                file
            }
            "pdf" -> {
                val file = File(exportDir, "${safeName}_${System.currentTimeMillis()}.pdf")
                val pdfDocument = android.graphics.pdf.PdfDocument()
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(bmpW, bmpH, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
                FileOutputStream(file).use { fos ->
                    pdfDocument.writeTo(fos)
                }
                pdfDocument.close()
                file
            }
            else -> {
                val file = File(exportDir, "${safeName}_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
                file
            }
        }
        bitmap.recycle()
        outFile
    } catch (_: Exception) {
        null
    }
}
