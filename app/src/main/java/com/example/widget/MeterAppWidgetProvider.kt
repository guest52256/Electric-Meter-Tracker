package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MeterAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET ||
            intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, MeterAppWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            updateWidgets(context, appWidgetManager, allWidgetIds)
        }
    }

    companion object {
        private const val TAG = "MeterWidgetProvider"
        const val ACTION_REFRESH_WIDGET = "com.example.widget.ACTION_REFRESH_METER_WIDGET"

        fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val appContext = context.applicationContext
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    val db = AppDatabase.getDatabase(appContext)
                    val meters = db.meterDao().getMeterById(1L) ?: db.meterDao().getMeterByName("Muhammad Iqbal S/O Luqman")
                    val activeMeters = if (meters != null) listOf(meters) else emptyList()

                    // Try to get first active meter or latest reading overall
                    val meterId = activeMeters.firstOrNull()?.id ?: 1L
                    val meterName = activeMeters.firstOrNull()?.name ?: "Electric Meter"

                    val cycle = db.billingCycleDao().getBillingCycleForMeterDirect(meterId)
                    val latestReading = db.dailyReadingDao().getLatestReadingForMeterDirect(meterId)

                    val prevBill = cycle?.previousBillReading ?: 0.0
                    val currentReading = latestReading?.currentReading ?: prevBill
                    val unitsSinceBill = latestReading?.unitsSinceBill
                        ?: (currentReading - prevBill).coerceAtLeast(0.0)
                    val readingDate = latestReading?.dateString ?: "No reading logged"
                    val isAlert = unitsSinceBill >= 100.0

                    val timeFormat = SimpleDateFormat("h:mm a", Locale.ENGLISH)
                    val updatedTimeText = "Updated ${timeFormat.format(Date())}"

                    withContext(Dispatchers.Main) {
                        for (widgetId in appWidgetIds) {
                            val views = RemoteViews(appContext.packageName, R.layout.widget_meter_summary)

                            // Title & Header
                            views.setTextViewText(R.id.widget_meter_name, meterName)
                            views.setTextViewText(R.id.widget_last_updated, updatedTimeText)

                            // Latest Reading
                            views.setTextViewText(
                                R.id.widget_latest_reading_val,
                                String.format(Locale.ENGLISH, "%.1f", currentReading)
                            )
                            views.setTextViewText(R.id.widget_reading_date, readingDate)

                            // Cycle Consumed
                            views.setTextViewText(
                                R.id.widget_cycle_units_val,
                                "${unitsSinceBill.toInt()} Units"
                            )
                            views.setTextViewText(
                                R.id.widget_baseline_info,
                                "Base: ${String.format(Locale.ENGLISH, "%.1f", prevBill)}"
                            )

                            // Status Pill
                            if (isAlert) {
                                views.setInt(
                                    R.id.widget_status_pill,
                                    "setBackgroundResource",
                                    R.drawable.widget_pill_alert
                                )
                                views.setTextViewText(
                                    R.id.widget_status_text,
                                    "🔴 ALERT: ${unitsSinceBill.toInt()} Units (Exceeded 100)"
                                )
                                views.setTextColor(
                                    R.id.widget_status_text,
                                    android.graphics.Color.parseColor("#FCA5A5")
                                )
                            } else {
                                views.setInt(
                                    R.id.widget_status_pill,
                                    "setBackgroundResource",
                                    R.drawable.widget_pill_normal
                                )
                                views.setTextViewText(
                                    R.id.widget_status_text,
                                    "🟢 Normal Usage: ${unitsSinceBill.toInt()} / 100 Units"
                                )
                                views.setTextColor(
                                    R.id.widget_status_text,
                                    android.graphics.Color.parseColor("#6EE7B7")
                                )
                            }

                            // Click Intent to launch MainActivity
                            val clickIntent = Intent(appContext, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                            val pendingIntent = PendingIntent.getActivity(
                                appContext,
                                widgetId,
                                clickIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                            appWidgetManager.updateAppWidget(widgetId, views)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating widgets: ${e.message}", e)
                }
            }
        }
    }
}
