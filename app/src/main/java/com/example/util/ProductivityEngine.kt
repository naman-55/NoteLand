package com.example.util

import com.example.data.model.FocusSessionEntity
import com.example.data.model.FocusSessionStatus
import com.example.data.model.FocusSessionType
import com.example.data.model.HabitEntity
import com.example.data.model.HabitFrequency
import com.example.data.model.HabitLogEntity
import com.example.data.model.KanbanColumnEntity
import com.example.data.model.NoteEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.TaskDependencyEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskRecurrenceRule
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.json.JSONArray
import org.json.JSONObject

data class HabitStreakSummary(
    val habitId: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val completedTodayCount: Int,
    val targetPerDay: Int,
    val isDoneToday: Boolean,
    val completionRate30d: Int,
    val last7DaysStatus: List<Boolean>
)

data class PomodoroRuntimeState(
    val activeSessionId: String? = null,
    val sessionType: FocusSessionType = FocusSessionType.WORK,
    val status: FocusSessionStatus = FocusSessionStatus.CANCELLED, // CANCELLED used as IDLE when no active session
    val isActiveOrPaused: Boolean = false,
    val plannedDurationSec: Int = FocusSessionType.WORK.defaultDurationSec,
    val accumulatedElapsedMs: Long = 0L,
    val lastResumedWallClockMs: Long = 0L,
    val linkedTaskId: String? = null,
    val linkedNoteId: String? = null,
    val completedWorkCycles: Int = 0
)

data class FocusStatisticsSummary(
    val todayFocusMinutes: Int,
    val todayCompletedSessions: Int,
    val weekFocusMinutes: Int,
    val weekCompletedSessions: Int,
    val totalFocusMinutes: Int,
    val totalCompletedSessions: Int,
    val completionRatePercent: Int,
    val averageSessionMinutes: Int
)

data class DailyReportBar(
    val localDateKey: String,
    val dayLabel: String,
    val tasksCompleted: Int,
    val focusMinutes: Int,
    val habitsCompleted: Int
)

data class DailyProgressReport(
    val localDateKey: String,
    val tasksCompletedToday: Int,
    val tasksDueOrPendingToday: Int,
    val overdueTasksCount: Int,
    val subtasksCompleted: Int,
    val subtasksTotal: Int,
    val scheduledBlocksCompleted: Int,
    val scheduledBlocksTotal: Int,
    val habitsCompletedToday: Int,
    val habitsTotal: Int,
    val focusMinutesToday: Int,
    val focusSessionsToday: Int,
    val productivityScorePercent: Int
)

data class WeeklyProgressReport(
    val startDateKey: String,
    val endDateKey: String,
    val dailyBars: List<DailyReportBar>,
    val totalTasksCompleted: Int,
    val totalPendingTasks: Int,
    val totalFocusMinutes: Int,
    val totalFocusSessions: Int,
    val totalHabitCheckIns: Int,
    val bestActiveHabitStreak: Int,
    val mostProductiveDayLabel: String,
    val categoryTaskCounts: Map<String, Int>,
    val weeklyCompletionRatePercent: Int
)

enum class DashboardWidgetType(
    val id: String,
    val title: String,
    val subtitle: String
) {
    KANBAN_SUMMARY("kanban_summary", "Kanban Board Overview", "Column breakdown & urgent tasks"),
    POMODORO_TIMER("pomodoro_timer", "Pomodoro Focus", "Quick timer launch & today's focus time"),
    HABIT_STREAKS("habit_streaks", "Habit Streaks", "One-tap daily check-ins & active streaks"),
    TIME_BLOCKS("time_blocks", "Today's Time Blocks", "Scheduled blocks & overdue alerts"),
    PROGRESS_REPORT("progress_report", "Productivity Score", "Daily tasks, habits & focus snapshot")
}

data class DashboardWidgetItem(
    val type: DashboardWidgetType,
    val enabled: Boolean = true,
    val orderIndex: Int = 0
)

object ProductivityEngine {

