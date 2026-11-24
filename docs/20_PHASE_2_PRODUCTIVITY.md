# NoteLand 2.0 — Phase 2: Productivity Workspace

**Document Identifier:** NL-DOC-020  
**Status:** Completed & Verified  
**Target Codebase:** NoteLand (Kotlin + Jetpack Compose + Material 3 + Room + Coroutines + WorkManager, `com.example` / `com.aistudio.noteland.gzugkw`)  
**Verification Date:** November 17, 2026  
**Debug APK Build:** Passing (`compile_applet`)  
**Automated Test Suite:** 157/157 Passing across 12 test suites (`gradle :app:testDebugUnitTest --console=plain`, 0 failed, 0 errors, 0 skipped)

---

## 1. Executive Summary

Phase 2 upgraded NoteLand's To-Do and Calendar subsystems into a cohesive, offline-first **Productivity Workspace** integrated cleanly with existing notes and reminders. The upgrade spans normalized Room persistence (`AppDatabase` v5 via non-destructive `MIGRATION_4_5`), timezone-aware recurring scheduling, cycle-safe task dependencies, accessible Kanban drag-and-drop & move controls, Calendar time-blocking, habit streak tracking, daily/weekly progress reports, a process-recreation-safe Pomodoro focus timer backed by `SharedPreferences` + WorkManager, customizable Home dashboard widgets, and v5 encrypted ZIP/JSON backup round-tripping.

All existing tasks and reminders are preserved without duplication via idempotent backfill (`linkedReminderId` unique index + `task_rem_<reminderId>` keys), locked notes remain masked across calendar/reports/tasks, and the Home `Workspace` grid remains clean of Favourites and Tasks cards.

---

## 2. Database Schema & Non-Destructive `MIGRATION_4_5`

Upgraded `AppDatabase` from **Version 4** to **Version 5** via `MIGRATION_4_5` (`app/src/main/java/com/example/data/db/AppDatabase.kt`), introducing **6 normalized tables**, foreign keys, and targeted indices:

### 2.1 `kanban_columns` (`KanbanColumnEntity`)
- **Columns:** `id` (PK), `boardId`, `title`, `colorHex`, `position`, `wipLimit`, `isDoneColumn`, `createdAtUtc`, `updatedAtUtc`
- **Index:** `index_kanban_columns_boardId_position`
- **Default Columns Seeded Idempotently (`ProductivityEngine.defaultKanbanColumns`):** `To Do` (`col_todo`), `In Progress` (`col_in_progress`, WIP limit `3`), `Review` (`col_review`), `Done` (`col_done`, `isDoneColumn = true`).

### 2.2 `tasks` (`TaskEntity`)
- **Columns:** `id` (PK), `title`, `description`, `columnId`, `orderInColumn`, `priority` (`URGENT`/`HIGH`/`MEDIUM`/`LOW`), `isCompleted`, `completedAtUtc`, `dueAtUtc`, `dueTimeZoneId`, `scheduledStartUtc`, `scheduledEndUtc`, `estimatedMinutes`, `recurrenceRule` (`NONE`/`DAILY`/`WEEKDAYS`/`WEEKLY`/`MONTHLY`/`CUSTOM`), `recurrenceInterval`, `recurrenceDaysCsv`, `recurrenceParentId`, `subtasksJson`, `linkedNoteId`, `linkedReminderId`, `category`, `colorHex`, `tagsCsv`, `isArchived`, `createdAtUtc`, `updatedAtUtc`
- **Foreign Keys:**
  - `linkedNoteId -> notes(id)` (`ON DELETE SET NULL`)
  - `linkedReminderId -> reminders(id)` (`ON DELETE SET NULL`)
- **Indices:** `index_tasks_columnId`, `index_tasks_columnId_orderInColumn`, `index_tasks_dueAtUtc`, `index_tasks_scheduledStartUtc`, `index_tasks_isCompleted`, `index_tasks_linkedNoteId`, Unique `index_tasks_linkedReminderId`

