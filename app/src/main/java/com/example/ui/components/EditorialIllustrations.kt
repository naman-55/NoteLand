package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NotelandAmber
import com.example.ui.theme.NotelandBlue
import com.example.ui.theme.NotelandEmerald
import com.example.ui.theme.NotelandMint
import com.example.ui.theme.NotelandPurple
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun DrawScope.drawLeaf(
    origin: Offset,
    tip: Offset,
    width: Float,
    color: Color,
    veinColor: Color
) {
    val dx = tip.x - origin.x
    val dy = tip.y - origin.y
    val len = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
    val nx = -dy / len
    val ny = dx / len
    val midX = origin.x + dx * 0.48f
    val midY = origin.y + dy * 0.48f

    val path = Path().apply {
        moveTo(origin.x, origin.y)
        quadraticTo(
            midX + nx * width,
            midY + ny * width,
            tip.x,
            tip.y
        )
        quadraticTo(
            midX - nx * width,
            midY - ny * width,
            origin.x,
            origin.y
        )
        close()
    }
    drawPath(path, color = color)
    drawLine(
        color = veinColor,
        start = origin,
        end = Offset(origin.x + dx * 0.82f, origin.y + dy * 0.82f),
        strokeWidth = (width * 0.12f).coerceAtLeast(1.2f),
        cap = StrokeCap.Round
    )
}

@Composable
fun BotanicalLeafCornerAccent(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "botanicalSway")
    val swayDeg by infiniteTransition.animateFloat(
        initialValue = -2.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swayDeg"
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val mainLeaf = if (isDark) NotelandMint.copy(alpha = 0.28f) else NotelandEmerald.copy(alpha = 0.34f)
        val softLeaf = if (isDark) NotelandEmerald.copy(alpha = 0.22f) else NotelandMint.copy(alpha = 0.38f)
        val vein = if (isDark) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.55f)

        rotate(degrees = swayDeg, pivot = Offset(w * 0.96f, h * 0.85f)) {
            drawLeaf(
                origin = Offset(w * 0.96f, h * 0.78f),
                tip = Offset(w * 0.44f, h * 0.12f),
                width = w * 0.165f,
                color = softLeaf,
                veinColor = vein
            )
            drawLeaf(
                origin = Offset(w * 0.98f, h * 0.86f),
                tip = Offset(w * 0.22f, h * 0.42f),
                width = w * 0.145f,
                color = mainLeaf,
                veinColor = vein
            )
        }
        rotate(degrees = -swayDeg * 0.8f, pivot = Offset(w * 0.84f, h * 0.96f)) {
            drawLeaf(
                origin = Offset(w * 0.84f, h * 0.96f),
                tip = Offset(w * 0.68f, h * 0.20f),
                width = w * 0.115f,
                color = mainLeaf,
                veinColor = vein
            )
        }
    }
}

@Composable
fun HeroCardLeafWatermark(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val leafFill = Color.White.copy(alpha = 0.14f)
        val leafVein = Color.White.copy(alpha = 0.24f)

        drawLeaf(
            origin = Offset(w * 0.92f, h * 0.94f),
            tip = Offset(w * 0.34f, h * 0.16f),
            width = w * 0.18f,
            color = leafFill,
            veinColor = leafVein
        )
        drawLeaf(
            origin = Offset(w * 0.86f, h * 0.96f),
            tip = Offset(w * 0.14f, h * 0.48f),
            width = w * 0.14f,
            color = leafFill,
            veinColor = leafVein
        )
    }
}

