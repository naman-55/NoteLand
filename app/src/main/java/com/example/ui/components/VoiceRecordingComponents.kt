package com.example.ui.components

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.PlaybackParams
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.storage.AttachmentStorageManager
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.SmartNoteUtils
import com.example.util.VoiceRecordingClip
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.sqrt

private val RecorderTerracotta: Color
    @Composable
    get() = if (isAppInDarkTheme) MaterialTheme.colorScheme.primary else Color(0xFFD85C37)
private val RecorderCrimson = Color(0xFFEF4444)

fun normalizeMediaRecorderAmplitude(maxAmplitude: Int): Float {
    if (maxAmplitude <= 0) return 0f
    val linear = (maxAmplitude.toFloat() / 32767f).coerceIn(0f, 1f)
    return sqrt(linear).coerceIn(0f, 1f)
}

fun downsampleWaveform(samples: List<Float>, targetCount: Int = 28): List<Float> {
    if (targetCount <= 0) return emptyList()
    if (samples.isEmpty()) {
        return List(targetCount) { idx ->
            0.22f + ((idx % 5) * 0.08f)
        }
    }
    if (samples.size <= targetCount) {
        val padded = MutableList(targetCount) { 0.16f }
        for (i in 0 until targetCount) {
            val mappedIdx = ((i.toFloat() / targetCount) * samples.size).toInt().coerceIn(0, samples.lastIndex)
            padded[i] = samples[mappedIdx].coerceIn(0.12f, 1f)
        }
        return padded
    }
    val step = samples.size.toFloat() / targetCount.toFloat()
    return List(targetCount) { bucket ->
        val start = (bucket * step).toInt().coerceIn(0, samples.lastIndex)
        val end = (((bucket + 1) * step).toInt()).coerceIn(start + 1, samples.size)
        var peak = 0.12f
        for (j in start until end) {
            if (samples[j] > peak) peak = samples[j]
        }
        peak.coerceIn(0.12f, 1f)
    }
}

