package com.example.habtrack.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
// Rescheduling policy: KEEP would ignore a changed reminder time, so edits must re-enqueue.

/**
 * HabitReminderScheduler: Manages scheduling habit reminders
 */
object HabitReminderScheduler {

    private const val WORK_NAME_PREFIX = "habit_reminder_"

    /**
     * Schedules a reminder for a habit at a specific time daily
     * @param context Application context
     * @param habitId Unique habit ID
     * @param habitName Name of the habit
     * @param reminderTime Time in HH:mm format (e.g., "09:00")
     */
    fun scheduleReminder(
        context: Context,
        habitId: Int,
        habitName: String,
        reminderTime: String
    ) {
        val workName = "$WORK_NAME_PREFIX$habitId"

        // Parse the reminder time to calculate initial delay
        val parts = reminderTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        // Calculate delay to next occurrence of the reminder time
        val initialDelayMillis = calculateDelayToTime(hour, minute)

        // Create the work request data
        val workData = Data.Builder()
            .putInt("habitId", habitId)
            .putString("habitName", habitName)
            .build()

        // Create a periodic work request to run once per day
        val reminderWork = PeriodicWorkRequestBuilder<HabitReminderWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        ).apply {
            setInputData(workData)
            setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
        }.build()

        // Enqueue the work
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            reminderWork
        )
    }

    /**
     * Cancels a reminder for a habit
     */
    fun cancelReminder(context: Context, habitId: Int) {
        val workName = "$WORK_NAME_PREFIX$habitId"
        WorkManager.getInstance(context).cancelUniqueWork(workName)
    }

    /**
     * Calculates the delay in milliseconds until a specific time (HH:mm)
     */
    private fun calculateDelayToTime(hour: Int, minute: Int): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)

            // If the time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
        }
        return calendar.timeInMillis - System.currentTimeMillis()
    }
}
