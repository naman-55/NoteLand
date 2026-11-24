package com.example.ui.screens

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import android.widget.Toast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FocusSessionType
import com.example.data.model.NoteEntity
import com.example.data.model.NoteType
import com.example.data.model.ReminderEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskRecurrenceRule
import com.example.ui.components.TaskEditorDialog
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.editorialBackdropBrush
import com.example.ui.theme.editorialHeaderSubColor
import com.example.ui.theme.editorialHeaderTextColor
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.ChecklistItem
import com.example.util.ProductivityEngine
import com.example.util.SmartNoteUtils
import com.example.util.rememberLifecycleCurrentHour
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.delay

private val CalendarViolet = Color(0xFF9381E8)
private val CalendarVioletDeep = Color(0xFF6E5AE0)

private data class CalendarCategoryStyle(
    val label: String,
    val accent: Color,
    val lightBg: Color,
    val darkBg: Color,
    val pillBgLight: Color,
    val pillBgDark: Color,
    val colorHex: String
)

private val CalendarCategories = listOf(
    CalendarCategoryStyle(
        label = "Health",
        accent = Color(0xFFEC4899),
        lightBg = Color(0xFFFDF2F8),
        darkBg = Color(0xFF2D1925),
        pillBgLight = Color(0xFFFCE7F3),
        pillBgDark = Color(0xFF472137),
        colorHex = "#FFE4E6"
    ),
    CalendarCategoryStyle(
        label = "Work",
        accent = Color(0xFF3B82F6),
        lightBg = Color(0xFFEFF6FF),
        darkBg = Color(0xFF162436),
        pillBgLight = Color(0xFFDBEAFE),
        pillBgDark = Color(0xFF1E3754),
        colorHex = "#E0F2FE"
    ),
    CalendarCategoryStyle(
        label = "Focus",
        accent = Color(0xFF8B5CF6),
        lightBg = Color(0xFFF5F3FF),
        darkBg = Color(0xFF241D3A),
        pillBgLight = Color(0xFFEDE9FE),
        pillBgDark = Color(0xFF362B57),
        colorHex = "#EDE9FE"
    ),
    CalendarCategoryStyle(
        label = "High Priority",
        accent = Color(0xFFF59E0B),
        lightBg = Color(0xFFFFFBEB),
        darkBg = Color(0xFF2B2214),
        pillBgLight = Color(0xFFFEF3C7),
        pillBgDark = Color(0xFF45341A),
        colorHex = "#FEF3C7"
    )
)

