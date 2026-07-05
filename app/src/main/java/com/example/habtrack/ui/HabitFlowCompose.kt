package com.example.habtrack.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habtrack.data.HabitEntity

// --- UI COMPONENTS ---

@Composable
fun HabitCard(
    habit: HabitEntity,
    onClick: () -> Unit
) {
    val progress = (habit.currentValue / habit.goalValue).coerceIn(0f, 1f)
    val isDone = progress >= 1f
    // Parse color from hex string stored in DB, fallback to Indigo
    val cardColor = try {
        Color(android.graphics.Color.parseColor(habit.colorHex))
    } catch (e: Exception) {
        Color(0xFF6366F1)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(cardColor, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White)
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .weight(1f)
            ) {
                Text(habit.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "${habit.currentValue} / ${habit.goalValue} ${habit.unit}",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isDone) Color(0xFF10B981) else Color(0xFFE2E8F0),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitLogBottomSheet(
    habit: HabitEntity,
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit
) {
    var logValue by remember { mutableFloatStateOf(habit.currentValue) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(habit.name, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("Manual Entry", color = Color.Gray, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { logValue = (logValue - 0.5f).coerceAtLeast(0f) }) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(32.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
                    Text(logValue.toString(), fontSize = 48.sp, fontWeight = FontWeight.Black)
                    Text(habit.unit.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                }

                IconButton(onClick = { logValue += 0.5f }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onSave(logValue) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
            ) {
                Text("Confirm Log", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HabitFlowApp(viewModel: HabitViewModel) {
    var selectedHabit by remember { mutableStateOf<HabitEntity?>(null) }
    val habits by viewModel.habitListState.collectAsState()

    val totalProgress = if (habits.isEmpty()) 0f else {
        habits.sumOf { (it.currentValue / it.goalValue).toDouble() }.toFloat() / habits.size
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Example: Adding a quick habit for testing
                    viewModel.addNewHabit("New Habit", 5f, "units", "#4F46E5")
                },
                containerColor = Color(0xFF4F46E5),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header
            Text("HabitFlow", fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("Today's Progress", color = Color.Gray, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Overview Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text("Daily Average", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("${(totalProgress * 100).toInt()}%", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.weight(1f))
                    LinearProgressIndicator(
                        progress = { totalProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn {
                item {
                    Text("Checklist", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
                items(habits, key = { it.id }) { habit ->
                    HabitCard(habit = habit) {
                        selectedHabit = habit
                    }
                }
            }
        }
    }

    // Bottom Sheet Logic
    selectedHabit?.let { habit ->
        HabitLogBottomSheet(
            habit = habit,
            onDismiss = { selectedHabit = null },
            onSave = { newValue ->
                viewModel.updateHabitProgress(habit, newValue)
                selectedHabit = null
            }
        )
    }
}