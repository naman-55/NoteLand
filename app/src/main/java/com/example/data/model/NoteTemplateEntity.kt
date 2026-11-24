package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "note_templates",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["category", "updatedAtUtc"]),
        Index(value = ["isBuiltIn"])
    ]
)
data class NoteTemplateEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val category: String = "Custom",
    val titlePattern: String = "{{title}}",
    val contentTemplate: String = "",
    val defaultTagsCsv: String = "",
    val defaultColorHex: String = "",
    val checklistTemplateJson: String = "",
    val isBuiltIn: Boolean = false,
    val usageCount: Int = 0,
    val createdAtUtc: Long = System.currentTimeMillis(),
    val updatedAtUtc: Long = System.currentTimeMillis()
)
