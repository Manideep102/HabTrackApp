package com.example.habtrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt

/**
 * HabitStrengthGauge: Displays habit strength as a circular progress indicator
 */
@Composable
fun HabitStrengthGauge(strength: HabitStrength) {
    val habitColor = try {
        Color(strength.color.toColorInt())
    } catch (e: Exception) {
        Color(0xFF6366F1)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(strength.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(strength.strengthLevel, fontSize = 12.sp, color = habitColor, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text("Score", fontSize = 10.sp, color = Color.Gray)
                        Text("${strength.strengthScore.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Streak", fontSize = 10.sp, color = Color.Gray)
                        Text("${strength.currentStreak}d", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Total", fontSize = 10.sp, color = Color.Gray)
                        Text("${strength.totalCompletions}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { strength.strengthScore / 100f },
                    modifier = Modifier.size(90.dp),
                    color = habitColor,
                    trackColor = habitColor.copy(alpha = 0.15f),
                    strokeWidth = 6.dp
                )
                Text(
                    strength.strengthEmoji,
                    fontSize = 32.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

/**
 * HabitStrengthGrid: Displays all habit strengths in a grid
 */
@Composable
fun HabitStrengthGrid(strengths: List<HabitStrength>) {
    if (strengths.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No habits yet. Create one to get started!", color = Color.Gray)
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        strengths.forEach { strength ->
            HabitStrengthGauge(strength = strength)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
