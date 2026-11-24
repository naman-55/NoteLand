package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.NoteLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteLinkDao {
    @Query("SELECT * FROM note_links ORDER BY updatedAtUtc DESC")
    fun getAllLinks(): Flow<List<NoteLinkEntity>>

    @Query("SELECT * FROM note_links ORDER BY updatedAtUtc DESC")
    suspend fun getAllLinksDirect(): List<NoteLinkEntity>

    @Query("SELECT * FROM note_links WHERE sourceNoteId = :sourceNoteId ORDER BY updatedAtUtc DESC")
    fun getOutgoingLinksForNote(sourceNoteId: String): Flow<List<NoteLinkEntity>>

    @Query("SELECT * FROM note_links WHERE sourceNoteId = :sourceNoteId")
    suspend fun getOutgoingLinksDirect(sourceNoteId: String): List<NoteLinkEntity>

    @Query("SELECT * FROM note_links WHERE targetNoteId = :targetNoteId ORDER BY updatedAtUtc DESC")
    fun getBacklinksForNote(targetNoteId: String): Flow<List<NoteLinkEntity>>

    @Query("SELECT * FROM note_links WHERE targetNoteId = :targetNoteId")
    suspend fun getBacklinksDirect(targetNoteId: String): List<NoteLinkEntity>

    @Query("SELECT * FROM note_links WHERE id = :id")
    suspend fun getLinkByIdDirect(id: String): NoteLinkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: NoteLinkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinks(links: List<NoteLinkEntity>)

    @Update
    suspend fun updateLink(link: NoteLinkEntity)

    @Query("DELETE FROM note_links WHERE sourceNoteId = :sourceNoteId")
    suspend fun deleteOutgoingLinks(sourceNoteId: String)

    @Query("UPDATE note_links SET targetNoteId = NULL WHERE targetNoteId = :noteId")
    suspend fun clearIncomingTargetNoteId(noteId: String)

    @Query("DELETE FROM note_links")
    suspend fun clearAll()
}
