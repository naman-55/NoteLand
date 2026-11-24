package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.theme.EditorialDarkGlassCard
import com.example.ui.theme.editorialBackdropBrush
import com.example.ui.theme.isAppInDarkTheme
import kotlin.math.roundToInt

private enum class ChangePinStep(
    val index: Int,
    val label: String,
    val headlinePrefix: String,
    val headlineAccent: String,
    val subtitle: String,
    val ctaLabel: String
) {
    CURRENT(
        index = 0,
        label = "Current",
        headlinePrefix = "Verify Current",
        headlineAccent = "PIN",
        subtitle = "Enter your current 4-digit Vault PIN to authorize this change.",
        ctaLabel = "Verify & Continue"
    ),
    NEW(
        index = 1,
        label = "New",
        headlinePrefix = "Create New",
        headlineAccent = "PIN",
        subtitle = "Choose a new 4-digit passcode for your Locked Folder.",
        ctaLabel = "Continue"
    ),
    CONFIRM(
        index = 2,
        label = "Confirm",
        headlinePrefix = "Confirm New",
        headlineAccent = "PIN",
        subtitle = "Re-enter your new 4-digit passcode to save it.",
        ctaLabel = "Update PIN"
    )
}

@Composable
fun ChangePinOverlay(
    onDismiss: () -> Unit,
    onUpdatePin: (String, String) -> Boolean,
    onVerifyCurrentPin: ((String) -> Boolean)? = null
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme

    val isSetupMode = onVerifyCurrentPin == null
    var currentStep by remember { mutableStateOf(if (isSetupMode) ChangePinStep.NEW else ChangePinStep.CURRENT) }
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showDigits by remember { mutableStateOf(false) }

    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var errorPulse by remember { mutableIntStateOf(0) }
    var isFieldFocused by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val heroScale = remember { Animatable(0.90f) }
    val shakeOffset = remember { Animatable(0f) }

    val activePinValue = when (currentStep) {
        ChangePinStep.CURRENT -> currentPin
        ChangePinStep.NEW -> newPin
        ChangePinStep.CONFIRM -> confirmPin
    }

    val handleBackNavigation: () -> Unit = {
        isError = false
        when (currentStep) {
            ChangePinStep.CURRENT -> onDismiss()
            ChangePinStep.NEW -> {
                if (isSetupMode) onDismiss() else currentStep = ChangePinStep.CURRENT
            }
            ChangePinStep.CONFIRM -> {
                confirmPin = ""
                currentStep = ChangePinStep.NEW
            }
        }
    }

    BackHandler(onBack = handleBackNavigation)

    LaunchedEffect(Unit) {
        heroScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    LaunchedEffect(currentStep) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(errorPulse) {
        if (errorPulse > 0) {
            val pattern = listOf(-14f, 14f, -10f, 10f, -5f, 5f, 0f)
            for (target in pattern) {
                shakeOffset.animateTo(target, animationSpec = tween(42))
            }
        }
    }

    val submitCurrentStep: () -> Unit = {
        when (currentStep) {
            ChangePinStep.CURRENT -> {
                if (currentPin.length < 4) {
                    errorMessage = "Please enter all 4 digits of your current PIN."
                    isError = true
                    errorPulse++
                } else if (onVerifyCurrentPin != null && !onVerifyCurrentPin(currentPin)) {
                    errorMessage = "Current PIN is incorrect. Please try again."
                    isError = true
                    errorPulse++
                } else {
                    isError = false
                    currentStep = ChangePinStep.NEW
                }
            }

            ChangePinStep.NEW -> {
                if (newPin.length < 4) {
                    errorMessage = "Please enter a 4-digit new PIN."
                    isError = true
                    errorPulse++
                } else {
                    isError = false
                    currentStep = ChangePinStep.CONFIRM
                }
            }

            ChangePinStep.CONFIRM -> {
                if (confirmPin.length < 4) {
                    errorMessage = "Please re-enter all 4 digits to confirm."
                    isError = true
                    errorPulse++
                } else if (confirmPin != newPin) {
                    errorMessage = "PINs do not match. Please try again."
                    isError = true
                    errorPulse++
                } else {
                    val updated = onUpdatePin(currentPin, newPin)
                    if (updated) {
                        Toast.makeText(context, "Vault PIN updated successfully!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } else {
                        currentStep = ChangePinStep.CURRENT
                        confirmPin = ""
                        errorMessage = "Current PIN is incorrect. Please verify again."
                        isError = true
                        errorPulse++
                    }
                }
            }
        }
    }

    val backdropBrush = editorialBackdropBrush(isDark)
    val primaryHeadingColor = if (isDark) Color(0xFFF3F4F6) else MaterialTheme.colorScheme.onBackground
    val subtitleColor = if (isDark) Color(0xFF9CA3AF) else MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary
    val warmGold = Color(0xFFF59E0B)
    val cardBg = if (isDark) EditorialDarkGlassCard else MaterialTheme.colorScheme.surface
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.13f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backdropBrush)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* absorb clicks behind overlay */ }
            .testTag("change_pin_overlay")
    ) {
        // Subtle ambient security curves anchored toward the lower grey band in dark mode
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                color = (if (isDark) warmGold else accentColor).copy(alpha = if (isDark) 0.08f else 0.09f),
                radius = w * 0.34f,
                center = Offset(w * 0.94f, h * 0.16f)
            )

            val wavePath = Path().apply {
                moveTo(0f, h * 0.86f)
                cubicTo(w * 0.28f, h * 0.81f, w * 0.68f, h * 0.94f, w, h * 0.85f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = wavePath,
                color = if (isDark) Color.White.copy(alpha = 0.04f) else accentColor.copy(alpha = 0.06f)
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
            // Top bar with Back action and current step label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    onClick = handleBackNavigation,
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF16181B) else MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                    border = BorderStroke(
                        0.8.dp,
                        if (isDark) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("change_pin_back_button")
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

                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF16181B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    border = BorderStroke(0.8.dp, cardBorder)
                ) {
                    Text(
                        text = "Step ${currentStep.index + 1} of 3",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = subtitleColor,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.28f))

            ChangePinHeroBadge(
                isDark = isDark,
                accentColor = accentColor,
                modifier = Modifier
                    .size(136.dp)
                    .graphicsLayer {
                        scaleX = heroScale.value
                        scaleY = heroScale.value
                    }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Elevated Editorial Card
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = cardBg,
                tonalElevation = 0.dp,
                shadowElevation = if (isDark) 16.dp else 20.dp,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 390.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 3-Step Segmented Indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isDark) Color(0xFF181A1E) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ChangePinStep.entries.forEach { step ->
                            if (isSetupMode && step == ChangePinStep.CURRENT) return@forEach
                            val isActive = step == currentStep
                            val isCompleted = step.index < currentStep.index
                            val canTapToReturn = isCompleted
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    isActive -> accentColor
                                    isCompleted -> accentColor.copy(alpha = if (isDark) 0.18f else 0.14f)
                                    else -> Color.Transparent
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .then(
                                        if (canTapToReturn) {
                                            Modifier.clickable {
                                                isError = false
                                                if (step == ChangePinStep.CURRENT) {
                                                    confirmPin = ""
                                                } else if (step == ChangePinStep.NEW) {
                                                    confirmPin = ""
                                                }
                                                currentStep = step
                                            }
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .testTag("change_pin_step_${step.index}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp)
                                ) {
                                    if (isCompleted) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = step.label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isActive || isCompleted) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = when {
                                            isActive -> MaterialTheme.colorScheme.onPrimary
                                            isCompleted -> accentColor
                                            else -> subtitleColor
                                        },
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            val forward = targetState.index >= initialState.index
                            (slideInHorizontally(tween(220)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(180)))
                                .togetherWith(
                                    slideOutHorizontally(tween(180)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(140))
                                )
                        },
                        label = "change_pin_header_transition"
                    ) { step ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val styledTitle = buildAnnotatedString {
                                withStyle(SpanStyle(color = primaryHeadingColor)) {
                                    append(step.headlinePrefix)
                                    append(" ")
                                }
                                withStyle(SpanStyle(color = accentColor)) {
                                    append(step.headlineAccent)
                                }
                            }

                            Text(
                                text = styledTitle,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.3).sp
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = step.subtitle,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.5.sp,
                                    lineHeight = 19.sp
                                ),
                                color = subtitleColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // 4 Rounded-Square PIN Digit Boxes backed by BasicTextField
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
                                } catch (_: Exception) {
                                }
                            }
                    ) {
                        BasicTextField(
                            value = activePinValue,
                            onValueChange = { incoming ->
                                if (incoming.length <= 4 && incoming.all { ch -> ch.isDigit() }) {
                                    when (currentStep) {
                                        ChangePinStep.CURRENT -> currentPin = incoming
                                        ChangePinStep.NEW -> newPin = incoming
                                        ChangePinStep.CONFIRM -> confirmPin = incoming
                                    }
                                    isError = false
                                }
                            },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
                            cursorBrush = SolidColor(Color.Transparent),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = if (currentStep == ChangePinStep.CONFIRM) ImeAction.Done else ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { submitCurrentStep() },
                                onDone = { submitCurrentStep() }
                            ),
                            modifier = Modifier
                                .matchParentSize()
                                .alpha(0.01f)
                                .focusRequester(focusRequester)
                                .onFocusChanged { isFieldFocused = it.isFocused }
                                .testTag("change_pin_input_field")
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val boxSize = 54.dp
                            for (index in 0 until boxCount) {
                                val isFilled = index < activePinValue.length
                                val isActiveSlot = isFieldFocused && index == activePinValue.length.coerceAtMost(boxCount - 1) && !isFilled

                                val borderColor = when {
                                    isError -> MaterialTheme.colorScheme.error
                                    isActiveSlot -> accentColor
                                    isFilled -> accentColor.copy(alpha = if (isDark) 0.65f else 0.72f)
                                    else -> if (isDark) Color(0xFF282B31) else MaterialTheme.colorScheme.outlineVariant
                                }
                                val boxFill = if (isDark) Color(0xFF090A0C) else MaterialTheme.colorScheme.surfaceContainerLowest

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = boxFill,
                                    border = BorderStroke(if (isActiveSlot || isError) 1.8.dp else 1.2.dp, borderColor),
                                    modifier = Modifier.size(boxSize)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isFilled) {
                                            if (showDigits) {
                                                Text(
                                                    text = activePinValue[index].toString(),
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 20.sp
                                                    ),
                                                    color = primaryHeadingColor
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(9.dp)
                                                        .clip(CircleShape)
                                                        .background(primaryHeadingColor)
                                                )
                                            }
                                        } else if (isActiveSlot) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(20.dp)
                                                    .clip(CircleShape)
                                                    .background(accentColor)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Show/Hide PIN digits chip
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { showDigits = !showDigits }
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (showDigits) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showDigits) "Hide PIN digits" else "Show PIN digits",
                            tint = subtitleColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (showDigits) "Hide digits" else "Show digits",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = subtitleColor,
                            modifier = Modifier.testTag("change_pin_visibility_toggle")
                        )
                    }

                    if (isError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary CTA Button
                    val ctaBrush = if (isDark) {
                        Brush.horizontalGradient(colors = listOf(Color(0xFF00D8F6), Color(0xFF00A8C6)))
                    } else {
                        Brush.horizontalGradient(colors = listOf(accentColor, accentColor.copy(alpha = 0.85f)))
                    }
                    val ctaContentColor = if (isDark) Color(0xFF001C22) else MaterialTheme.colorScheme.onPrimary

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(
                                elevation = 10.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = accentColor.copy(alpha = 0.30f),
                                spotColor = accentColor.copy(alpha = 0.40f)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(ctaBrush)
                            .clickable(onClick = submitCurrentStep)
                            .padding(horizontal = 20.dp)
                            .testTag("change_pin_primary_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (currentStep == ChangePinStep.CONFIRM) Icons.Default.Check else Icons.Default.Key,
                                contentDescription = null,
                                tint = ctaContentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentStep.ctaLabel,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp
                                ),
                                color = ctaContentColor
                            )
                        }

                        if (currentStep != ChangePinStep.CONFIRM) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = ctaContentColor,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = if (currentStep == ChangePinStep.CURRENT) onDismiss else handleBackNavigation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("change_pin_secondary_button")
                    ) {
                        Text(
                            text = if (currentStep == ChangePinStep.CURRENT) "Cancel" else "Previous Step",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            color = subtitleColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.60f))
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ChangePinHeroBadge(
    isDark: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.50f
        val cy = h * 0.50f

        val goldLight = Color(0xFFFBBF24)
        val goldDeep = Color(0xFFD97706)

        // Concentric halo rings
        drawCircle(
            color = (if (isDark) goldDeep else accentColor).copy(alpha = if (isDark) 0.14f else 0.10f),
            radius = w * 0.45f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = (if (isDark) goldLight else accentColor).copy(alpha = if (isDark) 0.16f else 0.14f),
            radius = w * 0.34f,
            center = Offset(cx, cy)
        )

        // Central Shield Plate
        val sw = w * 0.46f
        val sh = h * 0.54f
        val shieldPath = Path().apply {
            moveTo(cx, cy - sh * 0.52f)
            lineTo(cx + sw * 0.50f, cy - sh * 0.32f)
            quadraticTo(cx + sw * 0.52f, cy + sh * 0.24f, cx, cy + sh * 0.54f)
            quadraticTo(cx - sw * 0.52f, cy + sh * 0.24f, cx - sw * 0.50f, cy - sh * 0.32f)
            close()
        }
        drawPath(
            path = shieldPath,
            brush = Brush.verticalGradient(
                colors = if (isDark) listOf(Color(0xFF252830), Color(0xFF14161A)) else listOf(Color.White, Color(0xFFEFF4FF)),
                startY = cy - sh * 0.52f,
                endY = cy + sh * 0.54f
            )
        )
        drawPath(
            path = shieldPath,
            color = if (isDark) Color.White.copy(alpha = 0.22f) else accentColor.copy(alpha = 0.35f),
            style = Stroke(width = w * 0.018f, join = StrokeJoin.Round)
        )

        // Golden Key emblem inside the shield
        val keyRingCenter = Offset(cx, cy - h * 0.08f)
        val keyRingRadius = w * 0.085f
        drawCircle(
            brush = Brush.linearGradient(colors = listOf(goldLight, goldDeep)),
            radius = keyRingRadius,
            center = keyRingCenter,
            style = Stroke(width = w * 0.036f)
        )
        // Key shaft
        val shaftX = cx
        val shaftTop = keyRingCenter.y + keyRingRadius * 0.72f
        val shaftBottom = cy + h * 0.14f
        drawLine(
            brush = Brush.verticalGradient(colors = listOf(goldLight, goldDeep)),
            start = Offset(shaftX, shaftTop),
            end = Offset(shaftX, shaftBottom),
            strokeWidth = w * 0.036f,
            cap = StrokeCap.Round
        )
        // Key teeth
        drawLine(
            color = goldDeep,
            start = Offset(shaftX, shaftBottom - h * 0.055f),
            end = Offset(shaftX + w * 0.065f, shaftBottom - h * 0.055f),
            strokeWidth = w * 0.030f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = goldDeep,
            start = Offset(shaftX, shaftBottom - h * 0.005f),
            end = Offset(shaftX + w * 0.05f, shaftBottom - h * 0.005f),
            strokeWidth = w * 0.030f,
            cap = StrokeCap.Round
        )

        // Small cyan/primary security sparkle dot on bottom-right edge of shield
        val badgeCx = cx + sw * 0.36f
        val badgeCy = cy + sh * 0.24f
        drawRoundRect(
            color = accentColor,
            topLeft = Offset(badgeCx - w * 0.065f, badgeCy - w * 0.065f),
            size = Size(w * 0.13f, w * 0.13f),
            cornerRadius = CornerRadius(w * 0.065f, w * 0.065f)
        )
        val refreshArc = w * 0.038f
        drawArc(
            color = if (isDark) Color(0xFF001C22) else Color.White,
            startAngle = -30f,
            sweepAngle = 250f,
            useCenter = false,
            topLeft = Offset(badgeCx - refreshArc, badgeCy - refreshArc),
            size = Size(refreshArc * 2f, refreshArc * 2f),
            style = Stroke(width = w * 0.018f, cap = StrokeCap.Round)
        )
    }
}
