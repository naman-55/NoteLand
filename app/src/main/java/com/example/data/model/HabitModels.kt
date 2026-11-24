package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.TimeZone
import java.util.UUID

enum class HabitFrequency(val label: String) {
    DAILY("Every day"),
    WEEKDAYS("Weekdays"),
    WEEKLY_TARGET("Weekly goal");

    companion object {
        fun fromName(name: String?): HabitFrequency {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DAILY
        }
    }
}

@Entity(
    tableName = "habits",
    indices = [
        Index(value = ["isArchived"], name = "index_habits_isArchived"),
        Index(value = ["createdAtUtc"], name = "index_habits_createdAtUtc")
    ]
)
data class HabitEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val category: String = "Health",
    val colorHex: String = "#10B981",
    val iconName: String = "check_circle",
    val frequencyType: String = HabitFrequency.DAILY.name,
    val targetPerDay: Int = 1,
    val targetDaysPerWeek: Int = 7,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val timeZoneId: String = TimeZone.getDefault().id,
    val isArchived: Boolean = false,
    val createdAtUtc: Long = System.currentTimeMillis(),
    val updatedAtUtc: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId"], name = "index_habit_logs_habitId"),
        Index(value = ["localDateKey"], name = "index_habit_logs_localDateKey"),
        Index(value = ["habitId", "localDateKey"], unique = true, name = "index_habit_logs_habitId_localDateKey")
    ]
)
data class HabitLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val habitId: String,
    val localDateKey: String,
    val completedCount: Int = 1,
    val targetCountSnapshot: Int = 1,
    val isCompleted: Boolean = true,
    val note: String = "",
    val timeZoneId: String = TimeZone.getDefault().id,
    val completedAtUtc: Long = System.currentTimeMillis()
)
