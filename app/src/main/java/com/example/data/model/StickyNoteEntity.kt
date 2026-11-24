package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sticky_notes",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["noteId"]),
        Index(value = ["isPinned", "orderIndex"])
    ]
)
data class StickyNoteEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val noteId: String? = null,
    val boardName: String = "Main Wall",
    val title: String = "",
    val content: String = "",
    val colorHex: String = "#FEF3C7",
    val orderIndex: Int = 0,
    val isPinned: Boolean = false,
    val createdAtUtc: Long = System.currentTimeMillis(),
    val updatedAtUtc: Long = System.currentTimeMillis()
)
