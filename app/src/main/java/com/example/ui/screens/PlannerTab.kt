package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TaskPriority
import com.example.ui.components.EmptyStateView
import com.example.ui.components.parseHexColorSafe
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.editorialCardBorderColor
import com.example.ui.theme.editorialCardContainerColor
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.ProductivityEngine
import com.example.util.SmartNoteUtils
import java.util.Calendar

@Composable
fun PlannerTab(
    viewModel: NoteViewModel,
    onNavigateToCalendar: () -> Unit,
    onRequestCreateTask: () -> Unit
) {
    val tasks by viewModel.activeTasks.collectAsStateWithLifecycle()
    val isDark = isAppInDarkTheme
    var selectedHourSlot by remember { mutableIntStateOf(9) }
    var plannerViewMode by remember { mutableStateOf("Daily") }

    val now = System.currentTimeMillis()
    val todayStart = SmartNoteUtils.startOfDayUtc(now)
    val todayEnd = todayStart + 24 * 3600_000L
    
    val endOfWeek = todayStart + 7 * 24 * 3600_000L
    val endOfMonth = todayStart + 30 * 24 * 3600_000L

    val scheduledToday = remember(tasks, todayStart) {
        tasks.filter { task ->
            val ts = task.scheduledStartUtc ?: task.dueAtUtc
            ts != null && ts in todayStart until todayEnd
        }.sortedBy { it.scheduledStartUtc ?: it.dueAtUtc ?: Long.MAX_VALUE }
    }

    val overdueTasks = remember(tasks, todayStart) {
        tasks.filter { task ->
            !task.isCompleted && (task.dueAtUtc ?: Long.MAX_VALUE) < todayStart
        }.sortedBy { it.dueAtUtc ?: Long.MAX_VALUE }
    }

    val backlogToBlock = remember(tasks) {
        tasks.filter { !it.isCompleted && it.scheduledStartUtc == null }
            .sortedByDescending { ProductivityEngine.prioritySortWeight(it.priority) }
    }
    
    val scheduledWeekly = remember(tasks, todayStart) {
        tasks.filter { task ->
            val ts = task.scheduledStartUtc ?: task.dueAtUtc
            ts != null && ts in todayStart until endOfWeek
        }.sortedBy { it.scheduledStartUtc ?: it.dueAtUtc ?: Long.MAX_VALUE }
    }
    
    val scheduledMonthly = remember(tasks, todayStart) {
        tasks.filter { task ->
            val ts = task.scheduledStartUtc ?: task.dueAtUtc
            ts != null && ts in todayStart until endOfMonth
        }.sortedBy { it.scheduledStartUtc ?: it.dueAtUtc ?: Long.MAX_VALUE }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("planner_tab_root"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // View Mode Selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Daily", "Weekly", "Monthly").forEach { mode ->
                    val isSelected = plannerViewMode == mode
                    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = containerColor,
                        border = BorderStroke(0.8.dp, if (isSelected) MaterialTheme.colorScheme.primary else editorialCardBorderColor(isDark)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { plannerViewMode = mode }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                            Text(text = mode, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = contentColor)
                        }
                    }
                }
            }
        }

        if (plannerViewMode == "Daily") {
            // Daily Overview Header
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = editorialCardContainerColor(isDark)),
                border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Daily Planner",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = DisplayFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "${scheduledToday.size} scheduled today · ${backlogToBlock.size} unscheduled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onNavigateToCalendar,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Calendar", fontSize = 12.5.sp)
                    }
                }
            }
        }

        // Overdue triage section
        if (overdueTasks.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Overdue (${overdueTasks.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFDC2626)
                    )
                }
            }
            items(overdueTasks, key = { "overdue_${it.id}" }) { task ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFEF4444).copy(alpha = if (isDark) 0.14f else 0.07f),
                    border = BorderStroke(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.30f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.toggleTaskCompleted(task.id) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.RadioButtonUnchecked, contentDescription = "Complete task")
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Due ${task.dueAtUtc?.let { SmartNoteUtils.formatShortDate(it) } ?: ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFDC2626)
                            )
                        }
                        TextButton(
                            onClick = {
                                val startCal = Calendar.getInstance().apply {
                                    timeInMillis = todayStart
                                    set(Calendar.HOUR_OF_DAY, selectedHourSlot)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val start = startCal.timeInMillis
                                viewModel.assignTaskTimeBlock(task.id, start, start + task.estimatedMinutes * 60_000L)
                            }
                        ) {
                            Text("Move Today")
                        }
                    }
                }
            }
        }

        // Today's Time-Blocked Agenda
        item {
            Text(
                text = "Today's Agenda (${scheduledToday.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (scheduledToday.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = editorialCardContainerColor(isDark),
                    border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Nothing time-blocked for today",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Pick a slot below and schedule any unscheduled task with one tap.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onRequestCreateTask, shape = RoundedCornerShape(12.dp)) {
                            Text("New Task")
                        }
                    }
                }
            }
        } else {
            items(scheduledToday, key = { "today_${it.id}" }) { task ->
                val p = TaskPriority.fromName(task.priority)
                val accent = parseHexColorSafe(p.colorHex)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = editorialCardContainerColor(isDark),
                    border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.toggleTaskCompleted(task.id) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Toggle task",
                                tint = if (task.isCompleted) Color(0xFF10B981) else accent
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val slotTime = task.scheduledStartUtc?.let { SmartNoteUtils.formatTimestamp(it) }
                                ?: task.dueAtUtc?.let { SmartNoteUtils.formatShortDate(it) }
                                ?: ""
                            Text(
                                text = "$slotTime · ${p.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Quick Time-Blocking Queue
        if (backlogToBlock.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Quick Time-Block Queue (${backlogToBlock.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        listOf(9 to "9 AM", 11 to "11 AM", 14 to "2 PM", 16 to "4 PM", 19 to "7 PM").forEach { (h, label) ->
                            val selected = selectedHourSlot == h
                            Surface(
                                shape = CircleShape,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { selectedHourSlot = h }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            items(backlogToBlock.take(10), key = { "queue_${it.id}" }) { task ->
                val accent = parseHexColorSafe(TaskPriority.fromName(task.priority).colorHex)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = editorialCardContainerColor(isDark),
                    border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(10.dp).clip(CircleShape).let { m ->
                                m
                            }
                        ) {
                            Surface(shape = CircleShape, color = accent, modifier = Modifier.fillMaxSize()) {}
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${task.category} · ${task.estimatedMinutes}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply {
                                    timeInMillis = todayStart
                                    set(Calendar.HOUR_OF_DAY, selectedHourSlot)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val start = cal.timeInMillis
                                viewModel.assignTaskTimeBlock(task.id, start, start + task.estimatedMinutes * 60_000L)
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Block", fontSize = 12.sp)
                        }
                    }
                }
            }
        } else if (plannerViewMode == "Daily" && scheduledToday.isNotEmpty() && overdueTasks.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.CheckCircle,
                    title = "All Active Tasks Planned",
                    description = "Every pending task is already scheduled on your timeline."
                )
            }
        }
        
        if (plannerViewMode == "Weekly") {
            item {
                Text("Coming Up This Week (${scheduledWeekly.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (scheduledWeekly.isEmpty()) {
                item { EmptyStateView(Icons.Default.Schedule, "No Tasks", "Nothing scheduled for the week.") }
            } else {
                items(scheduledWeekly, key = { "w_${it.id}" }) { task ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = editorialCardContainerColor(isDark),
                        border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(task.title, fontWeight = FontWeight.Bold)
                                Text(
                                    text = (task.scheduledStartUtc ?: task.dueAtUtc)?.let { SmartNoteUtils.formatShortDate(it) } ?: "No Date",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (plannerViewMode == "Monthly") {
            item {
                Text("Coming Up This Month (${scheduledMonthly.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (scheduledMonthly.isEmpty()) {
                item { EmptyStateView(Icons.Default.Schedule, "No Tasks", "Nothing scheduled for the month.") }
            } else {
                items(scheduledMonthly, key = { "m_${it.id}" }) { task ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = editorialCardContainerColor(isDark),
                        border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(task.title, fontWeight = FontWeight.Bold)
                                Text(
                                    text = (task.scheduledStartUtc ?: task.dueAtUtc)?.let { SmartNoteUtils.formatShortDate(it) } ?: "No Date",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
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
