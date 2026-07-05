package com.example.habtrack.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for DailyCompletion table.
 * Provides database operations for daily completion records.
 */
@Dao
interface DailyCompletionDao {

    @Insert
    suspend fun insertDailyCompletion(completion: DailyCompletion)

    @Update
    suspend fun updateDailyCompletion(completion: DailyCompletion)

    @Delete
    suspend fun deleteDailyCompletion(completion: DailyCompletion)

    @Query("SELECT * FROM daily_completions WHERE habitId = :habitId ORDER BY dateTime DESC")
    fun getDailyCompletionsForHabit(habitId: Int): Flow<List<DailyCompletion>>

    @Query("SELECT * FROM daily_completions WHERE habitId = :habitId AND dateTime >= :startDate AND dateTime <= :endDate")
    suspend fun getDailyCompletionsBetween(habitId: Int, startDate: Long, endDate: Long): List<DailyCompletion>

    @Query("SELECT * FROM daily_completions WHERE habitId = :habitId AND dateTime = :date")
    suspend fun getDailyCompletionForDate(habitId: Int, date: Long): DailyCompletion?

    @Query("DELETE FROM daily_completions WHERE habitId = :habitId")
    suspend fun deleteAllCompletionsForHabit(habitId: Int)

    @Query("SELECT COUNT(*) FROM daily_completions WHERE habitId = :habitId AND isCompleted = 1")
    suspend fun getCompletionCount(habitId: Int): Int

    @Query("SELECT COUNT(*) FROM daily_completions WHERE habitId = :habitId AND dateTime >= :startDate")
    suspend fun getCompletionCountSince(habitId: Int, startDate: Long): Int

    @Query("SELECT * FROM daily_completions ORDER BY dateTime DESC LIMIT :limit")
    fun getRecentCompletions(limit: Int = 365): Flow<List<DailyCompletion>>
}
