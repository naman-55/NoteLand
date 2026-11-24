package com.example

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.AttachmentEntity
import com.example.data.model.BookmarkEntity
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.data.model.NoteType
import com.example.data.repository.NoteRepository
import com.example.data.storage.AttachmentStorageManager
import com.example.security.SecurityManager
import com.example.ui.screens.PdfHighlightSegment
import com.example.ui.screens.PdfPageAnnotation
import com.example.ui.screens.parsePdfAnnotation
import com.example.util.FolderHierarchyUtils
import com.example.util.PdfDocumentEngine
import com.example.util.PdfValidationResult
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.Deflater
import kotlinx.coroutines.flow.first
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
class DocumentAndFolderManagementTest {

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

    /**
     * Constructs a valid 2-page PDF with real object table, /Pages tree, and FlateDecode + plain text streams.
     */
    private fun buildSampleTwoPagePdfBytes(page1Text: String, page2Text: String): ByteArray {
        val deflater = Deflater()
        val p1Script = "BT /F1 12 Tf 72 720 Td ($page1Text) Tj ET".toByteArray(Charsets.ISO_8859_1)
        deflater.setInput(p1Script)
        deflater.finish()
        val compressedOut = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        while (!deflater.finished()) {
            val count = deflater.deflate(buf)
            compressedOut.write(buf, 0, count)
        }
        deflater.end()
        val p1FlateBytes = compressedOut.toByteArray()

        val p2Script = "BT /F1 12 Tf 72 720 Td ($page2Text) Tj ET"

        val header = "%PDF-1.4\n" +
            "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n" +
            "2 0 obj << /Type /Pages /Kids [3 0 R 4 0 R] /Count 2 >> endobj\n" +
            "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 5 0 R >> endobj\n" +
            "4 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 6 0 R >> endobj\n" +
            "5 0 obj << /Length ${p1FlateBytes.size} /Filter /FlateDecode >>\nstream\n"

        val mid = "\nendstream\nendobj\n" +
            "6 0 obj << /Length ${p2Script.length} >>\nstream\n" +
            p2Script +
            "\nendstream\nendobj\n%%EOF\n"

        return ByteArrayOutputStream().apply {
            write(header.toByteArray(Charsets.ISO_8859_1))
            write(p1FlateBytes)
            write(mid.toByteArray(Charsets.ISO_8859_1))
        }.toByteArray()
    }

