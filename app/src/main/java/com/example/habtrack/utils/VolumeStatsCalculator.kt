package com.example.habtrack.utils

import com.example.habtrack.data.DailyCompletion
import com.example.habtrack.data.HabitEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * VolumeStatsCalculator: Aggregates real logged volume from a habit's daily_completions
 * (e.g. "400km walked this year"). Windowed totals are rolling from today, inclusive.
 */
object VolumeStatsCalculator {

    private fun dateOf(millis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    /**
     * Collapses [completions] to one value per calendar day. A day can hold several rows
     * (repeated manual logs are inserted, not merged); the latest write (max timestamp) is
     * authoritative, matching the habit's currentValue semantics.
     */
    private fun valuePerDay(completions: List<DailyCompletion>, zone: ZoneId): Map<LocalDate, Float> =
        completions
            .groupBy { dateOf(it.dateTime, zone) }
            .mapValues { (_, rows) -> rows.maxByOrNull { it.timestamp }?.progressValue ?: 0f }

    /** Total logged today. */
    fun getTodayVolume(completions: List<DailyCompletion>, zone: ZoneId = ZoneId.systemDefault()): Float =
        valuePerDay(completions, zone)[LocalDate.now(zone)] ?: 0f

    /** Sum of per-day totals over the last [days] days (inclusive of today). */
    private fun windowVolume(completions: List<DailyCompletion>, days: Int, zone: ZoneId): Float {
        val today = LocalDate.now(zone)
        val start = today.minusDays((days - 1).toLong())
        return valuePerDay(completions, zone)
            .filterKeys { !it.isBefore(start) && !it.isAfter(today) }
            .values.sum()
    }

    fun getWeeklyVolume(completions: List<DailyCompletion>, zone: ZoneId = ZoneId.systemDefault()): Float =
        windowVolume(completions, 7, zone)

    fun getMonthlyVolume(completions: List<DailyCompletion>, zone: ZoneId = ZoneId.systemDefault()): Float =
        windowVolume(completions, 30, zone)

    fun getYearlyVolume(completions: List<DailyCompletion>, zone: ZoneId = ZoneId.systemDefault()): Float =
        windowVolume(completions, 365, zone)

    /**
     * Average of the per-day totals over the last 30 days, counting only days that have data
     * (0 if none) — a "typical active day", not diluted by untracked days.
     */
    fun getAverageDailyVolume(completions: List<DailyCompletion>, zone: ZoneId = ZoneId.systemDefault()): Float {
        val today = LocalDate.now(zone)
        val start = today.minusDays(29)
        val inWindow = valuePerDay(completions, zone)
            .filterKeys { !it.isBefore(start) && !it.isAfter(today) }
        return if (inWindow.isEmpty()) 0f else inWindow.values.sum() / inWindow.size
    }

    /** Highest value ever achieved — tracked on the habit itself. */
    fun getPersonalRecord(habit: HabitEntity): Float = habit.personalRecord

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
