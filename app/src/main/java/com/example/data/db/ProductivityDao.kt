package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FocusSessionEntity
import com.example.data.model.HabitEntity
import com.example.data.model.HabitLogEntity
import com.example.data.model.KanbanColumnEntity
import com.example.data.model.TaskDependencyEntity
import com.example.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductivityDao {

    // ---- Kanban Columns ----
    @Query("SELECT * FROM kanban_columns ORDER BY position ASC, createdAtUtc ASC")
    fun getAllKanbanColumns(): Flow<List<KanbanColumnEntity>>

    @Query("SELECT * FROM kanban_columns ORDER BY position ASC, createdAtUtc ASC")
    suspend fun getAllKanbanColumnsDirect(): List<KanbanColumnEntity>

    @Query("SELECT * FROM kanban_columns WHERE id = :id LIMIT 1")
    suspend fun getKanbanColumnByIdDirect(id: String): KanbanColumnEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertKanbanColumnsIgnore(columns: List<KanbanColumnEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKanbanColumn(column: KanbanColumnEntity): Long

    @Update
    suspend fun updateKanbanColumn(column: KanbanColumnEntity)

    @Query("DELETE FROM kanban_columns WHERE id = :id")
    suspend fun deleteKanbanColumnById(id: String)

    @Query("DELETE FROM kanban_columns")
    suspend fun clearAllKanbanColumns()

    // ---- Tasks ----
    @Query("SELECT * FROM tasks WHERE isArchived = 0 ORDER BY isCompleted ASC, orderInColumn ASC, updatedAtUtc DESC")
    fun getActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY updatedAtUtc DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY orderInColumn ASC, updatedAtUtc DESC")
    suspend fun getAllTasksDirect(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskByIdDirect(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE linkedReminderId = :reminderId LIMIT 1")
    suspend fun getTaskByLinkedReminderIdDirect(reminderId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE linkedNoteId = :noteId")
    suspend fun getTasksByLinkedNoteIdDirect(noteId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE columnId = :columnId AND isArchived = 0 ORDER BY orderInColumn ASC")
    suspend fun getTasksInColumnDirect(columnId: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTasksIgnore(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET columnId = :targetColumnId, updatedAtUtc = :updatedAtUtc WHERE columnId = :sourceColumnId")
    suspend fun reassignTasksToColumn(sourceColumnId: String, targetColumnId: String, updatedAtUtc: Long)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("UPDATE tasks SET linkedNoteId = NULL WHERE linkedNoteId = :noteId")
    suspend fun clearLinkedNoteId(noteId: String)

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()

    // ---- Task Dependencies ----
    @Query("SELECT * FROM task_dependencies")
    fun getAllTaskDependencies(): Flow<List<TaskDependencyEntity>>

    @Query("SELECT * FROM task_dependencies")
    suspend fun getAllTaskDependenciesDirect(): List<TaskDependencyEntity>

    @Query("SELECT * FROM task_dependencies WHERE taskId = :taskId")
    suspend fun getDependenciesForTaskDirect(taskId: String): List<TaskDependencyEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskDependency(dependency: TaskDependencyEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskDependenciesIgnore(dependencies: List<TaskDependencyEntity>)

    @Query("DELETE FROM task_dependencies WHERE taskId = :taskId AND dependsOnTaskId = :dependsOnTaskId")
    suspend fun deleteTaskDependency(taskId: String, dependsOnTaskId: String)

    @Query("DELETE FROM task_dependencies")
    suspend fun clearAllTaskDependencies()

    // ---- Habits & Habit Logs ----
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAtUtc ASC")
    fun getActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY createdAtUtc ASC")
    suspend fun getAllHabitsDirect(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitByIdDirect(id: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: String)

    @Query("DELETE FROM habits")
    suspend fun clearAllHabits()

    @Query("SELECT * FROM habit_logs ORDER BY localDateKey DESC")
    fun getAllHabitLogs(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs ORDER BY localDateKey DESC")
    suspend fun getAllHabitLogsDirect(): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND localDateKey = :localDateKey LIMIT 1")
    suspend fun getHabitLogForDateDirect(habitId: String, localDateKey: String): HabitLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitLog(log: HabitLogEntity): Long

    @Update
    suspend fun updateHabitLog(log: HabitLogEntity)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND localDateKey = :localDateKey")
    suspend fun deleteHabitLogForDate(habitId: String, localDateKey: String)

    @Query("DELETE FROM habit_logs")
    suspend fun clearAllHabitLogs()

    // ---- Focus Sessions ----
    @Query("SELECT * FROM focus_sessions ORDER BY startedAtUtc DESC")
    fun getAllFocusSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY startedAtUtc DESC")
    suspend fun getAllFocusSessionsDirect(): List<FocusSessionEntity>

    @Query("SELECT * FROM focus_sessions WHERE id = :id LIMIT 1")
    suspend fun getFocusSessionByIdDirect(id: String): FocusSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSession(session: FocusSessionEntity): Long

    @Update
    suspend fun updateFocusSession(session: FocusSessionEntity)

    @Query("DELETE FROM focus_sessions WHERE id = :id")
    suspend fun deleteFocusSessionById(id: String)

    @Query("UPDATE focus_sessions SET linkedNoteId = NULL WHERE linkedNoteId = :noteId")
    suspend fun clearFocusSessionLinkedNoteId(noteId: String)

    @Query("DELETE FROM focus_sessions")
    suspend fun clearAllFocusSessions()
}
