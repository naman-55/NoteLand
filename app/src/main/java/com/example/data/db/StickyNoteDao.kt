package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.StickyNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StickyNoteDao {

    @Query("SELECT * FROM sticky_notes ORDER BY isPinned DESC, orderIndex ASC, updatedAtUtc DESC")
    fun getAllStickyNotes(): Flow<List<StickyNoteEntity>>

    @Query("SELECT * FROM sticky_notes WHERE id = :id LIMIT 1")
    suspend fun getStickyNoteById(id: String): StickyNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStickyNote(stickyNote: StickyNoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStickyNotes(stickyNotes: List<StickyNoteEntity>)

    @Update
    suspend fun updateStickyNote(stickyNote: StickyNoteEntity)

    @Delete
    suspend fun deleteStickyNote(stickyNote: StickyNoteEntity)

    @Query("DELETE FROM sticky_notes WHERE id = :id")
    suspend fun deleteStickyNoteById(id: String)

    @Query("SELECT * FROM sticky_notes")
    suspend fun getAllStickyNotesDirect(): List<StickyNoteEntity>

    @Query("DELETE FROM sticky_notes")
    suspend fun clearAll()
}