private data class ScheduleBlock(
    val id: String,
    val title: String,
    val subtitle: String,
    val startUtc: Long,
    val endUtc: Long,
    val category: String,
    val isCompleted: Boolean,
    val reminder: ReminderEntity?,
    val note: NoteEntity?,
    val task: TaskEntity? = null,
    val hasConflict: Boolean = false,
    val colorIndex: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: NoteViewModel,
    initialTab: Int = 0, // 0: My Calendar Timeline, 1: Daily Tasks & Checklists
    onNavigateToEditor: (String, String) -> Unit,
    onNavigateToProductivity: (Int) -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val allReminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val allNotes by viewModel.allActiveNotes.collectAsStateWithLifecycle()
    val activeTasks by viewModel.activeTasks.collectAsStateWithLifecycle()
    val kanbanColumns by viewModel.kanbanColumns.collectAsStateWithLifecycle()
    val taskDependencies by viewModel.taskDependencies.collectAsStateWithLifecycle()
    val isDark = isAppInDarkTheme

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var scheduleViewMode by remember { mutableStateOf("WEEKLY") } // DAILY, WEEKLY, MONTHLY
    var selectedDayUtc by remember { mutableLongStateOf(SmartNoteUtils.startOfDayUtc(System.currentTimeMillis())) }
    var showCompleted by remember { mutableStateOf(true) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showCreateEventDialog by remember { mutableStateOf(false) }
    var editingScheduleBlock by remember { mutableStateOf<ScheduleBlock?>(null) }
    var showTaskEditorDialog by remember { mutableStateOf(false) }
    var editingTaskEntity by remember { mutableStateOf<TaskEntity?>(null) }
    var showTimeBlockPickerSheet by remember { mutableStateOf(false) }
    var activeScheduleBlock by remember { mutableStateOf<ScheduleBlock?>(null) }
    var reschedulingBlock by remember { mutableStateOf<ScheduleBlock?>(null) }

    // Live time-of-day greeting (lifecycle-aware, hour-boundary scheduled)
    val currentHour = rememberLifecycleCurrentHour()
    val timeGreeting = SmartNoteUtils.getTimeBasedGreeting(currentHour)

    val weekDays = remember(selectedDayUtc) {
        SmartNoteUtils.getWeekDaysStartingMonday(selectedDayUtc)
    }

    val linkedReminderIds = remember(activeTasks) {
        activeTasks.mapNotNull { it.linkedReminderId }.toSet()
    }
    val standaloneReminders = remember(allReminders, linkedReminderIds) {
        allReminders.filter { it.id !in linkedReminderIds }
    }
    val unscheduledTasks = remember(activeTasks) {
        activeTasks.filter { !it.isCompleted && it.scheduledStartUtc == null }
    }
    val tasksById = remember(activeTasks) { activeTasks.associateBy { it.id } }
    val blockedTaskIds = remember(activeTasks, taskDependencies, tasksById) {
        activeTasks.filter { ProductivityEngine.isTaskBlocked(it.id, taskDependencies, tasksById) }
            .map { it.id }
            .toSet()
    }

    // Build unified schedule items across time-blocked tasks, standalone reminders, and note timestamps
    val dayScheduleBlocks = remember(activeTasks, standaloneReminders, allReminders, allNotes, selectedDayUtc, showCompleted) {
        val rawBlocks = mutableListOf<ScheduleBlock>()
        val linkedNoteIds = mutableSetOf<String>()
        val notesById = allNotes.associateBy { it.id }
        val remindersById = allReminders.associateBy { it.id }

        // 1. Time-blocked & scheduled TaskEntity items
        activeTasks.forEachIndexed { idx, task ->
            val blockStart = task.scheduledStartUtc ?: task.dueAtUtc ?: return@forEachIndexed
            val matchesDay = matchesRecurrenceOnDay(
                sourceUtc = blockStart,
                targetDayUtc = selectedDayUtc,
                rule = task.recurrenceRule
            )
            if (matchesDay && (showCompleted || !task.isCompleted)) {
                val linkedNote = task.linkedNoteId?.let { notesById[it] }
                val linkedReminder = task.linkedReminderId?.let { remindersById[it] }
                val isLockedOrHiddenNote = linkedNote?.isLocked == true || linkedNote?.isHidden == true
                if (linkedNote != null) linkedNoteIds.add(linkedNote.id)
                val displayStart = alignTimeToDay(blockStart, selectedDayUtc)
                val durationMs = if (task.scheduledEndUtc != null && task.scheduledEndUtc > blockStart) {
                    (task.scheduledEndUtc - blockStart).coerceAtLeast(15 * 60_000L)
                } else {
                    (task.estimatedMinutes.coerceIn(15, 480)) * 60_000L
                }
                rawBlocks.add(
                    ScheduleBlock(
                        id = "task_${task.id}",
                        title = if (isLockedOrHiddenNote && (task.id.startsWith("task_cal_") || task.id.startsWith("backfill_"))) {
                            "Locked Note Reminder"
                        } else {
                            task.title.ifBlank { "Scheduled Task" }
                        },
                        subtitle = when {
                            isLockedOrHiddenNote -> "Unlock Vault to view details"
                            task.description.isNotBlank() -> task.description.take(80).trim()
                            linkedNote != null -> "Note: ${ProductivityEngine.safeTaskLinkedNoteTitle(linkedNote)}"
                            else -> "${TaskPriority.fromName(task.priority).label} priority"
                        },
                        startUtc = displayStart,
                        endUtc = displayStart + durationMs,
                        category = task.category.ifBlank { inferCategory(linkedNote) },
                        isCompleted = task.isCompleted,
                        reminder = linkedReminder,
                        note = linkedNote,
                        task = task,
                        colorIndex = idx
                    )
                )
            }
        }

        // 2. Standalone Reminders not already linked to a TaskEntity
        standaloneReminders.forEachIndexed { idx, rem ->
            val note = allNotes.firstOrNull { it.id == rem.noteId }
            val meta = note?.let { SmartNoteUtils.parseCalendarMeta(it.canvasDataJson) }
            val durationMs = if (meta != null && meta.endUtc > meta.startUtc) {
                (meta.endUtc - meta.startUtc).coerceAtLeast(30 * 60_000L)
            } else {
                60 * 60_000L
            }

            val matchesSelectedDay = matchesRecurrenceOnDay(
                sourceUtc = rem.scheduledTimeUtc,
                targetDayUtc = selectedDayUtc,
                rule = rem.repeatInterval
            )

            if (matchesSelectedDay && (showCompleted || !rem.isCompleted)) {
                val displayStart = alignTimeToDay(rem.scheduledTimeUtc, selectedDayUtc)
                val category = meta?.category ?: inferCategory(note)
                val isLockedNote = note?.isLocked == true || note?.isHidden == true
                linkedNoteIds.add(rem.noteId)
                rawBlocks.add(
                    ScheduleBlock(
                        id = rem.id,
                        title = when {
                            isLockedNote -> "Locked Note Reminder"
                            else -> note?.title?.ifBlank { "Scheduled Task" } ?: "Reminder Task"
                        },
                        subtitle = if (isLockedNote) "Unlock Vault to view details" else note?.content?.take(80)?.trim().orEmpty(),
                        startUtc = displayStart,
                        endUtc = displayStart + durationMs,
                        category = category,
                        isCompleted = rem.isCompleted,
                        reminder = rem,
                        note = note,
                        colorIndex = idx
                    )
                )
            }
        }

        // 3. Also include any active note with reminderTimestamp on selectedDayUtc without an explicit ReminderEntity or TaskEntity
        allNotes.filter { !it.isLocked && !it.isHidden && it.id !in linkedNoteIds && it.reminderTimestamp != null }.forEachIndexed { idx, note ->
            val ts = note.reminderTimestamp ?: return@forEachIndexed
            if (SmartNoteUtils.isSameDay(ts, selectedDayUtc)) {
                val meta = SmartNoteUtils.parseCalendarMeta(note.canvasDataJson)
                val endTs = meta?.endUtc ?: (ts + 60 * 60_000L)
                rawBlocks.add(
                    ScheduleBlock(
                        id = "note_${note.id}",
                        title = note.title.ifBlank { "Untitled Note" },
                        subtitle = note.content.take(80).trim(),
                        startUtc = ts,
                        endUtc = endTs,
                        category = meta?.category ?: inferCategory(note),
                        isCompleted = false,
                        reminder = null,
                        note = note,
                        colorIndex = idx + 1
                    )
                )
            }
        }

        // Sort & flag overlapping time blocks
        val sorted = rawBlocks.sortedBy { it.startUtc }.toMutableList()
        for (i in sorted.indices) {
            val cur = sorted[i]
            val overlapsAny = sorted.indices.any { j ->
                i != j && !sorted[j].isCompleted && !cur.isCompleted &&
                    cur.startUtc < sorted[j].endUtc && sorted[j].startUtc < cur.endUtc
            }
            if (overlapsAny) {
                sorted[i] = cur.copy(hasConflict = true)
            }
        }
        sorted.toList()
    }

    // Days in the visible week that have at least one scheduled event/reminder/time-block
    val activeDayKeys = remember(activeTasks, standaloneReminders, allNotes, weekDays) {
        weekDays.filter { day ->
            activeTasks.any { task ->
                val ts = task.scheduledStartUtc ?: task.dueAtUtc ?: return@any false
                matchesRecurrenceOnDay(ts, day, task.recurrenceRule)
            } || standaloneReminders.any { rem ->
                matchesRecurrenceOnDay(rem.scheduledTimeUtc, day, rem.repeatInterval)
            } || allNotes.any { note ->
                note.reminderTimestamp != null && SmartNoteUtils.isSameDay(note.reminderTimestamp, day)
            }
        }.toSet()
    }

    // Daily tasks: notes with checklists + Kanban/Productivity tasks + standalone reminders
    val checklistNotes = remember(allNotes) {
        allNotes.filter { note ->
            !note.isLocked && SmartNoteUtils.parseChecklistJson(note.checklistJson).isNotEmpty()
        }.sortedByDescending { it.updatedAtUtc }
    }

    val totalTaskStats = remember(activeTasks, checklistNotes, standaloneReminders) {
        var done = activeTasks.count { it.isCompleted } + standaloneReminders.count { it.isCompleted }
        var total = activeTasks.size + standaloneReminders.size
        checklistNotes.forEach { note ->
            val items = SmartNoteUtils.parseChecklistJson(note.checklistJson)
            done += items.count { it.isDone }
            total += items.size
        }
        done to total
    }

    val headerTextColor = editorialHeaderTextColor(isDark)
    val headerSubColor = editorialHeaderSubColor(isDark)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(editorialBackdropBrush(isDark))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateEventDialog = true },
                    shape = RoundedCornerShape(18.dp),
                    containerColor = CalendarViolet,
                    contentColor = Color.White,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("calendar_add_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Calendar Task")
                }
            }
        ) { innerPadding ->
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Top Utility Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onNavigateBack)
                        .testTag("calendar_back_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable {
                                val hostActivity = context.findCalendarActivity()
                                if (hostActivity != null && !hostActivity.isFinishing && !hostActivity.isDestroyed) {
                                    try {
                                        val cal = Calendar.getInstance().apply { timeInMillis = selectedDayUtc }
                                        DatePickerDialog(
                                            hostActivity,
                                            { _, y, m, d ->
                                                val picked = Calendar.getInstance().apply {
                                                    set(y, m, d, 0, 0, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }
                                                selectedDayUtc = picked.timeInMillis
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                            .testTag("calendar_pick_date_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "Pick Date",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Box {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable { showOverflowMenu = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MoreHoriz,
                                    contentDescription = "Calendar Options",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Jump to Today") },
                                onClick = {
                                    selectedDayUtc = SmartNoteUtils.startOfDayUtc(System.currentTimeMillis())
                                    showOverflowMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (showCompleted) "Hide Completed" else "Show Completed") },
                                onClick = {
                                    showCompleted = !showCompleted
                                    showOverflowMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("New Checklist Note") },
                                onClick = {
                                    showOverflowMenu = false
                                    onNavigateToEditor("0", "CHECKLIST")
                                }
                            )
                        }
                    }
                }
            }

            // Title & Date Header + Greeting Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedTab == 0) "My Calendar" else "Tasks",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 29.sp
                        ),
                        color = headerTextColor
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = SmartNoteUtils.formatFullDateHeader(selectedDayUtc),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = headerSubColor
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = CalendarViolet.copy(alpha = if (isDark) 0.22f else 0.12f),
                    border = BorderStroke(0.8.dp, CalendarViolet.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = timeGreeting,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isDark) Color(0xFFD6CEFF) else CalendarVioletDeep,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mode Switcher Pills: Schedule | Daily Tasks + Phase 2 Shortcuts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CalendarModePill(
                    label = "Schedule",
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.testTag("calendar_tab_schedule")
                )
                CalendarModePill(
                    label = "Tasks (${totalTaskStats.first}/${totalTaskStats.second})",
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.testTag("calendar_tab_tasks")
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onNavigateToProductivity(0) }
                        .testTag("calendar_open_kanban_chip")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.ViewKanban,
                            contentDescription = "Kanban Board",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Kanban",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                if (!SmartNoteUtils.isSameDay(selectedDayUtc, System.currentTimeMillis()) && selectedTab == 0) {
                    TextButton(
                        onClick = { selectedDayUtc = SmartNoteUtils.startOfDayUtc(System.currentTimeMillis()) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Today", color = CalendarVioletDeep, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "calendar_tab_switch",
                modifier = Modifier.weight(1f)
            ) { tab ->
                if (tab == 0) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Daily / Weekly / Monthly View Switcher
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                Triple("DAILY", "Daily", "calendar_view_daily"),
                                Triple("WEEKLY", "Weekly", "calendar_view_weekly"),
                                Triple("MONTHLY", "Monthly", "calendar_view_monthly")
                            ).forEach { (key, label, tag) ->
                                val active = scheduleViewMode == key
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (active) CalendarVioletDeep.copy(alpha = 0.16f) else Color.Transparent,
                                    border = BorderStroke(
                                        0.8.dp,
                                        if (active) CalendarVioletDeep else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { scheduleViewMode = key }
                                        .testTag(tag)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (active) CalendarVioletDeep else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        when (scheduleViewMode) {
                            "DAILY" -> {
                                DailyHeaderSelector(
                                    selectedDayUtc = selectedDayUtc,
                                    onPreviousDay = {
                                        selectedDayUtc = Calendar.getInstance().apply {
                                            timeInMillis = selectedDayUtc
                                            add(Calendar.DAY_OF_YEAR, -1)
                                        }.timeInMillis
                                    },
                                    onNextDay = {
                                        selectedDayUtc = Calendar.getInstance().apply {
                                            timeInMillis = selectedDayUtc
                                            add(Calendar.DAY_OF_YEAR, 1)
                                        }.timeInMillis
                                    }
                                )
                            }
                            "MONTHLY" -> {
                                MonthlyCalendarGrid(
                                    selectedDayUtc = selectedDayUtc,
                                    activeTasks = activeTasks,
                                    standaloneReminders = standaloneReminders,
                                    allNotes = allNotes,
                                    onSelectDay = { selectedDayUtc = it },
                                    onPreviousMonth = {
                                        selectedDayUtc = Calendar.getInstance().apply {
                                            timeInMillis = selectedDayUtc
                                            add(Calendar.MONTH, -1)
                                            set(Calendar.DAY_OF_MONTH, 1)
                                        }.timeInMillis
                                    },
                                    onNextMonth = {
                                        selectedDayUtc = Calendar.getInstance().apply {
                                            timeInMillis = selectedDayUtc
                                            add(Calendar.MONTH, 1)
                                            set(Calendar.DAY_OF_MONTH, 1)
                                        }.timeInMillis
                                    }
                                )
                            }
                            else -> {
                                // 7-Day Horizontal Week Strip
                                WeekStripSelector(
                                    weekDays = weekDays,
                                    selectedDayUtc = selectedDayUtc,
                                    activeDayKeys = activeDayKeys,
                                    onSelectDay = { selectedDayUtc = it },
                                    onPreviousWeek = {
                                        val cal = Calendar.getInstance().apply {
                                            timeInMillis = selectedDayUtc
                                            add(Calendar.DAY_OF_YEAR, -7)
                                        }
                                        selectedDayUtc = cal.timeInMillis
                                    },
                                    onNextWeek = {
                                        val cal = Calendar.getInstance().apply {
                                            timeInMillis = selectedDayUtc
                                            add(Calendar.DAY_OF_YEAR, 7)
                                        }
                                        selectedDayUtc = cal.timeInMillis
                                    }
                                )
                            }
                        }

                        // Time-Blocking Bar: TimeZone + Block Unscheduled Task action
                        val tzCity = remember {
                            TimeZone.getDefault().id.substringAfterLast('/').replace('_', ' ')
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TZ: $tzCity · ${dayScheduleBlocks.size} blocks",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = CircleShape,
                                color = CalendarViolet.copy(alpha = if (isDark) 0.24f else 0.14f),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { showTimeBlockPickerSheet = true }
                                    .testTag("calendar_timeblock_picker_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = "Time-block unscheduled task",
                                        tint = if (isDark) Color(0xFFD6CEFF) else CalendarVioletDeep,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Time-Block Task (${unscheduledTasks.size})",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isDark) Color(0xFFD6CEFF) else CalendarVioletDeep
                                    )
                                }
                            }
                        }

                        // Timeline Schedule List
                        if (dayScheduleBlocks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp, vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(26.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = CalendarViolet.copy(alpha = 0.14f),
                                            modifier = Modifier.size(54.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Event,
                                                    contentDescription = null,
                                                    tint = CalendarVioletDeep,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "No events scheduled for this day",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Tap + to schedule an event or time-block a Kanban task on this day.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { showCreateEventDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = CalendarVioletDeep),
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Schedule")
                                            }
                                            if (unscheduledTasks.isNotEmpty()) {
                                                OutlinedButton(
                                                    onClick = { showTimeBlockPickerSheet = true },
                                                    shape = RoundedCornerShape(14.dp)
                                                ) {
                                                    Text("Time-Block")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 96.dp + navBottom),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                dayScheduleBlocks.forEachIndexed { index, block ->
                                    item(key = block.id) {
                                        TimelineScheduleRow(
                                            block = block,
                                            isDark = isDark,
                                            onToggleDone = {
                                                if (block.task != null) {
                                                    viewModel.toggleTaskCompleted(block.task.id)
                                                } else {
                                                    block.reminder?.let { viewModel.toggleReminderCompleted(it) }
                                                }
                                            },
                                            onClick = { activeScheduleBlock = block }
                                        )
                                    }

                                    // Render "Break" slot if gap between consecutive events >= 45 mins
                                    if (index < dayScheduleBlocks.lastIndex) {
                                        val nextBlock = dayScheduleBlocks[index + 1]
                                        val gapMillis = nextBlock.startUtc - block.endUtc
                                        if (gapMillis >= 45 * 60_000L) {
                                            item(key = "break_${block.id}_${nextBlock.id}") {
                                                TimelineBreakRow(
                                                    breakStartUtc = block.endUtc,
                                                    breakEndUtc = nextBlock.startUtc,
                                                    isDark = isDark
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // DAILY TASKS, KANBAN TASKS & CHECKLISTS TAB
                    DailyTasksContent(
                        activeTasks = activeTasks,
                        kanbanColumns = kanbanColumns,
                        blockedTaskIds = blockedTaskIds,
                        checklistNotes = checklistNotes,
                        standaloneReminders = standaloneReminders,
                        allNotes = allNotes,
                        doneCount = totalTaskStats.first,
                        totalCount = totalTaskStats.second,
                        isDark = isDark,
                        navBottomPadding = navBottom,
                        onToggleTask = { task ->
                            viewModel.toggleTaskCompleted(task.id)
                        },
                        onToggleTaskSubtask = { task, idx ->
                            viewModel.toggleTaskSubtask(task.id, idx)
                        },
                        onDeleteTask = { task ->
                            viewModel.deleteTask(task.id)
                            Toast.makeText(context, "Task deleted", Toast.LENGTH_SHORT).show()
                        },
                        onOpenKanban = { onNavigateToProductivity(0) },
                        onToggleChecklistItem = { note, idx ->
                            viewModel.toggleNoteChecklistItem(note, idx)
                        },
                        onToggleReminder = { rem ->
                            viewModel.toggleReminderCompleted(rem)
                        },
                        onTrashNote = { note ->
                            viewModel.softDeleteNote(note)
                            Toast.makeText(context, "Moved to Trash", Toast.LENGTH_SHORT).show()
                        },
                        onTrashReminderTask = { rem, linkedNote ->
                            viewModel.deleteReminder(rem.id)
                            if (linkedNote != null &&
                                !linkedNote.isLocked &&
                                SmartNoteUtils.parseCalendarMeta(linkedNote.canvasDataJson) != null
                            ) {
                                viewModel.softDeleteNote(linkedNote)
                                Toast.makeText(context, "Task moved to Trash", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Reminder removed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenNote = { note ->
                            if (note.isLocked && !viewModel.isVaultUnlocked.value) {
                                Toast.makeText(context, "Locked note. Unlock Vault to open.", Toast.LENGTH_SHORT).show()
                            } else {
                                onNavigateToEditor(note.id, note.noteType)
                            }
                        },
                        onCreateChecklistNote = {
                            onNavigateToEditor("0", "CHECKLIST")
                        },
                        onScheduleTask = { showCreateEventDialog = true },
                        onCreateTask = {
                            editingTaskEntity = null
                            showTaskEditorDialog = true
                        },
                        onEditTask = { task ->
                            editingTaskEntity = task
                            showTaskEditorDialog = true
                        }
                    )
                }
            }
        }
        }
    }

    // Quick Action Sheet for tapping a Timeline Schedule Block
    activeScheduleBlock?.let { block ->
        ModalBottomSheet(onDismissRequest = { activeScheduleBlock = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = block.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = SmartNoteUtils.formatTimeRange(block.startUtc, block.endUtc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (block.subtitle.isNotBlank()) {
                    Text(
                        text = block.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Edit Event / Task Details button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            activeScheduleBlock = null
                            if (block.task != null && block.reminder == null && block.note == null) {
                                editingTaskEntity = block.task
                                showTaskEditorDialog = true
                            } else {
                                editingScheduleBlock = block
                            }
                        }
                        .testTag("calendar_edit_event_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = CalendarVioletDeep)
                        Text(
                            text = if (block.task != null && block.reminder == null && block.note == null) "Edit Task" else "Edit Event",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (block.task != null) {
                    val task = block.task
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                viewModel.toggleTaskCompleted(task.id)
                                activeScheduleBlock = null
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CalendarVioletDeep)
                            Text(if (task.isCompleted) "Mark Incomplete" else "Mark Complete", fontWeight = FontWeight.Medium)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                val sec = (task.estimatedMinutes.coerceIn(5, 90)) * 60
                                viewModel.setPomodoroLinks(task.id, task.linkedNoteId)
                                viewModel.startPomodoro(
                                    type = FocusSessionType.WORK,
                                    durationSec = sec,
                                    linkedTaskId = task.id,
                                    linkedNoteId = task.linkedNoteId
                                )
                                activeScheduleBlock = null
                                onNavigateToProductivity(2)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF8B5CF6))
                            Text("Start Focus Timer (${task.estimatedMinutes}m)", fontWeight = FontWeight.Medium)
                        }
                    }

                    if (task.scheduledStartUtc != null && block.reminder == null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.assignTaskTimeBlock(task.id, null, null)
                                    activeScheduleBlock = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Remove Time Block", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                } else {
                    block.reminder?.let { rem ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.toggleReminderCompleted(rem)
                                    activeScheduleBlock = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CalendarVioletDeep)
                                Text(if (rem.isCompleted) "Mark Incomplete" else "Mark Complete", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                block.reminder?.let { rem ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                activeScheduleBlock = null
                                reschedulingBlock = block
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Reschedule Date & Time", fontWeight = FontWeight.Medium)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                viewModel.snoozeReminder(rem, 30 * 60_000L)
                                activeScheduleBlock = null
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Snooze, contentDescription = null, tint = Color(0xFFD97706))
                            Text("Snooze +30 mins", fontWeight = FontWeight.Medium)
                        }
                    }
                }

                block.note?.let { note ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                activeScheduleBlock = null
                                if (note.isLocked && !viewModel.isVaultUnlocked.value) {
                                    Toast.makeText(context, "Locked note. Unlock Vault to open.", Toast.LENGTH_SHORT).show()
                                } else {
                                    onNavigateToEditor(note.id, note.noteType)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF059669))
                            Text("Open Linked Note", fontWeight = FontWeight.Medium)
                        }
                    }
                }

                if (block.task != null) {
                    val task = block.task
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                viewModel.deleteTask(task.id)
                                activeScheduleBlock = null
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text("Delete Task", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    block.reminder?.let { rem ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.deleteReminder(rem.id)
                                    activeScheduleBlock = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text("Delete Event", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                block.note?.let { linkedNote ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                block.task?.let { viewModel.deleteTask(it.id) }
                                    ?: block.reminder?.let { viewModel.deleteReminder(it.id) }
                                viewModel.softDeleteNote(linkedNote)
                                Toast.makeText(context, "Moved to Trash", Toast.LENGTH_SHORT).show()
                                activeScheduleBlock = null
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text("Move Task Note to Trash", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    // Time-Block Unscheduled Task Sheet
    if (showTimeBlockPickerSheet) {
        var chosenHour by remember { mutableIntStateOf(9) }
        var chosenDurationMins by remember { mutableIntStateOf(60) }
        ModalBottomSheet(onDismissRequest = { showTimeBlockPickerSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Time-Block Task on ${SmartNoteUtils.formatShortDate(selectedDayUtc)}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text("Start Time", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(8 to "8 AM", 9 to "9 AM", 11 to "11 AM", 14 to "2 PM", 16 to "4 PM", 19 to "7 PM").forEach { (h, label) ->
                        val active = chosenHour == h
                        Surface(
                            shape = CircleShape,
                            color = if (active) CalendarVioletDeep else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clip(CircleShape).clickable { chosenHour = h }
                        ) {
                            Text(
                                text = label,
                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(30 to "30m", 60 to "1hr", 90 to "1.5hr", 120 to "2hr").forEach { (mins, label) ->
                        val active = chosenDurationMins == mins
                        Surface(
                            shape = CircleShape,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clip(CircleShape).clickable { chosenDurationMins = mins }
                        ) {
                            Text(
                                text = label,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                if (unscheduledTasks.isEmpty()) {
                    Text(
                        text = "All active tasks are already time-blocked. Create a task in Kanban or tap Schedule.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    unscheduledTasks.take(8).forEach { task ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    val startCal = Calendar.getInstance().apply {
                                        timeInMillis = selectedDayUtc
                                        set(Calendar.HOUR_OF_DAY, chosenHour)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    val startUtc = startCal.timeInMillis
                                    val endUtc = startUtc + chosenDurationMins * 60_000L
                                    viewModel.assignTaskTimeBlock(task.id, startUtc, endUtc)
                                    showTimeBlockPickerSheet = false
                                }
                                .testTag("timeblock_assign_${task.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${TaskPriority.fromName(task.priority).label} · ${task.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("Assign", color = CalendarVioletDeep, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }

    reschedulingBlock?.reminder?.let { rem ->
        RescheduleReminderDialog(
            currentUtc = rem.scheduledTimeUtc,
            onDismiss = { reschedulingBlock = null },
            onConfirm = { newUtc ->
                viewModel.rescheduleReminder(rem, newUtc)
                reschedulingBlock = null
            }
        )
    }

    if (showCreateEventDialog) {
        CalendarEventDialog(
            defaultDateUtc = selectedDayUtc,
            onDismiss = { showCreateEventDialog = false },
            onConfirm = { title, desc, startUtc, endUtc, category, colorHex, repeat, subtasks ->
                viewModel.createCalendarEvent(
                    title = title,
                    description = desc,
                    startUtc = startUtc,
                    endUtc = endUtc,
                    category = category,
                    colorHex = colorHex,
                    repeatInterval = repeat,
                    checklistItems = subtasks
                ) {
                    selectedDayUtc = SmartNoteUtils.startOfDayUtc(startUtc)
                    showCreateEventDialog = false
                }
            }
        )
    }

    editingScheduleBlock?.let { block ->
        val durationMins = (((block.endUtc - block.startUtc) / 60_000L).toInt()).coerceIn(30, 240)
        val initialSubtasks = block.task?.let { SmartNoteUtils.parseChecklistJson(it.subtasksJson) }
            ?: block.note?.let { SmartNoteUtils.parseChecklistJson(it.checklistJson) }
            ?: emptyList()
        val initialRepeat = block.reminder?.repeatInterval
            ?: block.task?.recurrenceRule?.takeIf { it != TaskRecurrenceRule.NONE.name }
        CalendarEventDialog(
            defaultDateUtc = block.startUtc,
            initialTitle = block.title,
            initialDescription = block.note?.takeIf { !it.isLocked }?.content ?: block.task?.description.orEmpty(),
            initialStartUtc = block.startUtc,
            initialDurationMins = durationMins,
            initialCategory = block.category,
            initialRepeat = initialRepeat,
            initialSubtasks = initialSubtasks,
            dialogTitle = "Edit Calendar Event",
            onDismiss = { editingScheduleBlock = null },
            onConfirm = { title, desc, startUtc, endUtc, category, colorHex, repeat, subtasks ->
                viewModel.updateCalendarEvent(
                    reminderId = block.reminder?.id,
                    taskId = block.task?.id,
                    noteId = block.note?.id,
                    title = title,
                    description = desc,
                    startUtc = startUtc,
                    endUtc = endUtc,
                    category = category,
                    colorHex = colorHex,
                    repeatInterval = repeat,
                    checklistItems = subtasks
                ) {
                    selectedDayUtc = SmartNoteUtils.startOfDayUtc(startUtc)
                    editingScheduleBlock = null
                }
            }
        )
    }

    if (showTaskEditorDialog) {
        val initialReminderUtc = editingTaskEntity?.linkedReminderId?.let { rid ->
            allReminders.firstOrNull { it.id == rid }?.scheduledTimeUtc
        }
        TaskEditorDialog(
            initialTask = editingTaskEntity,
            initialColumnId = kanbanColumns.firstOrNull { !it.isDoneColumn }?.id ?: "col_todo",
            columns = kanbanColumns.sortedBy { it.position },
            allTasks = activeTasks,
            allDependencies = taskDependencies,
            eligibleNotes = allNotes.filter { !it.isLocked && !it.isHidden },
            initialReminderTimeUtc = initialReminderUtc,
            onDismiss = {
                showTaskEditorDialog = false
                editingTaskEntity = null
            },
            onSave = { task, selectedDeps ->
                viewModel.saveTaskWithReminder(task, selectedDeps, false, null)
                showTaskEditorDialog = false
                editingTaskEntity = null
            },
            onSaveWithReminder = { task, selectedDeps, reminderEnabled, reminderTimeUtc ->
                viewModel.saveTaskWithReminder(task, selectedDeps, reminderEnabled, reminderTimeUtc)
                showTaskEditorDialog = false
                editingTaskEntity = null
            },
            onDelete = { target ->
                viewModel.deleteTask(target.id)
                showTaskEditorDialog = false
                editingTaskEntity = null
            }
        )
    }
}

@Composable
private fun CalendarModePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = if (selected) CalendarVioletDeep else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            0.9.dp,
            if (selected) CalendarVioletDeep else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        ),
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun WeekStripSelector(
    weekDays: List<Long>,
    selectedDayUtc: Long,
    activeDayKeys: Set<Long>,
    onSelectDay: (Long) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit
) {
    val dayShortFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val dayNumFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }
    val sundayCalendar = remember { Calendar.getInstance() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onPreviousWeek)
                .testTag("calendar_prev_week_btn")
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous week",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            weekDays.forEach { dayUtc ->
                val isSelected = SmartNoteUtils.isSameDay(dayUtc, selectedDayUtc)
                val isToday = SmartNoteUtils.isSameDay(dayUtc, System.currentTimeMillis())
                val hasEvents = activeDayKeys.any { SmartNoteUtils.isSameDay(it, dayUtc) }
                val dayLabel = remember(dayUtc) { dayShortFormat.format(Date(dayUtc)) }
                val isSunday = remember(dayUtc) {
                    sundayCalendar.timeInMillis = dayUtc
                    sundayCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectDay(dayUtc) }
                        .padding(vertical = 4.dp)
                        .testTag("calendar_day_${dayNumFormat.format(Date(dayUtc))}")
                ) {
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (isSelected || isSunday) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.onBackground
                            isSunday -> CalendarVioletDeep
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .border(1.5.dp, Color(0xFFF5B749), CircleShape)
                                        .padding(2.dp)
                                } else {
                                    Modifier
                                }
                            )
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> CalendarViolet
                                    isToday -> CalendarViolet.copy(alpha = 0.14f)
                                    else -> MaterialTheme.colorScheme.surfaceContainerLow
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayNumFormat.format(Date(dayUtc)),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.5.sp
                            ),
                            color = when {
                                isSelected -> Color.White
                                isToday -> CalendarVioletDeep
                                isSunday -> CalendarVioletDeep
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasEvents) CalendarViolet else Color.Transparent
                            )
                    )
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onNextWeek)
                .testTag("calendar_next_week_btn")
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next week",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun TimelineScheduleRow(
    block: ScheduleBlock,
    isDark: Boolean,
    onToggleDone: () -> Unit,
    onClick: () -> Unit
) {
    val style = CalendarCategories.firstOrNull { it.label.equals(block.category, ignoreCase = true) }
        ?: CalendarCategories[block.colorIndex % CalendarCategories.size]
    val cardBg = if (isDark) style.darkBg else style.lightBg
    val pillBg = if (isDark) style.pillBgDark else style.pillBgLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        // Left Time Label (e.g., 08:30 AM)
        Text(
            text = SmartNoteUtils.formatTimeMarker(block.startUtc),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(56.dp)
                .padding(top = 8.dp, end = 10.dp)
        )

        // Right Pastel Card with Vertical Left Accent Bar (matching right screen in image.png)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(0.8.dp, style.accent.copy(alpha = if (isDark) 0.25f else 0.14f)),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(style.accent)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = block.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (block.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (block.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = pillBg
                        ) {
                            Text(
                                text = SmartNoteUtils.formatTimeRange(block.startUtc, block.endUtc),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (isDark) Color.White.copy(alpha = 0.9f) else style.accent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = block.category,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (block.hasConflict) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "Time overlap conflict",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "Overlap",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                if (block.reminder != null || block.task != null) {
                    IconButton(
                        onClick = onToggleDone,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (block.isCompleted) style.accent else Color.Transparent
                                )
                                .border(
                                    1.6.dp,
                                    if (block.isCompleted) style.accent else style.accent.copy(alpha = 0.65f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (block.isCompleted) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
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
private fun TimelineBreakRow(
    breakStartUtc: Long,
    breakEndUtc: Long,
    isDark: Boolean
) {
    val diffMins = ((breakEndUtc - breakStartUtc) / 60_000L).coerceAtLeast(1L)
    val breakLabel = if (diffMins >= 60) {
        val hrs = diffMins / 60
        val remMins = diffMins % 60
        if (remMins == 0L) "Break ${hrs}hr" else "Break ${hrs}h ${remMins}m"
    } else {
        "Break ${diffMins}m"
    }

    val stripeColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFE5E7EB).copy(alpha = 0.65f)
    val lineColor = CalendarViolet.copy(alpha = 0.45f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(56.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDark) Color.White.copy(alpha = 0.03f) else Color(0xFFF8FAFC))
                .padding(vertical = 8.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val step = 14.dp.toPx()
                var x = -size.height
                while (x < size.width + size.height) {
                    drawLine(
                        color = stripeColor,
                        start = Offset(x, size.height),
                        end = Offset(x + size.height, 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                    x += step
                }
                drawLine(
                    color = lineColor,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 1.2.dp.toPx()
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = CalendarViolet
                    ) {
                        Text(
                            text = breakLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.5.sp
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = SmartNoteUtils.formatTimeRange(breakStartUtc, breakEndUtc),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyHeaderSelector(
    selectedDayUtc: Long,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .testTag("calendar_daily_header")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPreviousDay, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
            }
            Text(
                text = formatter.format(Date(selectedDayUtc)),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            IconButton(onClick = onNextDay, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
            }
        }
    }
}

@Composable
private fun MonthlyCalendarGrid(
    selectedDayUtc: Long,
    activeTasks: List<TaskEntity>,
    standaloneReminders: List<ReminderEntity>,
    allNotes: List<NoteEntity>,
    onSelectDay: (Long) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val monthTitle = remember(selectedDayUtc) { monthFormat.format(Date(selectedDayUtc)) }
    val monthCells = remember(selectedDayUtc) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = selectedDayUtc
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        // Convert Calendar.DAY_OF_WEEK (Sun=1, Mon=2..Sat=7) into 0-based Monday-first offset
        val firstDowOffset = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7)
        val cells = mutableListOf<Long?>()
        repeat(firstDowOffset) { cells.add(null) }
        for (d in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, d)
            cells.add(cal.timeInMillis)
        }
        while (cells.size % 7 != 0) cells.add(null)
        cells
    }
    val todayUtc = SmartNoteUtils.startOfDayUtc(System.currentTimeMillis())

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .testTag("calendar_monthly_grid")
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                }
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onNextMonth, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { dow ->
                    Text(
                        text = dow,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            monthCells.chunked(7).forEach { weekRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    weekRow.forEach { dayUtc ->
                        if (dayUtc == null) {
                            Spacer(modifier = Modifier.weight(1f).height(38.dp))
                        } else {
                            val cal = Calendar.getInstance().apply { timeInMillis = dayUtc }
                            val dayNum = cal.get(Calendar.DAY_OF_MONTH)
                            val isSelected = SmartNoteUtils.isSameDay(dayUtc, selectedDayUtc)
                            val isToday = SmartNoteUtils.isSameDay(dayUtc, todayUtc)
                            val hasEvents = remember(dayUtc, activeTasks, standaloneReminders, allNotes) {
                                activeTasks.any { t ->
                                    val ts = t.scheduledStartUtc ?: t.dueAtUtc ?: return@any false
                                    matchesRecurrenceOnDay(ts, dayUtc, t.recurrenceRule)
                                } || standaloneReminders.any { r ->
                                    matchesRecurrenceOnDay(r.scheduledTimeUtc, dayUtc, r.repeatInterval)
                                } || allNotes.any { n ->
                                    n.reminderTimestamp != null && SmartNoteUtils.isSameDay(n.reminderTimestamp, dayUtc)
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isSelected -> CalendarVioletDeep
                                            isToday -> CalendarViolet.copy(alpha = 0.16f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable { onSelectDay(dayUtc) }
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = when {
                                        isSelected -> Color.White
                                        isToday -> CalendarVioletDeep
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (hasEvents) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else CalendarVioletDeep)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyTasksContent(
    activeTasks: List<TaskEntity>,
    kanbanColumns: List<com.example.data.model.KanbanColumnEntity>,
    blockedTaskIds: Set<String>,
    checklistNotes: List<NoteEntity>,
    standaloneReminders: List<ReminderEntity>,
    allNotes: List<NoteEntity>,
    doneCount: Int,
    totalCount: Int,
    isDark: Boolean,
    navBottomPadding: androidx.compose.ui.unit.Dp,
    onToggleTask: (TaskEntity) -> Unit,
    onToggleTaskSubtask: (TaskEntity, Int) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onOpenKanban: () -> Unit,
    onToggleChecklistItem: (NoteEntity, Int) -> Unit,
    onToggleReminder: (ReminderEntity) -> Unit,
    onTrashNote: (NoteEntity) -> Unit,
    onTrashReminderTask: (ReminderEntity, NoteEntity?) -> Unit,
    onOpenNote: (NoteEntity) -> Unit,
    onCreateChecklistNote: () -> Unit,
    onScheduleTask: () -> Unit,
    onCreateTask: () -> Unit = {},
    onEditTask: (TaskEntity) -> Unit = {}
) {
    val progress = if (totalCount > 0) doneCount.toFloat() / totalCount.toFloat() else 0f
    val colTitleById = remember(kanbanColumns) { kanbanColumns.associate { it.id to it.title } }
    var statusFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, DONE
    var categoryFilter by remember { mutableStateOf<String?>(null) }

    val filteredTasks = remember(activeTasks, statusFilter, categoryFilter) {
        activeTasks.filter { task ->
            val matchesStatus = when (statusFilter) {
                "PENDING" -> !task.isCompleted
                "DONE" -> task.isCompleted
                else -> true
            }
            val matchesCategory = categoryFilter == null || task.category.equals(categoryFilter, ignoreCase = true)
            matchesStatus && matchesCategory
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 96.dp + navBottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Progress Summary Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF231E36) else Color(0xFFF3EFFE)
                ),
                border = BorderStroke(0.8.dp, CalendarViolet.copy(alpha = 0.30f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Task Completion",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$doneCount of $totalCount completed (${(progress * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClick = onOpenKanban)
                                    .testTag("tasks_open_kanban_btn")
                            ) {
                                Text(
                                    text = "Kanban",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CalendarVioletDeep,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClick = onCreateTask)
                                    .testTag("tasks_add_task_button")
                            ) {
                                Text(
                                    text = "+ Task",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = CalendarVioletDeep,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }

        if (activeTasks.isEmpty() && checklistNotes.isEmpty() && standaloneReminders.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No daily tasks or checklists yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Create a scheduled task, Kanban card, or checklist note to track your daily progress here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onScheduleTask,
                                colors = ButtonDefaults.buttonColors(containerColor = CalendarVioletDeep),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Schedule Task")
                            }
                            TextButton(onClick = onOpenKanban) {
                                Text("Open Kanban")
                            }
                        }
                    }
                }
            }
        }

        // Task Status & Category Filter Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("ALL" to "All", "PENDING" to "Pending", "DONE" to "Done").forEach { (key, label) ->
                    val active = statusFilter == key && categoryFilter == null
                    Surface(
                        shape = CircleShape,
                        color = if (active) CalendarVioletDeep else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(0.8.dp, if (active) CalendarVioletDeep else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                statusFilter = key
                                categoryFilter = null
                            }
                            .testTag("tasks_filter_${key.lowercase()}")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                CalendarCategories.forEach { cat ->
                    val active = categoryFilter == cat.label
                    Surface(
                        shape = CircleShape,
                        color = if (active) cat.accent else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(0.8.dp, if (active) cat.accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                categoryFilter = if (active) null else cat.label
                            }
                            .testTag("tasks_cat_${cat.label.lowercase()}")
                    ) {
                        Text(
                            text = cat.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Kanban & Structured Tasks Section
        if (filteredTasks.isNotEmpty()) {
            item {
                Text(
                    text = "Workspace Tasks (${filteredTasks.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(
                filteredTasks.sortedWith(
                    compareBy<TaskEntity> { it.isCompleted }
                        .thenByDescending { ProductivityEngine.prioritySortWeight(it.priority) }
                        .thenBy { it.dueAtUtc ?: Long.MAX_VALUE }
                ),
                key = { "task_${it.id}" }
            ) { task ->
                val linkedNote = task.linkedNoteId?.let { id -> allNotes.firstOrNull { it.id == id } }
                val isLockedLinked = linkedNote?.isLocked == true || linkedNote?.isHidden == true
                val displayTitle = if (isLockedLinked && (task.id.startsWith("task_cal_") || task.id.startsWith("backfill_"))) {
                    "Locked Note Reminder"
                } else {
                    task.title.ifBlank { "Task" }
                }
                val subtasks = SmartNoteUtils.parseChecklistJson(task.subtasksJson)
                val catStyle = CalendarCategories.firstOrNull { it.label.equals(task.category, ignoreCase = true) }
                    ?: CalendarCategories[1]
                val cardBg = if (isDark) catStyle.darkBg else catStyle.lightBg
                val isBlocked = task.id in blockedTaskIds
                val priority = TaskPriority.fromName(task.priority)
                val recurrence = TaskRecurrenceRule.fromName(task.recurrenceRule)

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(0.8.dp, catStyle.accent.copy(alpha = 0.22f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onEditTask(task) }
                        .testTag("task_card_${task.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onToggleTask(task) },
                                    modifier = Modifier.size(48.dp).testTag("task_toggle_${task.id}")
                                ) {
                                    Icon(
                                        imageVector = if (task.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = "Toggle ${task.title}",
                                        tint = catStyle.accent
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteTask(task) },
                                    modifier = Modifier.size(48.dp).testTag("task_trash_task_${task.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Task",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }

                        if (!isLockedLinked && task.description.isNotBlank()) {
                            Text(
                                text = task.description.trim(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        if (subtasks.isNotEmpty()) {
                            subtasks.take(3).forEachIndexed { idx, sub ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onToggleTaskSubtask(task, idx) }
                                        .padding(vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = if (sub.isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = "Toggle subtask ${sub.text}",
                                        tint = catStyle.accent,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = sub.text,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            textDecoration = if (sub.isDone) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            colTitleById[task.columnId]?.let { colTitle ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CalendarViolet.copy(alpha = if (isDark) 0.25f else 0.16f)
                                ) {
                                    Text(
                                        text = colTitle,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isDark) Color.White else CalendarVioletDeep,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = catStyle.accent.copy(alpha = if (isDark) 0.25f else 0.16f)
                            ) {
                                Text(
                                    text = priority.label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = catStyle.accent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            (task.scheduledStartUtc ?: task.dueAtUtc)?.let { targetUtc ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = SmartNoteUtils.formatShortDate(targetUtc),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            if (recurrence != TaskRecurrenceRule.NONE) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = recurrence.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            if (linkedNote != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onOpenNote(linkedNote) }
                                        .testTag("task_open_note_${task.id}")
                                ) {
                                    Text(
                                        text = if (isLockedLinked) "Locked Note" else "Open Note",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            if (isBlocked) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = "Blocked",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Standalone Scheduled Reminders section (excluding those already listed under Workspace Tasks)
        if (standaloneReminders.isNotEmpty()) {
            item {
                Text(
                    text = "Standalone Reminders (${standaloneReminders.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(standaloneReminders.sortedBy { it.scheduledTimeUtc }, key = { "rem_${it.id}" }) { rem ->
                val note = allNotes.firstOrNull { it.id == rem.noteId }
                val meta = note?.let { SmartNoteUtils.parseCalendarMeta(it.canvasDataJson) }
                val catLabel = meta?.category ?: inferCategory(note)
                val catStyle = CalendarCategories.firstOrNull { it.label.equals(catLabel, ignoreCase = true) }
                    ?: CalendarCategories[1]
                val cardBg = if (isDark) catStyle.darkBg else catStyle.lightBg

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(0.8.dp, catStyle.accent.copy(alpha = 0.22f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            if (note != null) onOpenNote(note)
                        }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val isLockedNote = note?.isLocked == true
                            Text(
                                text = when {
                                    isLockedNote -> "Locked Note Reminder"
                                    else -> note?.title?.ifBlank { "Scheduled Task" } ?: "Scheduled Task"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (rem.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onToggleReminder(rem) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (rem.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = "Toggle Task",
                                        tint = catStyle.accent
                                    )
                                }
                                IconButton(
                                    onClick = { onTrashReminderTask(rem, note) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("task_trash_rem_${rem.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Move Task to Trash",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }

                        val noteContent = if (note?.isLocked == true) "" else note?.content?.trim().orEmpty()
                        if (noteContent.isNotEmpty()) {
                            Text(
                                text = noteContent,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CalendarViolet.copy(alpha = if (isDark) 0.25f else 0.16f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Event, contentDescription = null, tint = CalendarVioletDeep, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = SmartNoteUtils.formatShortDate(rem.scheduledTimeUtc),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isDark) Color.White else CalendarVioletDeep
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = catStyle.accent.copy(alpha = if (isDark) 0.25f else 0.16f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.LocalOffer, contentDescription = null, tint = catStyle.accent, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = catLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = catStyle.accent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Checklist Notes section
        if (checklistNotes.isNotEmpty()) {
            item {
                Text(
                    text = "Checklist Notes (${checklistNotes.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            items(checklistNotes, key = { "chk_${it.id}" }) { note ->
                val itemsList = remember(note.checklistJson) {
                    SmartNoteUtils.parseChecklistJson(note.checklistJson)
                }
                val styleIdx = kotlin.math.abs(note.id.hashCode()) % CalendarCategories.size
                val catStyle = CalendarCategories[styleIdx]
                val cardBg = if (isDark) catStyle.darkBg else catStyle.lightBg

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(0.8.dp, catStyle.accent.copy(alpha = 0.22f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onOpenNote(note) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = note.title.ifBlank { "Untitled Checklist" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onTrashNote(note) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("task_trash_note_${note.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Move Task Note to Trash",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        itemsList.take(4).forEachIndexed { idx, taskItem ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onToggleChecklistItem(note, idx) }
                                    .padding(vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = if (taskItem.isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = "Toggle ${taskItem.text}",
                                    tint = catStyle.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = taskItem.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textDecoration = if (taskItem.isDone) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (taskItem.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (itemsList.size > 4) {
                            Text(
                                text = "+${itemsList.size - 4} more tasks",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CalendarViolet.copy(alpha = if (isDark) 0.25f else 0.16f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Event, contentDescription = null, tint = CalendarVioletDeep, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = SmartNoteUtils.formatShortDate(note.updatedAtUtc),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isDark) Color.White else CalendarVioletDeep
                                    )
                                }
                            }

                            val doneHere = itemsList.count { it.isDone }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = catStyle.accent.copy(alpha = if (isDark) 0.25f else 0.16f)
                            ) {
                                Text(
                                    text = "$doneHere/${itemsList.size} done",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = catStyle.accent,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
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
fun CalendarEventDialog(
    defaultDateUtc: Long,
    initialTitle: String = "",
    initialDescription: String = "",
    initialStartUtc: Long? = null,
    initialDurationMins: Int = 60,
    initialCategory: String = "Work",
    initialRepeat: String? = null,
    initialSubtasks: List<ChecklistItem> = emptyList(),
    dialogTitle: String = "Schedule Task / Event",
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        startUtc: Long,
        endUtc: Long,
        category: String,
        colorHex: String,
        repeat: String?,
        subtasks: List<ChecklistItem>
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var selectedCategory by remember {
        mutableStateOf(
            CalendarCategories.firstOrNull { it.label.equals(initialCategory, ignoreCase = true) }
                ?: CalendarCategories[1]
        )
    }
    var selectedDurationMins by remember { mutableIntStateOf(initialDurationMins) }
    var selectedRepeat by remember { mutableStateOf(initialRepeat) }
    var newSubtaskText by remember { mutableStateOf("") }
    val subtasks = remember { mutableStateListOf<ChecklistItem>().apply { addAll(initialSubtasks) } }

    // Default start time: initialStartUtc or selected date at next rounded hour
    var chosenStartUtc by remember {
        mutableLongStateOf(
            initialStartUtc ?: Calendar.getInstance().apply {
                val now = Calendar.getInstance()
                timeInMillis = defaultDateUtc
                val baseHour = if (SmartNoteUtils.isSameDay(defaultDateUtc, System.currentTimeMillis())) {
                    (now.get(Calendar.HOUR_OF_DAY) + 1).coerceIn(8, 22)
                } else {
                    9
                }
                set(Calendar.HOUR_OF_DAY, baseHour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                dialogTitle,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g., Client Meeting)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calendar_event_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date & Start Time pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val hostActivity = context.findCalendarActivity()
                                if (hostActivity != null && !hostActivity.isFinishing && !hostActivity.isDestroyed) {
                                    try {
                                        val c = Calendar.getInstance().apply { timeInMillis = chosenStartUtc }
                                        DatePickerDialog(
                                            hostActivity,
                                            { _, y, m, d ->
                                                val updated = Calendar.getInstance().apply {
                                                    timeInMillis = chosenStartUtc
                                                    set(Calendar.YEAR, y)
                                                    set(Calendar.MONTH, m)
                                                    set(Calendar.DAY_OF_MONTH, d)
                                                }
                                                chosenStartUtc = updated.timeInMillis
                                            },
                                            c.get(Calendar.YEAR),
                                            c.get(Calendar.MONTH),
                                            c.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp), tint = CalendarVioletDeep)
                            Text(SmartNoteUtils.formatShortDate(chosenStartUtc), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val hostActivity = context.findCalendarActivity()
                                if (hostActivity != null && !hostActivity.isFinishing && !hostActivity.isDestroyed) {
                                    try {
                                        val c = Calendar.getInstance().apply { timeInMillis = chosenStartUtc }
                                        TimePickerDialog(
                                            hostActivity,
                                            { _, hour, minute ->
                                                val updated = Calendar.getInstance().apply {
                                                    timeInMillis = chosenStartUtc
                                                    set(Calendar.HOUR_OF_DAY, hour)
                                                    set(Calendar.MINUTE, minute)
                                                    set(Calendar.SECOND, 0)
                                                }
                                                chosenStartUtc = updated.timeInMillis
                                            },
                                            c.get(Calendar.HOUR_OF_DAY),
                                            c.get(Calendar.MINUTE),
                                            false
                                        ).show()
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = CalendarVioletDeep)
                            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                            Text(sdf.format(Date(chosenStartUtc)), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Duration Pills
                Text("Duration", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    listOf(30 to "30m", 60 to "1hr", 90 to "1.5hr", 120 to "2hr").forEach { (mins, label) ->
                        val active = selectedDurationMins == mins
                        Surface(
                            shape = CircleShape,
                            color = if (active) CalendarVioletDeep else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { selectedDurationMins = mins }
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Category Pills
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    CalendarCategories.forEach { cat ->
                        val active = selectedCategory.label == cat.label
                        Surface(
                            shape = CircleShape,
                            color = if (active) cat.accent else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Optional quick checklist task
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newSubtaskText,
                        onValueChange = { newSubtaskText = it },
                        label = { Text("Add checklist item") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (newSubtaskText.isNotBlank()) {
                                subtasks.add(
                                    ChecklistItem(
                                        id = UUID.randomUUID().toString(),
                                        text = newSubtaskText.trim(),
                                        isDone = false
                                    )
                                )
                                newSubtaskText = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add sub-task", tint = CalendarVioletDeep)
                    }
                }
                if (subtasks.isNotEmpty()) {
                    Text(
                        "${subtasks.size} task(s) attached",
                        style = MaterialTheme.typography.labelSmall,
                        color = CalendarVioletDeep
                    )
                }

                // Repeat
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    listOf(
                        null to "Once",
                        "DAILY" to "Daily",
                        "WEEKDAYS" to "Weekdays",
                        "WEEKLY" to "Weekly",
                        "MONTHLY" to "Monthly"
                    ).forEach { (rep, label) ->
                        val active = selectedRepeat == rep
                        Surface(
                            shape = CircleShape,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { selectedRepeat = rep }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val endUtc = chosenStartUtc + (selectedDurationMins * 60_000L)
                    onConfirm(
                        title.ifBlank { "Scheduled Task" },
                        description,
                        chosenStartUtc,
                        endUtc,
                        selectedCategory.label,
                        selectedCategory.colorHex,
                        selectedRepeat,
                        subtasks.toList()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CalendarVioletDeep),
                modifier = Modifier.testTag("calendar_save_event_btn")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun matchesRecurrenceOnDay(
    sourceUtc: Long,
    targetDayUtc: Long,
    rule: String?
): Boolean {
    if (SmartNoteUtils.isSameDay(sourceUtc, targetDayUtc)) return true
    if (SmartNoteUtils.startOfDayUtc(sourceUtc) > targetDayUtc) return false
    return when (rule?.uppercase(Locale.US)) {
        "DAILY" -> true
        "WEEKDAYS" -> {
            val cal = Calendar.getInstance().apply { timeInMillis = targetDayUtc }
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            dow in Calendar.MONDAY..Calendar.FRIDAY
        }
        "WEEKLY" -> {
            val c1 = Calendar.getInstance().apply { timeInMillis = sourceUtc }
            val c2 = Calendar.getInstance().apply { timeInMillis = targetDayUtc }
            c1.get(Calendar.DAY_OF_WEEK) == c2.get(Calendar.DAY_OF_WEEK)
        }
        "MONTHLY" -> {
            val c1 = Calendar.getInstance().apply { timeInMillis = sourceUtc }
            val c2 = Calendar.getInstance().apply { timeInMillis = targetDayUtc }
            val desiredDom = c1.get(Calendar.DAY_OF_MONTH)
            val maxDom = c2.getActualMaximum(Calendar.DAY_OF_MONTH)
            c2.get(Calendar.DAY_OF_MONTH) == desiredDom.coerceAtMost(maxDom)
        }
        else -> false
    }
}

private fun alignTimeToDay(sourceTimeUtc: Long, targetDayUtc: Long): Long {
    val src = Calendar.getInstance().apply { timeInMillis = sourceTimeUtc }
    return Calendar.getInstance().apply {
        timeInMillis = targetDayUtc
        set(Calendar.HOUR_OF_DAY, src.get(Calendar.HOUR_OF_DAY))
        set(Calendar.MINUTE, src.get(Calendar.MINUTE))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun inferCategory(note: NoteEntity?): String {
    if (note == null) return "Focus"
    val tags = note.tagsCsv.lowercase()
    return when {
        tags.contains("health") || tags.contains("workout") -> "Health"
        tags.contains("work") || tags.contains("client") || tags.contains("meeting") -> "Work"
        tags.contains("important") || tags.contains("priority") -> "High Priority"
        else -> "Focus"
    }
}

private tailrec fun Context.findCalendarActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findCalendarActivity()
    else -> null
}
