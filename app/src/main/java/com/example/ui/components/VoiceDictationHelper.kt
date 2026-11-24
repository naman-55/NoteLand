package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.isAppInDarkTheme
import java.util.Locale

enum class DictationTarget {
    BODY,
    TITLE
}

fun normalizeRmsDb(rmsdB: Float): Float {
    if (rmsdB.isNaN()) return 0f
    return ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
}

fun formatSpeechErrorMessage(errorCode: Int): String {
    return when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error"
        SpeechRecognizer.ERROR_CLIENT -> "Speech client error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network speech service unavailable"
        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that — tap mic to speak again"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy"
        SpeechRecognizer.ERROR_SERVER -> "Speech server unavailable"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
        else -> "Voice recognition unavailable"
    }
}

/**
 * Inserts dictated speech cleanly at the current selection/cursor with smart boundary spacing
 * and sentence-start capitalization.
 */
fun mergeDictatedSegment(
    current: TextFieldValue,
    spokenRaw: String
): TextFieldValue {
    val trimmed = spokenRaw.trim()
    if (trimmed.isEmpty()) return current

    val text = current.text
    val start = current.selection.min.coerceIn(0, text.length)
    val end = current.selection.max.coerceIn(0, text.length)

    val startsWithPunctuation = trimmed.first() in setOf(',', '.', '!', '?', ':', ';', ')', ']')
    val beforeSlice = text.substring(0, start)
    val charBefore = beforeSlice.lastOrNull()
    val nonSpaceBefore = beforeSlice.trimEnd().lastOrNull()

    val needsLeadingSpace = charBefore != null && !charBefore.isWhitespace() && !startsWithPunctuation
    val shouldCapitalize = nonSpaceBefore == null ||
        charBefore == '\n' ||
        nonSpaceBefore in setOf('.', '!', '?')

    val formattedSpoken = if (shouldCapitalize && trimmed.first().isLowerCase()) {
        trimmed.replaceFirstChar { it.titlecase(Locale.getDefault()) }
    } else {
        trimmed
    }

    val charAfter = text.getOrNull(end)
    val needsTrailingSpace = charAfter != null &&
        !charAfter.isWhitespace() &&
        charAfter !in setOf(',', '.', '!', '?', ':', ';', ')', ']')

    val leading = if (needsLeadingSpace) " " else ""
    val trailing = if (needsTrailingSpace) " " else ""
    val insertion = "$leading$formattedSpoken$trailing"
    val newText = text.replaceRange(start, end, insertion)
    val cursorPos = start + leading.length + formattedSpoken.length
    return TextFieldValue(
        text = newText,
        selection = TextRange(cursorPos)
    )
}

fun buildRecognizerIntent(
    context: Context,
    languageTag: String? = null
): Intent {
    return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        val localeTag = languageTag?.takeIf { it.isNotBlank() } ?: Locale.getDefault().toLanguageTag()
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1600L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
    }
}

