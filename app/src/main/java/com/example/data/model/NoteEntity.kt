package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class NoteType {
    STANDARD,
    STICKY,
    CANVAS,
    PDF_DOC
}

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["isPinned", "updatedAtUtc"]),
        Index(value = ["isArchived"]),
        Index(value = ["isDeleted"]),
        Index(value = ["isLocked"]),
        Index(value = ["isHidden"]),
        Index(value = ["noteType"])
    ]
)
data class NoteEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val folderId: String? = null,
    val folderName: String = "",
    val title: String = "",
    val content: String = "",
    val noteType: String = NoteType.STANDARD.name,
    val colorHex: String = "",
    val tagsCsv: String = "",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val isLocked: Boolean = false,
    val isHidden: Boolean = false,
    val isFavorite: Boolean = false,
    val reminderTimestamp: Long? = null,
    val canvasDataJson: String = "",
    val checklistJson: String = "",
    val imageUri: String = "",
    val createdAtUtc: Long = System.currentTimeMillis(),
    val updatedAtUtc: Long = System.currentTimeMillis(),
    val deletedAtUtc: Long? = null
) {
    // Compatibility accessors for UI
    val createdAt: Long get() = createdAtUtc
    val updatedAt: Long get() = updatedAtUtc
    val deletedAt: Long? get() = deletedAtUtc
}
