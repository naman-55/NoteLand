package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.NoteVersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteVersionDao {
    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY versionNumber DESC, createdAtUtc DESC")
    fun getVersionsForNote(noteId: String): Flow<List<NoteVersionEntity>>

    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY versionNumber DESC, createdAtUtc DESC")
    suspend fun getVersionsForNoteDirect(noteId: String): List<NoteVersionEntity>

    @Query("SELECT * FROM note_versions ORDER BY createdAtUtc DESC")
    suspend fun getAllVersionsDirect(): List<NoteVersionEntity>

    @Query("SELECT * FROM note_versions WHERE id = :id")
    suspend fun getVersionByIdDirect(id: String): NoteVersionEntity?

    @Query("SELECT COALESCE(MAX(versionNumber), 0) FROM note_versions WHERE noteId = :noteId")
    suspend fun getLatestVersionNumber(noteId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: NoteVersionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersions(versions: List<NoteVersionEntity>)

    @Update
    suspend fun updateVersion(version: NoteVersionEntity)

    @Query("DELETE FROM note_versions WHERE noteId = :noteId")
    suspend fun deleteVersionsForNote(noteId: String)

    @Query(
        "DELETE FROM note_versions WHERE noteId = :noteId AND id NOT IN (" +
            "SELECT id FROM note_versions WHERE noteId = :noteId ORDER BY versionNumber DESC, createdAtUtc DESC LIMIT :keepCount" +
        ")"
    )
    suspend fun pruneOldVersions(noteId: String, keepCount: Int = 25)

    @Query("DELETE FROM note_versions")
    suspend fun clearAll()
}