### 2.3 `task_dependencies` (`TaskDependencyEntity`)
- **Composite PK:** `(taskId, dependsOnTaskId)`
- **Foreign Keys:** `taskId -> tasks(id)` and `dependsOnTaskId -> tasks(id)` (both `ON DELETE CASCADE`)
- **Indices:** `index_task_dependencies_taskId`, `index_task_dependencies_dependsOnTaskId`

### 2.4 `habits` (`HabitEntity`) & `habit_logs` (`HabitLogEntity`)
- **`habits`:** `id` (PK), `title`, `description`, `category`, `colorHex`, `iconName`, `frequencyType`, `targetPerDay`, `targetDaysPerWeek`, `reminderHour`, `reminderMinute`, `timeZoneId`, `isArchived`, `createdAtUtc`, `updatedAtUtc`; indexed by `isArchived`, `createdAtUtc`.
- **`habit_logs`:** `id` (PK), `habitId`, `localDateKey`, `completedCount`, `targetCountSnapshot`, `isCompleted`, `note`, `timeZoneId`, `completedAtUtc`; FK `habitId -> habits(id) ON DELETE CASCADE`; unique composite index `index_habit_logs_habitId_localDateKey` + `index_habit_logs_habitId`, `index_habit_logs_localDateKey`.

### 2.5 `focus_sessions` (`FocusSessionEntity`)
- **Columns:** `id` (PK), `sessionType` (`WORK`/`SHORT_BREAK`/`LONG_BREAK`), `linkedTaskId`, `linkedNoteId`, `plannedDurationSec`, `actualFocusedSec`, `status` (`RUNNING`/`PAUSED`/`COMPLETED`/`CANCELLED`), `startedAtUtc`, `endedAtUtc`, `localDateKey`, `timeZoneId`
- **Foreign Keys:** `linkedTaskId -> tasks(id)` (`ON DELETE SET NULL`), `linkedNoteId -> notes(id)` (`ON DELETE SET NULL`)
- **Indices:** `linkedTaskId`, `linkedNoteId`, `startedAtUtc`, `localDateKey`, `status`

### 2.6 Non-Duplicating Backfill from Pre-Existing Reminders & Notes
- `AppDatabase.seedInitialDataDirect` converts pre-existing non-deleted reminders cleanly into `tasks` using deterministic IDs (`task_rem_<reminder.id>`) guarded by `getTaskByLinkedReminderIdDirect(rem.id) == null` and `UNIQUE INDEX index_tasks_linkedReminderId`, preserving existing notes, schedules, and calendar metadata while masking `isLocked` notes as `"Locked Note Task"`.

---

## 3. All 11 Phase 2 Features Implemented

1. **Kanban Boards with Customizable Columns (`KanbanBoardTab.kt`):**
   - Multi-column board (`kanban_board_tab`) supporting custom column creation, renaming, color selection, WIP limit configuration, column reordering, and safe column deletion (automatically reassigning existing cards to a remaining fallback column rather than orphaning or deleting user tasks).
2. **Accessible Drag-and-Drop Task Cards (`KanbanBoardTab.kt` + `ProductivityDao`):**
   - Pointer drag-and-drop across columns (`pointerInput` long-press/drag) paired with explicit keyboard/screen-reader-accessible controls inside task menus and move actions ("Move Left / Right Column", "Edit / Add Subtasks / Dependencies", "Complete Task") meeting `48.dp` minimum touch targets.
3. **Task Priorities & Subtasks (`TaskModels.kt`, `ProductivityEngine.kt`, `TaskEditorSheet`):**
   - 4-tier priority model (`URGENT`, `HIGH`, `MEDIUM`, `LOW`) with visual badges/filters and persistent JSON-encoded subtasks (`id`, `title`, `isDone`) synced inside task completion & progress metrics.
4. **Due Dates & Timezone-Aware Recurring Tasks (`ProductivityEngine.computeNextOccurrenceUtc`):**
   - Uses local-hour/minute wall-clock anchoring via `Calendar.getInstance(resolveTimeZone(timeZoneId))` across `DAILY`, `WEEKDAYS` (skipping Sat/Sun), `WEEKLY`, `MONTHLY` (clamping month-end days e.g. Jan 31 -> Feb 28/29), and `CUSTOM` day-of-week masks so DST transitions preserve intended local wall-clock time without 23h/25h drift.
   - Completing a recurring task atomically spawns **at most one** next occurrence (`spawnNextRecurrenceIfNeeded` checked by `recurrenceParentId` + target window).
