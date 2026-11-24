package com.example.widget

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.model.AttachmentEntity
import com.example.data.model.CanvasEntity
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.data.model.NoteType
import com.example.data.model.ReminderEntity
import com.example.data.model.StickyNoteEntity
import com.example.util.ReminderNotificationManager
import com.example.util.SmartNoteUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WidgetTaskItem(
    val id: String,
    val title: String,
    val sourceLabel: String,
    val isDone: Boolean,
    val isReminder: Boolean,
    val noteId: String,
    val checklistIndex: Int = -1,
    val reminderId: String = "",
    val taskId: String = ""
)

data class DailyTasksWidgetSnapshot(
    val doneCount: Int,
    val totalCount: Int,
    val items: List<WidgetTaskItem>
)

data class WidgetReminderItem(
    val reminderId: String,
    val noteId: String,
    val noteTitle: String,
    val timeLabel: String,
    val repeatLabel: String?,
    val isOverdue: Boolean,
    val isToday: Boolean,
    val isLockedNote: Boolean,
    val scheduledTimeUtc: Long
)

data class RemindersWidgetSnapshot(
    val pendingCount: Int,
    val overdueCount: Int,
    val todayCount: Int,
    val items: List<WidgetReminderItem>
)

data class WidgetFolderSummary(
    val id: String,
    val name: String,
    val noteCount: Int
)

data class CollectionsWidgetSnapshot(
    val activeNotesCount: Int,
    val folderCount: Int,
    val pendingTasksCount: Int,
    val pendingRemindersCount: Int,
    val favoritesCount: Int,
    val stickyCount: Int,
    val canvasCount: Int,
    val pdfCount: Int,
    val topFolders: List<WidgetFolderSummary>
)

object WidgetDataLoader {

