package com.example.habtrack.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a daily completion record for a habit.
 * Tracks whether a habit was completed on a specific day.
 */
@Entity(
    tableName = "daily_completions",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId"])
    ]
)
data class DailyCompletion(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val habitId: Int,
    val dateTime: Long, // Timestamp (milliseconds since epoch)
    val isCompleted: Boolean, // Whether the habit was completed on this day
    val progressValue: Float = 0f, // The actual progress value for that day
    val timestamp: Long = System.currentTimeMillis()
)
