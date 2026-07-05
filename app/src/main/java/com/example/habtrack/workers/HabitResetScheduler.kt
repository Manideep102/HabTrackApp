package com.example.habtrack.workers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * HabitResetScheduler: Manages scheduling the daily habit reset task
 * Schedules the HabitResetWorker to run once per day at midnight
 */
object HabitResetScheduler {

    private const val WORK_NAME = "habit_reset_work"

    /**
     * Schedules the daily habit reset task
     * This should be called once when the app starts
     */
    fun scheduleHabitReset(context: Context) {
        // Create a periodic work request to run once per day
        val habitResetWork = PeriodicWorkRequestBuilder<HabitResetWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        ).apply {
            // Set initial delay to run at midnight (can be adjusted)
            setInitialDelay(calculateDelayToMidnight(), TimeUnit.MILLISECONDS)
        }.build()

        // Enqueue the work, replacing any existing work with the same name
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            habitResetWork
        )
    }

    /**
     * Calculates the delay in milliseconds until midnight
     */
    private fun calculateDelayToMidnight(): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            // Set to next day at midnight
            add(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis - System.currentTimeMillis()
    }

    /**
     * Cancels the habit reset task
     */
    fun cancelHabitReset(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
