package com.example.habtrack.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.habtrack.data.HabitDatabase
import com.example.habtrack.data.HabitRepository

/**
 * HabitResetWorker: Background task that runs daily to reset habit progress
 * This worker resets all habits' currentValue to 0 at midnight
 */
class HabitResetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Get the database and repository
            val database = HabitDatabase.getDatabase(applicationContext)
            val repository = HabitRepository(database.habitDao(), database.dailyCompletionDao())

            // Reset all habit progress
            repository.resetAll()

            // Return success
            Result.success()
        } catch (e: Exception) {
            // Log the error and retry
            e.printStackTrace()
            Result.retry()
        }
    }
}
