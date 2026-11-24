package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "note_versions",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["noteId", "createdAtUtc"]),
        Index(value = ["noteId", "versionNumber"])
    ]
)
data class NoteVersionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val noteId: String,
    val versionNumber: Int,
    val title: String,
    val content: String,
    val tagsCsv: String = "",
    val checklistJson: String = "",
    val changeSummary: String = "",
    val wordCount: Int = 0,
    val createdAtUtc: Long = System.currentTimeMillis()
)
