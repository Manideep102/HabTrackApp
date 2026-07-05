package com.example.habtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.habtrack.data.HabitEntity
import com.example.habtrack.data.HabitDatabase
import com.example.habtrack.data.HabitRepository
import com.example.habtrack.health.HealthConnectAvailability
import com.example.habtrack.health.HealthConnectManager
import com.example.habtrack.health.HealthMetric
import com.example.habtrack.ui.HabitViewModel
import com.example.habtrack.ui.AnalyticsScreen
import com.example.habtrack.ui.SettingsScreen
import com.example.habtrack.workers.HabitResetScheduler
import com.example.habtrack.notifications.HabitNotificationManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create notification channel
        HabitNotificationManager.createNotificationChannel(this)

        // Schedule the daily habit reset task
        HabitResetScheduler.scheduleHabitReset(this)

        setContent {
            MaterialTheme {
                // Initialize the repository and create ViewModel with factory
                val database = HabitDatabase.getDatabase(this@MainActivity)
                val repository = HabitRepository(database.habitDao(), database.dailyCompletionDao())
                val habitViewModel = HabitViewModel(repository)
                HabTrackApp(viewModel = habitViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsSheet(
    habit: HabitEntity,
    onDismiss: () -> Unit,
    onSave: (String, Boolean) -> Unit,
    onSyncSave: (Boolean, HealthMetric?) -> Unit,
    healthConnectAvailable: Boolean
) {
    var reminderTime by remember { mutableStateOf(habit.reminderTime) }
    var reminderEnabled by remember { mutableStateOf(habit.reminderEnabled) }
    var autoSyncEnabled by remember { mutableStateOf(habit.autoSyncEnabled) }
    var selectedMetric by remember {
        mutableStateOf(HealthMetric.entries.find { it.name == habit.autoSyncMetric } ?: HealthMetric.STEPS)
    }
    var metricMenuExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp).fillMaxWidth()) {
            Text("Reminder Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            // Toggle reminder
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable Reminders", fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { reminderEnabled = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time picker (simplified - using text input)
            OutlinedTextField(
                value = reminderTime,
                onValueChange = { reminderTime = it },
                label = { Text("Reminder Time (HH:mm)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("09:00") },
                enabled = reminderEnabled
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Auto-Sync from Health Connect", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            if (!healthConnectAvailable) {
                Text(
                    "Connect Health Connect in Settings first.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sync this habit automatically", fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = autoSyncEnabled,
                    onCheckedChange = { autoSyncEnabled = it },
                    enabled = healthConnectAvailable
                )
            }

            if (autoSyncEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = metricMenuExpanded,
                    onExpandedChange = { metricMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedMetric.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Metric") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = metricMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = metricMenuExpanded,
                        onDismissRequest = { metricMenuExpanded = false }
                    ) {
                        HealthMetric.entries.forEach { metric ->
                            DropdownMenuItem(
                                text = { Text(metric.displayName) },
                                onClick = {
                                    selectedMetric = metric
                                    metricMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    onSave(reminderTime, reminderEnabled)
                    onSyncSave(autoSyncEnabled, if (autoSyncEnabled) selectedMetric else null)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text("Save") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitSheet(
    onDismiss: () -> Unit,
    onSave: (String, Float, String, Float) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var increment by remember { mutableStateOf("1") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp).fillMaxWidth()) {
            Text("New Habit", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    label = { Text("Goal") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = increment,
                onValueChange = { increment = it },
                label = { Text("Increment") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = { Text("Amount added per click") }
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { 
                    onSave(
                        name, 
                        goal.toFloatOrNull() ?: 1f, 
                        unit,
                        increment.toFloatOrNull() ?: 1f
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text("Save") }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitCard(habit: HabitEntity, onClick: () -> Unit, onLongClick: () -> Unit, onReminderClick: () -> Unit = {}) {
    val progress = (habit.currentValue / habit.goalValue).coerceIn(0f, 1f)
    val themeColor = try {
        Color(habit.colorHex.toColorInt())
    } catch (_: Exception) {
        Color(0xFF6366F1)
    }
    val habitIcon = when (habit.iconName) {
        "favorite" -> Icons.Default.Favorite
        "directions_run" -> Icons.Default.DirectionsRun
        "bedtime" -> Icons.Default.Bedtime
        "local_fire_department" -> Icons.Default.LocalFireDepartment
        "self_improvement" -> Icons.Default.SelfImprovement
        "fitness_center" -> Icons.Default.FitnessCenter
        "directions_walk" -> Icons.Default.DirectionsWalk
        else -> Icons.Default.Bolt
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(themeColor, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(habitIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        if (habit.currentStreak >= 2) {
                            Box(
                                modifier = Modifier
                                    .offset(x = 6.dp, y = (-6).dp)
                                    .size(18.dp)
                                    .background(Color(0xFFFF6B00), RoundedCornerShape(9.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    habit.currentStreak.toString(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(habit.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${habit.currentValue} / ${habit.goalValue} ${habit.unit}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onReminderClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Habit settings",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    if (progress >= 1f) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(28.dp))
                    } else {
                        Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(28.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = themeColor,
                trackColor = themeColor.copy(alpha = 0.15f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitLogBottomSheet(habit: HabitEntity, onDismiss: () -> Unit, onSave: (Float, Long) -> Unit) {
    var value by remember { mutableFloatStateOf(habit.currentValue) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    val dateFormat = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
    val selectedDateString = dateFormat.format(java.util.Date(selectedDateMillis))
    
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .padding(bottom = 32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(habit.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (habit.autoSyncEnabled) {
                // Health Connect is authoritative for this habit's progress - no manual entry.
                Text(
                    "This habit syncs automatically from Health Connect.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "${habit.currentValue} / ${habit.goalValue} ${habit.unit}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            } else {
                // Date selector
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedDateString)
                }

                if (showDatePicker) {
                    DatePickerDialog(
                        selectedDateMillis = selectedDateMillis,
                        onDateSelected = { selectedDateMillis = it },
                        onDismiss = { showDatePicker = false }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { value = (value - habit.incrementValue).coerceAtLeast(0f) }) {
                        Icon(Icons.Default.Remove, null)
                    }
                    Text(
                        value.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    IconButton(onClick = { value += habit.incrementValue }) {
                        Icon(Icons.Default.Add, null)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Increment: +${habit.incrementValue} ${habit.unit}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onSave(value, selectedDateMillis) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update Progress")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = remember {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = selectedDateMillis
        DatePickerState(
            yearRange = 2020..2030,
            initialSelectedDateMillis = selectedDateMillis,
            initialDisplayMode = DisplayMode.Picker,
            locale = java.util.Locale.getDefault()
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Date") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                DatePicker(
                    state = datePickerState,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun HabTrackApp(viewModel: HabitViewModel) {
    var selectedHabit by remember { mutableStateOf<HabitEntity?>(null) }
    var habitToDelete by remember { mutableStateOf<HabitEntity?>(null) }
    var habitForReminder by remember { mutableStateOf<HabitEntity?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val habits by viewModel.habitListState.collectAsState()
    val averageProgress by viewModel.averageProgress.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var healthConnectAvailable by remember { mutableStateOf(false) }

    // Refresh auto-synced habits from Health Connect once per app session.
    LaunchedEffect(Unit) {
        try {
            val availability = HealthConnectManager(context).getAvailability()
            healthConnectAvailable = availability is HealthConnectAvailability.Available
            if (healthConnectAvailable) {
                viewModel.syncFromHealthConnect(context)
            }
        } catch (_: Exception) {
            healthConnectAvailable = false
        }
    }

    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
        return
    }

    // Get current day name
    val dayName = java.text.SimpleDateFormat("EEEE, MMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date())

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color(0xFFF8FAFC)).padding(horizontal = 16.dp, vertical = 16.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("HabTrack", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                        Text(dayName, fontSize = 12.sp, color = Color.Gray)
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFF64748B))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFF4F46E5),
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        text = { 
                            Text("Today", 
                                fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            ) 
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        text = { 
                            Text("Analytics", 
                                fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            ) 
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = { showAdd = true },
                    containerColor = Color(0xFF6366F1),
                    contentColor = Color.White,
                    modifier = Modifier.padding(16.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { p ->
        when (selectedTabIndex) {
            0 -> {
                // Today Tab - Habits View
                LazyColumn(
                    modifier = Modifier
                        .padding(p)
                        .fillMaxSize()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Daily Progress Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        listOf(Color(0xFF6366F1), Color(0xFF7C3AED))
                                    ),
                                    shape = RoundedCornerShape(28.dp)
                                )
                                .padding(24.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "DAILY PROGRESS",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "${averageProgress.toInt()}%",
                                            color = Color.White,
                                            fontSize = 54.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🔥", fontSize = 32.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                LinearProgressIndicator(
                                    progress = { averageProgress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                val completedCount = habits.count { it.currentValue >= it.goalValue }
                                val remaining = habits.size - completedCount
                                val quoteText = when {
                                    habits.isEmpty() -> "Add habits to get started"
                                    remaining == 0 -> "All habits complete! Great work today."
                                    remaining == 1 -> "1 habit left — finish strong!"
                                    else -> "$remaining habits left today"
                                }
                                Text(
                                    quoteText,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))
                        Text("Active Habits", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    items(habits) { habit ->
                        HabitCard(
                            habit = habit,
                            onClick = { selectedHabit = habit },
                            onLongClick = { habitToDelete = habit },
                            onReminderClick = { habitForReminder = habit }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
            1 -> {
                // Analytics Tab
                AnalyticsScreen(viewModel = viewModel)
            }
        }
    }

    if (showAdd) {
        AddHabitSheet(
            onDismiss = { showAdd = false },
            onSave = { n, g, u, inc ->
                viewModel.addNewHabit(n, g, u, "#4F46E5", inc)
                showAdd = false
            }
        )
    }

    selectedHabit?.let { h ->
        HabitLogBottomSheet(
            habit = h,
            onDismiss = { selectedHabit = null },
            onSave = { v, dateMillis ->
                viewModel.updateHabitProgress(h, v, dateMillis)
                selectedHabit = null
            }
        )
    }

    habitToDelete?.let { h ->
        AlertDialog(
            onDismissRequest = { habitToDelete = null },
            title = { Text("Delete Habit") },
            text = { Text("Delete '${h.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteHabit(h)
                    habitToDelete = null
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { habitToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    habitForReminder?.let { h ->
        ReminderSettingsSheet(
            habit = h,
            onDismiss = { habitForReminder = null },
            onSave = { time, enabled ->
                viewModel.setHabitReminder(h, time, enabled, context)
                habitForReminder = null
            },
            onSyncSave = { enabled, metric ->
                viewModel.setHabitAutoSync(h, enabled, metric)
            },
            healthConnectAvailable = healthConnectAvailable
        )
    }
}