@Stable
class VoiceDictationController(
    private val context: Context,
    private val onFinalTranscript: (String, DictationTarget) -> Unit,
    private val onFallbackIntentRequested: (DictationTarget) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var quietRetryCount = 0

    var isListening by mutableStateOf(false)
        private set
    var isReady by mutableStateOf(false)
        private set
    var isProcessing by mutableStateOf(false)
        private set
    var isContinuous by mutableStateOf(false)
    var target by mutableStateOf(DictationTarget.BODY)
    var partialTranscript by mutableStateOf("")
        private set
    var normalizedLevel by mutableFloatStateOf(0f)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var restartRunnable: Runnable? = null

    private fun ensureRecognizer(): SpeechRecognizer? {
        if (speechRecognizer != null) return speechRecognizer
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return null
        return try {
            SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                setRecognitionListener(createListener())
            }.also { speechRecognizer = it }
        } catch (_: Throwable) {
            null
        }
    }

    fun startListening() {
        mainHandler.post {
            cancelPendingRestart()
            errorMessage = null
            partialTranscript = ""
            normalizedLevel = 0f

            val recognizer = ensureRecognizer()
            if (recognizer == null) {
                isListening = false
                isReady = false
                isProcessing = false
                onFallbackIntentRequested(target)
                return@post
            }

            try {
                isListening = true
                isReady = false
                isProcessing = false
                recognizer.startListening(buildRecognizerIntent(context))
            } catch (_: Throwable) {
                isListening = false
                isReady = false
                isProcessing = false
                onFallbackIntentRequested(target)
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            cancelPendingRestart()
            quietRetryCount = 0
            isListening = false
            isReady = false
            isProcessing = false
            partialTranscript = ""
            normalizedLevel = 0f
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (_: Throwable) {
            }
        }
    }

    fun reportError(message: String) {
        isListening = false
        isReady = false
        isProcessing = false
        partialTranscript = ""
        normalizedLevel = 0f
        errorMessage = message
    }

    fun toggleContinuous() {
        isContinuous = !isContinuous
    }

    fun toggleTarget() {
        target = if (target == DictationTarget.BODY) DictationTarget.TITLE else DictationTarget.BODY
    }

    fun clearError() {
        errorMessage = null
    }

    fun destroy() {
        mainHandler.post {
            cancelPendingRestart()
            isListening = false
            isReady = false
            isProcessing = false
            partialTranscript = ""
            normalizedLevel = 0f
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Throwable) {
            } finally {
                speechRecognizer = null
            }
        }
    }

    private fun cancelPendingRestart() {
        restartRunnable?.let { mainHandler.removeCallbacks(it) }
        restartRunnable = null
    }

    private fun scheduleRestart(delayMs: Long) {
        cancelPendingRestart()
        val runnable = Runnable {
            if (isListening) {
                startListening()
            }
        }
        restartRunnable = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isReady = true
                isProcessing = false
                errorMessage = null
            }

            override fun onBeginningOfSpeech() {
                quietRetryCount = 0
                isReady = true
                isProcessing = false
            }

            override fun onRmsChanged(rmsdB: Float) {
                normalizedLevel = normalizeRmsDb(rmsdB)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isReady = false
                isProcessing = isListening
                normalizedLevel = 0f
            }

            override fun onError(error: Int) {
                isReady = false
                isProcessing = false
                normalizedLevel = 0f
                if (!isListening) return

                val isQuietPause = error == SpeechRecognizer.ERROR_NO_MATCH ||
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT

                if (isContinuous && isQuietPause && quietRetryCount < 2) {
                    quietRetryCount++
                    partialTranscript = ""
                    scheduleRestart(200L)
                    return
                }

                if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                    try {
                        speechRecognizer?.destroy()
                    } catch (_: Throwable) {
                    }
                    speechRecognizer = null
                }

                isListening = false
                partialTranscript = ""
                errorMessage = formatSpeechErrorMessage(error)
            }

            override fun onResults(results: Bundle?) {
                quietRetryCount = 0
                isProcessing = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spoken = matches?.firstOrNull()?.trim().orEmpty()
                partialTranscript = ""
                normalizedLevel = 0f

                if (spoken.isNotEmpty()) {
                    onFinalTranscript(spoken, target)
                }

                if (isListening && isContinuous) {
                    scheduleRestart(180L)
                } else {
                    isListening = false
                    isReady = false
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()?.trim().orEmpty()
                if (partial.isNotEmpty()) {
                    partialTranscript = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}

@Composable
fun VoiceDictationBar(
    isListening: Boolean,
    isReady: Boolean,
    isProcessing: Boolean = false,
    isContinuous: Boolean,
    target: DictationTarget,
    partialTranscript: String,
    normalizedLevel: Float,
    errorMessage: String?,
    onToggleContinuous: () -> Unit,
    onToggleTarget: () -> Unit,
    onRetry: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme
    val barBg = if (isDark) Color(0xFF0F1518) else Color(0xFF18181A)
    val accentColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFFD85C37)
    val softAccent = if (isDark) Color(0xFF67F2FF) else Color(0xFFFFB094)
    val visible = isListening || isProcessing || !errorMessage.isNullOrBlank()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = barBg.copy(alpha = 0.97f),
            border = BorderStroke(1.dp, accentColor.copy(alpha = if (isListening || isProcessing) 0.55f else 0.3f)),
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color.Black.copy(alpha = 0.3f),
                    spotColor = Color.Black.copy(alpha = 0.3f)
                )
                .testTag("voice_dictation_bar")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Live Waveform Visualizer / Processing Spinner
                    when {
                        isProcessing -> {
                            CircularProgressIndicator(
                                color = accentColor,
                                strokeWidth = 2.2.dp,
                                modifier = Modifier
                                    .size(20.dp)
                                    .testTag("voice_processing_indicator")
                            )
                        }
                        isListening -> {
                            ActiveDictationWaveform(
                                isReady = isReady,
                                normalizedLevel = normalizedLevel,
                                accentColor = accentColor,
                                softAccent = softAccent
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Live transcript or status text
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                !errorMessage.isNullOrBlank() -> errorMessage
                                isProcessing -> "Processing speech…"
                                partialTranscript.isNotBlank() -> partialTranscript
                                isReady -> "Listening… speak naturally"
                                else -> "Starting microphone…"
                            },
                            color = if (!errorMessage.isNullOrBlank()) Color(0xFFFCA5A5) else Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = if (partialTranscript.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                            fontStyle = if (partialTranscript.isBlank() && errorMessage.isNullOrBlank()) FontStyle.Italic else FontStyle.Normal,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("voice_dictation_transcript")
                        )
                    }

                    // Action Stop or Retry
                    if (isListening) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF4444),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .clickable(onClick = onStop)
                                .testTag("voice_stop_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Stop,
                                    contentDescription = "Stop Dictation",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Stop",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = accentColor,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .clickable(onClick = onRetry)
                                .testTag("voice_retry_button")
                        ) {
                            Text(
                                text = "Retry",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Controls strip: Dictate into Body/Title + Continuous toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.14f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onToggleTarget)
                            .testTag("voice_target_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Default.Title,
                                contentDescription = null,
                                tint = softAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (target == DictationTarget.TITLE) "Into: Title" else "Into: Body",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isContinuous) accentColor.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(
                            0.8.dp,
                            if (isContinuous) accentColor.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.14f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onToggleContinuous)
                            .testTag("voice_continuous_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Default.AllInclusive,
                                contentDescription = null,
                                tint = if (isContinuous) softAccent else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isContinuous) "Continuous: On" else "Single Phrase",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    if (!isListening) {
                        Text(
                            text = "Dismiss",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onStop)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveDictationWaveform(
    isReady: Boolean,
    normalizedLevel: Float,
    accentColor: Color,
    softAccent: Color
) {
    val idleBreath = if (normalizedLevel <= 0.05f) {
        val infiniteTransition = rememberInfiniteTransition(label = "dictationPulse")
        val anim by infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.65f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "idleBreath"
        )
        anim
    } else {
        0.45f
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .height(26.dp)
            .padding(horizontal = 2.dp)
    ) {
        val factors = listOf(0.45f, 0.78f, 1.0f, 0.68f, 0.92f, 0.58f, 0.42f)
        factors.forEachIndexed { idx, factor ->
            val rawAmp = if (normalizedLevel > 0.05f) {
                (normalizedLevel * factor).coerceIn(0.18f, 1f)
            } else {
                (idleBreath * factor).coerceIn(0.18f, 0.72f)
            }
            val animatedAmp by animateFloatAsState(
                targetValue = rawAmp,
                animationSpec = tween(durationMillis = 90),
                label = "waveBar$idx"
            )
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height((6f + animatedAmp * 18f).dp)
                    .clip(CircleShape)
                    .background(if (isReady) accentColor else softAccent)
            )
        }
    }
}
