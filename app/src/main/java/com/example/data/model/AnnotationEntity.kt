package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "annotations",
    foreignKeys = [
        ForeignKey(
            entity = AttachmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["attachmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["attachmentId"]),
        Index(value = ["attachmentId", "pageNumber"])
    ]
)
data class AnnotationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val attachmentId: String,
    val annotationType: String = "HIGHLIGHT", // HIGHLIGHT, DRAWING, STAMP, TEXT_NOTE
    val pageNumber: Int = 1,
    val coordinatesJson: String, // rect bounds or path coordinates
    val content: String = "",
    val colorHex: String = "#FDE047",
    val createdAtUtc: Long = System.currentTimeMillis()
)