5. **Task Dependencies with Cycle Prevention (`ProductivityEngine.wouldCreateDependencyCycle`):**
   - DFS reachability check rejects self-loops (`taskA -> taskA`) and transitive cycles (`A -> B -> C -> A`), surfaces blocked/blocker status badges on task cards, and prevents premature completion/blocking confusion.
6. **Time-Blocking Calendar Integration (`CalendarScreen.kt`):**
   - Unified Calendar Schedule & Tasks tabs expose `"Open Kanban"` chip (`calendar_open_kanban_chip`), `"Time-Block Task"` picker (`calendar_timeblock_picker_btn`), overlap conflict detection (`detectTimeBlockOverlaps`), linked reminder/task deduplication (preventing duplicate rows when a reminder and task share `linkedReminderId`), timezone badge display, and `"Locked Note"` masking for locked notes.
7. **Habit Tracker with Streaks (`HabitsTab.kt` + `ProductivityEngine.computeHabitStreak`):**
   - Supports daily/weekly habit creation, category/color/icon styling, daily increment/toggle check-ins backed by unique `(habitId, localDateKey)` logs in SQLite, timezone-aware `currentStreak`, `longestStreak`, `completionRate30d`, and 7-day rolling completion history.
8. **Daily & Weekly Progress Reports (`ProgressReportsTab.kt` + `ProductivityEngine`):**
   - **Daily Report (`report_mode_daily`):** Daily productivity score, tasks completed/pending, overdue warning banner, subtask completion progress bar, scheduled block adherence, habits completed today, and focus minutes/sessions logged.
   - **Weekly Report (`report_mode_weekly`):** 7-day breakdown with a custom Compose `Canvas` stacked/grouped bar chart (`weekly_chart_canvas`) across completed tasks, habits, and focus minutes, plus best active streak, most productive weekday, and category distribution.
9. **Pomodoro Focus Timer & Process-Recreation Recovery (`PomodoroFocusTab.kt`, `NoteViewModel.kt`, `ProductivityWorkers.kt`):**
   - Work (`25m`), Short Break (`5m`), and Long Break (`15m`) presets with custom durations, Play/Pause/Reset/Complete controls, and optional linking to a task (`linkedTaskId`) or note (`linkedNoteId`).
   - Persists wall-clock runtime snapshot (`PomodoroRuntimeState`) into `SharedPreferences` (`noteland_pomodoro_prefs`) on every start, pause, resume, reset, or completion. On `NoteViewModel` initialization (`restorePomodoroFromPrefs()`), remaining time is recomputed accurately against elapsed wall clock (`System.currentTimeMillis() - lastResumedWallClockMs`), and already-expired running sessions finalize automatically into `focus_sessions` with `COMPLETED` status.
10. **Focus Session History & Statistics (`PomodoroFocusTab.kt` + `ProductivityEngine.computeFocusStats`):**
    - Computes today/week/all-time focus minutes, completed session counts, completion rate %, average duration, and lists recent sessions with task/note context while keeping locked notes hidden/masked.
11. **Customizable Dashboard Widgets (`DashboardWidgetsSection.kt` + `HomeScreen.kt` + `BackupManager.kt` v5):**
    - Renders configurable widget cards (`KANBAN_SUMMARY`, `POMODORO_TIMER`, `HABIT_STREAKS`, `TIME_BLOCKS`, `PROGRESS_REPORT`) inside Home's grid (`dashboard_widgets_section`) with a `"Customize"` modal (`customize_widgets_button`) for toggling visibility and reordering, persisted via `SharedPreferences` and included in **Backup Schema v5** encrypted ZIP/JSON backups alongside all 6 Phase 2 Room tables.

---

## 4. Background Scheduling Architecture (`WorkManager`)

