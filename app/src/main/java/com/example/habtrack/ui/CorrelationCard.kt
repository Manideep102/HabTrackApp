package com.example.habtrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habtrack.utils.HabitCorrelationCalculator

@Composable
fun CorrelationCard(correlation: HabitCorrelationCalculator.HabitCorrelation) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Habit pair and correlation strength
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${correlation.habit1Name} ↔ ${correlation.habit2Name}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Text(
                        text = correlation.interpretation,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Text(
                    text = correlation.emoji,
                    fontSize = 24.sp
                )
            }

            // Correlation strength bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { (correlation.correlationScore + 1f) / 2f },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp),
                    color = getCorrelationColor(correlation.correlationScore),
                    trackColor = Color(0xFFE0E0E0)
                )
                Text(
                    text = "${"%.2f".format(correlation.correlationScore)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(45.dp)
                )
            }

            // Interpretation text
            Text(
                text = getCorrelationExplanation(correlation.correlationScore),
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun CorrelationGridSection(correlations: List<HabitCorrelationCalculator.HabitCorrelation>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "🔗 Habit Correlations",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        if (correlations.isEmpty()) {
            Text(
                text = "Not enough data to analyze correlations. Track more habits to see patterns!",
                fontSize = 14.sp,
                color = Color.Gray,
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Habit Cluster",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
            Text(
                text = clusterName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Text(
                text = "These ${habits.size} habits tend to be completed together",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun CorrelationClusterGrid(clusters: Map<String, List<Pair<Int, String>>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "🎯 Habit Clusters",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        if (clusters.isEmpty()) {
            Text(
                text = "No habit clusters found. Build stronger habits to discover patterns!",
                fontSize = 14.sp,
                color = Color.Gray,
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
 * Get color for correlation strength visualization
 */
fun getCorrelationColor(score: Float): Color {
    return when {
        score > 0.6f -> Color(0xFF4CAF50) // Strong positive - green
        score > 0.3f -> Color(0xFF8BC34A) // Moderate positive - light green
        score > 0f -> Color(0xFFFFC107) // Weak positive - yellow
        score > -0.3f -> Color(0xFFFF9800) // Weak negative - orange
        score > -0.6f -> Color(0xFFF44336) // Moderate negative - red
        else -> Color(0xFFB71C1C) // Strong negative - dark red
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
