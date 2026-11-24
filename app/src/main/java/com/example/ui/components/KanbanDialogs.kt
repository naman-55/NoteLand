package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.data.model.KanbanColumnEntity
import com.example.data.model.NoteEntity
import com.example.data.model.TaskDependencyEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskRecurrenceRule
import com.example.util.ChecklistItem
import com.example.util.KnowledgeBrainEngine
import com.example.util.ProductivityEngine
import com.example.util.SmartNoteUtils
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

private val KanbanColumnPalette = listOf(
    "#6366F1", "#F59E0B", "#8B5CF6", "#10B981", "#3B82F6", "#EC4899", "#EF4444", "#0D9488"
)

fun parseHexColorSafe(hex: String, fallback: Color = Color(0xFF6366F1)): Color {
    return try {
        Color(hex.toColorInt())
    } catch (_: Exception) {
        fallback
    }
}

@Composable
fun TaskEditorDialog(
    initialTask: TaskEntity?,
    initialColumnId: String,
    columns: List<KanbanColumnEntity>,
    allTasks: List<TaskEntity>,
    allDependencies: List<TaskDependencyEntity>,
    eligibleNotes: List<NoteEntity>,
    initialReminderTimeUtc: Long? = null,
    onDismiss: () -> Unit,
    onSave: (
        task: TaskEntity,
        selectedDependencyIds: Set<String>
    ) -> Unit,
    onSaveWithReminder: ((
        task: TaskEntity,
        selectedDependencyIds: Set<String>,
        reminderEnabled: Boolean,
        reminderTimeUtc: Long?
    ) -> Unit)? = null,
    onDelete: ((TaskEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var columnId by remember {
        mutableStateOf(
            initialTask?.columnId?.takeIf { id -> columns.any { it.id == id } }
                ?: initialColumnId.takeIf { id -> columns.any { it.id == id } }
                ?: columns.firstOrNull()?.id.orEmpty()
        )
    }
    var priority by remember { mutableStateOf(TaskPriority.fromName(initialTask?.priority)) }
    var recurrence by remember { mutableStateOf(TaskRecurrenceRule.fromName(initialTask?.recurrenceRule)) }
    var recurrenceInterval by remember { mutableIntStateOf(initialTask?.recurrenceInterval?.coerceAtLeast(1) ?: 1) }
    var category by remember { mutableStateOf(initialTask?.category?.ifBlank { "Work" } ?: "Work") }
    var dueAtUtc by remember { mutableStateOf(initialTask?.dueAtUtc) }
    var scheduledStartUtc by remember { mutableStateOf(initialTask?.scheduledStartUtc) }
    var estimatedMinutes by remember { mutableIntStateOf(initialTask?.estimatedMinutes ?: 30) }
    var linkedNoteId by remember { mutableStateOf(initialTask?.linkedNoteId) }
    var reminderEnabled by remember {
        mutableStateOf(!initialTask?.linkedReminderId.isNullOrBlank() || initialReminderTimeUtc != null)
    }
    var reminderTimeUtc by remember {
        mutableStateOf(
            initialReminderTimeUtc
                ?: initialTask?.scheduledStartUtc
                ?: initialTask?.dueAtUtc
                ?: (System.currentTimeMillis() + 3600_000L)
        )
    }

    val initialDeps = remember(initialTask, allDependencies) {
        if (initialTask == null) emptySet()
        else allDependencies.filter { it.taskId == initialTask.id }.map { it.dependsOnTaskId }.toSet()
    }
    var selectedDeps by remember { mutableStateOf(initialDeps) }
    val subtasks = remember {
        mutableStateListOf<ChecklistItem>().apply {
            addAll(SmartNoteUtils.parseChecklistJson(initialTask?.subtasksJson.orEmpty()))
        }
    }
    var newSubtaskText by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val taskIdForCycleCheck = initialTask?.id ?: "new_temp_task"
    val candidatePrereqs = remember(allTasks, initialTask) {
        allTasks.filter { it.id != initialTask?.id && !it.isArchived }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialTask == null) "New Kanban Task" else "Edit Task",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                if (initialTask != null && onDelete != null) {
                    IconButton(
                        onClick = { onDelete(initialTask) },
                        modifier = Modifier.size(48.dp).testTag("task_dialog_delete_btn")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete task",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationError = null
                    },
                    label = { Text("Task Title") },
                    singleLine = true,
                    isError = validationError != null,
                    supportingText = validationError?.let { err -> { Text(err) } },
                    modifier = Modifier.fillMaxWidth().testTag("task_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Details (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("task_desc_input")
                )

                // Column selection
                if (columns.isNotEmpty()) {
                    Text("Column", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        columns.forEach { col ->
                            val selected = col.id == columnId
                            val accent = parseHexColorSafe(col.colorHex)
                            Surface(
                                shape = CircleShape,
                                color = if (selected) accent else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { columnId = col.id }
                                    .testTag("task_col_chip_${col.id}")
                            ) {
                                Text(
                                    text = col.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Priority chips
                Text("Priority", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    TaskPriority.entries.forEach { p ->
                        val selected = priority == p
                        val accent = parseHexColorSafe(p.colorHex)
                        Surface(
                            shape = CircleShape,
                            color = if (selected) accent else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { priority = p }
                                .testTag("task_priority_${p.name.lowercase()}")
                        ) {
                            Text(
                                text = p.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Category chips
                Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    listOf("Work", "Focus", "Health", "Personal", "High Priority").forEach { cat ->
                        val selected = category == cat
                        Surface(
                            shape = CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { category = cat }
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Due Date & Time Block
                Text("Schedule & Due Date", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val act = context.findHostActivity()
                            if (act != null && !act.isFinishing && !act.isDestroyed) {
                                val cal = Calendar.getInstance().apply {
                                    timeInMillis = dueAtUtc ?: System.currentTimeMillis()
                                }
                                runCatching {
                                    DatePickerDialog(
                                        act,
                                        { _, y, m, d ->
                                            val updated = Calendar.getInstance().apply {
                                                timeInMillis = dueAtUtc ?: System.currentTimeMillis()
                                                set(y, m, d, get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE), 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            dueAtUtc = updated.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                            } else {
                                // Fallback increment by 1 day in headless test context
                                dueAtUtc = (dueAtUtc ?: System.currentTimeMillis()) + 24 * 3600_000L
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("task_due_date_btn")
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = dueAtUtc?.let { SmartNoteUtils.formatShortDate(it) } ?: "Set Due Date",
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (dueAtUtc != null) {
                        TextButton(
                            onClick = {
                                dueAtUtc = null
                                scheduledStartUtc = null
                            }
                        ) {
                            Text("Clear")
                        }
                    }
                }

                // Reminder Toggle & Time Picker
                if (onSaveWithReminder != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Task Reminder",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (reminderEnabled) {
                                    SmartNoteUtils.formatTimestamp(reminderTimeUtc ?: System.currentTimeMillis())
                                } else {
                                    "Notify me before deadline"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { checked ->
                                reminderEnabled = checked
                                if (checked && reminderTimeUtc <= 0L) {
                                    reminderTimeUtc = dueAtUtc ?: (System.currentTimeMillis() + 3600_000L)
                                }
                            },
                            modifier = Modifier.testTag("task_reminder_switch")
                        )
                    }
                    if (reminderEnabled) {
                        OutlinedButton(
                            onClick = {
                                val act = context.findHostActivity()
                                val base = reminderTimeUtc ?: dueAtUtc ?: System.currentTimeMillis()
                                if (act != null && !act.isFinishing && !act.isDestroyed) {
                                    val cal = Calendar.getInstance().apply { timeInMillis = base }
                                    runCatching {
                                        TimePickerDialog(
                                            act,
                                            { _, h, m ->
                                                val updated = Calendar.getInstance().apply {
                                                    timeInMillis = dueAtUtc ?: base
                                                    set(Calendar.HOUR_OF_DAY, h)
                                                    set(Calendar.MINUTE, m)
                                                    set(Calendar.SECOND, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }
                                                reminderTimeUtc = updated.timeInMillis
                                            },
                                            cal.get(Calendar.HOUR_OF_DAY),
                                            cal.get(Calendar.MINUTE),
                                            false
                                        ).show()
                                    }
                                } else {
                                    reminderTimeUtc = base + 3600_000L
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("task_reminder_time_btn")
                        ) {
                            Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(
                                text = "Reminder: ${SmartNoteUtils.formatTimestamp(reminderTimeUtc ?: System.currentTimeMillis())}",
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Recurrence
                Text("Recurrence", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    TaskRecurrenceRule.entries.forEach { r ->
                        val selected = recurrence == r
                        Surface(
                            shape = CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { recurrence = r }
                                .testTag("task_recurrence_${r.name.lowercase()}")
                        ) {
                            Text(
                                text = r.label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Subtasks
                Text("Subtasks (${subtasks.size})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newSubtaskText,
                        onValueChange = { newSubtaskText = it },
                        placeholder = { Text("Add subtask...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("task_subtask_input")
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
                        },
                        modifier = Modifier.size(48.dp).testTag("task_add_subtask_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add subtask")
                    }
                }
                subtasks.forEachIndexed { idx, item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = item.isDone,
                            onCheckedChange = { subtasks[idx] = item.copy(isDone = it) }
                        )
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = { subtasks.removeAt(idx) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove subtask", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Link to unlocked note
                val unlockedEligible = remember(eligibleNotes) {
                    eligibleNotes.filter { KnowledgeBrainEngine.isNoteKnowledgeEligible(it) }.take(20)
                }
                if (unlockedEligible.isNotEmpty()) {
                    Text("Link Note (Unlocked only)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (linkedNoteId == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { linkedNoteId = null }
                        ) {
                            Text(
                                text = "None",
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        unlockedEligible.forEach { note ->
                            val selected = linkedNoteId == note.id
                            Surface(
                                shape = CircleShape,
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { linkedNoteId = note.id }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = note.title.ifBlank { "Untitled" }.take(20),
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Dependencies (blocked by)
                if (candidatePrereqs.isNotEmpty()) {
                    Text("Blocked By Prerequisites", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        candidatePrereqs.take(8).forEach { prereq ->
                            val wouldCycle = ProductivityEngine.wouldCreateDependencyCycle(
                                taskId = taskIdForCycleCheck,
                                dependsOnTaskId = prereq.id,
                                existingDependencies = allDependencies
                            )
                            val isChecked = prereq.id in selectedDeps
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = !wouldCycle) {
                                        selectedDeps = if (isChecked) {
                                            selectedDeps - prereq.id
                                        } else {
                                            selectedDeps + prereq.id
                                        }
                                    }
                                    .padding(vertical = 2.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    enabled = !wouldCycle,
                                    onCheckedChange = { checked ->
                                        if (!wouldCycle) {
                                            selectedDeps = if (checked) selectedDeps + prereq.id else selectedDeps - prereq.id
                                        }
                                    }
                                )
                                Text(
                                    text = if (wouldCycle) "${prereq.title} (Cycle prevented)" else prereq.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (wouldCycle) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f) else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = title.trim()
                    if (trimmed.isBlank()) {
                        validationError = "Please enter a task title"
                        return@Button
                    }
                    val colIsDone = columns.firstOrNull { it.id == columnId }?.isDoneColumn == true
                    val now = System.currentTimeMillis()
                    val task = (initialTask ?: TaskEntity(title = trimmed)).copy(
                        title = trimmed,
                        description = description.trim(),
                        columnId = columnId,
                        priority = priority.name,
                        isCompleted = if (initialTask == null) colIsDone else initialTask.isCompleted,
                        completedAtUtc = if (initialTask == null && colIsDone) now else initialTask?.completedAtUtc,
                        dueAtUtc = dueAtUtc ?: if (recurrence != TaskRecurrenceRule.NONE) now else null,
                        dueTimeZoneId = TimeZone.getDefault().id,
                        scheduledStartUtc = scheduledStartUtc,
                        estimatedMinutes = estimatedMinutes,
                        recurrenceRule = recurrence.name,
                        recurrenceInterval = recurrenceInterval.coerceAtLeast(1),
                        subtasksJson = if (subtasks.isEmpty()) "" else SmartNoteUtils.encodeChecklistJson(subtasks),
                        linkedNoteId = linkedNoteId,
                        category = category,
                        colorHex = priority.colorHex
                    )
                    if (onSaveWithReminder != null) {
                        onSaveWithReminder(
                            task,
                            selectedDeps,
                            reminderEnabled,
                            if (reminderEnabled) (reminderTimeUtc ?: task.dueAtUtc ?: (now + 3600_000L)) else null
                        )
                    } else {
                        onSave(task, selectedDeps)
                    }
                },
                modifier = Modifier.testTag("task_save_button")
            ) {
                Text("Save Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun KanbanColumnManagerDialog(
    columns: List<KanbanColumnEntity>,
    onDismiss: () -> Unit,
    onSaveColumn: (KanbanColumnEntity) -> Unit,
    onReorderColumn: (String, Int) -> Unit,
    onDeleteColumn: (String) -> Unit
) {
    var editingColumn by remember { mutableStateOf<KanbanColumnEntity?>(null) }
    var colTitle by remember { mutableStateOf("") }
    var colColor by remember { mutableStateOf(KanbanColumnPalette.first()) }
    var colWip by remember { mutableStateOf("0") }
    var colIsDone by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Manage Kanban Columns",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                columns.sortedBy { it.position }.forEachIndexed { idx, col ->
                    val accent = parseHexColorSafe(col.colorHex)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(accent)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = col.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = buildString {
                                        if (col.wipLimit > 0) append("WIP ${col.wipLimit}") else append("No WIP limit")
                                        if (col.isDoneColumn) append(" · Done column")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onReorderColumn(col.id, -1) },
                                enabled = idx > 0,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Move column left", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { onReorderColumn(col.id, 1) },
                                enabled = idx < columns.lastIndex,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Move column right", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = {
                                    editingColumn = col
                                    colTitle = col.title
                                    colColor = col.colorHex
                                    colWip = col.wipLimit.toString()
                                    colIsDone = col.isDoneColumn
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit column", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { onDeleteColumn(col.id) },
                                enabled = columns.size > 1,
                                modifier = Modifier.size(36.dp).testTag("delete_col_${col.id}")
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete column",
                                    tint = if (columns.size > 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (editingColumn == null) "Add New Column" else "Update '${editingColumn?.title}'",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = colTitle,
                    onValueChange = { colTitle = it },
                    label = { Text("Column Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("kanban_col_name_input")
                )

                OutlinedTextField(
                    value = colWip,
                    onValueChange = { colWip = it.filter { ch -> ch.isDigit() } },
                    label = { Text("WIP Limit (0 = unlimited)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    KanbanColumnPalette.forEach { hex ->
                        val c = parseHexColorSafe(hex)
                        val selected = colColor.equals(hex, ignoreCase = true)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (selected) 2.5.dp else 0.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colColor = hex }
                        ) {
                            if (selected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Marks tasks completed", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = colIsDone, onCheckedChange = { colIsDone = it })
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (colTitle.isNotBlank()) {
                                val target = editingColumn
                                onSaveColumn(
                                    (target ?: KanbanColumnEntity(title = colTitle.trim())).copy(
                                        title = colTitle.trim(),
                                        colorHex = colColor,
                                        wipLimit = colWip.toIntOrNull()?.coerceIn(0, 99) ?: 0,
                                        isDoneColumn = colIsDone
                                    )
                                )
                                editingColumn = null
                                colTitle = ""
                                colWip = "0"
                                colIsDone = false
                            }
                        },
                        modifier = Modifier.testTag("kanban_save_col_btn")
                    ) {
                        Text(if (editingColumn == null) "Add Column" else "Save Column")
                    }
                    if (editingColumn != null) {
                        TextButton(
                            onClick = {
                                editingColumn = null
                                colTitle = ""
                                colWip = "0"
                                colIsDone = false
                            }
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

private tailrec fun Context.findHostActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}