    @Test
    fun `folder CRUD, nested hierarchy, cycle prevention, and document organization work end to end`(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = openInMemoryDb()
        val storage = AttachmentStorageManager(context)
        try {
            val repo = NoteRepository(db, context)

            val rootFolder = FolderEntity(id = "f-work", name = "Work", parentId = null)
            val subFolder = FolderEntity(id = "f-projects", name = "Projects", parentId = rootFolder.id)
            val deepFolder = FolderEntity(id = "f-noteland", name = "NoteLand", parentId = subFolder.id)
            repo.insertFolder(rootFolder)
            repo.insertFolder(subFolder)
            repo.insertFolder(deepFolder)

            // Verify breadcrumbs and path string
            val allFolders = repo.folders.first()
            assertEquals("Work / Projects / NoteLand", FolderHierarchyUtils.getFolderPathString(allFolders, deepFolder.id))
            assertEquals("Work / Projects", FolderHierarchyUtils.getParentPathString(allFolders, deepFolder.id))

            // Cycle prevention: moving Work inside its descendant NoteLand must fail
            val cycleRes = repo.moveFolder(rootFolder.id, deepFolder.id)
            assertTrue(cycleRes.isFailure)
            assertTrue(FolderHierarchyUtils.wouldCreateCycle(allFolders, rootFolder.id, deepFolder.id))

            // Save a PDF into deepFolder (zero notes inside Work/Projects/NoteLand at this point)
            val pdfBytes = buildSampleTwoPagePdfBytes("Quarterly architecture plan", "Appendix")
            val att = storage.saveAttachmentWithDedup(
                fileName = "Architecture.pdf",
                mimeType = "application/pdf",
                inputStream = ByteArrayInputStream(pdfBytes),
                folderId = deepFolder.id,
                expectPdf = true,
                validateFormatHeaders = true
            ).getOrThrow().attachment
            repo.saveAttachment(att)

            val allNotes = repo.activeNotes.first()
            val allAtts = repo.attachments.first()

            // Parent rootFolder must count as active via subtree document even with 0 notes
            assertTrue(FolderHierarchyUtils.isFolderOrSubtreeActive(allFolders, allNotes, allAtts, rootFolder.id))
            assertEquals(0, FolderHierarchyUtils.countDirectAttachments(allAtts, rootFolder.id))
            assertEquals(1, FolderHierarchyUtils.countSubtreeAttachments(allFolders, allAtts, rootFolder.id))
            val treeNodes = FolderHierarchyUtils.flattenFolderTree(allFolders, allNotes, attachments = allAtts)
            val rootNode = treeNodes.first { it.folder.id == rootFolder.id }
            assertEquals(1, rootNode.totalDocumentCount)

            // Move document to rootFolder and add a note in subFolder
            repo.moveAttachmentToFolder(att.id, rootFolder.id)
            val movedAtt = repo.getAttachmentById(att.id)!!
            assertEquals(rootFolder.id, movedAtt.folderId)

            val note = NoteEntity(
                id = "note-proj-1",
                title = "Sprint Notes",
                content = "Review architecture",
                folderId = subFolder.id,
                folderName = subFolder.name
            )
            repo.saveOrUpdateNote(note)

            // Rename subFolder updates child note's cached folderName
            repo.updateFolder(subFolder.copy(name = "Engineering"))
            assertEquals("Engineering", repo.getNoteByIdDirect(note.id)!!.folderName)

            // Deleting intermediate parent subFolder promotes deepFolder to rootFolder and keeps note & doc safely
            repo.deleteFolder(subFolder.id)
            val remainingFolders = repo.folders.first()
            assertNull(remainingFolders.firstOrNull { it.id == subFolder.id })
            assertEquals(rootFolder.id, remainingFolders.first { it.id == deepFolder.id }.parentId)
            assertNull(repo.getNoteByIdDirect(note.id)!!.folderId)
            assertEquals(rootFolder.id, repo.getAttachmentById(att.id)!!.folderId)

            storage.deleteFile(att.relativePath)
        } finally {
            db.close()
        }
    }