    fun defaultKanbanColumns(now: Long = System.currentTimeMillis()): List<KanbanColumnEntity> = listOf(
        KanbanColumnEntity(
            id = "col_todo",
            boardId = "default",
            title = "To Do",
            colorHex = "#10B981", // Green to match screenshot
            position = 0,
            wipLimit = 0,
            isDoneColumn = false,
            createdAtUtc = now,
            updatedAtUtc = now
        ),
        KanbanColumnEntity(
            id = "col_in_progress",
            boardId = "default",
            title = "In Progress",
            colorHex = "#F59E0B",
            position = 1,
            wipLimit = 3,
            isDoneColumn = false,
            createdAtUtc = now,
            updatedAtUtc = now
        ),
        KanbanColumnEntity(
            id = "col_review",
            boardId = "default",
            title = "Review",
            colorHex = "#8B5CF6",
            position = 2,
            wipLimit = 0,
            isDoneColumn = false,
            createdAtUtc = now,
            updatedAtUtc = now
        ),
        KanbanColumnEntity(
            id = "col_done",
            boardId = "default",
            title = "Done",
            colorHex = "#10B981",
            position = 3,
            wipLimit = 0,
            isDoneColumn = true,
            createdAtUtc = now,
            updatedAtUtc = now
        )
    )

    fun resolveTimeZone(timeZoneId: String?): TimeZone {
        val clean = timeZoneId?.trim().orEmpty()
        if (clean.isBlank()) return TimeZone.getDefault()
        val tz = TimeZone.getTimeZone(clean)
        return if (tz.id == "GMT" && !clean.equals("GMT", ignoreCase = true) && !clean.equals("UTC", ignoreCase = true)) {
            TimeZone.getDefault()
        } else {
            tz
        }
    }

