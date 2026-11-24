package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.AnnotationEntity
import com.example.data.model.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Query("SELECT * FROM attachments WHERE isDeleted = 0 AND isArchived = 0 AND isLocked = 0 AND isHidden = 0 ORDER BY updatedAtUtc DESC, createdAtUtc DESC")
    fun getAllAttachments(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE isDeleted = 1 ORDER BY deletedAtUtc DESC")
    fun getDeletedAttachments(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE isFavorite = 1 AND isDeleted = 0 AND isArchived = 0 AND isLocked = 0 AND isHidden = 0 ORDER BY updatedAtUtc DESC")
    fun getFavoriteAttachments(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE isArchived = 1 AND isDeleted = 0 AND isLocked = 0 ORDER BY updatedAtUtc DESC")
    fun getArchivedAttachments(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE isLocked = 1 AND isDeleted = 0 ORDER BY updatedAtUtc DESC")
    fun getLockedAttachments(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE folderId = :folderId AND isDeleted = 0 AND isArchived = 0 AND isLocked = 0 AND isHidden = 0 ORDER BY updatedAtUtc DESC")
    fun getAttachmentsByFolder(folderId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE noteId = :noteId AND isDeleted = 0 ORDER BY createdAtUtc DESC")
    fun getAttachmentsForNote(noteId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE id = :id LIMIT 1")
    suspend fun getAttachmentById(id: String): AttachmentEntity?

    @Query("SELECT * FROM attachments WHERE contentHash = :contentHash AND contentHash != '' AND isDeleted = 0 AND isLocked = 0 LIMIT 1")
    suspend fun getAttachmentByHash(contentHash: String): AttachmentEntity?

    @Query("SELECT * FROM attachments WHERE isDeleted = 1 AND deletedAtUtc IS NOT NULL AND deletedAtUtc <= :cutoffUtc")
    suspend fun getExpiredDeletedAttachmentsDirect(cutoffUtc: Long): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE isDeleted = 1")
    suspend fun getDeletedAttachmentsDirect(): List<AttachmentEntity>

    @Query("UPDATE attachments SET folderId = NULL WHERE folderId = :folderId")
    suspend fun detachAttachmentsFromFolder(folderId: String)

    @Query("UPDATE attachments SET folderId = :targetFolderId, updatedAtUtc = :updatedAtUtc WHERE id = :attachmentId")
    suspend fun moveAttachmentToFolder(attachmentId: String, targetFolderId: String?, updatedAtUtc: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity): Long

    @Update
    suspend fun updateAttachment(attachment: AttachmentEntity)

    @Delete
    suspend fun deleteAttachment(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteAttachmentById(id: String)

    // Annotations
    @Query("SELECT * FROM annotations WHERE attachmentId = :attachmentId ORDER BY pageNumber ASC, createdAtUtc ASC")
    fun getAnnotationsForAttachment(attachmentId: String): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotations WHERE attachmentId = :attachmentId ORDER BY pageNumber ASC, createdAtUtc ASC")
    suspend fun getAnnotationsForAttachmentDirect(attachmentId: String): List<AnnotationEntity>

    @Query("SELECT * FROM annotations WHERE id = :id LIMIT 1")
    suspend fun getAnnotationByIdDirect(id: String): AnnotationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: AnnotationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotations(annotations: List<AnnotationEntity>)

    @Update
    suspend fun updateAnnotation(annotation: AnnotationEntity)

    @Delete
    suspend fun deleteAnnotation(annotation: AnnotationEntity)

    @Query("DELETE FROM annotations WHERE attachmentId = :attachmentId")
    suspend fun clearAnnotationsForAttachment(attachmentId: String)

    @Transaction
    suspend fun saveAttachmentWithAnnotations(attachment: AttachmentEntity, annotations: List<AnnotationEntity>) {
        if (getAttachmentById(attachment.id) != null) {
            updateAttachment(attachment)
        } else {
            insertAttachment(attachment)
        }
        clearAnnotationsForAttachment(attachment.id)
        insertAnnotations(annotations)
    }

    @Query("SELECT * FROM attachments")
    suspend fun getAllAttachmentsDirect(): List<AttachmentEntity>

    @Query("SELECT * FROM annotations")
    suspend fun getAllAnnotationsDirect(): List<AnnotationEntity>

    @Query("DELETE FROM attachments")
    suspend fun clearAllAttachments()

    @Query("DELETE FROM annotations")
    suspend fun clearAllAnnotations()
}