@Composable
fun StudioHeaderIllustration(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = 132.dp, height = 78.dp)) {
        val w = size.width
        val h = size.height

        // Soft lavender glow behind desk items
        drawCircle(
            color = NotelandPurple.copy(alpha = if (isDark) 0.20f else 0.16f),
            radius = h * 0.44f,
            center = Offset(w * 0.56f, h * 0.50f)
        )

        // Stylus / pencil cup on left
        rotate(degrees = -12f, pivot = Offset(w * 0.20f, h * 0.48f)) {
            drawRoundRect(
                color = NotelandBlue,
                topLeft = Offset(w * 0.17f, h * 0.18f),
                size = Size(w * 0.045f, h * 0.34f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
        rotate(degrees = 10f, pivot = Offset(w * 0.24f, h * 0.48f)) {
            drawRoundRect(
                color = NotelandAmber,
                topLeft = Offset(w * 0.23f, h * 0.14f),
                size = Size(w * 0.045f, h * 0.38f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
        drawRoundRect(
            color = if (isDark) Color(0xFF28243E) else Color(0xFFDFD6FE),
            topLeft = Offset(w * 0.12f, h * 0.44f),
            size = Size(w * 0.18f, h * 0.34f),
            cornerRadius = CornerRadius(10f, 10f)
        )

        // Tablet / Canvas Frame in center
        rotate(degrees = -5f, pivot = Offset(w * 0.54f, h * 0.52f)) {
            val frameLeft = w * 0.31f
            val frameTop = h * 0.22f
            val frameW = w * 0.44f
            val frameH = h * 0.56f
            drawRoundRect(
                color = if (isDark) Color(0xFF242838) else Color(0xFF334155),
                topLeft = Offset(frameLeft, frameTop),
                size = Size(frameW, frameH),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF1A2234), Color(0xFF231D38))
                    } else {
                        listOf(Color(0xFFFFFFFF), Color(0xFFF3EEFF))
                    }
                ),
                topLeft = Offset(frameLeft + 4f, frameTop + 4f),
                size = Size(frameW - 8f, frameH - 8f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            // Sketch node accents on screen
            drawCircle(
                color = NotelandPurple.copy(alpha = 0.78f),
                radius = 7f,
                center = Offset(frameLeft + frameW * 0.34f, frameTop + frameH * 0.44f)
            )
            drawCircle(
                color = NotelandEmerald.copy(alpha = 0.85f),
                radius = 5.5f,
                center = Offset(frameLeft + frameW * 0.68f, frameTop + frameH * 0.34f)
            )
            drawCircle(
                color = NotelandAmber.copy(alpha = 0.85f),
                radius = 5.5f,
                center = Offset(frameLeft + frameW * 0.66f, frameTop + frameH * 0.68f)
            )
            drawLine(
                color = NotelandBlue.copy(alpha = 0.65f),
                start = Offset(frameLeft + frameW * 0.34f, frameTop + frameH * 0.44f),
                end = Offset(frameLeft + frameW * 0.68f, frameTop + frameH * 0.34f),
                strokeWidth = 2.5f
            )
            drawLine(
                color = NotelandBlue.copy(alpha = 0.65f),
                start = Offset(frameLeft + frameW * 0.34f, frameTop + frameH * 0.44f),
                end = Offset(frameLeft + frameW * 0.66f, frameTop + frameH * 0.68f),
                strokeWidth = 2.5f
            )
        }

        // Small potted leaf plant on right
        val leafTone = if (isDark) NotelandMint.copy(alpha = 0.85f) else NotelandEmerald
        val leafLight = if (isDark) Color(0xFF6EE7B7) else NotelandMint
        drawLeaf(
            origin = Offset(w * 0.84f, h * 0.56f),
            tip = Offset(w * 0.74f, h * 0.14f),
            width = w * 0.06f,
            color = leafLight,
            veinColor = Color.White.copy(alpha = 0.4f)
        )
        drawLeaf(
            origin = Offset(w * 0.84f, h * 0.56f),
            tip = Offset(w * 0.88f, h * 0.08f),
            width = w * 0.065f,
            color = leafTone,
            veinColor = Color.White.copy(alpha = 0.4f)
        )
        drawLeaf(
            origin = Offset(w * 0.86f, h * 0.56f),
            tip = Offset(w * 0.97f, h * 0.24f),
            width = w * 0.055f,
            color = leafLight,
            veinColor = Color.White.copy(alpha = 0.4f)
        )
        drawRoundRect(
            color = if (isDark) Color(0xFF475569) else Color(0xFFE2E8F0),
            topLeft = Offset(w * 0.77f, h * 0.54f),
            size = Size(w * 0.16f, h * 0.24f),
            cornerRadius = CornerRadius(8f, 8f)
        )
    }
}

@Composable
fun ProductivityHeaderIllustration(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "productivityAmbient")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lampGlow"
    )
    Canvas(modifier = modifier.size(width = 112.dp, height = 84.dp)) {
        val w = size.width
        val h = size.height

        // Hanging warm lamp cord & cone at top-left
        drawLine(
            color = if (isDark) Color.White.copy(alpha = 0.35f) else Color(0xFF475569),
            start = Offset(w * 0.26f, 0f),
            end = Offset(w * 0.26f, h * 0.16f),
            strokeWidth = 2.5f
        )
        // Warm glow cone
        val glowPath = Path().apply {
            moveTo(w * 0.16f, h * 0.26f)
            lineTo(w * 0.36f, h * 0.26f)
            lineTo(w * 0.46f, h * 0.58f)
            lineTo(w * 0.06f, h * 0.58f)
            close()
        }
        drawPath(
            glowPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    NotelandAmber.copy(alpha = glowAlpha),
                    Color.Transparent
                )
            )
        )
        val shadePath = Path().apply {
            moveTo(w * 0.20f, h * 0.15f)
            lineTo(w * 0.32f, h * 0.15f)
            lineTo(w * 0.39f, h * 0.26f)
            lineTo(w * 0.13f, h * 0.26f)
            close()
        }
        drawPath(shadePath, color = if (isDark) Color(0xFF334155) else Color(0xFF3F5E53))
        drawCircle(
            color = NotelandAmber,
            radius = 5f,
            center = Offset(w * 0.26f, h * 0.27f)
        )

        // Desk clock center-bottom
        val clockCenter = Offset(w * 0.44f, h * 0.72f)
        val clockRadius = h * 0.21f
        drawCircle(
            color = if (isDark) Color(0xFF28241C) else Color(0xFFF6E7CC),
            radius = clockRadius,
            center = clockCenter
        )
        drawCircle(
            color = if (isDark) Color(0xFF15181C) else Color.White,
            radius = clockRadius * 0.78f,
            center = clockCenter
        )
        drawLine(
            color = NotelandEmerald,
            start = clockCenter,
            end = Offset(clockCenter.x, clockCenter.y - clockRadius * 0.48f),
            strokeWidth = 2.4f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = NotelandAmber,
            start = clockCenter,
            end = Offset(clockCenter.x + clockRadius * 0.38f, clockCenter.y + clockRadius * 0.14f),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )

        // Plant leaves on right
        val leafMain = if (isDark) NotelandMint.copy(alpha = 0.85f) else NotelandEmerald
        val leafAccent = if (isDark) Color(0xFF6EE7B7) else NotelandMint
        drawLeaf(
            origin = Offset(w * 0.78f, h * 0.62f),
            tip = Offset(w * 0.66f, h * 0.22f),
            width = w * 0.08f,
            color = leafAccent,
            veinColor = Color.White.copy(alpha = 0.35f)
        )
        drawLeaf(
            origin = Offset(w * 0.78f, h * 0.62f),
            tip = Offset(w * 0.82f, h * 0.16f),
            width = w * 0.08f,
            color = leafMain,
            veinColor = Color.White.copy(alpha = 0.35f)
        )
        drawLeaf(
            origin = Offset(w * 0.80f, h * 0.62f),
            tip = Offset(w * 0.96f, h * 0.32f),
            width = w * 0.07f,
            color = leafAccent,
            veinColor = Color.White.copy(alpha = 0.35f)
        )
        drawRoundRect(
            color = if (isDark) Color(0xFF334155) else Color(0xFFD5EBE0),
            topLeft = Offset(w * 0.69f, h * 0.60f),
            size = Size(w * 0.20f, h * 0.32f),
            cornerRadius = CornerRadius(10f, 10f)
        )
    }
}

