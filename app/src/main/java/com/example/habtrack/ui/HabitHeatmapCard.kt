package com.example.habtrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habtrack.utils.HabitHeatmapCalculator

@Composable
fun HeatmapCell(
    cell: HabitHeatmapCalculator.HeatmapCell,
    modifier: Modifier = Modifier
) {
    val intensity = HabitHeatmapCalculator.getIntensityLevel(
        if (cell.isCompleted) 75f else 0f
    )
    val colorHex = HabitHeatmapCalculator.getHeatmapColor(cell.isCompleted, intensity)
    
    val color = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color.LightGray
    }

    Box(
        modifier = modifier
            .size(14.dp)
            .background(color, RoundedCornerShape(2.dp))
    )
}

@Composable
fun HabitHeatmap(heatmapData: List<HabitHeatmapCalculator.HeatmapCell>) {
    if (heatmapData.isEmpty()) {
        Text(
            text = "No completion data available",
            fontSize = 14.sp,
            color = Color.Gray,
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
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.width(20.dp)
            )
            for (i in 1..6) {
                Text(
                    text = HabitHeatmapCalculator.getDayOfWeekAbbr(i),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
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
            // Header with habit name
            Text(
                text = habitName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBox(
                    label = "Current Streak",
                    value = "$currentStreak days",
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    label = "Longest Streak",
                    value = "$longestStreak days",
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Less", fontSize = 10.sp, color = Color.Gray)
                
                listOf("#F0F0F0", "#C6E48B", "#7BC96F", "#239A3B", "#196127").forEach { colorHex ->
                    val color = try {
                        Color(android.graphics.Color.parseColor(colorHex))
                    } catch (e: Exception) {
                        Color.LightGray
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color, RoundedCornerShape(1.dp))
                    )
                }
                
                Text("More", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .padding(0.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
fun HeatmapGrid(heatmapList: List<Pair<String, HabitHeatmapData>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "🔥 Completion Heatmap",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

        if (heatmapList.isEmpty()) {
            Text(
                text = "No habits tracked yet",
                fontSize = 14.sp,
                color = Color.Gray,
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
