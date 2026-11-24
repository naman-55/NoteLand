package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SmoothEaseOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val SmoothEaseInOut = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

/**
 * Pure, full-bleed starting animation presenting only the NoteLand splash artwork
 * with a smooth GPU scale glide and cross-dissolve into the workspace.
 */
@Composable
fun StartingAnimationOverlay(
    onFinished: () -> Unit,
    durationMs: Long = 1600L
) {
    var isExiting by remember { mutableStateOf(false) }

    val introProgress = remember { Animatable(0f) }
    val exitProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            introProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMs.toInt(), easing = SmoothEaseOut)
            )
        }
        delay(durationMs - 120L)
        isExiting = true
    }

    LaunchedEffect(isExiting) {
        if (isExiting) {
            exitProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, easing = SmoothEaseInOut)
            )
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 1f - exitProgress.value
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .background(Color(0xFFCFE7FD))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isExiting = true
            }
            .testTag("starting_animation_overlay")
    ) {
        Image(
            painter = painterResource(id = R.drawable.noteland_splash),
            contentDescription = "NoteLand starting animation",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val t = introProgress.value
                    val ex = exitProgress.value
                    val scale = 1.05f - (0.038f * t) - (0.012f * ex)
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}
