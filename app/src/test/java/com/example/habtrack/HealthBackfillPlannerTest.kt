package com.example.habtrack

import com.example.habtrack.utils.HealthBackfillPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HealthBackfillPlannerTest {

    private val zone: ZoneId = ZoneId.of("America/New_York")
    private val today: LocalDate = LocalDate.of(2026, 7, 9)

    private fun dateOf(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    @Test
    fun skipsExistingDaysAndToday_insertsMissingDays() {
        val d1 = today.minusDays(1)
        val d2 = today.minusDays(2)
        val d3 = today.minusDays(3)
        val history = mapOf(d1 to 6000f, d2 to 3000f, d3 to 8000f, today to 1000f)

        val rows = HealthBackfillPlanner.planRows(
            habitId = 7,
            goalValue = 5000f,
            history = history,
            existingDays = setOf(d2),      // already recorded → skip
            today = today,                 // today handled by the normal sync → skip
            zone = zone
        )

        // d2 (existing) and today are excluded; d1 and d3 remain.
        assertEquals(setOf(d1, d3), rows.map { dateOf(it.dateTime) }.toSet())
        rows.forEach { assertEquals(7, it.habitId) }
    }

    @Test
    fun completionThresholdUsesGoal() {
        val d1 = today.minusDays(1) // above goal
        val d2 = today.minusDays(2) // below goal
        val rows = HealthBackfillPlanner.planRows(
            habitId = 1,
            goalValue = 5000f,
            history = mapOf(d1 to 5000f, d2 to 4999f),
            existingDays = emptySet(),
            today = today,
            zone = zone
        )
        val byDate = rows.associateBy { dateOf(it.dateTime) }
        assertTrue(byDate.getValue(d1).isCompleted)
        assertFalse(byDate.getValue(d2).isCompleted)
        assertEquals(4999f, byDate.getValue(d2).progressValue)
    }

    @Test
    fun timestampFallsOnItsOwnDay() {
        val d = today.minusDays(10)
        val rows = HealthBackfillPlanner.planRows(
            habitId = 1,
            goalValue = 100f,
            history = mapOf(d to 50f),
            existingDays = emptySet(),
            today = today,
            zone = zone
        )
        assertEquals(d, dateOf(rows.single().dateTime))
    }

    @Test
    fun emptyHistoryProducesNoRows() {
        val rows = HealthBackfillPlanner.planRows(
            habitId = 1,
            goalValue = 100f,
            history = emptyMap(),
            existingDays = emptySet(),
            today = today,
            zone = zone
        )
        assertTrue(rows.isEmpty())
    }
}
