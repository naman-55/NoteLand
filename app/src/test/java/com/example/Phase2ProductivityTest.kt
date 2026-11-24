package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.FocusSessionEntity
import com.example.data.model.FocusSessionStatus
import com.example.data.model.FocusSessionType
import com.example.data.model.HabitEntity
import com.example.data.model.HabitFrequency
import com.example.data.model.HabitLogEntity
import com.example.data.model.KanbanColumnEntity
import com.example.data.model.NoteEntity
import com.example.data.model.NoteType
import com.example.data.model.ReminderEntity
import com.example.data.model.TaskDependencyEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskRecurrenceRule
import com.example.data.repository.NoteRepository
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProductivityScreen
import com.example.ui.theme.NoteLandTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.BackupManager
import com.example.util.ChecklistItem
import com.example.util.DashboardWidgetType
import com.example.util.PomodoroRuntimeState
import com.example.util.ProductivityEngine
import com.example.util.RestoreStrategy
import com.example.util.SmartNoteUtils
import com.example.worker.PomodoroCompletionWorker
import java.io.File
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase2ProductivityTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun openIsolatedDb(context: Context, name: String): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.setForeignKeyConstraintsEnabled(true)
                }
            })
            .allowMainThreadQueries()
            .build()
    }

    // =========================================================================
    // 1. SAFE ROOM MIGRATION 4 -> 5, INDICES, BACKFILL & PRIVACY
    // =========================================================================

    @Test
    fun `safe MIGRATION_4_5 preserves v4 data, builds indexed tables, backfills reminders cleanly, and masks locked notes`() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "phase2_migration_v4_v5_${UUID.randomUUID()}.db"
        context.deleteDatabase(dbName)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        AppDatabase.MIGRATION_1_2.migrate(db)
                        AppDatabase.MIGRATION_2_3.migrate(db)
                        AppDatabase.MIGRATION_3_4.migrate(db)
                        db.execSQL("INSERT INTO `folders` VALUES ('f-v4', NULL, 'Work', '#3B82F6', 'work', 1000, 1000)")
                        db.execSQL(
                            "INSERT INTO `notes` (`id`, `folderId`, `folderName`, `title`, `content`, `noteType`, `colorHex`, `tagsCsv`, `isPinned`, `isArchived`, `isDeleted`, `isLocked`, `isHidden`, `isFavorite`, `reminderTimestamp`, `canvasDataJson`, `checklistJson`, `imageUri`, `createdAtUtc`, `updatedAtUtc`, `deletedAtUtc`) " +
                                "VALUES ('n-open', 'f-v4', 'Work', 'Quarterly Plan', 'Ship v2 cleanly', 'STANDARD', '#FFFFFF', 'Work', 0, 0, 0, 0, 0, 0, 5000000, '', '[]', '', 1000, 2000, NULL)"
                        )
                        db.execSQL(
                            "INSERT INTO `notes` (`id`, `folderId`, `folderName`, `title`, `content`, `noteType`, `colorHex`, `tagsCsv`, `isPinned`, `isArchived`, `isDeleted`, `isLocked`, `isHidden`, `isFavorite`, `reminderTimestamp`, `canvasDataJson`, `checklistJson`, `imageUri`, `createdAtUtc`, `updatedAtUtc`, `deletedAtUtc`) " +
                                "VALUES ('n-locked', 'f-v4', 'Work', 'Secret PIN Vault', 'Private passphrase', 'STANDARD', '#FFFFFF', '', 0, 0, 0, 1, 0, 0, 6000000, '', '[]', '', 1000, 2000, NULL)"
                        )
                        db.execSQL("INSERT INTO `reminders` VALUES ('r-open', 'n-open', 5000000, 'DAILY', 0, 1500)")
                        db.execSQL("INSERT INTO `reminders` VALUES ('r-locked', 'n-locked', 6000000, NULL, 0, 1600)")
                        db.execSQL("INSERT INTO `app_settings` VALUES ('db_initialized', 'true', 1000)")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.writableDatabase.close()
        helper.close()

        val db = openIsolatedDb(context, dbName)
        AppDatabase.seedInitialDataDirect(db)
        // Running seed twice must be 100% idempotent (no duplicate tasks or columns)
        AppDatabase.seedInitialDataDirect(db)

        val openNote = db.noteDao().getNoteByIdDirect("n-open")
        assertNotNull(openNote)
        assertEquals("Quarterly Plan", openNote!!.title)

        val columns = db.productivityDao().getAllKanbanColumnsDirect()
        assertEquals(4, columns.size)
        assertEquals(listOf("To Do", "In Progress", "Review", "Done"), columns.map { it.title })

        val tasks = db.productivityDao().getAllTasksDirect()
        assertEquals("Pre-existing reminders should backfill once into tasks", 2, tasks.size)
        val lockedTask = tasks.first { it.linkedReminderId == "r-locked" }
        assertEquals("Locked Note Task", lockedTask.title)
        assertEquals("", lockedTask.description)

        val openTask = tasks.first { it.linkedReminderId == "r-open" }
        assertEquals("Quarterly Plan", openTask.title)
        assertEquals("DAILY", openTask.recurrenceRule)

        val sqlite = db.openHelper.writableDatabase
        sqlite.query("PRAGMA integrity_check").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("ok", c.getString(0))
        }
        sqlite.query("PRAGMA foreign_key_check").use { c ->
            assertFalse("No FK violations after MIGRATION_4_5", c.moveToFirst())
        }

        val taskIndices = mutableSetOf<String>()
        sqlite.query("PRAGMA index_list(`tasks`)").use { c ->
            val idx = c.getColumnIndex("name")
            while (c.moveToNext()) taskIndices.add(c.getString(idx))
        }
        assertTrue(taskIndices.contains("index_tasks_columnId"))
        assertTrue(taskIndices.contains("index_tasks_linkedReminderId"))
        assertTrue(taskIndices.contains("index_tasks_dueAtUtc"))

        db.close()
        context.deleteDatabase(dbName)
    }

    // =========================================================================
    // 2. KANBAN COLUMNS, CARDS, REORDER & NON-DESTRUCTIVE DELETE
    // =========================================================================

    @Test
    fun `kanban columns move cards sync completion and delete reassigns tasks safely`() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "phase2_kanban_${UUID.randomUUID()}.db"
        context.deleteDatabase(dbName)
        val db = openIsolatedDb(context, dbName)
        AppDatabase.seedInitialDataDirect(db)
        val repo = NoteRepository(db)

        val customCol = KanbanColumnEntity(
            id = "col_qa",
            title = "QA Review",
            colorHex = "#EC4899",
            position = 2,
            wipLimit = 3,
            isDoneColumn = false
        )
        assertTrue(repo.saveKanbanColumn(customCol).isSuccess)

        val t1 = TaskEntity(
            id = "t-1",
            title = "Implement OAuth",
            columnId = "col_qa",
            orderInColumn = 0,
            priority = TaskPriority.HIGH.name,
            subtasksJson = SmartNoteUtils.encodeChecklistJson(
                listOf(ChecklistItem("s1", "Token refresh", false))
            )
        )
        val t2 = TaskEntity(
            id = "t-2",
            title = "Write integration suite",
            columnId = "col_qa",
            orderInColumn = 1,
            priority = TaskPriority.URGENT.name
        )
        assertTrue(repo.saveTask(t1).isSuccess)
        assertTrue(repo.saveTask(t2).isSuccess)

        // Reorder within column
        assertTrue(repo.reorderTaskInColumn("t-2", -1).isSuccess)
        val inQaAfterReorder = db.productivityDao().getTasksInColumnDirect("col_qa")
        assertEquals(listOf("t-2", "t-1"), inQaAfterReorder.map { it.id })

        // Moving to Done column automatically marks task completed
        assertTrue(repo.moveTaskToColumn("t-1", "col_done").isSuccess)
        val movedToDone = repo.getTaskByIdDirect("t-1")!!
        assertEquals("col_done", movedToDone.columnId)
        assertTrue(movedToDone.isCompleted)
        assertNotNull(movedToDone.completedAtUtc)

        // Deleting custom column reassigns remaining t-2 rather than deleting it
        assertTrue(repo.deleteKanbanColumn("col_qa").isSuccess)
        val reassigned = repo.getTaskByIdDirect("t-2")
        assertNotNull("Task in deleted column must be reassigned, not lost", reassigned)
        assertFalse(reassigned!!.columnId == "col_qa")

        db.close()
        context.deleteDatabase(dbName)
    }

    // =========================================================================
    // 3. RECURRING TASKS (DST, WEEKDAYS, MONTH-END) & NO DUPLICATES
    // =========================================================================

    @Test
    fun `recurrence rules handle DST transition, weekdays skip weekends, and monthly clamps month end`() {
        val nyTzId = "America/New_York"
        val nyTz = TimeZone.getTimeZone(nyTzId)

        // DST Spring Forward in America/New_York: 2026-03-08 02:00 jumps to 03:00
        val calBeforeDst = Calendar.getInstance(nyTz).apply {
            set(2026, Calendar.MARCH, 7, 9, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nextDaily = ProductivityEngine.calculateNextOccurrenceUtc(
            currentUtc = calBeforeDst.timeInMillis,
            rule = TaskRecurrenceRule.DAILY,
            interval = 1,
            timeZoneId = nyTzId
        )!!
        val resultDailyCal = Calendar.getInstance(nyTz).apply { timeInMillis = nextDaily }
        assertEquals(Calendar.MARCH, resultDailyCal.get(Calendar.MONTH))
        assertEquals(8, resultDailyCal.get(Calendar.DAY_OF_MONTH))
        assertEquals("Wall clock hour must remain 9 AM across DST", 9, resultDailyCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, resultDailyCal.get(Calendar.MINUTE))

        // WEEKDAYS from Friday -> Monday
        val fridayCal = Calendar.getInstance(nyTz).apply {
            set(2026, Calendar.MAY, 8, 10, 0, 0) // Friday May 8, 2026
            set(Calendar.MILLISECOND, 0)
        }
        val nextWeekday = ProductivityEngine.calculateNextOccurrenceUtc(
            currentUtc = fridayCal.timeInMillis,
            rule = TaskRecurrenceRule.WEEKDAYS,
            interval = 1,
            timeZoneId = nyTzId
        )!!
        val resultWeekdayCal = Calendar.getInstance(nyTz).apply { timeInMillis = nextWeekday }
        assertEquals(Calendar.MONDAY, resultWeekdayCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(11, resultWeekdayCal.get(Calendar.DAY_OF_MONTH))

        // MONTHLY from Jan 31 -> Feb 28 (2026 non-leap year)
        val jan31 = Calendar.getInstance(nyTz).apply {
            set(2026, Calendar.JANUARY, 31, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nextMonthly = ProductivityEngine.calculateNextOccurrenceUtc(
            currentUtc = jan31.timeInMillis,
            rule = TaskRecurrenceRule.MONTHLY,
            interval = 1,
            timeZoneId = nyTzId
        )!!
        val resultMonthlyCal = Calendar.getInstance(nyTz).apply { timeInMillis = nextMonthly }
        assertEquals(Calendar.FEBRUARY, resultMonthlyCal.get(Calendar.MONTH))
        assertEquals(28, resultMonthlyCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `completing recurring task spawns exactly one next occurrence without duplication`() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "phase2_recur_${UUID.randomUUID()}.db"
        context.deleteDatabase(dbName)
        val db = openIsolatedDb(context, dbName)
        AppDatabase.seedInitialDataDirect(db)
        val repo = NoteRepository(db)

        val now = System.currentTimeMillis()
        val recurringTask = TaskEntity(
            id = "t-recur-1",
            title = "Daily Standup Prep",
            columnId = "col_todo",
            dueAtUtc = now,
            scheduledStartUtc = now,
            scheduledEndUtc = now + 30 * 60_000L,
            recurrenceRule = TaskRecurrenceRule.DAILY.name
        )
        repo.saveTask(recurringTask)

        // Complete once -> spawns 1 next occurrence
        assertTrue(repo.toggleTaskCompleted("t-recur-1").isSuccess)
        val afterFirstToggle = repo.getAllTasksDirect().filter { it.title == "Daily Standup Prep" }
        assertEquals(2, afterFirstToggle.size)
        val child = afterFirstToggle.first { it.id != "t-recur-1" }
        assertEquals("t-recur-1", child.recurrenceParentId)
        assertFalse(child.isCompleted)

        // Reopen & re-complete original task -> must NOT spawn a duplicate occurrence
        assertTrue(repo.toggleTaskCompleted("t-recur-1").isSuccess)
        assertTrue(repo.toggleTaskCompleted("t-recur-1").isSuccess)
        val afterRetry = repo.getAllTasksDirect().filter { it.title == "Daily Standup Prep" }
        assertEquals("Must not duplicate spawned recurring instance", 2, afterRetry.size)

        db.close()
        context.deleteDatabase(dbName)
    }

    // =========================================================================
    // 4. TASK DEPENDENCY GRAPH & CYCLE DETECTION
    // =========================================================================

    @Test
    fun `task dependency engine rejects self loops and transitive cycles`() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "phase2_deps_${UUID.randomUUID()}.db"
        context.deleteDatabase(dbName)
        val db = openIsolatedDb(context, dbName)
        AppDatabase.seedInitialDataDirect(db)
        val repo = NoteRepository(db)

        val a = TaskEntity(id = "task-A", title = "A", columnId = "col_todo")
        val b = TaskEntity(id = "task-B", title = "B", columnId = "col_todo")
        val c = TaskEntity(id = "task-C", title = "C", columnId = "col_todo")
        repo.saveTask(a); repo.saveTask(b); repo.saveTask(c)

        // Self-loop rejected
        assertTrue(repo.addTaskDependency("task-A", "task-A").isFailure)

        // B depends on A; C depends on B -> valid DAG
        assertTrue(repo.addTaskDependency("task-B", "task-A").isSuccess)
        assertTrue(repo.addTaskDependency("task-C", "task-B").isSuccess)

        // A depends on C would close cycle A -> C -> B -> A -> rejected!
        assertTrue(repo.addTaskDependency("task-A", "task-C").isFailure)

        val deps = db.productivityDao().getAllTaskDependenciesDirect()
        val byId = listOf(a, b, c).associateBy { it.id }
        assertTrue(ProductivityEngine.isTaskBlocked("task-B", deps, byId))
        assertFalse(ProductivityEngine.isTaskBlocked("task-A", deps, byId))

        db.close()
        context.deleteDatabase(dbName)
    }

    // =========================================================================
    // 5. HABIT TRACKER, UNIQUE DATE LOGS & STREAKS
    // =========================================================================

    @Test
    fun `habit tracker computes streaks cleanly and enforces unique habitId dateKey logs`() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "phase2_habits_${UUID.randomUUID()}.db"
        context.deleteDatabase(dbName)
        val db = openIsolatedDb(context, dbName)
        val repo = NoteRepository(db)

        val tzId = TimeZone.getDefault().id
        val now = System.currentTimeMillis()
        val todayKey = ProductivityEngine.localDateKeyFromMillis(now, tzId)
        val yesterdayKey = ProductivityEngine.addDaysToDateKey(todayKey, -1, tzId)
        val twoDaysAgoKey = ProductivityEngine.addDaysToDateKey(todayKey, -2, tzId)

        val habit = HabitEntity(
            id = "h-read",
            title = "Read 20 Pages",
            frequencyType = HabitFrequency.DAILY.name,
            targetPerDay = 2,
            timeZoneId = tzId
        )
        assertTrue(repo.saveHabit(habit).isSuccess)

        // Past completed days
        db.productivityDao().insertHabitLog(
            HabitLogEntity("log-2", habit.id, twoDaysAgoKey, 2, 2, true, "", tzId, now - 172_800_000L)
        )
        db.productivityDao().insertHabitLog(
            HabitLogEntity("log-1", habit.id, yesterdayKey, 2, 2, true, "", tzId, now - 86_400_000L)
        )

        // Two check-ins today for targetPerDay = 2 update single unique row rather than duplicating
        assertTrue(repo.checkInHabit(habit.id, todayKey, 1).isSuccess)
        assertTrue(repo.checkInHabit(habit.id, todayKey, 1).isSuccess)
        val todayLogs = db.productivityDao().getAllHabitLogsDirect().filter { it.localDateKey == todayKey }
        assertEquals(1, todayLogs.size)
        assertTrue(todayLogs.first().isCompleted)
        assertEquals(2, todayLogs.first().completedCount)

        val summary = ProductivityEngine.computeHabitStreak(
            habit = habit,
            logsForHabit = db.productivityDao().getAllHabitLogsDirect(),
            nowUtc = now
        )
        assertEquals(3, summary.currentStreak)
        assertEquals(3, summary.longestStreak)
        assertTrue(summary.isDoneToday)
        assertTrue(summary.completionRate30d in 0..100)

        db.close()
        context.deleteDatabase(dbName)
    }

    // =========================================================================
    // 6. POMODORO RECOVERY ACROSS PROCESS RECREATION & FOCUS WORKER
    // =========================================================================

    @Test
    fun `pomodoro state survives ViewModel recreation and records focus statistics`() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences(PomodoroCompletionWorker.PREFS_POMODORO, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        val db = AppDatabase.getDatabase(context)
        AppDatabase.seedInitialDataDirect(db)
        db.productivityDao().clearAllFocusSessions()

        val sessionId = "pom-recover-1"
        val now = System.currentTimeMillis()
        // Simulate a 25-minute session that started 10 minutes ago and was interrupted by process death
        val runningSession = FocusSessionEntity(
            id = sessionId,
            sessionType = FocusSessionType.WORK.name,
            plannedDurationSec = 1500,
            actualFocusedSec = 0,
            status = FocusSessionStatus.RUNNING.name,
            startedAtUtc = now - 600_000L,
            localDateKey = ProductivityEngine.localDateKeyFromMillis(now),
            timeZoneId = TimeZone.getDefault().id
        )
        db.productivityDao().insertFocusSession(runningSession)

        val persistedRuntime = PomodoroRuntimeState(
            activeSessionId = sessionId,
            sessionType = FocusSessionType.WORK,
            status = FocusSessionStatus.RUNNING,
            isActiveOrPaused = true,
            plannedDurationSec = 1500,
            accumulatedElapsedMs = 0L,
            lastResumedWallClockMs = now - 600_000L,
            completedWorkCycles = 1
        )
        prefs.edit().putString(
            PomodoroCompletionWorker.KEY_POMODORO_STATE,
            ProductivityEngine.serializePomodoroState(persistedRuntime)
        ).commit()

        val recreatedVm = NoteViewModel(context.applicationContext as Application)
        recreatedVm.restorePomodoroFromPrefs()

        val restoredState = recreatedVm.pomodoroState.value
        assertEquals(FocusSessionStatus.RUNNING, restoredState.status)
        assertEquals(sessionId, restoredState.activeSessionId)
        val remaining = recreatedVm.pomodoroRemainingSeconds.value
        assertTrue("Recovered remaining seconds (~900s) expected, got $remaining", remaining in 880..910)

        // Simulate expired session finalizing automatically when process restarts after timer finished
        val expiredSessionId = "pom-expired-1"
        db.productivityDao().insertFocusSession(
            runningSession.copy(id = expiredSessionId, startedAtUtc = now - 2_000_000L)
        )
        prefs.edit().putString(
            PomodoroCompletionWorker.KEY_POMODORO_STATE,
            ProductivityEngine.serializePomodoroState(
                persistedRuntime.copy(
                    activeSessionId = expiredSessionId,
                    lastResumedWallClockMs = now - 2_000_000L
                )
            )
        ).commit()

        recreatedVm.restorePomodoroFromPrefs()
        // Allow Main + IO coroutines to finalize expired Pomodoro session in SQLite
        var finalized: FocusSessionEntity? = null
        repeat(40) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            finalized = db.productivityDao().getFocusSessionByIdDirect(expiredSessionId)
            if (finalized?.status == FocusSessionStatus.COMPLETED.name) return@repeat
            kotlinx.coroutines.delay(50)
        }
        assertEquals(FocusSessionStatus.COMPLETED.name, finalized?.status)
        assertEquals(1500, finalized?.actualFocusedSec)
        recreatedVm.resetPomodoro(recordCancelled = false)
        prefs.edit().clear().commit()
    }

    // =========================================================================
    // 7. REPORTS DEDUPLICATION, PRIVACY, WIDGET CONFIG & BACKUP V5
    // =========================================================================

    @Test
    fun `reports deduplicate linked reminders, protect locked notes, and BackupManager v5 roundtrips Phase 2`() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val now = System.currentTimeMillis()
        val tzId = TimeZone.getDefault().id
        val todayKey = ProductivityEngine.localDateKeyFromMillis(now, tzId)

        val lockedNote = NoteEntity(
            id = "n-sec",
            title = "Top Secret Medical",
            content = "Hidden content",
            isLocked = true
        )
        assertEquals("Locked Note", ProductivityEngine.safeTaskLinkedNoteTitle(lockedNote))

        val linkedRem = ReminderEntity(
            id = "rem-dup",
            noteId = lockedNote.id,
            scheduledTimeUtc = now,
            isCompleted = true
        )
        val linkedTask = TaskEntity(
            id = "task-dup",
            title = "Health Check",
            columnId = "col_done",
            isCompleted = true,
            completedAtUtc = now,
            scheduledStartUtc = now,
            scheduledEndUtc = now + 3600_000L,
            linkedReminderId = linkedRem.id,
            linkedNoteId = lockedNote.id
        )
        val soloTask = TaskEntity(
            id = "task-solo",
            title = "Write spec",
            columnId = "col_todo",
            isCompleted = false,
            scheduledStartUtc = now + 7200_000L,
            scheduledEndUtc = now + 10_800_000L
        )
        val sessions = listOf(
            FocusSessionEntity(
                id = "fs1",
                sessionType = FocusSessionType.WORK.name,
                plannedDurationSec = 1500,
                actualFocusedSec = 1500,
                status = FocusSessionStatus.COMPLETED.name,
                startedAtUtc = now - 1600_000L,
                endedAtUtc = now - 100_000L,
                localDateKey = todayKey,
                timeZoneId = tzId
            )
        )

        val dailyReport = ProductivityEngine.buildDailyProgressReport(
            tasks = listOf(linkedTask, soloTask),
            reminders = listOf(linkedRem),
            notes = listOf(lockedNote),
            habits = emptyList(),
            habitLogs = emptyList(),
            focusSessions = sessions,
            nowUtc = now
        )
        // linkedRem must NOT be double counted with linkedTask -> total scheduled blocks = 2 (not 3!)
        assertEquals(2, dailyReport.scheduledBlocksTotal)
        assertEquals(1, dailyReport.scheduledBlocksCompleted)
        assertEquals(25, dailyReport.focusMinutesToday)

        val weeklyReport = ProductivityEngine.buildWeeklyProgressReport(
            tasks = listOf(linkedTask, soloTask),
            reminders = listOf(linkedRem),
            habits = emptyList(),
            habitLogs = emptyList(),
            focusSessions = sessions,
            nowUtc = now
        )
        assertEquals(7, weeklyReport.dailyBars.size)
        assertEquals(25, weeklyReport.totalFocusMinutes)

        // Backup v5 Export & Replace Restore verification
        val dbName = "phase2_backup_${UUID.randomUUID()}.db"
        context.deleteDatabase(dbName)
        val db = openIsolatedDb(context, dbName)
        AppDatabase.seedInitialDataDirect(db)
        val repo = NoteRepository(db)

        repo.saveKanbanColumn(KanbanColumnEntity("col_custom", title = "Deployed", colorHex = "#10B981", position = 4))
        repo.saveTask(soloTask.copy(id = "task_bk_1", title = "Backed Up Task"))
        repo.saveHabit(HabitEntity("hab_bk_1", "Meditate", frequencyType = HabitFrequency.DAILY.name))
        repo.checkInHabit("hab_bk_1", todayKey, 1)
        repo.saveFocusSession(sessions.first())

        val settingsPrefs = context.getSharedPreferences("noteland_settings_prefs", Context.MODE_PRIVATE)
        val customWidgets = ProductivityEngine.encodeDashboardWidgets(
            ProductivityEngine.defaultDashboardWidgets().map {
                if (it.type == DashboardWidgetType.PROGRESS_REPORT) it.copy(enabled = false) else it
            }
        )
        settingsPrefs.edit().putString("dashboard_widgets_config", customWidgets).commit()

        val backupManager = BackupManager(context, repo)
        val zipFile = backupManager.createBackupZip()
        assertTrue(zipFile.exists() && zipFile.length() > 0L)

        // Wipe DB and settings before restore
        repo.clearAllData()
        settingsPrefs.edit().clear().commit()
        assertEquals(0, repo.getAllTasksDirect().size)

        val restoreRes = backupManager.restoreFromZip(
            zipFile = zipFile,
            strategy = RestoreStrategy.REPLACE
        )
        assertTrue(restoreRes.isSuccess)
        assertEquals(1, repo.getAllTasksDirect().count { it.id == "task_bk_1" })
        assertEquals(1, db.productivityDao().getAllKanbanColumnsDirect().count { it.id == "col_custom" })
        assertEquals(1, db.productivityDao().getAllHabitsDirect().count { it.id == "hab_bk_1" })
        assertEquals(1, db.productivityDao().getAllHabitLogsDirect().size)
        assertEquals(1, db.productivityDao().getAllFocusSessionsDirect().size)

        val restoredWidgets = ProductivityEngine.parseDashboardWidgets(
            settingsPrefs.getString("dashboard_widgets_config", null)
        )
        assertFalse(restoredWidgets.first { it.type == DashboardWidgetType.PROGRESS_REPORT }.enabled)

        zipFile.delete()
        db.close()
        context.deleteDatabase(dbName)
    }

    // =========================================================================
    // 8. COMPOSE UI TESTS: PRODUCTIVITY HUB, CALENDAR TIME-BLOCKING & HOME WIDGETS
    // =========================================================================

    @Test
    fun `compose ProductivityScreen switches across Kanban Habits Focus and Reports tabs cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = NoteViewModel(context.applicationContext as Application)

        composeRule.setContent {
            NoteLandTheme {
                ProductivityScreen(
                    viewModel = vm,
                    initialTab = 0,
                    onNavigateToCalendar = {},
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("productivity_screen_root").assertIsDisplayed()
        composeRule.onNodeWithTag("kanban_board_tab").assertIsDisplayed()

        // Open Habits tab
        composeRule.onNodeWithTag("productivity_tab_habits").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("habits_tab_root").assertIsDisplayed()

        // Open Focus tab
        composeRule.onNodeWithTag("productivity_tab_focus").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pomodoro_focus_tab").assertIsDisplayed()

        // Open Reports tab
        composeRule.onNodeWithTag("productivity_tab_reports").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("progress_reports_tab").assertIsDisplayed()
        composeRule.onNodeWithTag("report_mode_weekly").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("progress_reports_tab").performScrollToIndex(1)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("weekly_chart_canvas").assertIsDisplayed()
    }

    @Test
    fun `compose HomeScreen preserves clean Workspace without Productivity Widgets, Brain, or Templates`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = NoteViewModel(context.applicationContext as Application)
        var navigatedRoute: String? = null

        composeRule.setContent {
            NoteLandTheme {
                HomeScreen(
                    viewModel = vm,
                    onNavigateToEditor = { _, _ -> },
                    onNavigateToCanvas = {},
                    onNavigateToScreen = { navigatedRoute = it }
                )
            }
        }
        composeRule.waitForIdle()

        // Workspace must remain free of removed cards, Productivity Widgets, Pinned Notes strip, and Recent Activity
        assertEquals(0, composeRule.onAllNodesWithTag("feature_card_favourites").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("feature_card_tasks").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("feature_card_tags").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("feature_card_brain").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("feature_card_templates").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("dashboard_widgets_section").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("home_pinned_notes_section").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("home_recent_activity_section").fetchSemanticsNodes().size)

        // Pinned Notes shortcut tile remains intact and navigates directly to Pinned Notes
        composeRule.onNodeWithTag("feature_card_pinned notes").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(com.example.ui.navigation.Screen.PinnedNotes.route, navigatedRoute)
    }

    @Test
    fun `compose CalendarScreen exposes Time-Block Task picker and Kanban integration`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = NoteViewModel(context.applicationContext as Application)

        composeRule.setContent {
            NoteLandTheme {
                CalendarScreen(
                    viewModel = vm,
                    initialTab = 0,
                    onNavigateToEditor = { _, _ -> },
                    onNavigateToProductivity = {},
                    onNavigateBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calendar_open_kanban_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("calendar_timeblock_picker_btn").performClick()
        composeRule.waitForIdle()
        assertTrue(composeRule.onAllNodesWithText("Start Time").fetchSemanticsNodes().isNotEmpty())
    }
}