    fun buildDailyTasksSnapshot(
        notes: List<NoteEntity>,
        reminders: List<ReminderEntity>,
        nowMs: Long = System.currentTimeMillis(),
        tasks: List<com.example.data.model.TaskEntity> = emptyList()
    ): DailyTasksWidgetSnapshot {
        val visibleNotes = notes.filter { !it.isDeleted && !it.isArchived && !it.isHidden && !it.isLocked }
        val noteById = visibleNotes.associateBy { it.id }
        val allNoteById = notes.associateBy { it.id }
        val linkedReminderIds = tasks.mapNotNull { it.linkedReminderId }.toSet()

        val taskItems = mutableListOf<WidgetTaskItem>()
        var doneCount = 0
        var totalCount = 0

        // 0. Kanban & Productivity Tasks
        val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        tasks.forEach { task ->
            val linkedNote = task.linkedNoteId?.let { allNoteById[it] }
            val isHiddenOrLocked = linkedNote != null && (linkedNote.isLocked || linkedNote.isHidden || linkedNote.isDeleted)
            val displayTitle = if (isHiddenOrLocked) "Locked Note Task" else task.title.ifBlank { "Task" }
            val targetTime = task.scheduledStartUtc ?: task.dueAtUtc
            val sourceLabel = if (targetTime != null) {
                val timeText = timeFmt.format(Date(targetTime))
                if (SmartNoteUtils.isSameDay(targetTime, nowMs)) "Today · $timeText"
                else "${SmartNoteUtils.formatShortDate(targetTime)} · $timeText"
            } else {
                task.priority.lowercase().replaceFirstChar { it.uppercase() }
            }
            totalCount++
            if (task.isCompleted) doneCount++
            if (!task.isCompleted || (targetTime != null && SmartNoteUtils.isSameDay(targetTime, nowMs))) {
                taskItems.add(
                    WidgetTaskItem(
                        id = "task_${task.id}",
                        title = displayTitle,
                        sourceLabel = sourceLabel,
                        isDone = task.isCompleted,
                        isReminder = task.linkedReminderId != null,
                        noteId = if (!isHiddenOrLocked && linkedNote != null) linkedNote.id else "",
                        reminderId = task.linkedReminderId.orEmpty(),
                        taskId = task.id
                    )
                )
            }
        }

        // 1. Checklist items from unlocked active notes
        visibleNotes
            .sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.updatedAtUtc })
            .forEach { note ->
                val checklist = SmartNoteUtils.parseChecklistJson(note.checklistJson)
                if (checklist.isNotEmpty()) {
                    val noteLabel = note.title.ifBlank { "Checklist" }
                    checklist.forEachIndexed { idx, item ->
                        if (item.text.isNotBlank()) {
                            totalCount++
                            if (item.isDone) doneCount++
                            taskItems.add(
                                WidgetTaskItem(
                                    id = "chk_${note.id}_$idx",
                                    title = item.text.trim(),
                                    sourceLabel = noteLabel,
                                    isDone = item.isDone,
                                    isReminder = false,
                                    noteId = note.id,
                                    checklistIndex = idx
                                )
                            )
                        }
                    }
                }
            }

        // 2. Standalone scheduled reminders (excluding those already represented by TaskEntity)
        reminders.filter { it.id !in linkedReminderIds }.forEach { rem ->
            val linkedNote = noteById[rem.noteId] ?: return@forEach
            val isTodayOrPending = !rem.isCompleted || SmartNoteUtils.isSameDay(rem.scheduledTimeUtc, nowMs)
            totalCount++
            if (rem.isCompleted) doneCount++
            if (isTodayOrPending) {
                val timeText = timeFmt.format(Date(rem.scheduledTimeUtc))
                val label = if (SmartNoteUtils.isSameDay(rem.scheduledTimeUtc, nowMs)) {
                    "Today · $timeText"
                } else {
                    "${SmartNoteUtils.formatShortDate(rem.scheduledTimeUtc)} · $timeText"
                }
                taskItems.add(
                    WidgetTaskItem(
                        id = "rem_${rem.id}",
                        title = linkedNote.title.ifBlank { "Scheduled Task" },
                        sourceLabel = label,
                        isDone = rem.isCompleted,
                        isReminder = true,
                        noteId = linkedNote.id,
                        reminderId = rem.id
                    )
                )
            }
        }

        // Put incomplete tasks first, then completed tasks
        val sortedItems = (taskItems.filter { !it.isDone } + taskItems.filter { it.isDone }).take(14)

        return DailyTasksWidgetSnapshot(
            doneCount = doneCount,
            totalCount = totalCount,
            items = sortedItems
        )
    }

    fun buildRemindersSnapshot(
        allNotes: List<NoteEntity>,
        reminders: List<ReminderEntity>,
        nowMs: Long = System.currentTimeMillis()
    ): RemindersWidgetSnapshot {
        val notesById = allNotes.associateBy { it.id }
        val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dayFmt = SimpleDateFormat("MMM d", Locale.getDefault())

        val pending = reminders
            .filter { !it.isCompleted }
            .mapNotNull { rem ->
                val note = notesById[rem.noteId]
                if (note != null && note.isDeleted) return@mapNotNull null
                val isLocked = note?.isLocked == true || note?.isHidden == true
                val title = when {
                    isLocked -> "Locked Note Reminder"
                    note != null && note.title.isNotBlank() -> note.title
                    else -> "Note Reminder"
                }
                val isOverdue = rem.scheduledTimeUtc < nowMs
                val isToday = SmartNoteUtils.isSameDay(rem.scheduledTimeUtc, nowMs)
                val timeStr = timeFmt.format(Date(rem.scheduledTimeUtc))
                val timeLabel = when {
                    isOverdue && isToday -> "Overdue · $timeStr"
                    isOverdue -> "Overdue · ${dayFmt.format(Date(rem.scheduledTimeUtc))}"
                    isToday -> "Today · $timeStr"
                    else -> "${dayFmt.format(Date(rem.scheduledTimeUtc))} · $timeStr"
                }
                WidgetReminderItem(
                    reminderId = rem.id,
                    noteId = rem.noteId,
                    noteTitle = title,
                    timeLabel = timeLabel,
                    repeatLabel = rem.repeatInterval,
                    isOverdue = isOverdue,
                    isToday = isToday,
                    isLockedNote = isLocked,
                    scheduledTimeUtc = rem.scheduledTimeUtc
                )
            }
            .sortedBy { it.scheduledTimeUtc }

        return RemindersWidgetSnapshot(
            pendingCount = pending.size,
            overdueCount = pending.count { it.isOverdue },
            todayCount = pending.count { it.isToday },
            items = pending.take(12)
        )
    }

    fun buildCollectionsSnapshot(
        allNotes: List<NoteEntity>,
        folders: List<FolderEntity>,
        stickyNotes: List<StickyNoteEntity>,
        canvases: List<CanvasEntity>,
        attachments: List<AttachmentEntity>,
        reminders: List<ReminderEntity>,
        tasks: List<com.example.data.model.TaskEntity> = emptyList()
    ): CollectionsWidgetSnapshot {
        val visibleNotes = allNotes.filter {
            !it.isDeleted && !it.isArchived && !it.isHidden && !it.isLocked
        }
        val standardActive = visibleNotes.filter { it.noteType != NoteType.PDF_DOC.name }
        val favoritesCount = standardActive.count { it.isFavorite }

        val deletedNoteIds = allNotes.filter { it.isDeleted }.map { it.id }.toSet()
        val visibleNoteIds = visibleNotes.map { it.id }.toSet()
        var pendingChecklistTasks = 0
        visibleNotes.forEach { note ->
            pendingChecklistTasks += SmartNoteUtils.parseChecklistJson(note.checklistJson).count { !it.isDone }
        }
        val linkedReminderIds = tasks.mapNotNull { it.linkedReminderId }.toSet()
        val pendingKanbanTasks = tasks.count { !it.isCompleted }
        val pendingVisibleReminders = reminders.count {
            it.id !in linkedReminderIds && !it.isCompleted && (it.noteId in visibleNoteIds || allNotes.none { n -> n.id == it.noteId })
        }
        val activeRemindersCount = reminders.count { !it.isCompleted && it.noteId !in deletedNoteIds }

        val nonPublicNoteIds = allNotes.filter {
            it.isDeleted || it.isArchived || it.isLocked || it.isHidden
        }.map { it.id }.toSet()

        val pdfCount = attachments.count {
            !it.id.startsWith("demo-") && it.id != "demo_pdf" &&
                !it.isDeleted && !it.isArchived && !it.isLocked && !it.isHidden &&
                (it.noteId == null || it.noteId !in nonPublicNoteIds) &&
                (it.mimeType.contains("pdf", ignoreCase = true) || it.fileExtension.equals("pdf", ignoreCase = true))
        }

        val countsByFolder = standardActive
            .mapNotNull { it.folderId?.takeIf { id -> id.isNotBlank() } }
            .groupingBy { it }
            .eachCount()

        val topFolders = folders
            .map { folder ->
                WidgetFolderSummary(
                    id = folder.id,
                    name = folder.name,
                    noteCount = countsByFolder[folder.id] ?: 0
                )
            }
            .sortedWith(compareByDescending<WidgetFolderSummary> { it.noteCount }.thenBy { it.name.lowercase() })
            .take(4)

        return CollectionsWidgetSnapshot(
            activeNotesCount = standardActive.size,
            folderCount = folders.size,
            pendingTasksCount = pendingKanbanTasks + pendingChecklistTasks + pendingVisibleReminders,
            pendingRemindersCount = activeRemindersCount,
            favoritesCount = favoritesCount,
            stickyCount = stickyNotes.size,
            canvasCount = canvases.size,
            pdfCount = pdfCount,
            topFolders = topFolders
        )
    }

    suspend fun loadDailyTasksSnapshot(context: Context): DailyTasksWidgetSnapshot = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context.applicationContext)
        AppDatabase.seedInitialDataDirect(db)
        val notes = db.noteDao().getAllNotesDirect()
        val reminders = db.reminderDao().getAllRemindersDirect()
        val tasks = db.productivityDao().getAllTasksDirect()
        buildDailyTasksSnapshot(notes, reminders, System.currentTimeMillis(), tasks)
    }

    suspend fun loadRemindersSnapshot(context: Context): RemindersWidgetSnapshot = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context.applicationContext)
        AppDatabase.seedInitialDataDirect(db)
        val notes = db.noteDao().getAllNotesDirect()
        val reminders = db.reminderDao().getAllRemindersDirect()
        buildRemindersSnapshot(notes, reminders)
    }

    suspend fun loadCollectionsSnapshot(context: Context): CollectionsWidgetSnapshot = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context.applicationContext)
        AppDatabase.seedInitialDataDirect(db)
        val notes = db.noteDao().getAllNotesDirect()
        val folders = db.folderDao().getAllFoldersDirect()
        val stickies = db.stickyNoteDao().getAllStickyNotesDirect()
        val canvases = db.canvasDao().getAllCanvasesDirect()
        val attachments = db.attachmentDao().getAllAttachmentsDirect()
        val reminders = db.reminderDao().getAllRemindersDirect()
        val tasks = db.productivityDao().getAllTasksDirect()
        buildCollectionsSnapshot(notes, folders, stickies, canvases, attachments, reminders, tasks)
    }

    suspend fun toggleChecklistTask(context: Context, noteId: String, itemIndex: Int) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context.applicationContext)
            val note = db.noteDao().getNoteByIdDirect(noteId) ?: return@withContext
            if (note.isLocked || note.isDeleted) return@withContext
            val items = SmartNoteUtils.parseChecklistJson(note.checklistJson).toMutableList()
            if (itemIndex !in items.indices) return@withContext
            items[itemIndex] = items[itemIndex].copy(isDone = !items[itemIndex].isDone)
            val updated = note.copy(
                checklistJson = SmartNoteUtils.encodeChecklistJson(items),
                updatedAtUtc = System.currentTimeMillis()
            )
            db.noteDao().updateNote(updated)
        }
        WidgetUpdateHelper.updateAllWidgets(context)
    }

    suspend fun toggleKanbanTask(context: Context, taskId: String) {
        withContext(Dispatchers.IO) {
            val appContext = context.applicationContext
            val db = AppDatabase.getDatabase(appContext)
            val repo = com.example.data.repository.NoteRepository(db, appContext)
            val taskBefore = db.productivityDao().getTaskByIdDirect(taskId) ?: return@withContext
            repo.toggleTaskCompleted(taskId)
            val linkedRemId = taskBefore.linkedReminderId
            if (!linkedRemId.isNullOrBlank()) {
                val reminder = db.reminderDao().getReminderById(linkedRemId)
                if (reminder != null) {
                    if (reminder.isCompleted) {
                        ReminderNotificationManager.cancelReminder(appContext, reminder.id)
                    } else if (reminder.scheduledTimeUtc > System.currentTimeMillis()) {
                        val note = db.noteDao().getNoteByIdDirect(reminder.noteId)
                        ReminderNotificationManager.scheduleReminder(
                            context = appContext,
                            reminder = reminder,
                            noteTitle = if (note?.isLocked == true) "Locked Note Reminder" else (note?.title ?: taskBefore.title)
                        )
                    }
                }
            }
        }
        WidgetUpdateHelper.updateAllWidgets(context)
    }

    suspend fun toggleReminderCompletion(context: Context, reminderId: String) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context.applicationContext)
            val reminder = db.reminderDao().getReminderById(reminderId) ?: return@withContext
            val updated = reminder.copy(isCompleted = !reminder.isCompleted)
            db.reminderDao().updateReminder(updated)
            // Keep any linked Kanban task in sync
            val linkedTask = db.productivityDao().getTaskByLinkedReminderIdDirect(reminderId)
            if (linkedTask != null && linkedTask.isCompleted != updated.isCompleted) {
                val columns = db.productivityDao().getAllKanbanColumnsDirect()
                val targetCol = if (updated.isCompleted) {
                    columns.firstOrNull { it.isDoneColumn }?.id ?: linkedTask.columnId
                } else {
                    columns.firstOrNull { !it.isDoneColumn }?.id ?: linkedTask.columnId
                }
                val now = System.currentTimeMillis()
                db.productivityDao().updateTask(
                    linkedTask.copy(
                        isCompleted = updated.isCompleted,
                        completedAtUtc = if (updated.isCompleted) now else null,
                        columnId = targetCol,
                        updatedAtUtc = now
                    )
                )
            }
            if (updated.isCompleted) {
                ReminderNotificationManager.cancelReminder(context.applicationContext, reminder.id)
            } else if (updated.scheduledTimeUtc > System.currentTimeMillis()) {
                val note = db.noteDao().getNoteByIdDirect(reminder.noteId)
                ReminderNotificationManager.scheduleReminder(
                    context = context.applicationContext,
                    reminder = updated,
                    noteTitle = if (note?.isLocked == true) "Locked Note Reminder" else (note?.title ?: "Reminder")
                )
            }
        }
        WidgetUpdateHelper.updateAllWidgets(context)
    }
}
