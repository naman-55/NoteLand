package com.example.worker

import android.content.Context
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.db.AppDatabase
import com.example.data.model.FocusSessionEntity
import com.example.data.model.FocusSessionStatus
import com.example.data.model.FocusSessionType
import com.example.util.ProductivityEngine
import com.example.util.ReminderNotificationManager
import com.example.widget.WidgetUpdateHelper
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class PomodoroCompletionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PREFS_POMODORO, Context.MODE_PRIVATE)
        val rawState = prefs.getString(KEY_POMODORO_STATE, null)
        val state = ProductivityEngine.deserializePomodoroState(rawState)

        if (!state.isActiveOrPaused || state.status != FocusSessionStatus.RUNNING || state.activeSessionId.isNullOrBlank()) {
            return Result.success()
        }

        val now = System.currentTimeMillis()
        val remaining = ProductivityEngine.computeRemainingSeconds(state, now)
        if (remaining > 1) {
            // Re-schedule if system woke early
            ProductivityWorkScheduler.schedulePomodoroCompletion(
                context = applicationContext,
                delayMs = remaining * 1000L,
                sessionTitle = inputData.getString(KEY_SESSION_TITLE) ?: state.sessionType.label
            )
            return Result.success()
        }

        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.productivityDao()
        val existing = dao.getFocusSessionByIdDirect(state.activeSessionId)
        val tzId = TimeZone.getDefault().id
        val completedEntity = if (existing != null) {
            existing.copy(
                actualFocusedSec = state.plannedDurationSec,
                status = FocusSessionStatus.COMPLETED.name,
                endedAtUtc = now
            )
        } else {
            FocusSessionEntity(
                id = state.activeSessionId,
                sessionType = state.sessionType.name,
                linkedTaskId = state.linkedTaskId,
                linkedNoteId = state.linkedNoteId,
                plannedDurationSec = state.plannedDurationSec,
                actualFocusedSec = state.plannedDurationSec,
                status = FocusSessionStatus.COMPLETED.name,
                startedAtUtc = (now - state.plannedDurationSec * 1000L).coerceAtLeast(0L),
                endedAtUtc = now,
                localDateKey = ProductivityEngine.localDateKeyFromMillis(now, tzId),
                timeZoneId = tzId
            )
        }
        dao.insertFocusSession(completedEntity)

        val nextCycles = if (state.sessionType == FocusSessionType.WORK) {
            state.completedWorkCycles + 1
        } else {
            state.completedWorkCycles
        }
        val clearedState = state.copy(
            activeSessionId = null,
            status = FocusSessionStatus.COMPLETED,
            isActiveOrPaused = false,
            accumulatedElapsedMs = 0L,
            lastResumedWallClockMs = 0L,
            completedWorkCycles = nextCycles
        )
        prefs.edit {
            putString(KEY_POMODORO_STATE, ProductivityEngine.serializePomodoroState(clearedState))
        }

        val title = inputData.getString(KEY_SESSION_TITLE) ?: "${state.sessionType.label} Complete"
        val body = if (state.sessionType == FocusSessionType.WORK) {
            "Great work! ${state.plannedDurationSec / 60}m focus session logged."
        } else {
            "Break finished. Ready to focus again?"
        }
        ReminderNotificationManager.showNotification(
            context = applicationContext,
            reminderId = "pomodoro_${completedEntity.id}",
            noteId = state.linkedNoteId ?: "",
            noteTitle = title,
            content = body,
            targetRoute = com.example.ui.navigation.Screen.Productivity.createRoute(2)
        )
        return Result.success()
    }

    companion object {
        const val PREFS_POMODORO = "noteland_pomodoro_prefs"
        const val KEY_POMODORO_STATE = "pomodoro_runtime_state"
        const val KEY_SESSION_TITLE = "session_title"
    }
}

class ProductivityMaintenanceWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            AppDatabase.seedInitialDataDirect(db)
            val prefs = applicationContext.getSharedPreferences("noteland_settings_prefs", Context.MODE_PRIVATE)
            val retentionDays = prefs.getInt(com.example.ui.viewmodel.NoteViewModel.KEY_TRASH_RETENTION_DAYS, 30)
            if (retentionDays > 0) {
                com.example.data.repository.NoteRepository(db, applicationContext)
                    .purgeExpiredTrashItems(retentionDays)
            }
            WidgetUpdateHelper.updateAllWidgets(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

object ProductivityWorkScheduler {
    private const val WORK_POMODORO_COMPLETION = "noteland_pomodoro_completion"
    private const val WORK_PRODUCTIVITY_MAINTENANCE = "noteland_productivity_maintenance"

    fun schedulePomodoroCompletion(
        context: Context,
        delayMs: Long,
        sessionTitle: String
    ) {
        if (delayMs <= 0L) return
        runCatching {
            val request = OneTimeWorkRequestBuilder<PomodoroCompletionWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(PomodoroCompletionWorker.KEY_SESSION_TITLE to sessionTitle))
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                WORK_POMODORO_COMPLETION,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    fun cancelPomodoroCompletion(context: Context) {
        runCatching {
            WorkManager.getInstance(context.applicationContext)
                .cancelUniqueWork(WORK_POMODORO_COMPLETION)
        }
    }

    fun ensurePeriodicMaintenanceScheduled(context: Context) {
        runCatching {
            val request = PeriodicWorkRequestBuilder<ProductivityMaintenanceWorker>(
                12,
                TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK_PRODUCTIVITY_MAINTENANCE,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
