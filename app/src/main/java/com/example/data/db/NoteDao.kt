package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.NoteEntity
import com.example.data.model.NoteTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 0 AND isHidden = 0 ORDER BY isPinned DESC, updatedAtUtc DESC")
    fun getAllActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 1 ORDER BY updatedAtUtc DESC")
    fun getArchivedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedAtUtc DESC")
    fun getDeletedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 0 AND isHidden = 0 AND isFavorite = 1 ORDER BY updatedAtUtc DESC")
    fun getFavoriteNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 0 AND isLocked = 1 ORDER BY updatedAtUtc DESC")
    fun getLockedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 0 AND isHidden = 1 ORDER BY updatedAtUtc DESC")
    fun getHiddenNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 0 AND isHidden = 0 AND noteType = :type ORDER BY updatedAtUtc DESC")
    fun getNotesByType(type: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 0 AND isHidden = 0 AND folderId = :folderId ORDER BY updatedAtUtc DESC")
    fun getNotesByFolder(folderId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getNoteById(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteByIdDirect(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 0 AND isHidden = 0 AND isLocked = 0 AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tagsCsv LIKE '%' || :query || '%') ORDER BY updatedAtUtc DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND reminderTimestamp IS NOT NULL ORDER BY reminderTimestamp ASC")
    fun getNotesWithReminders(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deletePermanently(id: String)

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun emptyRecycleBin()

    @Query("UPDATE notes SET folderId = NULL, folderName = '' WHERE folderId = :folderId")
    suspend fun detachNotesFromFolder(folderId: String)

    @Query("UPDATE notes SET folderName = :newFolderName WHERE folderId = :folderId")
    suspend fun updateNotesFolderName(folderId: String, newFolderName: String)

    @Query("UPDATE notes SET folderId = :targetFolderId, folderName = :targetFolderName, updatedAtUtc = :updatedAtUtc WHERE id = :noteId")
    suspend fun moveNoteToFolder(noteId: String, targetFolderId: String?, targetFolderName: String, updatedAtUtc: Long = System.currentTimeMillis())

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesDirect(): List<NoteEntity>

    @Query("DELETE FROM notes")
    suspend fun clearAllNotes()

    // NoteTags operations
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTag(noteTag: NoteTagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTags(noteTags: List<NoteTagEntity>)

    @Query("SELECT * FROM note_tags")
    suspend fun getAllNoteTagsDirect(): List<NoteTagEntity>

    @Query("DELETE FROM note_tags")
    suspend fun clearAllNoteTags()

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun removeAllTagsForNote(noteId: String)

    @Query("SELECT tagId FROM note_tags WHERE noteId = :noteId")
    suspend fun getTagIdsForNote(noteId: String): List<String>

    @Transaction
    suspend fun updateNoteWithTags(note: NoteEntity, tagIds: List<String>) {
        updateNote(note)
        removeAllTagsForNote(note.id)
        tagIds.forEach { tagId ->
            insertNoteTag(NoteTagEntity(noteId = note.id, tagId = tagId))
        }
    }
}
