package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.CanvasElementEntity
import com.example.data.model.CanvasEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CanvasDao {

    @Query("SELECT * FROM canvases ORDER BY updatedAtUtc DESC")
    fun getAllCanvases(): Flow<List<CanvasEntity>>

    @Query("SELECT * FROM canvases WHERE id = :id LIMIT 1")
    fun getCanvasById(id: String): Flow<CanvasEntity?>

    @Query("SELECT * FROM canvases WHERE id = :id LIMIT 1")
    suspend fun getCanvasByIdDirect(id: String): CanvasEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCanvas(canvas: CanvasEntity): Long

    @Update
    suspend fun updateCanvas(canvas: CanvasEntity)

    @Delete
    suspend fun deleteCanvas(canvas: CanvasEntity)

    @Query("DELETE FROM canvases WHERE id = :id")
    suspend fun deleteCanvasById(id: String)

    // Canvas elements
    @Query("SELECT * FROM canvas_elements WHERE canvasId = :canvasId ORDER BY zIndex ASC, createdAtUtc ASC")
    fun getElementsForCanvas(canvasId: String): Flow<List<CanvasElementEntity>>

    @Query("SELECT * FROM canvas_elements WHERE canvasId = :canvasId ORDER BY zIndex ASC, createdAtUtc ASC")
    suspend fun getElementsForCanvasDirect(canvasId: String): List<CanvasElementEntity>

    @Query("SELECT * FROM canvas_elements WHERE id = :id LIMIT 1")
    suspend fun getElementByIdDirect(id: String): CanvasElementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertElement(element: CanvasElementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertElements(elements: List<CanvasElementEntity>)

    @Update
    suspend fun updateElement(element: CanvasElementEntity)

    @Query("DELETE FROM canvas_elements WHERE canvasId = :canvasId")
    suspend fun clearElementsForCanvas(canvasId: String)

    @Transaction
    suspend fun saveCanvasWithElements(canvas: CanvasEntity, elements: List<CanvasElementEntity>) {
        if (getCanvasByIdDirect(canvas.id) != null) {
            updateCanvas(canvas)
        } else {
            insertCanvas(canvas)
        }
        clearElementsForCanvas(canvas.id)
        insertElements(elements)
    }

    @Query("SELECT * FROM canvases")
    suspend fun getAllCanvasesDirect(): List<CanvasEntity>

    @Query("SELECT * FROM canvas_elements")
    suspend fun getAllElementsDirect(): List<CanvasElementEntity>

    @Query("DELETE FROM canvases")
    suspend fun clearAllCanvases()

    @Query("DELETE FROM canvas_elements")
    suspend fun clearAllElements()
}
