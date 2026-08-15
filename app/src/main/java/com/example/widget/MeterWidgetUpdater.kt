package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

object MeterWidgetUpdater {
    private const val TAG = "MeterWidgetUpdater"

    fun updateAllWidgets(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, MeterAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds.isNotEmpty()) {
                MeterAppWidgetProvider.updateWidgets(context, appWidgetManager, appWidgetIds)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error requesting widget update: ${e.message}")
        }
    }
}
