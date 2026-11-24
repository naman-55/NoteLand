package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FocusSessionStatus
import com.example.data.model.FocusSessionType
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.editorialCardBorderColor
import com.example.ui.theme.editorialCardContainerColor
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.KnowledgeBrainEngine
import com.example.util.ProductivityEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PomodoroFocusTab(viewModel: NoteViewModel) {
    val runtime by viewModel.pomodoroState.collectAsStateWithLifecycle()
    val remainingSec by viewModel.pomodoroRemainingSeconds.collectAsStateWithLifecycle()
    val stats by viewModel.focusStatistics.collectAsStateWithLifecycle()
    val sessions by viewModel.focusSessions.collectAsStateWithLifecycle()
    val tasks by viewModel.activeTasks.collectAsStateWithLifecycle()
    val notes by viewModel.allActiveNotes.collectAsStateWithLifecycle()
    val isDark = isAppInDarkTheme

    var showLinkPicker by remember { mutableStateOf(false) }

    val tasksById = remember(tasks) { tasks.associateBy { it.id } }
    val notesById = remember(notes) { notes.associateBy { it.id } }
    val linkedTaskTitle = runtime.linkedTaskId?.let { tasksById[it]?.title }
    val linkedNoteTitle = runtime.linkedNoteId?.let { ProductivityEngine.safeTaskLinkedNoteTitle(notesById[it]) }
    val activeLinkLabel = when {
        !linkedTaskTitle.isNullOrBlank() -> "Task: $linkedTaskTitle"
        !linkedNoteTitle.isNullOrBlank() -> "Note: $linkedNoteTitle"
        else -> "Link task or note (optional)"
    }

    val totalSec = runtime.plannedDurationSec.coerceAtLeast(1)
    val progress = if (runtime.isActiveOrPaused) {
        (remainingSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }
    val mins = (remainingSec / 60).coerceAtLeast(0)
    val secs = (remainingSec % 60).coerceAtLeast(0)
    val formattedClock = String.format(Locale.US, "%02d:%02d", mins, secs)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pomodoro_focus_tab"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Timer Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = editorialCardContainerColor(isDark)),
                border = BorderStroke(0.9.dp, editorialCardBorderColor(isDark)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val prodColors = com.example.ui.theme.currentSectionColors
                    // Mode Presets
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        FocusSessionType.entries.forEach { preset ->
                            val selected = runtime.sessionType == preset
                            Surface(
                                shape = CircleShape,
                                color = if (selected) prodColors.accent else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(enabled = !runtime.isActiveOrPaused) {
                                        viewModel.selectPomodoroPreset(preset, preset.defaultDurationSec)
                                    }
                                    .testTag("pomodoro_mode_${preset.name.lowercase()}")
                            ) {
                                Text(
                                    text = "${preset.label} (${preset.defaultDurationSec / 60}m)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) prodColors.onAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    // Custom Duration quick chips when Focus & Idle
                    if (!runtime.isActiveOrPaused && runtime.sessionType == FocusSessionType.WORK) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(15, 25, 45, 60).forEach { m ->
                                val active = runtime.plannedDurationSec == m * 60
                                Surface(
                                    shape = CircleShape,
                                    color = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            viewModel.selectPomodoroPreset(FocusSessionType.WORK, m * 60)
                                        }
                                ) {
                                    Text(
                                        text = "${m}m",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Circular Countdown Gauge
                    val ringPrimary = when (runtime.sessionType) {
                        FocusSessionType.WORK -> prodColors.accent
                        FocusSessionType.SHORT_BREAK -> Color(0xFF10B981)
                        FocusSessionType.LONG_BREAK -> Color(0xFF0EA5E9)
                    }
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(210.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 14.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                            drawArc(
                                color = trackColor,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(diameter, diameter),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = ringPrimary,
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(diameter, diameter),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formattedClock,
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 40.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("pomodoro_clock_display")
                            )
                            Text(
                                text = when {
                                    runtime.status == FocusSessionStatus.RUNNING -> "Focusing..."
                                    runtime.status == FocusSessionStatus.PAUSED -> "Paused"
                                    else -> "Ready · Cycles: ${runtime.completedWorkCycles}"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Linked Task/Note Selector Pill
                    OutlinedButton(
                        onClick = { showLinkPicker = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("pomodoro_link_button")
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = activeLinkLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.5.sp
                        )
                    }

                    // Primary Timer Controls (>=48dp touch targets)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when {
                            !runtime.isActiveOrPaused -> {
                                Button(
                                    onClick = {
                                        viewModel.startPomodoro(
                                            type = runtime.sessionType,
                                            durationSec = runtime.plannedDurationSec,
                                            linkedTaskId = runtime.linkedTaskId,
                                            linkedNoteId = runtime.linkedNoteId
                                        )
                                    },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = prodColors.accent,
                                        contentColor = prodColors.onAccent
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("pomodoro_start_btn")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Start Focus")
                                    Spacer(Modifier.width(6.dp))
                                    Text("Start ${runtime.sessionType.label}")
                                }
                            }
                            runtime.status == FocusSessionStatus.RUNNING -> {
                                Button(
                                    onClick = { viewModel.pausePomodoro() },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = prodColors.accent,
                                        contentColor = prodColors.onAccent
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("pomodoro_pause_btn")
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = "Pause")
                                    Spacer(Modifier.width(4.dp))
                                    Text("Pause")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.completePomodoroEarly() },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("pomodoro_complete_btn")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Complete")
                                    Spacer(Modifier.width(4.dp))
                                    Text("Finish")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.resetPomodoro(recordCancelled = true) },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("pomodoro_reset_btn")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
                                }
                            }
                            else -> { // PAUSED
                                Button(
                                    onClick = { viewModel.resumePomodoro() },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("pomodoro_resume_btn")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                                    Spacer(Modifier.width(4.dp))
                                    Text("Resume")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.completePomodoroEarly() },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("pomodoro_complete_btn")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Complete")
                                    Spacer(Modifier.width(4.dp))
                                    Text("Finish")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.resetPomodoro(recordCancelled = true) },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("pomodoro_reset_btn")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Focus Statistics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FocusStatTile(
                    label = "Today",
                    value = "${stats.todayFocusMinutes}m",
                    subtitle = "${stats.todayCompletedSessions} sessions",
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
                FocusStatTile(
                    label = "7-Day Focus",
                    value = "${stats.weekFocusMinutes}m",
                    subtitle = "${stats.weekCompletedSessions} sessions",
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
                FocusStatTile(
                    label = "Completion",
                    value = "${stats.completionRatePercent}%",
                    subtitle = "Avg ${stats.averageSessionMinutes}m",
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Session History Header
        item {
            Text(
                text = "Focus Session History (${sessions.size})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (sessions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = editorialCardContainerColor(isDark),
                    border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No completed focus sessions yet. Start a 25-minute Focus block to build your history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(sessions.take(25), key = { it.id }) { session ->
                val type = FocusSessionType.fromName(session.sessionType)
                val status = FocusSessionStatus.fromName(session.status)
                val taskTitle = session.linkedTaskId?.let { tasksById[it]?.title }
                val noteTitle = session.linkedNoteId?.let { ProductivityEngine.safeTaskLinkedNoteTitle(notesById[it]) }
                val dateTimeLabel = remember(session.startedAtUtc) {
                    SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()).format(Date(session.startedAtUtc))
                }
                val durationLabel = "${(session.actualFocusedSec / 60)}m ${(session.actualFocusedSec % 60)}s"
                val statusColor = when (status) {
                    FocusSessionStatus.COMPLETED -> Color(0xFF10B981)
                    FocusSessionStatus.RUNNING -> MaterialTheme.colorScheme.primary
                    FocusSessionStatus.PAUSED -> Color(0xFFF59E0B)
                    FocusSessionStatus.CANCELLED -> MaterialTheme.colorScheme.error
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = editorialCardContainerColor(isDark),
                    border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
                    modifier = Modifier.fillMaxWidth().testTag("focus_session_row_${session.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = statusColor, modifier = Modifier.size(18.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${type.label} · $durationLabel",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = buildString {
                                    append(dateTimeLabel)
                                    if (!taskTitle.isNullOrBlank()) append(" · $taskTitle")
                                    else if (!noteTitle.isNullOrBlank()) append(" · $noteTitle")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(shape = CircleShape, color = statusColor.copy(alpha = 0.14f)) {
                            Text(
                                text = status.name.lowercase().replaceFirstChar { it.uppercase() },
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLinkPicker) {
        val eligibleNotes = remember(notes) {
            notes.filter { KnowledgeBrainEngine.isNoteKnowledgeEligible(it) }
        }
        val activePendingTasks = remember(tasks) {
            tasks.filter { !it.isCompleted && !it.isArchived }
        }
        AlertDialog(
            onDismissRequest = { showLinkPicker = false },
            title = { Text("Link Task or Note") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        TextButton(
                            onClick = {
                                viewModel.setPomodoroLinks(null, null)
                                showLinkPicker = false
                            }
                        ) {
                            Text("Clear Link")
                        }
                    }
                    if (activePendingTasks.isNotEmpty()) {
                        item {
                            Text("Tasks", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        items(activePendingTasks.take(12), key = { "pt_${it.id}" }) { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.setPomodoroLinks(task.id, task.linkedNoteId)
                                        showLinkPicker = false
                                    }
                                    .padding(10.dp)
                            ) {
                                Text(task.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (eligibleNotes.isNotEmpty()) {
                        item {
                            Text("Unlocked Notes", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        items(eligibleNotes.take(12), key = { "pn_${it.id}" }) { note ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.setPomodoroLinks(null, note.id)
                                        showLinkPicker = false
                                    }
                                    .padding(10.dp)
                            ) {
                                Text(note.title.ifBlank { "Untitled" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLinkPicker = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun FocusStatTile(
    label: String,
    value: String,
    subtitle: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = editorialCardContainerColor(isDark),
        border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
