package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.isAppInDarkTheme
import kotlin.math.roundToInt

@Composable
fun PinDialog(
    title: String = "Unlock Locked Folder",
    message: String = "Enter your 4-digit PIN to access your locked notes",
    onConfirm: (String) -> Boolean,
    onDismiss: () -> Unit,
    onBiometricClick: (() -> Unit)? = null,
    showDefaultPinHint: Boolean = false,
    confirmButtonText: String = "Unlock"
) {
    var pinText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("Incorrect PIN. Please try again.") }
    var errorPulse by remember { mutableIntStateOf(0) }
    var isFieldFocused by remember { mutableStateOf(false) }
    val isDark = isAppInDarkTheme
    val focusRequester = remember { FocusRequester() }
    val heroScale = remember { Animatable(0.90f) }
    val shakeOffset = remember { Animatable(0f) }

    BackHandler(onBack = onDismiss)

    LaunchedEffect(Unit) {
        heroScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    LaunchedEffect(errorPulse) {
        if (errorPulse > 0) {
            val pattern = listOf(-14f, 14f, -10f, 10f, -5f, 5f, 0f)
            for (target in pattern) {
                shakeOffset.animateTo(target, animationSpec = tween(42))
            }
        }
    }

    val cleanMessage = remember(message) {
        message
            .replace(Regex("\\s*\\(Default:\\s*1234\\)", RegexOption.IGNORE_CASE), "")
            .trim()
            .ifBlank { "Enter your 4-digit PIN to access your locked notes" }
    }

    val submitPin: () -> Unit = {
        if (pinText.length < 4) {
            errorMessage = "Please enter all 4 digits."
            isError = true
            errorPulse++
        } else {
            val ok = onConfirm(pinText)
            if (!ok) {
                errorMessage = "Incorrect PIN. Please try again."
                isError = true
                errorPulse++
            }
        }
    }

    val bgBrush = if (isDark) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to com.example.ui.theme.EditorialBackdropDarkTop,
                0.55f to com.example.ui.theme.EditorialBackdropDarkMid,
                0.70f to com.example.ui.theme.EditorialBackdropDarkLow,
                0.86f to com.example.ui.theme.EditorialBackdropDarkGreyMid,
                1.00f to com.example.ui.theme.EditorialBackdropDarkBottom
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xFFEEF5FF), Color(0xFFF8FBFF), Color(0xFFEAF2FF))
        )
    }

    val primaryHeadingColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF0C1B33)
    val accentBlue = if (isDark) Color(0xFF00D8F6) else Color(0xFF1677FF)
    val subtitleColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0xFF111214) else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Absorb touches so underlying screen cannot be tapped */ }
            .testTag("pin_blur_overlay")
    ) {
        // Atmospheric curved background shapes
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val topBlobColor = if (isDark) Color(0xFF0B353F).copy(alpha = 0.42f) else Color(0xFFD6E8FF).copy(alpha = 0.65f)
            drawCircle(
                color = topBlobColor,
                radius = w * 0.36f,
                center = Offset(w * 1.04f, h * 0.14f)
            )

            val waveBack = Path().apply {
                moveTo(0f, h * 0.87f)
                cubicTo(w * 0.25f, h * 0.83f, w * 0.65f, h * 0.94f, w, h * 0.86f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = waveBack,
                color = if (isDark) Color(0xFF0B252E).copy(alpha = 0.55f) else Color(0xFFD9EAFF).copy(alpha = 0.72f)
            )

            val waveFront = Path().apply {
                moveTo(0f, h * 0.92f)
                cubicTo(w * 0.38f, h * 0.96f, w * 0.72f, h * 0.88f, w, h * 0.91f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = waveFront,
                color = if (isDark) Color(0xFF0E343F).copy(alpha = 0.45f) else Color(0xFFC8E0FF).copy(alpha = 0.58f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top-left floating back button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF16181B) else Color.White,
                    shadowElevation = 6.dp,
                    border = BorderStroke(
                        0.8.dp,
                        if (isDark) Color.White.copy(alpha = 0.14f) else Color(0xFFE7EFFB)
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = primaryHeadingColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.35f))

            // 3D-feel Locked Folder + Shield hero illustration
            LockedFolderHeroBadge(
                isDark = isDark,
                modifier = Modifier
                    .size(164.dp)
                    .graphicsLayer {
                        scaleX = heroScale.value
                        scaleY = heroScale.value
                    }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Main Elevated Card
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = cardBg,
                tonalElevation = 0.dp,
                shadowElevation = if (isDark) 16.dp else 22.dp,
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.13f) else Color(0xFFEBF2FC)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Two-tone headline (last word highlighted in blue)
                    val titleWords = title.trim().split(" ")
                    val styledTitle = buildAnnotatedString {
                        if (titleWords.size > 1) {
                            withStyle(SpanStyle(color = primaryHeadingColor)) {
                                append(titleWords.dropLast(1).joinToString(" "))
                                append(" ")
                            }
                            withStyle(SpanStyle(color = accentBlue)) {
                                append(titleWords.last())
                            }
                        } else {
                            withStyle(SpanStyle(color = primaryHeadingColor)) {
                                append(title)
                            }
                        }
                    }

                    Text(
                        text = styledTitle,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.3).sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = cleanMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        ),
                        color = subtitleColor,
                        textAlign = TextAlign.Center
                    )

                    if (showDefaultPinHint) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) Color(0xFF0B353F) else Color(0xFFEEF5FF),
                            border = BorderStroke(
                                1.dp,
                                if (isDark) Color(0xFF00D8F6).copy(alpha = 0.55f) else Color(0xFFD9E9FF)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF00D8F6) else Color(0xFF1D6CF5),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Default PIN: 1234",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp
                                    ),
                                    color = if (isDark) Color(0xFF67F2FF) else Color(0xFF1D6CF5)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Strictly 4 Rounded-Square PIN Digit Boxes backed by BasicTextField
                    val boxCount = 4
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                try {
                                    focusRequester.requestFocus()
                                } catch (_: Exception) {}
                            }
                    ) {
                        BasicTextField(
                            value = pinText,
                            onValueChange = { incoming ->
                                if (incoming.length <= 4 && incoming.all { ch -> ch.isDigit() }) {
                                    pinText = incoming
                                    isError = false
                                }
                            },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
                            cursorBrush = SolidColor(Color.Transparent),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { submitPin() }),
                            modifier = Modifier
                                .matchParentSize()
                                .alpha(0.01f)
                                .focusRequester(focusRequester)
                                .onFocusChanged { isFieldFocused = it.isFocused }
                                .testTag("pin_input_field")
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val boxSize = 54.dp
                            for (index in 0 until boxCount) {
                                val isFilled = index < pinText.length
                                val isActiveSlot = isFieldFocused && index == pinText.length.coerceAtMost(boxCount - 1) && !isFilled

                                val borderColor = when {
                                    isError -> MaterialTheme.colorScheme.error
                                    isActiveSlot -> accentBlue
                                    isFilled -> if (isDark) Color(0xFF00D8F6).copy(alpha = 0.65f) else Color(0xFFC2DAFB)
                                    else -> if (isDark) Color(0xFF27343A) else Color(0xFFD9E6F8)
                                }
                                val boxFill = if (isDark) Color(0xFF090D0F) else Color(0xFFF7FAFF)

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = boxFill,
                                    border = BorderStroke(if (isActiveSlot || isError) 1.8.dp else 1.2.dp, borderColor),
                                    modifier = Modifier.size(boxSize)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isFilled) {
                                            Box(
                                                modifier = Modifier
                                                    .size(9.dp)
                                                    .clip(CircleShape)
                                                    .background(primaryHeadingColor)
                                            )
                                        } else if (isActiveSlot) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(20.dp)
                                                    .clip(CircleShape)
                                                    .background(accentBlue)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isError) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Vibrant Unlock Button
                    val unlockBrush = if (isDark) {
                        Brush.horizontalGradient(colors = listOf(Color(0xFF00D8F6), Color(0xFF00A8C6)))
                    } else {
                        Brush.horizontalGradient(colors = listOf(Color(0xFF2F80FF), Color(0xFF1252D4)))
                    }
                    val unlockContentColor = if (isDark) Color(0xFF001C22) else Color.White
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(
                                elevation = 10.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = accentBlue.copy(alpha = 0.35f),
                                spotColor = accentBlue.copy(alpha = 0.45f)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(unlockBrush)
                            .clickable(onClick = submitPin)
                            .padding(horizontal = 20.dp)
                            .testTag("pin_submit_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = unlockContentColor,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = confirmButtonText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = unlockContentColor
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = unlockContentColor,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(19.dp)
                        )
                    }

                    if (onBiometricClick != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onBiometricClick,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isDark) Color(0xFF00D8F6).copy(alpha = 0.48f) else Color(0xFFD5E5FB)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("biometric_unlock_overlay_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = accentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Unlock with Biometrics",
                                color = accentBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            color = subtitleColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.65f))
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LockedFolderHeroBadge(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.48f
        val cy = h * 0.50f

        // Concentric pastel halo rings
        drawCircle(
            color = if (isDark) Color(0xFF182C4F).copy(alpha = 0.55f) else Color(0xFFE3EEFF).copy(alpha = 0.65f),
            radius = w * 0.44f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = if (isDark) Color(0xFF1E3764).copy(alpha = 0.65f) else Color(0xFFD6E7FF).copy(alpha = 0.72f),
            radius = w * 0.34f,
            center = Offset(cx, cy)
        )

        // Back folder plate with top-left folder tab
        val backPath = Path().apply {
            val left = w * 0.20f
            val right = w * 0.76f
            val top = h * 0.25f
            val tabRight = w * 0.43f
            val notchBottom = h * 0.31f
            val bottom = h * 0.72f
            val r = w * 0.055f

            moveTo(left + r, top)
            lineTo(tabRight - r * 0.6f, top)
            quadraticTo(tabRight, top, tabRight + r * 0.7f, notchBottom)
            lineTo(right - r, notchBottom)
            quadraticTo(right, notchBottom, right, notchBottom + r)
            lineTo(right, bottom - r)
            quadraticTo(right, bottom, right - r, bottom)
            lineTo(left + r, bottom)
            quadraticTo(left, bottom, left, bottom - r)
            lineTo(left, top + r)
            quadraticTo(left, top, left + r, top)
            close()
        }
        drawPath(
            path = backPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF64ADFF), Color(0xFF2878F5)),
                startY = h * 0.25f,
                endY = h * 0.72f
            )
        )

        // Cream/white folded document sheet peeking inside the folder
        drawRoundRect(
            color = Color(0xFFF6FAFF),
            topLeft = Offset(w * 0.23f, h * 0.34f),
            size = Size(w * 0.49f, h * 0.16f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )

        // Front main 3D folder pocket
        val frontLeft = w * 0.18f
        val frontTop = h * 0.39f
        val frontWidth = w * 0.56f
        val frontHeight = h * 0.36f
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF5DA8FF), Color(0xFF1A65EE)),
                start = Offset(frontLeft, frontTop),
                end = Offset(frontLeft + frontWidth, frontTop + frontHeight)
            ),
            topLeft = Offset(frontLeft, frontTop),
            size = Size(frontWidth, frontHeight),
            cornerRadius = CornerRadius(w * 0.075f, w * 0.075f)
        )

        // Subtle top rim highlight on the front pocket
        drawRoundRect(
            color = Color.White.copy(alpha = 0.25f),
            topLeft = Offset(frontLeft + w * 0.02f, frontTop + h * 0.01f),
            size = Size(frontWidth - w * 0.04f, h * 0.025f),
            cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
        )

        // Centered white padlock on front pocket
        val lockCx = frontLeft + frontWidth * 0.50f
        val lockBodyW = w * 0.16f
        val lockBodyH = h * 0.13f
        val lockBodyTop = frontTop + frontHeight * 0.42f

        // Padlock shackle
        drawArc(
            color = Color.White,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(lockCx - lockBodyW * 0.31f, lockBodyTop - lockBodyH * 0.52f),
            size = Size(lockBodyW * 0.62f, lockBodyH * 0.95f),
            style = Stroke(width = w * 0.030f, cap = StrokeCap.Round)
        )
        // Padlock body
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(lockCx - lockBodyW / 2f, lockBodyTop),
            size = Size(lockBodyW, lockBodyH),
            cornerRadius = CornerRadius(w * 0.035f, w * 0.035f)
        )
        // Keyhole inside padlock
        drawCircle(
            color = Color(0xFF1F6FF2),
            radius = w * 0.018f,
            center = Offset(lockCx, lockBodyTop + lockBodyH * 0.42f)
        )
        drawRoundRect(
            color = Color(0xFF1F6FF2),
            topLeft = Offset(lockCx - w * 0.010f, lockBodyTop + lockBodyH * 0.45f),
            size = Size(w * 0.020f, lockBodyH * 0.32f),
            cornerRadius = CornerRadius(w * 0.01f, w * 0.01f)
        )

        // Overlapping Verified Shield Badge at bottom-right
        val scx = w * 0.72f
        val scy = h * 0.71f
        val sw = w * 0.21f
        val sh = h * 0.24f
        val shieldPath = Path().apply {
            moveTo(scx, scy - sh * 0.52f)
            lineTo(scx + sw * 0.48f, scy - sh * 0.34f)
            quadraticTo(scx + sw * 0.50f, scy + sh * 0.24f, scx, scy + sh * 0.52f)
            quadraticTo(scx - sw * 0.50f, scy + sh * 0.24f, scx - sw * 0.48f, scy - sh * 0.34f)
            close()
        }
        drawPath(
            path = shieldPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFC5E0FF), Color(0xFF7CB4FF)),
                startY = scy - sh * 0.52f,
                endY = scy + sh * 0.52f
            )
        )
        drawPath(
            path = shieldPath,
            color = Color.White.copy(alpha = 0.78f),
            style = Stroke(width = w * 0.014f, join = StrokeJoin.Round)
        )

        // Checkmark inside shield
        val checkPath = Path().apply {
            moveTo(scx - sw * 0.21f, scy + sh * 0.02f)
            lineTo(scx - sw * 0.04f, scy + sh * 0.19f)
            lineTo(scx + sw * 0.24f, scy - sh * 0.14f)
        }
        drawPath(
            path = checkPath,
            color = Color.White,
            style = Stroke(width = w * 0.026f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
