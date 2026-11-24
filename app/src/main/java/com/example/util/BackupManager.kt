package com.example.util

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import com.example.data.model.AnnotationEntity
import com.example.data.model.AttachmentEntity
import com.example.data.model.BookmarkEntity
import com.example.data.model.CanvasElementEntity
import com.example.data.model.CanvasEntity
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
import com.example.data.repository.NoteRepository
import com.example.security.SecurityManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

enum class RestoreStrategy {
    MERGE,
    REPLACE
}

data class BackupPreview(
    val isValid: Boolean,
    val isEncrypted: Boolean,
    val schemaVersion: Int,
    val appVersion: String,
    val createdAtUtc: Long,
    val notesCount: Int,
    val foldersCount: Int,
    val tagsCount: Int,
    val attachmentsCount: Int,
    val errorMessage: String? = null
)

data class RestoreResult(
    val success: Boolean,
    val restoredNotesCount: Int,
    val restoredFoldersCount: Int,
    val restoredAttachmentsCount: Int,
    val message: String
)

class BackupManager(
    private val context: Context,
    private val repository: NoteRepository
) {
    private val securityManager = SecurityManager(context)

    companion object {
        const val CURRENT_SCHEMA_VERSION = 6
        const val APP_IDENTIFIER = "NoteLand"
        const val MANIFEST_ENTRY = "manifest.json"
        const val DATABASE_ENTRY = "database.json"
    }

    // ==========================================
    // BACKUP GENERATION (ZIP + JSON MANIFEST + CHECKSUMS)
    // ==========================================

    suspend fun createBackupZip(password: String? = null): File {
        val notes = repository.getAllNotesDirect()
        val folders = repository.getAllFoldersDirect()
        val tags = repository.getAllTagsDirect()
        val noteTags = repository.getAllNoteTagsDirect()
        val stickies = repository.getAllStickyNotesDirect()
        val canvases = repository.getAllCanvasesDirect()
        val canvasElements = repository.getAllCanvasElementsDirect()
        val attachments = repository.getAllAttachmentsDirect()
        val annotations = repository.getAllAnnotationsDirect()
        val bookmarks = repository.getAllBookmarksDirect()
        val reminders = repository.getAllRemindersDirect()
        val lockedIds = notes.filter { it.isLocked }.map { it.id }.toSet()
        val noteLinks = repository.getAllNoteLinksDirect()
            .filter { it.sourceNoteId !in lockedIds && (it.targetNoteId == null || it.targetNoteId !in lockedIds) }
        val noteTemplates = repository.getAllNoteTemplatesDirect()
        val noteVersions = repository.getAllNoteVersionsDirect()
            .filter { it.noteId !in lockedIds }
        val kanbanColumns = repository.getAllKanbanColumnsDirect()
        val tasks = repository.getAllTasksDirect()
        val taskDependencies = repository.getAllTaskDependenciesDirect()
        val habits = repository.getAllHabitsDirect()
        val habitLogs = repository.getAllHabitLogsDirect()
        val focusSessions = repository.getAllFocusSessionsDirect()
        val settingsPrefs = context.getSharedPreferences("noteland_settings_prefs", Context.MODE_PRIVATE)
        val stickyBoardLayoutsJson = settingsPrefs.getString("sticky_board_layouts", "") ?: ""
        val dashboardWidgetsJson = settingsPrefs.getString("dashboard_widgets_config", "") ?: ""

        // 1. Build database JSON dump
        val dbJson = buildDatabaseJson(
            notes = notes,
            folders = folders,
            tags = tags,
            noteTags = noteTags,
            stickies = stickies,
            canvases = canvases,
            canvasElements = canvasElements,
            attachments = attachments,
            annotations = annotations,
            bookmarks = bookmarks,
            reminders = reminders,
            noteLinks = noteLinks,
            noteTemplates = noteTemplates,
            noteVersions = noteVersions,
            kanbanColumns = kanbanColumns,
            tasks = tasks,
            taskDependencies = taskDependencies,
            habits = habits,
            habitLogs = habitLogs,
            focusSessions = focusSessions,
            stickyBoardLayoutsJson = stickyBoardLayoutsJson,
            dashboardWidgetsJson = dashboardWidgetsJson
        )
        val rawDbBytes = dbJson.toByteArray(Charsets.UTF_8)

        // Optional encryption
        val pwd = password?.takeIf { it.isNotBlank() }
        val isEncrypted = pwd != null
        val finalDbBytes: ByteArray
        var dbSalt: ByteArray? = null
        var kdfSaltB64 = ""

        if (pwd != null) {
            val (encryptedPayload, salt) = securityManager.encryptWithPassword(rawDbBytes, pwd)
            finalDbBytes = encryptedPayload
            dbSalt = salt
            kdfSaltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        } else {
            finalDbBytes = rawDbBytes
        }

        // Checksums map
        val checksumsMap = mutableMapOf<String, String>()
        checksumsMap[DATABASE_ENTRY] = calculateSha256(finalDbBytes)

        // Read physical attachment files, voice recording audio clips, and note backgrounds
        val attachmentFiles = mutableMapOf<String, ByteArray>()
        for (att in attachments) {
            val file = if (att.relativePath.startsWith("/")) {
                File(att.relativePath)
            } else {
                File(context.filesDir, att.relativePath)
            }
            if (file.exists() && file.isFile) {
                val rawBytes = file.readBytes()
                val uniqueBaseName = File(att.relativePath).name.ifBlank { "${att.id}_${att.fileName}" }
                var entryPath = "attachments/$uniqueBaseName"
                if (attachmentFiles.containsKey(entryPath)) {
                    entryPath = "attachments/${att.id}_$uniqueBaseName"
                }
                val storedBytes = if (pwd != null && dbSalt != null) {
                    securityManager.encryptWithPassword(rawBytes, pwd, dbSalt)
                } else {
                    rawBytes
                }
                attachmentFiles[entryPath] = storedBytes
                checksumsMap[entryPath] = calculateSha256(storedBytes)
            }
        }

        val bundledVoiceNames = mutableSetOf<String>()
        val bundledBgNames = mutableSetOf<String>()
        for (note in notes) {
            val clips = SmartNoteUtils.parseVoiceRecordings(note.canvasDataJson)
            for (clip in clips) {
                val voiceFile = if (clip.filePath.startsWith("/")) {
                    File(clip.filePath)
                } else {
                    File(context.filesDir, clip.filePath)
                }
                if (voiceFile.exists() && voiceFile.isFile && bundledVoiceNames.add(voiceFile.name)) {
                    val rawBytes = voiceFile.readBytes()
                    val entryPath = "voice_recordings/${voiceFile.name}"
                    val storedBytes = if (pwd != null && dbSalt != null) {
                        securityManager.encryptWithPassword(rawBytes, pwd, dbSalt)
                    } else {
                        rawBytes
                    }
                    attachmentFiles[entryPath] = storedBytes
                    checksumsMap[entryPath] = calculateSha256(storedBytes)
                }
            }
            val bgPath = SmartNoteUtils.parseNoteBackgroundImage(note.canvasDataJson)
            if (bgPath.isNotBlank()) {
                val bgFile = if (bgPath.startsWith("/")) {
                    File(bgPath)
                } else {
                    File(context.filesDir, bgPath)
                }
                if (bgFile.exists() && bgFile.isFile && bundledBgNames.add(bgFile.name)) {
                    val rawBytes = bgFile.readBytes()
                    val entryPath = "note_backgrounds/${bgFile.name}"
                    val storedBytes = if (pwd != null && dbSalt != null) {
                        securityManager.encryptWithPassword(rawBytes, pwd, dbSalt)
                    } else {
                        rawBytes
                    }
                    attachmentFiles[entryPath] = storedBytes
                    checksumsMap[entryPath] = calculateSha256(storedBytes)
                }
            }
        }

        // 2. Build Manifest JSON
        val manifestObj = JSONObject().apply {
            put("app", APP_IDENTIFIER)
            put("schemaVersion", CURRENT_SCHEMA_VERSION)
            put("appVersion", com.example.BuildConfig.VERSION_NAME)
            put("createdAtUtc", System.currentTimeMillis())
            put("isEncrypted", isEncrypted)
            put("kdfSalt", kdfSaltB64)
            put("notesCount", notes.size)
            put("foldersCount", folders.size)
            put("tagsCount", tags.size)
            put("attachmentsCount", attachmentFiles.size)

            val checksumsJson = JSONObject()
            checksumsMap.forEach { (path, hash) -> checksumsJson.put(path, hash) }
            put("checksums", checksumsJson)
        }

        // 3. Write ZIP archive
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()
        val zipFile = File(backupDir, "noteland_backup_$timeStamp.zip")

        FileOutputStream(zipFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                // Write manifest.json
                zos.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                zos.write(manifestObj.toString(2).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Write database.json
                zos.putNextEntry(ZipEntry(DATABASE_ENTRY))
                zos.write(finalDbBytes)
                zos.closeEntry()

                // Write attachments
                for ((entryPath, bytes) in attachmentFiles) {
                    zos.putNextEntry(ZipEntry(entryPath))
                    zos.write(bytes)
                    zos.closeEntry()
                }
            }
        }

        return zipFile
    }

    fun listLocalBackups(): List<File> {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) return emptyList()
        return backupDir.listFiles { _, name -> name.endsWith(".zip") || name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    // ==========================================
    // VALIDATION & PREVIEW
    // ==========================================

    fun validateAndPreview(zipFile: File, password: String? = null): Result<BackupPreview> {
        if (!zipFile.exists() || zipFile.length() == 0L) {
            return Result.failure(IllegalArgumentException("Backup file does not exist or is empty."))
        }

        return try {
            val entries = readZipEntries(zipFile)
            val manifestBytes = entries[MANIFEST_ENTRY]
                ?: return Result.failure(IllegalArgumentException("Malformed backup: Missing $MANIFEST_ENTRY"))

            val manifest = JSONObject(String(manifestBytes, Charsets.UTF_8))
            val app = manifest.optString("app", "")
            if (app != APP_IDENTIFIER) {
                return Result.failure(IllegalArgumentException("Unrecognized backup source: $app"))
            }

            val schemaVersion = manifest.optInt("schemaVersion", 1)
            if (schemaVersion > CURRENT_SCHEMA_VERSION) {
                return Result.failure(IllegalArgumentException("Unsupported backup schema version $schemaVersion (current supported version is $CURRENT_SCHEMA_VERSION)"))
            }
            val isEncrypted = manifest.optBoolean("isEncrypted", false)
            val kdfSaltB64 = manifest.optString("kdfSalt", "")

            // Validate checksums
            val checksumsJson = manifest.optJSONObject("checksums")
            if (checksumsJson != null) {
                for (key in checksumsJson.keys()) {
                    val expectedHash = checksumsJson.getString(key)
                    val entryBytes = entries[key]
                        ?: return Result.failure(IllegalArgumentException("Malformed backup: Missing entry '$key' specified in manifest."))
                    val actualHash = calculateSha256(entryBytes)
                    if (actualHash != expectedHash) {
                        return Result.failure(SecurityException("Tampered backup: Checksum mismatch for entry '$key'."))
                    }
                }
            }

            if (isEncrypted) {
                if (password.isNullOrBlank()) {
                    return Result.success(
                        BackupPreview(
                            isValid = true,
                            isEncrypted = true,
                            schemaVersion = schemaVersion,
                            appVersion = manifest.optString("appVersion", "1.0.0"),
                            createdAtUtc = manifest.optLong("createdAtUtc", System.currentTimeMillis()),
                            notesCount = manifest.optInt("notesCount", 0),
                            foldersCount = manifest.optInt("foldersCount", 0),
                            tagsCount = manifest.optInt("tagsCount", 0),
                            attachmentsCount = manifest.optInt("attachmentsCount", 0),
                            errorMessage = "Password required to decrypt this backup."
                        )
                    )
                }
                // Attempt password decryption check
                try {
                    val salt = Base64.decode(kdfSaltB64, Base64.NO_WRAP)
                    val dbEncBytes = entries[DATABASE_ENTRY] ?: throw IllegalArgumentException("Missing database.json")
                    securityManager.decryptWithPassword(dbEncBytes, salt, password)
                } catch (e: Exception) {
                    return Result.failure(SecurityException("Incorrect backup password or corrupted ciphertext."))
                }
            }

            Result.success(
                BackupPreview(
                    isValid = true,
                    isEncrypted = isEncrypted,
                    schemaVersion = schemaVersion,
                    appVersion = manifest.optString("appVersion", "1.0.0"),
                    createdAtUtc = manifest.optLong("createdAtUtc", System.currentTimeMillis()),
                    notesCount = manifest.optInt("notesCount", 0),
                    foldersCount = manifest.optInt("foldersCount", 0),
                    tagsCount = manifest.optInt("tagsCount", 0),
                    attachmentsCount = manifest.optInt("attachmentsCount", 0)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // SAFE RESTORE (MERGE / REPLACE)
    // ==========================================

    suspend fun restoreFromZip(
        zipFile: File,
        strategy: RestoreStrategy,
        password: String? = null
    ): Result<RestoreResult> {
        val previewResult = validateAndPreview(zipFile, password)
        if (previewResult.isFailure) {
            return Result.failure(previewResult.exceptionOrNull() ?: Exception("Backup validation failed"))
        }

        val preview = previewResult.getOrThrow()
        val pwd = password?.takeIf { it.isNotBlank() }
        if (preview.isEncrypted && pwd == null) {
            return Result.failure(SecurityException("Encrypted backup requires a password."))
        }

        return try {
            val entries = readZipEntries(zipFile)
            val manifestBytes = entries[MANIFEST_ENTRY]
                ?: return Result.failure(IllegalArgumentException("Missing $MANIFEST_ENTRY in archive."))
            val manifest = JSONObject(String(manifestBytes, Charsets.UTF_8))
            val isEncrypted = manifest.optBoolean("isEncrypted", false)
            if (isEncrypted && pwd == null) {
                return Result.failure(SecurityException("Encrypted backup requires a password."))
            }
            val kdfSaltB64 = manifest.optString("kdfSalt", "")
            val salt = if (isEncrypted) Base64.decode(kdfSaltB64, Base64.NO_WRAP) else ByteArray(0)

            val rawDbBytes = entries[DATABASE_ENTRY]
                ?: return Result.failure(IllegalArgumentException("Missing database.json in archive."))

            val decryptedDbBytes = if (pwd != null && isEncrypted) {
                securityManager.decryptWithPassword(rawDbBytes, salt, pwd)
            } else {
                rawDbBytes
            }

            val dbJsonStr = String(decryptedDbBytes, Charsets.UTF_8)
            val root = JSONObject(dbJsonStr)
            val stats = restoreDatabasePayload(root, strategy)

            // Extract physical attachment files safely inside context.filesDir (prevent ZipSlip / traversal)
            val filesRootCanonical = context.filesDir.canonicalPath
            val attachmentsDir = File(context.filesDir, "attachments").apply { if (!exists()) mkdirs() }
            val voiceDir = File(context.filesDir, "voice_recordings").apply { if (!exists()) mkdirs() }
            val voiceDirCanonical = voiceDir.canonicalPath
            val bgDir = File(context.filesDir, "note_backgrounds").apply { if (!exists()) mkdirs() }
            val bgDirCanonical = bgDir.canonicalPath

            var restoredFilesCount = 0
            for (att in stats.restoredAttachments) {
                val uniqueBaseName = File(att.relativePath).name.ifBlank { "${att.id}_${att.fileName}" }
                val fileBytes = entries["attachments/${att.id}_$uniqueBaseName"]
                    ?: entries["attachments/$uniqueBaseName"]
                    ?: entries["attachments/${att.fileName}"]
                if (fileBytes != null) {
                    val decryptedBytes = if (pwd != null && isEncrypted) {
                        securityManager.decryptWithPassword(fileBytes, salt, pwd)
                    } else {
                        fileBytes
                    }
                    val candidateFile = if (att.relativePath.startsWith("/")) {
                        File(att.relativePath)
                    } else {
                        File(context.filesDir, att.relativePath)
                    }
                    val candidateCanonical = candidateFile.canonicalPath
                    val targetFile = if (
                        candidateCanonical.startsWith(filesRootCanonical + File.separator)
                    ) {
                        candidateFile
                    } else {
                        val safeBaseName = uniqueBaseName.replace(Regex("[/\\\\]"), "_")
                        File(attachmentsDir, safeBaseName)
                    }
                    targetFile.parentFile?.mkdirs()
                    targetFile.writeBytes(decryptedBytes)
                    restoredFilesCount++
                }
            }

            // Extract bundled voice recording audio files into context.filesDir/voice_recordings/
            for ((entryKey, rawBytes) in entries) {
                if (entryKey.startsWith("voice_recordings/") && entryKey.length > "voice_recordings/".length) {
                    val safeName = File(entryKey).name
                    if (safeName.isNotBlank() && safeName != "." && safeName != "..") {
                        val targetFile = File(voiceDir, safeName)
                        if (targetFile.canonicalPath.startsWith(voiceDirCanonical + File.separator)) {
                            val decryptedBytes = if (pwd != null && isEncrypted) {
                                securityManager.decryptWithPassword(rawBytes, salt, pwd)
                            } else {
                                rawBytes
                            }
                            targetFile.parentFile?.mkdirs()
                            targetFile.writeBytes(decryptedBytes)
                            restoredFilesCount++
                        }
                    }
                } else if (entryKey.startsWith("note_backgrounds/") && entryKey.length > "note_backgrounds/".length) {
                    val safeName = File(entryKey).name
                    if (safeName.isNotBlank() && safeName != "." && safeName != "..") {
                        val targetFile = File(bgDir, safeName)
                        if (targetFile.canonicalPath.startsWith(bgDirCanonical + File.separator)) {
                            val decryptedBytes = if (pwd != null && isEncrypted) {
                                securityManager.decryptWithPassword(rawBytes, salt, pwd)
                            } else {
                                rawBytes
                            }
                            targetFile.parentFile?.mkdirs()
                            targetFile.writeBytes(decryptedBytes)
                            restoredFilesCount++
                        }
                    }
                }
            }

            Result.success(
                RestoreResult(
                    success = true,
                    restoredNotesCount = stats.restoredNotesCount,
                    restoredFoldersCount = stats.restoredFoldersCount,
                    restoredAttachmentsCount = restoredFilesCount,
                    message = "Successfully restored ${stats.restoredNotesCount} notes using $strategy strategy."
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private data class PayloadRestoreStats(
        val restoredNotesCount: Int,
        val restoredFoldersCount: Int,
        val restoredAttachments: List<AttachmentEntity>
    )

    private suspend fun restoreDatabasePayload(
        root: JSONObject,
        strategy: RestoreStrategy
    ): PayloadRestoreStats {
        val now = System.currentTimeMillis()

        // Parse Notes
        val notesArray = root.optJSONArray("notes") ?: JSONArray()
        val incomingNotes = mutableListOf<NoteEntity>()
        for (i in 0 until notesArray.length()) {
            val nObj = notesArray.getJSONObject(i)
            val rawFolderId = nObj.optString("folderId", "")
            val reminderVal = nObj.optLong("reminderTimestamp", -1L)
            val deletedAtVal = nObj.optLong("deletedAtUtc", -1L)
            incomingNotes.add(
                NoteEntity(
                    id = nObj.optString("id", UUID.randomUUID().toString()),
                    folderId = if (rawFolderId.isNotBlank() && rawFolderId != "-1" && rawFolderId != "null") rawFolderId else null,
                    folderName = nObj.optString("folderName", ""),
                    title = nObj.optString("title", "Untitled"),
                    content = nObj.optString("content", ""),
                    noteType = nObj.optString("noteType", "STANDARD"),
                    colorHex = nObj.optString("colorHex", ""),
                    tagsCsv = nObj.optString("tagsCsv", ""),
                    isPinned = nObj.optBoolean("isPinned", false),
                    isArchived = nObj.optBoolean("isArchived", false),
                    isDeleted = nObj.optBoolean("isDeleted", false),
                    isLocked = nObj.optBoolean("isLocked", false),
                    isHidden = nObj.optBoolean("isHidden", false),
                    isFavorite = nObj.optBoolean("isFavorite", false),
                    reminderTimestamp = if (reminderVal > 0) reminderVal else null,
                    canvasDataJson = nObj.optString("canvasDataJson", ""),
                    checklistJson = nObj.optString("checklistJson", ""),
                    imageUri = nObj.optString("imageUri", ""),
                    createdAtUtc = nObj.optLong("createdAtUtc", now),
                    updatedAtUtc = nObj.optLong("updatedAtUtc", now),
                    deletedAtUtc = if (deletedAtVal > 0) deletedAtVal else null
                )
            )
        }

        // Parse Folders
        val foldersArray = root.optJSONArray("folders") ?: JSONArray()
        val incomingFolders = mutableListOf<FolderEntity>()
        for (i in 0 until foldersArray.length()) {
            val fObj = foldersArray.getJSONObject(i)
            val rawParentId = fObj.optString("parentId", "")
            val created = fObj.optLong("createdAtUtc", now)
            incomingFolders.add(
                FolderEntity(
                    id = fObj.optString("id", UUID.randomUUID().toString()),
                    parentId = rawParentId.takeIf { it.isNotBlank() && it != "null" },
                    name = fObj.optString("name", "Folder"),
                    colorHex = fObj.optString("colorHex", "#3B82F6"),
                    iconName = fObj.optString("iconName", "folder"),
                    createdAtUtc = created,
                    updatedAtUtc = fObj.optLong("updatedAtUtc", created)
                )
            )
        }

        // Parse Tags
        val tagsArray = root.optJSONArray("tags") ?: JSONArray()
        val incomingTags = mutableListOf<TagEntity>()
        for (i in 0 until tagsArray.length()) {
            val tObj = tagsArray.getJSONObject(i)
            incomingTags.add(
                TagEntity(
                    id = tObj.optString("id", UUID.randomUUID().toString()),
                    name = tObj.optString("name", "Tag"),
                    colorHex = tObj.optString("colorHex", "#6366F1"),
                    createdAtUtc = tObj.optLong("createdAtUtc", now)
                )
            )
        }

        // Parse NoteTags
        val noteTagsArray = root.optJSONArray("noteTags") ?: JSONArray()
        val incomingNoteTags = mutableListOf<NoteTagEntity>()
        for (i in 0 until noteTagsArray.length()) {
            val ntObj = noteTagsArray.getJSONObject(i)
            val nId = ntObj.optString("noteId", "")
            val tId = ntObj.optString("tagId", "")
            if (nId.isNotBlank() && tId.isNotBlank()) {
                incomingNoteTags.add(
                    NoteTagEntity(
                        noteId = nId,
                        tagId = tId,
                        assignedAtUtc = ntObj.optLong("assignedAtUtc", now)
                    )
                )
            }
        }

        // Parse Stickies
        val stickiesArray = root.optJSONArray("stickyNotes") ?: JSONArray()
        val incomingStickies = mutableListOf<StickyNoteEntity>()
        for (i in 0 until stickiesArray.length()) {
            val sObj = stickiesArray.getJSONObject(i)
            val rawNoteId = sObj.optString("noteId", "")
            val created = sObj.optLong("createdAtUtc", now)
            incomingStickies.add(
                StickyNoteEntity(
                    id = sObj.optString("id", UUID.randomUUID().toString()),
                    noteId = rawNoteId.takeIf { it.isNotBlank() && it != "null" },
                    title = sObj.optString("title", "Sticky"),
                    content = sObj.optString("content", ""),
                    colorHex = sObj.optString("colorHex", "#FEF08A"),
                    orderIndex = sObj.optInt("orderIndex", 0),
                    isPinned = sObj.optBoolean("isPinned", false),
                    createdAtUtc = created,
                    updatedAtUtc = sObj.optLong("updatedAtUtc", created)
                )
            )
        }

        // Parse Canvases
        val canvasesArray = root.optJSONArray("canvases") ?: JSONArray()
        val incomingCanvases = mutableListOf<CanvasEntity>()
        for (i in 0 until canvasesArray.length()) {
            val cObj = canvasesArray.getJSONObject(i)
            val created = cObj.optLong("createdAtUtc", now)
            incomingCanvases.add(
                CanvasEntity(
                    id = cObj.optString("id", UUID.randomUUID().toString()),
                    title = cObj.optString("title", "Canvas"),
                    backgroundColorHex = cObj.optString("backgroundColorHex", "#FAF5FF"),
                    width = cObj.optDouble("width", 1920.0).toFloat(),
                    height = cObj.optDouble("height", 1080.0).toFloat(),
                    createdAtUtc = created,
                    updatedAtUtc = cObj.optLong("updatedAtUtc", created)
                )
            )
        }

        // Parse Elements
        val elementsArray = root.optJSONArray("canvasElements") ?: JSONArray()
        val incomingElements = mutableListOf<CanvasElementEntity>()
        for (i in 0 until elementsArray.length()) {
            val eObj = elementsArray.getJSONObject(i)
            incomingElements.add(
                CanvasElementEntity(
                    id = eObj.optString("id", UUID.randomUUID().toString()),
                    canvasId = eObj.optString("canvasId", ""),
                    elementType = eObj.optString("elementType", "STROKE"),
                    elementDataJson = eObj.optString("elementDataJson", "[]"),
                    zIndex = eObj.optInt("zIndex", 0),
                    createdAtUtc = eObj.optLong("createdAtUtc", now)
                )
            )
        }

        // Parse Attachments
        val attArray = root.optJSONArray("attachments") ?: JSONArray()
        val incomingAttachments = mutableListOf<AttachmentEntity>()
        for (i in 0 until attArray.length()) {
            val aObj = attArray.getJSONObject(i)
            val created = aObj.optLong("createdAtUtc", now)
            val deletedAtRaw = aObj.optLong("deletedAtUtc", -1L)
            incomingAttachments.add(
                AttachmentEntity(
                    id = aObj.optString("id", UUID.randomUUID().toString()),
                    noteId = aObj.optString("noteId").takeIf { it.isNotBlank() && it != "null" },
                    canvasId = aObj.optString("canvasId").takeIf { it.isNotBlank() && it != "null" },
                    fileName = aObj.optString("fileName", "file"),
                    fileExtension = aObj.optString("fileExtension", ""),
                    relativePath = aObj.optString("relativePath", ""),
                    mimeType = aObj.optString("mimeType", "application/octet-stream"),
                    sizeBytes = aObj.optLong("sizeBytes", 0L),
                    createdAtUtc = created,
                    folderId = aObj.optString("folderId").takeIf { it.isNotBlank() && it != "null" },
                    contentHash = aObj.optString("contentHash", ""),
                    pageCount = aObj.optInt("pageCount", 0),
                    isFavorite = aObj.optBoolean("isFavorite", false),
                    isArchived = aObj.optBoolean("isArchived", false),
                    isLocked = aObj.optBoolean("isLocked", false),
                    isHidden = aObj.optBoolean("isHidden", false),
                    isDeleted = aObj.optBoolean("isDeleted", false),
                    deletedAtUtc = if (deletedAtRaw > 0L) deletedAtRaw else null,
                    updatedAtUtc = aObj.optLong("updatedAtUtc", created)
                )
            )
        }

        // Parse Annotations
        val annotArray = root.optJSONArray("annotations") ?: JSONArray()
        val incomingAnnotations = mutableListOf<AnnotationEntity>()
        for (i in 0 until annotArray.length()) {
            val anObj = annotArray.getJSONObject(i)
            incomingAnnotations.add(
                AnnotationEntity(
                    id = anObj.optString("id", UUID.randomUUID().toString()),
                    attachmentId = anObj.optString("attachmentId", ""),
                    annotationType = anObj.optString("annotationType", "FREEHAND"),
                    pageNumber = anObj.optInt("pageNumber", 1),
                    coordinatesJson = anObj.optString("coordinatesJson", "[]"),
                    content = anObj.optString("content", ""),
                    colorHex = anObj.optString("colorHex", "#EF4444"),
                    createdAtUtc = anObj.optLong("createdAtUtc", now)
                )
            )
        }

        // Parse Reminders
        val remArray = root.optJSONArray("reminders") ?: JSONArray()
        val incomingReminders = mutableListOf<ReminderEntity>()
        for (i in 0 until remArray.length()) {
            val rObj = remArray.getJSONObject(i)
            incomingReminders.add(
                ReminderEntity(
                    id = rObj.optString("id", UUID.randomUUID().toString()),
                    noteId = rObj.optString("noteId", ""),
                    scheduledTimeUtc = rObj.optLong("scheduledTimeUtc", now),
                    repeatInterval = rObj.optString("repeatInterval").takeIf { it.isNotBlank() && it != "null" },
                    isCompleted = rObj.optBoolean("isCompleted", false),
                    createdAtUtc = rObj.optLong("createdAtUtc", now)
                )
            )
        }

        // Parse Bookmarks
        val bookmarksArray = root.optJSONArray("bookmarks") ?: JSONArray()
        val incomingBookmarks = mutableListOf<BookmarkEntity>()
        for (i in 0 until bookmarksArray.length()) {
            val bObj = bookmarksArray.getJSONObject(i)
            val targetId = bObj.optString("targetId", "")
            if (targetId.isNotBlank()) {
                incomingBookmarks.add(
                    BookmarkEntity(
                        id = bObj.optString("id", UUID.randomUUID().toString()),
                        targetType = bObj.optString("targetType", "NOTE"),
                        targetId = targetId,
                        label = bObj.optString("label", "Bookmark"),
                        createdAtUtc = bObj.optLong("createdAtUtc", now)
                    )
                )
            }
        }

        // Parse NoteLinks (Phase 1)
        val linksArray = root.optJSONArray("noteLinks") ?: JSONArray()
        val incomingNoteLinks = mutableListOf<NoteLinkEntity>()
        for (i in 0 until linksArray.length()) {
            val lObj = linksArray.getJSONObject(i)
            val srcId = lObj.optString("sourceNoteId", "")
            val tgtTitle = lObj.optString("targetTitle", "")
            if (srcId.isNotBlank() && tgtTitle.isNotBlank()) {
                val rawTgtId = lObj.optString("targetNoteId", "")
                incomingNoteLinks.add(
                    NoteLinkEntity(
                        id = lObj.optString("id", UUID.randomUUID().toString()),
                        sourceNoteId = srcId,
                        targetNoteId = rawTgtId.takeIf { it.isNotBlank() && it != "null" },
                        targetTitle = tgtTitle,
                        normalizedTargetTitle = lObj.optString(
                            "normalizedTargetTitle",
                            KnowledgeBrainEngine.normalizeTitleKey(tgtTitle)
                        ),
                        anchorHeading = lObj.optString("anchorHeading", ""),
                        displayAlias = lObj.optString("displayAlias", ""),
                        contextSnippet = lObj.optString("contextSnippet", ""),
                        createdAtUtc = lObj.optLong("createdAtUtc", now),
                        updatedAtUtc = lObj.optLong("updatedAtUtc", now)
                    )
                )
            }
        }

        // Parse NoteTemplates (Phase 1)
        val templatesArray = root.optJSONArray("noteTemplates") ?: JSONArray()
        val incomingNoteTemplates = mutableListOf<NoteTemplateEntity>()
        for (i in 0 until templatesArray.length()) {
            val tplObj = templatesArray.getJSONObject(i)
            val name = tplObj.optString("name", "")
            if (name.isNotBlank()) {
                val created = tplObj.optLong("createdAtUtc", now)
                incomingNoteTemplates.add(
                    NoteTemplateEntity(
                        id = tplObj.optString("id", UUID.randomUUID().toString()),
                        name = name,
                        description = tplObj.optString("description", ""),
                        category = tplObj.optString("category", "Custom"),
                        titlePattern = tplObj.optString("titlePattern", "{{title}}"),
                        contentTemplate = tplObj.optString("contentTemplate", ""),
                        defaultTagsCsv = tplObj.optString("defaultTagsCsv", ""),
                        defaultColorHex = tplObj.optString("defaultColorHex", ""),
                        checklistTemplateJson = tplObj.optString("checklistTemplateJson", ""),
                        isBuiltIn = tplObj.optBoolean("isBuiltIn", false),
                        usageCount = tplObj.optInt("usageCount", 0),
                        createdAtUtc = created,
                        updatedAtUtc = tplObj.optLong("updatedAtUtc", created)
                    )
                )
            }
        }

        // Parse NoteVersions (Phase 1)
        val versionsArray = root.optJSONArray("noteVersions") ?: JSONArray()
        val incomingNoteVersions = mutableListOf<NoteVersionEntity>()
        for (i in 0 until versionsArray.length()) {
            val vObj = versionsArray.getJSONObject(i)
            val nId = vObj.optString("noteId", "")
            if (nId.isNotBlank()) {
                incomingNoteVersions.add(
                    NoteVersionEntity(
                        id = vObj.optString("id", UUID.randomUUID().toString()),
                        noteId = nId,
                        versionNumber = vObj.optInt("versionNumber", 1),
                        title = vObj.optString("title", ""),
                        content = vObj.optString("content", ""),
                        tagsCsv = vObj.optString("tagsCsv", ""),
                        checklistJson = vObj.optString("checklistJson", ""),
                        changeSummary = vObj.optString("changeSummary", ""),
                        wordCount = vObj.optInt("wordCount", 0),
                        createdAtUtc = vObj.optLong("createdAtUtc", now)
                    )
                )
            }
        }

        // Parse Phase 2: Kanban Columns, Tasks, Dependencies, Habits, HabitLogs, FocusSessions
        val kanbanColsArray = root.optJSONArray("kanbanColumns") ?: JSONArray()
        val incomingKanbanColumns = mutableListOf<KanbanColumnEntity>()
        for (i in 0 until kanbanColsArray.length()) {
            val cObj = kanbanColsArray.getJSONObject(i)
            val created = cObj.optLong("createdAtUtc", now)
            incomingKanbanColumns.add(
                KanbanColumnEntity(
                    id = cObj.optString("id", UUID.randomUUID().toString()),
                    boardId = cObj.optString("boardId", "default"),
                    title = cObj.optString("title", "Column"),
                    colorHex = cObj.optString("colorHex", "#6366F1"),
                    position = cObj.optInt("position", i),
                    wipLimit = cObj.optInt("wipLimit", 0),
                    isDoneColumn = cObj.optBoolean("isDoneColumn", false),
                    createdAtUtc = created,
                    updatedAtUtc = cObj.optLong("updatedAtUtc", created)
                )
            )
        }

        val tasksArray = root.optJSONArray("tasks") ?: JSONArray()
        val incomingTasks = mutableListOf<TaskEntity>()
        for (i in 0 until tasksArray.length()) {
            val tObj = tasksArray.getJSONObject(i)
            val compAt = tObj.optLong("completedAtUtc", -1L)
            val dueAt = tObj.optLong("dueAtUtc", -1L)
            val schedStart = tObj.optLong("scheduledStartUtc", -1L)
            val schedEnd = tObj.optLong("scheduledEndUtc", -1L)
            val rawLinkedNote = tObj.optString("linkedNoteId", "")
            val rawLinkedRem = tObj.optString("linkedReminderId", "")
            val rawRecParent = tObj.optString("recurrenceParentId", "")
            incomingTasks.add(
                TaskEntity(
                    id = tObj.optString("id", UUID.randomUUID().toString()),
                    title = tObj.optString("title", "Task"),
                    description = tObj.optString("description", ""),
                    columnId = tObj.optString("columnId", "col_todo"),
                    orderInColumn = tObj.optInt("orderInColumn", i),
                    priority = tObj.optString("priority", "MEDIUM"),
                    isCompleted = tObj.optBoolean("isCompleted", false),
                    completedAtUtc = if (compAt > 0L) compAt else null,
                    dueAtUtc = if (dueAt > 0L) dueAt else null,
                    dueTimeZoneId = tObj.optString("dueTimeZoneId", java.util.TimeZone.getDefault().id),
                    scheduledStartUtc = if (schedStart > 0L) schedStart else null,
                    scheduledEndUtc = if (schedEnd > 0L) schedEnd else null,
                    estimatedMinutes = tObj.optInt("estimatedMinutes", 30),
                    recurrenceRule = tObj.optString("recurrenceRule", "NONE"),
                    recurrenceInterval = tObj.optInt("recurrenceInterval", 1),
                    recurrenceDaysCsv = tObj.optString("recurrenceDaysCsv", ""),
                    recurrenceParentId = rawRecParent.takeIf { it.isNotBlank() && it != "null" },
                    subtasksJson = tObj.optString("subtasksJson", ""),
                    linkedNoteId = rawLinkedNote.takeIf { it.isNotBlank() && it != "null" },
                    linkedReminderId = rawLinkedRem.takeIf { it.isNotBlank() && it != "null" },
                    category = tObj.optString("category", "Work"),
                    colorHex = tObj.optString("colorHex", "#3B82F6"),
                    tagsCsv = tObj.optString("tagsCsv", ""),
                    isArchived = tObj.optBoolean("isArchived", false),
                    createdAtUtc = tObj.optLong("createdAtUtc", now),
                    updatedAtUtc = tObj.optLong("updatedAtUtc", now)
                )
            )
        }

        val depsArray = root.optJSONArray("taskDependencies") ?: JSONArray()
        val incomingTaskDeps = mutableListOf<TaskDependencyEntity>()
        for (i in 0 until depsArray.length()) {
            val dObj = depsArray.getJSONObject(i)
            val tId = dObj.optString("taskId", "")
            val depId = dObj.optString("dependsOnTaskId", "")
            if (tId.isNotBlank() && depId.isNotBlank() && tId != depId) {
                incomingTaskDeps.add(
                    TaskDependencyEntity(
                        taskId = tId,
                        dependsOnTaskId = depId,
                        createdAtUtc = dObj.optLong("createdAtUtc", now)
                    )
                )
            }
        }

        val habitsArray = root.optJSONArray("habits") ?: JSONArray()
        val incomingHabits = mutableListOf<HabitEntity>()
        for (i in 0 until habitsArray.length()) {
            val hObj = habitsArray.getJSONObject(i)
            val remHour = hObj.optInt("reminderHour", -1)
            val remMin = hObj.optInt("reminderMinute", -1)
            val created = hObj.optLong("createdAtUtc", now)
            incomingHabits.add(
                HabitEntity(
                    id = hObj.optString("id", UUID.randomUUID().toString()),
                    title = hObj.optString("title", "Habit"),
                    description = hObj.optString("description", ""),
                    category = hObj.optString("category", "Health"),
                    colorHex = hObj.optString("colorHex", "#10B981"),
                    iconName = hObj.optString("iconName", "check_circle"),
                    frequencyType = hObj.optString("frequencyType", "DAILY"),
                    targetPerDay = hObj.optInt("targetPerDay", 1),
                    targetDaysPerWeek = hObj.optInt("targetDaysPerWeek", 7),
                    reminderHour = if (remHour in 0..23) remHour else null,
                    reminderMinute = if (remMin in 0..59) remMin else null,
                    timeZoneId = hObj.optString("timeZoneId", java.util.TimeZone.getDefault().id),
                    isArchived = hObj.optBoolean("isArchived", false),
                    createdAtUtc = created,
                    updatedAtUtc = hObj.optLong("updatedAtUtc", created)
                )
            )
        }

        val habitLogsArray = root.optJSONArray("habitLogs") ?: JSONArray()
        val incomingHabitLogs = mutableListOf<HabitLogEntity>()
        for (i in 0 until habitLogsArray.length()) {
            val hlObj = habitLogsArray.getJSONObject(i)
            val hId = hlObj.optString("habitId", "")
            val dKey = hlObj.optString("localDateKey", "")
            if (hId.isNotBlank() && dKey.isNotBlank()) {
                incomingHabitLogs.add(
                    HabitLogEntity(
                        id = hlObj.optString("id", UUID.randomUUID().toString()),
                        habitId = hId,
                        localDateKey = dKey,
                        completedCount = hlObj.optInt("completedCount", 1),
                        targetCountSnapshot = hlObj.optInt("targetCountSnapshot", 1),
                        isCompleted = hlObj.optBoolean("isCompleted", true),
                        note = hlObj.optString("note", ""),
                        timeZoneId = hlObj.optString("timeZoneId", java.util.TimeZone.getDefault().id),
                        completedAtUtc = hlObj.optLong("completedAtUtc", now)
                    )
                )
            }
        }

        val focusArray = root.optJSONArray("focusSessions") ?: JSONArray()
        val incomingFocusSessions = mutableListOf<FocusSessionEntity>()
        for (i in 0 until focusArray.length()) {
            val fsObj = focusArray.getJSONObject(i)
            val endedAt = fsObj.optLong("endedAtUtc", -1L)
            val rawLinkedTask = fsObj.optString("linkedTaskId", "")
            val rawLinkedNote = fsObj.optString("linkedNoteId", "")
            incomingFocusSessions.add(
                FocusSessionEntity(
                    id = fsObj.optString("id", UUID.randomUUID().toString()),
                    sessionType = fsObj.optString("sessionType", "WORK"),
                    linkedTaskId = rawLinkedTask.takeIf { it.isNotBlank() && it != "null" },
                    linkedNoteId = rawLinkedNote.takeIf { it.isNotBlank() && it != "null" },
                    plannedDurationSec = fsObj.optInt("plannedDurationSec", 1500),
                    actualFocusedSec = fsObj.optInt("actualFocusedSec", 0),
                    status = fsObj.optString("status", "COMPLETED"),
                    startedAtUtc = fsObj.optLong("startedAtUtc", now),
                    endedAtUtc = if (endedAt > 0L) endedAt else null,
                    localDateKey = fsObj.optString("localDateKey", ""),
                    timeZoneId = fsObj.optString("timeZoneId", java.util.TimeZone.getDefault().id)
                )
            )
        }

        // Sanitize all entity foreign key references to avoid SQLiteConstraintException
        val existingFolderIds = if (strategy == RestoreStrategy.REPLACE) emptySet() else repository.getAllFoldersDirect().map { it.id }.toSet()
        val allValidFolderIds = existingFolderIds + incomingFolders.map { it.id }

        val sanitizedFolders = incomingFolders.map { folder ->
            val validParentId = folder.parentId?.takeIf { it != folder.id && allValidFolderIds.contains(it) }
            if (validParentId != folder.parentId) folder.copy(parentId = validParentId) else folder
        }

        val filesRootCanonical = context.filesDir.canonicalPath
        fun normalizeRestoredSubdirPath(rawPath: String, subdir: String, preferAbsolute: Boolean): String {
            val trimmed = rawPath.trim()
            if (trimmed.isBlank()) return rawPath
            val marker = "/$subdir/"
            val idx = trimmed.lastIndexOf(marker)
            val fileName = when {
                idx >= 0 -> trimmed.substring(idx + marker.length)
                trimmed.startsWith("$subdir/") -> trimmed.removePrefix("$subdir/")
                else -> null
            }?.takeIf { it.isNotBlank() && !it.contains('/') && !it.contains('\\') } ?: return rawPath
            val isAlreadyLocal = if (trimmed.startsWith("/")) {
                runCatching { File(trimmed).canonicalPath.startsWith(filesRootCanonical + File.separator) }
                    .getOrDefault(false)
            } else {
                !preferAbsolute
            }
            if (isAlreadyLocal) return rawPath
            return if (preferAbsolute) File(context.filesDir, "$subdir/$fileName").absolutePath else "$subdir/$fileName"
        }

        val sanitizedNotes = incomingNotes.map { note ->
            val validFolderId = note.folderId?.takeIf { allValidFolderIds.contains(it) }
            val normalizedImageUri = if (note.imageUri.isNotBlank()) {
                note.imageUri.split(",").joinToString(",") { part ->
                    val trimmedPart = part.trim()
                    when {
                        trimmedPart.contains("/attachments/") -> normalizeRestoredSubdirPath(trimmedPart, "attachments", preferAbsolute = true)
                        trimmedPart.contains("/vault_files/") -> normalizeRestoredSubdirPath(trimmedPart, "vault_files", preferAbsolute = true)
                        trimmedPart.contains("/note_backgrounds/") -> normalizeRestoredSubdirPath(trimmedPart, "note_backgrounds", preferAbsolute = true)
                        else -> part
                    }
                }
            } else note.imageUri

            var normalizedCanvasJson = note.canvasDataJson
            if (normalizedCanvasJson.isNotBlank()) {
                val bg = SmartNoteUtils.parseNoteBackgroundImage(normalizedCanvasJson)
                if (bg.contains("/note_backgrounds/") || bg.contains("/attachments/")) {
                    val sub = if (bg.contains("/note_backgrounds/")) "note_backgrounds" else "attachments"
                    val mappedBg = normalizeRestoredSubdirPath(bg, sub, preferAbsolute = true)
                    if (mappedBg != bg) {
                        normalizedCanvasJson = SmartNoteUtils.withNoteStyleMeta(
                            existingJson = normalizedCanvasJson,
                            textureKey = SmartNoteUtils.parseNoteTexture(normalizedCanvasJson),
                            backgroundImagePath = mappedBg
                        )
                    }
                }
                val clips = SmartNoteUtils.parseVoiceRecordings(normalizedCanvasJson)
                if (clips.any { it.filePath.contains("/voice_recordings/") }) {
                    val mappedClips = clips.map { c ->
                        if (c.filePath.contains("/voice_recordings/")) {
                            c.copy(filePath = normalizeRestoredSubdirPath(c.filePath, "voice_recordings", preferAbsolute = true))
                        } else c
                    }
                    if (mappedClips != clips) {
                        normalizedCanvasJson = SmartNoteUtils.withVoiceRecordings(normalizedCanvasJson, mappedClips)
                    }
                }
            }

            if (validFolderId != note.folderId || normalizedImageUri != note.imageUri || normalizedCanvasJson != note.canvasDataJson) {
                note.copy(
                    folderId = validFolderId,
                    imageUri = normalizedImageUri,
                    canvasDataJson = normalizedCanvasJson
                )
            } else {
                note
            }
        }

        val existingNotesAll = if (strategy == RestoreStrategy.REPLACE) emptyList() else repository.getAllNotesDirect()
        val mergedNotesById = (existingNotesAll + sanitizedNotes).associateBy { it.id }
        val existingNoteIds = existingNotesAll.map { it.id }.toSet()
        val allValidNoteIds = existingNoteIds + sanitizedNotes.map { it.id }
        val lockedNoteIds = mergedNotesById.values.filter { it.isLocked }.map { it.id }.toSet()

        val sanitizedStickies = incomingStickies.map { sticky ->
            val validNoteId = sticky.noteId?.takeIf { allValidNoteIds.contains(it) }
            if (validNoteId != sticky.noteId) sticky.copy(noteId = validNoteId) else sticky
        }

        val existingCanvasIds = if (strategy == RestoreStrategy.REPLACE) emptySet() else repository.getAllCanvasesDirect().map { it.id }.toSet()
        val allValidCanvasIds = existingCanvasIds + incomingCanvases.map { it.id }

        val sanitizedElements = incomingElements.filter { allValidCanvasIds.contains(it.canvasId) }

        val sanitizedAttachments = incomingAttachments.map { att ->
            val validNoteId = att.noteId?.takeIf { allValidNoteIds.contains(it) }
            val validCanvasId = att.canvasId?.takeIf { allValidCanvasIds.contains(it) }
            val normalizedRelPath = when {
                att.relativePath.isBlank() -> att.relativePath
                att.relativePath.startsWith("attachments/") || att.relativePath.startsWith("vault_files/") -> att.relativePath
                att.relativePath.contains("/vault_files/") -> normalizeRestoredSubdirPath(att.relativePath, "vault_files", preferAbsolute = false)
                att.relativePath.contains("/attachments/") -> normalizeRestoredSubdirPath(att.relativePath, "attachments", preferAbsolute = false)
                else -> {
                    val candidateCanonical = runCatching { File(att.relativePath).canonicalPath }.getOrDefault("")
                    if (att.relativePath.startsWith("/") && !candidateCanonical.startsWith(filesRootCanonical + File.separator)) {
                        val safeName = File(att.relativePath).name.ifBlank { "${att.id}_${att.fileName}" }.replace(Regex("[/\\\\]"), "_")
                        if (att.isLocked) "vault_files/$safeName" else "attachments/$safeName"
                    } else {
                        att.relativePath
                    }
                }
            }
            if (validNoteId != att.noteId || validCanvasId != att.canvasId || normalizedRelPath != att.relativePath) {
                att.copy(noteId = validNoteId, canvasId = validCanvasId, relativePath = normalizedRelPath)
            } else {
                att
            }
        }

        val existingAttachmentIds = if (strategy == RestoreStrategy.REPLACE) emptySet() else repository.getAllAttachmentsDirect().map { it.id }.toSet()
        val allValidAttachmentIds = existingAttachmentIds + sanitizedAttachments.map { it.id }

        val sanitizedAnnotations = incomingAnnotations.filter { allValidAttachmentIds.contains(it.attachmentId) }
        val sanitizedReminders = incomingReminders.filter { allValidNoteIds.contains(it.noteId) }
        val allValidReminderIds = (if (strategy == RestoreStrategy.REPLACE) emptySet() else repository.getAllRemindersDirect().map { it.id }.toSet()) +
            sanitizedReminders.map { it.id }

        val existingTaskIds = if (strategy == RestoreStrategy.REPLACE) emptySet() else repository.getAllTasksDirect().map { it.id }.toSet()
        val allValidTaskIds = existingTaskIds + incomingTasks.map { it.id }
        val sanitizedTasks = incomingTasks.map { task ->
            val safeNoteId = task.linkedNoteId?.takeIf { allValidNoteIds.contains(it) }
            val safeRemId = task.linkedReminderId?.takeIf { allValidReminderIds.contains(it) }
            task.copy(linkedNoteId = safeNoteId, linkedReminderId = safeRemId)
        }
        val sanitizedTaskDeps = incomingTaskDeps.filter { dep ->
            allValidTaskIds.contains(dep.taskId) && allValidTaskIds.contains(dep.dependsOnTaskId)
        }
        val allValidHabitIds = (if (strategy == RestoreStrategy.REPLACE) emptySet() else repository.getAllHabitsDirect().map { it.id }.toSet()) +
            incomingHabits.map { it.id }
        val sanitizedHabitLogs = incomingHabitLogs.filter { allValidHabitIds.contains(it.habitId) }
        val sanitizedFocusSessions = incomingFocusSessions.map { s ->
            s.copy(
                linkedTaskId = s.linkedTaskId?.takeIf { allValidTaskIds.contains(it) },
                linkedNoteId = s.linkedNoteId?.takeIf { allValidNoteIds.contains(it) && it !in lockedNoteIds }
            )
        }
        val sanitizedLinks = incomingNoteLinks.mapNotNull { link ->
            if (!allValidNoteIds.contains(link.sourceNoteId) || link.sourceNoteId in lockedNoteIds) {
                null
            } else {
                val safeTargetId = link.targetNoteId?.takeIf {
                    allValidNoteIds.contains(it) && it !in lockedNoteIds && it != link.sourceNoteId
                }
                if (safeTargetId != link.targetNoteId) link.copy(targetNoteId = safeTargetId) else link
            }
        }
        val sanitizedVersions = incomingNoteVersions.filter {
            allValidNoteIds.contains(it.noteId) && it.noteId !in lockedNoteIds
        }

        // Atomic Transaction: Use non-cascading safe upserts so colliding parent rows in MERGE
        // never fire ON DELETE CASCADE and delete existing child rows.
        repository.runInTransaction {
            if (strategy == RestoreStrategy.REPLACE) {
                repository.clearAllData()
            }

            repository.upsertFoldersSafe(sanitizedFolders)
            val tagIdRemap = repository.upsertTagsSafe(incomingTags)
            val validTagIds = repository.getAllTagsDirect().map { it.id }.toSet()
            repository.upsertNotesSafe(sanitizedNotes)

            val sanitizedNoteTags = incomingNoteTags.mapNotNull { nt ->
                val resolvedTagId = tagIdRemap[nt.tagId] ?: nt.tagId
                if (allValidNoteIds.contains(nt.noteId) && validTagIds.contains(resolvedTagId)) {
                    nt.copy(tagId = resolvedTagId)
                } else {
                    null
                }
            }.distinctBy { it.noteId to it.tagId }
            repository.insertNoteTags(sanitizedNoteTags)

            repository.upsertStickyNotesSafe(sanitizedStickies)
            repository.upsertCanvasesSafe(incomingCanvases)
            repository.upsertCanvasElementsSafe(sanitizedElements)
            repository.upsertAttachmentsSafe(sanitizedAttachments)
            repository.upsertAnnotationsSafe(sanitizedAnnotations)
            repository.upsertBookmarksSafe(incomingBookmarks)
            repository.upsertRemindersSafe(sanitizedReminders)
            repository.upsertNoteTemplatesSafe(incomingNoteTemplates)
            repository.upsertNoteLinksSafe(sanitizedLinks)
            repository.upsertNoteVersionsSafe(sanitizedVersions)
            repository.upsertKanbanColumnsSafe(incomingKanbanColumns)
            repository.upsertTasksSafe(sanitizedTasks)
            repository.upsertTaskDependenciesSafe(sanitizedTaskDeps)
            repository.upsertHabitsSafe(incomingHabits)
            repository.upsertHabitLogsSafe(sanitizedHabitLogs)
            repository.upsertFocusSessionsSafe(sanitizedFocusSessions)
        }
        repository.ensureBuiltInTemplatesSeeded()
        repository.ensureDefaultKanbanColumnsSeeded()
        repository.reconcileAllKnowledgeLinks()

        // Restore Sticky Board spatial layouts & dashboard widgets cleanly into SharedPreferences
        val incomingLayoutsRaw = root.optString("stickyBoardLayouts", "")
        val incomingWidgetsRaw = root.optString("dashboardWidgetsConfig", "")
        val settingsPrefs = context.getSharedPreferences("noteland_settings_prefs", Context.MODE_PRIVATE)
        if (strategy == RestoreStrategy.REPLACE) {
            settingsPrefs.edit {
                if (incomingLayoutsRaw.isNotBlank()) {
                    putString("sticky_board_layouts", incomingLayoutsRaw)
                } else {
                    remove("sticky_board_layouts")
                }
                if (incomingWidgetsRaw.isNotBlank()) {
                    putString("dashboard_widgets_config", incomingWidgetsRaw)
                } else {
                    remove("dashboard_widgets_config")
                }
            }
        } else {
            if (incomingLayoutsRaw.isNotBlank()) {
                try {
                    val existingRaw = settingsPrefs.getString("sticky_board_layouts", "") ?: ""
                    val mergedObj = if (existingRaw.isNotBlank()) JSONObject(existingRaw) else JSONObject()
                    val incomingObj = JSONObject(incomingLayoutsRaw)
                    val keys = incomingObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        mergedObj.put(key, incomingObj.getJSONObject(key))
                    }
                    settingsPrefs.edit { putString("sticky_board_layouts", mergedObj.toString()) }
                } catch (_: Exception) {
                    settingsPrefs.edit { putString("sticky_board_layouts", incomingLayoutsRaw) }
                }
            }
            if (incomingWidgetsRaw.isNotBlank()) {
                settingsPrefs.edit { putString("dashboard_widgets_config", incomingWidgetsRaw) }
            }
        }

        return PayloadRestoreStats(
            restoredNotesCount = incomingNotes.size,
            restoredFoldersCount = incomingFolders.size,
            restoredAttachments = sanitizedAttachments
        )
    }

    suspend fun saveBackupToFile(): File {
        val notes = repository.getAllNotesDirect()
        val folders = repository.getAllFoldersDirect()
        val tags = repository.getAllTagsDirect()
        val noteTags = repository.getAllNoteTagsDirect()
        val stickies = repository.getAllStickyNotesDirect()
        val canvases = repository.getAllCanvasesDirect()
        val canvasElements = repository.getAllCanvasElementsDirect()
        val attachments = repository.getAllAttachmentsDirect()
        val annotations = repository.getAllAnnotationsDirect()
        val bookmarks = repository.getAllBookmarksDirect()
        val reminders = repository.getAllRemindersDirect()
        val lockedIds = notes.filter { it.isLocked }.map { it.id }.toSet()
        val noteLinks = repository.getAllNoteLinksDirect()
            .filter { it.sourceNoteId !in lockedIds && (it.targetNoteId == null || it.targetNoteId !in lockedIds) }
        val noteTemplates = repository.getAllNoteTemplatesDirect()
        val noteVersions = repository.getAllNoteVersionsDirect()
            .filter { it.noteId !in lockedIds }
        val kanbanColumns = repository.getAllKanbanColumnsDirect()
        val tasks = repository.getAllTasksDirect()
        val taskDependencies = repository.getAllTaskDependenciesDirect()
        val habits = repository.getAllHabitsDirect()
        val habitLogs = repository.getAllHabitLogsDirect()
        val focusSessions = repository.getAllFocusSessionsDirect()
        val settingsPrefs = context.getSharedPreferences("noteland_settings_prefs", Context.MODE_PRIVATE)
        val stickyBoardLayoutsJson = settingsPrefs.getString("sticky_board_layouts", "") ?: ""
        val dashboardWidgetsJson = settingsPrefs.getString("dashboard_widgets_config", "") ?: ""

        val json = buildDatabaseJson(
            notes = notes,
            folders = folders,
            tags = tags,
            noteTags = noteTags,
            stickies = stickies,
            canvases = canvases,
            canvasElements = canvasElements,
            attachments = attachments,
            annotations = annotations,
            bookmarks = bookmarks,
            reminders = reminders,
            noteLinks = noteLinks,
            noteTemplates = noteTemplates,
            noteVersions = noteVersions,
            kanbanColumns = kanbanColumns,
            tasks = tasks,
            taskDependencies = taskDependencies,
            habits = habits,
            habitLogs = habitLogs,
            focusSessions = focusSessions,
            stickyBoardLayoutsJson = stickyBoardLayoutsJson,
            dashboardWidgetsJson = dashboardWidgetsJson
        )
        val file = File(context.filesDir, "noteland_backup.json")
        file.writeText(json)
        return file
    }

    suspend fun restoreFromJson(json: String): Result<Int> {
        return try {
            val root = JSONObject(json)
            val stats = restoreDatabasePayload(root, RestoreStrategy.MERGE)
            Result.success(stats.restoredNotesCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private fun readZipEntries(zipFile: File): Map<String, ByteArray> {
        val map = mutableMapOf<String, ByteArray>()
        FileInputStream(zipFile).use { fis ->
            ZipInputStream(fis).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val buffer = ByteArrayOutputStream()
                    val temp = ByteArray(4096)
                    var read: Int
                    while (zis.read(temp).also { read = it } != -1) {
                        buffer.write(temp, 0, read)
                    }
                    map[entry.name] = buffer.toByteArray()
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
        return map
    }

    fun calculateSha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun buildDatabaseJson(
        notes: List<NoteEntity>,
        folders: List<FolderEntity>,
        tags: List<TagEntity>,
        noteTags: List<NoteTagEntity>,
        stickies: List<StickyNoteEntity>,
        canvases: List<CanvasEntity>,
        canvasElements: List<CanvasElementEntity>,
        attachments: List<AttachmentEntity>,
        annotations: List<AnnotationEntity>,
        bookmarks: List<BookmarkEntity>,
        reminders: List<ReminderEntity>,
        noteLinks: List<NoteLinkEntity> = emptyList(),
        noteTemplates: List<NoteTemplateEntity> = emptyList(),
        noteVersions: List<NoteVersionEntity> = emptyList(),
        kanbanColumns: List<KanbanColumnEntity> = emptyList(),
        tasks: List<TaskEntity> = emptyList(),
        taskDependencies: List<TaskDependencyEntity> = emptyList(),
        habits: List<HabitEntity> = emptyList(),
        habitLogs: List<HabitLogEntity> = emptyList(),
        focusSessions: List<FocusSessionEntity> = emptyList(),
        stickyBoardLayoutsJson: String = "",
        dashboardWidgetsJson: String = ""
    ): String {
        val root = JSONObject()
        root.put("app", APP_IDENTIFIER)
        root.put("schemaVersion", CURRENT_SCHEMA_VERSION)
        root.put("exportedAtUtc", System.currentTimeMillis())

        // Notes
        val notesArray = JSONArray()
        for (n in notes) {
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("title", n.title)
            obj.put("content", n.content)
            obj.put("noteType", n.noteType)
            obj.put("colorHex", n.colorHex)
            obj.put("folderId", n.folderId ?: "")
            obj.put("folderName", n.folderName)
            obj.put("tagsCsv", n.tagsCsv)
            obj.put("isPinned", n.isPinned)
            obj.put("isArchived", n.isArchived)
            obj.put("isDeleted", n.isDeleted)
            obj.put("isLocked", n.isLocked)
            obj.put("isHidden", n.isHidden)
            obj.put("isFavorite", n.isFavorite)
            obj.put("reminderTimestamp", n.reminderTimestamp ?: -1L)
            obj.put("canvasDataJson", n.canvasDataJson)
            obj.put("checklistJson", n.checklistJson)
            obj.put("imageUri", n.imageUri)
            obj.put("createdAtUtc", n.createdAtUtc)
            obj.put("updatedAtUtc", n.updatedAtUtc)
            obj.put("deletedAtUtc", n.deletedAtUtc ?: -1L)
            notesArray.put(obj)
        }
        root.put("notes", notesArray)

        // Folders
        val foldersArray = JSONArray()
        for (f in folders) {
            val obj = JSONObject()
            obj.put("id", f.id)
            obj.put("parentId", f.parentId ?: "")
            obj.put("name", f.name)
            obj.put("colorHex", f.colorHex)
            obj.put("iconName", f.iconName)
            obj.put("createdAtUtc", f.createdAtUtc)
            obj.put("updatedAtUtc", f.updatedAtUtc)
            foldersArray.put(obj)
        }
        root.put("folders", foldersArray)

        // Tags
        val tagsArray = JSONArray()
        for (t in tags) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("name", t.name)
            obj.put("colorHex", t.colorHex)
            obj.put("createdAtUtc", t.createdAtUtc)
            tagsArray.put(obj)
        }
        root.put("tags", tagsArray)

        // NoteTags
        val noteTagsArray = JSONArray()
        for (nt in noteTags) {
            val obj = JSONObject()
            obj.put("noteId", nt.noteId)
            obj.put("tagId", nt.tagId)
            obj.put("assignedAtUtc", nt.assignedAtUtc)
            noteTagsArray.put(obj)
        }
        root.put("noteTags", noteTagsArray)

        // Sticky notes
        val stickiesArray = JSONArray()
        for (s in stickies) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("noteId", s.noteId ?: "")
            obj.put("title", s.title)
            obj.put("content", s.content)
            obj.put("colorHex", s.colorHex)
            obj.put("orderIndex", s.orderIndex)
            obj.put("isPinned", s.isPinned)
            obj.put("createdAtUtc", s.createdAtUtc)
            obj.put("updatedAtUtc", s.updatedAtUtc)
            stickiesArray.put(obj)
        }
        root.put("stickyNotes", stickiesArray)

        // Canvases
        val canvasesArray = JSONArray()
        for (c in canvases) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("title", c.title)
            obj.put("backgroundColorHex", c.backgroundColorHex)
            obj.put("width", c.width.toDouble())
            obj.put("height", c.height.toDouble())
            obj.put("createdAtUtc", c.createdAtUtc)
            obj.put("updatedAtUtc", c.updatedAtUtc)
            canvasesArray.put(obj)
        }
        root.put("canvases", canvasesArray)

        // Canvas elements
        val elementsArray = JSONArray()
        for (e in canvasElements) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("canvasId", e.canvasId)
            obj.put("elementType", e.elementType)
            obj.put("elementDataJson", e.elementDataJson)
            obj.put("zIndex", e.zIndex)
            obj.put("createdAtUtc", e.createdAtUtc)
            elementsArray.put(obj)
        }
        root.put("canvasElements", elementsArray)

        // Attachments
        val attArray = JSONArray()
        for (a in attachments) {
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("noteId", a.noteId ?: "")
            obj.put("canvasId", a.canvasId ?: "")
            obj.put("fileName", a.fileName)
            obj.put("fileExtension", a.fileExtension)
            obj.put("relativePath", a.relativePath)
            obj.put("mimeType", a.mimeType)
            obj.put("sizeBytes", a.sizeBytes)
            obj.put("createdAtUtc", a.createdAtUtc)
            obj.put("folderId", a.folderId ?: "")
            obj.put("contentHash", a.contentHash)
            obj.put("pageCount", a.pageCount)
            obj.put("isFavorite", a.isFavorite)
            obj.put("isArchived", a.isArchived)
            obj.put("isLocked", a.isLocked)
            obj.put("isHidden", a.isHidden)
            obj.put("isDeleted", a.isDeleted)
            obj.put("deletedAtUtc", a.deletedAtUtc ?: -1L)
            obj.put("updatedAtUtc", a.updatedAtUtc)
            attArray.put(obj)
        }
        root.put("attachments", attArray)

        // Annotations
        val annotArray = JSONArray()
        for (an in annotations) {
            val obj = JSONObject()
            obj.put("id", an.id)
            obj.put("attachmentId", an.attachmentId)
            obj.put("annotationType", an.annotationType)
            obj.put("pageNumber", an.pageNumber)
            obj.put("coordinatesJson", an.coordinatesJson)
            obj.put("content", an.content)
            obj.put("colorHex", an.colorHex)
            obj.put("createdAtUtc", an.createdAtUtc)
            annotArray.put(obj)
        }
        root.put("annotations", annotArray)

        // Bookmarks
        val bookmarksArray = JSONArray()
        for (b in bookmarks) {
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("targetType", b.targetType)
            obj.put("targetId", b.targetId)
            obj.put("label", b.label)
            obj.put("createdAtUtc", b.createdAtUtc)
            bookmarksArray.put(obj)
        }
        root.put("bookmarks", bookmarksArray)

        // Reminders
        val remArray = JSONArray()
        for (r in reminders) {
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("noteId", r.noteId)
            obj.put("scheduledTimeUtc", r.scheduledTimeUtc)
            obj.put("repeatInterval", r.repeatInterval ?: "")
            obj.put("isCompleted", r.isCompleted)
            obj.put("createdAtUtc", r.createdAtUtc)
            remArray.put(obj)
        }
        root.put("reminders", remArray)

        // NoteLinks (Phase 1)
        val linksArray = JSONArray()
        for (l in noteLinks) {
            val obj = JSONObject()
            obj.put("id", l.id)
            obj.put("sourceNoteId", l.sourceNoteId)
            obj.put("targetNoteId", l.targetNoteId ?: "")
            obj.put("targetTitle", l.targetTitle)
            obj.put("normalizedTargetTitle", l.normalizedTargetTitle)
            obj.put("anchorHeading", l.anchorHeading)
            obj.put("displayAlias", l.displayAlias)
            obj.put("contextSnippet", l.contextSnippet)
            obj.put("createdAtUtc", l.createdAtUtc)
            obj.put("updatedAtUtc", l.updatedAtUtc)
            linksArray.put(obj)
        }
        root.put("noteLinks", linksArray)

        // NoteTemplates (Phase 1)
        val templatesArray = JSONArray()
        for (tpl in noteTemplates) {
            val obj = JSONObject()
            obj.put("id", tpl.id)
            obj.put("name", tpl.name)
            obj.put("description", tpl.description)
            obj.put("category", tpl.category)
            obj.put("titlePattern", tpl.titlePattern)
            obj.put("contentTemplate", tpl.contentTemplate)
            obj.put("defaultTagsCsv", tpl.defaultTagsCsv)
            obj.put("defaultColorHex", tpl.defaultColorHex)
            obj.put("checklistTemplateJson", tpl.checklistTemplateJson)
            obj.put("isBuiltIn", tpl.isBuiltIn)
            obj.put("usageCount", tpl.usageCount)
            obj.put("createdAtUtc", tpl.createdAtUtc)
            obj.put("updatedAtUtc", tpl.updatedAtUtc)
            templatesArray.put(obj)
        }
        root.put("noteTemplates", templatesArray)

        // NoteVersions (Phase 1)
        val versionsArray = JSONArray()
        for (v in noteVersions) {
            val obj = JSONObject()
            obj.put("id", v.id)
            obj.put("noteId", v.noteId)
            obj.put("versionNumber", v.versionNumber)
            obj.put("title", v.title)
            obj.put("content", v.content)
            obj.put("tagsCsv", v.tagsCsv)
            obj.put("checklistJson", v.checklistJson)
            obj.put("changeSummary", v.changeSummary)
            obj.put("wordCount", v.wordCount)
            obj.put("createdAtUtc", v.createdAtUtc)
            versionsArray.put(obj)
        }
        root.put("noteVersions", versionsArray)

        // Phase 2: KanbanColumns
        val colsArray = JSONArray()
        for (c in kanbanColumns) {
            colsArray.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("boardId", c.boardId)
                    put("title", c.title)
                    put("colorHex", c.colorHex)
                    put("position", c.position)
                    put("wipLimit", c.wipLimit)
                    put("isDoneColumn", c.isDoneColumn)
                    put("createdAtUtc", c.createdAtUtc)
                    put("updatedAtUtc", c.updatedAtUtc)
                }
            )
        }
        root.put("kanbanColumns", colsArray)

        // Phase 2: Tasks
        val tasksArray = JSONArray()
        for (t in tasks) {
            tasksArray.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("description", t.description)
                    put("columnId", t.columnId)
                    put("orderInColumn", t.orderInColumn)
                    put("priority", t.priority)
                    put("isCompleted", t.isCompleted)
                    put("completedAtUtc", t.completedAtUtc ?: -1L)
                    put("dueAtUtc", t.dueAtUtc ?: -1L)
                    put("dueTimeZoneId", t.dueTimeZoneId)
                    put("scheduledStartUtc", t.scheduledStartUtc ?: -1L)
                    put("scheduledEndUtc", t.scheduledEndUtc ?: -1L)
                    put("estimatedMinutes", t.estimatedMinutes)
                    put("recurrenceRule", t.recurrenceRule)
                    put("recurrenceInterval", t.recurrenceInterval)
                    put("recurrenceDaysCsv", t.recurrenceDaysCsv)
                    put("recurrenceParentId", t.recurrenceParentId ?: "")
                    put("subtasksJson", t.subtasksJson)
                    put("linkedNoteId", t.linkedNoteId ?: "")
                    put("linkedReminderId", t.linkedReminderId ?: "")
                    put("category", t.category)
                    put("colorHex", t.colorHex)
                    put("tagsCsv", t.tagsCsv)
                    put("isArchived", t.isArchived)
                    put("createdAtUtc", t.createdAtUtc)
                    put("updatedAtUtc", t.updatedAtUtc)
                }
            )
        }
        root.put("tasks", tasksArray)

        // Phase 2: TaskDependencies
        val depsArray = JSONArray()
        for (d in taskDependencies) {
            depsArray.put(
                JSONObject().apply {
                    put("taskId", d.taskId)
                    put("dependsOnTaskId", d.dependsOnTaskId)
                    put("createdAtUtc", d.createdAtUtc)
                }
            )
        }
        root.put("taskDependencies", depsArray)

        // Phase 2: Habits
        val habitsArray = JSONArray()
        for (h in habits) {
            habitsArray.put(
                JSONObject().apply {
                    put("id", h.id)
                    put("title", h.title)
                    put("description", h.description)
                    put("category", h.category)
                    put("colorHex", h.colorHex)
                    put("iconName", h.iconName)
                    put("frequencyType", h.frequencyType)
                    put("targetPerDay", h.targetPerDay)
                    put("targetDaysPerWeek", h.targetDaysPerWeek)
                    put("reminderHour", h.reminderHour ?: -1)
                    put("reminderMinute", h.reminderMinute ?: -1)
                    put("timeZoneId", h.timeZoneId)
                    put("isArchived", h.isArchived)
                    put("createdAtUtc", h.createdAtUtc)
                    put("updatedAtUtc", h.updatedAtUtc)
                }
            )
        }
        root.put("habits", habitsArray)

        // Phase 2: HabitLogs
        val habitLogsArray = JSONArray()
        for (hl in habitLogs) {
            habitLogsArray.put(
                JSONObject().apply {
                    put("id", hl.id)
                    put("habitId", hl.habitId)
                    put("localDateKey", hl.localDateKey)
                    put("completedCount", hl.completedCount)
                    put("targetCountSnapshot", hl.targetCountSnapshot)
                    put("isCompleted", hl.isCompleted)
                    put("note", hl.note)
                    put("timeZoneId", hl.timeZoneId)
                    put("completedAtUtc", hl.completedAtUtc)
                }
            )
        }
        root.put("habitLogs", habitLogsArray)

        // Phase 2: FocusSessions
        val focusArray = JSONArray()
        for (fs in focusSessions) {
            focusArray.put(
                JSONObject().apply {
                    put("id", fs.id)
                    put("sessionType", fs.sessionType)
                    put("linkedTaskId", fs.linkedTaskId ?: "")
                    put("linkedNoteId", fs.linkedNoteId ?: "")
                    put("plannedDurationSec", fs.plannedDurationSec)
                    put("actualFocusedSec", fs.actualFocusedSec)
                    put("status", fs.status)
                    put("startedAtUtc", fs.startedAtUtc)
                    put("endedAtUtc", fs.endedAtUtc ?: -1L)
                    put("localDateKey", fs.localDateKey)
                    put("timeZoneId", fs.timeZoneId)
                }
            )
        }
        root.put("focusSessions", focusArray)

        if (stickyBoardLayoutsJson.isNotBlank()) {
            root.put("stickyBoardLayouts", stickyBoardLayoutsJson)
        }
        if (dashboardWidgetsJson.isNotBlank()) {
            root.put("dashboardWidgetsConfig", dashboardWidgetsJson)
        }

        return root.toString(2)
    }
}
