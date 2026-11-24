package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.TimeZone
import java.util.UUID

enum class TaskPriority(val level: Int, val label: String, val colorHex: String) {
    LOW(1, "Low", "#10B981"),
    MEDIUM(2, "Medium", "#3B82F6"),
    HIGH(3, "High", "#F59E0B"),
    URGENT(4, "Urgent", "#EF4444");

    companion object {
        fun fromName(name: String?): TaskPriority {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: MEDIUM
        }
    }
}

enum class TaskRecurrenceRule(val label: String) {
    NONE("Once"),
    DAILY("Daily"),
    WEEKDAYS("Weekdays"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    companion object {
        fun fromName(name: String?): TaskRecurrenceRule {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NONE
        }
    }
}

@Entity(
    tableName = "kanban_columns",
    indices = [
        Index(value = ["boardId", "position"], name = "index_kanban_columns_boardId_position")
    ]
)
data class KanbanColumnEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val boardId: String = "default",
    val title: String,
    val colorHex: String = "#6366F1",
    val position: Int = 0,
    val wipLimit: Int = 0,
    val isDoneColumn: Boolean = false,
    val createdAtUtc: Long = System.currentTimeMillis(),
    val updatedAtUtc: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedNoteId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ReminderEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedReminderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["columnId"], name = "index_tasks_columnId"),
        Index(value = ["columnId", "orderInColumn"], name = "index_tasks_columnId_orderInColumn"),
        Index(value = ["dueAtUtc"], name = "index_tasks_dueAtUtc"),
        Index(value = ["scheduledStartUtc"], name = "index_tasks_scheduledStartUtc"),
        Index(value = ["isCompleted"], name = "index_tasks_isCompleted"),
        Index(value = ["linkedNoteId"], name = "index_tasks_linkedNoteId"),
        Index(value = ["linkedReminderId"], unique = true, name = "index_tasks_linkedReminderId")
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val columnId: String = "col_todo",
    val orderInColumn: Int = 0,
    val priority: String = TaskPriority.MEDIUM.name,
    val isCompleted: Boolean = false,
    val completedAtUtc: Long? = null,
    val dueAtUtc: Long? = null,
    val dueTimeZoneId: String = TimeZone.getDefault().id,
    val scheduledStartUtc: Long? = null,
    val scheduledEndUtc: Long? = null,
    val estimatedMinutes: Int = 30,
    val recurrenceRule: String = TaskRecurrenceRule.NONE.name,
    val recurrenceInterval: Int = 1,
    val recurrenceDaysCsv: String = "",
    val recurrenceParentId: String? = null,
    val subtasksJson: String = "",
    val linkedNoteId: String? = null,
    val linkedReminderId: String? = null,
    val category: String = "Work",
    val colorHex: String = "#3B82F6",
    val tagsCsv: String = "",
    val isArchived: Boolean = false,
    val createdAtUtc: Long = System.currentTimeMillis(),
    val updatedAtUtc: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "task_dependencies",
    primaryKeys = ["taskId", "dependsOnTaskId"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["dependsOnTaskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["taskId"], name = "index_task_dependencies_taskId"),
        Index(value = ["dependsOnTaskId"], name = "index_task_dependencies_dependsOnTaskId")
    ]
)
data class TaskDependencyEntity(
    val taskId: String,
    val dependsOnTaskId: String,
    val createdAtUtc: Long = System.currentTimeMillis()
)
