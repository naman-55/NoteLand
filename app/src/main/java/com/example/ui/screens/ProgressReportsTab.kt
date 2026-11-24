package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.editorialCardBorderColor
import com.example.ui.theme.editorialCardContainerColor
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel
import java.util.TimeZone

@Composable
fun ProgressReportsTab(viewModel: NoteViewModel) {
    val dailyReport by viewModel.dailyProgressReport.collectAsStateWithLifecycle()
    val weeklyReport by viewModel.weeklyProgressReport.collectAsStateWithLifecycle()
    val isDark = isAppInDarkTheme

    var mode by remember { mutableIntStateOf(0) } // 0 = Daily, 1 = Weekly
    val tzId = remember { TimeZone.getDefault().id }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("progress_reports_tab"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val prodColors = com.example.ui.theme.currentSectionColors
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = { mode = 0 },
                        shape = CircleShape,
                        color = if (mode == 0) prodColors.accent else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("report_mode_daily")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Daily Report",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (mode == 0) prodColors.onAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        onClick = { mode = 1 },
                        shape = CircleShape,
                        color = if (mode == 1) prodColors.accent else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("report_mode_weekly")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Weekly Report",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (mode == 1) prodColors.onAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = tzId.substringAfterLast('/'),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (mode == 0) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = editorialCardContainerColor(isDark)),
                    border = BorderStroke(0.9.dp, editorialCardBorderColor(isDark)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Daily Productivity Score",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = dailyReport.localDateKey,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${dailyReport.productivityScorePercent}%",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { dailyReport.productivityScorePercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReportMetricCard(
                        icon = Icons.Default.CheckCircle,
                        accent = Color(0xFF6366F1),
                        label = "Tasks Done Today",
                        value = "${dailyReport.tasksCompletedToday}",
                        subtitle = "${dailyReport.tasksDueOrPendingToday} due/pending",
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                    ReportMetricCard(
                        icon = Icons.Default.LocalFireDepartment,
                        accent = Color(0xFF10B981),
                        label = "Habits Completed",
                        value = "${dailyReport.habitsCompletedToday}/${dailyReport.habitsTotal}",
                        subtitle = "Active habits",
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReportMetricCard(
                        icon = Icons.Default.Timer,
                        accent = Color(0xFF8B5CF6),
                        label = "Focus Time",
                        value = "${dailyReport.focusMinutesToday}m",
                        subtitle = "${dailyReport.focusSessionsToday} sessions",
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                    ReportMetricCard(
                        icon = Icons.Default.Schedule,
                        accent = Color(0xFF3B82F6),
                        label = "Time Blocks",
                        value = "${dailyReport.scheduledBlocksCompleted}/${dailyReport.scheduledBlocksTotal}",
                        subtitle = "Subtasks ${dailyReport.subtasksCompleted}/${dailyReport.subtasksTotal}",
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (dailyReport.overdueTasksCount > 0) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(
                                text = "${dailyReport.overdueTasksCount} overdue task(s) need attention or rescheduling.",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        } else {
            // Weekly Report View
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = editorialCardContainerColor(isDark)),
                    border = BorderStroke(0.9.dp, editorialCardBorderColor(isDark)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "7-Day Activity Trend",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "${weeklyReport.startDateKey} → ${weeklyReport.endDateKey}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${weeklyReport.weeklyCompletionRatePercent}% rate",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Custom Canvas 7-day bar chart
                        val bars = weeklyReport.dailyBars
                        val maxScore = (bars.maxOfOrNull { it.tasksCompleted * 2 + it.habitsCompleted * 2 + (it.focusMinutes / 15) } ?: 1).coerceAtLeast(1)
                        val taskBarColor = Color(0xFF6366F1)
                        val habitBarColor = Color(0xFF10B981)
                        val focusBarColor = Color(0xFFF59E0B)

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .testTag("weekly_chart_canvas")
                        ) {
                            val colWidth = size.width / bars.size.coerceAtLeast(1)
                            val barW = (colWidth * 0.2f).coerceIn(6.dp.toPx(), 14.dp.toPx())
                            bars.forEachIndexed { idx, bar ->
                                val baseX = idx * colWidth + (colWidth - barW * 3 - 4.dp.toPx()) / 2f
                                val tH = ((bar.tasksCompleted * 2f) / maxScore.toFloat()).coerceIn(0.05f, 1f) * size.height
                                val hH = ((bar.habitsCompleted * 2f) / maxScore.toFloat()).coerceIn(0.05f, 1f) * size.height
                                val fH = (((bar.focusMinutes / 15f)) / maxScore.toFloat()).coerceIn(0.05f, 1f) * size.height

                                drawRoundRect(
                                    color = taskBarColor,
                                    topLeft = Offset(baseX, size.height - tH),
                                    size = Size(barW, tH),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                                drawRoundRect(
                                    color = habitBarColor,
                                    topLeft = Offset(baseX + barW + 2.dp.toPx(), size.height - hH),
                                    size = Size(barW, hH),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                                drawRoundRect(
                                    color = focusBarColor,
                                    topLeft = Offset(baseX + (barW + 2.dp.toPx()) * 2f, size.height - fH),
                                    size = Size(barW, fH),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            bars.forEach { bar ->
                                Text(
                                    text = bar.dayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            LegendDot("Tasks", taskBarColor)
                            LegendDot("Habits", habitBarColor)
                            LegendDot("Focus", focusBarColor)
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReportMetricCard(
                        icon = Icons.Default.CheckCircle,
                        accent = Color(0xFF6366F1),
                        label = "Weekly Tasks Done",
                        value = "${weeklyReport.totalTasksCompleted}",
                        subtitle = "${weeklyReport.totalPendingTasks} pending",
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                    ReportMetricCard(
                        icon = Icons.Default.Timer,
                        accent = Color(0xFFF59E0B),
                        label = "Weekly Focus",
                        value = "${weeklyReport.totalFocusMinutes}m",
                        subtitle = "${weeklyReport.totalFocusSessions} sessions",
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReportMetricCard(
                        icon = Icons.Default.LocalFireDepartment,
                        accent = Color(0xFF10B981),
                        label = "Habit Check-Ins",
                        value = "${weeklyReport.totalHabitCheckIns}",
                        subtitle = "Best streak ${weeklyReport.bestActiveHabitStreak}d",
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                    ReportMetricCard(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        accent = Color(0xFF0D9488),
                        label = "Peak Day",
                        value = weeklyReport.mostProductiveDayLabel,
                        subtitle = "Highest combined output",
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (weeklyReport.categoryTaskCounts.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = editorialCardContainerColor(isDark),
                        border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Completed by Category",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            val maxCat = (weeklyReport.categoryTaskCounts.values.maxOrNull() ?: 1).coerceAtLeast(1)
                            weeklyReport.categoryTaskCounts.forEach { (cat, count) ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(cat, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                        Text("$count", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                    LinearProgressIndicator(
                                        progress = { count.toFloat() / maxCat.toFloat() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        strokeCap = StrokeCap.Round
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
private fun ReportMetricCard(
    icon: ImageVector,
    accent: Color,
    label: String,
    value: String,
    subtitle: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = editorialCardContainerColor(isDark),
        border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = accent)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
