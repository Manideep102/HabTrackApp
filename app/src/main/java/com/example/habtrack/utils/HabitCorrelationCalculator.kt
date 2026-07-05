package com.example.habtrack.utils

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Analyzes correlations between habit completion patterns.
 * Finds which habits tend to be completed together.
 */
object HabitCorrelationCalculator {

    /**
     * Represents a correlation between two habits
     */
    data class HabitCorrelation(
        val habit1Id: Int,
        val habit1Name: String,
        val habit2Id: Int,
        val habit2Name: String,
        val correlationScore: Float, // -1.0 to 1.0
        val interpretation: String,
        val emoji: String
    )

    /**
     * Calculates Pearson correlation coefficient between two sets of completion values
     * Returns a value between -1.0 and 1.0:
     * - 1.0 means perfect positive correlation (when one increases, so does the other)
     * - 0.0 means no correlation
     * - -1.0 means perfect negative correlation (when one increases, the other decreases)
     */
    fun calculatePearsonCorrelation(values1: List<Float>, values2: List<Float>): Float {
        if (values1.size < 2 || values2.size < 2 || values1.size != values2.size) {
            return 0f
        }

        val mean1 = values1.average()
        val mean2 = values2.average()

        var numerator = 0.0
        var denominator1 = 0.0
        var denominator2 = 0.0

        for (i in values1.indices) {
            val diff1 = values1[i] - mean1
            val diff2 = values2[i] - mean2

            numerator += diff1 * diff2
            denominator1 += diff1.pow(2)
            denominator2 += diff2.pow(2)
        }

        val denominator = sqrt(denominator1 * denominator2)
        return if (denominator > 0) {
            (numerator / denominator).toFloat().coerceIn(-1f, 1f)
        } else {
            0f
        }
    }

    /**
     * Get interpretation string for correlation score
     */
    fun getCorrelationInterpretation(score: Float): String {
        return when {
            score > 0.7f -> "Very Strong Positive"
            score > 0.5f -> "Strong Positive"
            score > 0.3f -> "Moderate Positive"
            score > 0.1f -> "Weak Positive"
            score > -0.1f -> "No Correlation"
            score > -0.3f -> "Weak Negative"
            score > -0.5f -> "Moderate Negative"
            score > -0.7f -> "Strong Negative"
            else -> "Very Strong Negative"
        }
    }

    /**
     * Get emoji representing correlation strength
     */
    fun getCorrelationEmoji(score: Float): String {
        return when {
            score > 0.6f -> "🔥" // Very strong positive
            score > 0.4f -> "💪" // Strong positive
            score > 0.2f -> "✅" // Moderate positive
            score > 0f -> "👍" // Weak positive
            score > -0.2f -> "😐" // No correlation
            score > -0.4f -> "👎" // Weak negative
            score > -0.6f -> "⚠️" // Strong negative
            else -> "❌" // Very strong negative
        }
    }

    /**
     * Find all significant correlations between habits
     * Only returns pairs with correlation > threshold
     */
    fun findSignificantCorrelations(
        habitCompletionData: Map<Int, List<Float>>,
        habitNames: Map<Int, String>,
        threshold: Float = 0.3f
    ): List<HabitCorrelation> {
        val correlations = mutableListOf<HabitCorrelation>()
        val habitIds = habitCompletionData.keys.toList()

        for (i in habitIds.indices) {
            for (j in (i + 1) until habitIds.size) {
                val id1 = habitIds[i]
                val id2 = habitIds[j]

                val values1 = habitCompletionData[id1] ?: emptyList()
                val values2 = habitCompletionData[id2] ?: emptyList()

                val score = calculatePearsonCorrelation(values1, values2)

                if (score.toDouble() > threshold) {
                    correlations.add(
                        HabitCorrelation(
                            habit1Id = id1,
                            habit1Name = habitNames[id1] ?: "Unknown",
                            habit2Id = id2,
                            habit2Name = habitNames[id2] ?: "Unknown",
                            correlationScore = score,
                            interpretation = getCorrelationInterpretation(score),
                            emoji = getCorrelationEmoji(score)
                        )
                    )
                }
            }
        }

        return correlations.sortedByDescending { it.correlationScore }
    }

    /**
     * Get top correlated habits for a specific habit
     */
    fun getTopCorrelatedHabits(
        habitId: Int,
        habitCompletionData: Map<Int, List<Float>>,
        habitNames: Map<Int, String>,
        topN: Int = 3
    ): List<HabitCorrelation> {
        val habitValues = habitCompletionData[habitId] ?: return emptyList()
        val correlations = mutableListOf<HabitCorrelation>()

        habitCompletionData.forEach { (otherId, otherValues) ->
            if (otherId != habitId) {
                val score = calculatePearsonCorrelation(habitValues, otherValues)

                correlations.add(
                    HabitCorrelation(
                        habit1Id = habitId,
                        habit1Name = habitNames[habitId] ?: "Unknown",
                        habit2Id = otherId,
                        habit2Name = habitNames[otherId] ?: "Unknown",
                        correlationScore = score,
                        interpretation = getCorrelationInterpretation(score),
                        emoji = getCorrelationEmoji(score)
                    )
                )
            }
        }

        return correlations.sortedByDescending { it.correlationScore }.take(topN)
    }

    /**
     * Categorize habits into clusters based on correlation
     * Returns groups of habits that are correlated with each other
     */
    fun clusterHabits(
        habitCompletionData: Map<Int, List<Float>>,
        habitNames: Map<Int, String>,
        threshold: Float = 0.5f
    ): Map<String, List<Pair<Int, String>>> {
        val clusters = mutableMapOf<String, MutableList<Pair<Int, String>>>()
        val processed = mutableSetOf<Int>()

        val habitIds = habitCompletionData.keys.toList()

        for (mainHabitId in habitIds) {
            if (mainHabitId in processed) continue

            val cluster = mutableListOf<Pair<Int, String>>()
            cluster.add(mainHabitId to (habitNames[mainHabitId] ?: "Unknown"))
            processed.add(mainHabitId)

            val mainHabitValues = habitCompletionData[mainHabitId] ?: continue

            for (otherId in habitIds) {
                if (otherId in processed) continue

                val otherValues = habitCompletionData[otherId] ?: continue
                val score = calculatePearsonCorrelation(mainHabitValues, otherValues)

                if (score >= threshold) {
                    cluster.add(otherId to (habitNames[otherId] ?: "Unknown"))
                    processed.add(otherId)
                }
            }

            if (cluster.size > 1) {
                val clusterName = cluster.joinToString(" + ") { it.second }
                clusters[clusterName] = cluster
            }
        }

        return clusters
    }
}
