package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.example.data.model.AnnotationEntity
import com.example.data.model.AttachmentEntity
import com.example.data.model.BookmarkEntity
import com.example.data.model.CanvasElementEntity
import com.example.data.model.CanvasEntity
import com.example.data.model.DeletedItemEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.FolderEntity
import com.example.data.model.HabitEntity
import com.example.data.model.HabitLogEntity
import com.example.data.model.KanbanColumnEntity
import com.example.data.model.NoteEntity
import com.example.data.model.NoteLinkEntity
import com.example.data.model.NoteTagEntity
import com.example.data.model.NoteTemplateEntity
import com.example.data.model.NoteVersionEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.StickyNoteEntity
import com.example.data.model.TagEntity
import com.example.data.model.TaskDependencyEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskRecurrenceRule
import com.example.util.ChecklistItem
import com.example.util.FolderHierarchyUtils
import com.example.util.KnowledgeBrainEngine
import com.example.util.ProductivityEngine
import com.example.util.SmartNoteUtils
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface INoteRepository {
    val activeNotes: Flow<List<NoteEntity>>
    val archivedNotes: Flow<List<NoteEntity>>
    val deletedNotes: Flow<List<NoteEntity>>
    val favoriteNotes: Flow<List<NoteEntity>>
    val lockedNotes: Flow<List<NoteEntity>>
    val hiddenNotes: Flow<List<NoteEntity>>
    val folders: Flow<List<FolderEntity>>
    val tags: Flow<List<TagEntity>>
    val stickyNotes: Flow<List<StickyNoteEntity>>
    val canvases: Flow<List<CanvasEntity>>
    val attachments: Flow<List<AttachmentEntity>>
    val deletedAttachments: Flow<List<AttachmentEntity>>
    val favoriteAttachments: Flow<List<AttachmentEntity>>
    val archivedAttachments: Flow<List<AttachmentEntity>>
    val lockedAttachments: Flow<List<AttachmentEntity>>
    val pendingReminders: Flow<List<ReminderEntity>>

    suspend fun saveOrUpdateNote(note: NoteEntity): Result<String>
    suspend fun insertNote(note: NoteEntity): Result<String>
    suspend fun updateNote(note: NoteEntity): Result<Unit>
    suspend fun softDeleteNote(note: NoteEntity): Result<Unit>
    suspend fun restoreNote(note: NoteEntity): Result<Unit>
    suspend fun deletePermanently(id: String): Result<Unit>
    suspend fun emptyRecycleBin(): Result<Unit>
    suspend fun purgeExpiredTrashItems(retentionDays: Int): Result<Int>
    suspend fun togglePin(note: NoteEntity): Result<Unit>
    suspend fun toggleFavorite(note: NoteEntity): Result<Unit>
    suspend fun toggleArchive(note: NoteEntity): Result<Unit>
    suspend fun toggleLock(note: NoteEntity): Result<Unit>
    suspend fun toggleHide(note: NoteEntity): Result<Unit>
    suspend fun insertFolder(folder: FolderEntity): Result<String>
    suspend fun updateFolder(folder: FolderEntity): Result<Unit>
    suspend fun moveFolder(folderId: String, newParentId: String?): Result<Unit>
    suspend fun deleteFolder(id: String): Result<Unit>
    suspend fun moveNoteToFolder(noteId: String, folder: FolderEntity?): Result<Unit>
    suspend fun duplicateNote(note: NoteEntity): Result<String>
    suspend fun insertTag(tag: TagEntity): Result<String>
    suspend fun deleteTag(id: String): Result<Unit>
    suspend fun insertStickyNote(sticky: StickyNoteEntity): Result<String>
    suspend fun updateStickyNote(sticky: StickyNoteEntity): Result<Unit>
    suspend fun deleteStickyNote(id: String): Result<Unit>
    suspend fun saveCanvasWithElements(canvas: CanvasEntity, elements: List<CanvasElementEntity>): Result<String>
    suspend fun deleteCanvas(id: String): Result<Unit>
    fun getElementsForCanvas(canvasId: String): Flow<List<CanvasElementEntity>>
    suspend fun getElementsForCanvasDirect(canvasId: String): List<CanvasElementEntity>
    suspend fun getSetting(key: String): String?
    suspend fun setSetting(key: String, value: String)
    suspend fun saveAttachment(attachment: AttachmentEntity): Result<String>
    suspend fun deleteAttachment(id: String): Result<Unit>
    suspend fun isFileReferenced(relativePath: String, ignoreAttachmentId: String? = null, ignoreNoteId: String? = null): Boolean
    suspend fun getAttachmentById(id: String): AttachmentEntity?
    suspend fun getAttachmentByHash(contentHash: String): AttachmentEntity?
    fun getAttachmentsByFolder(folderId: String): Flow<List<AttachmentEntity>>
    fun getAttachmentsForNote(noteId: String): Flow<List<AttachmentEntity>>
    suspend fun renameAttachment(id: String, newName: String): Result<Unit>
    suspend fun moveAttachmentToFolder(attachmentId: String, targetFolderId: String?): Result<Unit>
    suspend fun toggleAttachmentFavorite(attachment: AttachmentEntity): Result<Unit>
    suspend fun toggleAttachmentArchive(attachment: AttachmentEntity): Result<Unit>
    suspend fun softDeleteAttachment(attachment: AttachmentEntity): Result<Unit>
    suspend fun restoreAttachment(attachment: AttachmentEntity): Result<Unit>
    suspend fun deleteAttachmentPermanently(id: String): Result<Unit>
    suspend fun setAttachmentLocked(
        attachment: AttachmentEntity,
        locked: Boolean,
        storageManager: com.example.data.storage.AttachmentStorageManager
    ): Result<AttachmentEntity>
    fun getAnnotationsForAttachment(attachmentId: String): Flow<List<AnnotationEntity>>
    suspend fun getAnnotationsForAttachmentDirect(attachmentId: String): List<AnnotationEntity>
    suspend fun saveAttachmentWithAnnotations(attachment: AttachmentEntity, annotations: List<AnnotationEntity>): Result<Unit>
    suspend fun saveAnnotation(annotation: AnnotationEntity): Result<String>
    suspend fun deleteAnnotation(annotation: AnnotationEntity): Result<Unit>
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>
    suspend fun insertBookmark(bookmark: BookmarkEntity): Result<String>
    suspend fun updateBookmark(bookmark: BookmarkEntity): Result<Unit>
    suspend fun deleteBookmark(bookmark: BookmarkEntity): Result<Unit>
    suspend fun removeBookmark(targetType: String, targetId: String): Result<Unit>
    val allReminders: Flow<List<ReminderEntity>>
    suspend fun saveReminder(reminder: ReminderEntity): Result<String>
    suspend fun updateReminder(reminder: ReminderEntity): Result<Unit>
    suspend fun deleteReminder(id: String): Result<Unit>
    suspend fun getReminderById(id: String): ReminderEntity?

    // Phase 1: Knowledge Brain (Links, Templates, Version History)
    val noteLinks: Flow<List<NoteLinkEntity>>
    val noteTemplates: Flow<List<NoteTemplateEntity>>
    fun getBacklinksForNote(noteId: String): Flow<List<NoteLinkEntity>>
    fun getOutgoingLinksForNote(noteId: String): Flow<List<NoteLinkEntity>>
    fun getVersionsForNote(noteId: String): Flow<List<NoteVersionEntity>>
    suspend fun ensureBuiltInTemplatesSeeded()
    suspend fun saveTemplate(template: NoteTemplateEntity): Result<String>
    suspend fun deleteTemplate(id: String): Result<Unit>
    suspend fun incrementTemplateUsage(templateId: String)
    suspend fun restoreNoteVersion(noteId: String, versionId: String): Result<NoteEntity>
    suspend fun reconcileAllKnowledgeLinks()

    // Phase 2: Productivity Workspace (Kanban, Tasks, Dependencies, Habits, Focus Sessions)
    val kanbanColumns: Flow<List<KanbanColumnEntity>>
    val activeTasks: Flow<List<TaskEntity>>
    val allTasks: Flow<List<TaskEntity>>
    val taskDependencies: Flow<List<TaskDependencyEntity>>
    val activeHabits: Flow<List<HabitEntity>>
    val habitLogs: Flow<List<HabitLogEntity>>
    val focusSessions: Flow<List<FocusSessionEntity>>

    suspend fun ensureDefaultKanbanColumnsSeeded()
    suspend fun saveKanbanColumn(column: KanbanColumnEntity): Result<String>
    suspend fun reorderKanbanColumns(orderedIds: List<String>): Result<Unit>
    suspend fun deleteKanbanColumn(columnId: String): Result<Unit>
    suspend fun saveTask(task: TaskEntity): Result<String>
    suspend fun moveTaskToColumn(taskId: String, targetColumnId: String, targetOrder: Int? = null): Result<Unit>
    suspend fun reorderTaskInColumn(taskId: String, delta: Int): Result<Unit>
    suspend fun toggleTaskCompleted(taskId: String): Result<Unit>
    suspend fun deleteTask(taskId: String): Result<Unit>
    suspend fun addTaskDependency(taskId: String, dependsOnTaskId: String): Result<Unit>
    suspend fun removeTaskDependency(taskId: String, dependsOnTaskId: String): Result<Unit>
    suspend fun assignTaskTimeBlock(taskId: String, startUtc: Long?, endUtc: Long?): Result<Unit>
    suspend fun saveHabit(habit: HabitEntity): Result<String>
    suspend fun deleteHabit(habitId: String): Result<Unit>
    suspend fun checkInHabit(habitId: String, dateKey: String, increment: Int = 1, timeZoneId: String? = null): Result<HabitLogEntity>
    suspend fun setHabitCompletedForDate(habitId: String, dateKey: String, completed: Boolean, timeZoneId: String? = null): Result<Unit>
    suspend fun saveFocusSession(session: FocusSessionEntity): Result<String>
    suspend fun syncReminderCompletionFromReminder(reminderId: String, isCompleted: Boolean)
}

