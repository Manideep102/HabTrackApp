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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habtrack.data.HabitEntity
import com.example.habtrack.ui.theme.Obsidian
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MonthlyCalendarCard(
    habit: HabitEntity,
    dailyCompletions: List<Pair<Int, Boolean>>, // day of month to isCompleted
    dailyProgress: List<Pair<Int, Float>> // day of month to progress percentage
) {
    val currentMonth = YearMonth.now()
    val daysInMonth = currentMonth.lengthOfMonth()
    // Obsidian: single accent, not per-habit colors
    val habitColor = Obsidian.Accent

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
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = habit.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Obsidian.TextHi
            )
            Text(
                text = "${currentMonth.month.toString().take(3)} ${currentMonth.year}".uppercase(),
                fontSize = 9.sp,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.Medium,
                color = Obsidian.TextLow
            )
        }

        // Day of week headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Medium,
                        color = Obsidian.TextLow,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Days grid
        val firstDayOfWeek = LocalDate.of(currentMonth.year, currentMonth.month, 1)
            .dayOfWeek.value % 7 // 0 = Sunday

        val rows = (firstDayOfWeek + daysInMonth + 6) / 7

        for (row in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0..6) {
                    val dayIndex = row * 7 + col - firstDayOfWeek
                    val dayOfMonth = dayIndex + 1

                    if (dayOfMonth in 1..daysInMonth) {
                        val completionStatus = dailyCompletions.find { it.first == dayOfMonth }?.second
                        val progress = dailyProgress.find { it.first == dayOfMonth }?.second ?: 0f
                        val today = LocalDate.now()
                        val currentDate = LocalDate.of(currentMonth.year, currentMonth.month, dayOfMonth)
                        val isFutureDate = currentDate.isAfter(today)

                        DayCell(
                            day = dayOfMonth,
                            isCompleted = completionStatus ?: false,
                            progress = progress,
                            habitColor = habitColor,
                            isFutureDate = isFutureDate,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(
    day: Int,
    isCompleted: Boolean,
    progress: Float,
    habitColor: Color,
    isFutureDate: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(7.dp))
            .background(
                when {
                    isFutureDate -> Color.Transparent
                    isCompleted -> habitColor // Fully completed
                    progress > 0f -> habitColor.copy(alpha = 0.18f) // Partially completed
                    else -> Color.White.copy(alpha = 0.04f) // Not completed
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            fontSize = 11.sp,
            fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Medium,
            color = when {
                isFutureDate -> Obsidian.TextFaint
                isCompleted -> Obsidian.Bg
                progress > 0f -> habitColor
                else -> Obsidian.TextLow
            },
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MonthlyCalendarGrid(habits: List<HabitEntity>, monthlyData: Map<Int, Pair<List<Pair<Int, Boolean>>, List<Pair<Int, Float>>>>) {
    if (habits.isEmpty()) {
        Text(
            text = "No habits tracked yet",
            fontSize = 14.sp,
            color = Obsidian.TextLow,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SectionLabel("Monthly progress")

        habits.forEach { habit ->
            // Get real data from monthlyData map, or empty lists if not available
            val (completedDays, progressDays) = monthlyData[habit.id]
                ?: (emptyList<Pair<Int, Boolean>>() to emptyList<Pair<Int, Float>>())

            MonthlyCalendarCard(
                habit = habit,
                dailyCompletions = completedDays,
                dailyProgress = progressDays
            )
        }
    }
}
