package com.example.habtrack.utils

import com.example.habtrack.data.HabitEntity
import java.util.Calendar

/**
 * HabitStrengthCalculator: Calculates habit strength scores based on consistency
 * Strength grows with daily completion but decays gracefully when days are missed
 */
object HabitStrengthCalculator {

    /**
     * Calculates the strength score for a habit
     * Formula: Base score increases with consistency, decays gradually for missed days
     */
    fun calculateStrength(
        habit: HabitEntity,
        completedToday: Boolean
    ): Float {
        var newScore = habit.strengthScore

        if (completedToday) {
            // Increase strength by 5 points for daily completion (capped at 100)
            newScore = (newScore + 5f).coerceAtMost(100f)
        } else {
            // Gradual decay: lose 1 point per day missed (forgiving system)
            // Check if a day has passed since last completion
            val now = System.currentTimeMillis()
            val daysSinceCompletion = (now - habit.lastCompletedDate) / (24 * 60 * 60 * 1000)
            
            if (daysSinceCompletion > 0) {
                newScore = (newScore - (daysSinceCompletion * 1f)).coerceAtLeast(0f)
            }
        }

        return newScore
    }

    /**
     * Calculates the current streak (consecutive days of completion)
     */
    fun calculateStreak(
        habit: HabitEntity,
        completedToday: Boolean
    ): Int {
        return if (completedToday) {
            // Check if completion was today or yesterday
            val now = System.currentTimeMillis()
            val daysSinceLastCompletion = (now - habit.lastCompletedDate) / (24 * 60 * 60 * 1000)

            if (daysSinceLastCompletion <= 1) {
                // Streak continues
                habit.currentStreak + 1
            } else {
                // Streak broken, start new one
                1
            }
        } else {
            // Streak broken
            0
        }
    }

    /**
     * Gets a readable strength level description
     */
    fun getStrengthLevel(score: Float): String = when {
        score >= 80f -> "Legendary"
        score >= 60f -> "Strong"
        score >= 40f -> "Building"
        score >= 20f -> "Developing"
        else -> "Starting Out"
    }

    /**
     * Gets the emoji representation of strength level
     */
    fun getStrengthEmoji(score: Float): String = when {
        score >= 80f -> "🔥"
        score >= 60f -> "⭐"
        score >= 40f -> "💪"
        score >= 20f -> "🌱"
        else -> "🆕"
    }
}
