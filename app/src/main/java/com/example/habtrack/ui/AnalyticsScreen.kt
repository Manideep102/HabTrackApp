package com.example.habtrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habtrack.ui.theme.Obsidian

/** Tracked uppercase section label — the Obsidian house style. */
@Composable
fun SectionLabel(text: String, color: Color = Obsidian.TextLow, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
    )
}

/** Standard Obsidian card container. */
@Composable
fun ObsidianCard(
    modifier: Modifier = Modifier,
    corner: Int = 18,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(corner.dp))
            .background(Obsidian.Surface)
            .border(1.dp, Obsidian.Stroke, RoundedCornerShape(corner.dp))
            .padding(18.dp),
        content = content
    )
}

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
            .background(Obsidian.Bg)
            .padding(horizontal = 20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionLabel("Last 30 days")
            Spacer(modifier = Modifier.height(4.dp))
            Text("analytics", style = MaterialTheme.typography.headlineMedium, color = Obsidian.TextHi)
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Stats Overview Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Active habits",
                    value = totalHabits.value.toString(),
                    modifier = Modifier.weight(1f),
                    color = Obsidian.TextHi
                )
                StatCard(
                    title = "Completion",
                    value = "${completionRate.value.toInt()}%",
                    modifier = Modifier.weight(1f),
                    color = Obsidian.Accent
                )
                StatCard(
                    title = "Avg progress",
                    value = "${averageProgress.value.toInt()}%",
                    modifier = Modifier.weight(1f),
                    color = Obsidian.TextHi
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Overall Progress Card — glow hero
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF14181E), Color(0xFF0F1216))))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Obsidian.Accent.copy(alpha = 0.14f), Color.Transparent),
                            center = Offset(0.15f, 0f), radius = 900f
                        )
                    )
                    .border(1.dp, Obsidian.AccentBorder, RoundedCornerShape(24.dp))
                    .padding(22.dp)
            ) {
                Column {
                    SectionLabel("Overall progress")
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${averageProgress.value.toInt()}",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = Obsidian.TextHi,
                            lineHeight = 48.sp
                        )
                        Text(
                            "%",
                            fontSize = 22.sp,
                            color = Obsidian.TextLow,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((averageProgress.value / 100f).coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(99.dp))
                                .background(Obsidian.Accent)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }

        // AI Insights Section
        item {
            AiInsightsCard(
                state = insightsState.value,
                onGenerate = { viewModel.generateInsights(context) }
            )
            Spacer(modifier = Modifier.height(28.dp))
        }

        // Individual Habit Progress
        item {
            SectionLabel("Habit breakdown")
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(habitProgress.value) { habit ->
            HabitProgressCard(habit = habit)
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Habit Strength Section
        item {
            Spacer(modifier = Modifier.height(24.dp))
            SectionLabel("Habit strength")
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
            .clip(RoundedCornerShape(16.dp))
            .background(Obsidian.Surface)
            .border(1.dp, Obsidian.Stroke, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            title.uppercase(),
            fontSize = 9.sp,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Medium,
            color = Obsidian.TextLow,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AiInsightsCard(state: InsightsState, onGenerate: () -> Unit) {
    ObsidianCard(modifier = Modifier.fillMaxWidth(), corner = 20) {
        SectionLabel("AI insights", color = Obsidian.Accent)
        Spacer(modifier = Modifier.height(12.dp))

        when (state) {
            is InsightsState.Idle -> {
                Text(
                    "Get a quick, personalized read on your streaks and habit patterns from Claude.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = Obsidian.TextMid
                )
                Spacer(modifier = Modifier.height(16.dp))
                InsightsButton("Generate insights", onGenerate)
            }
            is InsightsState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Obsidian.Accent,
                        trackColor = Color.White.copy(alpha = 0.06f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Thinking about your habits...", fontSize = 13.sp, color = Obsidian.TextMid)
                }
            }
            is InsightsState.Success -> {
                Text(state.text, fontSize = 14.sp, lineHeight = 21.sp, color = Obsidian.TextHi)
                Spacer(modifier = Modifier.height(16.dp))
                InsightsButton("Refresh insights", onGenerate)
            }
            is InsightsState.Error -> {
                Text(state.message, fontSize = 13.sp, color = Color(0xFFF2B5B5))
                Spacer(modifier = Modifier.height(16.dp))
                InsightsButton("Try again", onGenerate)
            }
        }
    }
}

@Composable
private fun InsightsButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Obsidian.Accent, contentColor = Obsidian.Bg)
    ) {
        Text(label.uppercase(), letterSpacing = 2.sp, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
fun HabitProgressCard(habit: HabitProgress) {
    ObsidianCard(modifier = Modifier.fillMaxWidth(), corner = 16) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                habit.name,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Obsidian.TextHi,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${habit.progress.toInt()}%",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Obsidian.TextMid
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color.White.copy(alpha = 0.06f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((habit.progress / 100f).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(99.dp))
                    .background(Obsidian.Accent)
            )
        }
    }
}
