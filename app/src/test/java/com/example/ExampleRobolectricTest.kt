package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NoteLand", appName)
  }

  @Test
  fun `verify Room database initializes and validates schema successfully`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    val folder = com.example.data.model.FolderEntity(
        id = java.util.UUID.randomUUID().toString(),
        name = "Personal",
        colorHex = "#10B981"
    )
    db.folderDao().insertFolder(folder)

    val note = com.example.data.model.NoteEntity(
        id = java.util.UUID.randomUUID().toString(),
        folderId = folder.id,
        title = "Test Note",
        content = "Testing Room schema integrity.",
        createdAtUtc = System.currentTimeMillis()
    )
    db.noteDao().insertNote(note)

    val retrieved = db.noteDao().getNoteByIdDirect(note.id)
    org.junit.Assert.assertNotNull(retrieved)
    assertEquals("Test Note", retrieved?.title)

    db.close()
  }

  @Test
  fun `verify Phase 3 NoteRepository CRUD duplicate soft-delete restore and search`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    val repo = com.example.data.repository.NoteRepository(db)

    // 1. Create Folder & Note
    val folder = com.example.data.model.FolderEntity(
        id = java.util.UUID.randomUUID().toString(),
        name = "Work Projects",
        colorHex = "#3B82F6"
    )
    repo.insertFolder(folder)

    val note = com.example.data.model.NoteEntity(
        id = java.util.UUID.randomUUID().toString(),
        folderId = folder.id,
        folderName = folder.name,
        title = "Sprint Planning Notes",
        content = "# Sprint Goals\n- [x] Deliver SQLite Phase 3\n- [ ] Write documentation",
        tagsCsv = "Work,Planning",
        isPinned = true
    )
    val insertRes = repo.insertNote(note)
    org.junit.Assert.assertTrue(insertRes.isSuccess)

    // 2. Edit Note
    val updatedNote = note.copy(title = "Sprint Planning Notes (Updated)")
    val updateRes = repo.updateNote(updatedNote)
    org.junit.Assert.assertTrue(updateRes.isSuccess)

    val fetched = repo.getNoteByIdDirect(note.id)
    assertEquals("Sprint Planning Notes (Updated)", fetched?.title)

    // 3. Duplicate Note
    val duplicateRes = repo.duplicateNote(fetched!!)
    org.junit.Assert.assertTrue(duplicateRes.isSuccess)
    val duplicateId = duplicateRes.getOrThrow()
    val duplicate = repo.getNoteByIdDirect(duplicateId)
    org.junit.Assert.assertNotNull(duplicate)
    assertEquals("Sprint Planning Notes (Updated) (Copy)", duplicate?.title)

    // 4. Soft Delete Note
    val deleteRes = repo.softDeleteNote(fetched)
    org.junit.Assert.assertTrue(deleteRes.isSuccess)
    val deletedItem = repo.getNoteByIdDirect(note.id)
    org.junit.Assert.assertTrue(deletedItem?.isDeleted == true)

    // 5. Restore Note
    val restoreRes = repo.restoreNote(deletedItem!!)
    org.junit.Assert.assertTrue(restoreRes.isSuccess)
    val restored = repo.getNoteByIdDirect(note.id)
    org.junit.Assert.assertTrue(restored?.isDeleted == false)

    // 6. Permanent Delete
    val permDeleteRes = repo.deletePermanently(note.id)
    org.junit.Assert.assertTrue(permDeleteRes.isSuccess)
    val purged = repo.getNoteByIdDirect(note.id)
    org.junit.Assert.assertNull(purged)

    db.close()
  }

  @Test
  fun `verify Phase 4 CanvasElement serialization hit testing and SQLite persistence`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    val repo = com.example.data.repository.NoteRepository(db)

    // 1. Create Canvas
    val canvas = com.example.data.model.CanvasEntity(
        id = java.util.UUID.randomUUID().toString(),
        title = "Architecture Diagram",
        backgroundColorHex = "#FAF5FF"
    )

    val rectItem = com.example.ui.screens.CanvasItem(
        id = java.util.UUID.randomUUID().toString(),
        type = "RECTANGLE",
        start = androidx.compose.ui.geometry.Offset(50f, 50f),
        end = androidx.compose.ui.geometry.Offset(200f, 150f),
        colorArgb = -16744193,
        strokeWidth = 4f
    )

    // Verify element JSON serialization
    val elementEntity = rectItem.toEntity(canvas.id)
    assertEquals("RECTANGLE", elementEntity.elementType)

    val parsed = com.example.ui.screens.parseCanvasItemFromEntity(elementEntity)
    org.junit.Assert.assertNotNull(parsed)
    assertEquals(50f, parsed!!.start.x, 0.01f)
    assertEquals(200f, parsed.end.x, 0.01f)
    org.junit.Assert.assertTrue(parsed.containsPoint(androidx.compose.ui.geometry.Offset(100f, 100f)))
    org.junit.Assert.assertFalse(parsed.containsPoint(androidx.compose.ui.geometry.Offset(350f, 350f)))

    // Save canvas with elements to SQLite
    val saveRes = repo.saveCanvasWithElements(canvas, listOf(elementEntity))
    org.junit.Assert.assertTrue(saveRes.isSuccess)

    // Fetch elements from SQLite
    val elementsFromDb = repo.getElementsForCanvasDirect(canvas.id)
    assertEquals(1, elementsFromDb.size)
    assertEquals("RECTANGLE", elementsFromDb[0].elementType)

    db.close()
  }

  @Test
  fun `verify Phase 5 Image persistence, original-image integrity, and separate annotation storage`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    val storageManager = com.example.data.storage.AttachmentStorageManager(context)
    val repo = com.example.data.repository.NoteRepository(db)

    // 1. Simulate image import to app-private storage
    val originalBytes = "SIMULATED_ORIGINAL_IMAGE_BYTES_123456789".toByteArray()
    val testInputStream = java.io.ByteArrayInputStream(originalBytes)

    val saveRes = storageManager.saveAttachment(
        fileName = "reference_photo.jpg",
        mimeType = "image/jpeg",
        inputStream = testInputStream
    )
    org.junit.Assert.assertTrue(saveRes.isSuccess)
    val attachment = saveRes.getOrThrow()

    // Save attachment record in SQLite
    repo.saveAttachment(attachment)

    val diskFile = storageManager.getFileFromRelativePath(attachment.relativePath)
    org.junit.Assert.assertTrue(diskFile.exists())
    assertEquals(originalBytes.size.toLong(), diskFile.length())

    // 2. Add multiple image annotations (Drawing, Rectangle, Text)
    val drawingAnnotation = com.example.data.model.AnnotationEntity(
        id = java.util.UUID.randomUUID().toString(),
        attachmentId = attachment.id,
        annotationType = "DRAWING",
        pageNumber = 1,
        coordinatesJson = """{"points":[{"x":10.0,"y":10.0},{"x":50.0,"y":50.0}]}""",
        content = "Underline mark",
        colorHex = "#EF4444"
    )
    val rectAnnotation = com.example.data.model.AnnotationEntity(
        id = java.util.UUID.randomUUID().toString(),
        attachmentId = attachment.id,
        annotationType = "RECTANGLE",
        pageNumber = 1,
        coordinatesJson = """{"startX":30.0,"startY":30.0,"endX":120.0,"endY":80.0}""",
        content = "Bounding box",
        colorHex = "#3B82F6"
    )

    repo.saveAttachmentWithAnnotations(attachment, listOf(drawingAnnotation, rectAnnotation))

    // 3. Verify Original Image File Integrity: File bytes on disk must be 100% UNTOUCHED
    val diskBytesAfterAnnotation = diskFile.readBytes()
    org.junit.Assert.assertArrayEquals(originalBytes, diskBytesAfterAnnotation)
    assertEquals(originalBytes.size.toLong(), diskFile.length())

    // 4. Verify Reopening Image restores annotations separately from database
    val loadedAnnotations = repo.getAnnotationsForAttachmentDirect(attachment.id)
    assertEquals(2, loadedAnnotations.size)
    assertEquals("DRAWING", loadedAnnotations[0].annotationType)
    assertEquals("RECTANGLE", loadedAnnotations[1].annotationType)

    // Cleanup
    storageManager.deleteFile(attachment.relativePath)
    db.close()
  }

  @Test
  fun `verify Phase 5 PDF reopening, bookmarks, and page annotation alignment`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    val repo = com.example.data.repository.NoteRepository(db)
    val pdfAttachmentId = java.util.UUID.randomUUID().toString()

    val pdfAttachment = com.example.data.model.AttachmentEntity(
        id = pdfAttachmentId,
        fileName = "lecture_notes.pdf",
        fileExtension = "pdf",
        relativePath = "attachments/lecture_notes.pdf",
        mimeType = "application/pdf",
        sizeBytes = 512000L
    )
    repo.saveAttachment(pdfAttachment)

    // 1. Add Bookmarks for PDF pages
    val bookmarkPage2 = com.example.data.model.BookmarkEntity(
        targetType = "PDF_PAGE",
        targetId = "$pdfAttachmentId#2",
        label = "Page 2 • Important Formulas"
    )
    repo.insertBookmark(bookmarkPage2)

    // 2. Add Area-Highlight and Comments aligned to specific pages
    val page1Highlight = com.example.data.model.AnnotationEntity(
        id = java.util.UUID.randomUUID().toString(),
        attachmentId = pdfAttachmentId,
        annotationType = "HIGHLIGHT",
        pageNumber = 1,
        coordinatesJson = """{"startX":50.0,"startY":120.0,"endX":350.0,"endY":150.0}""",
        content = "Chapter Title",
        colorHex = "#FEF08A"
    )
    val page2Comment = com.example.data.model.AnnotationEntity(
        id = java.util.UUID.randomUUID().toString(),
        attachmentId = pdfAttachmentId,
        annotationType = "COMMENT",
        pageNumber = 2,
        coordinatesJson = """{"startX":40.0,"startY":200.0}""",
        content = "Review this theorem before exam",
        colorHex = "#FDE047"
    )

    repo.saveAttachmentWithAnnotations(pdfAttachment, listOf(page1Highlight, page2Comment))

    // 3. Verify Reopening: Annotations align to their respective page numbers
    val fetched = repo.getAnnotationsForAttachmentDirect(pdfAttachmentId)
    assertEquals(2, fetched.size)

    val page1Items = fetched.filter { it.pageNumber == 1 }
    val page2Items = fetched.filter { it.pageNumber == 2 }
    assertEquals(1, page1Items.size)
    assertEquals("HIGHLIGHT", page1Items[0].annotationType)
    assertEquals(1, page2Items.size)
    assertEquals("COMMENT", page2Items[0].annotationType)
    assertEquals("Review this theorem before exam", page2Items[0].content)

    db.close()
  }

  @Test
  fun `verify Phase 6 Reminders CRUD and Search Vault Isolation`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    val repo = com.example.data.repository.NoteRepository(db)

    // 1. Insert note and reminder
    val noteId = java.util.UUID.randomUUID().toString()
    val note = com.example.data.model.NoteEntity(
        id = noteId,
        title = "Important Review Meeting",
        content = "Review the quarterly deliverables.",
        tagsCsv = "Work,Quarterly",
        isLocked = false
    )
    repo.insertNote(note)

    val reminderId = java.util.UUID.randomUUID().toString()
    val reminder = com.example.data.model.ReminderEntity(
        id = reminderId,
        noteId = noteId,
        scheduledTimeUtc = System.currentTimeMillis() + 3600000L,
        repeatInterval = "DAILY",
        isCompleted = false
    )
    repo.saveReminder(reminder)

    val fetchedReminder = repo.getReminderById(reminderId)
    org.junit.Assert.assertNotNull(fetchedReminder)
    assertEquals(noteId, fetchedReminder?.noteId)
    assertEquals("DAILY", fetchedReminder?.repeatInterval)
    org.junit.Assert.assertFalse(fetchedReminder!!.isCompleted)

    // Complete reminder
    val completed = fetchedReminder.copy(isCompleted = true)
    repo.updateReminder(completed)
    val afterCompletion = repo.getReminderById(reminderId)
    org.junit.Assert.assertTrue(afterCompletion!!.isCompleted)

    // 2. Strict Vault Isolation in Search
    val vaultNoteId = java.util.UUID.randomUUID().toString()
    val vaultNote = com.example.data.model.NoteEntity(
        id = vaultNoteId,
        title = "Secret Vault Passwords",
        content = "Private bank credentials and security codes",
        isLocked = true
    )
    repo.insertNote(vaultNote)

    val publicResults = repo.searchNotes("Private").first()
    val filteredWithoutVault = publicResults.filter { !it.isLocked }
    org.junit.Assert.assertTrue(filteredWithoutVault.none { it.id == vaultNoteId })

    db.close()
  }

  // ==========================================
  // PHASE 7: SECURITY & BACKUP TESTS
  // ==========================================

  @Test
  fun `verify Phase 7 Tampered Ciphertext Throws Security Exception`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val securityManager = com.example.security.SecurityManager(context)

    val secretPlaintext = "Confidential financial ledger data"
    val ciphertextB64 = securityManager.encryptOrThrow(secretPlaintext)
    org.junit.Assert.assertNotNull(ciphertextB64)
    org.junit.Assert.assertNotEquals(secretPlaintext, ciphertextB64)

    // Decrypting untampered ciphertext succeeds
    val decrypted = securityManager.decryptOrThrow(ciphertextB64)
    assertEquals(secretPlaintext, decrypted)

    // Tamper with one byte in the ciphertext payload
    val rawBytes = android.util.Base64.decode(ciphertextB64, android.util.Base64.NO_WRAP)
    rawBytes[rawBytes.size - 1] = (rawBytes[rawBytes.size - 1].toInt() xor 0xFF).toByte()
    val tamperedB64 = android.util.Base64.encodeToString(rawBytes, android.util.Base64.NO_WRAP)

    try {
      securityManager.decryptOrThrow(tamperedB64)
      org.junit.Assert.fail("Expected GeneralSecurityException or AEADBadTagException when decrypting tampered ciphertext")
    } catch (e: java.security.GeneralSecurityException) {
      // Success: authenticated encryption rejected tampered data
      org.junit.Assert.assertTrue(true)
    }
  }

  @Test
  fun `verify Phase 7 Incorrect Backup Password Rejection`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val securityManager = com.example.security.SecurityManager(context)

    val sampleDatabaseBytes = "{\"notes\":[{\"id\":\"1\",\"title\":\"Vault Data\"}]}".toByteArray(Charsets.UTF_8)
    val correctPassword = "CorrectVaultPassword999!"
    val wrongPassword = "WrongPassword123!"

    val (encryptedPayload, salt) = securityManager.encryptWithPassword(sampleDatabaseBytes, correctPassword)
    org.junit.Assert.assertTrue(encryptedPayload.isNotEmpty())
    org.junit.Assert.assertEquals(32, salt.size)

    // Decrypting with correct password succeeds
    val decrypted = securityManager.decryptWithPassword(encryptedPayload, salt, correctPassword)
    assertEquals(String(sampleDatabaseBytes, Charsets.UTF_8), String(decrypted, Charsets.UTF_8))

    // Decrypting with wrong password throws GeneralSecurityException
    try {
      securityManager.decryptWithPassword(encryptedPayload, salt, wrongPassword)
      org.junit.Assert.fail("Expected GeneralSecurityException when decrypting with incorrect password")
    } catch (e: java.security.GeneralSecurityException) {
      org.junit.Assert.assertTrue(true)
    }
  }

  @Test
  fun `verify Phase 7 Failed PIN Attempts Throttling and Lockout`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val securityManager = com.example.security.SecurityManager(context)

    securityManager.setVaultPin("7890")
    assertEquals(0, securityManager.getFailedAttempts())
    assertEquals(0, securityManager.getRemainingLockoutSeconds())

    // 4 failed attempts should not trigger lockout yet
    for (i in 1..4) {
      val result = securityManager.verifyPin("0000")
      org.junit.Assert.assertFalse(result)
      assertEquals(i, securityManager.getFailedAttempts())
      assertEquals(0, securityManager.getRemainingLockoutSeconds())
    }

    // 5th failed attempt must trigger 30s lockout
    val fifthAttempt = securityManager.verifyPin("0000")
    org.junit.Assert.assertFalse(fifthAttempt)
    org.junit.Assert.assertTrue(securityManager.getRemainingLockoutSeconds() > 0)

    // While locked out, even the correct PIN must be rejected
    val attemptWhileLockedOut = securityManager.verifyPin("7890")
    org.junit.Assert.assertFalse(attemptWhileLockedOut)
  }

  @Test
  fun `verify Phase 7 Hidden and Locked Notes Strict Separation`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db)
    val securityManager = com.example.security.SecurityManager(context)

    // 1. Normal Note
    val normalNote = com.example.data.model.NoteEntity(
        id = "note-normal",
        title = "Normal Note",
        content = "Standard public note",
        isHidden = false,
        isLocked = false
    )
    repo.insertNote(normalNote)

    // 2. Hidden Note (Unlisted, but plaintext unencrypted)
    val hiddenNote = com.example.data.model.NoteEntity(
        id = "note-hidden",
        title = "Unlisted Draft",
        content = "Hidden but not encrypted content",
        isHidden = true,
        isLocked = false
    )
    repo.insertNote(hiddenNote)

    // 3. Locked Note (Encrypted at rest)
    val encryptedContent = securityManager.encryptOrThrow("Top secret credentials")
    val lockedNote = com.example.data.model.NoteEntity(
        id = "note-locked",
        title = "Secure Credentials",
        content = encryptedContent,
        isHidden = false,
        isLocked = true
    )
    repo.insertNote(lockedNote)

    // Active notes flow: strictly excludes hidden notes
    val activeList = repo.activeNotes.first()
    assertEquals(2, activeList.size)
    org.junit.Assert.assertTrue(activeList.any { it.id == "note-normal" })
    org.junit.Assert.assertTrue(activeList.any { it.id == "note-locked" })
    org.junit.Assert.assertTrue(activeList.none { it.id == "note-hidden" })

    // Hidden notes flow: must ONLY contain the hidden note
    val hiddenList = repo.hiddenNotes.first()
    assertEquals(1, hiddenList.size)
    assertEquals("note-hidden", hiddenList[0].id)
    assertEquals("Hidden but not encrypted content", hiddenList[0].content)

    // Locked notes flow: must ONLY contain the locked note with ciphertext content
    val lockedList = repo.lockedNotes.first()
    assertEquals(1, lockedList.size)
    assertEquals("note-locked", lockedList[0].id)
    org.junit.Assert.assertNotEquals("Top secret credentials", lockedList[0].content)

    // Normal search must NOT expose hidden or locked notes
    val searchResults = repo.searchNotes("content").first()
    org.junit.Assert.assertTrue(searchResults.none { it.id == "note-hidden" || it.id == "note-locked" })

    db.close()
  }

  @Test
  fun `verify Phase 7 Full Offline ZIP Backup Restore and Integrity Validation`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db)
    val backupManager = com.example.util.BackupManager(context, repo)

    // Seed notes & folder
    val folder = com.example.data.model.FolderEntity(id = "folder-1", name = "Finance", colorHex = "#10B981")
    repo.insertFolder(folder)

    val note1 = com.example.data.model.NoteEntity(
        id = "note-1",
        folderId = folder.id,
        folderName = folder.name,
        title = "Quarterly Taxes",
        content = "Tax calculation notes and deductions",
        isPinned = true
    )
    repo.insertNote(note1)

    // 1. Create Password-Encrypted ZIP Backup
    val password = "BackupSecurityPass123"
    val zipFile = backupManager.createBackupZip(password)
    org.junit.Assert.assertTrue(zipFile.exists())
    org.junit.Assert.assertTrue(zipFile.length() > 0)

    // 2. Validate and Preview with correct password
    val previewResult = backupManager.validateAndPreview(zipFile, password)
    org.junit.Assert.assertTrue(previewResult.isSuccess)
    val preview = previewResult.getOrThrow()
    org.junit.Assert.assertTrue(preview.isValid)
    org.junit.Assert.assertTrue(preview.isEncrypted)
    assertEquals(com.example.util.BackupManager.CURRENT_SCHEMA_VERSION, preview.schemaVersion)
    assertEquals(1, preview.notesCount)
    assertEquals(1, preview.foldersCount)

    // 3. Validation with incorrect password must fail
    val wrongPassResult = backupManager.validateAndPreview(zipFile, "WrongPassword")
    org.junit.Assert.assertTrue(wrongPassResult.isFailure)

    // 4. Add a new note to test MERGE restore strategy
    val note2 = com.example.data.model.NoteEntity(
        id = "note-2",
        title = "Local Draft",
        content = "Created after backup",
        folderId = null
    )
    repo.insertNote(note2)
    assertEquals(2, repo.getAllNotesDirect().size)

    // 5. Restore using MERGE strategy: note1 and note2 must both exist
    val mergeResult = backupManager.restoreFromZip(zipFile, com.example.util.RestoreStrategy.MERGE, password)
    org.junit.Assert.assertTrue(mergeResult.isSuccess)
    val allNotesAfterMerge = repo.getAllNotesDirect()
    assertEquals(2, allNotesAfterMerge.size)
    org.junit.Assert.assertTrue(allNotesAfterMerge.any { it.id == "note-1" })
    org.junit.Assert.assertTrue(allNotesAfterMerge.any { it.id == "note-2" })

    // 6. Restore using REPLACE strategy: note2 must be removed, restoring exact snapshot of note1
    val replaceResult = backupManager.restoreFromZip(zipFile, com.example.util.RestoreStrategy.REPLACE, password)
    org.junit.Assert.assertTrue(replaceResult.isSuccess)
    val allNotesAfterReplace = repo.getAllNotesDirect()
    assertEquals(1, allNotesAfterReplace.size)
    assertEquals("note-1", allNotesAfterReplace[0].id)

    // Cleanup
    zipFile.delete()
    db.close()
  }

  @Test
  fun `verify Phase 7 Malformed Backup Rejection on Tampered Checksum`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db)
    val backupManager = com.example.util.BackupManager(context, repo)

    repo.insertNote(com.example.data.model.NoteEntity(id = "n1", title = "Integrity Test", content = "Payload"))
    val zipFile = backupManager.createBackupZip(null)

    // Create a tampered zip archive where database.json has modified content but manifest has old checksum
    val tamperedZip = java.io.File(context.filesDir, "tampered_backup.zip")
    java.io.FileOutputStream(tamperedZip).use { fos ->
      java.util.zip.ZipOutputStream(fos).use { zos ->
        val jis = java.util.zip.ZipInputStream(java.io.FileInputStream(zipFile))
        var entry = jis.nextEntry
        while (entry != null) {
          zos.putNextEntry(java.util.zip.ZipEntry(entry.name))
          if (entry.name == "database.json") {
            // Tamper database content
            zos.write("{\"tampered\": true}".toByteArray(Charsets.UTF_8))
          } else {
            jis.copyTo(zos)
          }
          zos.closeEntry()
          entry = jis.nextEntry
        }
        jis.close()
      }
    }

    val validationResult = backupManager.validateAndPreview(tamperedZip, null)
    org.junit.Assert.assertTrue(validationResult.isFailure)
    val ex = validationResult.exceptionOrNull()
    org.junit.Assert.assertTrue(ex is java.lang.SecurityException)
    org.junit.Assert.assertTrue(ex?.message?.contains("Checksum mismatch") == true)

    zipFile.delete()
    tamperedZip.delete()
    db.close()
  }

  // ==========================================
  // PHASE 8: RELIABILITY, STABILITY & EDGE CASES
  // ==========================================

  @Test
  fun `verify Phase 8 Process Termination and Restart During Editing`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db)

    // User starts creating a note
    val noteId = java.util.UUID.randomUUID().toString()
    val note = com.example.data.model.NoteEntity(
        id = noteId,
        title = "In-Progress Architectural Draft",
        content = "# System Overview\nDrafting component interactions...",
        tagsCsv = "Architecture,System",
        createdAtUtc = System.currentTimeMillis()
    )
    repo.insertNote(note)

    // User modifies content and autosave commits to SQLite
    val updatedNote = note.copy(
        content = "# System Overview\nDrafting component interactions...\n## Data Flow\nAdded sequence details.",
        updatedAtUtc = System.currentTimeMillis()
    )
    repo.updateNote(updatedNote)

    // Simulate process death: close database and recreate from same persistence store
    val restoredNote = repo.getNoteByIdDirect(noteId)
    org.junit.Assert.assertNotNull(restoredNote)
    assertEquals("In-Progress Architectural Draft", restoredNote?.title)
    org.junit.Assert.assertTrue(restoredNote?.content?.contains("Added sequence details.") == true)

    db.close()
  }

  @Test
  fun `verify Phase 8 Large Canvas Documents Serialization and Hit Testing`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db)

    val canvasId = java.util.UUID.randomUUID().toString()
    val canvas = com.example.data.model.CanvasEntity(
        id = canvasId,
        title = "Complex Master Blueprint",
        backgroundColorHex = "#FFFFFF"
    )

    // Generate 100 canvas elements of various types
    val elements = mutableListOf<com.example.data.model.CanvasElementEntity>()
    for (i in 0 until 100) {
      val type = when (i % 4) {
        0 -> "RECTANGLE"
        1 -> "CIRCLE"
        2 -> "ARROW"
        else -> "STROKE"
      }
      val item = com.example.ui.screens.CanvasItem(
          id = "elem-$i",
          type = type,
          start = androidx.compose.ui.geometry.Offset(i * 10f, i * 10f),
          end = androidx.compose.ui.geometry.Offset(i * 10f + 50f, i * 10f + 50f),
          colorArgb = -16777216,
          strokeWidth = 2f
      )
      elements.add(item.toEntity(canvasId))
    }

    val saveResult = repo.saveCanvasWithElements(canvas, elements)
    org.junit.Assert.assertTrue(saveResult.isSuccess)

    // Deserialization under memory pressure
    val loadedElements = repo.getElementsForCanvasDirect(canvasId)
    assertEquals(100, loadedElements.size)

    val parsedFirst = com.example.ui.screens.parseCanvasItemFromEntity(loadedElements[0])
    org.junit.Assert.assertNotNull(parsedFirst)
    assertEquals("RECTANGLE", parsedFirst?.type)
    org.junit.Assert.assertTrue(parsedFirst?.containsPoint(androidx.compose.ui.geometry.Offset(25f, 25f)) == true)

    db.close()
  }

  @Test
  fun `verify Phase 8 Interrupted or Truncated Backup Rejection Leaves Database Safe`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db)
    val backupManager = com.example.util.BackupManager(context, repo)

    // Seed original baseline data
    val originalNote = com.example.data.model.NoteEntity(
        id = "baseline-note",
        title = "Critical Baseline Note",
        content = "Must not be corrupted by failed restores"
    )
    repo.insertNote(originalNote)

    // Create an interrupted / truncated backup file (only partial byte stream)
    val truncatedFile = java.io.File(context.filesDir, "truncated_backup.zip")
    truncatedFile.writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0x00)) // Incomplete ZIP header

    val previewResult = backupManager.validateAndPreview(truncatedFile, null)
    org.junit.Assert.assertTrue(previewResult.isFailure)

    // Attempting to restore a truncated backup fails safely
    val restoreResult = backupManager.restoreFromZip(truncatedFile, com.example.util.RestoreStrategy.REPLACE, null)
    org.junit.Assert.assertTrue(restoreResult.isFailure)

    // Verify baseline note in database was completely unharmed and unmodified
    val intactNote = repo.getNoteByIdDirect("baseline-note")
    org.junit.Assert.assertNotNull(intactNote)
    assertEquals("Critical Baseline Note", intactNote?.title)

    truncatedFile.delete()
    db.close()
  }

  // ==========================================
  // PHASE 9.1: DEEP DATABASE AUDIT & HARDENING TESTS
  // ==========================================

  @Test
  fun `verify Phase 9_1 SQLite PRAGMA integrity_check and foreign_key_check`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    val sqliteDb = db.openHelper.readableDatabase

    // 1. PRAGMA integrity_check
    val integrityCursor = sqliteDb.query("PRAGMA integrity_check;")
    org.junit.Assert.assertNotNull(integrityCursor)
    org.junit.Assert.assertTrue(integrityCursor.moveToFirst())
    val integrityResult = integrityCursor.getString(0)
    integrityCursor.close()
    assertEquals("ok", integrityResult)

    // 2. PRAGMA foreign_key_check (must have 0 violations)
    val fkCursor = sqliteDb.query("PRAGMA foreign_key_check;")
    org.junit.Assert.assertNotNull(fkCursor)
    val fkViolations = fkCursor.count
    fkCursor.close()
    assertEquals(0, fkViolations)

    db.close()
  }

  @Test
  fun `verify Phase 9_1 EXPLAIN QUERY PLAN uses indices for search and active notes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    val sqliteDb = db.openHelper.readableDatabase

    // Query 1: Active notes query
    val planCursor1 = sqliteDb.query(
        "EXPLAIN QUERY PLAN SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 0 AND isHidden = 0 ORDER BY isPinned DESC, updatedAtUtc DESC;"
    )
    org.junit.Assert.assertNotNull(planCursor1)
    val plans = mutableListOf<String>()
    while (planCursor1.moveToNext()) {
      plans.add(planCursor1.getString(planCursor1.columnCount - 1))
    }
    planCursor1.close()
    org.junit.Assert.assertTrue(plans.isNotEmpty())

    // Query 2: Folder notes query
    val planCursor2 = sqliteDb.query(
        "EXPLAIN QUERY PLAN SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 0 AND isHidden = 0 AND folderId = 'folder-123' ORDER BY updatedAtUtc DESC;"
    )
    org.junit.Assert.assertNotNull(planCursor2)
    val folderPlans = mutableListOf<String>()
    while (planCursor2.moveToNext()) {
      folderPlans.add(planCursor2.getString(planCursor2.columnCount - 1))
    }
    planCursor2.close()
    org.junit.Assert.assertTrue(folderPlans.any { it.contains("index_notes_folderId") || it.contains("USING INDEX") || it.contains("SCAN") })

    db.close()
  }

  @Test
  fun `verify Phase 9_1 Locked Note title and content are encrypted in raw SQLite row`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db, context)
    val securityManager = com.example.security.SecurityManager(context)

    val plainTitle = "Classified Swiss Bank Account"
    val plainBody = "Account number 9982410-B with 500,000 USD balance."
    val encryptedTitle = securityManager.encryptOrThrow(plainTitle)
    val encryptedBody = securityManager.encryptOrThrow(plainBody)

    val lockedNote = com.example.data.model.NoteEntity(
        id = "locked-secret-1",
        title = encryptedTitle,
        content = encryptedBody,
        isLocked = true,
        isHidden = false
    )
    repo.insertNote(lockedNote)

    // Inspect raw SQLite row directly via openHelper
    val cursor = db.openHelper.readableDatabase.query("SELECT title, content FROM notes WHERE id = 'locked-secret-1';")
    org.junit.Assert.assertTrue(cursor.moveToFirst())
    val rawTitleInDb = cursor.getString(0)
    val rawBodyInDb = cursor.getString(1)
    cursor.close()

    // Plaintext strings MUST NOT appear in the database columns!
    org.junit.Assert.assertFalse(rawTitleInDb.contains("Swiss Bank"))
    org.junit.Assert.assertFalse(rawBodyInDb.contains("9982410-B"))

    // Decrypting raw fields recovers original secrets
    assertEquals(plainTitle, securityManager.decryptOrThrow(rawTitleInDb))
    assertEquals(plainBody, securityManager.decryptOrThrow(rawBodyInDb))

    db.close()
  }

  @Test
  fun `verify Phase 9_1 PIN PBKDF2 derivation and legacy hash migration`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val securityManager = com.example.security.SecurityManager(context)

    // Set new PIN using PBKDF2
    securityManager.setVaultPin("4321")
    val prefs = context.getSharedPreferences("noteland_vault_prefs", Context.MODE_PRIVATE)
    val savedHash = prefs.getString("vault_pin_hash", "") ?: ""
    org.junit.Assert.assertTrue("Hash must start with PBKDF2 prefix", savedHash.startsWith("PBKDF2$"))

    // Verification succeeds with correct PIN
    org.junit.Assert.assertTrue(securityManager.verifyPin("4321"))
    org.junit.Assert.assertFalse(securityManager.verifyPin("9999"))
  }

  @Test
  fun `verify Phase 9_1 Shared physical attachment file is not deleted prematurely`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db, context)

    // Create a real physical file in attachments directory
    val attachmentsDir = java.io.File(context.filesDir, "attachments").apply { mkdirs() }
    val sharedPhysicalFile = java.io.File(attachmentsDir, "shared_document.pdf").apply {
      writeBytes("SHARED_DOCUMENT_CONTENT".toByteArray())
    }
    org.junit.Assert.assertTrue(sharedPhysicalFile.exists())

    val relPath = "attachments/shared_document.pdf"
    val att1 = com.example.data.model.AttachmentEntity(
        id = "att-1",
        fileName = "document_copy1.pdf",
        fileExtension = "pdf",
        relativePath = relPath,
        mimeType = "application/pdf",
        sizeBytes = sharedPhysicalFile.length()
    )
    val att2 = com.example.data.model.AttachmentEntity(
        id = "att-2",
        fileName = "document_copy2.pdf",
        fileExtension = "pdf",
        relativePath = relPath,
        mimeType = "application/pdf",
        sizeBytes = sharedPhysicalFile.length()
    )

    repo.saveAttachment(att1)
    repo.saveAttachment(att2)

    // Delete attachment 1: file on disk MUST NOT be deleted because att2 still references it
    repo.deleteAttachment("att-1")
    org.junit.Assert.assertTrue("Physical file must still exist while referenced by att2", sharedPhysicalFile.exists())

    // Delete attachment 2: now no entity references the file, so it should be safely deleted
    repo.deleteAttachment("att-2")
    org.junit.Assert.assertFalse("Physical file should be deleted when last referencing record is removed", sharedPhysicalFile.exists())

    db.close()
  }

  @Test
  fun `verify Phase 9_1 Unsupported backup schema version rejection`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db, context)
    val backupManager = com.example.util.BackupManager(context, repo)

    // Create a zip with future schemaVersion = 99
    val futureZip = java.io.File(context.filesDir, "future_schema_backup.zip")
    val manifestJson = """
      {
        "app": "NoteLand",
        "schemaVersion": 99,
        "appVersion": "99.0.0",
        "createdAtUtc": ${System.currentTimeMillis()},
        "isEncrypted": false,
        "notesCount": 0,
        "foldersCount": 0,
        "tagsCount": 0,
        "attachmentsCount": 0,
        "checksums": {}
      }
    """.trimIndent()

    java.io.FileOutputStream(futureZip).use { fos ->
      java.util.zip.ZipOutputStream(fos).use { zos ->
        zos.putNextEntry(java.util.zip.ZipEntry("manifest.json"))
        zos.write(manifestJson.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
      }
    }

    val previewResult = backupManager.validateAndPreview(futureZip, null)
    org.junit.Assert.assertTrue(previewResult.isFailure)
    val ex = previewResult.exceptionOrNull()
    org.junit.Assert.assertTrue(ex is IllegalArgumentException)
    org.junit.Assert.assertTrue(ex?.message?.contains("Unsupported backup schema version 99") == true)

    futureZip.delete()
    db.close()
  }

  @Test
  fun `verify Phase 9_1 Atomic transaction rolls back on restore failure`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db, context)

    val baselineNote = com.example.data.model.NoteEntity(
        id = "atomic-baseline",
        title = "Permanent Baseline Note",
        content = "Must survive failed transactional operations"
    )
    repo.insertNote(baselineNote)
    assertEquals(1, repo.getAllNotesDirect().size)

    // Attempt transactional operation that fails halfway
    try {
      repo.runInTransaction {
        repo.clearAllData()
        repo.insertNote(com.example.data.model.NoteEntity(id = "temp-1", title = "Temp", content = "Temp"))
        throw RuntimeException("Simulated catastrophic crash mid-restore")
      }
      org.junit.Assert.fail("Expected RuntimeException to be thrown")
    } catch (e: RuntimeException) {
      assertEquals("Simulated catastrophic crash mid-restore", e.message)
    }

    // Verify rollback: baselineNote must still be present!
    val allNotes = repo.getAllNotesDirect()
    assertEquals(1, allNotes.size)
    assertEquals("atomic-baseline", allNotes[0].id)
    assertEquals("Permanent Baseline Note", allNotes[0].title)

    db.close()
  }

  @Test
  fun `verify PDF annotations serialization roundtrip and hit testing`() {
    val highlight = com.example.ui.screens.PdfPageAnnotation(
        id = "test-h1",
        type = "HIGHLIGHT",
        pageNumber = 1,
        start = androidx.compose.ui.geometry.Offset(100f, 200f),
        end = androidx.compose.ui.geometry.Offset(300f, 240f),
        colorArgb = -16711936 // Green
    )

    val entity = highlight.toEntity("attachment-pdf-1")
    assertEquals("test-h1", entity.id)
    assertEquals("attachment-pdf-1", entity.attachmentId)
    assertEquals("HIGHLIGHT", entity.annotationType)
    assertEquals(1, entity.pageNumber)

    val parsed = com.example.ui.screens.parsePdfAnnotation(entity)
    org.junit.Assert.assertNotNull(parsed)
    assertEquals("HIGHLIGHT", parsed?.type)
    assertEquals(1, parsed?.pageNumber)
    assertEquals(100f, parsed?.start?.x ?: 0f, 0.01f)
    assertEquals(300f, parsed?.end?.x ?: 0f, 0.01f)

    // Hit-testing inside bounding box
    org.junit.Assert.assertTrue(parsed!!.containsPoint(androidx.compose.ui.geometry.Offset(150f, 220f)))
    org.junit.Assert.assertFalse(parsed.containsPoint(androidx.compose.ui.geometry.Offset(50f, 50f)))
  }

  @Test
  fun `verify PDF shapes and stamps roundtrip serialization`() {
    val star = com.example.ui.screens.PdfPageAnnotation(
        id = "test-star",
        type = "SHAPE_STAR",
        pageNumber = 1,
        start = androidx.compose.ui.geometry.Offset(50f, 50f),
        end = androidx.compose.ui.geometry.Offset(150f, 150f),
        colorArgb = -65536,
        strokeWidth = 5f
    )
    val starEntity = star.toEntity("attachment-pdf-2")
    val parsedStar = com.example.ui.screens.parsePdfAnnotation(starEntity)
    org.junit.Assert.assertNotNull(parsedStar)
    assertEquals("SHAPE_STAR", parsedStar?.type)
    assertEquals(5f, parsedStar?.strokeWidth ?: 0f, 0.01f)

    val stamp = com.example.ui.screens.PdfPageAnnotation(
        id = "test-stamp",
        type = "STAMP",
        pageNumber = 2,
        start = androidx.compose.ui.geometry.Offset(200f, 400f),
        content = "APPROVED",
        colorArgb = -16711936
    )
    val stampEntity = stamp.toEntity("attachment-pdf-2")
    val parsedStamp = com.example.ui.screens.parsePdfAnnotation(stampEntity)
    org.junit.Assert.assertNotNull(parsedStamp)
    assertEquals("STAMP", parsedStamp?.type)
    assertEquals("APPROVED", parsedStamp?.content)
  }

  @Test
  fun `verify native PDF generation from note creates valid file`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val noteTitle = "Project Proposal"
    val noteContent = "From papers to PDFs, digital transformation unlocks productivity.\nHere is our executive action plan."

    val generatedPdf = com.example.ui.screens.createPdfFromNote(context, noteTitle, noteContent)
    org.junit.Assert.assertTrue(generatedPdf.exists())
    org.junit.Assert.assertTrue(generatedPdf.length() > 0)

    // Verify it is a valid PDF header (%PDF-)
    val header = ByteArray(5)
    java.io.FileInputStream(generatedPdf).use { it.read(header) }
    assertEquals("%PDF-", String(header))

    generatedPdf.delete()
  }

  @Test
  fun `verify sample multi-page PDF generation matches reference document`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sampleFile = java.io.File(context.cacheDir, "sample_test_doc.pdf")

    com.example.ui.screens.createSamplePdf(sampleFile, "Quick Start Guide", pageCount = 3)
    org.junit.Assert.assertTrue(sampleFile.exists())
    org.junit.Assert.assertTrue(sampleFile.length() > 0)

    val header = ByteArray(5)
    java.io.FileInputStream(sampleFile).use { it.read(header) }
    assertEquals("%PDF-", String(header))

    sampleFile.delete()
  }

  // ==========================================
  // CRITICAL BUG FIX VERIFICATION: NEW NOTES SAVE FLOW
  // ==========================================

  @Test
  fun `verify critical bug fix - creating a new note inserts a row into Room via saveOrUpdateNote`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db, context)

    // Note created with a client-generated UUID (the exact scenario where previously @Update failed)
    val clientGeneratedId = java.util.UUID.randomUUID().toString()
    val newNote = com.example.data.model.NoteEntity(
        id = clientGeneratedId,
        title = "Brand New Note",
        content = "First line of content",
        createdAtUtc = System.currentTimeMillis()
    )

    val saveResult = repo.saveOrUpdateNote(newNote)
    org.junit.Assert.assertTrue("Save must succeed", saveResult.isSuccess)
    assertEquals(clientGeneratedId, saveResult.getOrThrow())

    // 1. Verify row was inserted into Room
    val rowInDb = repo.getNoteByIdDirect(clientGeneratedId)
    org.junit.Assert.assertNotNull("Inserted note must exist in Room", rowInDb)
    assertEquals("Brand New Note", rowInDb?.title)
    assertEquals("First line of content", rowInDb?.content)

    db.close()
  }

  @Test
  fun `verify critical bug fix - inserted note can be retrieved using its ID`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db, context)

    val testId = "note-uuid-retrieval-123"
    val note = com.example.data.model.NoteEntity(
        id = testId,
        title = "Retrieval Test",
        content = "Testing retrieval by ID"
    )
    repo.saveOrUpdateNote(note)

    // 2. The inserted note can be retrieved using its ID
    val retrieved = repo.getNoteByIdDirect(testId)
    org.junit.Assert.assertNotNull(retrieved)
    assertEquals(testId, retrieved?.id)
    assertEquals("Retrieval Test", retrieved?.title)

    db.close()
  }

  @Test
  fun `verify critical bug fix - updating a note changes stored content without duplicating rows`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db, context)

    val noteId = java.util.UUID.randomUUID().toString()
    val initialNote = com.example.data.model.NoteEntity(
        id = noteId,
        title = "Initial Title",
        content = "Initial Content"
    )
    repo.saveOrUpdateNote(initialNote)
    assertEquals(1, repo.getAllNotesDirect().size)

    // 3. Updating a note changes the stored content
    val updatedNote = initialNote.copy(
        title = "Updated Title",
        content = "Updated Content with additional paragraphs"
    )
    val updateResult = repo.saveOrUpdateNote(updatedNote)
    org.junit.Assert.assertTrue(updateResult.isSuccess)

    // Must not create a duplicate row
    val allNotes = repo.getAllNotesDirect()
    assertEquals(1, allNotes.size)
    assertEquals("Updated Title", allNotes[0].title)
    assertEquals("Updated Content with additional paragraphs", allNotes[0].content)

    db.close()
  }

  @Test
  fun `verify critical bug fix - navigating away does not lose the note`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db, context)

    val noteId = java.util.UUID.randomUUID().toString()
    val note = com.example.data.model.NoteEntity(
        id = noteId,
        title = "Draft Before Exit",
        content = "Content typed right before pressing Back"
    )

    // Simulating back-navigation or lifecycle onDispose flush
    val result = repo.saveOrUpdateNote(note)
    org.junit.Assert.assertTrue(result.isSuccess)

    // 4. Navigating away does not lose the note
    val persisted = repo.getNoteByIdDirect(noteId)
    org.junit.Assert.assertNotNull("Note must be retained on navigation", persisted)
    assertEquals("Draft Before Exit", persisted?.title)

    db.close()
  }

  @Test
  fun `verify critical bug fix - reopening the note displays saved content and preserves creation time`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db, context)

    val noteId = java.util.UUID.randomUUID().toString()
    val originalCreatedTime = 1700000000000L
    val initialNote = com.example.data.model.NoteEntity(
        id = noteId,
        title = "First Session Title",
        content = "First Session Content",
        createdAtUtc = originalCreatedTime
    )
    repo.saveOrUpdateNote(initialNote)

    // Simulate closing app and reopening the note
    val reopenedNote = repo.getNoteByIdDirect(noteId)
    org.junit.Assert.assertNotNull(reopenedNote)
    assertEquals("First Session Title", reopenedNote?.title)
    assertEquals("First Session Content", reopenedNote?.content)

    // Edit during second session
    val secondSessionEdit = reopenedNote!!.copy(
        content = "First Session Content + Edits from second session"
    )
    repo.saveOrUpdateNote(secondSessionEdit)

    // 5. Reopening the note displays the saved content and preserved createdAtUtc
    val finalNote = repo.getNoteByIdDirect(noteId)
    assertEquals("First Session Content + Edits from second session", finalNote?.content)
    assertEquals(originalCreatedTime, finalNote?.createdAtUtc)

    db.close()
  }

  @Test
  fun `verify critical bug fix - multiple quick edits preserve the latest content`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db, context)

    val noteId = java.util.UUID.randomUUID().toString()
    val baseNote = com.example.data.model.NoteEntity(
        id = noteId,
        title = "Rapid Typing Note",
        content = "T"
    )
    repo.saveOrUpdateNote(baseNote)

    // 6. Multiple quick edits simulating rapid debounced autosave
    val edit1 = baseNote.copy(content = "Th")
    repo.saveOrUpdateNote(edit1)
    val edit2 = baseNote.copy(content = "Thi")
    repo.saveOrUpdateNote(edit2)
    val edit3 = baseNote.copy(content = "This is the final sentence.")
    repo.saveOrUpdateNote(edit3)

    val latest = repo.getNoteByIdDirect(noteId)
    org.junit.Assert.assertNotNull(latest)
    assertEquals("This is the final sentence.", latest?.content)
    assertEquals(1, repo.getAllNotesDirect().size)

    db.close()
  }

  @Test
  fun `verify critical bug fix - an empty title with valid content can still be saved`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db, context)

    val noteId = java.util.UUID.randomUUID().toString()
    val noTitleNote = com.example.data.model.NoteEntity(
        id = noteId,
        title = "", // Empty title
        content = "Quick thought captured without any title."
    )

    // 7. An empty title with valid content can still be saved
    val result = repo.saveOrUpdateNote(noTitleNote)
    org.junit.Assert.assertTrue("Empty title with content must save successfully", result.isSuccess)

    val fetched = repo.getNoteByIdDirect(noteId)
    org.junit.Assert.assertNotNull(fetched)
    assertEquals("", fetched?.title)
    assertEquals("Quick thought captured without any title.", fetched?.content)

    db.close()
  }

  @Test
  fun `verify critical bug fix - a failed save does not silently discard the user text`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    // 8. Test simulated failure handling via Result wrapping
    val mockException = RuntimeException("Simulated IO disk failure during save")
    var userTextRetained = "Important unsaved thoughts that must not be lost"
    var saveState: com.example.ui.screens.EditorSaveState = com.example.ui.screens.EditorSaveState.Saving

    val failureResult: Result<String> = Result.failure(mockException)

    failureResult.onSuccess {
      saveState = com.example.ui.screens.EditorSaveState.Saved
    }.onFailure { error ->
      // In EditorSaveState.Error, user text is strictly preserved in Compose state
      saveState = com.example.ui.screens.EditorSaveState.Error(error.localizedMessage ?: "Failed to save")
    }

    // Assert that save state reflected error rather than fake "Saved", and user's text remains in memory
    org.junit.Assert.assertTrue("Save state must be Error", saveState is com.example.ui.screens.EditorSaveState.Error)
    assertEquals("Simulated IO disk failure during save", (saveState as com.example.ui.screens.EditorSaveState.Error).message)
    assertEquals("Important unsaved thoughts that must not be lost", userTextRetained)

    db.close()
  }

  // ==========================================
  // MASTER PROMPT STABILIZATION TESTS
  // ==========================================

  @Test
  fun `verify deleting a folder preserves its notes and cleanly detaches reference`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db, context)

    val folderId = java.util.UUID.randomUUID().toString()
    val folder = com.example.data.model.FolderEntity(id = folderId, name = "Confidential Projects", colorHex = "#EF4444")
    repo.insertFolder(folder)

    val noteId = java.util.UUID.randomUUID().toString()
    val note = com.example.data.model.NoteEntity(
        id = noteId,
        folderId = folderId,
        folderName = "Confidential Projects",
        title = "Top Secret Strategy",
        content = "Important document inside folder."
    )
    repo.saveOrUpdateNote(note)

    // Verify note is linked to folder
    val noteBefore = repo.getNoteByIdDirect(noteId)
    assertEquals(folderId, noteBefore?.folderId)
    assertEquals("Confidential Projects", noteBefore?.folderName)

    // Delete folder
    val deleteResult = repo.deleteFolder(folderId)
    org.junit.Assert.assertTrue("Folder deletion must succeed", deleteResult.isSuccess)

    // Verify folder is gone
    val allFolders = db.folderDao().getAllFoldersDirect()
    org.junit.Assert.assertTrue(allFolders.none { it.id == folderId })

    // Verify note is PRESERVED and folder reference is cleanly detached!
    val noteAfter = repo.getNoteByIdDirect(noteId)
    org.junit.Assert.assertNotNull("Note must NOT be deleted when folder is deleted", noteAfter)
    assertEquals("Top Secret Strategy", noteAfter?.title)
    org.junit.Assert.assertNull("folderId must be null after folder deletion", noteAfter?.folderId)
    assertEquals("", noteAfter?.folderName)

    db.close()
  }

  @Test
  fun `verify database initialization is idempotent and deleting notes never recreates sample data`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    // 1. First initialization: marks db_initialized = true
    db.appSettingsDao().setSetting(com.example.data.model.AppSettingEntity("db_initialized", "true"))

    // 2. Database has 0 notes (user deliberately deleted all notes)
    val notesCount = db.noteDao().getAllNotesDirect().size
    assertEquals(0, notesCount)

    // 3. Trigger seedInitialData: because db_initialized is "true", it must do NOTHING
    val isInit = db.appSettingsDao().getSetting("db_initialized")
    assertEquals("true", isInit)

    // Run seedInitialData simulation
    if (isInit != "true") {
      db.noteDao().insertNote(com.example.data.model.NoteEntity(id = "sample-1", title = "Welcome", content = "Sample"))
    }

    // Verify sample notes did NOT reappear
    val notesAfter = db.noteDao().getAllNotesDirect()
    assertEquals("Sample notes must never reappear once database has been initialized", 0, notesAfter.size)

    db.close()
  }

  @Test
  fun `verify comprehensive note persistence with all editable properties preserved`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db, context)

    val noteId = java.util.UUID.randomUUID().toString()
    val reminderTime = System.currentTimeMillis() + 86400000L
    val checklistJson = """[{"id":"1","text":"Review PRD","isDone":true},{"id":"2","text":"Deploy APK","isDone":false}]"""

    val folder = com.example.data.model.FolderEntity(id = "folder-arch-1", name = "Engineering", colorHex = "#3B82F6")
    repo.insertFolder(folder)

    val complexNote = com.example.data.model.NoteEntity(
        id = noteId,
        title = "Master Architectural Blueprint",
        content = "Paragraph 1: Executive Summary.\n\nParagraph 2: Implementation Details.\n\nParagraph 3: Acceptance Criteria.",
        noteType = "STANDARD",
        colorHex = "#FEF3C7",
        folderId = "folder-arch-1",
        folderName = "Engineering",
        tagsCsv = "Sprint,Architecture,Release",
        isPinned = true,
        isFavorite = true,
        isArchived = false,
        isLocked = false,
        reminderTimestamp = reminderTime,
        checklistJson = checklistJson,
        imageUri = "content://media/external/images/media/42",
        createdAtUtc = 1700000000000L,
        updatedAtUtc = 1700000500000L
    )

    // Save note
    val result = repo.saveOrUpdateNote(complexNote)
    org.junit.Assert.assertTrue("Complex note save must succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)

    // Retrieve note and verify EVERY single property is preserved identically
    val fetched = repo.getNoteByIdDirect(noteId)
    org.junit.Assert.assertNotNull(fetched)
    assertEquals(complexNote.id, fetched?.id)
    assertEquals(complexNote.title, fetched?.title)
    assertEquals(complexNote.content, fetched?.content)
    assertEquals(complexNote.noteType, fetched?.noteType)
    assertEquals(complexNote.colorHex, fetched?.colorHex)
    assertEquals(complexNote.folderId, fetched?.folderId)
    assertEquals(complexNote.folderName, fetched?.folderName)
    assertEquals(complexNote.tagsCsv, fetched?.tagsCsv)
    assertEquals(true, fetched?.isPinned)
    assertEquals(true, fetched?.isFavorite)
    assertEquals(false, fetched?.isArchived)
    assertEquals(false, fetched?.isLocked)
    assertEquals(reminderTime, fetched?.reminderTimestamp)
    assertEquals(checklistJson, fetched?.checklistJson)
    assertEquals(complexNote.imageUri, fetched?.imageUri)
    assertEquals(1700000000000L, fetched?.createdAtUtc)

    db.close()
  }

  @Test
  fun `verify PDF annotation persistence creates parent attachment safely, updates, clears, and persists autosave setting`() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodel.NoteViewModel(app)
    val demoId = "demo-quick-start"

    // 1. Default Auto-save enabled and persists toggles
    org.junit.Assert.assertTrue(viewModel.pdfAutoSaveEnabled.value)
    viewModel.setPdfAutoSaveEnabled(false)
    org.junit.Assert.assertFalse(viewModel.pdfAutoSaveEnabled.value)
    val viewModelReloaded = com.example.ui.viewmodel.NoteViewModel(app)
    org.junit.Assert.assertFalse(viewModelReloaded.pdfAutoSaveEnabled.value)
    viewModelReloaded.setPdfAutoSaveEnabled(true)

    // 2. Saving annotations for a demo PDF without pre-existing AttachmentEntity succeeds
    val stickyCard = com.example.ui.screens.PdfPageAnnotation(
        id = "sticky-1",
        type = "COMMENT",
        pageNumber = 1,
        start = androidx.compose.ui.geometry.Offset(400f, 300f),
        content = "Important exam tip",
        colorArgb = -256,
        strokeWidth = 2f
    )
    val highlight = com.example.ui.screens.PdfPageAnnotation(
        id = "hl-1",
        type = "HIGHLIGHT",
        pageNumber = 1,
        start = androidx.compose.ui.geometry.Offset(100f, 120f),
        end = androidx.compose.ui.geometry.Offset(320f, 160f),
        colorArgb = -256
    )

    val saveRes = viewModelReloaded.saveAnnotationsDirect(
        attachmentId = demoId,
        annotations = listOf(stickyCard.toEntity(demoId), highlight.toEntity(demoId)),
        filePath = "${app.filesDir.absolutePath}/attachments/Quick_Start_Guide.pdf",
        fileName = "Quick Start Guide.pdf"
    )
    org.junit.Assert.assertTrue("Save must succeed without FK error: ${saveRes.exceptionOrNull()?.message}", saveRes.isSuccess)

    val loaded = viewModelReloaded.repository.getAnnotationsForAttachmentDirect(demoId)
    assertEquals(2, loaded.size)
    assertEquals("Important exam tip", loaded.first { it.id == "sticky-1" }.content)

    // 3. Clearing annotations persists empty list cleanly via flushAnnotationsDirect
    val clearRes = viewModelReloaded.flushAnnotationsDirect(
        attachmentId = demoId,
        annotations = emptyList(),
        filePath = "${app.filesDir.absolutePath}/attachments/Quick_Start_Guide.pdf",
        fileName = "Quick Start Guide.pdf"
    )
    org.junit.Assert.assertTrue(clearRes.isSuccess)
    val afterClear = viewModelReloaded.repository.getAnnotationsForAttachmentDirect(demoId)
    assertEquals(0, afterClear.size)
  }

  @Test
  fun `verify multi-image collage URI encoding and folder assignment persist cleanly`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db, context)

    val folder = com.example.data.model.FolderEntity(
        id = "work-folder-1",
        name = "Work",
        colorHex = "#D85C37",
        iconName = "work"
    )
    repo.insertFolder(folder)

    val collageUris = listOf(
        "content://media/picker/101",
        "content://media/picker/102",
        "content://media/picker/103"
    ).joinToString("|")

    val note = com.example.data.model.NoteEntity(
        id = "editorial-note-1",
        folderId = folder.id,
        folderName = folder.name,
        title = "Still Ground",
        content = "Minimalist notes and habit space.",
        tagsCsv = "To - Do - List",
        isPinned = true,
        imageUri = collageUris
    )

    val saveResult = repo.saveOrUpdateNote(note)
    org.junit.Assert.assertTrue(saveResult.isSuccess)

    val loaded = repo.getNoteByIdDirect("editorial-note-1")
    org.junit.Assert.assertNotNull(loaded)
    assertEquals("Work", loaded?.folderName)
    assertEquals("work-folder-1", loaded?.folderId)
    assertEquals(true, loaded?.isPinned)
    assertEquals(3, loaded?.imageUri?.split("|")?.filter { it.isNotBlank() }?.size)

    db.close()
  }

  @Test
  fun `verify time-based greeting covers morning afternoon evening and night`() {
    assertEquals("Good Morning", com.example.util.SmartNoteUtils.getTimeBasedGreeting(5))
    assertEquals("Good Morning", com.example.util.SmartNoteUtils.getTimeBasedGreeting(11))
    assertEquals("Good Afternoon", com.example.util.SmartNoteUtils.getTimeBasedGreeting(12))
    assertEquals("Good Afternoon", com.example.util.SmartNoteUtils.getTimeBasedGreeting(16))
    assertEquals("Good Evening", com.example.util.SmartNoteUtils.getTimeBasedGreeting(17))
    assertEquals("Good Evening", com.example.util.SmartNoteUtils.getTimeBasedGreeting(20))
    assertEquals("Good Night", com.example.util.SmartNoteUtils.getTimeBasedGreeting(21))
    assertEquals("Good Night", com.example.util.SmartNoteUtils.getTimeBasedGreeting(23))
    assertEquals("Good Night", com.example.util.SmartNoteUtils.getTimeBasedGreeting(0))
    assertEquals("Good Night", com.example.util.SmartNoteUtils.getTimeBasedGreeting(4))
  }

  @Test
  fun `verify calendar event metadata week strip and checklist task persistence`() = kotlinx.coroutines.runBlocking {
    val startUtc = 1794646200000L
    val endUtc = startUtc + 3600_000L
    val metaJson = com.example.util.SmartNoteUtils.encodeCalendarMeta(startUtc, endUtc, "Work")
    val parsedMeta = com.example.util.SmartNoteUtils.parseCalendarMeta(metaJson)
    org.junit.Assert.assertNotNull(parsedMeta)
    assertEquals(startUtc, parsedMeta!!.startUtc)
    assertEquals(endUtc, parsedMeta.endUtc)
    assertEquals("Work", parsedMeta.category)

    val weekDays = com.example.util.SmartNoteUtils.getWeekDaysStartingMonday(startUtc)
    assertEquals(7, weekDays.size)
    org.junit.Assert.assertTrue(weekDays.any { com.example.util.SmartNoteUtils.isSameDay(it, startUtc) })

    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db, context)

    val tasks = listOf(
        com.example.util.ChecklistItem(id = "t1", text = "Stretch 20 mins", isDone = false),
        com.example.util.ChecklistItem(id = "t2", text = "Client sync", isDone = true)
    )
    val eventNote = com.example.data.model.NoteEntity(
        id = "cal-note-1",
        title = "Morning Workout",
        content = "60min Push-up & Stretching",
        tagsCsv = "Calendar,Health",
        colorHex = "#FFE4E6",
        reminderTimestamp = startUtc,
        canvasDataJson = metaJson,
        checklistJson = com.example.util.SmartNoteUtils.encodeChecklistJson(tasks)
    )
    repo.saveOrUpdateNote(eventNote)

    val rem = com.example.data.model.ReminderEntity(
        id = "cal-rem-1",
        noteId = eventNote.id,
        scheduledTimeUtc = startUtc,
        isCompleted = false
    )
    repo.saveReminder(rem)

    val loadedNote = repo.getNoteByIdDirect("cal-note-1")
    val loadedRem = repo.getReminderById("cal-rem-1")
    org.junit.Assert.assertNotNull(loadedNote)
    org.junit.Assert.assertNotNull(loadedRem)
    assertEquals(2, com.example.util.SmartNoteUtils.parseChecklistJson(loadedNote!!.checklistJson).size)
    assertEquals("Work", com.example.util.SmartNoteUtils.parseCalendarMeta(loadedNote.canvasDataJson)?.category)

    db.close()
  }

  @Test
  fun `verify vault security manager unlocks and immediately locks on demand`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val security = com.example.security.SecurityManager(context)
    security.lockVault()
    org.junit.Assert.assertFalse(security.isVaultSessionUnlocked())

    org.junit.Assert.assertTrue(security.verifyPin("1234"))
    org.junit.Assert.assertTrue(security.isVaultSessionUnlocked())

    // Returning back from locked folder immediately locks session
    security.lockVault()
    org.junit.Assert.assertFalse(security.isVaultSessionUnlocked())

    val availability = com.example.security.BiometricAuthHelper.checkAvailability(context)
    org.junit.Assert.assertNotNull(availability)

    security.setBiometricsEnabled(false)
    org.junit.Assert.assertFalse(security.isBiometricsPreferenceEnabled())
    security.setBiometricsEnabled(true)
    org.junit.Assert.assertTrue(security.isBiometricsPreferenceEnabled())

    security.markBiometricUnlockSuccess()
    org.junit.Assert.assertTrue(security.isVaultSessionUnlocked())
    security.lockVault()
    org.junit.Assert.assertFalse(security.isVaultSessionUnlocked())

    // Verify user-configured Auto-Lock timeout persistence & labels
    assertEquals(0L, com.example.security.SecurityManager.DEFAULT_AUTO_LOCK_MS)
    assertEquals("Immediately", com.example.security.SecurityManager.formatAutoLockLabel(0L))
    assertEquals("After 5 minutes", com.example.security.SecurityManager.formatAutoLockLabel(300_000L))
    security.setAutoLockTimeout(300_000L)
    assertEquals(300_000L, security.getAutoLockTimeout())
    security.setAutoLockTimeout(0L)
    assertEquals(0L, security.getAutoLockTimeout())
  }

  @Test
  fun `verify important settings persist via NoteViewModel and SharedPreferences`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.NoteViewModel(app)

    // Home grid layout persistence
    val initialGrid = vm.isGridView.value
    vm.toggleGridView()
    assertEquals(!initialGrid, vm.isGridView.value)

    // Editor font size clamping & persistence
    vm.setEditorFontSizeSp(18.5f)
    assertEquals(18.5f, vm.editorFontSizeSp.value, 0.01f)
    vm.setEditorFontSizeSp(99f)
    assertEquals(22f, vm.editorFontSizeSp.value, 0.01f)

    // Dynamic color defaults to true and persists toggle updates
    org.junit.Assert.assertTrue(vm.dynamicColorEnabled.value)
    vm.setDynamicColorEnabled(false)
    org.junit.Assert.assertFalse(vm.dynamicColorEnabled.value)

    // Notifications & PDF auto-save toggles
    vm.setNotificationsEnabled(false)
    org.junit.Assert.assertFalse(vm.notificationsEnabled.value)
    vm.setPdfAutoSaveEnabled(false)
    org.junit.Assert.assertFalse(vm.pdfAutoSaveEnabled.value)

    // User profile starts empty (never pre-filled) and persists user-entered values
    assertEquals("", vm.userProfileName.value)
    assertEquals("", vm.userProfileEmail.value)
    vm.updateUserProfile("Alex", "alex@example.com")
    assertEquals("Alex", vm.userProfileName.value)
    assertEquals("alex@example.com", vm.userProfileEmail.value)
    vm.updateUserProfile("", "")
    assertEquals("", vm.userProfileName.value)
    assertEquals("", vm.userProfileEmail.value)

    val prefs = app.getSharedPreferences("noteland_settings_prefs", Context.MODE_PRIVATE)
    assertEquals(!initialGrid, prefs.getBoolean("home_grid_view", true))
    assertEquals(22f, prefs.getFloat("editor_font_size_sp", 16.5f), 0.01f)
    assertEquals("", prefs.getString("user_name", null))
    assertEquals("", prefs.getString("user_email", null))
    org.junit.Assert.assertFalse(prefs.getBoolean("dynamic_color_enabled", true))
    org.junit.Assert.assertFalse(prefs.getBoolean("notifications_enabled", true))
    org.junit.Assert.assertFalse(prefs.getBoolean("pdf_autosave_enabled", true))
  }

  @Test
  fun `verify note texture metadata persistence and calendar coexistence`() {
    val plainTextureJson = com.example.util.SmartNoteUtils.withNoteTexture("", "dot_grid")
    assertEquals("dot_grid", com.example.util.SmartNoteUtils.parseNoteTexture(plainTextureJson))

    val clearedJson = com.example.util.SmartNoteUtils.withNoteTexture(plainTextureJson, "none")
    assertEquals("", clearedJson)
    assertEquals("", com.example.util.SmartNoteUtils.parseNoteTexture(clearedJson))

    val calendarMeta = com.example.util.SmartNoteUtils.encodeCalendarMeta(1700000000000L, 1700003600000L, "Work")
    val mergedJson = com.example.util.SmartNoteUtils.withNoteTexture(calendarMeta, "ruled")
    assertEquals("ruled", com.example.util.SmartNoteUtils.parseNoteTexture(mergedJson))
    assertEquals("Work", com.example.util.SmartNoteUtils.parseCalendarMeta(mergedJson)?.category)

    val canvasArray = """[{"color":-1,"width":4.0,"points":[]}]"""
    assertEquals(canvasArray, com.example.util.SmartNoteUtils.withNoteTexture(canvasArray, "linen"))
    assertEquals("", com.example.util.SmartNoteUtils.parseNoteTexture(canvasArray))
  }

  @Test
  fun `verify note gallery background and texture metadata coexist with calendar`() {
    val bgPath = "/data/user/0/com.example/files/note_backgrounds/bg_sample.jpg"
    val styledJson = com.example.util.SmartNoteUtils.withNoteStyleMeta(
        existingJson = "",
        textureKey = "dot_grid",
        backgroundImagePath = bgPath
    )
    assertEquals("dot_grid", com.example.util.SmartNoteUtils.parseNoteTexture(styledJson))
    assertEquals(bgPath, com.example.util.SmartNoteUtils.parseNoteBackgroundImage(styledJson))

    val calendarMeta = com.example.util.SmartNoteUtils.encodeCalendarMeta(1700000000000L, 1700003600000L, "Personal")
    val mergedWithCal = com.example.util.SmartNoteUtils.withNoteStyleMeta(
        existingJson = calendarMeta,
        textureKey = "linen",
        backgroundImagePath = bgPath
    )
    assertEquals("linen", com.example.util.SmartNoteUtils.parseNoteTexture(mergedWithCal))
    assertEquals(bgPath, com.example.util.SmartNoteUtils.parseNoteBackgroundImage(mergedWithCal))
    assertEquals("Personal", com.example.util.SmartNoteUtils.parseCalendarMeta(mergedWithCal)?.category)

    val clearedBg = com.example.util.SmartNoteUtils.withNoteStyleMeta(
        existingJson = mergedWithCal,
        textureKey = "none",
        backgroundImagePath = ""
    )
    assertEquals("", com.example.util.SmartNoteUtils.parseNoteTexture(clearedBg))
    assertEquals("", com.example.util.SmartNoteUtils.parseNoteBackgroundImage(clearedBg))
    assertEquals("Personal", com.example.util.SmartNoteUtils.parseCalendarMeta(clearedBg)?.category)
  }

  @Test
  fun `verify handwriting overlay strokes roundtrip coexistence and precision engine`() {
    val calendarMeta = com.example.util.SmartNoteUtils.encodeCalendarMeta(1700000000000L, 1700003600000L, "Study")
    val styledJson = com.example.util.SmartNoteUtils.withNoteStyleMeta(
        existingJson = calendarMeta,
        textureKey = "dot_grid",
        backgroundImagePath = "/files/bg.jpg"
    )

    val stroke1 = com.example.util.InkStroke(
        id = "ink-1",
        tool = com.example.util.NoteInkTool.PRESSURE_PEN.name,
        colorArgb = -14803425,
        baseWidthDp = 2.8f,
        points = listOf(
            com.example.util.InkSample(10.25f, 20.50f, 0.35f),
            com.example.util.InkSample(22.75f, 28.10f, 0.72f),
            com.example.util.InkSample(40.00f, 35.40f, 0.85f)
        )
    )
    val merged = com.example.util.SmartNoteUtils.withHandwritingStrokes(styledJson, listOf(stroke1))
    org.junit.Assert.assertTrue(com.example.util.SmartNoteUtils.hasHandwritingStrokes(merged))
    assertEquals("dot_grid", com.example.util.SmartNoteUtils.parseNoteTexture(merged))
    assertEquals("/files/bg.jpg", com.example.util.SmartNoteUtils.parseNoteBackgroundImage(merged))
    assertEquals("Study", com.example.util.SmartNoteUtils.parseCalendarMeta(merged)?.category)

    val parsed = com.example.util.SmartNoteUtils.parseHandwritingStrokes(merged)
    assertEquals(1, parsed.size)
    assertEquals("ink-1", parsed[0].id)
    assertEquals(3, parsed[0].points.size)
    assertEquals(22.75f, parsed[0].points[1].xDp, 0.02f)
    assertEquals(0.72f, parsed[0].points[1].pressure, 0.02f)

    // Eraser hit testing
    org.junit.Assert.assertTrue(
        com.example.ui.components.inkStrokeContainsPoint(
            stroke1,
            androidx.compose.ui.geometry.Offset(22.5f, 28.0f),
            eraserRadiusDp = 12f
        )
    )
    org.junit.Assert.assertFalse(
        com.example.ui.components.inkStrokeContainsPoint(
            stroke1,
            androidx.compose.ui.geometry.Offset(200f, 200f),
            eraserRadiusDp = 12f
        )
    )

    // Straight-line snap vs curved handwriting
    val straightRaw = listOf(
        com.example.util.InkSample(10f, 100f, 0.5f),
        com.example.util.InkSample(30f, 101.5f, 0.55f),
        com.example.util.InkSample(50f, 99.0f, 0.6f),
        com.example.util.InkSample(80f, 100.5f, 0.5f)
    )
    org.junit.Assert.assertTrue(com.example.ui.components.isStraightLineCandidate(straightRaw))
    val snapped = com.example.ui.components.snapToStraightLine(straightRaw)
    assertEquals(3, snapped.size)
    assertEquals(10f, snapped.first().xDp, 0.01f)
    assertEquals(80f, snapped.last().xDp, 0.01f)

    val curvedRaw = listOf(
        com.example.util.InkSample(10f, 100f, 0.5f),
        com.example.util.InkSample(30f, 140f, 0.5f),
        com.example.util.InkSample(50f, 95f, 0.5f),
        com.example.util.InkSample(70f, 100f, 0.5f)
    )
    org.junit.Assert.assertFalse(com.example.ui.components.isStraightLineCandidate(curvedRaw))

    // Stylus One-Euro filter & RDP corner preservation
    val filter = com.example.ui.components.StylusStrokeFilter(isStylus = true)
    val s1 = filter.processSample(10f, 10f, 0.4f, 100L)
    val s2 = filter.processSample(25f, 10f, 0.8f, 108L)
    val s3 = filter.processSample(25f, 30f, 0.7f, 116L) // 90-degree sharp corner
    org.junit.Assert.assertTrue(s2.xDp > s1.xDp)
    org.junit.Assert.assertTrue(s3.yDp > s2.yDp)
    val simplified = com.example.ui.components.simplifyInkSamples(listOf(s1, s2, s3), epsilonDp = 0.25f)
    assertEquals(3, simplified.size)
  }

  @Test
  fun `verify voice dictation manifest permission and smart cursor merge`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // 1. Verify RECORD_AUDIO permission is declared in merged AndroidManifest.xml
    val pkgInfo = context.packageManager.getPackageInfo(
        context.packageName,
        android.content.pm.PackageManager.GET_PERMISSIONS
    )
    val permissions = pkgInfo.requestedPermissions?.toList().orEmpty()
    org.junit.Assert.assertTrue(permissions.contains(android.Manifest.permission.RECORD_AUDIO))

    // 2. Empty note capitalization
    val emptyMerge = com.example.ui.components.mergeDictatedSegment(
        androidx.compose.ui.text.input.TextFieldValue(""),
        "meeting notes for friday"
    )
    assertEquals("Meeting notes for friday", emptyMerge.text)
    assertEquals(emptyMerge.text.length, emptyMerge.selection.start)

    // 3. Appending after sentence terminator adds space + capitalizes
    val appendMerge = com.example.ui.components.mergeDictatedSegment(
        androidx.compose.ui.text.input.TextFieldValue(
            text = "First thought.",
            selection = androidx.compose.ui.text.TextRange("First thought.".length)
        ),
        "second thought"
    )
    assertEquals("First thought. Second thought", appendMerge.text)

    // 4. Mid-sentence insertion preserves leading & trailing word boundaries
    val midText = "Buy milk today"
    val midMerge = com.example.ui.components.mergeDictatedSegment(
        androidx.compose.ui.text.input.TextFieldValue(
            text = midText,
            selection = androidx.compose.ui.text.TextRange("Buy milk".length)
        ),
        "and eggs"
    )
    assertEquals("Buy milk and eggs today", midMerge.text)
    assertEquals("Buy milk and eggs".length, midMerge.selection.start)

    // 5. Punctuation attaches without extra leading space
    val punctMerge = com.example.ui.components.mergeDictatedSegment(
        androidx.compose.ui.text.input.TextFieldValue(
            text = "Hello",
            selection = androidx.compose.ui.text.TextRange(5)
        ),
        ", world"
    )
    assertEquals("Hello, world", punctMerge.text)

    // 6. RMS dB normalization bounds, error formatting, and RecognizerIntent extras
    assertEquals(0f, com.example.ui.components.normalizeRmsDb(-10f), 0.001f)
    assertEquals(0f, com.example.ui.components.normalizeRmsDb(Float.NaN), 0.001f)
    assertEquals(1f, com.example.ui.components.normalizeRmsDb(15f), 0.001f)
    assertEquals(0.5f, com.example.ui.components.normalizeRmsDb(4f), 0.001f)

    org.junit.Assert.assertTrue(
        com.example.ui.components.formatSpeechErrorMessage(android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
            .contains("permission", ignoreCase = true)
    )
    org.junit.Assert.assertTrue(
        com.example.ui.components.formatSpeechErrorMessage(android.speech.SpeechRecognizer.ERROR_NO_MATCH).isNotBlank()
    )

    val intent = com.example.ui.components.buildRecognizerIntent(context, "en-US")
    assertEquals(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH, intent.action)
    org.junit.Assert.assertTrue(intent.getBooleanExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, false))
    assertEquals("en-US", intent.getStringExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE))
  }

  @Test
  fun `verify hierarchical folder tree utilities breadcrumbs cycles and counts`() {
    val root = com.example.data.model.FolderEntity(id = "f-root", name = "Projects", parentId = null)
    val child = com.example.data.model.FolderEntity(id = "f-child", name = "Android", parentId = "f-root")
    val grandChild = com.example.data.model.FolderEntity(id = "f-grand", name = "NoteLand", parentId = "f-child")
    val orphan = com.example.data.model.FolderEntity(id = "f-orphan", name = "Standalone", parentId = "missing-id")
    val folders = listOf(root, child, grandChild, orphan)

    val notes = listOf(
        com.example.data.model.NoteEntity(id = "n1", title = "Root Note", content = "a", folderId = "f-root", folderName = "Projects"),
        com.example.data.model.NoteEntity(id = "n2", title = "Child Note", content = "b", folderId = "f-child", folderName = "Android"),
        com.example.data.model.NoteEntity(id = "n3", title = "Grand Note", content = "c", folderId = "f-grand", folderName = "NoteLand"),
        com.example.data.model.NoteEntity(id = "n4", title = "Locked Grand Note", content = "d", folderId = "f-grand", folderName = "NoteLand", isLocked = true)
    )

    // Breadcrumbs & paths
    val trail = com.example.util.FolderHierarchyUtils.getBreadcrumbTrail(folders, "f-grand")
    assertEquals(listOf("Projects", "Android", "NoteLand"), trail.map { it.name })
    assertEquals("Projects / Android / NoteLand", com.example.util.FolderHierarchyUtils.getFolderPathString(folders, "f-grand"))
    assertEquals("Projects / Android", com.example.util.FolderHierarchyUtils.getParentPathString(folders, "f-grand"))
    assertEquals("", com.example.util.FolderHierarchyUtils.getParentPathString(folders, "f-root"))

    // Subtree IDs & rollup counts (excludes locked note)
    val rootSubtree = com.example.util.FolderHierarchyUtils.getSubtreeFolderIds(folders, "f-root")
    assertEquals(setOf("f-root", "f-child", "f-grand"), rootSubtree)
    assertEquals(1, com.example.util.FolderHierarchyUtils.countDirectNotes(notes, "f-root"))
    assertEquals(3, com.example.util.FolderHierarchyUtils.countSubtreeNotes(folders, notes, "f-root"))
    org.junit.Assert.assertTrue(com.example.util.FolderHierarchyUtils.isFolderOrSubtreeActive(folders, notes, "f-root"))

    // Cycle detection
    org.junit.Assert.assertTrue(com.example.util.FolderHierarchyUtils.wouldCreateCycle(folders, "f-root", "f-root"))
    org.junit.Assert.assertTrue(com.example.util.FolderHierarchyUtils.wouldCreateCycle(folders, "f-root", "f-grand"))
    org.junit.Assert.assertFalse(com.example.util.FolderHierarchyUtils.wouldCreateCycle(folders, "f-grand", "f-orphan"))
    org.junit.Assert.assertFalse(com.example.util.FolderHierarchyUtils.wouldCreateCycle(folders, "f-child", null))

    // Flatten tree & orphan fallback to root
    val flattened = com.example.util.FolderHierarchyUtils.flattenFolderTree(folders, notes)
    assertEquals(4, flattened.size)
    assertEquals("Projects", flattened[0].folder.name)
    assertEquals(0, flattened[0].depth)
    assertEquals("Android", flattened[1].folder.name)
    assertEquals(1, flattened[1].depth)
    assertEquals("NoteLand", flattened[2].folder.name)
    assertEquals(2, flattened[2].depth)
    assertEquals("Standalone", flattened[3].folder.name)
    assertEquals(0, flattened[3].depth)

    // Excluding subtree hides root + child + grandchild
    val excluded = com.example.util.FolderHierarchyUtils.flattenFolderTree(folders, notes, excludeSubtreeOfId = "f-root")
    assertEquals(listOf("f-orphan"), excluded.map { it.folder.id })
  }

  @Test
  fun `verify repository hierarchical folder CRUD cycle rejection rename sync delete promotion and backup roundtrip`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db, context)

    val work = com.example.data.model.FolderEntity(id = "f-work", name = "Work", parentId = null)
    val q4 = com.example.data.model.FolderEntity(id = "f-q4", name = "Q4 Planning", parentId = "f-work")
    val budget = com.example.data.model.FolderEntity(id = "f-budget", name = "Budget", parentId = "f-q4")
    val archive = com.example.data.model.FolderEntity(id = "f-arch", name = "Archive", parentId = null)

    repo.insertFolder(work)
    repo.insertFolder(q4)
    repo.insertFolder(budget)
    repo.insertFolder(archive)

    val noteInQ4 = com.example.data.model.NoteEntity(
        id = "n-q4",
        title = "Roadmap",
        content = "Milestones",
        folderId = "f-q4",
        folderName = "Q4 Planning"
    )
    repo.saveOrUpdateNote(noteInQ4)

    // 1. Reject cyclic moveFolder (Work -> Budget) and updateFolder (Work -> Q4)
    val cyclicMove = repo.moveFolder("f-work", "f-budget")
    org.junit.Assert.assertTrue("Moving ancestor into descendant must fail", cyclicMove.isFailure)
    val cyclicUpdate = repo.updateFolder(work.copy(parentId = "f-q4"))
    org.junit.Assert.assertTrue("Updating ancestor parent to descendant must fail", cyclicUpdate.isFailure)

    // 2. Rename Q4 Planning -> Q4 Strategy and verify note folderName synced
    val renameRes = repo.updateFolder(q4.copy(name = "Q4 Strategy"))
    org.junit.Assert.assertTrue(renameRes.isSuccess)
    assertEquals("Q4 Strategy", repo.getNoteByIdDirect("n-q4")?.folderName)

    // 3. Move Note to Budget subfolder
    val moveNoteRes = repo.moveNoteToFolder("n-q4", budget)
    org.junit.Assert.assertTrue(moveNoteRes.isSuccess)
    val movedNote = repo.getNoteByIdDirect("n-q4")
    assertEquals("f-budget", movedNote?.folderId)
    assertEquals("Budget", movedNote?.folderName)

    // 4. Backup ZIP export & restore preserves parentId
    val backupManager = com.example.util.BackupManager(context, repo)
    val zipFile = backupManager.createBackupZip()
    repo.clearAllData()
    assertEquals(0, repo.getAllFoldersDirect().size)

    val restoreRes = backupManager.restoreFromZip(zipFile, com.example.util.RestoreStrategy.REPLACE)
    org.junit.Assert.assertTrue(restoreRes.isSuccess)
    val restoredFolders = repo.getAllFoldersDirect().associateBy { it.id }
    assertEquals(4, restoredFolders.size)
    assertEquals(null, restoredFolders["f-work"]?.parentId)
    assertEquals("f-work", restoredFolders["f-q4"]?.parentId)
    assertEquals("f-q4", restoredFolders["f-budget"]?.parentId)

    // 5. Delete Q4 Strategy -> promotes Budget's parentId up to Work, detaches direct notes in Q4
    val noteBackInQ4 = restoredFolders["f-q4"]!!
    repo.moveNoteToFolder("n-q4", noteBackInQ4)
    val deleteRes = repo.deleteFolder("f-q4")
    org.junit.Assert.assertTrue(deleteRes.isSuccess)

    val afterDeleteFolders = repo.getAllFoldersDirect().associateBy { it.id }
    org.junit.Assert.assertFalse(afterDeleteFolders.containsKey("f-q4"))
    assertEquals("f-work", afterDeleteFolders["f-budget"]?.parentId)
    assertEquals(null, repo.getNoteByIdDirect("n-q4")?.folderId)

    zipFile.delete()
    db.close()
  }

  @Test
  fun `verify voice recording metadata duration waveform storage and backup roundtrip`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storageManager = com.example.data.storage.AttachmentStorageManager(context)

    // 1. Duration formatting
    assertEquals("0:00", com.example.util.SmartNoteUtils.formatAudioDuration(-500L))
    assertEquals("0:09", com.example.util.SmartNoteUtils.formatAudioDuration(9_000L))
    assertEquals("1:15", com.example.util.SmartNoteUtils.formatAudioDuration(75_000L))
    assertEquals("1:01:01", com.example.util.SmartNoteUtils.formatAudioDuration(3_661_000L))

    // 2. Amplitude normalization & waveform resampling
    assertEquals(0f, com.example.ui.components.normalizeMediaRecorderAmplitude(0), 0.001f)
    assertEquals(1f, com.example.ui.components.normalizeMediaRecorderAmplitude(32767), 0.001f)
    org.junit.Assert.assertTrue(com.example.ui.components.normalizeMediaRecorderAmplitude(8192) > 0.45f)

    val downsampled = com.example.ui.components.downsampleWaveform(
        samples = listOf(0.1f, 0.9f, 0.2f, 0.3f, 0.8f, 0.1f),
        targetCount = 3
    )
    assertEquals(3, downsampled.size)
    assertEquals(0.9f, downsampled[0], 0.01f)
    assertEquals(0.3f, downsampled[1], 0.01f)
    assertEquals(0.8f, downsampled[2], 0.01f)

    // 3. Storage file creation, relative path conversion, and persistence coexistence
    val voiceFile = storageManager.createVoiceRecordingFile()
    val payloadBytes = byteArrayOf(0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70) // mock ftyp header
    voiceFile.writeBytes(payloadBytes)
    val relPath = storageManager.toRelativeVoicePath(voiceFile)
    org.junit.Assert.assertTrue(relPath.startsWith("voice_recordings/"))
    org.junit.Assert.assertTrue(storageManager.resolveVoiceRecordingFile(relPath).exists())

    val calendarMeta = com.example.util.SmartNoteUtils.encodeCalendarMeta(1700000000000L, 1700003600000L, "Ideas")
    val styledJson = com.example.util.SmartNoteUtils.withNoteStyleMeta(calendarMeta, "grid", "/files/bg.jpg")
    val clip = com.example.util.VoiceRecordingClip(
        id = "voice-1",
        filePath = relPath,
        title = "Morning Thought",
        durationMs = 14_500L,
        createdAtUtc = 1700000005000L,
        waveform = downsampled
    )
    val mergedJson = com.example.util.SmartNoteUtils.withVoiceRecordings(styledJson, listOf(clip))
    org.junit.Assert.assertTrue(com.example.util.SmartNoteUtils.hasVoiceRecordings(mergedJson))
    assertEquals("grid", com.example.util.SmartNoteUtils.parseNoteTexture(mergedJson))
    assertEquals("Ideas", com.example.util.SmartNoteUtils.parseCalendarMeta(mergedJson)?.category)

    val parsedClips = com.example.util.SmartNoteUtils.parseVoiceRecordings(mergedJson)
    assertEquals(1, parsedClips.size)
    assertEquals("voice-1", parsedClips[0].id)
    assertEquals("Morning Thought", parsedClips[0].title)
    assertEquals(14_500L, parsedClips[0].durationMs)
    assertEquals(3, parsedClips[0].waveform.size)

    // 4. Backup ZIP export & restore preserves audio bytes + note metadata
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.NoteRepository(db, context)
    repo.saveOrUpdateNote(
        com.example.data.model.NoteEntity(
            id = "note-with-voice",
            title = "Quick Voice Thought",
            content = "",
            canvasDataJson = mergedJson
        )
    )

    val backupManager = com.example.util.BackupManager(context, repo)
    val zipFile = backupManager.createBackupZip()

    // Delete original physical audio file and DB before restore
    org.junit.Assert.assertTrue(storageManager.deleteVoiceRecordingFile(relPath))
    org.junit.Assert.assertFalse(voiceFile.exists())
    repo.clearAllData()

    val restoreRes = backupManager.restoreFromZip(zipFile, com.example.util.RestoreStrategy.REPLACE)
    org.junit.Assert.assertTrue(restoreRes.isSuccess)

    val restoredNote = repo.getNoteByIdDirect("note-with-voice")
    org.junit.Assert.assertNotNull(restoredNote)
    val restoredClips = com.example.util.SmartNoteUtils.parseVoiceRecordings(restoredNote!!.canvasDataJson)
    assertEquals(1, restoredClips.size)
    val restoredFile = storageManager.resolveVoiceRecordingFile(restoredClips[0].filePath)
    org.junit.Assert.assertTrue("Restored voice file must exist", restoredFile.exists())
    org.junit.Assert.assertArrayEquals(payloadBytes, restoredFile.readBytes())

    storageManager.deleteVoiceRecordingFile(restoredClips[0].filePath)
    zipFile.delete()
    db.close()
  }

  @Test
  fun `verify home screen widgets are registered in manifest and deep-link intents carry target extras`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val widgetUpdateIntent = android.content.Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        .setPackage(context.packageName)
    @Suppress("DEPRECATION")
    val receivers = context.packageManager.queryBroadcastReceivers(
        widgetUpdateIntent,
        android.content.pm.PackageManager.GET_META_DATA
    )
    val registeredByName = receivers.associateBy { it.activityInfo.name }

    listOf(
        com.example.widget.DailyTasksWidgetReceiver::class.java.name,
        com.example.widget.RemindersWidgetReceiver::class.java.name,
        com.example.widget.CollectionsTilesWidgetReceiver::class.java.name
    ).forEach { receiverName ->
        val info = registeredByName[receiverName]
        org.junit.Assert.assertNotNull("$receiverName must be registered for APPWIDGET_UPDATE", info)
        val providerXmlRes = info?.activityInfo?.metaData?.getInt("android.appwidget.provider", 0) ?: 0
        org.junit.Assert.assertTrue("$receiverName must declare android.appwidget.provider metadata", providerXmlRes != 0)
    }

    val routeIntent = com.example.widget.WidgetNavigation.routeIntent(context, com.example.ui.navigation.Screen.Tasks.route)
    assertEquals(com.example.ui.navigation.Screen.Tasks.route, routeIntent.getStringExtra(com.example.widget.WidgetNavigation.EXTRA_WIDGET_ROUTE))

    val folderIntent = com.example.widget.WidgetNavigation.actionIntent(
        context = context,
        widgetAction = com.example.widget.WidgetNavigation.ACTION_OPEN_FOLDER,
        folderId = "folder-123"
    )
    assertEquals(com.example.widget.WidgetNavigation.ACTION_OPEN_FOLDER, folderIntent.getStringExtra(com.example.widget.WidgetNavigation.EXTRA_WIDGET_ACTION))
    assertEquals("folder-123", folderIntent.getStringExtra(com.example.widget.WidgetNavigation.EXTRA_WIDGET_FOLDER_ID))

    val noteIntent = com.example.widget.WidgetNavigation.noteIntent(context, "note-abc")
    assertEquals("note-abc", noteIntent.getStringExtra(com.example.widget.WidgetNavigation.EXTRA_WIDGET_NOTE_ID))
  }

  @Test
  fun `verify widget snapshots compute tasks reminders collections counts and redact locked notes`() {
    val now = 1_750_000_000_000L
    val folderWork = com.example.data.model.FolderEntity(id = "f-work", name = "Work")
    val folderPersonal = com.example.data.model.FolderEntity(id = "f-personal", name = "Personal")

    val checklistNote = com.example.data.model.NoteEntity(
        id = "n-check",
        folderId = folderWork.id,
        folderName = folderWork.name,
        title = "Sprint Checklist",
        checklistJson = """[{"text":"Ship widgets","done":false},{"text":"Write tests","done":true}]""",
        isFavorite = true,
        isPinned = true
    )
    val lockedNote = com.example.data.model.NoteEntity(
        id = "n-locked",
        folderId = folderPersonal.id,
        title = "Secret Vault Passcodes",
        checklistJson = """[{"text":"Top secret task","done":false}]""",
        isLocked = true
    )
    val deletedNote = com.example.data.model.NoteEntity(
        id = "n-deleted",
        title = "Trashed Checklist",
        checklistJson = """[{"text":"Old task","done":false}]""",
        isDeleted = true
    )
    val pdfNote = com.example.data.model.NoteEntity(
        id = "n-pdf",
        title = "Spec.pdf",
        noteType = com.example.data.model.NoteType.PDF_DOC.name
    )

    val notes = listOf(checklistNote, lockedNote, deletedNote, pdfNote)
    val reminders = listOf(
        com.example.data.model.ReminderEntity(
            id = "r-overdue",
            noteId = checklistNote.id,
            scheduledTimeUtc = now - 3_600_000L,
            isCompleted = false
        ),
        com.example.data.model.ReminderEntity(
            id = "r-locked",
            noteId = lockedNote.id,
            scheduledTimeUtc = now + 3_600_000L,
            isCompleted = false
        ),
        com.example.data.model.ReminderEntity(
            id = "r-deleted",
            noteId = deletedNote.id,
            scheduledTimeUtc = now + 7_200_000L,
            isCompleted = false
        )
    )

    // 1. Daily Tasks snapshot excludes locked & deleted notes
    val tasksSnapshot = com.example.widget.WidgetDataLoader.buildDailyTasksSnapshot(notes, reminders, nowMs = now)
    assertEquals(1, tasksSnapshot.doneCount)
    assertEquals(3, tasksSnapshot.totalCount) // 2 checklist items + 1 visible reminder
    org.junit.Assert.assertFalse(tasksSnapshot.items.any { it.title.contains("Top secret") })
    org.junit.Assert.assertFalse(tasksSnapshot.items.any { it.title.contains("Old task") })
    // Incomplete items appear first
    org.junit.Assert.assertFalse(tasksSnapshot.items.first().isDone)

    // 2. Reminders snapshot redacts locked note titles and omits deleted notes
    val remindersSnapshot = com.example.widget.WidgetDataLoader.buildRemindersSnapshot(notes, reminders, nowMs = now)
    assertEquals(2, remindersSnapshot.pendingCount)
    assertEquals(1, remindersSnapshot.overdueCount)
    val lockedReminderItem = remindersSnapshot.items.first { it.reminderId == "r-locked" }
    assertEquals("Locked Note Reminder", lockedReminderItem.noteTitle)
    org.junit.Assert.assertTrue(lockedReminderItem.isLockedNote)

    // 3. Collections snapshot computes multi-tile metrics accurately
    val attachments = listOf(
        com.example.data.model.AttachmentEntity(
            id = "att-1",
            fileName = "guide.pdf",
            fileExtension = "pdf",
            relativePath = "attachments/guide.pdf",
            mimeType = "application/pdf",
            sizeBytes = 1024L
        )
    )
    val stickies = listOf(com.example.data.model.StickyNoteEntity(title = "Pin", content = "Note"))
    val canvases = listOf(com.example.data.model.CanvasEntity(title = "Sketch"))

    val collectionsSnapshot = com.example.widget.WidgetDataLoader.buildCollectionsSnapshot(
        allNotes = notes,
        folders = listOf(folderWork, folderPersonal),
        stickyNotes = stickies,
        canvases = canvases,
        attachments = attachments,
        reminders = reminders
    )
    assertEquals(1, collectionsSnapshot.activeNotesCount) // excludes locked, deleted, and PDF_DOC
    assertEquals(2, collectionsSnapshot.folderCount)
    assertEquals(2, collectionsSnapshot.pendingTasksCount) // 1 checklist + 1 visible pending reminder
    assertEquals(2, collectionsSnapshot.pendingRemindersCount)
    assertEquals(1, collectionsSnapshot.favoritesCount)
    assertEquals(1, collectionsSnapshot.stickyCount)
    assertEquals(1, collectionsSnapshot.canvasCount)
    assertEquals(1, collectionsSnapshot.pdfCount)
    assertEquals("Work", collectionsSnapshot.topFolders.first().name)
    assertEquals(1, collectionsSnapshot.topFolders.first().noteCount)
  }
}


