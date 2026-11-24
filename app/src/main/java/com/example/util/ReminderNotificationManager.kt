package com.example.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.ReminderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ReminderNotificationManager {

    const val CHANNEL_ID = "noteland_reminders"
    const val CHANNEL_NAME = "NoteLand Reminders & Alerts"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Timely alerts for personal notes, deadlines, and study reminders."
                enableVibration(true)
                enableLights(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(
        context: Context,
        reminder: ReminderEntity,
        noteTitle: String
    ) {
        createNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.noteland.ACTION_REMINDER"
            putExtra("EXTRA_REMINDER_ID", reminder.id)
            putExtra("EXTRA_NOTE_ID", reminder.noteId)
            putExtra("EXTRA_NOTE_TITLE", noteTitle)
            putExtra("EXTRA_REPEAT", reminder.repeatInterval)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = reminder.scheduledTimeUtc
        if (triggerAtMillis <= System.currentTimeMillis()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            // Android battery optimization or permission limitations: fallback to inexact alarm gracefully
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelReminder(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.noteland.ACTION_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun showNotification(
        context: Context,
        reminderId: String,
        noteId: String,
        noteTitle: String,
        content: String = "Reminder from your NoteLand sanctuary.",
        targetRoute: String? = null
    ) {
        createNotificationChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (noteId.isNotBlank()) {
                putExtra("NAVIGATE_TO_NOTE_ID", noteId)
            } else if (!targetRoute.isNullOrBlank()) {
                putExtra(com.example.widget.WidgetNavigation.EXTRA_WIDGET_ROUTE, targetRoute)
            }
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget_alarm)
            .setContentTitle("🔔 $noteTitle")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        val settingsPrefs = context.getSharedPreferences("noteland_settings_prefs", Context.MODE_PRIVATE)
        if (!settingsPrefs.getBoolean("notifications_enabled", true)) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        try {
            notificationManager.notify(reminderId.hashCode(), notification)
        } catch (_: SecurityException) {
            // Gracefully ignore if notification permission was revoked concurrently
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule all upcoming reminders after device reboot
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(appContext)
                    val upcoming = db.reminderDao().getUpcomingRemindersDirect(System.currentTimeMillis())
                    upcoming.forEach { reminder ->
                        val note = db.noteDao().getNoteByIdDirect(reminder.noteId)
                        val title = note?.title ?: "Reminder"
                        ReminderNotificationManager.scheduleReminder(appContext, reminder, title)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val reminderId = intent.getStringExtra("EXTRA_REMINDER_ID")
        if (reminderId.isNullOrBlank()) {
            pendingResult.finish()
            return
        }
        val noteId = intent.getStringExtra("EXTRA_NOTE_ID") ?: ""
        val noteTitle = intent.getStringExtra("EXTRA_NOTE_TITLE") ?: "Note Reminder"
        val repeatInterval = intent.getStringExtra("EXTRA_REPEAT")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(appContext)
                val note = if (noteId.isNotBlank()) db.noteDao().getNoteByIdDirect(noteId) else null
                val isLocked = note?.isLocked == true
                val displayTitle = if (isLocked) "🔒 Locked Note Reminder" else noteTitle
                val displayContent = if (isLocked) "Unlock NoteLand to view private reminder." else "Scheduled reminder: $noteTitle"

                ReminderNotificationManager.showNotification(
                    context = appContext,
                    reminderId = reminderId,
                    noteId = noteId,
                    noteTitle = displayTitle,
                    content = displayContent
                )

                // If repeating reminder, calculate next occurrence cleanly across time zones / month lengths
                if (!repeatInterval.isNullOrBlank()) {
                    val reminder = db.reminderDao().getReminderById(reminderId)
                    if (reminder != null && !reminder.isCompleted) {
                        val rule = com.example.data.model.TaskRecurrenceRule.fromName(repeatInterval)
                        val nextTime = ProductivityEngine.calculateNextOccurrenceUtc(
                            currentUtc = reminder.scheduledTimeUtc,
                            rule = rule
                        ) ?: 0L
                        if (nextTime > System.currentTimeMillis()) {
                            val updated = reminder.copy(scheduledTimeUtc = nextTime)
                            db.reminderDao().updateReminder(updated)
                            ReminderNotificationManager.scheduleReminder(appContext, updated, noteTitle)
                        }
                    }
                }
                com.example.widget.WidgetUpdateHelper.scheduleWidgetRefresh(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
