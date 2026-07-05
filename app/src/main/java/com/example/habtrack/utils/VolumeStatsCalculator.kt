package com.example.habtrack.utils

import com.example.habtrack.data.HabitEntity
import java.util.Calendar

/**
 * VolumeStatsCalculator: Calculates aggregate volume statistics for habits
 * Used for tracking cumulative totals like "400km walked this year"
 */
object VolumeStatsCalculator {

    /**
     * Calculates total volume for current day
     */
    fun getTodayVolume(habit: HabitEntity): Float {
        val isToday = isDateToday(habit.lastUpdated)
        return if (isToday) habit.currentValue else 0f
    }

    /**
     * Calculates estimated weekly volume based on current streak
     */
    fun getWeeklyVolume(habit: HabitEntity): Float {
        val daysInWeek = minOf(7, habit.currentStreak)
        val avgDailyValue = if (habit.totalCompletions > 0) {
            (habit.currentValue * (habit.currentStreak + 1)) / habit.totalCompletions
        } else {
            habit.currentValue
        }
        return avgDailyValue * daysInWeek
    }

    /**
     * Calculates estimated monthly volume
     */
    fun getMonthlyVolume(habit: HabitEntity): Float {
        val avgDaily = getAverageDailyVolume(habit)
        return avgDaily * 30f
    }

    /**
     * Calculates estimated yearly volume
     */
    fun getYearlyVolume(habit: HabitEntity): Float {
        val avgDaily = getAverageDailyVolume(habit)
        return avgDaily * 365f
    }

    /**
     * Calculates average daily volume based on completions
     */
    fun getAverageDailyVolume(habit: HabitEntity): Float {
        return if (habit.totalCompletions > 0) {
            habit.currentValue / (habit.totalCompletions / 30f)
        } else {
            habit.currentValue
        }
    }

    /**
     * Gets personal record (highest value ever achieved)
     */
    fun getPersonalRecord(habit: HabitEntity): Float {
        return habit.personalRecord
    }

    /**
     * Checks if a timestamp is today
     */
    private fun isDateToday(timestamp: Long): Boolean {
        val cal = Calendar.getInstance()
        val today = cal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val givenCal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return today.timeInMillis == givenCal.timeInMillis
    }

    /**
     * Formats volume for display with unit
     */
    fun formatVolume(value: Float, unit: String, decimals: Int = 1): String {
        return if (value % 1 == 0f) {
            "${value.toInt()} $unit"
        } else {
            "%.${decimals}f %s".format(value, unit)
        }
    }
}
