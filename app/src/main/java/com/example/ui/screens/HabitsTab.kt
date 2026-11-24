package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.HabitEntity
import com.example.data.model.HabitFrequency
import com.example.ui.components.EmptyStateView
import com.example.ui.components.parseHexColorSafe
import com.example.ui.theme.editorialCardBorderColor
import com.example.ui.theme.editorialCardContainerColor
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.HabitStreakSummary
import java.util.TimeZone

private val HabitColors = listOf("#10B981", "#6366F1", "#F59E0B", "#EC4899", "#3B82F6", "#0D9488")

@Composable
fun HabitsTab(
    viewModel: NoteViewModel,
    onOpenHabitDialogRequested: Boolean = false,
    onClearExternalHabitRequest: () -> Unit = {}
) {
    val habits by viewModel.activeHabits.collectAsStateWithLifecycle()
    val streaks by viewModel.habitStreaks.collectAsStateWithLifecycle()
    val isDark = isAppInDarkTheme

    var editingHabit by remember { mutableStateOf<HabitEntity?>(null) }
    var showHabitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(onOpenHabitDialogRequested) {
        if (onOpenHabitDialogRequested && !showHabitDialog) {
            editingHabit = null
            showHabitDialog = true
            onClearExternalHabitRequest()
        }
    }

    val doneTodayCount = habits.count { streaks[it.id]?.isDoneToday == true }
    val bestStreak = streaks.values.maxOfOrNull { it.currentStreak } ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("habits_tab_root")
    ) {
        // Summary Strip + New Habit CTA
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = editorialCardContainerColor(isDark),
                border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Today: $doneTodayCount/${habits.size}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Top active streak: ${bestStreak}d",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            val sectionAccent = com.example.ui.theme.currentSectionColors
            Button(
                onClick = {
                    editingHabit = null
                    showHabitDialog = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = sectionAccent.accent,
                    contentColor = sectionAccent.onAccent
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("habit_add_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("New Habit")
            }
        }

        if (habits.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.TrackChanges,
                title = "No Habits Tracked Yet",
                description = "Build consistency with daily or weekday habits, multi-check-in targets, and automatic streak tracking.",
                actionText = "Create Habit",
                onAction = {
                    editingHabit = null
                    showHabitDialog = true
                },
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(habits, key = { it.id }) { habit ->
                    val summary = streaks[habit.id] ?: HabitStreakSummary(
                        habitId = habit.id,
                        currentStreak = 0,
                        longestStreak = 0,
                        completedTodayCount = 0,
                        targetPerDay = habit.targetPerDay,
                        isDoneToday = false,
                        completionRate30d = 0,
                        last7DaysStatus = List(7) { false }
                    )
                    HabitTrackerCard(
                        habit = habit,
                        summary = summary,
                        isDark = isDark,
                        onCheckIn = { viewModel.checkInHabit(habit.id) },
                        onEdit = {
                            editingHabit = habit
                            showHabitDialog = true
                        },
                        onDelete = { viewModel.deleteHabit(habit.id) }
                    )
                }
            }
        }
    }

    if (showHabitDialog) {
        HabitEditorDialog(
            initialHabit = editingHabit,
            onDismiss = {
                showHabitDialog = false
                editingHabit = null
            },
            onSave = { habit ->
                viewModel.saveHabit(habit)
                showHabitDialog = false
                editingHabit = null
            }
        )
    }
}

@Composable
private fun HabitTrackerCard(
    habit: HabitEntity,
    summary: HabitStreakSummary,
    isDark: Boolean,
    onCheckIn: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = parseHexColorSafe(habit.colorHex)
    val freqLabel = HabitFrequency.fromName(habit.frequencyType).label

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = editorialCardContainerColor(isDark)),
        border = BorderStroke(
            width = if (summary.isDoneToday) 1.4.dp else 0.8.dp,
            color = if (summary.isDoneToday) accent else editorialCardBorderColor(isDark)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(accent)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$freqLabel · ${summary.completedTodayCount}/${summary.targetPerDay} today · 30d: ${summary.completionRate30d}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit ${habit.title}", modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp).testTag("habit_delete_${habit.id}")
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete ${habit.title}",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (habit.description.isNotBlank()) {
                Text(
                    text = habit.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Streak Badges & 7-Day Dot Strip
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(shape = CircleShape, color = Color(0xFFF59E0B).copy(alpha = 0.16f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${summary.currentStreak}d streak",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                            }
                        }
                        Text(
                            text = "Best ${summary.longestStreak}d",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        summary.last7DaysStatus.forEach { completed ->
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (completed) accent else accent.copy(alpha = 0.16f))
                            )
                        }
                    }
                }

                Button(
                    onClick = onCheckIn,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (summary.isDoneToday) accent else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (summary.isDoneToday) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("habit_checkin_${habit.id}")
                ) {
                    Icon(
                        imageVector = if (summary.isDoneToday) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = if (summary.isDoneToday) "Completed today" else "Check in ${habit.title}",
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = when {
                            summary.isDoneToday -> "Done"
                            habit.targetPerDay > 1 -> "+1 (${summary.completedTodayCount}/${habit.targetPerDay})"
                            else -> "Check In"
                        },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitEditorDialog(
    initialHabit: HabitEntity?,
    onDismiss: () -> Unit,
    onSave: (HabitEntity) -> Unit
) {
    var title by remember { mutableStateOf(initialHabit?.title ?: "") }
    var description by remember { mutableStateOf(initialHabit?.description ?: "") }
    var frequency by remember { mutableStateOf(HabitFrequency.fromName(initialHabit?.frequencyType)) }
    var targetPerDay by remember { mutableIntStateOf(initialHabit?.targetPerDay ?: 1) }
    var colorHex by remember { mutableStateOf(initialHabit?.colorHex ?: HabitColors.first()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialHabit == null) "New Habit" else "Edit Habit",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMsg = null
                    },
                    label = { Text("Habit Name (e.g., Read 20 pages)") },
                    singleLine = true,
                    isError = errorMsg != null,
                    supportingText = errorMsg?.let { err -> { Text(err) } },
                    modifier = Modifier.fillMaxWidth().testTag("habit_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Motivation / Cue (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Cadence", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    HabitFrequency.entries.forEach { freq ->
                        val active = frequency == freq
                        Surface(
                            shape = CircleShape,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { frequency = freq }
                        ) {
                            Text(
                                text = freq.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Text("Daily Target: $targetPerDay time(s)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3, 5, 8).forEach { count ->
                        val active = targetPerDay == count
                        Surface(
                            shape = CircleShape,
                            color = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { targetPerDay = count }
                        ) {
                            Text(
                                text = "$count/d",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HabitColors.forEach { hex ->
                        val c = parseHexColorSafe(hex)
                        val selected = colorHex.equals(hex, ignoreCase = true)
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
                                .clickable { colorHex = hex }
                        ) {
                            if (selected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMsg = "Please enter a habit name"
                        return@Button
                    }
                    val updated = (initialHabit ?: HabitEntity(title = title.trim())).copy(
                        title = title.trim(),
                        description = description.trim(),
                        frequencyType = frequency.name,
                        targetPerDay = targetPerDay.coerceAtLeast(1),
                        colorHex = colorHex,
                        timeZoneId = TimeZone.getDefault().id
                    )
                    onSave(updated)
                },
                modifier = Modifier.testTag("habit_save_button")
            ) {
                Text("Save Habit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
