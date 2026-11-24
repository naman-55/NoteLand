package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object WidgetUpdateHelper {
    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext
        runCatching { DailyTasksWidget().updateAll(appContext) }
        runCatching { RemindersWidget().updateAll(appContext) }
        runCatching { CollectionsTilesWidget().updateAll(appContext) }
    }

    fun scheduleWidgetRefresh(context: Context?) {
        val appContext = context?.applicationContext ?: return
        widgetScope.launch {
            updateAllWidgets(appContext)
        }
    }

    fun canRequestPinWidget(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = AppWidgetManager.getInstance(context) ?: return false
        return runCatching { manager.isRequestPinAppWidgetSupported }.getOrDefault(false)
    }

    fun requestPinWidget(
        context: Context,
        receiverClass: Class<out GlanceAppWidgetReceiver>
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = AppWidgetManager.getInstance(context) ?: return false
        return runCatching {
            if (!manager.isRequestPinAppWidgetSupported) return false
            val provider = ComponentName(context, receiverClass)
            manager.requestPinAppWidget(provider, null, null)
        }.getOrDefault(false)
    }
}