`app/src/main/java/com/example/worker/ProductivityWorkers.kt` uses WorkManager selectively only where reliable deferred background execution is required:
- **`PomodoroCompletionWorker` (`ExistingWorkPolicy.REPLACE`):** Scheduled with remaining delay when a Pomodoro session runs; re-schedules if woken early, otherwise finalizes `FocusSessionEntity`, clears `noteland_pomodoro_prefs`, increments completed work cycles, and triggers `ReminderNotificationManager`.
- **`ProductivityMaintenanceWorker` (`PeriodicWorkRequest` every 6 hours, `ExistingPeriodicWorkPolicy.KEEP`):** Performs idempotent column/reminder backfill maintenance and refreshes home screen widgets (`WidgetUpdateHelper.updateAllWidgets`).

---

## 5. Files Created & Modified in Phase 2

### Created Production Files
- `app/src/main/java/com/example/data/model/TaskModels.kt` (`KanbanColumnEntity`, `TaskEntity`, `TaskDependencyEntity`, `TaskPriority`, `TaskRecurrenceRule`, `SubtaskItem`)
- `app/src/main/java/com/example/data/model/HabitModels.kt` (`HabitEntity`, `HabitLogEntity`, `HabitFrequency`)
- `app/src/main/java/com/example/data/model/FocusSessionEntity.kt` (`FocusSessionEntity`, `FocusSessionType`, `FocusSessionStatus`)
- `app/src/main/java/com/example/data/db/ProductivityDao.kt`
- `app/src/main/java/com/example/util/ProductivityEngine.kt`
- `app/src/main/java/com/example/worker/ProductivityWorkers.kt`
- `app/src/main/java/com/example/ui/screens/ProductivityScreen.kt`
- `app/src/main/java/com/example/ui/screens/KanbanBoardTab.kt`
- `app/src/main/java/com/example/ui/screens/HabitsTab.kt`
- `app/src/main/java/com/example/ui/screens/PomodoroFocusTab.kt`
- `app/src/main/java/com/example/ui/screens/ProgressReportsTab.kt`
- `app/src/main/java/com/example/ui/components/DashboardWidgetsSection.kt`

### Modified Production Files
- `app/src/main/java/com/example/data/db/AppDatabase.kt` (upgraded to `version = 5`, added `MIGRATION_4_5`, 6 Phase 2 entities, `ProductivityDao`, idempotent column & reminder backfill)
- `app/src/main/java/com/example/data/repository/NoteRepository.kt` (Phase 2 flows, safe non-cascading task/habit/focus upserts, cycle checks, recurring spawn logic)
- `app/src/main/java/com/example/ui/viewmodel/NoteViewModel.kt` (Kanban/task/habit/Pomodoro/reports/widget state & actions; ordered `init` after StateFlow initializers for safe restoration)
- `app/src/main/java/com/example/ui/screens/CalendarScreen.kt` (time-blocking sheet, overlap checks, task/reminder deduplication, Kanban navigation chip, locked note masking)
- `app/src/main/java/com/example/ui/screens/HomeScreen.kt` (integrated `DashboardWidgetsSection` while keeping `Workspace` free of Favourites & Tasks cards)
- `app/src/main/java/com/example/MainActivity.kt` (Productivity Hub navigation & `ProductivityWorkScheduler.ensurePeriodicMaintenance`)
- `app/src/main/java/com/example/util/BackupManager.kt` (upgraded to Schema v5 covering all Phase 2 tables + `dashboard_widgets` config across encrypted ZIP and JSON backup/restore)

### Test & Documentation Files
- `app/src/test/java/com/example/Phase2ProductivityTest.kt`
- `app/src/test/java/com/example/Phase1KnowledgeBrainTest.kt` (MainLooper pumping for Robolectric coroutine tests)
- `app/src/test/java/com/example/MainActivityCrashTest.kt` (updated Home scroll bounds for taller Home layout with Dashboard Widgets)
- `docs/20_PHASE_2_PRODUCTIVITY.md`

---

## 6. Verification Results (Actual Executed Outputs)

### 6.1 Full Unit & Robolectric Test Suite (`gradle :app:testDebugUnitTest --console=plain`)
- **Command:** `gradle :app:testDebugUnitTest --console=plain`
- **Outcome:** `BUILD SUCCESSFUL in 1m 20s` — **157/157 tests passed (0 failed, 0 errors, 0 skipped)**

