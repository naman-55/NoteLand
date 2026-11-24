package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Tactile spring compression feedback when a card or pill button is pressed.
 */
fun Modifier.springPressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.965f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "springPressScale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Smooth, non-blocking staggered entrance reveal for screen sections and cards.
 * Keeps composables placed immediately for accessibility and UI test stability while
 * animating alpha, subtle vertical translation, and micro-scale on screen entry.
 */
@Composable
fun StaggeredSectionEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    animateOnEnter: Boolean = true,
    content: @Composable () -> Unit
) {
    if (!animateOnEnter) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val delayMs = (index * 36).coerceIn(0, 180)
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 260,
            delayMillis = delayMs,
            easing = FastOutSlowInEasing
        ),
        label = "staggeredEntrance_$index"
    )
    val offsetPx = with(LocalDensity.current) { 14.dp.toPx() }

    Box(
        modifier = modifier.graphicsLayer {
            alpha = 0.08f + (0.92f * progress)
            translationY = (1f - progress) * offsetPx
            val s = 0.975f + (0.025f * progress)
            scaleX = s
            scaleY = s
        }
    ) {
        content()
    }
}