@Composable
fun SettingsHeaderIllustration(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "settingsGearSpin")
    val gearAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gearAngle"
    )
    Canvas(modifier = modifier.size(width = 112.dp, height = 64.dp)) {
        val w = size.width
        val h = size.height

        // Soft leaf behind gear
        drawLeaf(
            origin = Offset(w * 0.84f, h * 0.82f),
            tip = Offset(w * 0.86f, h * 0.14f),
            width = w * 0.085f,
            color = if (isDark) NotelandMint.copy(alpha = 0.75f) else NotelandMint,
            veinColor = Color.White.copy(alpha = 0.35f)
        )
        drawLeaf(
            origin = Offset(w * 0.86f, h * 0.82f),
            tip = Offset(w * 0.97f, h * 0.34f),
            width = w * 0.07f,
            color = if (isDark) NotelandEmerald.copy(alpha = 0.85f) else NotelandEmerald,
            veinColor = Color.White.copy(alpha = 0.35f)
        )

        // Rounded solid card tile holding Settings teal gear
        drawRoundRect(
            color = if (isDark) com.example.ui.theme.NoteSurfaceDark else Color.White,
            topLeft = Offset(w * 0.18f, h * 0.10f),
            size = Size(w * 0.56f, h * 0.80f),
            cornerRadius = CornerRadius(18f, 18f)
        )

        val gearCenter = Offset(w * 0.46f, h * 0.50f)
        val outerR = h * 0.27f
        val innerR = h * 0.19f
        val gearColor = if (isDark) com.example.ui.theme.DarkSettingsAccent else com.example.ui.theme.LightSettingsAccent
        val teeth = 8
        rotate(degrees = gearAngle, pivot = gearCenter) {
            for (i in 0 until teeth) {
                val angle = (2.0 * PI * i) / teeth
                val tx = gearCenter.x + cos(angle).toFloat() * (outerR * 0.92f)
                val ty = gearCenter.y + sin(angle).toFloat() * (outerR * 0.92f)
                drawCircle(
                    color = gearColor,
                    radius = h * 0.065f,
                    center = Offset(tx, ty)
                )
            }
        }
        drawCircle(
            color = gearColor,
            radius = innerR,
            center = gearCenter
        )
        drawCircle(
            color = if (isDark) com.example.ui.theme.NoteSurfaceDark else Color.White,
            radius = h * 0.085f,
            center = gearCenter
        )

        // Small planter pot
        drawRoundRect(
            color = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
            topLeft = Offset(w * 0.76f, h * 0.66f),
            size = Size(w * 0.18f, h * 0.24f),
            cornerRadius = CornerRadius(7f, 7f)
        )
    }
}

