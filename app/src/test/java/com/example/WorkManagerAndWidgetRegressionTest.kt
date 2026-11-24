package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.impl.WorkManagerImpl
import com.example.data.db.AppDatabase
import com.example.data.model.TaskEntity
import com.example.widget.WidgetDataLoader
import com.example.worker.ProductivityWorkScheduler
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WorkManagerAndWidgetRegressionTest {

    @Test
    fun `WorkManager initializes and schedules productivity workers cleanly with Room 2_7 on SDK 36`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration.Builder()
            .setExecutor(Executors.newSingleThreadExecutor())
            .setTaskExecutor(Executors.newSingleThreadExecutor())
            .build()

        // Initialize WorkManager synchronously if not yet initialized
        if (!WorkManager.isInitialized()) {
            WorkManager.initialize(context, config)
        }

        val wm = WorkManager.getInstance(context)
        assertNotNull(wm)

        // Trigger WorkDatabase access and unique work scheduling (which runs Room queries on WorkDatabase_Impl)
        ProductivityWorkScheduler.ensurePeriodicMaintenanceScheduled(context)
        ProductivityWorkScheduler.schedulePomodoroCompletion(context, 60_000L, "Focus Test")

        // Synchronously query WorkManager's Room WorkDatabase to verify full Room 2.7 compatibility
        val workInfos = wm.getWorkInfosForUniqueWork("noteland_productivity_maintenance").get()
        assertTrue("Expected scheduled periodic maintenance work info", workInfos.isNotEmpty())

        ProductivityWorkScheduler.cancelPomodoroCompletion(context)
    }

    @Test
    fun `DailyTasksWidget snapshot includes standalone Kanban tasks and supports toggling completion`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        AppDatabase.seedInitialDataDirect(db)

        val taskId = "widget_kanban_task_${UUID.randomUUID()}"
        val task = TaskEntity(
            id = taskId,
            title = "Standalone Kanban Task",
            columnId = "col_todo",
            isCompleted = false
        )
        db.productivityDao().insertTask(task)

        try {
            val beforeSnapshot = WidgetDataLoader.loadDailyTasksSnapshot(context)
            val widgetItem = beforeSnapshot.items.firstOrNull { it.id == "task_$taskId" }
            assertNotNull("Standalone Kanban task should appear in DailyTasksWidget snapshot", widgetItem)
            assertEquals(false, widgetItem!!.isDone)

            // Toggle completion directly via WidgetDataLoader
            WidgetDataLoader.toggleKanbanTask(context, taskId)

            val updatedTask = db.productivityDao().getTaskByIdDirect(taskId)
            assertNotNull(updatedTask)
            assertTrue("Toggling standalone Kanban task from widget should mark it completed", updatedTask!!.isCompleted)
        } finally {
            db.productivityDao().deleteTaskById(taskId)
        }
    }
}
