package com.example

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.core.content.FileProvider
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.AnnotationEntity
import com.example.data.model.AppSettingEntity
import com.example.data.model.AttachmentEntity
import com.example.data.model.CanvasElementEntity
import com.example.data.model.CanvasEntity
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.data.model.NoteType
import com.example.data.model.ReminderEntity
import com.example.data.repository.NoteRepository
import com.example.ui.screens.CanvasItem
import com.example.ui.screens.ImageAnnotationItem
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.BackupManager
import com.example.util.RestoreStrategy
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StabilizationPassRobolectricTest {

    // ==========================================
    // 1. ROOM MIGRATION CONFLICT & INTEGRITY TESTS
    // ==========================================

    @Test
    fun `test migration from version 1 to 2 to 3 preserves user data with zero conflicts`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = File(context.filesDir, "migration_test_v1_to_v3.db")
        if (dbFile.exists()) dbFile.delete()

        val helperConfiguration = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbFile.name)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create Schema Version 1
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `notes` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `folderId` TEXT,
                            `folderName` TEXT NOT NULL DEFAULT '',
                            `title` TEXT NOT NULL DEFAULT '',
                            `content` TEXT NOT NULL DEFAULT '',
                            `noteType` TEXT NOT NULL DEFAULT 'STANDARD',
                            `colorHex` TEXT NOT NULL DEFAULT '',
                            `tagsCsv` TEXT NOT NULL DEFAULT '',
                            `isPinned` INTEGER NOT NULL DEFAULT 0,
                            `isArchived` INTEGER NOT NULL DEFAULT 0,
                            `isDeleted` INTEGER NOT NULL DEFAULT 0,
                            `isLocked` INTEGER NOT NULL DEFAULT 0,
                            `isFavorite` INTEGER NOT NULL DEFAULT 0,
                            `reminderTimestamp` INTEGER,
                            `canvasDataJson` TEXT NOT NULL DEFAULT '',
                            `checklistJson` TEXT NOT NULL DEFAULT '',
                            `imageUri` TEXT NOT NULL DEFAULT '',
                            `createdAtUtc` INTEGER NOT NULL,
                            `updatedAtUtc` INTEGER NOT NULL,
                            `deletedAtUtc` INTEGER
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(helperConfiguration)
        val v1Db = openHelper.writableDatabase

        // Insert pre-existing user data in Version 1
        val userNoteId = "user-v1-note-001"
        v1Db.execSQL(
            """
            INSERT INTO `notes` (id, title, content, noteType, colorHex, isPinned, createdAtUtc, updatedAtUtc)
            VALUES ('$userNoteId', 'V1 User Note', 'Critical user knowledge created in version 1', 'STANDARD', '#EFF6FF', 1, 1600000000000, 1600000000000)
            """.trimIndent()
        )

        // Verify Version 1 row is written
        val v1Cursor = v1Db.query("SELECT title, content, isPinned FROM notes WHERE id = '$userNoteId'")
        assertTrue(v1Cursor.moveToFirst())
        assertEquals("V1 User Note", v1Cursor.getString(0))
        assertEquals("Critical user knowledge created in version 1", v1Cursor.getString(1))
        assertEquals(1, v1Cursor.getInt(2))
        v1Cursor.close()

        // 1. Run MIGRATION_1_2
        AppDatabase.MIGRATION_1_2.migrate(v1Db)

        // Verify folders and tags tables created
        val folderCheckCursor = v1Db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='folders'")
        assertTrue("folders table must exist after MIGRATION_1_2", folderCheckCursor.moveToFirst())
        folderCheckCursor.close()

        // Verify existing note survived MIGRATION_1_2 untouched
        val midCursor = v1Db.query("SELECT title, content FROM notes WHERE id = '$userNoteId'")
        assertTrue(midCursor.moveToFirst())
        assertEquals("V1 User Note", midCursor.getString(0))
        midCursor.close()

        // 2. Run MIGRATION_2_3
        AppDatabase.MIGRATION_2_3.migrate(v1Db)

        // Verify isHidden column was created and default value is 0
        val postMigrationCursor = v1Db.query("SELECT id, title, content, isHidden FROM notes WHERE id = '$userNoteId'")
        assertTrue("User note must survive all migrations intact", postMigrationCursor.moveToFirst())
        assertEquals(userNoteId, postMigrationCursor.getString(0))
        assertEquals("V1 User Note", postMigrationCursor.getString(1))
        assertEquals("Critical user knowledge created in version 1", postMigrationCursor.getString(2))
        assertEquals(0, postMigrationCursor.getInt(3)) // Default isHidden = 0
        postMigrationCursor.close()

        v1Db.close()
        openHelper.close()

        // 3. Open migrated database using Room AppDatabase (supports upgrade v3 -> v5)
        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbFile.name)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
            .allowMainThreadQueries()
            .build()

        runBlocking {
            val retrieved = roomDb.noteDao().getNoteByIdDirect(userNoteId)
            assertNotNull("Room must open migrated database and find the original note", retrieved)
            assertEquals("V1 User Note", retrieved?.title)
            assertEquals("Critical user knowledge created in version 1", retrieved?.content)
            assertEquals(false, retrieved?.isHidden)
            assertEquals(true, retrieved?.isPinned)
        }

        roomDb.close()
        dbFile.delete()
    }

    @Test
    fun `test migration 2 to 3 is idempotent and handles existing isHidden column safely`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = File(context.filesDir, "migration_idempotent_test.db")
        if (dbFile.exists()) dbFile.delete()

        val helperConfiguration = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbFile.name)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create Schema Version 2
                    AppDatabase.MIGRATION_1_2.migrate(db)
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(helperConfiguration)
        val v2Db = openHelper.writableDatabase

        // Execute MIGRATION_2_3 first time
        AppDatabase.MIGRATION_2_3.migrate(v2Db)

        // Execute MIGRATION_2_3 second time: must NOT crash with duplicate column error!
        AppDatabase.MIGRATION_2_3.migrate(v2Db)

        v2Db.close()
        openHelper.close()
        dbFile.delete()
    }

    // ==========================================
    // 2. SAMPLE DATA INITIALIZATION TESTS
    // ==========================================

    @Test
    fun `test seedInitialDataDirect sets db_initialized flag and never reinserts after user deletes all notes`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // 1. Initial State: db_initialized is null
        val initialFlag = db.appSettingsDao().getSetting("db_initialized")
        assertNull("db_initialized should be null prior to initial seeding", initialFlag)

        // 2. Run seedInitialDataDirect on fresh database
        AppDatabase.seedInitialDataDirect(db)

        // 3. Verification: Demo notes are present and db_initialized is "true"
        val notesAfterSeed = db.noteDao().getAllNotesDirect()
        assertTrue("Seed data must populate initial sample notes", notesAfterSeed.isNotEmpty())
        val flagAfterSeed = db.appSettingsDao().getSetting("db_initialized")
        assertEquals("true", flagAfterSeed)

        // 4. User deletes all notes (simulating emptying trash/purging all notes)
        db.noteDao().clearAllNotes()
        val notesAfterDeletion = db.noteDao().getAllNotesDirect()
        assertEquals(0, notesAfterDeletion.size)

        // 5. Subsequent startup runs seedInitialDataDirect again
        AppDatabase.seedInitialDataDirect(db)

        // 6. CRITICAL VERIFICATION: Demo notes must NEVER be reinserted!
        val notesAfterSecondRun = db.noteDao().getAllNotesDirect()
        assertEquals("Demo notes must NEVER be reinserted after user deletes all notes", 0, notesAfterSecondRun.size)
        assertEquals("true", db.appSettingsDao().getSetting("db_initialized"))

        db.close()
    }

    @Test
    fun `test seedInitialDataDirect marks initialized if database already contained user notes`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // Pre-insert user note before seedInitialDataDirect runs (e.g. from restored backup or sync)
        val customNote = NoteEntity(
            id = "pre-existing-note",
            title = "My Imported Notes",
            content = "User existing content"
        )
        db.noteDao().insertNote(customNote)

        // Call seedInitialDataDirect
        AppDatabase.seedInitialDataDirect(db)

        // Verify: db_initialized is set to "true" and demo notes are NOT added to user database
        assertEquals("true", db.appSettingsDao().getSetting("db_initialized"))
        val allNotes = db.noteDao().getAllNotesDirect()
        assertEquals(1, allNotes.size)
        assertEquals("pre-existing-note", allNotes[0].id)

        db.close()
    }

    // ==========================================
    // 3. SERIALIZED AUTOSAVE & STALE WRITE PROTECTION
    // ==========================================

    @Test
    fun `test serialized save queue prevents older revisions from overwriting newer edits`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = NoteViewModel(app)

        val noteId = UUID.randomUUID().toString()
        val rev1Note = NoteEntity(id = noteId, title = "Title Rev 100", content = "Content at Rev 100")
        val rev2Note = NoteEntity(id = noteId, title = "Title Rev 200 (Latest)", content = "Content at Rev 200 (Latest)")
        val revStaleNote = NoteEntity(id = noteId, title = "Stale Edit", content = "Overwriting attempt with stale snapshot")

        // 1. Save revision 100
        val r1 = viewModel.saveNoteSerialized(rev1Note, revision = 100L)
        assertTrue(r1.isSuccess)

        // 2. Save newer revision 200
        val r2 = viewModel.saveNoteSerialized(rev2Note, revision = 200L)
        assertTrue(r2.isSuccess)

        // Verify database holds revision 200
        val fetchedAfterRev2 = viewModel.getNoteByIdDirect(noteId)
        assertEquals("Title Rev 200 (Latest)", fetchedAfterRev2?.title)

        // 3. Attempt to save stale snapshot with revision 150 (< 200)
        val rStale = viewModel.saveNoteSerialized(revStaleNote, revision = 150L)
        assertTrue(rStale.isSuccess)

        // 4. CRITICAL VERIFICATION: The database must STILL hold revision 200 content!
        val fetchedAfterStale = viewModel.getNoteByIdDirect(noteId)
        assertEquals("Newer revision must not be overwritten by stale revision", "Title Rev 200 (Latest)", fetchedAfterStale?.title)
        assertEquals("Content at Rev 200 (Latest)", fetchedAfterStale?.content)
    }

    // ==========================================
    // 4. EDITOR PROPERTY PERSISTENCE VERIFICATION
    // ==========================================

    @Test
    fun `test all note editor properties persist faithfully to SQLite`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = NoteViewModel(app)

        // Create folder
        val folder = FolderEntity(id = UUID.randomUUID().toString(), name = "Research", colorHex = "#3B82F6")
        viewModel.repository.insertFolder(folder)

        val noteId = UUID.randomUUID().toString()
        val reminderTime = System.currentTimeMillis() + 86400000L
        val checklistJson = """[{"id":"1","text":"Review architecture","isDone":true},{"id":"2","text":"Deploy release","isDone":false}]"""

        val fullNote = NoteEntity(
            id = noteId,
            title = "Quantum Computing Survey",
            content = "# Executive Summary\nAnalysis of superconducting qubits.",
            noteType = NoteType.STANDARD.name,
            colorHex = "#FEF3C7",
            folderId = folder.id,
            folderName = folder.name,
            tagsCsv = "Quantum,Physics,Research",
            isPinned = true,
            isFavorite = true,
            isArchived = false,
            isLocked = false,
            reminderTimestamp = reminderTime,
            checklistJson = checklistJson,
            imageUri = "file:///storage/emulated/0/photo.jpg",
            createdAtUtc = 1700000000000L,
            updatedAtUtc = 1700000050000L
        )

        val saveRes = viewModel.saveNoteSerialized(fullNote, revision = 1L)
        assertTrue(saveRes.isSuccess)

        val retrieved = viewModel.getNoteByIdDirect(noteId)
        assertNotNull(retrieved)
        assertEquals("Quantum Computing Survey", retrieved?.title)
        assertEquals("# Executive Summary\nAnalysis of superconducting qubits.", retrieved?.content)
        assertEquals(folder.id, retrieved?.folderId)
        assertEquals("Research", retrieved?.folderName)
        assertEquals("Quantum,Physics,Research", retrieved?.tagsCsv)
        assertEquals("#FEF3C7", retrieved?.colorHex)
        assertTrue(retrieved?.isPinned == true)
        assertTrue(retrieved?.isFavorite == true)
        assertEquals(reminderTime, retrieved?.reminderTimestamp)
        assertEquals(checklistJson, retrieved?.checklistJson)
        assertEquals("file:///storage/emulated/0/photo.jpg", retrieved?.imageUri)
    }

    // ==========================================
    // 5. LIFECYCLE BACKGROUND FLUSH & PROCESS DEATH
    // ==========================================

    @Test
    fun `test flushSaveDirect synchronously persists pending changes on lifecycle backgrounding`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = NoteViewModel(app)

        val noteId = UUID.randomUUID().toString()
        val activeDraft = NoteEntity(
            id = noteId,
            title = "Uncommitted Draft in RAM",
            content = "User was typing when sudden incoming phone call backgrounded the app"
        )

        // Lifecycle flush called synchronously on ON_STOP
        val flushResult = viewModel.flushSaveDirect(activeDraft, revision = 10L)
        assertTrue(flushResult.isSuccess)

        // Verify row is immediately committed to SQLite
        val inDatabase = viewModel.getNoteByIdDirect(noteId)
        assertNotNull("Synchronous lifecycle flush must write to SQLite before backgrounding finishes", inDatabase)
        assertEquals("Uncommitted Draft in RAM", inDatabase?.title)
        assertEquals("User was typing when sudden incoming phone call backgrounded the app", inDatabase?.content)
    }

    // ==========================================
    // 6. BUG-FIX REGRESSION TESTS
    // ==========================================

    @Test
    fun `test FileProvider resolves content uri for pdf files in attachments directory`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val attachmentsDir = File(context.filesDir, "attachments").apply { mkdirs() }
        val samplePdf = File(attachmentsDir, "share_test_${UUID.randomUUID()}.pdf").apply {
            writeBytes("%PDF-1.4\n%test\n".toByteArray())
        }

        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", samplePdf)
            assertEquals("content", uri.scheme)
            assertEquals("${context.packageName}.fileprovider", uri.authority)
        } finally {
            samplePdf.delete()
        }
    }

    @Test
    fun `test encrypted ZIP backup with attachments restores exact physical bytes cleanly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = NoteRepository(db, context)
        val backupManager = BackupManager(context, repo)

        val attachmentsDir = File(context.filesDir, "attachments").apply { mkdirs() }
        val relPath = "attachments/encrypted_backup_sample_${UUID.randomUUID()}.pdf"
        val physicalFile = File(context.filesDir, relPath)
        val expectedBytes = "BINARY_ATTACHMENT_PAYLOAD_987654321".toByteArray()
        physicalFile.writeBytes(expectedBytes)

        val att = AttachmentEntity(
            id = "att-enc-test",
            fileName = physicalFile.name,
            fileExtension = "pdf",
            relativePath = relPath,
            mimeType = "application/pdf",
            sizeBytes = expectedBytes.size.toLong()
        )
        repo.saveAttachment(att)

        val password = "StrongAttachmentBackupPassword!"
        val zipFile = backupManager.createBackupZip(password)
        assertTrue(zipFile.exists())

        // Delete original physical file before restore to verify clean extraction & decryption
        physicalFile.delete()
        assertFalse(physicalFile.exists())

        val restoreResult = backupManager.restoreFromZip(zipFile, RestoreStrategy.REPLACE, password)
        assertTrue("Encrypted backup with attachment must restore without AEADBadTagException: ${restoreResult.exceptionOrNull()}", restoreResult.isSuccess)
        assertEquals(1, restoreResult.getOrNull()?.restoredAttachmentsCount)
        assertTrue("Restored physical attachment file must exist on disk", physicalFile.exists())
        assertArrayEquals(expectedBytes, physicalFile.readBytes())

        zipFile.delete()
        physicalFile.delete()
        db.close()
    }

    @Test
    fun `test duplicateNote on locked note preserves decryptable title with Copy suffix`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = NoteViewModel(app)

        val originalId = UUID.randomUUID().toString()
        val lockedOriginal = NoteEntity(
            id = originalId,
            title = viewModel.securityManager.encrypt("Private Passwords"),
            content = viewModel.securityManager.encrypt("Top secret body"),
            isLocked = true
        )
        viewModel.saveNoteSerialized(lockedOriginal, revision = 1L)

        val duplicatedIdDeferred = CompletableDeferred<String>()
        viewModel.duplicateNote(lockedOriginal) { newId ->
            duplicatedIdDeferred.complete(newId)
        }

        val dupDeadline = System.currentTimeMillis() + 5_000L
        while (!duplicatedIdDeferred.isCompleted && System.currentTimeMillis() < dupDeadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue("Timed out awaiting duplicateNote callback", duplicatedIdDeferred.isCompleted)
        val duplicatedId = duplicatedIdDeferred.await()
        val duplicated = viewModel.getNoteByIdDirect(duplicatedId)
        assertNotNull(duplicated)
        assertTrue(duplicated!!.isLocked)
        assertEquals("Private Passwords (Copy)", viewModel.securityManager.decrypt(duplicated.title))
        assertEquals("Top secret body", viewModel.securityManager.decrypt(duplicated.content))
    }

    @Test
    fun `test importMediaFile persists attachment and linked note cleanly`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = NoteViewModel(app)

        val sourceFile = File(app.cacheDir, "import_source_${UUID.randomUUID()}.pdf").apply {
            writeBytes("%PDF-1.4\nSample content".toByteArray())
        }
        var importedAttachment: AttachmentEntity? = null
        try {
            val savedDeferred = CompletableDeferred<AttachmentEntity>()
            val errorDeferred = CompletableDeferred<Throwable>()
            viewModel.importMediaFile(
                uri = Uri.fromFile(sourceFile),
                fileName = "LectureSlides.pdf",
                isPdf = true,
                onError = { errorDeferred.complete(it) },
                onSaved = { savedDeferred.complete(it) }
            )

            val importDeadline = System.currentTimeMillis() + 5_000L
            while (!savedDeferred.isCompleted && !errorDeferred.isCompleted && System.currentTimeMillis() < importDeadline) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(10)
            }
            assertFalse("importMediaFile failed unexpectedly: ${if (errorDeferred.isCompleted) errorDeferred.await() else ""}", errorDeferred.isCompleted)
            assertTrue("Timed out awaiting importMediaFile callback", savedDeferred.isCompleted)
            importedAttachment = savedDeferred.await()
            assertEquals("LectureSlides.pdf", importedAttachment.fileName)
            assertEquals("pdf", importedAttachment.fileExtension)
            assertTrue(viewModel.attachmentStorageManager.getFileFromRelativePath(importedAttachment.relativePath).exists())
            val activeNotesAfterImport = com.example.data.db.AppDatabase.getDatabase(app).noteDao().getAllActiveNotes().first()
            assertFalse(
                "Imported PDF doc should not appear in Notes section",
                activeNotesAfterImport.any { it.noteType == NoteType.PDF_DOC.name && it.title.contains("LectureSlides") }
            )
        } finally {
            sourceFile.delete()
            importedAttachment?.let {
                viewModel.attachmentStorageManager.deleteFile(it.relativePath)
            }
        }
    }

    @Test
    fun `test custom app icon and starting animation drawables load cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val launcherDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
        val roundLauncherDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round)
        val iconDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.noteland_icon)
        val splashDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.noteland_splash)

        assertNotNull("ic_launcher mipmap must resolve", launcherDrawable)
        assertNotNull("ic_launcher_round mipmap must resolve", roundLauncherDrawable)
        assertNotNull("noteland_icon drawable must resolve", iconDrawable)
        assertNotNull("noteland_splash drawable must resolve", splashDrawable)
        assertTrue("Splash drawable must be vertical portrait artwork", splashDrawable!!.intrinsicHeight > splashDrawable.intrinsicWidth)
    }

    @Test
    fun `test deleteAttachment preserves shared physical file and clears linked note imageUri`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = NoteViewModel(app)
        val db = AppDatabase.getDatabase(app)
        val repo = NoteRepository(db, app)

        val attachmentsDir = File(app.filesDir, "attachments").apply { mkdirs() }
        val sharedFile = File(attachmentsDir, "shared_photo_${UUID.randomUUID()}.jpg").apply {
            writeBytes("PHOTO_BYTES".toByteArray())
        }
        val relPath = "attachments/${sharedFile.name}"

        val noteId = UUID.randomUUID().toString()
        val att1Id = UUID.randomUUID().toString()
        val att2Id = UUID.randomUUID().toString()
        val note = NoteEntity(
            id = noteId,
            title = "Photo Note",
            imageUri = sharedFile.absolutePath
        )
        val att1 = AttachmentEntity(
            id = att1Id,
            noteId = noteId,
            fileName = "photo.jpg",
            fileExtension = "jpg",
            relativePath = relPath,
            mimeType = "image/jpeg",
            sizeBytes = sharedFile.length()
        )
        val att2 = AttachmentEntity(
            id = att2Id,
            noteId = null,
            fileName = "photo_copy.jpg",
            fileExtension = "jpg",
            relativePath = relPath,
            mimeType = "image/jpeg",
            sizeBytes = sharedFile.length()
        )

        try {
            repo.insertNote(note)
            repo.saveAttachment(att1)
            repo.saveAttachment(att2)

            viewModel.deleteAttachment(att1)
            val deadline1 = System.currentTimeMillis() + 5_000L
            while ((repo.getAttachmentById(att1Id) != null || (repo.getNoteByIdDirect(noteId)?.imageUri?.isNotEmpty() == true)) && System.currentTimeMillis() < deadline1) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(15)
            }

            assertNull("Deleted attachment row must be removed", repo.getAttachmentById(att1Id))
            assertEquals("Linked note imageUri should be cleared on deletion", "", repo.getNoteByIdDirect(noteId)?.imageUri)
            assertTrue("Shared physical file must NOT be deleted while att2 still references it", sharedFile.exists())

            viewModel.deleteAttachment(att2)
            val deadline2 = System.currentTimeMillis() + 5_000L
            while ((repo.getAttachmentById(att2Id) != null || sharedFile.exists()) && System.currentTimeMillis() < deadline2) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(15)
            }

            assertNull(repo.getAttachmentById(att2Id))
            assertFalse("Physical file must be deleted once no attachment or note references it", sharedFile.exists())
        } finally {
            repo.deletePermanently(noteId)
            repo.deleteAttachment(att1Id)
            repo.deleteAttachment(att2Id)
            sharedFile.delete()
        }
    }

    @Test
    fun `test deleteReminder syncs or clears parent note reminderTimestamp`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = NoteViewModel(app)
        val db = AppDatabase.getDatabase(app)
        val repo = NoteRepository(db, app)

        val noteId = UUID.randomUUID().toString()
        val futureTime = System.currentTimeMillis() + 3_600_000L
        val note = NoteEntity(
            id = noteId,
            title = "Note With Reminder",
            reminderTimestamp = futureTime
        )
        val remId = UUID.randomUUID().toString()
        val rem = ReminderEntity(
            id = remId,
            noteId = noteId,
            scheduledTimeUtc = futureTime,
            isCompleted = false
        )

        try {
            repo.insertNote(note)
            repo.saveReminder(rem)

            viewModel.deleteReminder(remId)
            val deadline = System.currentTimeMillis() + 5_000L
            while ((repo.getReminderById(remId) != null || repo.getNoteByIdDirect(noteId)?.reminderTimestamp != null) && System.currentTimeMillis() < deadline) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(15)
            }

            assertNull("Deleted reminder must be removed", repo.getReminderById(remId))
            assertNull("Note reminderTimestamp must be cleared when last reminder is deleted", repo.getNoteByIdDirect(noteId)?.reminderTimestamp)
        } finally {
            repo.deleteReminder(remId)
            repo.deletePermanently(noteId)
        }
    }

    @Test
    fun `test backup and restore handles duplicate filenames and sanitizes dangling FK references`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = NoteRepository(db, context)
        val backupManager = BackupManager(context, repo)

        val attachmentsDir = File(context.filesDir, "attachments").apply { mkdirs() }
        val fileA = File(attachmentsDir, "dup_a_${UUID.randomUUID()}.jpg").apply {
            writeBytes("FIRST_IMAGE_BYTES".toByteArray())
        }
        val fileB = File(attachmentsDir, "dup_b_${UUID.randomUUID()}.jpg").apply {
            writeBytes("SECOND_IMAGE_BYTES".toByteArray())
        }

        val attA = AttachmentEntity(
            id = "att-dup-a",
            fileName = "photo.jpg",
            fileExtension = "jpg",
            relativePath = "attachments/${fileA.name}",
            mimeType = "image/jpeg",
            sizeBytes = fileA.length()
        )
        val attB = AttachmentEntity(
            id = "att-dup-b",
            fileName = "photo.jpg",
            fileExtension = "jpg",
            relativePath = "attachments/${fileB.name}",
            mimeType = "image/jpeg",
            sizeBytes = fileB.length()
        )

        var zipFile: File? = null
        var patchedZipFile: File? = null
        try {
            repo.saveAttachment(attA)
            repo.saveAttachment(attB)

            zipFile = backupManager.createBackupZip()

            // Read entries from the exported ZIP and inject orphaned FK rows into database.json
            val zipEntries = linkedMapOf<String, ByteArray>()
            java.util.zip.ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        zipEntries[entry.name] = zis.readBytes()
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val dbJson = org.json.JSONObject(String(zipEntries.getValue(BackupManager.DATABASE_ENTRY), Charsets.UTF_8))
            dbJson.getJSONArray("folders").put(
                org.json.JSONObject().apply {
                    put("id", "folder-orphan-parent")
                    put("parentId", "missing-parent")
                    put("name", "Sub")
                }
            )
            dbJson.getJSONArray("notes").put(
                org.json.JSONObject().apply {
                    put("id", "note-orphan-folder")
                    put("folderId", "missing-folder")
                    put("title", "Note")
                }
            )
            dbJson.getJSONArray("canvasElements").put(
                org.json.JSONObject().apply {
                    put("id", "elem-orphan")
                    put("canvasId", "missing-canvas")
                    put("elementType", "STROKE")
                    put("elementDataJson", "{}")
                }
            )
            dbJson.getJSONArray("annotations").put(
                org.json.JSONObject().apply {
                    put("id", "ann-orphan")
                    put("attachmentId", "missing-att")
                    put("annotationType", "PEN")
                    put("pageNumber", 1)
                    put("coordinatesJson", "{}")
                }
            )
            dbJson.getJSONArray("reminders").put(
                org.json.JSONObject().apply {
                    put("id", "rem-orphan")
                    put("noteId", "missing-note")
                    put("scheduledTimeUtc", System.currentTimeMillis())
                }
            )

            val patchedDbBytes = dbJson.toString(2).toByteArray(Charsets.UTF_8)
            zipEntries[BackupManager.DATABASE_ENTRY] = patchedDbBytes

            val digest = java.security.MessageDigest.getInstance("SHA-256").digest(patchedDbBytes)
                .joinToString("") { "%02x".format(it) }
            val manifestJson = org.json.JSONObject(String(zipEntries.getValue(BackupManager.MANIFEST_ENTRY), Charsets.UTF_8))
            manifestJson.getJSONObject("checksums").put(BackupManager.DATABASE_ENTRY, digest)
            zipEntries[BackupManager.MANIFEST_ENTRY] = manifestJson.toString(2).toByteArray(Charsets.UTF_8)

            patchedZipFile = File(context.cacheDir, "patched_dangling_fk_${UUID.randomUUID()}.zip")
            java.util.zip.ZipOutputStream(patchedZipFile.outputStream()).use { zos ->
                zipEntries.forEach { (name, bytes) ->
                    zos.putNextEntry(java.util.zip.ZipEntry(name))
                    zos.write(bytes)
                    zos.closeEntry()
                }
            }

            fileA.delete()
            fileB.delete()

            val restoreResult = backupManager.restoreFromZip(patchedZipFile, RestoreStrategy.REPLACE)
            assertTrue("Restore must sanitize dangling FKs instead of throwing: ${restoreResult.exceptionOrNull()}", restoreResult.isSuccess)
            assertArrayEquals("FIRST_IMAGE_BYTES".toByteArray(), fileA.readBytes())
            assertArrayEquals("SECOND_IMAGE_BYTES".toByteArray(), fileB.readBytes())

            val restoredFolders = repo.getAllFoldersDirect()
            assertEquals(1, restoredFolders.size)
            assertNull("Dangling folder parentId must be nullified", restoredFolders.first().parentId)

            val restoredNotes = repo.getAllNotesDirect()
            assertEquals(1, restoredNotes.size)
            assertNull("Dangling note folderId must be nullified", restoredNotes.first().folderId)
            assertTrue("Dangling canvas elements must be dropped", repo.getAllCanvasElementsDirect().isEmpty())
            assertTrue("Dangling annotations must be dropped", repo.getAllAnnotationsDirect().isEmpty())
            assertTrue("Dangling reminders must be dropped", repo.getAllRemindersDirect().isEmpty())
        } finally {
            zipFile?.delete()
            patchedZipFile?.delete()
            fileA.delete()
            fileB.delete()
            db.close()
        }
    }

    @Test
    fun `test CanvasItem and ImageAnnotationItem circle containsPoint matches diagonal radius`() {
        val start = androidx.compose.ui.geometry.Offset(0f, 0f)
        val end = androidx.compose.ui.geometry.Offset(100f, 100f)
        // Diagonal length is ~141.42 -> center (50, 50), radius ~70.71; point (115, 50) is distance 65 from center (inside circle)
        val insidePoint = androidx.compose.ui.geometry.Offset(115f, 50f)
        // Point (150, 50) is distance 100 from center -> outside circle even with small tolerance
        val outsidePoint = androidx.compose.ui.geometry.Offset(150f, 50f)

        val canvasCircle = CanvasItem(type = "CIRCLE", start = start, end = end)
        assertTrue(canvasCircle.containsPoint(insidePoint, tolerance = 5f))
        assertFalse(canvasCircle.containsPoint(outsidePoint, tolerance = 5f))

        val imgCircle = ImageAnnotationItem(type = "CIRCLE", start = start, end = end)
        assertTrue(imgCircle.containsPoint(insidePoint, tolerance = 5f))
        assertFalse(imgCircle.containsPoint(outsidePoint, tolerance = 5f))
    }

    @Test
    fun `test checklist JSON encode and parse round-trip preserves pasted items`() {
        val items = listOf(
            com.example.util.ChecklistItem(id = "a", text = "First pasted task", isDone = false),
            com.example.util.ChecklistItem(id = "b", text = "Second pasted task", isDone = true)
        )
        val encoded = com.example.util.SmartNoteUtils.encodeChecklistJson(items)
        val decoded = com.example.util.SmartNoteUtils.parseChecklistJson(encoded)
        assertEquals(2, decoded.size)
        assertEquals("First pasted task", decoded[0].text)
        assertFalse(decoded[0].isDone)
        assertEquals("Second pasted task", decoded[1].text)
        assertTrue(decoded[1].isDone)
    }
}
