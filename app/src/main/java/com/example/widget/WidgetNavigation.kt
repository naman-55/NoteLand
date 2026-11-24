package com.example.widget

import android.content.Context
import android.content.Intent
import com.example.MainActivity

object WidgetNavigation {
    const val EXTRA_WIDGET_ROUTE = "WIDGET_NAVIGATE_ROUTE"
    const val EXTRA_WIDGET_ACTION = "WIDGET_ACTION"
    const val EXTRA_WIDGET_FOLDER_ID = "WIDGET_FOLDER_ID"
    const val EXTRA_WIDGET_NOTE_ID = "NAVIGATE_TO_NOTE_ID"

    const val ACTION_NEW_NOTE = "NEW_NOTE"
    const val ACTION_NEW_CHECKLIST = "NEW_CHECKLIST"
    const val ACTION_OPEN_DAILY_DIARY = "OPEN_DAILY_DIARY"
    const val ACTION_NEW_FOLDER = "NEW_FOLDER"
    const val ACTION_OPEN_FOLDER = "OPEN_FOLDER"

    fun routeIntent(context: Context, route: String): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = "com.example.noteland.WIDGET_ROUTE_$route"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_WIDGET_ROUTE, route)
        }
    }

    fun noteIntent(context: Context, noteId: String): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = "com.example.noteland.WIDGET_NOTE_$noteId"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_WIDGET_NOTE_ID, noteId)
        }
    }

    fun actionIntent(context: Context, widgetAction: String, folderId: String? = null): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = "com.example.noteland.WIDGET_ACT_${widgetAction}_${folderId.orEmpty()}"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_WIDGET_ACTION, widgetAction)
            if (!folderId.isNullOrBlank()) {
                putExtra(EXTRA_WIDGET_FOLDER_ID, folderId)
            }
        }
    }
}
