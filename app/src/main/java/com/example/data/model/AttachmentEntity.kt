package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["noteId"]),
        Index(value = ["mimeType"]),
        Index(value = ["folderId"]),
        Index(value = ["contentHash"]),
        Index(value = ["isDeleted"]),
        Index(value = ["isLocked"])
    ]
)
data class AttachmentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val noteId: String? = null,
    val canvasId: String? = null,
    val fileName: String,
    val fileExtension: String,
    val relativePath: String, // Path relative to context.filesDir
    val mimeType: String,
    val sizeBytes: Long,
    val createdAtUtc: Long = System.currentTimeMillis(),
    val folderId: String? = null,
    val contentHash: String = "",
    val pageCount: Int = 0,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isLocked: Boolean = false,
    val isHidden: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAtUtc: Long? = null,
    val updatedAtUtc: Long = createdAtUtc
)
