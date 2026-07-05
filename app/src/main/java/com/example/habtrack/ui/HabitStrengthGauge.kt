package com.example.habtrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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

/**
 * HabitStrengthGauge: Displays habit strength as a circular progress indicator
 */
@Composable
fun HabitStrengthGauge(strength: HabitStrength) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Obsidian.Surface)
            .border(1.dp, Obsidian.Stroke, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(strength.name, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Obsidian.TextHi)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    strength.strengthLevel.uppercase(),
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp,
                    color = Obsidian.Accent,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Column {
                        Text("SCORE", fontSize = 8.5.sp, letterSpacing = 1.sp, color = Obsidian.TextLow)
                        Text("${strength.strengthScore.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Obsidian.TextHi)
                    }
                    Column {
                        Text("STREAK", fontSize = 8.5.sp, letterSpacing = 1.sp, color = Obsidian.TextLow)
                        Text("${strength.currentStreak}d", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Obsidian.TextHi)
                    }
                    Column {
                        Text("TOTAL", fontSize = 8.5.sp, letterSpacing = 1.sp, color = Obsidian.TextLow)
                        Text("${strength.totalCompletions}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Obsidian.TextHi)
                    }
                }
            }

            Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { strength.strengthScore / 100f },
                    modifier = Modifier.size(88.dp),
                    color = Obsidian.Accent,
                    trackColor = Color.White.copy(alpha = 0.06f),
                    strokeWidth = 7.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${strength.strengthScore.toInt()}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Obsidian.TextHi
                    )
                    Text("PTS", fontSize = 8.sp, letterSpacing = 1.2.sp, color = Obsidian.TextLow)
                }
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
            Text("No habits yet. Create one to get started!", color = Obsidian.TextLow)
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        strengths.forEach { strength ->
            HabitStrengthGauge(strength = strength)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
