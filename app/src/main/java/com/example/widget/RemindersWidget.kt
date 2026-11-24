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
import androidx.glance.text.TextStyle
import com.example.R
import com.example.ui.navigation.Screen

class RemindersWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataLoader.loadRemindersSnapshot(context)
        provideContent {
            GlanceTheme {
                RemindersContent(snapshot)
            }
        }
    }

    @Composable
    private fun RemindersContent(snapshot: RemindersWidgetSnapshot) {
        val context = LocalContext.current
        val size = LocalSize.current
        val isCompactHeight = size.height < 145.dp

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
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(actionStartActivity(WidgetNavigation.routeIntent(context, Screen.Reminders.route))),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(30.dp)
                            .cornerRadius(9.dp)
                            .background(WidgetThemeTokens.CoralContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_alarm),
                            contentDescription = "Reminders",
                            colorFilter = ColorFilter.tint(WidgetThemeTokens.CoralAccent),
                            modifier = GlanceModifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = GlanceModifier.width(9.dp))
                    Column {
                        Text(
                            text = "Reminders",
                            style = TextStyle(
                                color = WidgetThemeTokens.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        if (!isCompactHeight) {
                            val sub = when {
                                snapshot.overdueCount > 0 -> "${snapshot.overdueCount} overdue · ${snapshot.pendingCount} active"
                                snapshot.todayCount > 0 -> "${snapshot.todayCount} due today"
                                snapshot.pendingCount > 0 -> "${snapshot.pendingCount} upcoming"
                                else -> "All caught up"
                            }
                            Text(
                                text = sub,
                                style = TextStyle(
                                    color = if (snapshot.overdueCount > 0) WidgetThemeTokens.CoralAccent else WidgetThemeTokens.TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                if (snapshot.pendingCount > 0) {
                    Box(
                        modifier = GlanceModifier
                            .cornerRadius(10.dp)
                            .background(
                                if (snapshot.overdueCount > 0) WidgetThemeTokens.CoralContainer else WidgetThemeTokens.VioletContainer
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clickable(actionStartActivity(WidgetNavigation.routeIntent(context, Screen.Reminders.route)))
                    ) {
                        Text(
                            text = "${snapshot.pendingCount}",
                            style = TextStyle(
                                color = if (snapshot.overdueCount > 0) WidgetThemeTokens.CoralAccent else WidgetThemeTokens.VioletAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = GlanceModifier.width(6.dp))
                }

                Box(
                    modifier = GlanceModifier
                        .size(30.dp)
                        .cornerRadius(15.dp)
                        .background(WidgetThemeTokens.PrimaryAccent)
                        .clickable(actionStartActivity(WidgetNavigation.routeIntent(context, Screen.Reminders.route))),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_add),
                        contentDescription = "Schedule Reminder",
                        colorFilter = ColorFilter.tint(WidgetThemeTokens.OnPrimaryAccent),
                        modifier = GlanceModifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            if (snapshot.items.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(14.dp)
                        .background(WidgetThemeTokens.CardSurface)
                        .padding(12.dp)
                        .clickable(actionStartActivity(WidgetNavigation.routeIntent(context, Screen.Reminders.route))),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No upcoming reminders",
                            style = TextStyle(
                                color = WidgetThemeTokens.TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "Tap + to schedule a reminder",
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
                        itemId = { item -> item.reminderId.hashCode().toLong() }
                    ) { reminder ->
                        Column(modifier = GlanceModifier.fillMaxWidth()) {
                            ReminderRow(item = reminder, context = context)
                            Spacer(modifier = GlanceModifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ReminderRow(
        item: WidgetReminderItem,
        context: Context
    ) {
        val openIntent = if (item.isLockedNote || item.noteId.isBlank()) {
            WidgetNavigation.routeIntent(context, Screen.Reminders.route)
        } else {
            WidgetNavigation.noteIntent(context, item.noteId)
        }

        val badgeBg = when {
            item.isOverdue -> WidgetThemeTokens.CoralContainer
            item.isToday -> WidgetThemeTokens.AmberContainer
            else -> WidgetThemeTokens.VioletContainer
        }
        val badgeText = when {
            item.isOverdue -> WidgetThemeTokens.CoralAccent
            item.isToday -> WidgetThemeTokens.AmberAccent
            else -> WidgetThemeTokens.VioletAccent
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
                    .clickable(
                        actionRunCallback<ToggleReminderWidgetAction>(
                            actionParametersOf(
                                ToggleReminderWidgetAction.KEY_REMINDER_ID to item.reminderId
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_check_todo),
                    contentDescription = "Complete reminder",
                    colorFilter = ColorFilter.tint(badgeText),
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
                    text = item.noteTitle,
                    style = TextStyle(
                        color = WidgetThemeTokens.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = GlanceModifier
                            .cornerRadius(6.dp)
                            .background(badgeBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.timeLabel,
                            style = TextStyle(
                                color = badgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    }
                    if (!item.repeatLabel.isNullOrBlank()) {
                        Spacer(modifier = GlanceModifier.width(5.dp))
                        Text(
                            text = item.repeatLabel,
                            style = TextStyle(
                                color = WidgetThemeTokens.TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

class ToggleReminderWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val reminderId = parameters[KEY_REMINDER_ID].orEmpty()
        if (reminderId.isNotBlank()) {
            WidgetDataLoader.toggleReminderCompletion(context, reminderId)
        }
    }

    companion object {
        val KEY_REMINDER_ID = ActionParameters.Key<String>("reminder_id")
    }
}

class RemindersWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RemindersWidget()
}
