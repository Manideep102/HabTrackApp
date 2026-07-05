package com.example.habtrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt

/**
 * Analytics Screen: Displays habit statistics, completion rates, and progress charts
 */
@Composable
fun AnalyticsScreen(viewModel: HabitViewModel) {
    val completionRate = viewModel.completionRate.collectAsState()
    val averageProgress = viewModel.averageProgress.collectAsState()
    val totalHabits = viewModel.totalHabits.collectAsState()
    val habitProgress = viewModel.habitProgressData.collectAsState()
    val habitStrength = viewModel.habitStrengthData.collectAsState()
    val volumeStats = viewModel.volumeStatsData.collectAsState()
    val habits = viewModel.habitListState.collectAsState()
    val heatmapData = viewModel.heatmapData.collectAsState()
    val correlationData = viewModel.correlationData.collectAsState()
    val clusterData = viewModel.clusterData.collectAsState()
    val monthlyCompletions = viewModel.monthlyCompletionsData.collectAsState()
    val insightsState = viewModel.insightsState.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Analytics", fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("Your Habit Progress", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Stats Overview Cards
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Habits",
                    value = totalHabits.value.toString(),
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF6366F1)
                )
                StatCard(
                    title = "Completion",
                    value = "${completionRate.value.toInt()}%",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF10B981)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            StatCard(
                title = "Avg Progress",
                value = "${averageProgress.value.toInt()}%",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                color = Color(0xFFF59E0B)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Overall Progress Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        "Overall Progress",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "${averageProgress.value.toInt()}%",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { averageProgress.value / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // AI Insights Section
        item {
            AiInsightsCard(
                state = insightsState.value,
                onGenerate = { viewModel.generateInsights(context) }
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Individual Habit Progress
        item {
            Text("Habit Breakdown", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(habitProgress.value) { habit ->
            HabitProgressCard(habit = habit)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Habit Strength Section
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Habit Strength", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            HabitStrengthGrid(strengths = habitStrength.value)
        }

        // Volume Statistics Section
        item {
            Spacer(modifier = Modifier.height(24.dp))
            VolumeStatsGrid(volumeStatsList = volumeStats.value)
        }

        // Completion Heatmap Section
        item {
            Spacer(modifier = Modifier.height(24.dp))
            HeatmapGrid(heatmapList = heatmapData.value.mapIndexed { index, data ->
                Pair("${index}_${data.habitId}", data)
            })
        }

        // Monthly Calendar Section
        item {
            Spacer(modifier = Modifier.height(24.dp))
            MonthlyCalendarGrid(habits = habits.value, monthlyData = monthlyCompletions.value)
        }

        // Habit Correlations Section
        item {
            Spacer(modifier = Modifier.height(24.dp))
            CorrelationGridSection(correlations = correlationData.value)
        }

        // Habit Clusters Section
        item {
            Spacer(modifier = Modifier.height(24.dp))
            CorrelationClusterGrid(clusters = clusterData.value)
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color
) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            fontSize = 13.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            value,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AiInsightsCard(state: InsightsState, onGenerate: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✨", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Insights", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))

            when (state) {
                is InsightsState.Idle -> {
                    Text(
                        "Get a quick, personalized read on your streaks and habit patterns from Claude.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onGenerate,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Generate Insights")
                    }
                }
                is InsightsState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Thinking about your habits...", fontSize = 13.sp, color = Color(0xFF64748B))
                    }
                }
                is InsightsState.Success -> {
                    Text(state.text, fontSize = 14.sp, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onGenerate,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Refresh Insights")
                    }
                }
                is InsightsState.Error -> {
                    Text(state.message, fontSize = 13.sp, color = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onGenerate,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

@Composable
fun HabitProgressCard(habit: HabitProgress) {
    val habitColor = try {
        Color(habit.color.toColorInt())
    } catch (e: Exception) {
        Color(0xFF6366F1)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    habit.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${habit.progress.toInt()}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = habitColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { habit.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = habitColor,
                trackColor = habitColor.copy(alpha = 0.15f)
            )
        }
    }
}
