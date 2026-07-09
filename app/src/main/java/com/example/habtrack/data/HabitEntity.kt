package com.example.habtrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * HabitEntity: Defines the SQL table structure.
 * * NOTE: HabitDao and HabitRepository have been moved to their own
 * respective files to prevent "Redeclaration" errors.
 */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val goalValue: Float,
    val currentValue: Float = 0f,
    val unit: String,
    val colorHex: String,
    val iconName: String = "bolt",
    val lastUpdated: Long = System.currentTimeMillis(),
    val reminderTime: String = "09:00", // HH:mm format, e.g., "09:00" for 9 AM
    val reminderEnabled: Boolean = false, // Whether reminders are enabled for this habit
    val strengthScore: Float = 0f, // 0-100 score based on consistency
    val lastCompletedDate: Long = 0L, // Timestamp of last completion
    val totalCompletions: Int = 0, // Lifetime completions count
    val currentStreak: Int = 0, // Current consecutive days
    val personalRecord: Float = 0f, // Highest value ever achieved
    val incrementValue: Float = 1f, // Custom increment amount for +/- buttons
    val autoSyncEnabled: Boolean = false, // Whether this habit's progress is synced from Health Connect
    val autoSyncMetric: String? = null, // Health Connect metric name (HealthMetric.name) feeding this habit, or null
    val autoSyncBackfilled: Boolean = false // Whether the last-30-days Health Connect history has been backfilled for this metric
)