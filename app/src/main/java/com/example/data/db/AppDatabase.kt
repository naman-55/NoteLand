package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AnnotationEntity
import com.example.data.model.AppSettingEntity
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
import com.example.data.model.NoteType
import com.example.data.model.NoteVersionEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.StickyNoteEntity
import com.example.data.model.TagEntity
import com.example.data.model.TaskDependencyEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskRecurrenceRule
import com.example.util.KnowledgeBrainEngine
import com.example.util.ProductivityEngine
import com.example.util.SmartNoteUtils
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Database(
    entities = [
        NoteEntity::class,
        FolderEntity::class,
        TagEntity::class,
        NoteTagEntity::class,
        StickyNoteEntity::class,
        CanvasEntity::class,
        CanvasElementEntity::class,
        AttachmentEntity::class,
        AnnotationEntity::class,
        BookmarkEntity::class,
        ReminderEntity::class,
        AppSettingEntity::class,
        DeletedItemEntity::class,
        NoteLinkEntity::class,
        NoteTemplateEntity::class,
        NoteVersionEntity::class,
        KanbanColumnEntity::class,
        TaskEntity::class,
        TaskDependencyEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        FocusSessionEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun stickyNoteDao(): StickyNoteDao
    abstract fun canvasDao(): CanvasDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun reminderDao(): ReminderDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun deletedItemDao(): DeletedItemDao
    abstract fun noteLinkDao(): NoteLinkDao
    abstract fun noteTemplateDao(): NoteTemplateDao
    abstract fun noteVersionDao(): NoteVersionDao
    abstract fun productivityDao(): ProductivityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val seedMutex = Mutex()

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Non-destructive migration: preserve all existing user data while creating missing tables and indices
                // 1. Folders table
                db.execSQL("CREATE TABLE IF NOT EXISTS `folders` (`id` TEXT NOT NULL, `parentId` TEXT, `name` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `iconName` TEXT NOT NULL, `createdAtUtc` INTEGER NOT NULL, `updatedAtUtc` INTEGER NOT NULL, PRIMARY KEY(`id`))")

                // 2. Tags table and index
                db.execSQL("CREATE TABLE IF NOT EXISTS `tags` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `createdAtUtc` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")

                // 3. Notes table and indices for Schema Version 2 (ensuring foreign key to folders exists while preserving all user data)
                val notesTableCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='notes'")
                val notesExists = notesTableCursor.moveToFirst()
                notesTableCursor.close()

                if (!notesExists) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `notes` (`id` TEXT NOT NULL, `folderId` TEXT, `folderName` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `noteType` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `tagsCsv` TEXT NOT NULL, `isPinned` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `isLocked` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `reminderTimestamp` INTEGER, `canvasDataJson` TEXT NOT NULL, `checklistJson` TEXT NOT NULL, `imageUri` TEXT NOT NULL, `createdAtUtc` INTEGER NOT NULL, `updatedAtUtc` INTEGER NOT NULL, `deletedAtUtc` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`folderId`) REFERENCES `folders`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
                } else {
                    val fkCursor = db.query("PRAGMA foreign_key_list(`notes`)")
                    var hasFolderFk = false
                    while (fkCursor.moveToNext()) {
                        val tableColIndex = fkCursor.getColumnIndex("table")
                        if (tableColIndex != -1 && fkCursor.getString(tableColIndex).equals("folders", ignoreCase = true)) {
                            hasFolderFk = true
                            break
                        }
                    }
                    fkCursor.close()

                    if (!hasFolderFk) {
                        // Migrate notes table to add foreign key while preserving 100% of existing user data
                        db.execSQL("CREATE TABLE `notes_temp_v2` (`id` TEXT NOT NULL, `folderId` TEXT, `folderName` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `noteType` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `tagsCsv` TEXT NOT NULL, `isPinned` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `isLocked` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `reminderTimestamp` INTEGER, `canvasDataJson` TEXT NOT NULL, `checklistJson` TEXT NOT NULL, `imageUri` TEXT NOT NULL, `createdAtUtc` INTEGER NOT NULL, `updatedAtUtc` INTEGER NOT NULL, `deletedAtUtc` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`folderId`) REFERENCES `folders`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")

                        val colsCursor = db.query("PRAGMA table_info(`notes`)")
                        val existingCols = mutableSetOf<String>()
                        val nameIndex = colsCursor.getColumnIndex("name")
                        while (colsCursor.moveToNext()) {
                            if (nameIndex != -1) existingCols.add(colsCursor.getString(nameIndex))
                        }
                        colsCursor.close()

                        val v2Cols = listOf(
                            "id", "folderId", "folderName", "title", "content", "noteType",
                            "colorHex", "tagsCsv", "isPinned", "isArchived", "isDeleted",
                            "isLocked", "isFavorite", "reminderTimestamp", "canvasDataJson",
                            "checklistJson", "imageUri", "createdAtUtc", "updatedAtUtc", "deletedAtUtc"
                        )

                        val selectCols = v2Cols.joinToString(", ") { col ->
                            if (existingCols.contains(col)) "`$col`" else when (col) {
                                "isPinned", "isArchived", "isDeleted", "isLocked", "isFavorite" -> "0"
                                "createdAtUtc", "updatedAtUtc" -> "0"
                                "noteType" -> "'STANDARD'"
                                "folderName", "title", "content", "colorHex", "tagsCsv", "canvasDataJson", "checklistJson", "imageUri" -> "''"
                                else -> "NULL"
                            }
                        }

                        db.execSQL("INSERT INTO `notes_temp_v2` (`${v2Cols.joinToString("`, `")}`) SELECT $selectCols FROM `notes`")
                        db.execSQL("DROP TABLE `notes`")
                        db.execSQL("ALTER TABLE `notes_temp_v2` RENAME TO `notes`")
                    }
                }

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_folderId` ON `notes` (`folderId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isPinned_updatedAtUtc` ON `notes` (`isPinned`, `updatedAtUtc`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isArchived` ON `notes` (`isArchived`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isDeleted` ON `notes` (`isDeleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_noteType` ON `notes` (`noteType`)")

                // 4. NoteTags table and indices
                db.execSQL("CREATE TABLE IF NOT EXISTS `note_tags` (`noteId` TEXT NOT NULL, `tagId` TEXT NOT NULL, `assignedAtUtc` INTEGER NOT NULL, PRIMARY KEY(`noteId`, `tagId`), FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_tags_noteId` ON `note_tags` (`noteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_tags_tagId` ON `note_tags` (`tagId`)")

                // 5. StickyNotes table and indices
                db.execSQL("CREATE TABLE IF NOT EXISTS `sticky_notes` (`id` TEXT NOT NULL, `noteId` TEXT, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, `isPinned` INTEGER NOT NULL, `createdAtUtc` INTEGER NOT NULL, `updatedAtUtc` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sticky_notes_noteId` ON `sticky_notes` (`noteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sticky_notes_isPinned_orderIndex` ON `sticky_notes` (`isPinned`, `orderIndex`)")

                // 6. Canvases table
                db.execSQL("CREATE TABLE IF NOT EXISTS `canvases` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `backgroundColorHex` TEXT NOT NULL, `width` REAL NOT NULL, `height` REAL NOT NULL, `createdAtUtc` INTEGER NOT NULL, `updatedAtUtc` INTEGER NOT NULL, PRIMARY KEY(`id`))")

                // 7. CanvasElements table and indices
                db.execSQL("CREATE TABLE IF NOT EXISTS `canvas_elements` (`id` TEXT NOT NULL, `canvasId` TEXT NOT NULL, `elementType` TEXT NOT NULL, `elementDataJson` TEXT NOT NULL, `zIndex` INTEGER NOT NULL, `createdAtUtc` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`canvasId`) REFERENCES `canvases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_canvas_elements_canvasId` ON `canvas_elements` (`canvasId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_canvas_elements_canvasId_zIndex` ON `canvas_elements` (`canvasId`, `zIndex`)")

                // 8. Attachments table and indices
                db.execSQL("CREATE TABLE IF NOT EXISTS `attachments` (`id` TEXT NOT NULL, `noteId` TEXT, `canvasId` TEXT, `fileName` TEXT NOT NULL, `fileExtension` TEXT NOT NULL, `relativePath` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, `createdAtUtc` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_noteId` ON `attachments` (`noteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_mimeType` ON `attachments` (`mimeType`)")

                // 9. Annotations table and indices
                db.execSQL("CREATE TABLE IF NOT EXISTS `annotations` (`id` TEXT NOT NULL, `attachmentId` TEXT NOT NULL, `annotationType` TEXT NOT NULL, `pageNumber` INTEGER NOT NULL, `coordinatesJson` TEXT NOT NULL, `content` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `createdAtUtc` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`attachmentId`) REFERENCES `attachments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_annotations_attachmentId` ON `annotations` (`attachmentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_annotations_attachmentId_pageNumber` ON `annotations` (`attachmentId`, `pageNumber`)")

                // 10. Bookmarks table and index
                db.execSQL("CREATE TABLE IF NOT EXISTS `bookmarks` (`id` TEXT NOT NULL, `targetType` TEXT NOT NULL, `targetId` TEXT NOT NULL, `label` TEXT NOT NULL, `createdAtUtc` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_targetType_targetId` ON `bookmarks` (`targetType`, `targetId`)")

                // 11. Reminders table and indices
                db.execSQL("CREATE TABLE IF NOT EXISTS `reminders` (`id` TEXT NOT NULL, `noteId` TEXT NOT NULL, `scheduledTimeUtc` INTEGER NOT NULL, `repeatInterval` TEXT, `isCompleted` INTEGER NOT NULL, `createdAtUtc` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_noteId` ON `reminders` (`noteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_scheduledTimeUtc` ON `reminders` (`scheduledTimeUtc`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_isCompleted` ON `reminders` (`isCompleted`)")

                // 12. AppSettings table
                db.execSQL("CREATE TABLE IF NOT EXISTS `app_settings` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, `updatedAtUtc` INTEGER NOT NULL, PRIMARY KEY(`key`))")

                // 13. DeletedItems table and indices
                db.execSQL("CREATE TABLE IF NOT EXISTS `deleted_items` (`id` TEXT NOT NULL, `originalTable` TEXT NOT NULL, `originalId` TEXT NOT NULL, `itemSnapshotJson` TEXT NOT NULL, `deletedAtUtc` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_deleted_items_originalTable` ON `deleted_items` (`originalTable`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_deleted_items_deletedAtUtc` ON `deleted_items` (`deletedAtUtc`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ensure idempotency: verify whether isHidden column already exists in notes before altering
                val cursor = db.query("PRAGMA table_info(`notes`)")
                var hasIsHidden = false
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex != -1 && cursor.getString(nameIndex) == "isHidden") {
                        hasIsHidden = true
                        break
                    }
                }
                cursor.close()
                if (!hasIsHidden) {
                    db.execSQL("ALTER TABLE `notes` ADD COLUMN `isHidden` INTEGER NOT NULL DEFAULT 0")
                }
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isLocked` ON `notes` (`isLocked`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isHidden` ON `notes` (`isHidden`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Non-destructive Phase 1 Knowledge Brain migration:
                // Creates note_links, note_templates, and note_versions tables with indices & FKs.
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `note_links` (`id` TEXT NOT NULL, `sourceNoteId` TEXT NOT NULL, `targetNoteId` TEXT, `targetTitle` TEXT NOT NULL, `normalizedTargetTitle` TEXT NOT NULL, `anchorHeading` TEXT NOT NULL, `displayAlias` TEXT NOT NULL, `contextSnippet` TEXT NOT NULL, `createdAtUtc` INTEGER NOT NULL, `updatedAtUtc` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`sourceNoteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`targetNoteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_links_sourceNoteId` ON `note_links` (`sourceNoteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_links_targetNoteId` ON `note_links` (`targetNoteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_links_normalizedTargetTitle` ON `note_links` (`normalizedTargetTitle`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_note_links_sourceNoteId_normalizedTargetTitle` ON `note_links` (`sourceNoteId`, `normalizedTargetTitle`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `note_templates` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `category` TEXT NOT NULL, `titlePattern` TEXT NOT NULL, `contentTemplate` TEXT NOT NULL, `defaultTagsCsv` TEXT NOT NULL, `defaultColorHex` TEXT NOT NULL, `checklistTemplateJson` TEXT NOT NULL, `isBuiltIn` INTEGER NOT NULL, `usageCount` INTEGER NOT NULL, `createdAtUtc` INTEGER NOT NULL, `updatedAtUtc` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_note_templates_name` ON `note_templates` (`name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_templates_category_updatedAtUtc` ON `note_templates` (`category`, `updatedAtUtc`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_templates_isBuiltIn` ON `note_templates` (`isBuiltIn`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `note_versions` (`id` TEXT NOT NULL, `noteId` TEXT NOT NULL, `versionNumber` INTEGER NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `tagsCsv` TEXT NOT NULL, `checklistJson` TEXT NOT NULL, `changeSummary` TEXT NOT NULL, `wordCount` INTEGER NOT NULL, `createdAtUtc` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_versions_noteId_createdAtUtc` ON `note_versions` (`noteId`, `createdAtUtc`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_versions_noteId_versionNumber` ON `note_versions` (`noteId`, `versionNumber`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Non-destructive Phase 2 Productivity Workspace migration:
                // Adds kanban_columns, tasks, task_dependencies, habits, habit_logs, and focus_sessions.
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `kanban_columns` (`id` TEXT NOT NULL, `boardId` TEXT NOT NULL, `title` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `position` INTEGER NOT NULL, `wipLimit` INTEGER NOT NULL, `isDoneColumn` INTEGER NOT NULL, `createdAtUtc` INTEGER NOT NULL, `updatedAtUtc` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_kanban_columns_boardId_position` ON `kanban_columns` (`boardId`, `position`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tasks` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `columnId` TEXT NOT NULL, `orderInColumn` INTEGER NOT NULL, `priority` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `completedAtUtc` INTEGER, `dueAtUtc` INTEGER, `dueTimeZoneId` TEXT NOT NULL, `scheduledStartUtc` INTEGER, `scheduledEndUtc` INTEGER, `estimatedMinutes` INTEGER NOT NULL, `recurrenceRule` TEXT NOT NULL, `recurrenceInterval` INTEGER NOT NULL, `recurrenceDaysCsv` TEXT NOT NULL, `recurrenceParentId` TEXT, `subtasksJson` TEXT NOT NULL, `linkedNoteId` TEXT, `linkedReminderId` TEXT, `category` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `tagsCsv` TEXT NOT NULL, `isArchived` INTEGER NOT NULL, `createdAtUtc` INTEGER NOT NULL, `updatedAtUtc` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`linkedNoteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`linkedReminderId`) REFERENCES `reminders`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_columnId` ON `tasks` (`columnId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_columnId_orderInColumn` ON `tasks` (`columnId`, `orderInColumn`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_dueAtUtc` ON `tasks` (`dueAtUtc`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_scheduledStartUtc` ON `tasks` (`scheduledStartUtc`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_isCompleted` ON `tasks` (`isCompleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_linkedNoteId` ON `tasks` (`linkedNoteId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tasks_linkedReminderId` ON `tasks` (`linkedReminderId`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `task_dependencies` (`taskId` TEXT NOT NULL, `dependsOnTaskId` TEXT NOT NULL, `createdAtUtc` INTEGER NOT NULL, PRIMARY KEY(`taskId`, `dependsOnTaskId`), FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`dependsOnTaskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_dependencies_taskId` ON `task_dependencies` (`taskId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_dependencies_dependsOnTaskId` ON `task_dependencies` (`dependsOnTaskId`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `habits` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `category` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `iconName` TEXT NOT NULL, `frequencyType` TEXT NOT NULL, `targetPerDay` INTEGER NOT NULL, `targetDaysPerWeek` INTEGER NOT NULL, `reminderHour` INTEGER, `reminderMinute` INTEGER, `timeZoneId` TEXT NOT NULL, `isArchived` INTEGER NOT NULL, `createdAtUtc` INTEGER NOT NULL, `updatedAtUtc` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_isArchived` ON `habits` (`isArchived`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_createdAtUtc` ON `habits` (`createdAtUtc`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `habit_logs` (`id` TEXT NOT NULL, `habitId` TEXT NOT NULL, `localDateKey` TEXT NOT NULL, `completedCount` INTEGER NOT NULL, `targetCountSnapshot` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `note` TEXT NOT NULL, `timeZoneId` TEXT NOT NULL, `completedAtUtc` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_logs_habitId` ON `habit_logs` (`habitId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_logs_localDateKey` ON `habit_logs` (`localDateKey`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_logs_habitId_localDateKey` ON `habit_logs` (`habitId`, `localDateKey`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` TEXT NOT NULL, `sessionType` TEXT NOT NULL, `linkedTaskId` TEXT, `linkedNoteId` TEXT, `plannedDurationSec` INTEGER NOT NULL, `actualFocusedSec` INTEGER NOT NULL, `status` TEXT NOT NULL, `startedAtUtc` INTEGER NOT NULL, `endedAtUtc` INTEGER, `localDateKey` TEXT NOT NULL, `timeZoneId` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`linkedTaskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`linkedNoteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_linkedTaskId` ON `focus_sessions` (`linkedTaskId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_linkedNoteId` ON `focus_sessions` (`linkedNoteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_startedAtUtc` ON `focus_sessions` (`startedAtUtc`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_localDateKey` ON `focus_sessions` (`localDateKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_status` ON `focus_sessions` (`status`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val existingCols = mutableSetOf<String>()
                db.query("PRAGMA table_info(`attachments`)").use { cursor ->
                    val nameIdx = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        if (nameIdx != -1) existingCols.add(cursor.getString(nameIdx))
                    }
                }
                val columnsToAdd = listOf(
                    "folderId" to "TEXT DEFAULT NULL",
                    "contentHash" to "TEXT NOT NULL DEFAULT ''",
                    "pageCount" to "INTEGER NOT NULL DEFAULT 0",
                    "isFavorite" to "INTEGER NOT NULL DEFAULT 0",
                    "isArchived" to "INTEGER NOT NULL DEFAULT 0",
                    "isLocked" to "INTEGER NOT NULL DEFAULT 0",
                    "isHidden" to "INTEGER NOT NULL DEFAULT 0",
                    "isDeleted" to "INTEGER NOT NULL DEFAULT 0",
                    "deletedAtUtc" to "INTEGER DEFAULT NULL",
                    "updatedAtUtc" to "INTEGER NOT NULL DEFAULT 0"
                )
                for ((colName, colDef) in columnsToAdd) {
                    if (!existingCols.contains(colName)) {
                        db.execSQL("ALTER TABLE `attachments` ADD COLUMN `$colName` $colDef")
                    }
                }
                db.execSQL("UPDATE `attachments` SET `updatedAtUtc` = `createdAtUtc` WHERE `updatedAtUtc` = 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_folderId` ON `attachments` (`folderId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_contentHash` ON `attachments` (`contentHash`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_isDeleted` ON `attachments` (`isDeleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_isLocked` ON `attachments` (`isLocked`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sticky_notes` ADD COLUMN `boardName` TEXT NOT NULL DEFAULT 'Main Wall'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "noteland_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL("PRAGMA foreign_keys = ON;")
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedInitialDataDirect(database: AppDatabase) = seedMutex.withLock {
            // Ensure built-in knowledge templates exist even when upgrading from earlier DB versions
            database.noteTemplateDao().insertTemplates(KnowledgeBrainEngine.builtInTemplates())
            // Seed default Kanban columns idempotently if missing
            val productivityDao = database.productivityDao()
            if (productivityDao.getAllKanbanColumnsDirect().isEmpty()) {
                productivityDao.insertKanbanColumnsIgnore(ProductivityEngine.defaultKanbanColumns())
            }
            // Idempotently link any pre-existing reminders into tasks without duplicating
            val existingReminders = database.reminderDao().getAllRemindersDirect()
            if (existingReminders.isNotEmpty()) {
                val allNotesById = database.noteDao().getAllNotesDirect().associateBy { it.id }
                val columns = productivityDao.getAllKanbanColumnsDirect()
                val todoColId = columns.firstOrNull { !it.isDoneColumn }?.id ?: "col_todo"
                val doneColId = columns.firstOrNull { it.isDoneColumn }?.id ?: "col_done"
                val tzId = TimeZone.getDefault().id
                existingReminders.forEachIndexed { idx, rem ->
                    if (productivityDao.getTaskByLinkedReminderIdDirect(rem.id) == null) {
                        val linkedNote = allNotesById[rem.noteId]
                        if (linkedNote != null && !linkedNote.isDeleted) {
                            val meta = SmartNoteUtils.parseCalendarMeta(linkedNote.canvasDataJson)
                            val titleText = if (linkedNote.isLocked) {
                                "Locked Note Task"
                            } else {
                                linkedNote.title.ifBlank { "Scheduled Task" }
                            }
                            val descText = if (linkedNote.isLocked) "" else linkedNote.content.take(240)
                            val checklistText = if (linkedNote.isLocked) "" else linkedNote.checklistJson
                            val startUtc = meta?.startUtc ?: rem.scheduledTimeUtc
                            val endUtc = meta?.endUtc ?: (startUtc + 3600_000L)
                            val estMins = ((endUtc - startUtc) / 60_000L).toInt().coerceIn(15, 480)
                            val rule = TaskRecurrenceRule.fromName(rem.repeatInterval)
                            productivityDao.insertTasksIgnore(
                                listOf(
                                    TaskEntity(
                                        id = "task_rem_${rem.id}",
                                        title = titleText,
                                        description = descText,
                                        columnId = if (rem.isCompleted) doneColId else todoColId,
                                        orderInColumn = idx,
                                        priority = TaskPriority.MEDIUM.name,
                                        isCompleted = rem.isCompleted,
                                        completedAtUtc = if (rem.isCompleted) rem.createdAtUtc else null,
                                        dueAtUtc = rem.scheduledTimeUtc,
                                        dueTimeZoneId = tzId,
                                        scheduledStartUtc = startUtc,
                                        scheduledEndUtc = endUtc,
                                        estimatedMinutes = estMins,
                                        recurrenceRule = rule.name,
                                        subtasksJson = checklistText,
                                        linkedNoteId = linkedNote.id,
                                        linkedReminderId = rem.id,
                                        category = meta?.category ?: "Focus",
                                        colorHex = linkedNote.colorHex.ifBlank { "#3B82F6" },
                                        createdAtUtc = rem.createdAtUtc,
                                        updatedAtUtc = System.currentTimeMillis()
                                    )
                                )
                            )
                        }
                    }
                }
            }
            database.withTransaction {
                val initialized = database.appSettingsDao().getSetting("db_initialized")
                if (initialized == "true") {
                    return@withTransaction
                }
                try {
                    val existingNotes = database.noteDao().getAllNotesDirect()
                    val existingFolders = database.folderDao().getAllFoldersDirect()
                    if (existingNotes.isEmpty() && existingFolders.isEmpty()) {
                    val pFolderId = UUID.randomUUID().toString()
                    val wFolderId = UUID.randomUUID().toString()
                    val iFolderId = UUID.randomUUID().toString()

                    val folderPersonal = FolderEntity(id = pFolderId, name = "Personal", colorHex = "#10B981", iconName = "person")
                    val folderWork = FolderEntity(id = wFolderId, name = "Projects", colorHex = "#3B82F6", iconName = "work")
                    val folderIdeas = FolderEntity(id = iFolderId, name = "Creative", colorHex = "#F59E0B", iconName = "lightbulb")
                    val folderRoadmap = FolderEntity(
                        id = UUID.randomUUID().toString(),
                        parentId = wFolderId,
                        name = "Roadmaps",
                        colorHex = "#6366F1",
                        iconName = "work"
                    )
                    val folderReading = FolderEntity(
                        id = UUID.randomUUID().toString(),
                        parentId = iFolderId,
                        name = "Reading Notes",
                        colorHex = "#EC4899",
                        iconName = "school"
                    )
                    database.folderDao().insertFolders(
                        listOf(folderPersonal, folderWork, folderIdeas, folderRoadmap, folderReading)
                    )

                    val tImportant = TagEntity(id = UUID.randomUUID().toString(), name = "Important", colorHex = "#EF4444")
                    val tReading = TagEntity(id = UUID.randomUUID().toString(), name = "Reading", colorHex = "#8B5CF6")
                    val tIdea = TagEntity(id = UUID.randomUUID().toString(), name = "Idea", colorHex = "#F59E0B")
                    val tChecklist = TagEntity(id = UUID.randomUUID().toString(), name = "Checklist", colorHex = "#10B981")
                    database.tagDao().insertTags(listOf(tImportant, tReading, tIdea, tChecklist))

                    val now = System.currentTimeMillis()
                    val sampleNotes = listOf(
                        NoteEntity(
                            id = UUID.randomUUID().toString(),
                            title = "Welcome to NoteLand ✨",
                            content = """
# Welcome to NoteLand! 🚀

NoteLand is your **offline-first**, private personal knowledge management sanctuary.

### 🌟 What can you do here?
- **Rich Formatting**: Markdown headings, bold, italics, quotes, and code blocks.
- **Interactive Checklists**: Keep track of projects and daily to-dos.
- **Sticky Notes**: Fast colorful capture board for lightning-quick thoughts.
- **Infinite Canvas**: Sketch, draw diagrams, and brainstorm with freehand tools.
- **Biometric & PIN Vault**: Encrypt sensitive notes with AES-256 Android Keystore.
- **Full Offline Backup**: Export and restore your complete database anytime.

> *"Knowledge is power when structured and accessible."*
                            """.trimIndent(),
                            noteType = NoteType.STANDARD.name,
                            colorHex = "#EFF6FF",
                            folderId = pFolderId,
                            folderName = "Personal",
                            tagsCsv = "Important,Reading",
                            isPinned = true,
                            isFavorite = true,
                            checklistJson = """[{"text":"Explore NoteLand features","done":true},{"text":"Create my first custom note","done":false},{"text":"Try the Infinite Canvas sketch tool","done":false},{"text":"Set up secure Vault PIN","done":false}]""",
                            createdAtUtc = now - 3600000,
                            updatedAtUtc = now
                        ),
                        NoteEntity(
                            id = UUID.randomUUID().toString(),
                            title = "Weekly Project Roadmap 🎯",
                            content = """
## Architecture Sprint
- [x] Design offline-first SQLite database schema
- [x] Implement Material 3 dynamic tonal surfaces
- [ ] Review Android Keystore encryption for secure vault
- [ ] Export knowledge graph backup JSON

```kotlin
// Android Keystore AES-256 security
val cipher = Cipher.getInstance("AES/GCM/NoPadding")
cipher.init(Cipher.ENCRYPT_MODE, secretKey)
```
                            """.trimIndent(),
                            noteType = NoteType.STANDARD.name,
                            colorHex = "#F0FDF4",
                            folderId = wFolderId,
                            folderName = "Projects",
                            tagsCsv = "Checklist,Important",
                            isPinned = true,
                            checklistJson = """[{"text":"Design offline SQLite schema","done":true},{"text":"Implement Material 3 tonal surfaces","done":true},{"text":"Android Keystore vault encryption","done":false},{"text":"Export knowledge graph backup","done":false}]""",
                            createdAtUtc = now - 7200000,
                            updatedAtUtc = now - 1800000
                        ),
                        NoteEntity(
                            id = UUID.randomUUID().toString(),
                            title = "Mindmap Sketch 🎨",
                            content = "Visual architecture and flow diagram drawn on the infinite canvas.",
                            noteType = NoteType.CANVAS.name,
                            colorHex = "#FAF5FF",
                            folderId = iFolderId,
                            folderName = "Creative",
                            tagsCsv = "Idea",
                            canvasDataJson = """[{"color":-16744193,"width":5.0,"points":[{"x":120.0,"y":200.0},{"x":180.0,"y":240.0},{"x":260.0,"y":220.0}]},{"color":-3381760,"width":4.0,"points":[{"x":260.0,"y":220.0},{"x":340.0,"y":300.0},{"x":400.0,"y":280.0}]}]""",
                            createdAtUtc = now - 21600000,
                            updatedAtUtc = now - 7200000
                        ),
                        NoteEntity(
                            id = UUID.randomUUID().toString(),
                            title = "LangChain",
                            content = "Learn LangChain fundamentals Build a simple tool-calling agent Understand chains and runnables Learn LangGraph basics Create a customized workflow for autonomous task planning.",
                            noteType = NoteType.STANDARD.name,
                            tagsCsv = "Diary,Reading",
                            createdAtUtc = now - 14400000,
                            updatedAtUtc = now - 1200000
                        ),
                        NoteEntity(
                            id = UUID.randomUUID().toString(),
                            title = "Why use smolagents",
                            content = "In this module, we will explore the pros and cons of using smolagents, helping you make an informed decision right from the start.",
                            noteType = NoteType.STANDARD.name,
                            tagsCsv = "Reading",
                            createdAtUtc = now - 28800000,
                            updatedAtUtc = now - 3600000
                        ),
                        NoteEntity(
                            id = UUID.randomUUID().toString(),
                            title = "What is an Agent?",
                            content = "An agent is a computational entity that perceives its environment through sensors and takes actions to achieve specific goals autonomously.",
                            noteType = NoteType.STANDARD.name,
                            tagsCsv = "Idea",
                            createdAtUtc = now - 43200000,
                            updatedAtUtc = now - 7200000
                        )
                    )
                    database.noteDao().insertNotes(sampleNotes)

                    // Seed sticky notes
                    database.stickyNoteDao().insertStickyNotes(
                        listOf(
                            StickyNoteEntity(
                                title = "Meeting Takeaways 💡",
                                content = "100% on-device data sovereignty and instant latency.",
                                colorHex = "#FEF3C7",
                                orderIndex = 0,
                                isPinned = true
                            ),
                            StickyNoteEntity(
                                title = "Weekly Backup",
                                content = "Remember to export snapshot JSON every Friday.",
                                colorHex = "#FCE7F3",
                                orderIndex = 1
                            )
                        )
                    )
                }
                } finally {
                    // Set the initialization flag after initialization regardless of whether the database contains notes.
                    // Demo notes are never re-seeded after the user deletes all notes.
                    database.appSettingsDao().setSetting(AppSettingEntity("db_initialized", "true"))
                    val existingTheme = database.appSettingsDao().getSetting("theme_mode")
                    if (existingTheme == null) {
                        database.appSettingsDao().setSetting(AppSettingEntity("theme_mode", "light"))
                    }
                }
            }
        }

        fun seedInitialData(database: AppDatabase): kotlinx.coroutines.Job {
            return CoroutineScope(Dispatchers.IO).launch {
                seedInitialDataDirect(database)
            }
        }
    }
}