@Composable
fun KanbanTodoEmptyIllustration(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "todoFloat")
    val bobY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobY"
    )
    Canvas(
        modifier = modifier
            .size(width = 110.dp, height = 96.dp)
            .graphicsLayer { translationY = bobY }
    ) {
        val w = size.width
        val h = size.height
        val leafColor = if (isDark) NotelandMint.copy(alpha = 0.34f) else NotelandMint.copy(alpha = 0.42f)
        val accentLeaf = if (isDark) NotelandEmerald.copy(alpha = 0.32f) else NotelandEmerald.copy(alpha = 0.28f)

        // Decorative leaves behind clipboard
        drawLeaf(Offset(w * 0.32f, h * 0.78f), Offset(w * 0.08f, h * 0.42f), w * 0.08f, leafColor, Color.White.copy(alpha = 0.3f))
        drawLeaf(Offset(w * 0.68f, h * 0.78f), Offset(w * 0.92f, h * 0.36f), w * 0.08f, accentLeaf, Color.White.copy(alpha = 0.3f))
        drawLeaf(Offset(w * 0.70f, h * 0.80f), Offset(w * 0.94f, h * 0.60f), w * 0.065f, leafColor, Color.White.copy(alpha = 0.3f))

        // Soft ground shadow oval
        drawOval(
            color = NotelandEmerald.copy(alpha = if (isDark) 0.18f else 0.12f),
            topLeft = Offset(w * 0.18f, h * 0.82f),
            size = Size(w * 0.64f, h * 0.12f)
        )

        // Clipboard back & sheet
        drawRoundRect(
            color = if (isDark) Color(0xFF1B3A2E) else Color(0xFFBFE9D5),
            topLeft = Offset(w * 0.28f, h * 0.16f),
            size = Size(w * 0.44f, h * 0.68f),
            cornerRadius = CornerRadius(14f, 14f)
        )
        drawRoundRect(
            color = if (isDark) Color(0xFF141918) else Color.White,
            topLeft = Offset(w * 0.32f, h * 0.22f),
            size = Size(w * 0.36f, h * 0.56f),
            cornerRadius = CornerRadius(9f, 9f)
        )
        // Top clip
        drawRoundRect(
            color = NotelandEmerald,
            topLeft = Offset(w * 0.40f, h * 0.11f),
            size = Size(w * 0.20f, h * 0.11f),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // Checklist lines + checkmarks
        listOf(0.36f, 0.50f, 0.64f).forEach { yRatio ->
            val cy = h * yRatio
            drawCircle(
                color = NotelandEmerald,
                radius = 5f,
                center = Offset(w * 0.40f, cy)
            )
            drawLine(
                color = if (isDark) Color(0xFF4B5563) else Color(0xFFD1E5DA),
                start = Offset(w * 0.48f, cy),
                end = Offset(w * 0.62f, cy),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun KanbanProgressEmptyIllustration(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "progressFloat")
    val bobY by infiniteTransition.animateFloat(
        initialValue = 3f,
        targetValue = -3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobY"
    )
    Canvas(
        modifier = modifier
            .size(width = 110.dp, height = 96.dp)
            .graphicsLayer { translationY = bobY }
    ) {
        val w = size.width
        val h = size.height
        val leafColor = if (isDark) NotelandAmber.copy(alpha = 0.28f) else NotelandMint.copy(alpha = 0.36f)
        val accentLeaf = if (isDark) NotelandMint.copy(alpha = 0.26f) else NotelandAmber.copy(alpha = 0.28f)

        drawLeaf(Offset(w * 0.30f, h * 0.78f), Offset(w * 0.08f, h * 0.40f), w * 0.08f, leafColor, Color.White.copy(alpha = 0.3f))
        drawLeaf(Offset(w * 0.70f, h * 0.78f), Offset(w * 0.92f, h * 0.42f), w * 0.075f, accentLeaf, Color.White.copy(alpha = 0.3f))

        drawOval(
            color = NotelandAmber.copy(alpha = if (isDark) 0.18f else 0.14f),
            topLeft = Offset(w * 0.18f, h * 0.82f),
            size = Size(w * 0.64f, h * 0.12f)
        )

        // Hourglass bulbs
        val topBulb = Path().apply {
            moveTo(w * 0.34f, h * 0.20f)
            lineTo(w * 0.66f, h * 0.20f)
            lineTo(w * 0.52f, h * 0.48f)
            lineTo(w * 0.48f, h * 0.48f)
            close()
        }
        val bottomBulb = Path().apply {
            moveTo(w * 0.48f, h * 0.52f)
            lineTo(w * 0.52f, h * 0.52f)
            lineTo(w * 0.66f, h * 0.80f)
            lineTo(w * 0.34f, h * 0.80f)
            close()
        }
        drawPath(topBulb, color = if (isDark) Color(0xFF282012) else Color(0xFFFEF3C7))
        drawPath(bottomBulb, color = NotelandAmber.copy(alpha = 0.82f))
        drawPath(topBulb, color = NotelandAmber, style = Stroke(width = 3.2f))
        drawPath(bottomBulb, color = NotelandAmber, style = Stroke(width = 3.2f))

        // Top & bottom caps
        drawRoundRect(
            color = Color(0xFFD97706),
            topLeft = Offset(w * 0.30f, h * 0.15f),
            size = Size(w * 0.40f, h * 0.07f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawRoundRect(
            color = Color(0xFFD97706),
            topLeft = Offset(w * 0.30f, h * 0.78f),
            size = Size(w * 0.40f, h * 0.07f),
            cornerRadius = CornerRadius(6f, 6f)
        )
    }
}
