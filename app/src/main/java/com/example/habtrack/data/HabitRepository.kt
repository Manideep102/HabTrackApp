package com.example.habtrack.data

import kotlinx.coroutines.flow.Flow

/**
 * HabitRepository: Bridges the HabitDao and the HabitViewModel.
 * This class ensures that the UI doesn't talk directly to the Database.
 */
class HabitRepository(
    private val habitDao: HabitDao,
    private val dailyCompletionDao: DailyCompletionDao
) {

    // Expose the Flow of habits from the DAO
    val allHabits: Flow<List<HabitEntity>> = habitDao.getAllHabits()

    // Wrapper functions to call the DAO methods
    suspend fun insertHabit(habit: HabitEntity) {
        habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: HabitEntity) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habit: HabitEntity) {
        habitDao.deleteHabit(habit)
    }

    suspend fun resetAll() {
        habitDao.resetAllProgress()
    }

    // Daily completion methods
    suspend fun recordDailyCompletion(completion: DailyCompletion) {
        dailyCompletionDao.insertDailyCompletion(completion)
    }

    suspend fun updateDailyCompletion(completion: DailyCompletion) {
        dailyCompletionDao.updateDailyCompletion(completion)
    }

    fun getDailyCompletionsForHabit(habitId: Int): Flow<List<DailyCompletion>> {
        return dailyCompletionDao.getDailyCompletionsForHabit(habitId)
    }

    suspend fun getDailyCompletionsBetween(habitId: Int, startDate: Long, endDate: Long): List<DailyCompletion> {
        return dailyCompletionDao.getDailyCompletionsBetween(habitId, startDate, endDate)
    }

    suspend fun getDailyCompletionForDate(habitId: Int, date: Long): DailyCompletion? {
        return dailyCompletionDao.getDailyCompletionForDate(habitId, date)
    }
}