    fun localDateKeyFromMillis(
        millis: Long,
        timeZoneId: String? = null
    ): String {
        val tz = resolveTimeZone(timeZoneId)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = tz
        }
        return sdf.format(Date(millis))
    }

    fun startOfDayForDateKey(
        localDateKey: String,
        timeZoneId: String? = null
    ): Long {
        val tz = resolveTimeZone(timeZoneId)
        val parts = localDateKey.split("-")
        val cal = Calendar.getInstance(tz).apply {
            if (parts.size == 3) {
                val y = parts[0].toIntOrNull()
                val m = parts[1].toIntOrNull()
                val d = parts[2].toIntOrNull()
                if (y != null && m != null && d != null) {
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, (m - 1).coerceIn(0, 11))
                    set(Calendar.DAY_OF_MONTH, d.coerceIn(1, 31))
                }
            }
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun addDaysToDateKey(
        localDateKey: String,
        deltaDays: Int,
        timeZoneId: String? = null
    ): String {
        val tz = resolveTimeZone(timeZoneId)
        val parts = localDateKey.split("-")
        val cal = Calendar.getInstance(tz).apply {
            if (parts.size == 3) {
                set(Calendar.YEAR, parts[0].toIntOrNull() ?: get(Calendar.YEAR))
                set(Calendar.MONTH, ((parts[1].toIntOrNull() ?: 1) - 1).coerceIn(0, 11))
                set(Calendar.DAY_OF_MONTH, (parts[2].toIntOrNull() ?: 1).coerceIn(1, 31))
            }
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, deltaDays)
        }
        return localDateKeyFromMillis(cal.timeInMillis, tz.id)
    }

    fun dayLabelForDateKey(
        localDateKey: String,
        timeZoneId: String? = null
    ): String {
        val tz = resolveTimeZone(timeZoneId)
        val start = startOfDayForDateKey(localDateKey, tz.id) + 12 * 3600_000L
        return SimpleDateFormat("EEE", Locale.getDefault()).apply { timeZone = tz }.format(Date(start))
    }

    fun dayOfWeekForDateKey(
        localDateKey: String,
        timeZoneId: String? = null
    ): Int {
        val tz = resolveTimeZone(timeZoneId)
        val cal = Calendar.getInstance(tz).apply {
            timeInMillis = startOfDayForDateKey(localDateKey, tz.id) + 12 * 3600_000L
        }
        return cal.get(Calendar.DAY_OF_WEEK)
    }

    /**
     * Calculates the next occurrence timestamp preserving local wall-clock hour/minute across DST
     * transitions and clamping month-end dates cleanly (e.g., Jan 31 -> Feb 28/29).
     */
    fun calculateNextOccurrenceUtc(
        currentUtc: Long,
        rule: TaskRecurrenceRule,
        interval: Int = 1,
        daysCsv: String = "",
        timeZoneId: String? = null
    ): Long? {
        if (rule == TaskRecurrenceRule.NONE) return null
        val tz = resolveTimeZone(timeZoneId)
        val safeInterval = interval.coerceAtLeast(1)
        val cal = Calendar.getInstance(tz).apply { timeInMillis = currentUtc }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val second = cal.get(Calendar.SECOND)

        when (rule) {
            TaskRecurrenceRule.DAILY -> {
                cal.add(Calendar.DAY_OF_MONTH, safeInterval)
            }
            TaskRecurrenceRule.WEEKDAYS -> {
                do {
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                    val dow = cal.get(Calendar.DAY_OF_WEEK)
                } while (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY)
            }
            TaskRecurrenceRule.WEEKLY -> {
                val allowedDays = daysCsv.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .filter { it in Calendar.SUNDAY..Calendar.SATURDAY }
                    .toSet()
                if (allowedDays.isEmpty()) {
                    cal.add(Calendar.WEEK_OF_YEAR, safeInterval)
                } else {
                    var stepped = false
                    for (offset in 1..14) {
                        cal.add(Calendar.DAY_OF_MONTH, 1)
                        if (cal.get(Calendar.DAY_OF_WEEK) in allowedDays) {
                            stepped = true
                            break
                        }
                    }
                    if (!stepped) {
                        cal.add(Calendar.WEEK_OF_YEAR, safeInterval)
                    }
                }
            }
            TaskRecurrenceRule.MONTHLY -> {
                val desiredDom = cal.get(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, safeInterval)
                val maxDom = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, desiredDom.coerceAtMost(maxDom))
            }
            TaskRecurrenceRule.NONE -> return null
        }

        // Preserve exact wall-clock hour/minute across DST shifts
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, second)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Detects whether adding (taskId -> dependsOnTaskId) would create a self-loop or transitive cycle.
     */
    fun wouldCreateDependencyCycle(
        taskId: String,
        dependsOnTaskId: String,
        existingDependencies: List<TaskDependencyEntity>
    ): Boolean {
        if (taskId.isBlank() || dependsOnTaskId.isBlank()) return true
        if (taskId == dependsOnTaskId) return true

        val prereqMap = existingDependencies.groupBy({ it.taskId }, { it.dependsOnTaskId })
        val visited = HashSet<String>()
        val stack = ArrayDeque<String>()
        stack.addLast(dependsOnTaskId)

        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            if (current == taskId) return true
            if (visited.add(current)) {
                prereqMap[current]?.forEach { nextPrereq ->
                    if (nextPrereq !in visited) {
                        stack.addLast(nextPrereq)
                    }
                }
            }
        }
        return false
    }

    fun getBlockingTaskIds(
        taskId: String,
        dependencies: List<TaskDependencyEntity>,
        tasksById: Map<String, TaskEntity>
    ): List<String> {
        return dependencies
            .filter { it.taskId == taskId }
            .mapNotNull { dep ->
                val prereq = tasksById[dep.dependsOnTaskId]
                if (prereq != null && !prereq.isCompleted && !prereq.isArchived) prereq.id else null
            }
    }

    fun isTaskBlocked(
        taskId: String,
        dependencies: List<TaskDependencyEntity>,
        tasksById: Map<String, TaskEntity>
    ): Boolean = getBlockingTaskIds(taskId, dependencies, tasksById).isNotEmpty()

    fun computeHabitStreak(
        habit: HabitEntity,
        logsForHabit: List<HabitLogEntity>,
        nowUtc: Long = System.currentTimeMillis()
    ): HabitStreakSummary {
        val tzId = habit.timeZoneId.ifBlank { TimeZone.getDefault().id }
        val todayKey = localDateKeyFromMillis(nowUtc, tzId)
        val logsByDate = logsForHabit.associateBy { it.localDateKey }
        val todayLog = logsByDate[todayKey]
        val completedTodayCount = todayLog?.completedCount ?: 0
        val isDoneToday = todayLog?.isCompleted == true || completedTodayCount >= habit.targetPerDay.coerceAtLeast(1)

        val freq = HabitFrequency.fromName(habit.frequencyType)
        fun isScheduledDay(dateKey: String): Boolean {
            if (freq != HabitFrequency.WEEKDAYS) return true
            val dow = dayOfWeekForDateKey(dateKey, tzId)
            return dow != Calendar.SATURDAY && dow != Calendar.SUNDAY
        }

        fun isCompletedOn(dateKey: String): Boolean {
            val log = logsByDate[dateKey] ?: return false
            return log.isCompleted || log.completedCount >= habit.targetPerDay.coerceAtLeast(1)
        }

        // Last 7 days [today-6 .. today]
        val last7 = (-6..0).map { offset ->
            val key = addDaysToDateKey(todayKey, offset, tzId)
            isCompletedOn(key)
        }

        // 30-day scheduled completion rate
        var scheduledIn30 = 0
        var doneIn30 = 0
        for (offset in -29..0) {
            val key = addDaysToDateKey(todayKey, offset, tzId)
            if (isScheduledDay(key)) {
                scheduledIn30++
                if (isCompletedOn(key)) doneIn30++
            }
        }
        val rate30 = if (scheduledIn30 > 0) ((doneIn30 * 100f) / scheduledIn30).toInt().coerceIn(0, 100) else 0

        // Current streak (start from today if completed, otherwise allow yesterday if today is still in progress)
        var currentStreak = 0
        var cursorKey = if (isCompletedOn(todayKey)) {
            todayKey
        } else {
            addDaysToDateKey(todayKey, -1, tzId)
        }
        for (step in 0..365) {
            if (!isScheduledDay(cursorKey)) {
                cursorKey = addDaysToDateKey(cursorKey, -1, tzId)
                continue
            }
            if (isCompletedOn(cursorKey)) {
                currentStreak++
                cursorKey = addDaysToDateKey(cursorKey, -1, tzId)
            } else {
                break
            }
        }

        // Longest streak across chronological logs (up to 400 days window around earliest->today)
        var longestStreak = currentStreak
        if (logsByDate.isNotEmpty()) {
            val minKey = logsByDate.keys.minOrNull() ?: todayKey
            var scanKey = minKey
            var running = 0
            var guard = 0
            while (scanKey <= todayKey && guard < 1095) {
                guard++
                if (!isScheduledDay(scanKey)) {
                    scanKey = addDaysToDateKey(scanKey, 1, tzId)
                    continue
                }
                if (isCompletedOn(scanKey)) {
                    running++
                    if (running > longestStreak) longestStreak = running
                } else {
                    running = 0
                }
                scanKey = addDaysToDateKey(scanKey, 1, tzId)
            }
        }

        return HabitStreakSummary(
            habitId = habit.id,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            completedTodayCount = completedTodayCount,
            targetPerDay = habit.targetPerDay.coerceAtLeast(1),
            isDoneToday = isDoneToday,
            completionRate30d = rate30,
            last7DaysStatus = last7
        )
    }

    fun computeRemainingSeconds(state: PomodoroRuntimeState, nowUtc: Long = System.currentTimeMillis()): Int {
        if (!state.isActiveOrPaused) return state.plannedDurationSec.coerceAtLeast(0)
        val liveSegmentMs = if (state.status == FocusSessionStatus.RUNNING && state.lastResumedWallClockMs > 0L) {
            (nowUtc - state.lastResumedWallClockMs).coerceAtLeast(0L)
        } else {
            0L
        }
        val totalElapsedSec = ((state.accumulatedElapsedMs + liveSegmentMs) / 1000L).toInt()
        return (state.plannedDurationSec - totalElapsedSec).coerceAtLeast(0)
    }

    fun computeElapsedSeconds(state: PomodoroRuntimeState, nowUtc: Long = System.currentTimeMillis()): Int {
        if (!state.isActiveOrPaused) return 0
        val liveSegmentMs = if (state.status == FocusSessionStatus.RUNNING && state.lastResumedWallClockMs > 0L) {
            (nowUtc - state.lastResumedWallClockMs).coerceAtLeast(0L)
        } else {
            0L
        }
        val totalElapsedSec = ((state.accumulatedElapsedMs + liveSegmentMs) / 1000L).toInt()
        return totalElapsedSec.coerceIn(0, state.plannedDurationSec)
    }

    fun serializePomodoroState(state: PomodoroRuntimeState): String {
        return JSONObject().apply {
            put("activeSessionId", state.activeSessionId ?: "")
            put("sessionType", state.sessionType.name)
            put("status", state.status.name)
            put("isActiveOrPaused", state.isActiveOrPaused)
            put("plannedDurationSec", state.plannedDurationSec)
            put("accumulatedElapsedMs", state.accumulatedElapsedMs)
            put("lastResumedWallClockMs", state.lastResumedWallClockMs)
            put("linkedTaskId", state.linkedTaskId ?: "")
            put("linkedNoteId", state.linkedNoteId ?: "")
            put("completedWorkCycles", state.completedWorkCycles)
        }.toString()
    }

    fun deserializePomodoroState(jsonStr: String?): PomodoroRuntimeState {
        if (jsonStr.isNullOrBlank()) return PomodoroRuntimeState()
        return try {
            val obj = JSONObject(jsonStr)
            PomodoroRuntimeState(
                activeSessionId = obj.optString("activeSessionId", "").takeIf { it.isNotBlank() },
                sessionType = FocusSessionType.fromName(obj.optString("sessionType")),
                status = FocusSessionStatus.fromName(obj.optString("status")),
                isActiveOrPaused = obj.optBoolean("isActiveOrPaused", false),
                plannedDurationSec = obj.optInt("plannedDurationSec", FocusSessionType.WORK.defaultDurationSec).coerceAtLeast(60),
                accumulatedElapsedMs = obj.optLong("accumulatedElapsedMs", 0L).coerceAtLeast(0L),
                lastResumedWallClockMs = obj.optLong("lastResumedWallClockMs", 0L),
                linkedTaskId = obj.optString("linkedTaskId", "").takeIf { it.isNotBlank() },
                linkedNoteId = obj.optString("linkedNoteId", "").takeIf { it.isNotBlank() },
                completedWorkCycles = obj.optInt("completedWorkCycles", 0).coerceAtLeast(0)
            )
        } catch (_: Exception) {
            PomodoroRuntimeState()
        }
    }

    fun computeFocusStatistics(
        sessions: List<FocusSessionEntity>,
        nowUtc: Long = System.currentTimeMillis(),
        timeZoneId: String? = null
    ): FocusStatisticsSummary {
        val tzId = resolveTimeZone(timeZoneId).id
        val todayKey = localDateKeyFromMillis(nowUtc, tzId)
        val weekStartKey = addDaysToDateKey(todayKey, -6, tzId)

        val workSessions = sessions.filter { it.sessionType == FocusSessionType.WORK.name }
        val completedWork = workSessions.filter { it.status == FocusSessionStatus.COMPLETED.name }

        val todayCompleted = completedWork.filter { session ->
            val key = session.localDateKey.ifBlank { localDateKeyFromMillis(session.startedAtUtc, tzId) }
            key == todayKey
        }
        val weekCompleted = completedWork.filter { session ->
            val key = session.localDateKey.ifBlank { localDateKeyFromMillis(session.startedAtUtc, tzId) }
            key in weekStartKey..todayKey
        }

        val todayMins = (todayCompleted.sumOf { it.actualFocusedSec } / 60.0).toInt()
        val weekMins = (weekCompleted.sumOf { it.actualFocusedSec } / 60.0).toInt()
        val totalMins = (completedWork.sumOf { it.actualFocusedSec } / 60.0).toInt()
        val finishedOrCancelled = workSessions.count {
            it.status == FocusSessionStatus.COMPLETED.name || it.status == FocusSessionStatus.CANCELLED.name
        }
        val rate = if (finishedOrCancelled > 0) {
            ((completedWork.size * 100f) / finishedOrCancelled).toInt().coerceIn(0, 100)
        } else {
            0
        }
        val avgMins = if (completedWork.isNotEmpty()) {
            (completedWork.sumOf { it.actualFocusedSec } / 60.0 / completedWork.size).toInt()
        } else {
            0
        }

        return FocusStatisticsSummary(
            todayFocusMinutes = todayMins,
            todayCompletedSessions = todayCompleted.size,
            weekFocusMinutes = weekMins,
            weekCompletedSessions = weekCompleted.size,
            totalFocusMinutes = totalMins,
            totalCompletedSessions = completedWork.size,
            completionRatePercent = rate,
            averageSessionMinutes = avgMins
        )
    }

    fun buildDailyProgressReport(
        tasks: List<TaskEntity>,
        reminders: List<ReminderEntity>,
        notes: List<NoteEntity>,
        habits: List<HabitEntity>,
        habitLogs: List<HabitLogEntity>,
        focusSessions: List<FocusSessionEntity>,
        nowUtc: Long = System.currentTimeMillis(),
        timeZoneId: String? = null
    ): DailyProgressReport {
        val tzId = resolveTimeZone(timeZoneId).id
        val todayKey = localDateKeyFromMillis(nowUtc, tzId)
        val todayStart = startOfDayForDateKey(todayKey, tzId)

        val activeTasks = tasks.filter { !it.isArchived }
        val linkedReminderIds = activeTasks.mapNotNull { it.linkedReminderId }.toSet()
        // Count unlinked reminders once so tasks are never double-counted
        val standaloneReminders = reminders.filter { it.id !in linkedReminderIds }

        val tasksCompletedToday = activeTasks.count { task ->
            task.isCompleted && task.completedAtUtc != null &&
                localDateKeyFromMillis(task.completedAtUtc, tzId) == todayKey
        } + standaloneReminders.count { rem ->
            rem.isCompleted && localDateKeyFromMillis(rem.scheduledTimeUtc, tzId) == todayKey
        }

        val tasksDueOrPendingToday = activeTasks.count { task ->
            !task.isCompleted && (
                (task.dueAtUtc != null && localDateKeyFromMillis(task.dueAtUtc, tzId) == todayKey) ||
                    (task.scheduledStartUtc != null && localDateKeyFromMillis(task.scheduledStartUtc, tzId) == todayKey)
                )
        } + standaloneReminders.count { rem ->
            !rem.isCompleted && localDateKeyFromMillis(rem.scheduledTimeUtc, tzId) == todayKey
        }

        val overdueCount = activeTasks.count { task ->
            !task.isCompleted && task.dueAtUtc != null && task.dueAtUtc < todayStart
        } + standaloneReminders.count { rem ->
            !rem.isCompleted && rem.scheduledTimeUtc < todayStart
        }

        var subtasksDone = 0
        var subtasksTotal = 0
        activeTasks.forEach { task ->
            val items = SmartNoteUtils.parseChecklistJson(task.subtasksJson)
            subtasksDone += items.count { it.isDone }
            subtasksTotal += items.size
        }
        // Only inspect unlocked, visible, active notes for checklists
        notes.filter { !it.isLocked && !it.isHidden && !it.isDeleted && !it.isArchived && it.checklistJson.isNotBlank() }
            .forEach { note ->
                val items = SmartNoteUtils.parseChecklistJson(note.checklistJson)
                subtasksDone += items.count { it.isDone }
                subtasksTotal += items.size
            }

        val blocksTodayTasks = activeTasks.filter { task ->
            task.scheduledStartUtc != null && localDateKeyFromMillis(task.scheduledStartUtc, tzId) == todayKey
        }
        val blocksTodayReminders = standaloneReminders.filter { rem ->
            localDateKeyFromMillis(rem.scheduledTimeUtc, tzId) == todayKey
        }
        val scheduledBlocksTotal = blocksTodayTasks.size + blocksTodayReminders.size
        val scheduledBlocksDone = blocksTodayTasks.count { it.isCompleted } + blocksTodayReminders.count { it.isCompleted }

        val activeHabits = habits.filter { !it.isArchived }
        val habitLogsByHabit = habitLogs.groupBy { it.habitId }
        val habitsDoneToday = activeHabits.count { habit ->
            computeHabitStreak(habit, habitLogsByHabit[habit.id].orEmpty(), nowUtc).isDoneToday
        }

        val todayWorkSessions = focusSessions.filter { session ->
            session.sessionType == FocusSessionType.WORK.name &&
                session.status == FocusSessionStatus.COMPLETED.name &&
                (session.localDateKey.ifBlank { localDateKeyFromMillis(session.startedAtUtc, tzId) } == todayKey)
        }
        val focusMinutesToday = (todayWorkSessions.sumOf { it.actualFocusedSec } / 60.0).toInt()

        // Weighted productivity score (Tasks + Habits + Focus)
        val components = mutableListOf<Float>()
        val taskDenom = (tasksCompletedToday + tasksDueOrPendingToday)
        if (taskDenom > 0) {
            components.add(tasksCompletedToday.toFloat() / taskDenom.toFloat())
        }
        if (activeHabits.isNotEmpty()) {
            components.add(habitsDoneToday.toFloat() / activeHabits.size.toFloat())
        }
        if (focusMinutesToday > 0 || todayWorkSessions.isNotEmpty()) {
            components.add((focusMinutesToday / 50f).coerceIn(0f, 1f))
        }
        val scorePct = if (components.isNotEmpty()) {
            ((components.sum() / components.size) * 100f).toInt().coerceIn(0, 100)
        } else {
            0
        }

        return DailyProgressReport(
            localDateKey = todayKey,
            tasksCompletedToday = tasksCompletedToday,
            tasksDueOrPendingToday = tasksDueOrPendingToday,
            overdueTasksCount = overdueCount,
            subtasksCompleted = subtasksDone,
            subtasksTotal = subtasksTotal,
            scheduledBlocksCompleted = scheduledBlocksDone,
            scheduledBlocksTotal = scheduledBlocksTotal,
            habitsCompletedToday = habitsDoneToday,
            habitsTotal = activeHabits.size,
            focusMinutesToday = focusMinutesToday,
            focusSessionsToday = todayWorkSessions.size,
            productivityScorePercent = scorePct
        )
    }

    fun buildWeeklyProgressReport(
        tasks: List<TaskEntity>,
        reminders: List<ReminderEntity>,
        habits: List<HabitEntity>,
        habitLogs: List<HabitLogEntity>,
        focusSessions: List<FocusSessionEntity>,
        nowUtc: Long = System.currentTimeMillis(),
        timeZoneId: String? = null
    ): WeeklyProgressReport {
        val tzId = resolveTimeZone(timeZoneId).id
        val todayKey = localDateKeyFromMillis(nowUtc, tzId)
        val startKey = addDaysToDateKey(todayKey, -6, tzId)
        val dayKeys = (-6..0).map { addDaysToDateKey(todayKey, it, tzId) }

        val activeTasks = tasks.filter { !it.isArchived }
        val linkedReminderIds = activeTasks.mapNotNull { it.linkedReminderId }.toSet()
        val standaloneReminders = reminders.filter { it.id !in linkedReminderIds }
        val activeHabits = habits.filter { !it.isArchived }
        val habitLogsByHabit = habitLogs.groupBy { it.habitId }

        val completedWork = focusSessions.filter {
            it.sessionType == FocusSessionType.WORK.name && it.status == FocusSessionStatus.COMPLETED.name
        }

        val bars = dayKeys.map { key ->
            val tasksDoneOnDay = activeTasks.count { task ->
                task.isCompleted && task.completedAtUtc != null &&
                    localDateKeyFromMillis(task.completedAtUtc, tzId) == key
            } + standaloneReminders.count { rem ->
                rem.isCompleted && localDateKeyFromMillis(rem.scheduledTimeUtc, tzId) == key
            }

            val focusSecOnDay = completedWork.filter { s ->
                (s.localDateKey.ifBlank { localDateKeyFromMillis(s.startedAtUtc, tzId) }) == key
            }.sumOf { it.actualFocusedSec }

            val habitsOnDay = habitLogs.count { log ->
                log.localDateKey == key && log.isCompleted
            }

            DailyReportBar(
                localDateKey = key,
                dayLabel = dayLabelForDateKey(key, tzId),
                tasksCompleted = tasksDoneOnDay,
                focusMinutes = (focusSecOnDay / 60.0).toInt(),
                habitsCompleted = habitsOnDay
            )
        }

        val totalTasksDone = bars.sumOf { it.tasksCompleted }
        val totalPending = activeTasks.count { !it.isCompleted } + standaloneReminders.count { !it.isCompleted }
        val totalFocusMins = bars.sumOf { it.focusMinutes }
        val totalFocusSessions = completedWork.count { s ->
            val key = s.localDateKey.ifBlank { localDateKeyFromMillis(s.startedAtUtc, tzId) }
            key in startKey..todayKey
        }
        val totalHabitCheckIns = bars.sumOf { it.habitsCompleted }
        val bestStreak = activeHabits.maxOfOrNull { habit ->
            computeHabitStreak(habit, habitLogsByHabit[habit.id].orEmpty(), nowUtc).currentStreak
        } ?: 0

        val bestDay = bars.maxByOrNull { it.tasksCompleted * 10 + it.habitsCompleted * 5 + it.focusMinutes / 10 }
        val bestDayLabel = if (bestDay != null && (bestDay.tasksCompleted + bestDay.habitsCompleted + bestDay.focusMinutes) > 0) {
            bestDay.dayLabel
        } else {
            "—"
        }

        val catCounts = activeTasks.filter { it.isCompleted }.groupingBy { it.category.ifBlank { "Work" } }.eachCount()
        val totalWorkPool = totalTasksDone + totalPending
        val ratePct = if (totalWorkPool > 0) ((totalTasksDone * 100f) / totalWorkPool).toInt().coerceIn(0, 100) else 0

        return WeeklyProgressReport(
            startDateKey = startKey,
            endDateKey = todayKey,
            dailyBars = bars,
            totalTasksCompleted = totalTasksDone,
            totalPendingTasks = totalPending,
            totalFocusMinutes = totalFocusMins,
            totalFocusSessions = totalFocusSessions,
            totalHabitCheckIns = totalHabitCheckIns,
            bestActiveHabitStreak = bestStreak,
            mostProductiveDayLabel = bestDayLabel,
            categoryTaskCounts = catCounts,
            weeklyCompletionRatePercent = ratePct
        )
    }

    fun defaultDashboardWidgets(): List<DashboardWidgetItem> =
        DashboardWidgetType.entries.mapIndexed { idx, type ->
            DashboardWidgetItem(type = type, enabled = idx < 4, orderIndex = idx)
        }

    fun encodeDashboardWidgets(items: List<DashboardWidgetItem>): String {
        val arr = JSONArray()
        items.sortedBy { it.orderIndex }.forEachIndexed { idx, item ->
            arr.put(
                JSONObject().apply {
                    put("type", item.type.id)
                    put("enabled", item.enabled)
                    put("orderIndex", idx)
                }
            )
        }
        return arr.toString()
    }

    fun parseDashboardWidgets(jsonStr: String?): List<DashboardWidgetItem> {
        if (jsonStr.isNullOrBlank()) return defaultDashboardWidgets()
        return try {
            val arr = JSONArray(jsonStr)
            val parsed = mutableListOf<DashboardWidgetItem>()
            val seenTypes = HashSet<DashboardWidgetType>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val typeId = obj.optString("type", "")
                val type = DashboardWidgetType.entries.firstOrNull { it.id == typeId } ?: continue
                if (seenTypes.add(type)) {
                    parsed.add(
                        DashboardWidgetItem(
                            type = type,
                            enabled = obj.optBoolean("enabled", true),
                            orderIndex = obj.optInt("orderIndex", parsed.size)
                        )
                    )
                }
            }
            // Append any newly added widget types safely at the end
            DashboardWidgetType.entries.forEach { type ->
                if (type !in seenTypes) {
                    parsed.add(
                        DashboardWidgetItem(
                            type = type,
                            enabled = false,
                            orderIndex = parsed.size
                        )
                    )
                }
            }
            parsed.sortedBy { it.orderIndex }.mapIndexed { idx, item -> item.copy(orderIndex = idx) }
        } catch (_: Exception) {
            defaultDashboardWidgets()
        }
    }

    fun safeTaskLinkedNoteTitle(note: NoteEntity?): String {
        return when {
            note == null -> ""
            note.isLocked || note.isHidden -> "Locked Note"
            else -> note.title.ifBlank { "Untitled Note" }
        }
    }

    fun prioritySortWeight(priorityName: String): Int = TaskPriority.fromName(priorityName).level
}
