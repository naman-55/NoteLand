package com.example.data.storage

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.example.data.model.AttachmentEntity
import com.example.security.SecurityManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

data class AttachmentImportOutcome(
    val attachment: AttachmentEntity,
    val deduplicatedFromExisting: Boolean = false
)

class AttachmentStorageManager(private val context: Context) {

    private val securityManager: SecurityManager by lazy { SecurityManager(context) }

    private val attachmentsDir: File = File(context.filesDir, "attachments").apply {
        if (!exists()) mkdirs()
    }

    private val vaultFilesDir: File = File(context.filesDir, "vault_files").apply {
        if (!exists()) mkdirs()
    }

    private val tempVaultViewCacheDir: File = File(context.cacheDir, "vault_view_cache").apply {
        if (!exists()) mkdirs()
    }

    companion object {
        private val BLOCKED_EXTENSIONS = setOf(
            "apk", "dex", "jar", "so", "exe", "dll", "bat", "cmd", "sh", "msi", "vbs", "js"
        )

        private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "gif", "bmp", "heic", "heif")

        fun sanitizeFileName(rawName: String, fallback: String = "document"): String {
            val cleaned = rawName
                .substringAfterLast('/')
                .substringAfterLast('\\')
                .replace(Regex("[\\u0000-\\u001F<>:\"|?*]"), "_")
                .trim()
            return cleaned.ifBlank { fallback }
        }

        fun hasPdfHeader(headerBytes: ByteArray, length: Int): Boolean {
            if (length < 5) return false
            val prefix = String(headerBytes, 0, length.coerceAtMost(1024), Charsets.US_ASCII)
            return prefix.contains("%PDF-")
        }