@Stable
class VoiceRecorderController(
    private val context: Context,
    private val storageManager: AttachmentStorageManager
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var segmentStartElapsedRealtime = 0L
    private var accumulatedActiveMs = 0L
    private val collectedSamples = mutableListOf<Float>()

    var isRecording by mutableStateOf(false)
        private set
    var isPaused by mutableStateOf(false)
        private set
    var elapsedMs by mutableLongStateOf(0L)
        private set
    var currentLevel by mutableFloatStateOf(0f)
        private set
    val liveWaveform = mutableStateListOf<Float>()
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val tickerRunnable = object : Runnable {
        override fun run() {
            if (!isRecording) return
            if (!isPaused) {
                val now = SystemClock.elapsedRealtime()
                elapsedMs = accumulatedActiveMs + (now - segmentStartElapsedRealtime).coerceAtLeast(0L)
                val amp = try {
                    mediaRecorder?.maxAmplitude ?: 0
                } catch (_: Throwable) {
                    0
                }
                val level = normalizeMediaRecorderAmplitude(amp)
                currentLevel = level
                val barLevel = level.coerceAtLeast(0.14f)
                collectedSamples.add(barLevel)
                liveWaveform.add(barLevel)
                if (liveWaveform.size > 34) {
                    liveWaveform.removeAt(0)
                }
            }
            mainHandler.postDelayed(this, 120L)
        }
    }

    fun clearError() {
        errorMessage = null
    }

    fun startRecording(): Boolean {
        if (isRecording) return true
        errorMessage = null
        val outputFile = storageManager.createVoiceRecordingFile()

        return try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context.applicationContext)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            currentOutputFile = outputFile
            accumulatedActiveMs = 0L
            segmentStartElapsedRealtime = SystemClock.elapsedRealtime()
            elapsedMs = 0L
            currentLevel = 0f
            collectedSamples.clear()
            liveWaveform.clear()
            isPaused = false
            isRecording = true
            mainHandler.removeCallbacks(tickerRunnable)
            mainHandler.post(tickerRunnable)
            true
        } catch (t: Throwable) {
            releaseRecorderQuietly()
            if (outputFile.exists()) outputFile.delete()
            currentOutputFile = null
            isRecording = false
            isPaused = false
            elapsedMs = 0L
            currentLevel = 0f
            errorMessage = t.localizedMessage?.takeIf { it.isNotBlank() }
                ?: "Unable to start microphone recording"
            false
        }
    }

    fun togglePauseResume() {
        val recorder = mediaRecorder ?: return
        if (!isRecording) return
        try {
            if (isPaused) {
                recorder.resume()
                segmentStartElapsedRealtime = SystemClock.elapsedRealtime()
                isPaused = false
            } else {
                accumulatedActiveMs += (SystemClock.elapsedRealtime() - segmentStartElapsedRealtime).coerceAtLeast(0L)
                recorder.pause()
                elapsedMs = accumulatedActiveMs
                currentLevel = 0f
                isPaused = true
            }
        } catch (_: Throwable) {
            errorMessage = "Pause/resume is unavailable on this device"
        }
    }

    fun stopAndSaveRecording(defaultTitlePrefix: String = "Voice Note"): VoiceRecordingClip? {
        if (!isRecording) return null
        mainHandler.removeCallbacks(tickerRunnable)

        val finalDurationMs = if (isPaused) {
            accumulatedActiveMs
        } else {
            accumulatedActiveMs + (SystemClock.elapsedRealtime() - segmentStartElapsedRealtime).coerceAtLeast(0L)
        }.coerceAtLeast(400L)

        try {
            mediaRecorder?.stop()
        } catch (_: Throwable) {
            // Short tap stop(<1s) can throw RuntimeException on some HALs
        }
        releaseRecorderQuietly()

        val file = currentOutputFile
        val samplesSnapshot = collectedSamples.toList()
        currentOutputFile = null
        collectedSamples.clear()
        liveWaveform.clear()
        isRecording = false
        isPaused = false
        currentLevel = 0f
        elapsedMs = 0L

        if (file == null || !file.exists() || file.length() <= 0L) {
            file?.delete()
            errorMessage = "Recording was too short to save"
            return null
        }

        val timeLabel = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        return VoiceRecordingClip(
            id = UUID.randomUUID().toString(),
            filePath = storageManager.toRelativeVoicePath(file),
            title = "$defaultTitlePrefix · $timeLabel",
            durationMs = finalDurationMs,
            createdAtUtc = System.currentTimeMillis(),
            waveform = downsampleWaveform(samplesSnapshot, targetCount = 28)
        )
    }

    fun cancelRecording() {
        mainHandler.removeCallbacks(tickerRunnable)
        try {
            if (isRecording) {
                mediaRecorder?.stop()
            }
        } catch (_: Throwable) {
        }
        releaseRecorderQuietly()
        currentOutputFile?.let { if (it.exists()) it.delete() }
        currentOutputFile = null
        collectedSamples.clear()
        liveWaveform.clear()
        isRecording = false
        isPaused = false
        elapsedMs = 0L
        currentLevel = 0f
    }

    fun destroy() {
        cancelRecording()
    }

    private fun releaseRecorderQuietly() {
        try {
            mediaRecorder?.reset()
        } catch (_: Throwable) {
        }
        try {
            mediaRecorder?.release()
        } catch (_: Throwable) {
        }
        mediaRecorder = null
    }
}

