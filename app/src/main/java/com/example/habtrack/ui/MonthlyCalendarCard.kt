package com.example.habtrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habtrack.data.HabitEntity
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
    val habitColor = try {
        Color(android.graphics.Color.parseColor(habit.colorHex))
    } catch (e: Exception) {
        Color(0xFF6366F1)
    }

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
            // Header
            Text(
                text = "${habit.name} - ${currentMonth.month.toString().take(3)} ${currentMonth.year}",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )

            // Day of week headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
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
                        .height(40.dp),
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
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    isFutureDate -> Color(0xFFE5E7EB) // Future dates - light gray
                    isCompleted -> habitColor // Fully completed
                    progress > 0f -> habitColor.copy(alpha = 0.3f) // Partially completed
                    else -> Color(0xFFF5F5F5) // Not completed
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Show triangle for partial completion (only for past dates)
        if (!isFutureDate && progress > 0f && !isCompleted && progress < 1f) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val triangleSize = size.width / 2
                val triangleColor = habitColor

                // Draw triangle in top-right corner
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(size.width - triangleSize, 0f)
                    lineTo(size.width, triangleSize)
                    close()
                }
                drawPath(path, color = triangleColor)
            }
        }

        Text(
            text = day.toString(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isFutureDate) Color(0xFFB0B0B0) else if (isCompleted) Color.White else if (progress > 0f) habitColor else Color.Gray,
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
            color = Color.Gray,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "📅 Monthly Progress",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )

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
