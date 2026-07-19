package com.example.habtrack

import com.example.habtrack.data.DailyCompletion
import com.example.habtrack.utils.VolumeStatsCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class VolumeStatsCalculatorTest {

    private val zone: ZoneId = ZoneId.of("America/New_York")

    /** A completion row on [daysAgo] days before today, with the given value and write order. */
    private fun row(daysAgo: Long, value: Float, writeSeq: Long = 0L): DailyCompletion {
        val date = LocalDate.now(zone).minusDays(daysAgo)
        return DailyCompletion(
            habitId = 1,
            dateTime = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
            isCompleted = false,
            progressValue = value,
            timestamp = writeSeq
        )
    }

    @Test
    fun windowsSumAcrossDays() {
        val completions = listOf(
            row(0, 35f),   // today
            row(1, 20f),   // yesterday (within week)
            row(10, 100f), // within month, outside week
            row(200, 5f)   // within year, outside month
        )
        assertEquals(55f, VolumeStatsCalculator.getWeeklyVolume(completions, zone), 0.001f)   // 35+20
        assertEquals(155f, VolumeStatsCalculator.getMonthlyVolume(completions, zone), 0.001f) // 35+20+100
        assertEquals(160f, VolumeStatsCalculator.getYearlyVolume(completions, zone), 0.001f)  // +5
        assertEquals(35f, VolumeStatsCalculator.getTodayVolume(completions, zone), 0.001f)
    }

    @Test
    fun multipleRowsSameDayUseLatestWrite() {
        // Two logs today: 10 then corrected to 25 (higher timestamp wins, not summed).
        val completions = listOf(
            row(0, 10f, writeSeq = 1L),
            row(0, 25f, writeSeq = 2L)
        )
        assertEquals(25f, VolumeStatsCalculator.getTodayVolume(completions, zone), 0.001f)
        assertEquals(25f, VolumeStatsCalculator.getWeeklyVolume(completions, zone), 0.001f)
    }

    @Test
    fun dailyAverageIsOverActiveDaysOnly() {
        val completions = listOf(row(0, 30f), row(2, 10f)) // two active days in last 30
        assertEquals(20f, VolumeStatsCalculator.getAverageDailyVolume(completions, zone), 0.001f)
    }

    @Test
    fun emptyCompletionsAreZero() {
        val empty = emptyList<DailyCompletion>()
        assertEquals(0f, VolumeStatsCalculator.getWeeklyVolume(empty, zone), 0.001f)
        assertEquals(0f, VolumeStatsCalculator.getAverageDailyVolume(empty, zone), 0.001f)
        assertEquals(0f, VolumeStatsCalculator.getTodayVolume(empty, zone), 0.001f)
    }
}
