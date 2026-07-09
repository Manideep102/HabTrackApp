package com.example.habtrack.utils

import com.example.habtrack.data.DailyCompletion
import java.time.LocalDate
import java.time.ZoneId

/**
 * Decides which daily_completions rows a Health Connect 30-day backfill should insert.
 * Pure (no DB/HC deps) so the emulator's inability to hold HC history doesn't block testing.
 */
object HealthBackfillPlanner {

    /**
     * Rows to insert for [history] (already filtered to days with value > 0), excluding days
     * already recorded ([existingDays]) and [today]. Each row is timestamped at noon of its day
     * so date bucketing (`Instant.toLocalDate`) keeps it inside its own day under any zone.
     */
    fun planRows(
        habitId: Int,
        goalValue: Float,
        history: Map<LocalDate, Float>,
        existingDays: Set<LocalDate>,
        today: LocalDate,
        zone: ZoneId
    ): List<DailyCompletion> = history
        .filterKeys { it !in existingDays && it != today }
        .map { (date, value) ->
            DailyCompletion(
                habitId = habitId,
                dateTime = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
                isCompleted = value / goalValue >= 1f,
                progressValue = value
            )
        }
}
