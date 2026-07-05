package com.example.habtrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.habtrack.utils.HabitHeatmapCalculator

@Composable
fun HeatmapCell(
    cell: HabitHeatmapCalculator.HeatmapCell,
    modifier: Modifier = Modifier
) {
    // Obsidian: completed = accent, empty = faint white
    val color = if (cell.isCompleted) Obsidian.Accent else Color.White.copy(alpha = 0.06f)

    Box(
        modifier = modifier
            .size(14.dp)
            .background(color, RoundedCornerShape(4.dp))
    )
}

@Composable
fun HabitHeatmap(heatmapData: List<HabitHeatmapCalculator.HeatmapCell>) {
    if (heatmapData.isEmpty()) {
        Text(
            text = "No completion data available",
            fontSize = 14.sp,
            color = Obsidian.TextLow,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    // Group by weeks
    val weeks = heatmapData.groupBy { it.weekOfYear }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header with day abbreviations
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "S",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Obsidian.TextLow,
                modifier = Modifier.width(20.dp)
            )
            for (i in 1..6) {
                Text(
                    text = HabitHeatmapCalculator.getDayOfWeekAbbr(i),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Obsidian.TextLow,
                    modifier = Modifier
                        .width(20.dp)
                        .padding(horizontal = 2.dp)
                )
            }
        }

        // Heatmap grid
        weeks.values.forEach { weekCells ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Days of week: 0=Sunday to 6=Saturday
                for (dayOfWeek in 0..6) {
                    val cell = weekCells.find { it.dayOfWeek == dayOfWeek }
                    if (cell != null) {
                        HeatmapCell(cell, Modifier.size(14.dp))
                    } else {
                        Box(modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HabitHeatmapCard(
    habitName: String,
    heatmapData: List<HabitHeatmapCalculator.HeatmapCell>,
    currentStreak: Int,
    longestStreak: Int,
    completionPercentage: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Obsidian.Surface)
            .border(1.dp, Obsidian.Stroke, RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header with habit name
        Text(
            text = habitName,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Obsidian.TextHi
        )

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatBox(
                label = "Streak",
                value = "$currentStreak d",
                modifier = Modifier.weight(1f)
            )
            StatBox(
                label = "Longest",
                value = "$longestStreak d",
                modifier = Modifier.weight(1f)
            )
            StatBox(
                label = "Completion",
                value = "${"%.0f".format(completionPercentage)}%",
                modifier = Modifier.weight(1f)
            )
        }

        // Heatmap grid
        HabitHeatmap(heatmapData)

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Less", fontSize = 10.sp, color = Obsidian.TextLow)

            listOf(0.06f, 0.25f, 0.5f, 0.75f, 1f).forEach { alpha ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            if (alpha <= 0.06f) Color.White.copy(alpha = 0.06f)
                            else Obsidian.Accent.copy(alpha = alpha),
                            RoundedCornerShape(3.dp)
                        )
                )
            }

            Text("More", fontSize = 10.sp, color = Obsidian.TextLow)
        }
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Obsidian.TextHi
        )
        Text(
            text = label.uppercase(),
            fontSize = 8.5.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Medium,
            color = Obsidian.TextLow
        )
    }
}

@Composable
fun HeatmapGrid(heatmapList: List<Pair<String, HabitHeatmapData>>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SectionLabel("Completion heatmap")

        if (heatmapList.isEmpty()) {
            Text(
                text = "No habits tracked yet",
                fontSize = 14.sp,
                color = Obsidian.TextLow,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            heatmapList.forEach { (_, data) ->
                HabitHeatmapCard(
                    habitName = data.habitName,
                    heatmapData = data.cells,
                    currentStreak = data.currentStreak,
                    longestStreak = data.longestStreak,
                    completionPercentage = data.completionPercentage
                )
            }
        }
    }
}
