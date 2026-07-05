package com.example.habtrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habtrack.ui.theme.Obsidian
import com.example.habtrack.utils.HabitCorrelationCalculator

@Composable
fun CorrelationCard(correlation: HabitCorrelationCalculator.HabitCorrelation) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Obsidian.Surface)
            .border(1.dp, Obsidian.Stroke, RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header: Habit pair and correlation strength
        Column {
            Text(
                text = "${correlation.habit1Name} ↔ ${correlation.habit2Name}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Obsidian.TextHi
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = correlation.interpretation.uppercase(),
                fontSize = 9.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Medium,
                color = getCorrelationColor(correlation.correlationScore)
            )
        }

        // Correlation strength bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LinearProgressIndicator(
                progress = { (correlation.correlationScore + 1f) / 2f },
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = getCorrelationColor(correlation.correlationScore),
                trackColor = Color.White.copy(alpha = 0.06f)
            )
            Text(
                text = "%.2f".format(correlation.correlationScore),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Obsidian.TextMid,
                modifier = Modifier.width(42.dp)
            )
        }

        // Interpretation text
        Text(
            text = getCorrelationExplanation(correlation.correlationScore),
            fontSize = 12.sp,
            color = Obsidian.TextLow,
            lineHeight = 17.sp
        )
    }
}

@Composable
fun CorrelationGridSection(correlations: List<HabitCorrelationCalculator.HabitCorrelation>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SectionLabel("Habit correlations")

        if (correlations.isEmpty()) {
            Text(
                text = "Not enough data to analyze correlations yet.",
                fontSize = 13.sp,
                color = Obsidian.TextLow,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            correlations.forEach { correlation ->
                CorrelationCard(correlation)
            }
        }
    }
}

@Composable
fun CorrelationClusterCard(clusterName: String, habits: List<Pair<Int, String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Obsidian.Surface)
            .border(1.dp, Obsidian.Stroke, RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "HABIT CLUSTER",
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.Medium,
            color = Obsidian.Accent
        )
        Text(
            text = clusterName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Obsidian.TextHi
        )
        Text(
            text = "These ${habits.size} habits tend to be completed together",
            fontSize = 12.sp,
            color = Obsidian.TextLow,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun CorrelationClusterGrid(clusters: Map<String, List<Pair<Int, String>>>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SectionLabel("Habit clusters")

        if (clusters.isEmpty()) {
            Text(
                text = "No habit clusters found yet.",
                fontSize = 13.sp,
                color = Obsidian.TextLow,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            clusters.forEach { (clusterName, habits) ->
                CorrelationClusterCard(clusterName, habits)
            }
        }
    }
}

/**
 * Correlation strength color — Obsidian palette (teal for positive,
 * gray for neutral, muted rose for negative; no traffic-light greens).
 */
fun getCorrelationColor(score: Float): Color {
    return when {
        score > 0.3f -> Obsidian.Accent                  // positive — teal
        score > 0f -> Obsidian.Accent.copy(alpha = 0.6f) // weak positive
        score > -0.3f -> Obsidian.TextLow                // neutral / weak negative
        else -> Color(0xFFF2B5B5)                        // negative — muted rose
    }
}

/**
 * Get explanation text for correlation score
 */
fun getCorrelationExplanation(score: Float): String {
    return when {
        score > 0.7f -> "These habits are very strongly connected. Completing one makes the other very likely."
        score > 0.5f -> "These habits are strongly connected. They're often completed together."
        score > 0.3f -> "These habits show a moderate positive relationship. There's a pattern here."
        score > 0.1f -> "These habits have a weak positive connection. Some overlap in your completion."
        score > -0.1f -> "No meaningful relationship between these habits."
        score > -0.3f -> "These habits show a weak negative relationship. Slight conflict."
        score > -0.5f -> "These habits are moderately negatively correlated. Consider balancing them."
        score > -0.7f -> "These habits are strongly negatively correlated. Difficult to do both well."
        else -> "These habits are almost opposites in your routine. Very challenging to balance."
    }
}
