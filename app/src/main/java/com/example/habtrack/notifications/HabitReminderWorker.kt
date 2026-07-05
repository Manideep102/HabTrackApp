package com.example.habtrack.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * HabitReminderWorker: Background task that sends habit reminders at scheduled times
 */
class HabitReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Get the habit details from work input data
            val habitId = inputData.getInt("habitId", -1)
            val habitName = inputData.getString("habitName") ?: "Habit"

            if (habitId != -1) {
                // Show the notification
                HabitNotificationManager.showHabitReminder(
                    applicationContext,
                    habitId,
                    habitName
                )
            }

            // Return success
            Result.success()
        } catch (e: Exception) {
            // Log the error and retry
            e.printStackTrace()
            Result.retry()
        }
    }
}
