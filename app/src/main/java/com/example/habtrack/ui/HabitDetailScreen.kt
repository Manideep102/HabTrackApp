package com.example.habtrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habtrack.data.DailyCompletion
import com.example.habtrack.data.HabitDatabase
import com.example.habtrack.data.HabitEntity
import com.example.habtrack.ui.theme.Obsidian
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Full-screen habit detail: hero number, last-7-days chart against the goal,
 * stat grid, and completion history — all read live from Room.
 */
@Composable
fun HabitDetailScreen(
    habit: HabitEntity,
    onBack: () -> Unit,
    onLog: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { HabitDatabase.getDatabase(context) }
    val completions by db.dailyCompletionDao()
        .getDailyCompletionsForHabit(habit.id)
        .collectAsState(initial = emptyList())

    // ── derive stats ──
    val zoneCal = Calendar.getInstance()
    fun startOfDay(daysAgo: Int): Long {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -daysAgo)
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun valueForDay(daysAgo: Int): Float {
        if (daysAgo == 0) return habit.currentValue
        val start = startOfDay(daysAgo)
        val end = startOfDay(daysAgo - 1)
        return completions.filter { it.dateTime in start until end }
            .maxOfOrNull { it.progressValue } ?: 0f
    }

    val last7 = (6 downTo 0).map { valueForDay(it) }
    val avg7 = if (last7.any { it > 0f }) last7.sum() / last7.count() else 0f
    val best = maxOf(completions.maxOfOrNull { it.progressValue } ?: 0f, habit.currentValue, habit.personalRecord)
    val last30Start = startOfDay(29)
    val completedLast30 = completions.count { it.dateTime >= last30Start && it.isCompleted } +
        (if (habit.currentValue >= habit.goalValue) 1 else 0)
    val consistency = ((completedLast30 / 30f) * 100).toInt().coerceIn(0, 100)

    val progress = (habit.currentValue / habit.goalValue).coerceIn(0f, 1f)
    val dateFmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    val history = completions.take(14)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian.Bg)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
        ) {
            // ── header ──
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Obsidian.Surface, RoundedCornerShape(12.dp))
                            .border(1.dp, Obsidian.Stroke, RoundedCornerShape(12.dp))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Obsidian.TextMid,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(habit.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Obsidian.TextHi)
                        if (habit.autoSyncEnabled) {
                            Text(
                                "HEALTH CONNECT",
                                fontSize = 9.sp,
                                letterSpacing = 1.4.sp,
                                fontWeight = FontWeight.Medium,
                                color = Obsidian.Accent
                            )
                        }
                    }
                }
            }

            // ── hero number ──
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text(
                        "LOGGED TODAY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Obsidian.TextLow
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            formatValue(habit.currentValue),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            color = Obsidian.TextHi,
                            lineHeight = 52.sp
                        )
                        Text(
                            " / ${formatValue(habit.goalValue)} ${habit.unit}",
                            fontSize = 16.sp,
                            color = Obsidian.TextLow,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(99.dp))
                                .background(Obsidian.Accent)
                        )
                    }
                }
            }

            // ── last 7 days chart ──
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Obsidian.Surface)
                        .border(1.dp, Obsidian.Stroke, RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "LAST 7 DAYS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Obsidian.TextLow
                        )
                        Row {
                            Text("goal ", fontSize = 11.sp, color = Obsidian.TextLow)
                            Text(
                                "${formatValue(habit.goalValue)} ${habit.unit}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Obsidian.TextMid
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))

                    val chartMax = maxOf(habit.goalValue, last7.max()).coerceAtLeast(0.01f)
                    val goalFraction = (habit.goalValue / chartMax).coerceIn(0f, 1f)
                    val chartHeight = 110.dp

                    Box(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
                        // dashed-style goal line (solid faint line)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .offset(y = chartHeight * (1f - goalFraction))
                                .background(Color.White.copy(alpha = 0.14f))
                        )
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            last7.forEachIndexed { i, v ->
                                val frac = (v / chartMax).coerceIn(0.02f, 1f)
                                val isToday = i == 6
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(frac)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isToday) Obsidian.Accent
                                            else Color.White.copy(alpha = 0.1f)
                                        )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        (6 downTo 0).forEach { daysAgo ->
                            val c = Calendar.getInstance()
                            c.add(Calendar.DAY_OF_YEAR, -daysAgo)
                            val letter = SimpleDateFormat("E", Locale.getDefault())
                                .format(c.time).take(1)
                            Text(
                                letter,
                                fontSize = 9.sp,
                                letterSpacing = 1.sp,
                                color = if (daysAgo == 0) Obsidian.Accent else Obsidian.TextLow,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ── stat grid ──
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailStat("7-DAY AVG", formatValue(avg7), habit.unit, Modifier.weight(1f))
                    DetailStat("BEST", formatValue(best), habit.unit, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailStat("STREAK", "${habit.currentStreak}", "days", Modifier.weight(1f), accent = true)
                    DetailStat("CONSISTENCY", "$consistency", "%", Modifier.weight(1f))
                }
            }

            // ── history ──
            item {
                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    "HISTORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = Obsidian.TextLow,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (history.isEmpty()) {
                    Text(
                        "No history yet — log progress to start building your record.",
                        fontSize = 13.sp,
                        color = Obsidian.TextLow,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            if (history.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Obsidian.Surface)
                            .border(1.dp, Obsidian.Stroke, RoundedCornerShape(18.dp))
                    ) {
                        history.forEachIndexed { i, entry ->
                            HistoryRow(entry, habit.unit, dateFmt)
                            if (i < history.lastIndex) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }

        // ── log CTA ──
        Box(modifier = Modifier.padding(20.dp).navigationBarsPadding()) {
            Button(
                onClick = onLog,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Obsidian.Accent,
                    contentColor = Obsidian.Bg
                )
            ) {
                Text(
                    if (habit.autoSyncEnabled) "VIEW SYNCED PROGRESS" else "LOG PROGRESS",
                    letterSpacing = 2.sp, fontWeight = FontWeight.Bold, fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun DetailStat(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Obsidian.Surface)
            .border(1.dp, Obsidian.Stroke, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = if (accent) Obsidian.Accent else Obsidian.TextHi
            )
            Text(
                " $unit",
                fontSize = 12.sp,
                color = Obsidian.TextLow,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        Text(
            label,
            fontSize = 9.sp,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Medium,
            color = Obsidian.TextLow
        )
    }
}

@Composable
private fun HistoryRow(entry: DailyCompletion, unit: String, fmt: SimpleDateFormat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(
                    if (entry.isCompleted) Obsidian.Accent else Obsidian.TextLow,
                    CircleShape
                )
        )
        Text(
            fmt.format(Date(entry.dateTime)),
            fontSize = 12.5.sp,
            color = Obsidian.TextMid,
            modifier = Modifier.weight(1f)
        )
        Text(
            "${formatValue(entry.progressValue)} $unit",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Obsidian.TextHi
        )
    }
}

/** 6.0 → "6", 6.4 → "6.4" */
internal fun formatValue(v: Float): String =
    if (v % 1f == 0f) v.toInt().toString() else String.format(Locale.getDefault(), "%.1f", v)