    @Test
    fun `special folders handle favorites, archive, trash recovery, permanent delete, and retention policy purge`(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = openInMemoryDb()
        val storage = AttachmentStorageManager(context)
        try {
            val repo = NoteRepository(db, context)
            val pdfBytes = buildSampleTwoPagePdfBytes("Retention Policy Test", "Page 2")

            val attExpired = storage.saveAttachmentWithDedup(
                fileName = "expired_doc.pdf",
                mimeType = "application/pdf",
                inputStream = ByteArrayInputStream(pdfBytes),
                expectPdf = true,
                validateFormatHeaders = true
            ).getOrThrow().attachment
            val attRecent = storage.saveAttachmentWithDedup(
                fileName = "recent_doc.pdf",
                mimeType = "application/pdf",
                inputStream = ByteArrayInputStream(buildSampleTwoPagePdfBytes("Recent Draft", "Page 2")),
                expectPdf = true,
                validateFormatHeaders = true
            ).getOrThrow().attachment

            repo.saveAttachment(attExpired)
            repo.saveAttachment(attRecent)

            // Toggle Favorite & verify
            repo.toggleAttachmentFavorite(attRecent)
            assertTrue(repo.favoriteAttachments.first().any { it.id == attRecent.id })

            // Toggle Archive & Restore
            repo.toggleAttachmentArchive(attRecent)
            assertTrue(repo.archivedAttachments.first().any { it.id == attRecent.id })
            assertFalse(repo.attachments.first().any { it.id == attRecent.id })
            repo.toggleAttachmentArchive(repo.getAttachmentById(attRecent.id)!!)
            assertTrue(repo.attachments.first().any { it.id == attRecent.id })

            // Soft Delete + Restore for note & attachment
            val noteRecent = NoteEntity(id = "trash-note-recent", title = "Recent Trash Note", content = "Keep")
            repo.saveOrUpdateNote(noteRecent)
            repo.softDeleteNote(noteRecent)
            assertTrue(repo.deletedNotes.first().any { it.id == noteRecent.id })
            repo.restoreNote(repo.getNoteByIdDirect(noteRecent.id)!!)
            assertFalse(repo.deletedNotes.first().any { it.id == noteRecent.id })
            assertTrue(repo.activeNotes.first().any { it.id == noteRecent.id })

            // Retention Policy Purge (7 days): expired note & expired attachment are permanently purged; 1-day old trash stays
            val now = System.currentTimeMillis()
            val tenDaysAgo = now - 10 * 86_400_000L
            val oneDayAgo = now - 1 * 86_400_000L

            val expiredNote = NoteEntity(
                id = "trash-note-expired",
                title = "Old Note",
                isDeleted = true,
                deletedAtUtc = tenDaysAgo
            )
            val recentTrashedNote = NoteEntity(
                id = "trash-note-keep",
                title = "Recent Trashed Note",
                isDeleted = true,
                deletedAtUtc = oneDayAgo
            )
            db.noteDao().insertNote(expiredNote)
            db.noteDao().insertNote(recentTrashedNote)

            db.attachmentDao().updateAttachment(attExpired.copy(isDeleted = true, deletedAtUtc = tenDaysAgo))
            db.attachmentDao().updateAttachment(attRecent.copy(isDeleted = true, deletedAtUtc = oneDayAgo))

            val expiredFile = storage.getFileFromRelativePath(attExpired.relativePath)
            val recentFile = storage.getFileFromRelativePath(attRecent.relativePath)
            assertTrue(expiredFile.exists())
            assertTrue(recentFile.exists())

            val purged = repo.purgeExpiredTrashItems(retentionDays = 7).getOrThrow()
            assertEquals(2, purged)
            assertNull(repo.getNoteByIdDirect(expiredNote.id))
            assertNull(repo.getAttachmentById(attExpired.id))
            assertFalse(expiredFile.exists())

            assertNotNull(repo.getNoteByIdDirect(recentTrashedNote.id))
            assertNotNull(repo.getAttachmentById(attRecent.id))
            assertTrue(recentFile.exists())

            // Permanent delete cleans up recent trashed attachment + file
            repo.deleteAttachmentPermanently(attRecent.id)
            assertNull(repo.getAttachmentById(attRecent.id))
            assertFalse(recentFile.exists())
        } finally {
            db.close()
        }
    }

