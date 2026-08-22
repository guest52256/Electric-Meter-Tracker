package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.DailyReading
import com.example.model.Meter
import com.example.model.MeterBillingCycle
import com.example.model.SyncQueueItem

@Database(
    entities = [
        Meter::class,
        MeterBillingCycle::class,
        DailyReading::class,
        SyncQueueItem::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun meterDao(): MeterDao
    abstract fun billingCycleDao(): BillingCycleDao
    abstract fun dailyReadingDao(): DailyReadingDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "meter_tracker_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    try {
                        context.deleteDatabase("meter_tracker_database")
                    } catch (_: Exception) {}
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "meter_tracker_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                    instance
                }
            }
        }
    }
}