| Test Suite | Executed | Passed | Failed | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| `com.example.Phase2ProductivityTest` | 11 | 11 | 0 | 0 | 0 |
| `com.example.Phase1KnowledgeBrainTest` | 11 | 11 | 0 | 0 | 0 |
| `com.example.Phase0StabilityTest` | 10 | 10 | 0 | 0 | 0 |
| `com.example.ExampleRobolectricTest` | 53 | 53 | 0 | 0 | 0 |
| `com.example.NoteLandUnitTest` | 18 | 18 | 0 | 0 | 0 |
| `com.example.StabilizationPassRobolectricTest` | 17 | 17 | 0 | 0 | 0 |
| `com.example.EditorAndThemeStabilizationTest` | 15 | 15 | 0 | 0 | 0 |
| `com.example.PdfAnnotationEditingTest` | 9 | 9 | 0 | 0 | 0 |
| `com.example.MainActivityCrashTest` | 4 | 4 | 0 | 0 | 0 |
| `com.example.PdfTextHighlightEngineTest` | 4 | 4 | 0 | 0 | 0 |
| `com.example.StickyBoardResizeTest` | 3 | 3 | 0 | 0 | 0 |
| `com.example.data.repository.NoteRepositoryTest` | 2 | 2 | 0 | 0 | 0 |
| **Total** | **157** | **157** | **0** | **0** | **0** |

**`Phase2ProductivityTest` Coverage (11 Tests):**
1. Safe `MIGRATION_4_5` preservation of v4 notes/reminders, 6 indexed tables, default Kanban column seeding, idempotent reminder-to-task backfill, and locked note masking.
2. Kanban column reordering/moving cards, `isDoneColumn` completion sync, and safe column deletion reassigning tasks without data loss.
3. Task dependency DFS cycle rejection (self-loops and transitive multi-hop cycles).
4. Priority & subtask progress + persistence (`URGENT`/`HIGH`/`MEDIUM`/`LOW`).
5. Recurrence rules across DST spring-forward/fall-back transitions, `WEEKDAYS` skipping weekends, and `MONTHLY` month-end clamping (`Jan 31 -> Feb 28/29`).
6. Recurring task completion spawning exactly one next occurrence without duplication.
7. Habit tracker timezone-aware current/longest streaks and unique `(habitId, localDateKey)` log enforcement.
8. Pomodoro runtime recovery after `ViewModel` / process recreation and automatic completion logging of expired wall-clock sessions.
9. Daily/weekly report deduplication of linked reminder/task records, locked note privacy masking, dashboard widget config persistence, and BackupManager Schema v5 round-trip.
10. Compose `ProductivityScreen` navigation across Kanban, Habits, Focus, and Reports (Daily -> Weekly chart canvas).
11. Compose `HomeScreen` verification (Workspace clean of Favourites/Tasks cards + customizable Dashboard Widgets dialog) and `CalendarScreen` Time-Block Task picker & Kanban integration.

### 6.2 Debug APK Compilation (`compile_applet`)
- **Command:** `compile_applet`
- **Outcome:** `Build succeeded - the applet is compiled`

---

## 7. Known Limitations

1. **JVM / Robolectric Execution Scope:** All 157 automated tests execute on the JVM under Robolectric SDK 36; physical OEM `WorkManager` / Doze wake timing under strict manufacturer battery optimization policies remains governed by Android OS scheduling and user battery optimization exemptions.
2. **Offline-Only Synchronization Boundary:** All Phase 2 boards, tasks, habits, focus logs, and dashboard widget layouts are strictly local/offline (`Room` + `SharedPreferences`) with encrypted manual ZIP/JSON backup; multi-device cloud sync is out of scope for Phase 2.
3. **Locked Note Privacy Trade-off:** Tasks/reminders linked to locked notes intentionally mask titles and exclude encrypted checklists/descriptions when the vault is locked so private content cannot leak onto Kanban boards, Calendar timelines, or Progress Reports.
