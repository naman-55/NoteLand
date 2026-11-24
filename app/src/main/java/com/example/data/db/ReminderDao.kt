package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BookmarkEntity
import com.example.data.model.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY scheduledTimeUtc ASC")
    fun getPendingReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND scheduledTimeUtc > :currentTimeUtc ORDER BY scheduledTimeUtc ASC")
    suspend fun getUpcomingRemindersDirect(currentTimeUtc: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, scheduledTimeUtc ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE noteId = :noteId ORDER BY scheduledTimeUtc ASC")
    fun getRemindersForNote(noteId: String): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: String)

    @Query("SELECT * FROM reminders WHERE noteId = :noteId")
    suspend fun getRemindersForNoteDirect(noteId: String): List<ReminderEntity>

    @Query("DELETE FROM reminders WHERE noteId = :noteId")
    suspend fun deleteRemindersForNote(noteId: String)

    @Query("SELECT * FROM reminders")
    suspend fun getAllRemindersDirect(): List<ReminderEntity>

    @Query("DELETE FROM reminders")
    suspend fun clearAll()
}

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks ORDER BY createdAtUtc DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE id = :id LIMIT 1")
    suspend fun getBookmarkByIdDirect(id: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Update
    suspend fun updateBookmark(bookmark: BookmarkEntity)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE targetType = :targetType AND targetId = :targetId")
    suspend fun removeBookmark(targetType: String, targetId: String)

    @Query("DELETE FROM bookmarks WHERE targetType = 'PDF_PAGE' AND targetId LIKE :attachmentIdPrefix || '#%'")
    suspend fun removePdfBookmarksForAttachment(attachmentIdPrefix: String)

    @Query("SELECT * FROM bookmarks")
    suspend fun getAllBookmarksDirect(): List<BookmarkEntity>

    @Query("DELETE FROM bookmarks")
    suspend fun clearAll()
}
