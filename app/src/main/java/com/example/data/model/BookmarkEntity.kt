package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "bookmarks",
    indices = [
        Index(value = ["targetType", "targetId"])
    ]
)
data class BookmarkEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val targetType: String = "NOTE", // NOTE, ATTACHMENT, CANVAS
    val targetId: String,
    val label: String,
    val createdAtUtc: Long = System.currentTimeMillis()
)
