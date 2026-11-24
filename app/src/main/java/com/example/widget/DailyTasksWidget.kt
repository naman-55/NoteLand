package com.example.widget

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.example.R
import com.example.ui.navigation.Screen

class DailyTasksWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataLoader.loadDailyTasksSnapshot(context)
        provideContent {
            GlanceTheme {
                DailyTasksContent(snapshot)
            }
        }
    }

    @Composable
    private fun DailyTasksContent(snapshot: DailyTasksWidgetSnapshot) {
        val context = LocalContext.current
        val size = LocalSize.current
        val isCompactHeight = size.height < 150.dp
        val isWide = size.width >= 260.dp

        val containerModifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetThemeTokens.WidgetSurface)
            .let { mod ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    mod.cornerRadius(android.R.dimen.system_app_widget_background_radius)
                } else {
                    mod.cornerRadius(22.dp)
                }
            }
            .padding(14.dp)

        Column(modifier = containerModifier) {
            // Header Row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(actionStartActivity(WidgetNavigation.routeIntent(context, Screen.Tasks.route))),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(30.dp)
                            .cornerRadius(9.dp)
                            .background(WidgetThemeTokens.VioletContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_tasks),
                            contentDescription = "Daily Tasks",
                            colorFilter = ColorFilter.tint(WidgetThemeTokens.VioletAccent),
                            modifier = GlanceModifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = GlanceModifier.width(9.dp))
                    Column {
                        Text(
                            text = "Daily Tasks",
                            style = TextStyle(
                                color = WidgetThemeTokens.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        if (snapshot.totalCount > 0 && !isCompactHeight) {
                            val pct = ((snapshot.doneCount.toFloat() / snapshot.totalCount.toFloat()) * 100f).toInt()
                            Text(
                                text = "${snapshot.doneCount}/${snapshot.totalCount} done · $pct%",
                                style = TextStyle(
                                    color = WidgetThemeTokens.TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Count pill
                if (snapshot.totalCount > 0) {
                    Box(
                        modifier = GlanceModifier
                            .cornerRadius(10.dp)
                            .background(WidgetThemeTokens.AmberContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clickable(actionStartActivity(WidgetNavigation.routeIntent(context, Screen.Tasks.route)))
                    ) {
                        Text(
                            text = "${snapshot.doneCount}/${snapshot.totalCount}",
                            style = TextStyle(
                                color = WidgetThemeTokens.AmberAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = GlanceModifier.width(6.dp))
                }

                // Quick add checklist button
                Box(
                    modifier = GlanceModifier
                        .size(30.dp)
                        .cornerRadius(15.dp)
                        .background(WidgetThemeTokens.PrimaryAccent)
                        .clickable(
                            actionStartActivity(
                                WidgetNavigation.actionIntent(context, WidgetNavigation.ACTION_NEW_CHECKLIST)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_add),
                        contentDescription = "New Task",
                        colorFilter = ColorFilter.tint(WidgetThemeTokens.OnPrimaryAccent),
                        modifier = GlanceModifier.size(18.dp)
                    )
                }
            }

            if (snapshot.totalCount > 0 && !isCompactHeight) {
                Spacer(modifier = GlanceModifier.height(8.dp))
                val progress = (snapshot.doneCount.toFloat() / snapshot.totalCount.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = progress,
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .cornerRadius(3.dp),
                    color = WidgetThemeTokens.VioletAccent,
                    backgroundColor = WidgetThemeTokens.ProgressTrack
                )
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            if (snapshot.items.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(14.dp)
                        .background(WidgetThemeTokens.CardSurface)
                        .padding(12.dp)
                        .clickable(
                            actionStartActivity(
                                WidgetNavigation.actionIntent(context, WidgetNavigation.ACTION_NEW_CHECKLIST)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No daily tasks yet",
                            style = TextStyle(
                                color = WidgetThemeTokens.TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "Tap + to add a checklist or task",
                            style = TextStyle(
                                color = WidgetThemeTokens.TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(
                        items = snapshot.items,
                        itemId = { item -> item.id.hashCode().toLong() }
                    ) { task ->
                        Column(modifier = GlanceModifier.fillMaxWidth()) {
                            TaskRow(task = task, isWide = isWide, context = context)
                            Spacer(modifier = GlanceModifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TaskRow(
        task: WidgetTaskItem,
        isWide: Boolean,
        context: Context
    ) {
        val toggleAction = actionRunCallback<ToggleDailyTaskAction>(
            actionParametersOf(
                ToggleDailyTaskAction.KEY_IS_REMINDER to task.isReminder,
                ToggleDailyTaskAction.KEY_NOTE_ID to task.noteId,
                ToggleDailyTaskAction.KEY_CHECKLIST_INDEX to task.checklistIndex,
                ToggleDailyTaskAction.KEY_REMINDER_ID to task.reminderId,
                ToggleDailyTaskAction.KEY_TASK_ID to task.taskId
            )
        )

        val openIntent = when {
            task.taskId.isNotBlank() && task.noteId.isBlank() ->
                WidgetNavigation.routeIntent(context, Screen.Productivity.createRoute(0))
            task.isReminder ->
                WidgetNavigation.routeIntent(context, Screen.Tasks.route)
            task.noteId.isNotBlank() ->
                WidgetNavigation.noteIntent(context, task.noteId)
            else ->
                WidgetNavigation.routeIntent(context, Screen.Tasks.route)
        }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(12.dp)
                .background(WidgetThemeTokens.CardSurface)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(28.dp)
                    .clickable(toggleAction),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(
                        if (task.isDone) R.drawable.ic_widget_check_done else R.drawable.ic_widget_check_todo
                    ),
                    contentDescription = if (task.isDone) "Completed" else "Mark task done",
                    colorFilter = ColorFilter.tint(
                        if (task.isDone) WidgetThemeTokens.EmeraldAccent else WidgetThemeTokens.VioletAccent
                    ),
                    modifier = GlanceModifier.size(20.dp)
                )
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionStartActivity(openIntent))
            ) {
                Text(
                    text = task.title,
                    style = TextStyle(
                        color = if (task.isDone) WidgetThemeTokens.TextSecondary else WidgetThemeTokens.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (task.isDone) FontWeight.Normal else FontWeight.Medium,
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    maxLines = 1
                )
                if (isWide && task.sourceLabel.isNotBlank()) {
                    Text(
                        text = task.sourceLabel,
                        style = TextStyle(
                            color = WidgetThemeTokens.TextSecondary,
                            fontSize = 10.5.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

class ToggleDailyTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[KEY_TASK_ID].orEmpty()
        val isReminder = parameters[KEY_IS_REMINDER] ?: false
        when {
            taskId.isNotBlank() -> {
                WidgetDataLoader.toggleKanbanTask(context, taskId)
            }
            isReminder -> {
                val reminderId = parameters[KEY_REMINDER_ID].orEmpty()
                if (reminderId.isNotBlank()) {
                    WidgetDataLoader.toggleReminderCompletion(context, reminderId)
                }
            }
            else -> {
                val noteId = parameters[KEY_NOTE_ID].orEmpty()
                val idx = parameters[KEY_CHECKLIST_INDEX] ?: -1
                if (noteId.isNotBlank() && idx >= 0) {
                    WidgetDataLoader.toggleChecklistTask(context, noteId, idx)
                }
            }
        }
    }

    companion object {
        val KEY_IS_REMINDER = ActionParameters.Key<Boolean>("is_reminder")
        val KEY_NOTE_ID = ActionParameters.Key<String>("note_id")
        val KEY_CHECKLIST_INDEX = ActionParameters.Key<Int>("checklist_index")
        val KEY_REMINDER_ID = ActionParameters.Key<String>("reminder_id")
        val KEY_TASK_ID = ActionParameters.Key<String>("task_id")
    }
}

class DailyTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyTasksWidget()
}
