package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "canvas_elements",
    foreignKeys = [
        ForeignKey(
            entity = CanvasEntity::class,
            parentColumns = ["id"],
            childColumns = ["canvasId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["canvasId"]),
        Index(value = ["canvasId", "zIndex"])
    ]
)
data class CanvasElementEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val canvasId: String,
    val elementType: String = "STROKE", // STROKE, SHAPE, TEXT, IMAGE
    val elementDataJson: String,
    val zIndex: Int = 0,
    val createdAtUtc: Long = System.currentTimeMillis()
)
