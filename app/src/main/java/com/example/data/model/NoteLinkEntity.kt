package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "note_links",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceNoteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetNoteId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["sourceNoteId"]),
        Index(value = ["targetNoteId"]),
        Index(value = ["normalizedTargetTitle"]),
        Index(value = ["sourceNoteId", "normalizedTargetTitle"], unique = true)
    ]
)
data class NoteLinkEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sourceNoteId: String,
    val targetNoteId: String? = null,
    val targetTitle: String,
    val normalizedTargetTitle: String,
    val anchorHeading: String = "",
    val displayAlias: String = "",
    val contextSnippet: String = "",
    val createdAtUtc: Long = System.currentTimeMillis(),
    val updatedAtUtc: Long = System.currentTimeMillis()
)
