package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<FolderEntity>)

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getFolderByIdDirect(id: String): FolderEntity?

    @Query("SELECT * FROM folders WHERE parentId = :parentId ORDER BY name ASC")
    fun getChildFolders(parentId: String): Flow<List<FolderEntity>>

    @Query("UPDATE folders SET parentId = :newParentId, updatedAtUtc = :updatedAtUtc WHERE parentId = :oldParentId")
    suspend fun reparentChildFolders(oldParentId: String, newParentId: String?, updatedAtUtc: Long = System.currentTimeMillis())

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolder(id: String)

    @Query("SELECT * FROM folders")
    suspend fun getAllFoldersDirect(): List<FolderEntity>

    @Query("DELETE FROM folders")
    suspend fun clearAll()
}
