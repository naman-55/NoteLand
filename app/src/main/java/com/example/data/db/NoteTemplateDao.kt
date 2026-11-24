package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.NoteTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteTemplateDao {
    @Query("SELECT * FROM note_templates ORDER BY isBuiltIn DESC, usageCount DESC, updatedAtUtc DESC")
    fun getAllTemplates(): Flow<List<NoteTemplateEntity>>

    @Query("SELECT * FROM note_templates ORDER BY isBuiltIn DESC, usageCount DESC, updatedAtUtc DESC")
    suspend fun getAllTemplatesDirect(): List<NoteTemplateEntity>

    @Query("SELECT * FROM note_templates WHERE id = :id")
    suspend fun getTemplateByIdDirect(id: String): NoteTemplateEntity?

    @Query("SELECT * FROM note_templates WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getTemplateByNameDirect(name: String): NoteTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTemplate(template: NoteTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTemplates(templates: List<NoteTemplateEntity>)

    @Update
    suspend fun updateTemplate(template: NoteTemplateEntity)

    @Query("UPDATE note_templates SET usageCount = usageCount + 1, updatedAtUtc = :updatedAtUtc WHERE id = :templateId")
    suspend fun incrementUsageCount(templateId: String, updatedAtUtc: Long = System.currentTimeMillis())

    @Query("DELETE FROM note_templates WHERE id = :id")
    suspend fun deleteTemplate(id: String)

    @Query("DELETE FROM note_templates")
    suspend fun clearAll()
}