        fun hasSupportedImageHeader(headerBytes: ByteArray, length: Int): Boolean {
            if (length < 3) return false
            // PNG: 89 50 4E 47
            if (length >= 4 &&
                headerBytes[0] == 0x89.toByte() &&
                headerBytes[1] == 0x50.toByte() &&
                headerBytes[2] == 0x4E.toByte() &&
                headerBytes[3] == 0x47.toByte()
            ) return true
            // JPEG: FF D8 FF
            if (headerBytes[0] == 0xFF.toByte() &&
                headerBytes[1] == 0xD8.toByte() &&
                headerBytes[2] == 0xFF.toByte()
            ) return true
            // GIF: GIF8
            if (length >= 4 &&
                headerBytes[0] == 0x47.toByte() &&
                headerBytes[1] == 0x49.toByte() &&
                headerBytes[2] == 0x46.toByte() &&
                headerBytes[3] == 0x38.toByte()
            ) return true
            // WEBP: RIFF....WEBP
            if (length >= 12 &&
                headerBytes[0] == 0x52.toByte() &&
                headerBytes[1] == 0x49.toByte() &&
                headerBytes[2] == 0x46.toByte() &&
                headerBytes[3] == 0x46.toByte() &&
                headerBytes[8] == 0x57.toByte() &&
                headerBytes[9] == 0x45.toByte() &&
                headerBytes[10] == 0x42.toByte() &&
                headerBytes[11] == 0x50.toByte()
            ) return true
            // BMP: BM
            if (headerBytes[0] == 0x42.toByte() && headerBytes[1] == 0x4D.toByte()) return true
            // HEIC / ISO BMFF: ....ftyp
            if (length >= 12 &&
                headerBytes[4] == 0x66.toByte() &&
                headerBytes[5] == 0x74.toByte() &&
                headerBytes[6] == 0x79.toByte() &&
                headerBytes[7] == 0x70.toByte()
            ) return true
            return false
        }
    }

    fun resolveDisplayName(uri: Uri, fallbackName: String): String {
        try {
            if (uri.scheme == "content") {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            val name = cursor.getString(idx)
                            if (!name.isNullOrBlank()) {
                                return sanitizeFileName(name, fallbackName)
                            }
                        }
                    }
                }
            }
            if (fallbackName.isNotBlank()) {
                return sanitizeFileName(fallbackName, "Document")
            }
            val lastSeg = uri.lastPathSegment
            if (!lastSeg.isNullOrBlank()) {
                return sanitizeFileName(lastSeg, "Document")
            }
        } catch (_: Exception) {
        }
        return sanitizeFileName(fallbackName, "Document")
    }

    private fun resolveExtension(fileName: String, mimeType: String, expectPdf: Boolean): String {
        val rawExt = fileName.substringAfterLast(".", "").lowercase(Locale.US)
        if (rawExt.isNotBlank() && rawExt.length <= 10) return rawExt
        val mime = mimeType.lowercase(Locale.US)
        return when {
            expectPdf || mime.contains("pdf") -> "pdf"
            mime.contains("png") -> "png"
            mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
            mime.contains("webp") -> "webp"
            mime.contains("gif") -> "gif"
            mime.contains("plain") -> "txt"
            mime.contains("markdown") -> "md"
            mime.contains("csv") -> "csv"
            else -> "bin"
        }
    }

    private fun validateFileHeaderAndType(
        firstHeader: ByteArray,
        firstHeaderLen: Int,
        totalBytes: Long,
        extension: String,
        mimeType: String,
        expectPdf: Boolean,
        validateFormatHeaders: Boolean
    ): Throwable? {
        if (totalBytes <= 0L) {
            return IllegalArgumentException("Selected file is empty.")
        }
        if (extension in BLOCKED_EXTENSIONS) {
            return SecurityException("Unsupported or unsafe file type '.$extension'.")
        }
        // Reject Android DEX / ELF / ZIP-APK headers disguised as document extensions
        if (firstHeaderLen >= 4) {
            val isElf = firstHeader[0] == 0x7F.toByte() && firstHeader[1] == 0x45.toByte() &&
                firstHeader[2] == 0x4C.toByte() && firstHeader[3] == 0x46.toByte()
            val isDex = firstHeader[0] == 0x64.toByte() && firstHeader[1] == 0x65.toByte() &&
                firstHeader[2] == 0x78.toByte()
            if (isElf || isDex) {
                return SecurityException("Executable binary files cannot be imported.")
            }
        }
        if (validateFormatHeaders) {
            val isPdfType = expectPdf || extension == "pdf" || mimeType.contains("pdf", ignoreCase = true)
            if (isPdfType && !hasPdfHeader(firstHeader, firstHeaderLen)) {
                return IllegalArgumentException("Invalid or corrupted PDF file (missing %PDF header).")
            }
            val lowerMime = mimeType.lowercase(Locale.US)
            val isKnownRasterImageMime = lowerMime in setOf(
                "image/png", "image/jpeg", "image/jpg", "image/webp",
                "image/gif", "image/bmp", "image/heic", "image/heif"
            )
            val isImageType = extension in IMAGE_EXTENSIONS || isKnownRasterImageMime
            if (isImageType && firstHeaderLen >= 4 && !hasSupportedImageHeader(firstHeader, firstHeaderLen)) {
                return IllegalArgumentException("Unsupported or corrupted image file.")
            }
        }
        return null
    }

    fun inspectPdfPageCount(file: File): Int {
        if (!file.exists() || file.length() <= 0L) return 0
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            renderer.pageCount.coerceAtLeast(0)
        } catch (_: Throwable) {
            0
        } finally {
            try { renderer?.close() } catch (_: Throwable) {}
            try { pfd?.close() } catch (_: Throwable) {}
        }
    }

    fun computeSha256(file: File): String {
        if (!file.exists() || !file.isFile) return ""
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { input ->
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }

    fun saveAttachment(
        fileName: String,
        mimeType: String,
        inputStream: InputStream,
        noteId: String? = null,
        canvasId: String? = null
    ): Result<AttachmentEntity> {
        return saveAttachmentWithDedup(
            fileName = fileName,
            mimeType = mimeType,
            inputStream = inputStream,
            noteId = noteId,
            canvasId = canvasId,
            folderId = null,
            expectPdf = false,
            existingByHashLookup = null
        ).map { it.attachment }
    }

    private data class StagedAttachmentResult(
        val targetFile: File,
        val uniqueFileName: String,
        val cleanName: String,
        val extension: String,
        val normalizedMime: String,
        val totalBytes: Long,
        val contentHash: String
    )

    private fun stageAndValidateAttachmentFromStream(
        fileName: String,
        mimeType: String,
        inputStream: InputStream,
        expectPdf: Boolean,
        validateFormatHeaders: Boolean
    ): Result<StagedAttachmentResult> {
        var stagedFile: File? = null
        return try {
            val cleanName = sanitizeFileName(fileName, if (expectPdf) "Document.pdf" else "Document")
            val extension = resolveExtension(cleanName, mimeType, expectPdf)
            val normalizedMime = when {
                mimeType.isNotBlank() && mimeType != "application/octet-stream" -> mimeType
                extension == "pdf" -> "application/pdf"
                extension == "png" -> "image/png"
                extension == "jpg" || extension == "jpeg" -> "image/jpeg"
                extension == "webp" -> "image/webp"
                extension == "txt" -> "text/plain"
                else -> "application/octet-stream"
            }

            val uniqueFileName = "${UUID.randomUUID()}.$extension"
            val targetFile = File(attachmentsDir, uniqueFileName)
            stagedFile = targetFile

            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            val headerSample = ByteArray(1024)
            var headerLen = 0
            var totalBytes = 0L

            FileOutputStream(targetFile).use { output ->
                while (true) {
                    val read = inputStream.read(buffer)
                    if (read <= 0) break
                    if (headerLen < headerSample.size) {
                        val toCopy = read.coerceAtMost(headerSample.size - headerLen)
                        System.arraycopy(buffer, 0, headerSample, headerLen, toCopy)
                        headerLen += toCopy
                    }
                    digest.update(buffer, 0, read)
                    output.write(buffer, 0, read)
                    totalBytes += read
                }
                output.flush()
            }

            val validationError = validateFileHeaderAndType(
                firstHeader = headerSample,
                firstHeaderLen = headerLen,
                totalBytes = totalBytes,
                extension = extension,
                mimeType = normalizedMime,
                expectPdf = expectPdf,
                validateFormatHeaders = validateFormatHeaders
            )
            if (validationError != null) {
                targetFile.delete()
                return Result.failure(validationError)
            }

            val contentHash = digest.digest().joinToString("") { "%02x".format(it) }
            Result.success(
                StagedAttachmentResult(
                    targetFile = targetFile,
                    uniqueFileName = uniqueFileName,
                    cleanName = cleanName,
                    extension = extension,
                    normalizedMime = normalizedMime,
                    totalBytes = totalBytes,
                    contentHash = contentHash
                )
            )
        } catch (e: Exception) {
            stagedFile?.let { if (it.exists()) it.delete() }
            Result.failure(e)
        }
    }

    private fun finalizeStagedAttachment(
        staged: StagedAttachmentResult,
        noteId: String?,
        canvasId: String?,
        folderId: String?,
        existingCandidate: AttachmentEntity?
    ): AttachmentImportOutcome {
        val existingDuplicate = existingCandidate?.takeIf { existing ->
            val existingFile = getFileFromRelativePath(existing.relativePath)
            !existing.isLocked && !existing.isDeleted && existingFile.exists() && existingFile.length() == staged.totalBytes
        }

        val finalRelPath: String
        val finalPageCount: Int
        val deduplicated: Boolean
        if (existingDuplicate != null) {
            staged.targetFile.delete()
            finalRelPath = existingDuplicate.relativePath
            val existingFile = getFileFromRelativePath(finalRelPath)
            finalPageCount = if (existingDuplicate.pageCount > 0) {
                existingDuplicate.pageCount
            } else if (staged.extension == "pdf") {
                inspectPdfPageCount(existingFile)
            } else 0
            deduplicated = true
        } else {
            finalRelPath = "attachments/${staged.uniqueFileName}"
            finalPageCount = if (staged.extension == "pdf") inspectPdfPageCount(staged.targetFile) else 0
            deduplicated = false
        }

        val now = System.currentTimeMillis()
        val attachment = AttachmentEntity(
            noteId = noteId,
            canvasId = canvasId,
            folderId = folderId?.takeIf { it.isNotBlank() },
            fileName = staged.cleanName,
            fileExtension = staged.extension,
            relativePath = finalRelPath,
            mimeType = staged.normalizedMime,
            sizeBytes = staged.totalBytes,
            contentHash = staged.contentHash,
            pageCount = finalPageCount,
            createdAtUtc = now,
            updatedAtUtc = now
        )
        return AttachmentImportOutcome(attachment = attachment, deduplicatedFromExisting = deduplicated)
    }

    fun saveAttachmentWithDedup(
        fileName: String,
        mimeType: String,
        inputStream: InputStream,
        noteId: String? = null,
        canvasId: String? = null,
        folderId: String? = null,
        expectPdf: Boolean = false,
        validateFormatHeaders: Boolean = false,
        existingByHashLookup: ((String) -> AttachmentEntity?)? = null
    ): Result<AttachmentImportOutcome> {
        val stagedResult = stageAndValidateAttachmentFromStream(
            fileName = fileName,
            mimeType = mimeType,
            inputStream = inputStream,
            expectPdf = expectPdf,
            validateFormatHeaders = validateFormatHeaders
        )
        val staged = stagedResult.getOrElse { return Result.failure(it) }
        return try {
            val existingCandidate = if (staged.contentHash.isNotBlank()) {
                existingByHashLookup?.invoke(staged.contentHash)
            } else null
            Result.success(finalizeStagedAttachment(staged, noteId, canvasId, folderId, existingCandidate))
        } catch (e: Exception) {
            if (staged.targetFile.exists()) staged.targetFile.delete()
            Result.failure(e)
        }
    }

    suspend fun saveAttachmentWithDedupSuspend(
        fileName: String,
        mimeType: String,
        inputStream: InputStream,
        noteId: String? = null,
        canvasId: String? = null,
        folderId: String? = null,
        expectPdf: Boolean = false,
        validateFormatHeaders: Boolean = false,
        existingByHashLookup: (suspend (String) -> AttachmentEntity?)? = null
    ): Result<AttachmentImportOutcome> {
        val stagedResult = stageAndValidateAttachmentFromStream(
            fileName = fileName,
            mimeType = mimeType,
            inputStream = inputStream,
            expectPdf = expectPdf,
            validateFormatHeaders = validateFormatHeaders
        )
        val staged = stagedResult.getOrElse { return Result.failure(it) }
        return try {
            val existingCandidate = if (staged.contentHash.isNotBlank()) {
                existingByHashLookup?.invoke(staged.contentHash)
            } else null
            Result.success(finalizeStagedAttachment(staged, noteId, canvasId, folderId, existingCandidate))
        } catch (e: Exception) {
            if (staged.targetFile.exists()) staged.targetFile.delete()
            Result.failure(e)
        }
    }

    fun saveFromUri(
        uri: Uri,
        fileName: String,
        noteId: String? = null,
        canvasId: String? = null,
        folderId: String? = null,
        expectPdf: Boolean = false,
        existingByHashLookup: ((String) -> AttachmentEntity?)? = null
    ): Result<AttachmentEntity> {
        return saveFromUriWithDedup(
            uri = uri,
            fileName = fileName,
            noteId = noteId,
            canvasId = canvasId,
            folderId = folderId,
            expectPdf = expectPdf,
            existingByHashLookup = existingByHashLookup
        ).map { it.attachment }
    }

    fun saveFromUriWithDedup(
        uri: Uri,
        fileName: String,
        noteId: String? = null,
        canvasId: String? = null,
        folderId: String? = null,
        expectPdf: Boolean = false,
        existingByHashLookup: ((String) -> AttachmentEntity?)? = null
    ): Result<AttachmentImportOutcome> {
        val resolvedName = resolveDisplayName(uri, fileName)
        val mimeType = context.contentResolver.getType(uri)
            ?: if (expectPdf || resolvedName.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "application/octet-stream"
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return Result.failure(IllegalArgumentException("Cannot open input stream for URI: $uri"))
        return inputStream.use { stream ->
            saveAttachmentWithDedup(
                fileName = resolvedName,
                mimeType = mimeType,
                inputStream = stream,
                noteId = noteId,
                canvasId = canvasId,
                folderId = folderId,
                expectPdf = expectPdf,
                validateFormatHeaders = true,
                existingByHashLookup = existingByHashLookup
            )
        }
    }

    suspend fun saveFromUriWithDedupSuspend(
        uri: Uri,
        fileName: String,
        noteId: String? = null,
        canvasId: String? = null,
        folderId: String? = null,
        expectPdf: Boolean = false,
        existingByHashLookup: (suspend (String) -> AttachmentEntity?)? = null
    ): Result<AttachmentImportOutcome> {
        val resolvedName = resolveDisplayName(uri, fileName)
        val mimeType = context.contentResolver.getType(uri)
            ?: if (expectPdf || resolvedName.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "application/octet-stream"
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return Result.failure(IllegalArgumentException("Cannot open input stream for URI: $uri"))
        return inputStream.use { stream ->
            saveAttachmentWithDedupSuspend(
                fileName = resolvedName,
                mimeType = mimeType,
                inputStream = stream,
                noteId = noteId,
                canvasId = canvasId,
                folderId = folderId,
                expectPdf = expectPdf,
                validateFormatHeaders = true,
                existingByHashLookup = existingByHashLookup
            )
        }
    }

    fun encryptExistingFileToVault(relativePath: String): Result<String> {
        return try {
            val source = getFileFromRelativePath(relativePath)
            if (!source.exists()) {
                return Result.failure(java.io.FileNotFoundException("Source file not found"))
            }
            if (securityManager.isEncryptedVaultFile(source)) {
                return Result.success(relativePath)
            }
            val encName = "vault_${UUID.randomUUID()}.enc"
            val destFile = File(vaultFilesDir, encName)
            FileInputStream(source).use { input ->
                securityManager.encryptStreamToFile(input, destFile)
            }
            Result.success("vault_files/$encName")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun decryptVaultFileToPlain(relativePath: String, targetExtension: String): Result<String> {
        return try {
            val source = getFileFromRelativePath(relativePath)
            if (!source.exists()) {
                return Result.failure(java.io.FileNotFoundException("Vault file not found"))
            }
            val cleanExt = targetExtension.ifBlank { "bin" }.lowercase(Locale.US)
            if (!securityManager.isEncryptedVaultFile(source)) {
                // Already plain
                return Result.success(relativePath)
            }
            val plainName = "${UUID.randomUUID()}.$cleanExt"
            val destFile = File(attachmentsDir, plainName)
            FileOutputStream(destFile).use { out ->
                securityManager.decryptFileToStream(source, out)
            }
            Result.success("attachments/$plainName")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createTempDecryptedFileForViewer(attachment: AttachmentEntity): File? {
        val stored = getFileFromRelativePath(attachment.relativePath)
        if (!stored.exists()) return null
        if (!securityManager.isEncryptedVaultFile(stored)) {
            return stored
        }
        return try {
            val ext = attachment.fileExtension.ifBlank { "pdf" }
            val tempFile = File(tempVaultViewCacheDir, "view_${attachment.id}_${UUID.randomUUID()}.$ext")
            FileOutputStream(tempFile).use { out ->
                securityManager.decryptFileToStream(stored, out)
            }
            tempFile
        } catch (_: Exception) {
            null
        }
    }

    fun createTempDecryptedFileForViewerByPath(rawFilePath: String, extensionHint: String = "pdf"): File? {
        val stored = getFileFromRelativePath(rawFilePath)
        if (!stored.exists()) return null
        if (!securityManager.isEncryptedVaultFile(stored)) {
            return stored
        }
        return try {
            val ext = extensionHint.ifBlank { "pdf" }
            val tempFile = File(tempVaultViewCacheDir, "view_path_${UUID.randomUUID()}.$ext")
            FileOutputStream(tempFile).use { out ->
                securityManager.decryptFileToStream(stored, out)
            }
            tempFile
        } catch (_: Exception) {
            null
        }
    }

    fun deleteTempViewerFile(file: File?) {
        if (file == null) return
        try {
            val cacheCanonical = tempVaultViewCacheDir.canonicalPath
            val fileCanonical = file.canonicalPath
            if (fileCanonical.startsWith(cacheCanonical + File.separator) && file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }

    fun clearTempVaultViewCache() {
        try {
            tempVaultViewCacheDir.listFiles()?.forEach { f ->
                if (f.isFile) f.delete()
            }
        } catch (_: Exception) {
        }
    }

    fun getFileFromRelativePath(relativePath: String): File {
        val trimmed = relativePath.trim()
        return if (trimmed.startsWith("/")) {
            File(trimmed)
        } else {
            File(context.filesDir, trimmed)
        }
    }

    fun deleteFile(relativePath: String): Boolean {
        if (relativePath.isBlank()) return true
        val file = getFileFromRelativePath(relativePath)
        return if (file.exists()) file.delete() else true
    }

    fun saveBackgroundImageFromUri(uri: Uri): Result<String> {
        return try {
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val extension = when {
                mimeType.contains("png") -> "png"
                mimeType.contains("webp") -> "webp"
                else -> "jpg"
            }
            val bgDir = File(context.filesDir, "note_backgrounds").apply {
                if (!exists()) mkdirs()
            }
            val destFile = File(bgDir, "bg_${UUID.randomUUID()}.$extension")
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(IllegalArgumentException("Cannot open background image URI: $uri"))
            inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (!destFile.exists() || destFile.length() == 0L) {
                destFile.delete()
                return Result.failure(IllegalStateException("Background image stream was empty"))
            }
            Result.success(destFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun saveImageAttachmentFromUri(uri: Uri): Result<String> {
        return try {
            val mimeType = (context.contentResolver.getType(uri) ?: "").lowercase(Locale.US)
            val extension = when {
                mimeType.contains("png") -> "png"
                mimeType.contains("webp") -> "webp"
                mimeType.contains("gif") -> "gif"
                mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
                else -> "png"
            }
            val destFile = File(attachmentsDir, "pasted_${UUID.randomUUID()}.$extension")
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(IllegalArgumentException("Cannot open pasted image URI: $uri"))
            inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (!destFile.exists() || destFile.length() == 0L) {
                destFile.delete()
                return Result.failure(IllegalStateException("Pasted image stream was empty"))
            }
            Result.success(destFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createVoiceRecordingFile(): File {
        val voiceDir = File(context.filesDir, "voice_recordings").apply {
            if (!exists()) mkdirs()
        }
        return File(voiceDir, "voice_${UUID.randomUUID()}.m4a")
    }

    fun resolveVoiceRecordingFile(storedPath: String): File {
        val trimmed = storedPath.trim()
        return if (trimmed.startsWith("/")) {
            File(trimmed)
        } else {
            File(context.filesDir, trimmed)
        }
    }

    fun toRelativeVoicePath(file: File): String {
        val root = context.filesDir.absolutePath.trimEnd('/') + "/"
        val abs = file.absolutePath
        return if (abs.startsWith(root)) abs.removePrefix(root) else abs
    }

    fun deleteVoiceRecordingFile(storedPath: String): Boolean {
        if (storedPath.isBlank()) return true
        val file = resolveVoiceRecordingFile(storedPath)
        return if (file.exists()) file.delete() else true
    }
}
