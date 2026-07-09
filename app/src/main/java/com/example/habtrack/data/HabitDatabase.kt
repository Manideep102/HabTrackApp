package com.example.habtrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

// THE DATABASE
@Database(entities = [HabitEntity::class, DailyCompletion::class], version = 5, exportSchema = false)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun dailyCompletionDao(): DailyCompletionDao

    companion object {
        @Volatile
        private var Instance: HabitDatabase? = null

        /**
         * Adds the Health Connect auto-sync columns without wiping existing habit data.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN autoSyncEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE habits ADD COLUMN autoSyncMetric TEXT")
            }
        }

        /**
         * Adds the flag tracking whether a habit's last-30-days Health Connect history
         * has been backfilled, without wiping existing habit data.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN autoSyncBackfilled INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): HabitDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context,
                    HabitDatabase::class.java,
                    "habit_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}