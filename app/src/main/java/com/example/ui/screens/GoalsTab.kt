package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.editorialCardBorderColor
import com.example.ui.theme.editorialCardContainerColor
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel

@Composable
fun GoalsTab(viewModel: NoteViewModel) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val dailyReport by viewModel.dailyProgressReport.collectAsStateWithLifecycle()
    val isDark = isAppInDarkTheme

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalTitle by remember { mutableStateOf("") }
    var goalTargetText by remember { mutableStateOf("5") }
    var goalUnit by remember { mutableStateOf("") }
    var goalDeadline by remember { mutableStateOf<Long?>(null) }
    var goalMilestonesText by remember { mutableStateOf("") }

    val pendingGoals = remember(goals) { goals.filter { !it.isCompleted } }
    val completedGoals = remember(goals) { goals.filter { it.isCompleted } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("goals_tab_root"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Automatic Daily Productivity Targets Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = editorialCardContainerColor(isDark)),
                border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Targets",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Live progress synced from Tasks, Habits & Focus",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val prodColors = com.example.ui.theme.currentSectionColors
                        Button(
                            onClick = {
                                goalTitle = ""
                                goalTargetText = "5"
                                goalUnit = ""
                                goalDeadline = null
                                goalMilestonesText = ""
                                showAddGoalDialog = true
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = prodColors.accent,
                                contentColor = prodColors.onAccent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.heightIn(min = 48.dp).testTag("goal_add_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New Goal", fontSize = 12.5.sp)
                        }
                    }

                    GoalMetricRow(
                        label = "Focus Time",
                        current = dailyReport.focusMinutesToday,
                        target = 60,
                        unit = "min",
                        accent = Color(0xFF7C3AED)
                    )
                    GoalMetricRow(
                        label = "Tasks Completed",
                        current = dailyReport.tasksCompletedToday,
                        target = (dailyReport.tasksCompletedToday + dailyReport.tasksDueOrPendingToday).coerceAtLeast(1),
                        unit = "tasks",
                        accent = Color(0xFF2563EB)
                    )
                    GoalMetricRow(
                        label = "Habits Checked",
                        current = dailyReport.habitsCompletedToday,
                        target = dailyReport.habitsTotal.coerceAtLeast(1),
                        unit = "habits",
                        accent = Color(0xFF10B981)
                    )
                }
            }
        }

        // Custom Milestones & Goals
        item {
            Text(
                text = "Personal Milestones (${goals.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (goals.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.Flag,
                    title = "Set a Milestone Goal",
                    description = "Track reading pages, deep work blocks, or project milestones alongside your daily habits.",
                    actionText = "Create Goal",
                    onAction = { showAddGoalDialog = true }
                )
            }
        } else {
            if (pendingGoals.isNotEmpty()) {
                item {
                    Text("Pending Goals", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                items(pendingGoals, key = { it.id }) { goal ->
                    GoalCard(goal, viewModel, isDark)
                }
            }
            if (completedGoals.isNotEmpty()) {
                item {
                    Text("Completed Goals", style = MaterialTheme.typography.labelMedium, color = Color(0xFF10B981))
                }
                items(completedGoals, key = { it.id }) { goal ->
                    GoalCard(goal, viewModel, isDark)
                }
            }
        }
    }

    if (showAddGoalDialog) {
        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = { Text("Create Milestone Goal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = goalTitle,
                        onValueChange = { goalTitle = it },
                        label = { Text("Goal Title") },
                        placeholder = { Text("e.g., Read 12 Books") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("goal_title_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = goalTargetText,
                            onValueChange = { goalTargetText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Target") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("goal_target_input")
                        )
                        OutlinedTextField(
                            value = goalUnit,
                            onValueChange = { goalUnit = it },
                            label = { Text("Unit (optional)") },
                            placeholder = { Text("chapters") },
                            singleLine = true,
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                    OutlinedTextField(
                        value = goalMilestonesText,
                        onValueChange = { goalMilestonesText = it },
                        label = { Text("Milestones (comma separated)") },
                        placeholder = { Text("e.g. Find book, Read intro") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = goalTargetText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        val milestones = goalMilestonesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        if (goalTitle.isNotBlank()) {
                            viewModel.addGoal(
                                title = goalTitle,
                                targetValue = target,
                                unit = goalUnit,
                                deadlineUtc = goalDeadline,
                                milestones = milestones
                            )
                            showAddGoalDialog = false
                        }
                    },
                    modifier = Modifier.testTag("goal_save_button")
                ) {
                    Text("Save Goal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GoalMetricRow(
    label: String,
    current: Int,
    target: Int,
    unit: String,
    accent: Color
) {
    val pct = (current.toFloat() / target.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "$current / $target $unit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = accent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun GoalCard(
    goal: com.example.ui.viewmodel.GoalItem,
    viewModel: NoteViewModel,
    isDark: Boolean
) {
    val progress = (goal.currentValue.toFloat() / goal.targetValue.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = editorialCardContainerColor(isDark),
        border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
        modifier = Modifier.fillMaxWidth().testTag("goal_card_${goal.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = { viewModel.toggleGoalCompleted(goal.id) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = if (goal.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Toggle goal complete",
                        tint = if (goal.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (goal.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val unitSuffix = if (goal.unit.isNotBlank()) " ${goal.unit}" else ""
                    Text(
                        text = "${goal.currentValue} / ${goal.targetValue}$unitSuffix",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { viewModel.incrementGoal(goal.id, 1) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 40.dp).testTag("goal_increment_${goal.id}")
                ) {
                    Text("+1", fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = { viewModel.deleteGoal(goal.id) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete goal",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (goal.milestones.isNotEmpty()) {
                Text(
                    text = "Milestones: ${goal.milestones.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (goal.deadlineUtc != null) {
                Text(
                    text = "Deadline: ${com.example.util.SmartNoteUtils.formatShortDate(goal.deadlineUtc)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                color = if (goal.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
}
