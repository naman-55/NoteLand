package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AppSettingEntity
import com.example.data.model.DeletedItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {

    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    fun observeSetting(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSettingEntity)

    @Query("DELETE FROM app_settings WHERE `key` = :key")
    suspend fun removeSetting(key: String)
}

@Dao
interface DeletedItemDao {

    @Query("SELECT * FROM deleted_items ORDER BY deletedAtUtc DESC")
    fun getAllDeletedItems(): Flow<List<DeletedItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedItem(item: DeletedItemEntity): Long

    @Query("DELETE FROM deleted_items WHERE id = :id")
    suspend fun purgeDeletedItem(id: String)

    @Query("DELETE FROM deleted_items")
    suspend fun purgeAllDeletedItems()
}
