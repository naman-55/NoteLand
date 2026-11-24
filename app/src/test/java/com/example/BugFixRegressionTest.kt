package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.AttachmentEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.NoteEntity
import com.example.data.model.NoteLinkEntity
import com.example.data.model.NoteType
import com.example.data.model.NoteVersionEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.TaskEntity
import com.example.data.repository.NoteRepository
import com.example.data.storage.AttachmentStorageManager
import com.example.security.SecurityManager
import com.example.ui.editor.engine.RichDocumentReducer
import com.example.ui.editor.model.InlineFormatStyle
import com.example.ui.editor.model.InlineSpan
import com.example.ui.editor.model.RichBlock
import com.example.ui.editor.model.RichBlockKind
import com.example.ui.editor.model.RichNoteDocument
import com.example.ui.editor.serialization.RichNoteDocumentCodec
import com.example.util.ChecklistItem
import com.example.util.SmartNoteUtils
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BugFixRegressionTest {

    private fun openInMemoryDb(): AppDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.setForeignKeyConstraintsEnabled(true)
                }
            })
            .allowMainThreadQueries()
            .build()
    }

    @Test
    fun `toggleLock encrypts title content checklist and richDocumentJson then decrypts cleanly on unlock`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = openInMemoryDb()
        try {
            val repo = NoteRepository(db, context)
            val richDoc = RichNoteDocument(
                blocks = listOf(
                    RichBlock.TextBlock(
                        id = "b1",
                        kind = RichBlockKind.PARAGRAPH,
                        text = "Secret Rich Text [[LinkedNote]]",
                        spans = listOf(InlineSpan(0, 6, InlineFormatStyle(bold = true)))
                    )
                )
            )
            val richJson = RichNoteDocumentCodec.encode(richDoc, "Secret Rich Text [[LinkedNote]]")
            val canvasJson = SmartNoteUtils.withRichDocumentJson("", richJson)
            val checklistJson = SmartNoteUtils.encodeChecklistJson(
                listOf(ChecklistItem(text = "Secret task", isDone = false))
            )

            val note = NoteEntity(
                id = "vault-regression-note",
                title = "Confidential Title",
                content = "Top secret body with [[TargetDoc]]",
                noteType = NoteType.STANDARD.name,
                checklistJson = checklistJson,
                canvasDataJson = canvasJson,
                isLocked = false
            )
            repo.insertNote(note)

            // Confirm plaintext knowledge/version state existed before locking
            assertTrue(db.noteVersionDao().getVersionsForNoteDirect(note.id).isNotEmpty())
            assertTrue(db.noteLinkDao().getOutgoingLinksDirect(note.id).isNotEmpty())

            // Lock note
            val lockRes = repo.toggleLock(note)
            assertTrue(lockRes.isSuccess)

            val lockedNote = repo.getNoteByIdDirect(note.id)!!
            assertTrue(lockedNote.isLocked)
            assertNotEquals("Confidential Title", lockedNote.title)
            assertNotEquals("Top secret body with [[TargetDoc]]", lockedNote.content)
            assertNotEquals(checklistJson, lockedNote.checklistJson)

            val lockedRichPayload = SmartNoteUtils.parseRichDocumentJson(lockedNote.canvasDataJson)
            assertTrue(lockedRichPayload.isNotBlank())
            assertFalse(lockedRichPayload.trimStart().startsWith("{"))
            assertFalse(lockedRichPayload.contains("Secret Rich Text"))

            // Confirm knowledge links and plaintext versions were purged
            assertTrue(db.noteVersionDao().getVersionsForNoteDirect(note.id).isEmpty())
            assertTrue(db.noteLinkDao().getOutgoingLinksDirect(note.id).isEmpty())

            // Unlock note
            val unlockRes = repo.toggleLock(lockedNote)
            assertTrue(unlockRes.isSuccess)

            val unlockedNote = repo.getNoteByIdDirect(note.id)!!
            assertFalse(unlockedNote.isLocked)
            assertEquals("Confidential Title", unlockedNote.title)
            assertEquals("Top secret body with [[TargetDoc]]", unlockedNote.content)
            assertEquals(checklistJson, unlockedNote.checklistJson)

            val restoredRichPayload = SmartNoteUtils.parseRichDocumentJson(unlockedNote.canvasDataJson)
            assertTrue(restoredRichPayload.trimStart().startsWith("{"))
            val decodedDoc = RichNoteDocumentCodec.decode(restoredRichPayload)
            assertNotNull(decodedDoc)
            assertEquals(
                "Secret Rich Text [[LinkedNote]]",
                (decodedDoc!!.blocks.first() as RichBlock.TextBlock).text
            )
            // Unlocking resyncs wiki links automatically
            assertTrue(db.noteLinkDao().getOutgoingLinksDirect(note.id).isNotEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `deletePermanently and emptyRecycleBin clean up reminders and linkedNoteId references`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = openInMemoryDb()
        try {
            val repo = NoteRepository(db, context)

            val noteA = NoteEntity(id = "del-note-a", title = "Note A", isDeleted = true, deletedAtUtc = 100L)
            val noteB = NoteEntity(id = "del-note-b", title = "Note B", isDeleted = true, deletedAtUtc = 100L)
            val noteKeep = NoteEntity(id = "keep-note", title = "Keep", isDeleted = false)
            db.noteDao().insertNote(noteA)
            db.noteDao().insertNote(noteB)
            db.noteDao().insertNote(noteKeep)

            val remA = ReminderEntity(id = "rem-a", noteId = noteA.id, scheduledTimeUtc = System.currentTimeMillis() + 60_000L)
            val remB = ReminderEntity(id = "rem-b", noteId = noteB.id, scheduledTimeUtc = System.currentTimeMillis() + 60_000L)
            val remKeep = ReminderEntity(id = "rem-keep", noteId = noteKeep.id, scheduledTimeUtc = System.currentTimeMillis() + 60_000L)
            db.reminderDao().insertReminder(remA)
            db.reminderDao().insertReminder(remB)
            db.reminderDao().insertReminder(remKeep)

            val taskA = TaskEntity(id = "task-a", title = "Task A", linkedNoteId = noteA.id)
            val taskB = TaskEntity(id = "task-b", title = "Task B", linkedNoteId = noteB.id)
            val taskKeep = TaskEntity(id = "task-keep", title = "Task Keep", linkedNoteId = noteKeep.id)
            db.productivityDao().insertTask(taskA)
            db.productivityDao().insertTask(taskB)
            db.productivityDao().insertTask(taskKeep)

            val focusA = FocusSessionEntity(id = "focus-a", linkedNoteId = noteA.id)
            val focusB = FocusSessionEntity(id = "focus-b", linkedNoteId = noteB.id)
            val focusKeep = FocusSessionEntity(id = "focus-keep", linkedNoteId = noteKeep.id)
            db.productivityDao().insertFocusSession(focusA)
            db.productivityDao().insertFocusSession(focusB)
            db.productivityDao().insertFocusSession(focusKeep)

            // Permanent delete of Note A
            val delARes = repo.deletePermanently(noteA.id)
            assertTrue(delARes.isSuccess)
            assertNull(repo.getNoteByIdDirect(noteA.id))
            assertNull(db.reminderDao().getReminderById(remA.id))
            assertNull(db.productivityDao().getTaskByIdDirect(taskA.id)?.linkedNoteId)
            assertNull(db.productivityDao().getFocusSessionByIdDirect(focusA.id)?.linkedNoteId)

            // Unrelated keep rows intact
            assertNotNull(db.reminderDao().getReminderById(remKeep.id))
            assertEquals(noteKeep.id, db.productivityDao().getTaskByIdDirect(taskKeep.id)?.linkedNoteId)

            // Empty recycle bin cleans up Note B
            val emptyRes = repo.emptyRecycleBin()
            assertTrue(emptyRes.isSuccess)
            assertNull(repo.getNoteByIdDirect(noteB.id))
            assertNull(db.reminderDao().getReminderById(remB.id))
            assertNull(db.productivityDao().getTaskByIdDirect(taskB.id)?.linkedNoteId)
            assertNull(db.productivityDao().getFocusSessionByIdDirect(focusB.id)?.linkedNoteId)

            // Kept note & references still intact
            assertNotNull(repo.getNoteByIdDirect(noteKeep.id))
            assertNotNull(db.reminderDao().getReminderById(remKeep.id))
            assertEquals(noteKeep.id, db.productivityDao().getTaskByIdDirect(taskKeep.id)?.linkedNoteId)
            assertEquals(noteKeep.id, db.productivityDao().getFocusSessionByIdDirect(focusKeep.id)?.linkedNoteId)
        } finally {
            db.close()
        }
    }

    @Test
    fun `vault attachment encryption decrypts cleanly to temp viewer cache and preserves vault file on temp cleanup`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storageManager = AttachmentStorageManager(context)
        val securityManager = SecurityManager(context)
        val pdfBytes = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF".toByteArray(Charsets.UTF_8)

        val saveRes = storageManager.saveAttachmentWithDedup(
            fileName = "secret_contract.pdf",
            mimeType = "application/pdf",
            inputStream = ByteArrayInputStream(pdfBytes),
            expectPdf = true,
            validateFormatHeaders = true
        )
        assertTrue(saveRes.isSuccess)
        val plainAttachment = saveRes.getOrThrow().attachment

        val encRes = storageManager.encryptExistingFileToVault(plainAttachment.relativePath)
        assertTrue(encRes.isSuccess)
        val encRelPath = encRes.getOrThrow()
        assertTrue(encRelPath.startsWith("vault_files/"))

        val vaultAttachment = plainAttachment.copy(relativePath = encRelPath, isLocked = true)
        val vaultFile = storageManager.getFileFromRelativePath(encRelPath)
        assertTrue(vaultFile.exists())
        assertTrue(securityManager.isEncryptedVaultFile(vaultFile))

        // Decrypt via AttachmentEntity
        val tempFromEntity = storageManager.createTempDecryptedFileForViewer(vaultAttachment)
        assertNotNull(tempFromEntity)
        assertNotEquals(vaultFile.absolutePath, tempFromEntity!!.absolutePath)
        assertArrayEquals(pdfBytes, tempFromEntity.readBytes())

        // Decrypt via path fallback
        val tempFromPath = storageManager.createTempDecryptedFileForViewerByPath(vaultFile.absolutePath, "pdf")
        assertNotNull(tempFromPath)
        assertArrayEquals(pdfBytes, tempFromPath!!.readBytes())

        // Cleanup temp viewer files does NOT delete vault encrypted file
        storageManager.deleteTempViewerFile(tempFromEntity)
        storageManager.deleteTempViewerFile(tempFromPath)
        assertFalse(tempFromEntity.exists())
        assertFalse(tempFromPath.exists())
        assertTrue(vaultFile.exists())

        // Clean up test artifact
        storageManager.deleteFile(plainAttachment.relativePath)
        storageManager.deleteFile(encRelPath)
    }

    @Test
    fun `rapid ChecklistItem instantiations produce unique IDs`() {
        val items = (1..50).map { ChecklistItem(text = "Task $it") }
        val uniqueIds = items.map { it.id }.toSet()
        assertEquals(50, uniqueIds.size)
        assertTrue(uniqueIds.all { UUID.fromString(it) != null })
    }

    @Test
    fun `RichDocumentReducer splitBlockOnEnter after selection deletion splits cleanly without stale spaces`() {
        val block = RichBlock.TextBlock(
            id = "b1",
            kind = RichBlockKind.BULLET_LIST,
            text = "Alpha Beta Gamma",
            spans = listOf(InlineSpan(0, 5, InlineFormatStyle(bold = true)))
        )
        val doc = RichNoteDocument(blocks = listOf(block))

        // Simulate replacing selection [6..11) ("Beta ") with '\n' and stripping '\n' before splitBlockOnEnter(6)
        val afterSelectionDeleteText = "Alpha Gamma"
        val updatedBeforeSplit = doc.copy(
            blocks = listOf(block.copy(text = afterSelectionDeleteText))
        )

        val splitRes = RichDocumentReducer.splitBlockOnEnter(
            document = updatedBeforeSplit,
            blockId = "b1",
            cursorOffset = 6
        )
        assertEquals(2, splitRes.document.blocks.size)
        val first = splitRes.document.blocks[0] as RichBlock.TextBlock
        val second = splitRes.document.blocks[1] as RichBlock.TextBlock

        assertEquals("Alpha ", first.text)
        assertEquals("Gamma", second.text)
        assertEquals(RichBlockKind.BULLET_LIST, second.kind)
        assertEquals(1, first.spans.size)
        assertEquals(0, first.spans.first().start)
        assertEquals(5, first.spans.first().end)
    }
}
