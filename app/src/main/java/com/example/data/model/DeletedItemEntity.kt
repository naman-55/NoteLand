package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "deleted_items",
    indices = [
        Index(value = ["originalTable"]),
        Index(value = ["deletedAtUtc"])
    ]
)
data class DeletedItemEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val originalTable: String, // notes, sticky_notes, canvases, folders
    val originalId: String,
    val itemSnapshotJson: String,
    val deletedAtUtc: Long = System.currentTimeMillis()
)