@Stable
class VoicePlayerController(
    private val storageManager: AttachmentStorageManager
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null

    var playingClipId by mutableStateOf<String?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var currentPositionMs by mutableLongStateOf(0L)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set
    var playbackSpeed by mutableFloatStateOf(1.0f)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val progressRunnable = object : Runnable {
        override fun run() {
            val player = mediaPlayer
            if (player != null && isPlaying) {
                try {
                    currentPositionMs = player.currentPosition.toLong().coerceAtLeast(0L)
                    if (player.duration > 0) {
                        durationMs = player.duration.toLong()
                    }
                } catch (_: Throwable) {
                }
                mainHandler.postDelayed(this, 125L)
            }
        }
    }

    fun togglePlayPause(clip: VoiceRecordingClip) {
        errorMessage = null
        val player = mediaPlayer
        if (playingClipId == clip.id && player != null) {
            try {
                if (isPlaying) {
                    player.pause()
                    isPlaying = false
                    mainHandler.removeCallbacks(progressRunnable)
                } else {
                    applyPlaybackSpeed(player, playbackSpeed)
                    player.start()
                    isPlaying = true
                    mainHandler.removeCallbacks(progressRunnable)
                    mainHandler.post(progressRunnable)
                }
            } catch (_: Throwable) {
                stopPlayback()
                errorMessage = "Unable to resume audio clip"
            }
            return
        }

        stopPlayback()
        val audioFile = storageManager.resolveVoiceRecordingFile(clip.filePath)
        if (!audioFile.exists() || audioFile.length() <= 0L) {
            errorMessage = "Audio file is missing on device"
            return
        }

        try {
            val player = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()
                setOnCompletionListener { mp ->
                    mainHandler.removeCallbacks(progressRunnable)
                    this@VoicePlayerController.isPlaying = false
                    this@VoicePlayerController.currentPositionMs = 0L
                    try {
                        mp.seekTo(0)
                    } catch (_: Throwable) {
                    }
                }
            }
            applyPlaybackSpeed(player, playbackSpeed)
            mediaPlayer = player
            playingClipId = clip.id
            durationMs = player.duration.toLong().coerceAtLeast(clip.durationMs)
            currentPositionMs = 0L
            player.start()
            isPlaying = true
            mainHandler.removeCallbacks(progressRunnable)
            mainHandler.post(progressRunnable)
        } catch (_: Throwable) {
            stopPlayback()
            errorMessage = "Unable to play this recording"
        }
    }

    fun seekToFraction(clip: VoiceRecordingClip, fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        if (playingClipId != clip.id || mediaPlayer == null) {
            togglePlayPause(clip)
        }
        val player = mediaPlayer ?: return
        val total = durationMs.takeIf { it > 0L } ?: clip.durationMs
        if (total <= 0L) return
        val targetMs = (total * clamped).toInt().coerceIn(0, total.toInt())
        try {
            player.seekTo(targetMs)
            currentPositionMs = targetMs.toLong()
        } catch (_: Throwable) {
        }
    }

    fun cyclePlaybackSpeed() {
        val next = when {
            playbackSpeed < 1.2f -> 1.25f
            playbackSpeed < 1.4f -> 1.5f
            playbackSpeed < 1.8f -> 2.0f
            else -> 1.0f
        }
        playbackSpeed = next
        mediaPlayer?.let { player ->
            if (isPlaying) {
                applyPlaybackSpeed(player, next)
            }
        }
    }

    fun stopIfPlaying(clipId: String? = null) {
        if (clipId == null || playingClipId == clipId) {
            stopPlayback()
        }
    }

    fun stopPlayback() {
        mainHandler.removeCallbacks(progressRunnable)
        try {
            mediaPlayer?.stop()
        } catch (_: Throwable) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Throwable) {
        }
        mediaPlayer = null
        playingClipId = null
        isPlaying = false
        currentPositionMs = 0L
        durationMs = 0L
    }

    fun destroy() {
        stopPlayback()
    }

    private fun applyPlaybackSpeed(player: MediaPlayer, speed: Float) {
        try {
            val params: PlaybackParams = player.playbackParams
            params.speed = speed
            player.playbackParams = params
        } catch (_: Throwable) {
        }
    }
}