class NoteRepository(
    private val db: AppDatabase,
    private val context: Context? = null
) : INoteRepository {

    private val noteDao = db.noteDao()
    private val folderDao = db.folderDao()
    private val tagDao = db.tagDao()
    private val stickyNoteDao = db.stickyNoteDao()
    private val canvasDao = db.canvasDao()
    private val attachmentDao = db.attachmentDao()
    private val reminderDao = db.reminderDao()
    private val bookmarkDao = db.bookmarkDao()
    private val appSettingsDao = db.appSettingsDao()
    private val deletedItemDao = db.deletedItemDao()
    private val noteLinkDao = db.noteLinkDao()
    private val noteTemplateDao = db.noteTemplateDao()
    private val noteVersionDao = db.noteVersionDao()
    private val productivityDao = db.productivityDao()

    override val activeNotes: Flow<List<NoteEntity>> = noteDao.getAllActiveNotes()
    override val archivedNotes: Flow<List<NoteEntity>> = noteDao.getArchivedNotes()
    override val deletedNotes: Flow<List<NoteEntity>> = noteDao.getDeletedNotes()
    override val favoriteNotes: Flow<List<NoteEntity>> = noteDao.getFavoriteNotes()
    override val lockedNotes: Flow<List<NoteEntity>> = noteDao.getLockedNotes()
    override val hiddenNotes: Flow<List<NoteEntity>> = noteDao.getHiddenNotes()
    override val folders: Flow<List<FolderEntity>> = folderDao.getAllFolders()
    override val tags: Flow<List<TagEntity>> = tagDao.getAllTags()
    override val stickyNotes: Flow<List<StickyNoteEntity>> = stickyNoteDao.getAllStickyNotes()
    override val canvases: Flow<List<CanvasEntity>> = canvasDao.getAllCanvases()
    override val attachments: Flow<List<AttachmentEntity>> = attachmentDao.getAllAttachments()
    override val deletedAttachments: Flow<List<AttachmentEntity>> = attachmentDao.getDeletedAttachments()
    override val favoriteAttachments: Flow<List<AttachmentEntity>> = attachmentDao.getFavoriteAttachments()
    override val archivedAttachments: Flow<List<AttachmentEntity>> = attachmentDao.getArchivedAttachments()
    override val lockedAttachments: Flow<List<AttachmentEntity>> = attachmentDao.getLockedAttachments()
    override val pendingReminders: Flow<List<ReminderEntity>> = reminderDao.getPendingReminders()
    override val allReminders: Flow<List<ReminderEntity>> = reminderDao.getAllReminders()
    override val noteLinks: Flow<List<NoteLinkEntity>> = noteLinkDao.getAllLinks()
    override val noteTemplates: Flow<List<NoteTemplateEntity>> = noteTemplateDao.getAllTemplates()
    override val kanbanColumns: Flow<List<KanbanColumnEntity>> = productivityDao.getAllKanbanColumns()
    override val activeTasks: Flow<List<TaskEntity>> = productivityDao.getActiveTasks()
    override val allTasks: Flow<List<TaskEntity>> = productivityDao.getAllTasks()
    override val taskDependencies: Flow<List<TaskDependencyEntity>> = productivityDao.getAllTaskDependencies()
    override val activeHabits: Flow<List<HabitEntity>> = productivityDao.getActiveHabits()
    override val habitLogs: Flow<List<HabitLogEntity>> = productivityDao.getAllHabitLogs()
    override val focusSessions: Flow<List<FocusSessionEntity>> = productivityDao.getAllFocusSessions()

    val reminderNotes: Flow<List<NoteEntity>> = noteDao.getNotesWithReminders()

    private fun notifyWidgetsChanged() {
        com.example.widget.WidgetUpdateHelper.scheduleWidgetRefresh(context)
    }

    fun getNotesByType(type: String): Flow<List<NoteEntity>> = noteDao.getNotesByType(type)
    fun getNotesByFolder(folderId: String): Flow<List<NoteEntity>> = noteDao.getNotesByFolder(folderId)
    fun getNoteById(id: String): Flow<NoteEntity?> = noteDao.getNoteById(id)
    suspend fun getNoteByIdDirect(id: String): NoteEntity? = noteDao.getNoteByIdDirect(id)
    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)

    private suspend fun syncKnowledgeForNoteInternal(note: NoteEntity) {
        val allNotes = noteDao.getAllNotesDirect()
        val eligibleNotes = allNotes.filter { KnowledgeBrainEngine.isNoteKnowledgeEligible(it) }

        // Remove existing outbound links from this note; re-add only when eligible
        noteLinkDao.deleteOutgoingLinks(note.id)
        if (!KnowledgeBrainEngine.isNoteKnowledgeEligible(note)) {
            // Never allow locked, hidden, archived, or deleted notes to remain as link targets
            noteLinkDao.clearIncomingTargetNoteId(note.id)
            if (note.isLocked) {
                // Locked notes must never leave plaintext version snapshots
                noteVersionDao.deleteVersionsForNote(note.id)
            }
            return
        }

        val outgoing = KnowledgeBrainEngine.buildOutgoingLinkEntities(
            sourceNote = note,
            eligibleNotes = eligibleNotes
        )
        if (outgoing.isNotEmpty()) {
            noteLinkDao.insertLinks(outgoing)
        }

        // Reconcile existing links from other eligible notes in case a title changed or new target became eligible
        val eligibleIds = eligibleNotes.map { it.id }.toSet()
        val allLinks = noteLinkDao.getAllLinksDirect()
        for (link in allLinks) {
            if (link.sourceNoteId == note.id) continue
            if (link.sourceNoteId !in eligibleIds) {
                noteLinkDao.deleteOutgoingLinks(link.sourceNoteId)
                continue
            }
            val resolved = KnowledgeBrainEngine.resolveTargetNote(
                targetTitle = link.targetTitle,
                eligibleNotes = eligibleNotes,
                excludeNoteId = link.sourceNoteId
            )
            val resolvedId = resolved?.id
            if (link.targetNoteId != resolvedId) {
                noteLinkDao.updateLink(
                    link.copy(
                        targetNoteId = resolvedId,
                        updatedAtUtc = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private suspend fun captureVersionSnapshotIfMeaningful(
        note: NoteEntity,
        changeSummary: String
    ) {
        // Security invariant: NEVER capture plaintext version history for locked or deleted notes
        if (note.isLocked || note.isDeleted) return

        val existingVersions = noteVersionDao.getVersionsForNoteDirect(note.id)
        val latest = existingVersions.firstOrNull()
        if (latest != null &&
            latest.title == note.title &&
            latest.content == note.content &&
            latest.tagsCsv == note.tagsCsv &&
            latest.checklistJson == note.checklistJson
        ) {
            return
        }

        val nextNumber = (noteVersionDao.getLatestVersionNumber(note.id)) + 1
        val wordCount = SmartNoteUtils.countWords(note.content)
        noteVersionDao.insertVersion(
            NoteVersionEntity(
                id = UUID.randomUUID().toString(),
                noteId = note.id,
                versionNumber = nextNumber,
                title = note.title,
                content = note.content,
                tagsCsv = note.tagsCsv,
                checklistJson = note.checklistJson,
                changeSummary = changeSummary,
                wordCount = wordCount,
                createdAtUtc = System.currentTimeMillis()
            )
        )
        noteVersionDao.pruneOldVersions(note.id, keepCount = 25)
    }

    override suspend fun saveOrUpdateNote(note: NoteEntity): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val effectiveId = if (note.id.isBlank() || note.id == "0") UUID.randomUUID().toString() else note.id
                db.withTransaction {
                    val existing = noteDao.getNoteByIdDirect(effectiveId)
                    val persisted = if (existing == null) {
                        val newNote = note.copy(
                            id = effectiveId,
                            createdAtUtc = if (note.createdAtUtc > 0) note.createdAtUtc else System.currentTimeMillis(),
                            updatedAtUtc = System.currentTimeMillis()
                        )
                        noteDao.insertNote(newNote)
                        newNote
                    } else {
                        val updatedNote = note.copy(
                            id = effectiveId,
                            createdAtUtc = existing.createdAtUtc,
                            updatedAtUtc = System.currentTimeMillis()
                        )
                        noteDao.updateNote(updatedNote)
                        updatedNote
                    }
                    syncKnowledgeForNoteInternal(persisted)
                    val summary = if (existing == null) "Initial version" else "Saved edit"
                    captureVersionSnapshotIfMeaningful(persisted, summary)
                }
                notifyWidgetsChanged()
                Result.success(effectiveId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun insertNote(note: NoteEntity): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                db.withTransaction {
                    noteDao.insertNote(note)
                    syncKnowledgeForNoteInternal(note)
                    captureVersionSnapshotIfMeaningful(note, "Initial version")
                }
                notifyWidgetsChanged()
                Result.success(note.id)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateNote(note: NoteEntity): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.withTransaction {
                    noteDao.updateNote(note)
                    syncKnowledgeForNoteInternal(note)
                    captureVersionSnapshotIfMeaningful(note, "Updated note")
                }
                notifyWidgetsChanged()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun softDeleteNote(note: NoteEntity): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val updated = note.copy(
                        isDeleted = true,
                        deletedAtUtc = System.currentTimeMillis()
                    )
                    noteDao.updateNote(updated)
                    syncKnowledgeForNoteInternal(updated)

                    // Record tombstone in deleted_items
                    deletedItemDao.insertDeletedItem(
                        DeletedItemEntity(
                            originalTable = "notes",
                            originalId = note.id,
                            itemSnapshotJson = org.json.JSONObject().apply {
                                put("title", note.title)
                                put("content", note.content)
                            }.toString(),
                            deletedAtUtc = System.currentTimeMillis()
                        )
                    )
                }
                notifyWidgetsChanged()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun restoreNote(note: NoteEntity): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val updated = note.copy(
                        isDeleted = false,
                        deletedAtUtc = null,
                        updatedAtUtc = System.currentTimeMillis()
                    )
                    noteDao.updateNote(updated)
                    syncKnowledgeForNoteInternal(updated)
                }
                notifyWidgetsChanged()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun cleanupReferencesForDeletedNotesInternal(
        noteIds: List<String>,
        remindersToCancelOut: MutableList<String>
    ) {
        for (noteId in noteIds) {
            val associatedReminders = reminderDao.getRemindersForNoteDirect(noteId)
            associatedReminders.forEach { remindersToCancelOut.add(it.id) }
            reminderDao.deleteRemindersForNote(noteId)
            productivityDao.clearLinkedNoteId(noteId)
            productivityDao.clearFocusSessionLinkedNoteId(noteId)
        }
    }

    private fun transformNoteEncryptionForLock(note: NoteEntity, targetLocked: Boolean): NoteEntity {
        val appContext = context ?: return note.copy(
            isLocked = targetLocked,
            updatedAtUtc = System.currentTimeMillis()
        )
        val sec = com.example.security.SecurityManager(appContext)
        val rawRich = SmartNoteUtils.parseRichDocumentJson(note.canvasDataJson)
        val newTitle: String
        val newContent: String
        val newChecklist: String
        val newRichPayload: String
        if (targetLocked) {
            newTitle = sec.encrypt(note.title)
            newContent = sec.encrypt(note.content)
            newChecklist = if (note.checklistJson.isNotBlank() && note.checklistJson != "[]") {
                sec.encrypt(note.checklistJson)
            } else {
                note.checklistJson
            }
            newRichPayload = if (rawRich.isNotBlank() && rawRich.trimStart().startsWith("{")) {
                sec.encrypt(rawRich)
            } else {
                rawRich
            }
        } else {
            newTitle = sec.decrypt(note.title)
            newContent = sec.decrypt(note.content)
            newChecklist = if (note.checklistJson.isNotBlank() && note.checklistJson != "[]") {
                sec.decrypt(note.checklistJson)
            } else {
                note.checklistJson
            }
            newRichPayload = if (rawRich.isNotBlank() && !rawRich.trimStart().startsWith("{")) {
                sec.decrypt(rawRich)
            } else {
                rawRich
            }
        }
        val updatedCanvasJson = if (newRichPayload != rawRich) {
            SmartNoteUtils.withRichDocumentJson(note.canvasDataJson, newRichPayload)
        } else {
            note.canvasDataJson
        }
        return note.copy(
            isLocked = targetLocked,
            title = newTitle,
            content = newContent,
            checklistJson = newChecklist,
            canvasDataJson = updatedCanvasJson,
            updatedAtUtc = System.currentTimeMillis()
        )
    }

    override suspend fun deletePermanently(id: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val remindersToCancel = mutableListOf<String>()
                db.withTransaction {
                    cleanupReferencesForDeletedNotesInternal(listOf(id), remindersToCancel)
                    noteLinkDao.clearIncomingTargetNoteId(id)
                    bookmarkDao.removeBookmark("NOTE", id)
                    noteDao.deletePermanently(id)
                }
                context?.let { ctx ->
                    remindersToCancel.distinct().forEach { remId ->
                        com.example.util.ReminderNotificationManager.cancelReminder(ctx, remId)
                    }
                }
                notifyWidgetsChanged()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun emptyRecycleBin(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val pathsToCheck = mutableListOf<String>()
                val remindersToCancel = mutableListOf<String>()
                db.withTransaction {
                    val deletedNoteIds = noteDao.getAllNotesDirect().filter { it.isDeleted }.map { it.id }
                    cleanupReferencesForDeletedNotesInternal(deletedNoteIds, remindersToCancel)
                    deletedNoteIds.forEach { bookmarkDao.removeBookmark("NOTE", it) }

                    val deletedAtts = attachmentDao.getDeletedAttachmentsDirect()
                    deletedAtts.forEach { att ->
                        if (att.relativePath.isNotBlank()) pathsToCheck.add(att.relativePath)
                        attachmentDao.clearAnnotationsForAttachment(att.id)
                        bookmarkDao.removePdfBookmarksForAttachment(att.id)
                        attachmentDao.deleteAttachmentById(att.id)
                    }
                    noteDao.emptyRecycleBin()
                    deletedItemDao.purgeAllDeletedItems()
                    reconcileAllLinksInternal()
                }
                context?.let { ctx ->
                    remindersToCancel.distinct().forEach { remId ->
                        com.example.util.ReminderNotificationManager.cancelReminder(ctx, remId)
                    }
                }
                pathsToCheck.distinct().forEach { deletePhysicalFileIfUnreferenced(it) }
                notifyWidgetsChanged()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun purgeExpiredTrashItems(retentionDays: Int): Result<Int> {
        if (retentionDays <= 0) return Result.success(0)
        return withContext(Dispatchers.IO) {
            try {
                val cutoffUtc = System.currentTimeMillis() - retentionDays * 86_400_000L
                val pathsToCheck = mutableListOf<String>()
                val remindersToCancel = mutableListOf<String>()
                var purgedCount = 0
                db.withTransaction {
                    val expiredNotes = noteDao.getAllNotesDirect().filter { note ->
                        note.isDeleted && (note.deletedAtUtc ?: 0L) in 1L..cutoffUtc
                    }
                    cleanupReferencesForDeletedNotesInternal(expiredNotes.map { it.id }, remindersToCancel)
                    expiredNotes.forEach { note ->
                        noteLinkDao.clearIncomingTargetNoteId(note.id)
                        bookmarkDao.removeBookmark("NOTE", note.id)
                        noteDao.deletePermanently(note.id)
                        purgedCount++
                    }

                    val expiredAtts = attachmentDao.getExpiredDeletedAttachmentsDirect(cutoffUtc)
                    expiredAtts.forEach { att ->
                        if (att.relativePath.isNotBlank()) pathsToCheck.add(att.relativePath)
                        attachmentDao.clearAnnotationsForAttachment(att.id)
                        bookmarkDao.removePdfBookmarksForAttachment(att.id)
                        attachmentDao.deleteAttachmentById(att.id)
                        purgedCount++
                    }
                    if (expiredNotes.isNotEmpty()) {
                        reconcileAllLinksInternal()
                    }
                }
                context?.let { ctx ->
                    remindersToCancel.distinct().forEach { remId ->
                        com.example.util.ReminderNotificationManager.cancelReminder(ctx, remId)
                    }
                }
                pathsToCheck.distinct().forEach { deletePhysicalFileIfUnreferenced(it) }
                if (purgedCount > 0) {
                    notifyWidgetsChanged()
                }
                Result.success(purgedCount)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun togglePin(note: NoteEntity): Result<Unit> {
        return try {
            noteDao.updateNote(note.copy(isPinned = !note.isPinned, updatedAtUtc = System.currentTimeMillis()))
            notifyWidgetsChanged()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleFavorite(note: NoteEntity): Result<Unit> {
        return try {
            noteDao.updateNote(note.copy(isFavorite = !note.isFavorite, updatedAtUtc = System.currentTimeMillis()))
            notifyWidgetsChanged()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleArchive(note: NoteEntity): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val updated = note.copy(isArchived = !note.isArchived, updatedAtUtc = System.currentTimeMillis())
                    noteDao.updateNote(updated)
                    syncKnowledgeForNoteInternal(updated)
                }
                notifyWidgetsChanged()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun toggleLock(note: NoteEntity): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val current = noteDao.getNoteByIdDirect(note.id) ?: note
                    val targetLocked = !current.isLocked
                    val updated = transformNoteEncryptionForLock(current, targetLocked)
                    noteDao.updateNote(updated)
                    syncKnowledgeForNoteInternal(updated)
                }
                notifyWidgetsChanged()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun toggleHide(note: NoteEntity): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val updated = note.copy(isHidden = !note.isHidden, updatedAtUtc = System.currentTimeMillis())
                    noteDao.updateNote(updated)
                    syncKnowledgeForNoteInternal(updated)
                }
                notifyWidgetsChanged()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun insertFolder(folder: FolderEntity): Result<String> {
        return try {
            folderDao.insertFolder(folder)
            notifyWidgetsChanged()
            Result.success(folder.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateFolder(folder: FolderEntity): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val allFolders = folderDao.getAllFoldersDirect()
                    if (FolderHierarchyUtils.wouldCreateCycle(allFolders, folder.id, folder.parentId)) {
                        throw IllegalArgumentException("Cannot move a folder inside itself or one of its subfolders")
                    }
                    folderDao.updateFolder(folder.copy(updatedAtUtc = System.currentTimeMillis()))
                    noteDao.updateNotesFolderName(folder.id, folder.name)
                }
                notifyWidgetsChanged()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun moveFolder(folderId: String, newParentId: String?): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val target = folderDao.getFolderByIdDirect(folderId)
                        ?: throw IllegalArgumentException("Folder not found")
                    val allFolders = folderDao.getAllFoldersDirect()
                    val cleanParentId = newParentId?.takeIf { it.isNotBlank() }
                    if (FolderHierarchyUtils.wouldCreateCycle(allFolders, folderId, cleanParentId)) {
                        throw IllegalArgumentException("Cannot move a folder inside itself or one of its subfolders")
                    }
                    folderDao.updateFolder(
                        target.copy(
                            parentId = cleanParentId,
                            updatedAtUtc = System.currentTimeMillis()
                        )
                    )
                }
                notifyWidgetsChanged()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteFolder(id: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val target = folderDao.getFolderByIdDirect(id)
                    folderDao.reparentChildFolders(
                        oldParentId = id,
                        newParentId = target?.parentId,
                        updatedAtUtc = System.currentTimeMillis()
                    )
                    noteDao.detachNotesFromFolder(id)
                    attachmentDao.detachAttachmentsFromFolder(id)
                    folderDao.deleteFolder(id)
                }
                notifyWidgetsChanged()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun moveNoteToFolder(noteId: String, folder: FolderEntity?): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                noteDao.moveNoteToFolder(
                    noteId = noteId,
                    targetFolderId = folder?.id,
                    targetFolderName = folder?.name ?: "",
                    updatedAtUtc = System.currentTimeMillis()
                )
                notifyWidgetsChanged()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun duplicateNote(note: NoteEntity): Result<String> {
        return try {
            val newId = UUID.randomUUID().toString()
            val duplicated = note.copy(
                id = newId,
                title = if (note.title.isNotBlank()) "${note.title} (Copy)" else "Untitled (Copy)",
                createdAtUtc = System.currentTimeMillis(),
                updatedAtUtc = System.currentTimeMillis()
            )
            noteDao.insertNote(duplicated)
            notifyWidgetsChanged()
            Result.success(newId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun insertTag(tag: TagEntity): Result<String> {
        return try {
            tagDao.insertTag(tag)
            Result.success(tag.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTag(id: String): Result<Unit> {
        return try {
            tagDao.deleteTag(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun insertStickyNote(sticky: StickyNoteEntity): Result<String> {
        return try {
            stickyNoteDao.insertStickyNote(sticky)
            notifyWidgetsChanged()
            Result.success(sticky.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateStickyNote(sticky: StickyNoteEntity): Result<Unit> {
        return try {
            stickyNoteDao.updateStickyNote(sticky)
            notifyWidgetsChanged()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteStickyNote(id: String): Result<Unit> {
        return try {
            stickyNoteDao.deleteStickyNoteById(id)
            notifyWidgetsChanged()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveCanvasWithElements(canvas: CanvasEntity, elements: List<CanvasElementEntity>): Result<String> {
        return try {
            canvasDao.saveCanvasWithElements(canvas, elements)
            notifyWidgetsChanged()
            Result.success(canvas.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCanvas(id: String): Result<Unit> {
        return try {
            canvasDao.deleteCanvasById(id)
            notifyWidgetsChanged()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getElementsForCanvas(canvasId: String): Flow<List<CanvasElementEntity>> {
        return canvasDao.getElementsForCanvas(canvasId)
    }

    override suspend fun getElementsForCanvasDirect(canvasId: String): List<CanvasElementEntity> {
        return canvasDao.getElementsForCanvasDirect(canvasId)
    }

    suspend fun getCanvasByIdDirect(id: String): CanvasEntity? {
        return canvasDao.getCanvasByIdDirect(id)
    }

    override suspend fun getSetting(key: String): String? {
        return appSettingsDao.getSetting(key)
    }

    override suspend fun setSetting(key: String, value: String) {
        appSettingsDao.setSetting(com.example.data.model.AppSettingEntity(key, value))
    }

    override suspend fun saveAttachment(attachment: AttachmentEntity): Result<String> {
        return try {
            attachmentDao.insertAttachment(attachment)
            notifyWidgetsChanged()
            Result.success(attachment.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun resolvePathKey(path: String): String {
        val trimmed = path.trim()
        if (trimmed.isBlank()) return ""
        val file = if (trimmed.startsWith("/") || context == null) {
            File(trimmed)
        } else {
            File(context.filesDir, trimmed)
        }
        return try {
            file.canonicalPath
        } catch (_: Exception) {
            file.absolutePath
        }
    }

    override suspend fun isFileReferenced(
        relativePath: String,
        ignoreAttachmentId: String?,
        ignoreNoteId: String?
    ): Boolean {
        val targetKey = resolvePathKey(relativePath)
        if (targetKey.isBlank()) return false
        val hasAttachmentRef = attachmentDao.getAllAttachmentsDirect().any { att ->
            att.id != ignoreAttachmentId && resolvePathKey(att.relativePath) == targetKey
        }
        if (hasAttachmentRef) return true

        val hasNoteRef = noteDao.getAllNotesDirect().any { note ->
            note.id != ignoreNoteId &&
                note.imageUri.isNotBlank() &&
                note.imageUri.split("|").any { part -> resolvePathKey(part) == targetKey }
        }
        return hasNoteRef
    }

    private suspend fun deletePhysicalFileIfUnreferenced(relativePath: String) {
        if (context == null || relativePath.isBlank()) return
        val stillReferenced = isFileReferenced(relativePath)
        if (!stillReferenced) {
            val file = if (relativePath.startsWith("/")) {
                File(relativePath)
            } else {
                File(context.filesDir, relativePath)
            }
            if (file.exists()) {
                file.delete()
            }
        }
    }

    override suspend fun deleteAttachment(id: String): Result<Unit> {
        return deleteAttachmentPermanently(id)
    }

    override suspend fun deleteAttachmentPermanently(id: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val attachment = attachmentDao.getAttachmentById(id)
                if (attachment != null) {
                    db.withTransaction {
                        attachmentDao.clearAnnotationsForAttachment(id)
                        bookmarkDao.removePdfBookmarksForAttachment(id)
                        attachmentDao.deleteAttachmentById(id)
                    }
                    deletePhysicalFileIfUnreferenced(attachment.relativePath)
                    notifyWidgetsChanged()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getAttachmentById(id: String): AttachmentEntity? {
        return attachmentDao.getAttachmentById(id)
    }

    override suspend fun getAttachmentByHash(contentHash: String): AttachmentEntity? {
        if (contentHash.isBlank()) return null
        return attachmentDao.getAttachmentByHash(contentHash)
    }

    override fun getAttachmentsByFolder(folderId: String): Flow<List<AttachmentEntity>> {
        return attachmentDao.getAttachmentsByFolder(folderId)
    }

    override fun getAttachmentsForNote(noteId: String): Flow<List<AttachmentEntity>> {
        return attachmentDao.getAttachmentsForNote(noteId)
    }

    override suspend fun renameAttachment(id: String, newName: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val existing = attachmentDao.getAttachmentById(id)
                    ?: return@withContext Result.failure(IllegalArgumentException("Attachment not found"))
                val sanitized = newName.trim()
                if (sanitized.isBlank()) return@withContext Result.failure(IllegalArgumentException("Name cannot be empty"))
                attachmentDao.updateAttachment(
                    existing.copy(
                        fileName = sanitized,
                        updatedAtUtc = System.currentTimeMillis()
                    )
                )
                notifyWidgetsChanged()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun moveAttachmentToFolder(attachmentId: String, targetFolderId: String?): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanFolderId = targetFolderId?.takeIf { it.isNotBlank() }
                attachmentDao.moveAttachmentToFolder(
                    attachmentId = attachmentId,
                    targetFolderId = cleanFolderId,
                    updatedAtUtc = System.currentTimeMillis()
                )
                notifyWidgetsChanged()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun toggleAttachmentFavorite(attachment: AttachmentEntity): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val latest = attachmentDao.getAttachmentById(attachment.id) ?: attachment
                attachmentDao.updateAttachment(
                    latest.copy(
                        isFavorite = !latest.isFavorite,
                        updatedAtUtc = System.currentTimeMillis()
                    )
                )
                notifyWidgetsChanged()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun toggleAttachmentArchive(attachment: AttachmentEntity): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val latest = attachmentDao.getAttachmentById(attachment.id) ?: attachment
                attachmentDao.updateAttachment(
                    latest.copy(
                        isArchived = !latest.isArchived,
                        updatedAtUtc = System.currentTimeMillis()
                    )
                )
                notifyWidgetsChanged()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun softDeleteAttachment(attachment: AttachmentEntity): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val latest = attachmentDao.getAttachmentById(attachment.id) ?: attachment
                val now = System.currentTimeMillis()
                val updated = latest.copy(
                    isDeleted = true,
                    deletedAtUtc = now,
                    updatedAtUtc = now
                )
                attachmentDao.updateAttachment(updated)
                deletedItemDao.insertDeletedItem(
                    DeletedItemEntity(
                        originalTable = "attachments",
                        originalId = latest.id,
                        itemSnapshotJson = org.json.JSONObject().apply {
                            put("fileName", latest.fileName)
                            put("mimeType", latest.mimeType)
                        }.toString(),
                        deletedAtUtc = now
                    )
                )
                notifyWidgetsChanged()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun restoreAttachment(attachment: AttachmentEntity): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val latest = attachmentDao.getAttachmentById(attachment.id) ?: attachment
                attachmentDao.updateAttachment(
                    latest.copy(
                        isDeleted = false,
                        deletedAtUtc = null,
                        updatedAtUtc = System.currentTimeMillis()
                    )
                )
                notifyWidgetsChanged()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun setAttachmentLocked(
        attachment: AttachmentEntity,
        locked: Boolean,
        storageManager: com.example.data.storage.AttachmentStorageManager
    ): Result<AttachmentEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val latest = attachmentDao.getAttachmentById(attachment.id) ?: attachment
                if (latest.isLocked == locked) return@withContext Result.success(latest)
                val now = System.currentTimeMillis()
                val oldRelPath = latest.relativePath
                val sourceFile = storageManager.getFileFromRelativePath(oldRelPath)
                if (!sourceFile.exists()) {
                    return@withContext Result.failure(java.io.FileNotFoundException("Document file not found on device"))
                }

                val updated = if (locked) {
                    val vaultRelPath = storageManager.encryptExistingFileToVault(oldRelPath).getOrThrow()
                    latest.copy(
                        relativePath = vaultRelPath,
                        isLocked = true,
                        updatedAtUtc = now
                    )
                } else {
                    val existingNonLockedMatch = if (latest.contentHash.isNotBlank()) {
                        attachmentDao.getAttachmentByHash(latest.contentHash)
                    } else null
                    val existingFile = existingNonLockedMatch?.let {
                        storageManager.getFileFromRelativePath(it.relativePath)
                    }
                    val plainRelPath = if (existingNonLockedMatch != null && existingFile?.exists() == true) {
                        existingNonLockedMatch.relativePath
                    } else {
                        storageManager.decryptVaultFileToPlain(
                            oldRelPath,
                            latest.fileExtension.ifBlank { "pdf" }
                        ).getOrThrow()
                    }
                    val plainFile = storageManager.getFileFromRelativePath(plainRelPath)
                    val hash = latest.contentHash.ifBlank { storageManager.computeSha256(plainFile) }
                    latest.copy(
                        relativePath = plainRelPath,
                        contentHash = hash,
                        isLocked = false,
                        updatedAtUtc = now
                    )
                }

                attachmentDao.updateAttachment(updated)
                if (oldRelPath != updated.relativePath && !isFileReferenced(oldRelPath, ignoreAttachmentId = updated.id)) {
                    deletePhysicalFileIfUnreferenced(oldRelPath)
                }
                notifyWidgetsChanged()
                Result.success(updated)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun getAnnotationsForAttachment(attachmentId: String): Flow<List<AnnotationEntity>> {
        return attachmentDao.getAnnotationsForAttachment(attachmentId)
    }

    override suspend fun getAnnotationsForAttachmentDirect(attachmentId: String): List<AnnotationEntity> {
        return attachmentDao.getAnnotationsForAttachmentDirect(attachmentId)
    }

    override suspend fun saveAttachmentWithAnnotations(attachment: AttachmentEntity, annotations: List<AnnotationEntity>): Result<Unit> {
        return try {
            attachmentDao.saveAttachmentWithAnnotations(attachment, annotations)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveAnnotation(annotation: AnnotationEntity): Result<String> {
        return try {
            attachmentDao.insertAnnotation(annotation)
            Result.success(annotation.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAnnotation(annotation: AnnotationEntity): Result<Unit> {
        return try {
            attachmentDao.deleteAnnotation(annotation)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllBookmarks(): Flow<List<BookmarkEntity>> {
        return bookmarkDao.getAllBookmarks()
    }

    override suspend fun insertBookmark(bookmark: BookmarkEntity): Result<String> {
        return try {
            bookmarkDao.insertBookmark(bookmark)
            Result.success(bookmark.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateBookmark(bookmark: BookmarkEntity): Result<Unit> {
        return try {
            bookmarkDao.updateBookmark(bookmark)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteBookmark(bookmark: BookmarkEntity): Result<Unit> {
        return try {
            bookmarkDao.deleteBookmark(bookmark)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeBookmark(targetType: String, targetId: String): Result<Unit> {
        return try {
            bookmarkDao.removeBookmark(targetType, targetId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveReminder(reminder: ReminderEntity): Result<String> {
        return try {
            reminderDao.insertReminder(reminder)
            notifyWidgetsChanged()
            Result.success(reminder.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateReminder(reminder: ReminderEntity): Result<Unit> {
        return try {
            reminderDao.updateReminder(reminder)
            notifyWidgetsChanged()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteReminder(id: String): Result<Unit> {
        return try {
            reminderDao.deleteReminderById(id)
            notifyWidgetsChanged()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getReminderById(id: String): ReminderEntity? {
        return reminderDao.getReminderById(id)
    }

    // ==========================================
    // PHASE 1: KNOWLEDGE BRAIN (LINKS, TEMPLATES, VERSIONS)
    // ==========================================
    override fun getBacklinksForNote(noteId: String): Flow<List<NoteLinkEntity>> =
        noteLinkDao.getBacklinksForNote(noteId)

    override fun getOutgoingLinksForNote(noteId: String): Flow<List<NoteLinkEntity>> =
        noteLinkDao.getOutgoingLinksForNote(noteId)

    override fun getVersionsForNote(noteId: String): Flow<List<NoteVersionEntity>> =
        noteVersionDao.getVersionsForNote(noteId)

    suspend fun getVersionsForNoteDirect(noteId: String): List<NoteVersionEntity> =
        noteVersionDao.getVersionsForNoteDirect(noteId)

    override suspend fun ensureBuiltInTemplatesSeeded() {
        withContext(Dispatchers.IO) {
            noteTemplateDao.insertTemplates(KnowledgeBrainEngine.builtInTemplates())
        }
    }

    override suspend fun saveTemplate(template: NoteTemplateEntity): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanName = template.name.trim().ifBlank { "Untitled Template" }
                val effectiveId = template.id.ifBlank { UUID.randomUUID().toString() }
                val now = System.currentTimeMillis()
                val existing = noteTemplateDao.getTemplateByIdDirect(effectiveId)
                    ?: noteTemplateDao.getTemplateByNameDirect(cleanName)
                if (existing != null) {
                    val updated = template.copy(
                        id = existing.id,
                        name = cleanName,
                        isBuiltIn = existing.isBuiltIn,
                        createdAtUtc = existing.createdAtUtc,
                        updatedAtUtc = now
                    )
                    noteTemplateDao.updateTemplate(updated)
                    Result.success(updated.id)
                } else {
                    val inserted = template.copy(
                        id = effectiveId,
                        name = cleanName,
                        createdAtUtc = now,
                        updatedAtUtc = now
                    )
                    noteTemplateDao.insertTemplate(inserted)
                    Result.success(inserted.id)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteTemplate(id: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val existing = noteTemplateDao.getTemplateByIdDirect(id)
                if (existing != null && existing.isBuiltIn) {
                    return@withContext Result.failure(IllegalArgumentException("Built-in templates cannot be deleted."))
                }
                noteTemplateDao.deleteTemplate(id)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun incrementTemplateUsage(templateId: String) {
        withContext(Dispatchers.IO) {
            noteTemplateDao.incrementUsageCount(templateId)
        }
    }

    override suspend fun restoreNoteVersion(noteId: String, versionId: String): Result<NoteEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val restored = db.withTransaction {
                    val current = noteDao.getNoteByIdDirect(noteId)
                        ?: throw IllegalArgumentException("Note not found")
                    if (current.isLocked) {
                        throw SecurityException("Locked notes do not expose plaintext version history.")
                    }
                    val targetVersion = noteVersionDao.getVersionByIdDirect(versionId)
                        ?: throw IllegalArgumentException("Version snapshot not found")
                    if (targetVersion.noteId != noteId) {
                        throw IllegalArgumentException("Version snapshot belongs to a different note")
                    }

                    // Preserve current unsaved/latest edit before restoring
                    captureVersionSnapshotIfMeaningful(
                        current,
                        "Auto-backup before restoring v${targetVersion.versionNumber}"
                    )

                    val restoredNote = current.copy(
                        title = targetVersion.title,
                        content = targetVersion.content,
                        tagsCsv = targetVersion.tagsCsv,
                        checklistJson = targetVersion.checklistJson,
                        updatedAtUtc = System.currentTimeMillis()
                    )
                    noteDao.updateNote(restoredNote)
                    syncKnowledgeForNoteInternal(restoredNote)
                    captureVersionSnapshotIfMeaningful(
                        restoredNote,
                        "Restored from v${targetVersion.versionNumber}"
                    )
                    restoredNote
                }
                notifyWidgetsChanged()
                Result.success(restored)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun reconcileAllLinksInternal() {
        val allNotes = noteDao.getAllNotesDirect()
        val eligibleNotes = allNotes.filter { KnowledgeBrainEngine.isNoteKnowledgeEligible(it) }
        noteLinkDao.clearAll()
        val rebuiltLinks = mutableListOf<NoteLinkEntity>()
        for (source in eligibleNotes) {
            rebuiltLinks.addAll(
                KnowledgeBrainEngine.buildOutgoingLinkEntities(
                    sourceNote = source,
                    eligibleNotes = eligibleNotes
                )
            )
        }
        if (rebuiltLinks.isNotEmpty()) {
            noteLinkDao.insertLinks(rebuiltLinks)
        }
    }

    override suspend fun reconcileAllKnowledgeLinks() {
        withContext(Dispatchers.IO) {
            db.withTransaction {
                reconcileAllLinksInternal()
            }
        }
    }

    suspend fun getAllNotesDirect(): List<NoteEntity> = noteDao.getAllNotesDirect()
    suspend fun getAllFoldersDirect(): List<FolderEntity> = folderDao.getAllFoldersDirect()
    suspend fun getAllTagsDirect(): List<TagEntity> = tagDao.getAllTagsDirect()
    suspend fun getAllStickyNotesDirect(): List<StickyNoteEntity> = stickyNoteDao.getAllStickyNotesDirect()
    suspend fun getAllCanvasesDirect(): List<CanvasEntity> = canvasDao.getAllCanvasesDirect()
    suspend fun getAllCanvasElementsDirect(): List<CanvasElementEntity> = canvasDao.getAllElementsDirect()
    suspend fun getAllAttachmentsDirect(): List<AttachmentEntity> = attachmentDao.getAllAttachmentsDirect()
    suspend fun getAllAnnotationsDirect(): List<AnnotationEntity> = attachmentDao.getAllAnnotationsDirect()
    suspend fun getAllBookmarksDirect(): List<BookmarkEntity> = bookmarkDao.getAllBookmarksDirect()
    suspend fun getAllRemindersDirect(): List<ReminderEntity> = reminderDao.getAllRemindersDirect()
    suspend fun getAllNoteTagsDirect(): List<NoteTagEntity> = noteDao.getAllNoteTagsDirect()
    suspend fun getAllNoteLinksDirect(): List<NoteLinkEntity> = noteLinkDao.getAllLinksDirect()
    suspend fun getAllNoteTemplatesDirect(): List<NoteTemplateEntity> = noteTemplateDao.getAllTemplatesDirect()
    suspend fun getAllNoteVersionsDirect(): List<NoteVersionEntity> = noteVersionDao.getAllVersionsDirect()
    suspend fun getAllKanbanColumnsDirect(): List<KanbanColumnEntity> = productivityDao.getAllKanbanColumnsDirect()
    suspend fun getAllTasksDirect(): List<TaskEntity> = productivityDao.getAllTasksDirect()
    suspend fun getTaskByIdDirect(id: String): TaskEntity? = productivityDao.getTaskByIdDirect(id)
    suspend fun getTaskByLinkedReminderIdDirect(reminderId: String): TaskEntity? = productivityDao.getTaskByLinkedReminderIdDirect(reminderId)
    suspend fun getAllTaskDependenciesDirect(): List<TaskDependencyEntity> = productivityDao.getAllTaskDependenciesDirect()
    suspend fun getAllHabitsDirect(): List<HabitEntity> = productivityDao.getAllHabitsDirect()
    suspend fun getAllHabitLogsDirect(): List<HabitLogEntity> = productivityDao.getAllHabitLogsDirect()
    suspend fun getAllFocusSessionsDirect(): List<FocusSessionEntity> = productivityDao.getAllFocusSessionsDirect()

    // ==========================================
    // PHASE 2: PRODUCTIVITY WORKSPACE
    // ==========================================
    override suspend fun ensureDefaultKanbanColumnsSeeded() {
        withContext(Dispatchers.IO) {
            if (productivityDao.getAllKanbanColumnsDirect().isEmpty()) {
                productivityDao.insertKanbanColumnsIgnore(ProductivityEngine.defaultKanbanColumns())
            }
        }
    }

    override suspend fun saveKanbanColumn(column: KanbanColumnEntity): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val cleanTitle = column.title.trim().ifBlank { "Column" }
                val effectiveId = column.id.ifBlank { UUID.randomUUID().toString() }
                val existing = productivityDao.getKanbanColumnByIdDirect(effectiveId)
                if (existing != null) {
                    productivityDao.updateKanbanColumn(
                        column.copy(
                            id = existing.id,
                            title = cleanTitle,
                            createdAtUtc = existing.createdAtUtc,
                            updatedAtUtc = now
                        )
                    )
                } else {
                    val currentCols = productivityDao.getAllKanbanColumnsDirect()
                    val nextPos = if (column.position > 0) column.position else (currentCols.maxOfOrNull { it.position }?.plus(1) ?: 0)
                    productivityDao.insertKanbanColumn(
                        column.copy(
                            id = effectiveId,
                            title = cleanTitle,
                            position = nextPos,
                            createdAtUtc = now,
                            updatedAtUtc = now
                        )
                    )
                }
                Result.success(effectiveId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun reorderKanbanColumns(orderedIds: List<String>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val now = System.currentTimeMillis()
                    val existing = productivityDao.getAllKanbanColumnsDirect().associateBy { it.id }
                    orderedIds.forEachIndexed { idx, colId ->
                        existing[colId]?.let { col ->
                            if (col.position != idx) {
                                productivityDao.updateKanbanColumn(col.copy(position = idx, updatedAtUtc = now))
                            }
                        }
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteKanbanColumn(columnId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val cols = productivityDao.getAllKanbanColumnsDirect()
                    if (cols.size <= 1) {
                        throw IllegalStateException("At least one Kanban column must remain.")
                    }
                    val targetFallback = cols.firstOrNull { it.id != columnId && !it.isDoneColumn }
                        ?: cols.first { it.id != columnId }
                    val now = System.currentTimeMillis()
                    productivityDao.reassignTasksToColumn(columnId, targetFallback.id, now)
                    productivityDao.deleteKanbanColumnById(columnId)
                    // Normalize remaining positions
                    productivityDao.getAllKanbanColumnsDirect().forEachIndexed { idx, col ->
                        if (col.position != idx) {
                            productivityDao.updateKanbanColumn(col.copy(position = idx, updatedAtUtc = now))
                        }
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun saveTask(task: TaskEntity): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val effectiveId = task.id.ifBlank { UUID.randomUUID().toString() }
                db.withTransaction {
                    if (productivityDao.getAllKanbanColumnsDirect().isEmpty()) {
                        productivityDao.insertKanbanColumnsIgnore(ProductivityEngine.defaultKanbanColumns(now))
                    }
                    val columns = productivityDao.getAllKanbanColumnsDirect()
                    val validColumnId = if (columns.any { it.id == task.columnId }) {
                        task.columnId
                    } else {
                        columns.firstOrNull { !it.isDoneColumn }?.id ?: columns.first().id
                    }
                    val existing = productivityDao.getTaskByIdDirect(effectiveId)
                    val safeLinkedReminderId = task.linkedReminderId?.takeIf { it.isNotBlank() }
                    val safeLinkedNoteId = task.linkedNoteId?.takeIf { it.isNotBlank() }
                    val colIsDone = columns.firstOrNull { it.id == validColumnId }?.isDoneColumn == true
                    val finalCompleted = if (existing == null && colIsDone) true else task.isCompleted
                    val finalCompletedAt = if (finalCompleted) (task.completedAtUtc ?: now) else null
                    if (existing != null) {
                        val updated = task.copy(
                            id = existing.id,
                            title = task.title.trim().ifBlank { "Untitled Task" },
                            columnId = validColumnId,
                            isCompleted = finalCompleted,
                            completedAtUtc = finalCompletedAt,
                            linkedNoteId = safeLinkedNoteId,
                            linkedReminderId = safeLinkedReminderId,
                            createdAtUtc = existing.createdAtUtc,
                            updatedAtUtc = now
                        )
                        productivityDao.updateTask(updated)
                        syncLinkedReminderInternal(updated)
                    } else {
                        val inCol = productivityDao.getTasksInColumnDirect(validColumnId)
                        val order = if (task.orderInColumn > 0 || inCol.isEmpty()) task.orderInColumn else (inCol.maxOf { it.orderInColumn } + 1)
                        val inserted = task.copy(
                            id = effectiveId,
                            title = task.title.trim().ifBlank { "Untitled Task" },
                            columnId = validColumnId,
                            orderInColumn = order,
                            isCompleted = finalCompleted,
                            completedAtUtc = finalCompletedAt,
                            linkedNoteId = safeLinkedNoteId,
                            linkedReminderId = safeLinkedReminderId,
                            createdAtUtc = if (task.createdAtUtc > 0L) task.createdAtUtc else now,
                            updatedAtUtc = now
                        )
                        productivityDao.insertTask(inserted)
                        syncLinkedReminderInternal(inserted)
                    }
                }
                notifyWidgetsChanged()
                Result.success(effectiveId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun syncLinkedReminderInternal(task: TaskEntity) {
        val remId = task.linkedReminderId ?: return
        val existingRem = reminderDao.getReminderById(remId) ?: return
        val newScheduled = task.scheduledStartUtc ?: task.dueAtUtc ?: existingRem.scheduledTimeUtc
        val newRepeat = task.recurrenceRule.takeIf { it != TaskRecurrenceRule.NONE.name }
        if (existingRem.isCompleted != task.isCompleted ||
            existingRem.scheduledTimeUtc != newScheduled ||
            existingRem.repeatInterval != newRepeat
        ) {
            reminderDao.updateReminder(
                existingRem.copy(
                    isCompleted = task.isCompleted,
                    scheduledTimeUtc = newScheduled,
                    repeatInterval = newRepeat
                )
            )
        }
    }

    private suspend fun spawnNextRecurringOccurrenceIfNeededInternal(completedTask: TaskEntity, columns: List<KanbanColumnEntity>) {
        val rule = TaskRecurrenceRule.fromName(completedTask.recurrenceRule)
        if (rule == TaskRecurrenceRule.NONE) return
        val baseDue = completedTask.dueAtUtc ?: completedTask.scheduledStartUtc ?: System.currentTimeMillis()
        val nextDue = ProductivityEngine.calculateNextOccurrenceUtc(
            currentUtc = baseDue,
            rule = rule,
            interval = completedTask.recurrenceInterval,
            daysCsv = completedTask.recurrenceDaysCsv,
            timeZoneId = completedTask.dueTimeZoneId
        ) ?: return

        val parentKey = completedTask.recurrenceParentId ?: completedTask.id
        // Prevent duplicate next occurrences if task is toggled repeatedly
        val existingNext = productivityDao.getAllTasksDirect().firstOrNull { candidate ->
            !candidate.isCompleted &&
                !candidate.isArchived &&
                (candidate.recurrenceParentId == parentKey || candidate.id == parentKey) &&
                candidate.dueAtUtc == nextDue
        }
        if (existingNext != null) return

        val todoColId = columns.firstOrNull { !it.isDoneColumn }?.id ?: columns.firstOrNull()?.id ?: "col_todo"
        val deltaMs = nextDue - baseDue
        val nextSchedStart = completedTask.scheduledStartUtc?.let { it + deltaMs }
        val nextSchedEnd = completedTask.scheduledEndUtc?.let { it + deltaMs }
        val resetSubtasks = SmartNoteUtils.encodeChecklistJson(
            SmartNoteUtils.parseChecklistJson(completedTask.subtasksJson).map { it.copy(isDone = false) }
        )
        val now = System.currentTimeMillis()
        productivityDao.insertTask(
            completedTask.copy(
                id = UUID.randomUUID().toString(),
                columnId = todoColId,
                orderInColumn = 0,
                isCompleted = false,
                completedAtUtc = null,
                dueAtUtc = nextDue,
                scheduledStartUtc = nextSchedStart,
                scheduledEndUtc = nextSchedEnd,
                recurrenceParentId = parentKey,
                subtasksJson = resetSubtasks,
                linkedReminderId = null,
                createdAtUtc = now,
                updatedAtUtc = now
            )
        )
    }

    override suspend fun moveTaskToColumn(taskId: String, targetColumnId: String, targetOrder: Int?): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val now = System.currentTimeMillis()
                    val task = productivityDao.getTaskByIdDirect(taskId)
                        ?: throw IllegalArgumentException("Task not found")
                    val columns = productivityDao.getAllKanbanColumnsDirect()
                    val targetCol = columns.firstOrNull { it.id == targetColumnId }
                        ?: throw IllegalArgumentException("Target column not found")

                    val targetTasks = productivityDao.getTasksInColumnDirect(targetColumnId)
                        .filter { it.id != taskId }
                        .toMutableList()
                    val insertIdx = (targetOrder ?: targetTasks.size).coerceIn(0, targetTasks.size)
                    val markDone = targetCol.isDoneColumn
                    val movedTask = task.copy(
                        columnId = targetColumnId,
                        orderInColumn = insertIdx,
                        isCompleted = markDone,
                        completedAtUtc = if (markDone) (task.completedAtUtc ?: now) else null,
                        updatedAtUtc = now
                    )
                    targetTasks.add(insertIdx, movedTask)
                    targetTasks.forEachIndexed { idx, item ->
                        productivityDao.updateTask(
                            if (item.id == taskId) movedTask.copy(orderInColumn = idx)
                            else item.copy(orderInColumn = idx)
                        )
                    }
                    syncLinkedReminderInternal(movedTask)
                    if (!task.isCompleted && movedTask.isCompleted) {
                        spawnNextRecurringOccurrenceIfNeededInternal(movedTask, columns)
                    }
                }
                notifyWidgetsChanged()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun reorderTaskInColumn(taskId: String, delta: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val task = productivityDao.getTaskByIdDirect(taskId) ?: return@withTransaction
                    val colTasks = productivityDao.getTasksInColumnDirect(task.columnId).toMutableList()
                    val idx = colTasks.indexOfFirst { it.id == taskId }
                    if (idx == -1) return@withTransaction
                    val newIdx = (idx + delta).coerceIn(0, colTasks.lastIndex)
                    if (newIdx == idx) return@withTransaction
                    val item = colTasks.removeAt(idx)
                    colTasks.add(newIdx, item)
                    val now = System.currentTimeMillis()
                    colTasks.forEachIndexed { orderIdx, t ->
                        if (t.orderInColumn != orderIdx || t.id == taskId) {
                            productivityDao.updateTask(t.copy(orderInColumn = orderIdx, updatedAtUtc = now))
                        }
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun toggleTaskCompleted(taskId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val now = System.currentTimeMillis()
                    val task = productivityDao.getTaskByIdDirect(taskId)
                        ?: throw IllegalArgumentException("Task not found")
                    val columns = productivityDao.getAllKanbanColumnsDirect()
                    val newDone = !task.isCompleted
                    val doneCol = columns.firstOrNull { it.isDoneColumn }
                    val todoCol = columns.firstOrNull { !it.isDoneColumn }
                    val newColumnId = when {
                        newDone && doneCol != null -> doneCol.id
                        !newDone && columns.firstOrNull { it.id == task.columnId }?.isDoneColumn == true && todoCol != null -> todoCol.id
                        else -> task.columnId
                    }
                    val updated = task.copy(
                        isCompleted = newDone,
                        completedAtUtc = if (newDone) now else null,
                        columnId = newColumnId,
                        updatedAtUtc = now
                    )
                    productivityDao.updateTask(updated)
                    syncLinkedReminderInternal(updated)
                    if (!task.isCompleted && newDone) {
                        spawnNextRecurringOccurrenceIfNeededInternal(updated, columns)
                    }
                }
                notifyWidgetsChanged()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                productivityDao.deleteTaskById(taskId)
                notifyWidgetsChanged()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun addTaskDependency(taskId: String, dependsOnTaskId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val allDeps = productivityDao.getAllTaskDependenciesDirect()
                    if (ProductivityEngine.wouldCreateDependencyCycle(taskId, dependsOnTaskId, allDeps)) {
                        throw IllegalArgumentException("Cannot add dependency: circular dependency detected.")
                    }
                    if (productivityDao.getTaskByIdDirect(taskId) == null || productivityDao.getTaskByIdDirect(dependsOnTaskId) == null) {
                        throw IllegalArgumentException("Task or prerequisite task does not exist.")
                    }
                    productivityDao.insertTaskDependency(
                        TaskDependencyEntity(
                            taskId = taskId,
                            dependsOnTaskId = dependsOnTaskId,
                            createdAtUtc = System.currentTimeMillis()
                        )
                    )
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun removeTaskDependency(taskId: String, dependsOnTaskId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                productivityDao.deleteTaskDependency(taskId, dependsOnTaskId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun assignTaskTimeBlock(taskId: String, startUtc: Long?, endUtc: Long?): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val task = productivityDao.getTaskByIdDirect(taskId)
                        ?: throw IllegalArgumentException("Task not found")
                    val safeEnd = if (startUtc != null) {
                        val minEnd = startUtc + 15 * 60_000L
                        (endUtc ?: (startUtc + task.estimatedMinutes * 60_000L)).coerceAtLeast(minEnd)
                    } else {
                        null
                    }
                    val updated = task.copy(
                        scheduledStartUtc = startUtc,
                        scheduledEndUtc = safeEnd,
                        dueAtUtc = task.dueAtUtc ?: startUtc,
                        updatedAtUtc = System.currentTimeMillis()
                    )
                    productivityDao.updateTask(updated)
                    syncLinkedReminderInternal(updated)
                }
                notifyWidgetsChanged()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun saveHabit(habit: HabitEntity): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val effectiveId = habit.id.ifBlank { UUID.randomUUID().toString() }
                val existing = productivityDao.getHabitByIdDirect(effectiveId)
                val cleanTitle = habit.title.trim().ifBlank { "Daily Habit" }
                if (existing != null) {
                    productivityDao.updateHabit(
                        habit.copy(
                            id = existing.id,
                            title = cleanTitle,
                            targetPerDay = habit.targetPerDay.coerceAtLeast(1),
                            targetDaysPerWeek = habit.targetDaysPerWeek.coerceIn(1, 7),
                            createdAtUtc = existing.createdAtUtc,
                            updatedAtUtc = now
                        )
                    )
                } else {
                    productivityDao.insertHabit(
                        habit.copy(
                            id = effectiveId,
                            title = cleanTitle,
                            targetPerDay = habit.targetPerDay.coerceAtLeast(1),
                            targetDaysPerWeek = habit.targetDaysPerWeek.coerceIn(1, 7),
                            createdAtUtc = if (habit.createdAtUtc > 0L) habit.createdAtUtc else now,
                            updatedAtUtc = now
                        )
                    )
                }
                Result.success(effectiveId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteHabit(habitId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                productivityDao.deleteHabitById(habitId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun checkInHabit(
        habitId: String,
        dateKey: String,
        increment: Int,
        timeZoneId: String?
    ): Result<HabitLogEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val savedLog = db.withTransaction {
                    val habit = productivityDao.getHabitByIdDirect(habitId)
                        ?: throw IllegalArgumentException("Habit not found")
                    val target = habit.targetPerDay.coerceAtLeast(1)
                    val tzId = timeZoneId?.takeIf { it.isNotBlank() } ?: habit.timeZoneId
                    val existing = productivityDao.getHabitLogForDateDirect(habitId, dateKey)
                    val now = System.currentTimeMillis()
                    if (existing == null) {
                        val newCount = increment.coerceIn(1, target)
                        val log = HabitLogEntity(
                            id = UUID.randomUUID().toString(),
                            habitId = habitId,
                            localDateKey = dateKey,
                            completedCount = newCount,
                            targetCountSnapshot = target,
                            isCompleted = newCount >= target,
                            timeZoneId = tzId,
                            completedAtUtc = now
                        )
                        productivityDao.insertHabitLog(log)
                        log
                    } else {
                        val nextCount = if (existing.isCompleted && increment > 0) {
                            0
                        } else {
                            (existing.completedCount + increment).coerceIn(0, target)
                        }
                        val updated = existing.copy(
                            completedCount = nextCount,
                            targetCountSnapshot = target,
                            isCompleted = nextCount >= target,
                            timeZoneId = tzId,
                            completedAtUtc = now
                        )
                        productivityDao.updateHabitLog(updated)
                        updated
                    }
                }
                Result.success(savedLog)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun setHabitCompletedForDate(
        habitId: String,
        dateKey: String,
        completed: Boolean,
        timeZoneId: String?
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val habit = productivityDao.getHabitByIdDirect(habitId)
                        ?: throw IllegalArgumentException("Habit not found")
                    val target = habit.targetPerDay.coerceAtLeast(1)
                    val tzId = timeZoneId?.takeIf { it.isNotBlank() } ?: habit.timeZoneId
                    val existing = productivityDao.getHabitLogForDateDirect(habitId, dateKey)
                    val now = System.currentTimeMillis()
                    if (!completed && existing == null) {
                        return@withTransaction
                    }
                    val count = if (completed) target else 0
                    if (existing == null) {
                        productivityDao.insertHabitLog(
                            HabitLogEntity(
                                id = UUID.randomUUID().toString(),
                                habitId = habitId,
                                localDateKey = dateKey,
                                completedCount = count,
                                targetCountSnapshot = target,
                                isCompleted = completed,
                                timeZoneId = tzId,
                                completedAtUtc = now
                            )
                        )
                    } else {
                        productivityDao.updateHabitLog(
                            existing.copy(
                                completedCount = count,
                                targetCountSnapshot = target,
                                isCompleted = completed,
                                timeZoneId = tzId,
                                completedAtUtc = now
                            )
                        )
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun saveFocusSession(session: FocusSessionEntity): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val effectiveId = session.id.ifBlank { UUID.randomUUID().toString() }
                val dateKey = session.localDateKey.ifBlank {
                    ProductivityEngine.localDateKeyFromMillis(session.startedAtUtc, session.timeZoneId)
                }
                val safeTaskId = session.linkedTaskId?.takeIf { it.isNotBlank() }
                val safeNoteId = session.linkedNoteId?.takeIf { it.isNotBlank() }
                productivityDao.insertFocusSession(
                    session.copy(
                        id = effectiveId,
                        linkedTaskId = safeTaskId,
                        linkedNoteId = safeNoteId,
                        localDateKey = dateKey
                    )
                )
                Result.success(effectiveId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun syncReminderCompletionFromReminder(reminderId: String, isCompleted: Boolean) {
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val linkedTask = productivityDao.getTaskByLinkedReminderIdDirect(reminderId) ?: return@withTransaction
                if (linkedTask.isCompleted != isCompleted) {
                    val now = System.currentTimeMillis()
                    val cols = productivityDao.getAllKanbanColumnsDirect()
                    val doneCol = cols.firstOrNull { it.isDoneColumn }?.id
                    val todoCol = cols.firstOrNull { !it.isDoneColumn }?.id
                    val targetCol = when {
                        isCompleted && doneCol != null -> doneCol
                        !isCompleted && cols.firstOrNull { it.id == linkedTask.columnId }?.isDoneColumn == true && todoCol != null -> todoCol
                        else -> linkedTask.columnId
                    }
                    productivityDao.updateTask(
                        linkedTask.copy(
                            isCompleted = isCompleted,
                            completedAtUtc = if (isCompleted) now else null,
                            columnId = targetCol,
                            updatedAtUtc = now
                        )
                    )
                }
            }
        }
    }

    suspend fun insertNotes(notes: List<NoteEntity>) { noteDao.insertNotes(notes); notifyWidgetsChanged() }
    suspend fun insertFolders(folders: List<FolderEntity>) { folderDao.insertFolders(folders); notifyWidgetsChanged() }
    suspend fun insertTags(tags: List<TagEntity>) = tagDao.insertTags(tags)
    suspend fun insertNoteTags(noteTags: List<NoteTagEntity>) = noteDao.insertNoteTags(noteTags)
    suspend fun insertStickyNotes(stickyNotes: List<StickyNoteEntity>) { stickyNoteDao.insertStickyNotes(stickyNotes); notifyWidgetsChanged() }
    suspend fun insertCanvases(canvases: List<CanvasEntity>) { canvases.forEach { canvasDao.insertCanvas(it) }; notifyWidgetsChanged() }
    suspend fun insertCanvasElements(elements: List<CanvasElementEntity>) = canvasDao.insertElements(elements)
    suspend fun insertAttachments(attachments: List<AttachmentEntity>) { attachments.forEach { attachmentDao.insertAttachment(it) }; notifyWidgetsChanged() }
    suspend fun insertAnnotations(annotations: List<AnnotationEntity>) = attachmentDao.insertAnnotations(annotations)
    suspend fun insertBookmarks(bookmarks: List<BookmarkEntity>) = bookmarks.forEach { bookmarkDao.insertBookmark(it) }
    suspend fun insertReminders(reminders: List<ReminderEntity>) { reminders.forEach { reminderDao.insertReminder(it) }; notifyWidgetsChanged() }

    /**
     * Non-cascading safe upserts (Update if existing, Insert if new) so SQLite
     * ON DELETE CASCADE is never triggered by OnConflictStrategy.REPLACE during MERGE restores.
     */
    suspend fun upsertFoldersSafe(folders: List<FolderEntity>) {
        folders.forEach { folder ->
            if (folderDao.getFolderByIdDirect(folder.id) != null) {
                folderDao.updateFolder(folder)
            } else {
                folderDao.insertFolder(folder)
            }
        }
        notifyWidgetsChanged()
    }

    suspend fun upsertTagsSafe(tags: List<TagEntity>): Map<String, String> {
        val idRemap = mutableMapOf<String, String>()
        tags.forEach { tag ->
            val byId = tagDao.getTagByIdDirect(tag.id)
            val byName = tagDao.getTagByNameDirect(tag.name)
            when {
                byId != null -> {
                    if (byName == null || byName.id == byId.id) {
                        tagDao.updateTag(tag)
                    } else {
                        idRemap[tag.id] = byName.id
                    }
                }
                byName != null -> {
                    idRemap[tag.id] = byName.id
                    tagDao.updateTag(byName.copy(colorHex = tag.colorHex))
                }
                else -> {
                    tagDao.insertTag(tag)
                }
            }
        }
        return idRemap
    }

    suspend fun upsertNotesSafe(notes: List<NoteEntity>) {
        notes.forEach { note ->
            if (noteDao.getNoteByIdDirect(note.id) != null) {
                noteDao.updateNote(note)
            } else {
                noteDao.insertNote(note)
            }
        }
        notifyWidgetsChanged()
    }

    suspend fun upsertStickyNotesSafe(stickyNotes: List<StickyNoteEntity>) {
        stickyNotes.forEach { sticky ->
            if (stickyNoteDao.getStickyNoteById(sticky.id) != null) {
                stickyNoteDao.updateStickyNote(sticky)
            } else {
                stickyNoteDao.insertStickyNote(sticky)
            }
        }
        notifyWidgetsChanged()
    }

    suspend fun upsertCanvasesSafe(canvases: List<CanvasEntity>) {
        canvases.forEach { canvas ->
            if (canvasDao.getCanvasByIdDirect(canvas.id) != null) {
                canvasDao.updateCanvas(canvas)
            } else {
                canvasDao.insertCanvas(canvas)
            }
        }
        notifyWidgetsChanged()
    }

    suspend fun upsertCanvasElementsSafe(elements: List<CanvasElementEntity>) {
        elements.forEach { element ->
            if (canvasDao.getElementByIdDirect(element.id) != null) {
                canvasDao.updateElement(element)
            } else {
                canvasDao.insertElement(element)
            }
        }
    }

    suspend fun upsertAttachmentsSafe(attachments: List<AttachmentEntity>) {
        attachments.forEach { att ->
            if (attachmentDao.getAttachmentById(att.id) != null) {
                attachmentDao.updateAttachment(att)
            } else {
                attachmentDao.insertAttachment(att)
            }
        }
        notifyWidgetsChanged()
    }

    suspend fun upsertAnnotationsSafe(annotations: List<AnnotationEntity>) {
        annotations.forEach { ann ->
            if (attachmentDao.getAnnotationByIdDirect(ann.id) != null) {
                attachmentDao.updateAnnotation(ann)
            } else {
                attachmentDao.insertAnnotation(ann)
            }
        }
    }

    suspend fun upsertBookmarksSafe(bookmarks: List<BookmarkEntity>) {
        bookmarks.forEach { bookmark ->
            if (bookmarkDao.getBookmarkByIdDirect(bookmark.id) != null) {
                bookmarkDao.updateBookmark(bookmark)
            } else {
                bookmarkDao.insertBookmark(bookmark)
            }
        }
    }

    suspend fun upsertRemindersSafe(reminders: List<ReminderEntity>) {
        reminders.forEach { reminder ->
            if (reminderDao.getReminderById(reminder.id) != null) {
                reminderDao.updateReminder(reminder)
            } else {
                reminderDao.insertReminder(reminder)
            }
        }
        notifyWidgetsChanged()
    }

    suspend fun upsertNoteLinksSafe(links: List<NoteLinkEntity>) {
        if (links.isEmpty()) return
        val sourceIds = links.map { it.sourceNoteId }.distinct()
        sourceIds.forEach { sourceId ->
            val existingForSource = noteLinkDao.getOutgoingLinksDirect(sourceId)
            val incomingForSource = links.filter { it.sourceNoteId == sourceId }
            incomingForSource.forEach { incoming ->
                val match = existingForSource.firstOrNull {
                    it.id == incoming.id || it.normalizedTargetTitle == incoming.normalizedTargetTitle
                }
                if (match != null) {
                    noteLinkDao.updateLink(incoming.copy(id = match.id))
                } else {
                    noteLinkDao.insertLink(incoming)
                }
            }
        }
    }

    suspend fun upsertNoteTemplatesSafe(templates: List<NoteTemplateEntity>) {
        templates.forEach { template ->
            val byId = noteTemplateDao.getTemplateByIdDirect(template.id)
            val byName = noteTemplateDao.getTemplateByNameDirect(template.name)
            when {
                byId != null -> noteTemplateDao.updateTemplate(template)
                byName != null -> noteTemplateDao.updateTemplate(template.copy(id = byName.id))
                else -> noteTemplateDao.insertTemplate(template)
            }
        }
    }

    suspend fun upsertNoteVersionsSafe(versions: List<NoteVersionEntity>) {
        versions.forEach { version ->
            if (noteVersionDao.getVersionByIdDirect(version.id) != null) {
                noteVersionDao.updateVersion(version)
            } else {
                noteVersionDao.insertVersion(version)
            }
        }
    }

    suspend fun upsertKanbanColumnsSafe(columns: List<KanbanColumnEntity>) {
        columns.forEach { col ->
            if (productivityDao.getKanbanColumnByIdDirect(col.id) != null) {
                productivityDao.updateKanbanColumn(col)
            } else {
                productivityDao.insertKanbanColumn(col)
            }
        }
    }

    suspend fun upsertTasksSafe(tasks: List<TaskEntity>) {
        tasks.forEach { task ->
            val existingById = productivityDao.getTaskByIdDirect(task.id)
            val existingByReminder = task.linkedReminderId?.let { productivityDao.getTaskByLinkedReminderIdDirect(it) }
            when {
                existingById != null -> productivityDao.updateTask(task)
                existingByReminder != null -> productivityDao.updateTask(task.copy(id = existingByReminder.id))
                else -> productivityDao.insertTask(task)
            }
        }
        notifyWidgetsChanged()
    }

    suspend fun upsertTaskDependenciesSafe(dependencies: List<TaskDependencyEntity>) {
        val allExisting = productivityDao.getAllTaskDependenciesDirect().toMutableList()
        dependencies.forEach { dep ->
            if (!ProductivityEngine.wouldCreateDependencyCycle(dep.taskId, dep.dependsOnTaskId, allExisting)) {
                productivityDao.insertTaskDependency(dep)
                allExisting.add(dep)
            }
        }
    }

    suspend fun upsertHabitsSafe(habits: List<HabitEntity>) {
        habits.forEach { habit ->
            if (productivityDao.getHabitByIdDirect(habit.id) != null) {
                productivityDao.updateHabit(habit)
            } else {
                productivityDao.insertHabit(habit)
            }
        }
    }

    suspend fun upsertHabitLogsSafe(logs: List<HabitLogEntity>) {
        logs.forEach { log ->
            val existing = productivityDao.getHabitLogForDateDirect(log.habitId, log.localDateKey)
            if (existing != null) {
                productivityDao.updateHabitLog(log.copy(id = existing.id))
            } else {
                productivityDao.insertHabitLog(log)
            }
        }
    }

    suspend fun upsertFocusSessionsSafe(sessions: List<FocusSessionEntity>) {
        sessions.forEach { session ->
            if (productivityDao.getFocusSessionByIdDirect(session.id) != null) {
                productivityDao.updateFocusSession(session)
            } else {
                productivityDao.insertFocusSession(session)
            }
        }
    }

    suspend fun clearAllData() {
        productivityDao.clearAllFocusSessions()
        productivityDao.clearAllHabitLogs()
        productivityDao.clearAllHabits()
        productivityDao.clearAllTaskDependencies()
        productivityDao.clearAllTasks()
        productivityDao.clearAllKanbanColumns()
        noteLinkDao.clearAll()
        noteVersionDao.clearAll()
        noteTemplateDao.clearAll()
        noteDao.clearAllNoteTags()
        noteDao.clearAllNotes()
        folderDao.clearAll()
        tagDao.clearAll()
        stickyNoteDao.clearAll()
        canvasDao.clearAllCanvases()
        canvasDao.clearAllElements()
        attachmentDao.clearAllAttachments()
        attachmentDao.clearAllAnnotations()
        bookmarkDao.clearAll()
        reminderDao.clearAll()
        deletedItemDao.purgeAllDeletedItems()
        notifyWidgetsChanged()
    }

    suspend fun <R> runInTransaction(block: suspend () -> R): R {
        return db.withTransaction {
            block()
        }
    }

    suspend fun getOrphanedAttachmentFiles(): List<File> {
        if (context == null) return emptyList()
        val attachmentsDir = File(context.filesDir, "attachments")
        if (!attachmentsDir.exists() || !attachmentsDir.isDirectory) return emptyList()
        val allPaths = attachmentDao.getAllAttachmentsDirect().map { it.relativePath }.toSet()
        val filesOnDisk = attachmentsDir.listFiles() ?: emptyArray()
        return filesOnDisk.filter { file ->
            val rel = "attachments/${file.name}"
            !allPaths.contains(rel)
        }
    }

    suspend fun cleanupOrphanedFiles(): Int {
        val orphans = getOrphanedAttachmentFiles()
        var count = 0
        orphans.forEach { if (it.delete()) count++ }
        return count
    }
}
