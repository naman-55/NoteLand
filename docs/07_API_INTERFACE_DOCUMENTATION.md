# NoteLand — API & Interface Documentation

## 1. Scope
NoteLand is a standalone, client-side, offline-first Android application. It does not expose HTTP REST endpoints or gRPC network servers. This document defines the internal domain interfaces, manager contracts, and boundary abstractions governing NoteLand's component interactions.

---

## 2. Core Repository Interface: `INoteRepository`

Located at: `com.example.noteland.data.repository.INoteRepository`

```kotlin
interface INoteRepository {
    // --- Note Streams & CRUD ---
    fun getActiveNotes(): Flow<List<NoteWithDetails>>
    fun getArchivedNotes(): Flow<List<NoteWithDetails>>
    fun getTrashedNotes(): Flow<List<NoteWithDetails>>
    fun getPinnedNotes(): Flow<List<NoteWithDetails>>
    fun getNotesInFolder(folderId: Long): Flow<List<NoteWithDetails>>
    fun getNotesWithTag(tagId: Long): Flow<List<NoteWithDetails>>
    fun getLockedNotes(): Flow<List<NoteWithDetails>>
    fun getHiddenNotes(): Flow<List<NoteWithDetails>>
    fun getNoteById(id: Long): Flow<NoteWithDetails?>
    suspend fun insertNote(note: NoteEntity): Long
    suspend fun updateNote(note: NoteEntity)
    suspend fun deleteNotePermanent(id: Long)
    suspend fun moveToTrash(id: Long)
    suspend fun restoreFromTrash(id: Long)
    suspend fun emptyTrash()

    // --- Search & Queries ---
    fun searchNotes(query: String): Flow<List<NoteWithDetails>>
    fun getSearchHistory(): Flow<List<SearchHistoryEntity>>
    suspend fun addSearchQuery(query: String)
    suspend fun clearSearchHistory()

    // --- Folders & Hierarchies ---
    fun getAllFolders(): Flow<List<FolderEntity>>
    fun getRootFolders(): Flow<List<FolderEntity>>
    fun getSubFolders(parentId: Long): Flow<List<FolderEntity>>
    suspend fun insertFolder(folder: FolderEntity): Long
    suspend fun updateFolder(folder: FolderEntity)
    suspend fun deleteFolder(id: Long)

    // --- Tags ---
    fun getAllTags(): Flow<List<TagEntity>>
    suspend fun insertTag(tag: TagEntity): Long
    suspend fun deleteTag(id: Long)
    suspend fun addTagToNote(noteId: Long, tagId: Long)
    suspend fun removeTagFromNote(noteId: Long, tagId: Long)

    // --- Attachments & Media ---
    fun getAttachmentsForNote(noteId: Long): Flow<List<AttachmentEntity>>
    suspend fun insertAttachment(attachment: AttachmentEntity): Long
    suspend fun deleteAttachment(id: Long)

    // --- Sticky Board ---
    fun getAllStickyNotes(): Flow<List<StickyNoteEntity>>
    suspend fun insertStickyNote(sticky: StickyNoteEntity): Long
    suspend fun updateStickyNote(sticky: StickyNoteEntity)
    suspend fun deleteStickyNote(id: Long)

    // --- Canvas Elements ---
    fun getCanvasElements(noteId: Long?): Flow<List<CanvasElementEntity>>
    suspend fun insertCanvasElement(element: CanvasElementEntity)
    suspend fun deleteCanvasElement(id: String)
    suspend fun clearCanvas(noteId: Long?)

    // --- Reminders ---
    fun getActiveReminders(): Flow<List<ReminderEntity>>
    suspend fun insertReminder(reminder: ReminderEntity): Long
    suspend fun updateReminder(reminder: ReminderEntity)
    suspend fun completeReminder(id: Long)
    suspend fun deleteReminder(id: Long)
}
```

---

## 3. Security Manager Interface: `SecurityManager`

Located at: `com.example.noteland.security.SecurityManager`

```kotlin
object SecurityManager {
    /**
     * Encrypts plaintext string using AES-256-GCM.
     * Returns a Triple of (CiphertextBase64, IvBase64, SaltBase64).
     */
    fun encrypt(plainText: String, pin: String): EncryptedPayload

    /**
     * Decrypts AES-256-GCM payload using user PIN.
     * Throws GeneralSecurityException if PIN is incorrect or payload was tampered.
     */
    fun decrypt(cipherTextBase64: String, ivBase64: String, saltBase64: String, pin: String): String

    /**
     * Verifies master PIN against stored PBKDF2 hash.
     */
    fun verifyPin(enteredPin: String, context: Context): Boolean

    /**
     * Registers a failed attempt and returns true if lockout threshold reached.
     */
    fun recordFailedAttempt(): LockoutStatus

    /**
     * Resets failed attempt counter upon successful authentication.
     */
    fun resetAttempts()
}
```

---

## 4. Offline Smart Utilities Engine: `SmartUtilities`

Located at: `com.example.noteland.util.SmartUtilities`

Deterministic, on-device text intelligence requiring zero remote API connectivity:

```kotlin
object SmartUtilities {
    /** Calculates exact word count, character count, and paragraph count. */
    fun analyzeTextMetrics(text: String): TextMetrics

    /** Estimates reading time in minutes based on 200 words-per-minute average. */
    fun calculateReadingTime(text: String): Int

    /** Generates an extractive 3-sentence summary using frequency-based sentence ranking. */
    fun extractSummary(text: String, sentenceCount: Int = 3): String

    /** Parses lines starting with '-' or '[]' into interactive checklist structures. */
    fun extractChecklist(text: String): List<ChecklistItem>

    /** Recommends a concise note title from the first non-empty heading or prominent sentence. */
    fun suggestTitle(text: String): String

    /** Detects semantic duplicate note candidates using Jaccard Similarity on n-grams. */
    fun findDuplicates(target: String, candidateNotes: List<NoteEntity>): List<DuplicateMatch>
}
```

---

## 5. Backup & Restore Engine: `BackupManager`

Located at: `com.example.noteland.data.backup.BackupManager`

```kotlin
class BackupManager(private val context: Context, private val repository: INoteRepository) {
    /**
     * Packages SQLite database records into JSON + attachments into a compressed ZIP file.
     * Returns the output File URI and SHA-256 manifest hash.
     */
    suspend fun exportBackup(targetUri: Uri): BackupExportResult

    /**
     * Validates checksums and structure of an external ZIP archive without modifying database.
     */
    suspend fun inspectBackup(sourceUri: Uri): BackupManifest

    /**
     * Restores data from validated ZIP archive using selected resolution strategy.
     * @param strategy MERGE (adds non-existent records) or REPLACE (clears existing DB first).
     */
    suspend fun restoreBackup(sourceUri: Uri, strategy: RestoreStrategy): RestoreResult
}
```