@Composable
fun VoiceRecorderDockCard(
    isRecording: Boolean,
    isPaused: Boolean,
    elapsedMs: Long,
    currentLevel: Float,
    liveWaveform: List<Float>,
    errorMessage: String?,
    onTogglePauseResume: () -> Unit,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme
    val bgColor = if (isDark) Color(0xFF0F1518) else Color(0xFF18181A)
    val visible = isRecording || !errorMessage.isNullOrBlank()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = bgColor.copy(alpha = 0.98f),
            border = BorderStroke(
                1.dp,
                if (isRecording && !isPaused) RecorderCrimson.copy(alpha = 0.6f)
                else RecorderTerracotta.copy(alpha = 0.45f)
            ),
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color.Black.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.35f)
                )
                .testTag("voice_recorder_dock")
        ) {
            if (!isRecording && !errorMessage.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFCA5A5),
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f).padding(end = 10.dp)
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.14f),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .clickable(onClick = onDismissError)
                    ) {
                        Text(
                            text = "Dismiss",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top Row: Pulsing REC badge + Live scrolling amplitude waveform + Monospace Timer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RecordingStatusBadge(isPaused = isPaused)

                        // Scrolling live waveform bars
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.5.dp, Alignment.End)
                        ) {
                            val bars = if (liveWaveform.isEmpty()) {
                                List(18) { 0.16f + (currentLevel * 0.4f) }
                            } else {
                                liveWaveform.takeLast(24)
                            }
                            bars.forEach { sample ->
                                val barFraction = sample.coerceIn(0.14f, 1f)
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height((4f + barFraction * 22f).dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isPaused) Color.White.copy(alpha = 0.4f)
                                            else RecorderTerracotta
                                        )
                                )
                            }
                        }

                        Text(
                            text = SmartNoteUtils.formatAudioDuration(elapsedMs),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("voice_rec_timer")
                        )
                    }

                    // Bottom Controls: Discard | Pause/Resume | Save to Note
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .clickable(onClick = onDiscard)
                                .testTag("voice_rec_discard_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Discard Recording",
                                    tint = Color(0xFFFCA5A5),
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = "Discard",
                                    color = Color.White.copy(alpha = 0.88f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.14f),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .clickable(onClick = onTogglePauseResume)
                                .testTag("voice_rec_pause_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                            ) {
                                Icon(
                                    if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (isPaused) "Resume Recording" else "Pause Recording",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isPaused) "Resume" else "Pause",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = RecorderTerracotta,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .clickable(onClick = onSave)
                                .testTag("voice_rec_save_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Save Recording to Note",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = "Save Clip",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceClipsSection(
    clips: List<VoiceRecordingClip>,
    playerController: VoicePlayerController,
    readOnly: Boolean,
    onAddVoiceClip: (() -> Unit)? = null,
    onRenameClip: (VoiceRecordingClip) -> Unit = {},
    onShareClip: (VoiceRecordingClip) -> Unit = {},
    onDeleteClip: (VoiceRecordingClip) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (clips.isEmpty()) return
    val isDark = isAppInDarkTheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("voice_clips_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = RecorderTerracotta,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (clips.size == 1) "Voice Recording (1)" else "Voice Recordings (${clips.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (!readOnly && onAddVoiceClip != null) {
                Surface(
                    shape = CircleShape,
                    color = RecorderTerracotta.copy(alpha = 0.14f),
                    border = BorderStroke(0.8.dp, RecorderTerracotta.copy(alpha = 0.42f)),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onAddVoiceClip)
                        .testTag("voice_section_add_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = RecorderCrimson,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "Record New",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = RecorderTerracotta
                        )
                    }
                }
            }
        }

        if (!playerController.errorMessage.isNullOrBlank()) {
            Text(
                text = playerController.errorMessage.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        clips.forEachIndexed { index, clip ->
            val isActiveClip = playerController.playingClipId == clip.id
            val isClipPlaying = isActiveClip && playerController.isPlaying
            val totalDurationMs = if (isActiveClip && playerController.durationMs > 0L) {
                playerController.durationMs
            } else {
                clip.durationMs
            }
            val currentPosMs = if (isActiveClip) playerController.currentPositionMs else 0L

            VoiceClipCard(
                clip = clip,
                fallbackIndex = index + 1,
                isActive = isActiveClip,
                isPlaying = isClipPlaying,
                currentPositionMs = currentPosMs,
                totalDurationMs = totalDurationMs,
                playbackSpeed = playerController.playbackSpeed,
                isDark = isDark,
                readOnly = readOnly,
                onTogglePlayPause = { playerController.togglePlayPause(clip) },
                onSeekFraction = { fraction -> playerController.seekToFraction(clip, fraction) },
                onCycleSpeed = { playerController.cyclePlaybackSpeed() },
                onRename = { onRenameClip(clip) },
                onShare = { onShareClip(clip) },
                onDelete = { onDeleteClip(clip) }
            )
        }
    }
}

@Composable
private fun VoiceClipCard(
    clip: VoiceRecordingClip,
    fallbackIndex: Int,
    isActive: Boolean,
    isPlaying: Boolean,
    currentPositionMs: Long,
    totalDurationMs: Long,
    playbackSpeed: Float,
    isDark: Boolean,
    readOnly: Boolean,
    onTogglePlayPause: () -> Unit,
    onSeekFraction: (Float) -> Unit,
    onCycleSpeed: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val cardBg = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.94f)
    } else {
        Color.White.copy(alpha = 0.88f)
    }
    val borderColor = if (isActive) {
        RecorderTerracotta.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.68f)
    }

    val progress = if (totalDurationMs > 0L) {
        (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val displayTitle = clip.title.ifBlank { "Voice Clip #$fallbackIndex" }
    val bars = remember(clip.id, clip.waveform) {
        if (clip.waveform.isNotEmpty()) clip.waveform else downsampleWaveform(emptyList(), 28)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(if (isActive) 1.2.dp else 0.8.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voice_clip_card_${clip.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isPlaying) RecorderTerracotta else RecorderTerracotta.copy(alpha = 0.14f),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onTogglePlayPause)
                        .testTag("voice_clip_play_${clip.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause $displayTitle" else "Play $displayTitle",
                            tint = if (isPlaying) Color.White else RecorderTerracotta,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        )
                        Text(
                            text = if (isActive) {
                                "${SmartNoteUtils.formatAudioDuration(currentPositionMs)} / ${SmartNoteUtils.formatAudioDuration(totalDurationMs)}"
                            } else {
                                SmartNoteUtils.formatAudioDuration(totalDurationMs)
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (isActive) RecorderTerracotta else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    // Interactive Waveform Scrub Bar
                    val playedBarColor = RecorderTerracotta
                    val unplayedBarColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .pointerInput(clip.id, totalDurationMs) {
                                detectTapGestures { offset ->
                                    if (size.width > 0) {
                                        onSeekFraction((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
                                    }
                                }
                            }
                            .pointerInput(clip.id, totalDurationMs) {
                                detectHorizontalDragGestures { change, _ ->
                                    change.consume()
                                    if (size.width > 0) {
                                        onSeekFraction((change.position.x / size.width.toFloat()).coerceIn(0f, 1f))
                                    }
                                }
                            }
                    ) {
                        val count = bars.size.coerceAtLeast(1)
                        val gapPx = 3.dp.toPx()
                        val totalGap = gapPx * (count - 1)
                        val barWidth = ((size.width - totalGap) / count).coerceAtLeast(2.dp.toPx())
                        val centerY = size.height / 2f
                        bars.forEachIndexed { i, amp ->
                            val x = i * (barWidth + gapPx)
                            val barProgress = (i + 0.5f) / count.toFloat()
                            val barHeight = (size.height * amp.coerceIn(0.16f, 1f)).coerceAtLeast(4.dp.toPx())
                            drawRoundRect(
                                color = if (barProgress <= progress && isActive) playedBarColor else unplayedBarColor,
                                topLeft = Offset(x, centerY - barHeight / 2f),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }
                    }
                }

                // Speed control when active + More menu
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (isActive) {
                        val speedLabel = when {
                            playbackSpeed == 1.0f -> "1x"
                            playbackSpeed == 2.0f -> "2x"
                            else -> "${playbackSpeed}x"
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RecorderTerracotta.copy(alpha = 0.14f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onCycleSpeed)
                                .testTag("voice_clip_speed_${clip.id}")
                        ) {
                            Text(
                                text = speedLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = RecorderTerracotta,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Voice clip options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share Audio") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onShare()
                                }
                            )
                            if (!readOnly) {
                                DropdownMenuItem(
                                    text = { Text("Rename Clip") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onRename()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Clip", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingStatusBadge(isPaused: Boolean) {
    val dotAlpha = if (isPaused) {
        1f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "recDot")
        val recAlpha by infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(650, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "recDotAlpha"
        )
        recAlpha
    }

    Surface(
        shape = CircleShape,
        color = if (isPaused) Color.White.copy(alpha = 0.14f) else RecorderCrimson.copy(alpha = 0.2f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPaused) Color(0xFFFBBF24)
                        else RecorderCrimson.copy(alpha = dotAlpha)
                    )
            )
            Text(
                text = if (isPaused) "PAUSED" else "REC",
                color = Color.White,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
