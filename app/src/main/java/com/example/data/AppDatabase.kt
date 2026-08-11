package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.room.migration.Migration

@Database(entities = [CropRecord::class, Worker::class, AttendanceRecord::class, AppNotification::class, AdvancePayment::class, FarmerContact::class, RecycleBinEntity::class, InventoryItem::class, GardenPlanningEntry::class], version = 11, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cropRecordDao(): CropRecordDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun notificationDao(): NotificationDao
    abstract fun farmerContactDao(): FarmerContactDao
    abstract fun recycleBinDao(): RecycleBinDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun gardenPlanningDao(): GardenPlanningDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE crop_records ADD COLUMN paymentHistoryJson TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                    // Column may already exist
                }
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agri_crop_database"
                )
                .addMigrations(MIGRATION_9_10)
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.cropRecordDao())
                    }
                }
            }
        }

        suspend fun populateDatabase(dao: CropRecordDao) {
            // No automatic sample records inserted. Only manual user-saved bookings are retained.
        }
    }
}
