package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.TimeZone
import java.util.UUID

enum class FocusSessionType(val label: String, val defaultDurationSec: Int) {
    WORK("Focus", 25 * 60),
    SHORT_BREAK("Short Break", 5 * 60),
    LONG_BREAK("Long Break", 15 * 60);

    companion object {
        fun fromName(name: String?): FocusSessionType {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: WORK
        }
    }
}

enum class FocusSessionStatus {
    RUNNING,
    PAUSED,
    COMPLETED,
    CANCELLED;

    companion object {
        fun fromName(name: String?): FocusSessionStatus {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: COMPLETED
        }
    }
}

@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedTaskId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedNoteId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["linkedTaskId"], name = "index_focus_sessions_linkedTaskId"),
        Index(value = ["linkedNoteId"], name = "index_focus_sessions_linkedNoteId"),
        Index(value = ["startedAtUtc"], name = "index_focus_sessions_startedAtUtc"),
        Index(value = ["localDateKey"], name = "index_focus_sessions_localDateKey"),
        Index(value = ["status"], name = "index_focus_sessions_status")
    ]
)
data class FocusSessionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionType: String = FocusSessionType.WORK.name,
    val linkedTaskId: String? = null,
    val linkedNoteId: String? = null,
    val plannedDurationSec: Int = FocusSessionType.WORK.defaultDurationSec,
    val actualFocusedSec: Int = 0,
    val status: String = FocusSessionStatus.COMPLETED.name,
    val startedAtUtc: Long = System.currentTimeMillis(),
    val endedAtUtc: Long? = null,
    val localDateKey: String = "",
    val timeZoneId: String = TimeZone.getDefault().id
)