    @Test
    fun `vault PIN lockout, attachment encryption roundtrip, and security file validation enforce protection`(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = openInMemoryDb()
        val storage = AttachmentStorageManager(context)
        val security = SecurityManager(context)
        try {
            val repo = NoteRepository(db, context)

            // PIN verification and failed-attempt lockout throttling
            security.setVaultPin("2580")
            assertFalse(security.isVaultSessionUnlocked())
            repeat(SecurityManager.MAX_FAILED_ATTEMPTS) {
                assertFalse(security.verifyPin("0000"))
            }
            assertTrue(security.getRemainingLockoutSeconds() > 0)
            // Even correct PIN is blocked during active lockout
            assertFalse(security.verifyPin("2580"))
            // Reset PIN clears lockout & allows unlock
            security.setVaultPin("2580")
            assertEquals(0, security.getRemainingLockoutSeconds())
            assertTrue(security.verifyPin("2580"))
            assertTrue(security.isVaultSessionUnlocked())

            // Executable extension & ELF disguised header rejection
            val apkReject = storage.saveAttachmentWithDedup(
                fileName = "payload.apk",
                mimeType = "application/vnd.android.package-archive",
                inputStream = ByteArrayInputStream("PK\u0003\u0004dummy".toByteArray()),
                validateFormatHeaders = true
            )
            assertTrue(apkReject.isFailure)

            val elfHeader = byteArrayOf(0x7F, 0x45, 0x4C, 0x46, 0x02, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
            val disguisedElf = storage.saveAttachmentWithDedup(
                fileName = "fake_statement.pdf",
                mimeType = "application/pdf",
                inputStream = ByteArrayInputStream(elfHeader),
                expectPdf = true,
                validateFormatHeaders = true
            )
            assertTrue(disguisedElf.isFailure)

            // Non-PDF / corrupted header import rejection leaves no orphan file
            val beforeFiles = File(context.filesDir, "attachments").listFiles()?.map { it.name }?.toSet().orEmpty()
            val badHeaderPdf = storage.saveAttachmentWithDedup(
                fileName = "broken.pdf",
                mimeType = "application/pdf",
                inputStream = ByteArrayInputStream("<html>not actually a pdf document</html>".toByteArray()),
                expectPdf = true,
                validateFormatHeaders = true
            )
            assertTrue(badHeaderPdf.isFailure)
            val afterFiles = File(context.filesDir, "attachments").listFiles()?.map { it.name }?.toSet().orEmpty()
            assertEquals(beforeFiles, afterFiles)

            // Structurally truncated PDF detected gracefully by PdfDocumentEngine
            val truncatedPdfFile = File(context.cacheDir, "truncated_struct.pdf").apply {
                writeText("%PDF-1.4\ntruncated without object structure", Charsets.UTF_8)
            }
            assertTrue(PdfDocumentEngine.validatePdfFile(truncatedPdfFile) is PdfValidationResult.Corrupted)
            truncatedPdfFile.delete()

            // Lock/Unlock Attachment AES-256-GCM roundtrip via repository
            val validPdfBytes = buildSampleTwoPagePdfBytes("Secret Patent Filing", "Confidential Claims")
            val imported = storage.saveAttachmentWithDedup(
                fileName = "patent.pdf",
                mimeType = "application/pdf",
                inputStream = ByteArrayInputStream(validPdfBytes),
                expectPdf = true,
                validateFormatHeaders = true
            ).getOrThrow().attachment
            repo.saveAttachment(imported)

            val lockedAtt = repo.setAttachmentLocked(imported, locked = true, storageManager = storage).getOrThrow()
            assertTrue(lockedAtt.isLocked)
            assertTrue(lockedAtt.relativePath.startsWith("vault_files/"))
            val vaultFile = storage.getFileFromRelativePath(lockedAtt.relativePath)
            assertTrue(security.isEncryptedVaultFile(vaultFile))
            assertFalse(storage.getFileFromRelativePath(imported.relativePath).exists())

            val unlockedAtt = repo.setAttachmentLocked(lockedAtt, locked = false, storageManager = storage).getOrThrow()
            assertFalse(unlockedAtt.isLocked)
            assertTrue(unlockedAtt.relativePath.startsWith("attachments/"))
            val restoredFile = storage.getFileFromRelativePath(unlockedAtt.relativePath)
            assertArrayEquals(validPdfBytes, restoredFile.readBytes())
            assertFalse(vaultFile.exists())

            storage.deleteFile(unlockedAtt.relativePath)
        } finally {
            db.close()
        }
    }

    @Test
    fun `PDF text extraction, in-PDF search, linked note annotations, bookmark CRUD, and annotated export preserve original`(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = openInMemoryDb()
        val storage = AttachmentStorageManager(context)
        try {
            val repo = NoteRepository(db, context)
            val pdfBytes = buildSampleTwoPagePdfBytes(
                page1Text = "NoteLand Architecture Blueprint",
                page2Text = "Zero Trust Vault Encryption Details"
            )
            val pdfAtt = storage.saveAttachmentWithDedup(
                fileName = "Blueprint.pdf",
                mimeType = "application/pdf",
                inputStream = ByteArrayInputStream(pdfBytes),
                expectPdf = true,
                validateFormatHeaders = true
            ).getOrThrow().attachment
            repo.saveAttachment(pdfAtt)
            val sourceFile = storage.getFileFromRelativePath(pdfAtt.relativePath)
            val originalSha = storage.computeSha256(sourceFile)

            // Structural validation
            val validation = PdfDocumentEngine.validatePdfFile(sourceFile)
            assertTrue(validation is PdfValidationResult.Valid)
            assertTrue((validation as PdfValidationResult.Valid).pageCount >= 2)

            // Corrupted file returns Corrupted validation
            val badFile = File(context.cacheDir, "corrupt_check.pdf").apply {
                writeText("NOT A PDF", Charsets.UTF_8)
            }
            assertTrue(PdfDocumentEngine.validatePdfFile(badFile) is PdfValidationResult.Corrupted)
            badFile.delete()

            // Page text extraction (handles FlateDecode page 1 + uncompressed page 2)
            val extractedTexts = PdfDocumentEngine.extractPdfPageTexts(sourceFile)
            assertTrue(extractedTexts[1].orEmpty().contains("Blueprint"))
            assertTrue(extractedTexts[2].orEmpty().contains("Encryption"))

            // Create note linked to Page 2
            val linkedNote = NoteEntity(
                id = "note-from-pdf-1",
                title = "Vault Takeaways",
                content = "Zero Trust Vault Encryption Details\n\n[pdf_ref:${pdfAtt.id}#p2]"
            )
            repo.saveOrUpdateNote(linkedNote)

            // Persist highlight + underline + sticky note with bidirectional linkedNoteId
            val highlightAnn = PdfPageAnnotation(
                id = "ann-hl-1",
                type = "HIGHLIGHT",
                pageNumber = 1,
                start = Offset(72f, 100f),
                end = Offset(420f, 130f),
                colorArgb = 0x7AFEF08A,
                strokeWidth = 2f,
                segments = listOf(PdfHighlightSegment(72f, 100f, 420f, 130f)),
                linkedNoteId = linkedNote.id
            )
            val stickyAnn = PdfPageAnnotation(
                id = "ann-sticky-2",
                type = "COMMENT",
                pageNumber = 2,
                start = Offset(200f, 300f),
                end = Offset(240f, 160f),
                content = "Review key rotation schedule",
                colorArgb = android.graphics.Color.YELLOW,
                strokeWidth = 2f,
                linkedNoteId = linkedNote.id
            )
            val entities = listOf(highlightAnn.toEntity(pdfAtt.id), stickyAnn.toEntity(pdfAtt.id))
            repo.saveAttachmentWithAnnotations(pdfAtt, entities)

            val loadedParsed = repo.getAnnotationsForAttachmentDirect(pdfAtt.id).mapNotNull { parsePdfAnnotation(it) }
            assertEquals(2, loadedParsed.size)
            assertEquals(linkedNote.id, loadedParsed.first { it.id == "ann-hl-1" }.linkedNoteId)
            assertEquals("Review key rotation schedule", loadedParsed.first { it.id == "ann-sticky-2" }.content)

            // Search inside PDF text + annotations
            val textMatches = PdfDocumentEngine.searchPdfDocumentAndAnnotations(
                pdfFile = sourceFile,
                query = "Encryption",
                annotations = loadedParsed,
                preExtractedPages = extractedTexts
            )
            assertEquals(1, textMatches.size)
            assertEquals(2, textMatches.first().pageNumber)
            assertEquals("PAGE_TEXT", textMatches.first().sourceType)

            val annMatches = PdfDocumentEngine.searchPdfDocumentAndAnnotations(
                pdfFile = sourceFile,
                query = "rotation schedule",
                annotations = loadedParsed,
                preExtractedPages = extractedTexts
            )
            assertEquals(1, annMatches.size)
            assertEquals(2, annMatches.first().pageNumber)
            assertEquals("ANNOTATION", annMatches.first().sourceType)

            // Bookmark insert, rename, delete
            val bookmark = BookmarkEntity(
                id = "bm-pdf-p2",
                targetType = "PDF_PAGE",
                targetId = "${pdfAtt.id}#2",
                label = "Page 2"
            )
            repo.insertBookmark(bookmark)
            repo.updateBookmark(bookmark.copy(label = "Encryption Spec"))
            val updatedBm = repo.getAllBookmarks().first().first { it.id == bookmark.id }
            assertEquals("Encryption Spec", updatedBm.label)
            repo.deleteBookmark(updatedBm)
            assertTrue(repo.getAllBookmarks().first().none { it.id == bookmark.id })

            // Export annotated PDF to separate file without modifying original
            val exportedFile = File(context.cacheDir, "Blueprint_annotated.pdf")
            val exportRes = PdfDocumentEngine.exportAnnotatedPdfToFile(sourceFile, loadedParsed, exportedFile)
            assertTrue(exportRes.isSuccess)
            assertTrue(exportedFile.exists() && exportedFile.length() > 100L)
            assertNotEquals(sourceFile.absolutePath, exportedFile.absolutePath)
            assertEquals(originalSha, storage.computeSha256(sourceFile))

            exportedFile.delete()
            storage.deleteFile(pdfAtt.relativePath)
        } finally {
            db.close()
        }
    }

    @Test
    fun `duplicate file imports deduplicate storage and reference counting preserves shared file`(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = openInMemoryDb()
        val storage = AttachmentStorageManager(context)
        try {
            val repo = NoteRepository(db, context)
            val identicalBytes = buildSampleTwoPagePdfBytes("Dedup Document Content", "Same Hash")

            val firstOutcome = storage.saveAttachmentWithDedupSuspend(
                fileName = "FirstCopy.pdf",
                mimeType = "application/pdf",
                inputStream = ByteArrayInputStream(identicalBytes),
                expectPdf = true,
                validateFormatHeaders = true,
                existingByHashLookup = { hash -> repo.getAttachmentByHash(hash) }
            ).getOrThrow()
            assertFalse(firstOutcome.deduplicatedFromExisting)
            repo.saveAttachment(firstOutcome.attachment)

            val secondOutcome = storage.saveAttachmentWithDedupSuspend(
                fileName = "SecondCopy.pdf",
                mimeType = "application/pdf",
                inputStream = ByteArrayInputStream(identicalBytes),
                expectPdf = true,
                validateFormatHeaders = true,
                existingByHashLookup = { hash -> repo.getAttachmentByHash(hash) }
            ).getOrThrow()
            assertTrue(secondOutcome.deduplicatedFromExisting)
            assertEquals(firstOutcome.attachment.relativePath, secondOutcome.attachment.relativePath)
            repo.saveAttachment(secondOutcome.attachment)

            val sharedFile = storage.getFileFromRelativePath(firstOutcome.attachment.relativePath)
            assertTrue(sharedFile.exists())

            // Permanently deleting first reference keeps shared physical file intact for second reference
            repo.deleteAttachmentPermanently(firstOutcome.attachment.id)
            assertTrue(sharedFile.exists())

            // Permanently deleting last reference deletes physical file
            repo.deleteAttachmentPermanently(secondOutcome.attachment.id)
            assertFalse(sharedFile.exists())
        } finally {
            db.close()
        }
    }
}
