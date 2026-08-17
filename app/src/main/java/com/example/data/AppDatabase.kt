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

@Database(entities = [CropRecord::class, Worker::class, AttendanceRecord::class, AppNotification::class, AdvancePayment::class, FarmerContact::class, RecycleBinEntity::class, InventoryItem::class, GardenPlanningEntry::class], version = 16, exportSchema = false)
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

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `garden_planning_entries` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `serialNumber` TEXT NOT NULL,
                            `farmerName` TEXT NOT NULL,
                            `farmerAddress` TEXT NOT NULL,
                            `contactNumber` TEXT NOT NULL,
                            `totalKanalArea` REAL NOT NULL,
                            `plantsPerKanal` INTEGER NOT NULL,
                            `costPerPlant` REAL NOT NULL,
                            `totalCost` REAL NOT NULL,
                            `amountPaid` REAL NOT NULL DEFAULT 0.0,
                            `remainingBalance` REAL NOT NULL DEFAULT 0.0,
                            `paymentStatus` TEXT NOT NULL,
                            `bookingDate` TEXT NOT NULL,
                            `expectedDelivery` TEXT NOT NULL,
                            `notes` TEXT NOT NULL,
                            `timestamp` INTEGER NOT NULL
                        )
                    """.trimIndent())
                } catch (e: Exception) {
                    // Table may already exist
                }
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE garden_planning_entries ADD COLUMN amountPaid REAL NOT NULL DEFAULT 0.0")
                    db.execSQL("ALTER TABLE garden_planning_entries ADD COLUMN remainingBalance REAL NOT NULL DEFAULT 0.0")
                } catch (e: Exception) {
                    // Columns may already exist
                }
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE garden_planning_entries ADD COLUMN installmentHistoryJson TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                    // Column may already exist
                }
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE garden_planning_entries ADD COLUMN plantVariety TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE garden_planning_entries ADD COLUMN rootStock TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                    // Columns may already exist
                }
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE garden_planning_entries ADD COLUMN saplingAge TEXT NOT NULL DEFAULT '1 Year'")
                } catch (e: Exception) {
                    // Column may already exist
                }
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE garden_planning_entries ADD COLUMN plantOrigin TEXT NOT NULL DEFAULT 'Local Plants'")
                } catch (e: Exception) {
                    // Column may already exist
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agri_crop_database"
                )
                .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agri_crop_database"
                )
                .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
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
