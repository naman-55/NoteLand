package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "canvases")
data class CanvasEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled Canvas",
    val backgroundColorHex: String = "#FAF5FF",
    val width: Float = 1920f,
    val height: Float = 1080f,
    val createdAtUtc: Long = System.currentTimeMillis(),
    val updatedAtUtc: Long = System.currentTimeMillis()
)
