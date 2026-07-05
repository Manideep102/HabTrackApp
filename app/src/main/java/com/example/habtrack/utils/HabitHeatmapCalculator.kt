package com.example.habtrack.utils

import com.example.habtrack.data.DailyCompletion
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.WeekFields
import kotlin.math.abs

/**
 * Calculates and formats habit completion data for heatmap visualization.
 * Provides methods to analyze completion patterns over time.
 */
object HabitHeatmapCalculator {

    /**
     * Represents a single cell in the heatmap (day's completion status)
     */
    data class HeatmapCell(
        val date: LocalDate,
        val dayOfWeek: Int, // 0 = Sunday, 1 = Monday, etc.
        val weekOfYear: Int,
        val isCompleted: Boolean,
        val value: Float = 0f,
        val count: Int = 0
    )

    /**
     * Get the last 52 weeks of heatmap data from daily completions
     */
    fun generateHeatmapData(completions: List<DailyCompletion>): List<HeatmapCell> {
        val today = LocalDate.now()
        val startDate = today.minusWeeks(52)
        
        val completionMap = completions
            .groupBy { getDateFromTimestamp(it.dateTime) }
            .mapValues { (_, completions) ->
                completions.any { it.isCompleted } to completions.map { it.progressValue }.average().toFloat()
            }

        val cells = mutableListOf<HeatmapCell>()
        var currentDate = startDate

        while (!currentDate.isAfter(today)) {
            val (isCompleted, value) = completionMap[currentDate] ?: (false to 0f)
            val weekOfYear = currentDate.get(WeekFields.ISO.weekOfYear())
            
            cells.add(
                HeatmapCell(
                    date = currentDate,
                    dayOfWeek = currentDate.dayOfWeek.value % 7, // 0 = Sunday
                    weekOfYear = weekOfYear,
                    isCompleted = isCompleted,
                    value = value
                )
            )
            currentDate = currentDate.plusDays(1)
        }

        return cells
    }

    /**
     * Get heatmap data organized by week for easier rendering
     */
    fun generateWeeklyHeatmapData(completions: List<DailyCompletion>): List<List<HeatmapCell>> {
        val cells = generateHeatmapData(completions)
        return cells.groupBy { it.weekOfYear }.values.toList()
    }

    /**
     * Get intensity level (0-4) based on completion count in a date range
     */
    fun getIntensityLevel(value: Float, maxValue: Float = 100f): Int {
        return when {
            value <= 0f -> 0
            value <= maxValue * 0.25f -> 1
            value <= maxValue * 0.5f -> 2
            value <= maxValue * 0.75f -> 3
            else -> 4
        }
    }

    /**
     * Get color for heatmap cell based on intensity
     */
    fun getHeatmapColor(isCompleted: Boolean, intensity: Int): String {
        return when {
            !isCompleted -> "#F0F0F0" // Light gray - no completion
            intensity == 1 -> "#C6E48B" // Very light green
            intensity == 2 -> "#7BC96F" // Light green
            intensity == 3 -> "#239A3B" // Medium green
            else -> "#196127" // Dark green
        }
    }

    /**
     * Get completion streak from recent daily completions
     */
    fun calculateCurrentStreak(completions: List<DailyCompletion>): Int {
        val today = LocalDate.now()
        val sortedCompletions = completions.sortedByDescending { it.dateTime }
        
        var streak = 0
        var currentDate = today

        for (completion in sortedCompletions) {
            val completionDate = getDateFromTimestamp(completion.dateTime)
            
            if (completionDate.isEqual(currentDate) && completion.isCompleted) {
                streak++
                currentDate = currentDate.minusDays(1)
            } else if (completionDate.isBefore(currentDate)) {
                // Gap found
                break
            }
        }

        return streak
    }

    /**
     * Get longest streak from all daily completions
     */
    fun calculateLongestStreak(completions: List<DailyCompletion>): Int {
        val sortedCompletions = completions.filter { it.isCompleted }.sortedBy { it.dateTime }
        
        if (sortedCompletions.isEmpty()) return 0

        var longest = 1
        var current = 1
        var lastDate = getDateFromTimestamp(sortedCompletions[0].dateTime)

        for (i in 1 until sortedCompletions.size) {
            val currentDate = getDateFromTimestamp(sortedCompletions[i].dateTime)
            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastDate, currentDate)

            if (daysBetween == 1L) {
                current++
                longest = maxOf(longest, current)
            } else {
                current = 1
            }

            lastDate = currentDate
        }

        return longest
    }

    /**
     * Get completion percentage for a date range
     */
    fun getCompletionPercentage(completions: List<DailyCompletion>, days: Int): Float {
        if (completions.isEmpty()) return 0f
        
        val today = LocalDate.now()
        val startDate = today.minusDays((days - 1).toLong())
        
        val daysInRange = completions.filter { completion ->
            val completionDate = getDateFromTimestamp(completion.dateTime)
            !completionDate.isBefore(startDate) && !completionDate.isAfter(today)
        }

        return if (days > 0) {
            (daysInRange.count { it.isCompleted }.toFloat() / days) * 100f
        } else {
            0f
        }
    }

    /**
     * Convert timestamp to LocalDate
     */
    private fun getDateFromTimestamp(timestamp: Long): LocalDate {
        return java.time.Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    /**
     * Get month name from date
     */
    fun getMonthName(date: LocalDate): String {
        return date.month.toString().take(3)
    }

    /**
     * Get day of week abbreviation
     */
    fun getDayOfWeekAbbr(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            0 -> "Sun"
            1 -> "Mon"
            2 -> "Tue"
            3 -> "Wed"
            4 -> "Thu"
            5 -> "Fri"
            6 -> "Sat"
            else -> ""
        }
    }
}
