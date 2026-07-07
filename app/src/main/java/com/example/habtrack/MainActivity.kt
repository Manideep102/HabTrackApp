package com.example.habtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habtrack.data.HabitEntity
import com.example.habtrack.data.HabitDatabase
import com.example.habtrack.data.HabitRepository
import com.example.habtrack.health.HealthConnectAvailability
import com.example.habtrack.health.HealthConnectManager
import com.example.habtrack.health.HealthMetric
import com.example.habtrack.ui.HabitViewModel
import com.example.habtrack.ui.AnalyticsScreen
import com.example.habtrack.ui.SettingsScreen
import com.example.habtrack.ui.HabitDetailScreen
import com.example.habtrack.ui.AddHabitScreen
import com.example.habtrack.ui.theme.HabTrackTheme
import com.example.habtrack.ui.theme.Obsidian
import com.example.habtrack.ui.theme.ThemeStore
import com.example.habtrack.workers.HabitResetScheduler
import com.example.habtrack.notifications.HabitNotificationManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Restore the user's chosen accent color
        ThemeStore.load(this)

        // Create notification channel
        HabitNotificationManager.createNotificationChannel(this)

        // Schedule the daily habit reset task
        HabitResetScheduler.scheduleHabitReset(this)

        setContent {
            HabTrackTheme {
                // Initialize the repository and create ViewModel with factory
                val database = HabitDatabase.getDatabase(this@MainActivity)
                val repository = HabitRepository(database.habitDao(), database.dailyCompletionDao())
                val habitViewModel = HabitViewModel(repository)
                HabTrackApp(viewModel = habitViewModel)
            }
        }
    }
}

// ── Obsidian building blocks ───────────────────────────────────

/** Tracked uppercase micro-label, e.g. "DAILY PROGRESS" */
@Composable
fun MicroLabel(text: String, color: Color = Obsidian.TextLow) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color)
}

/** Progress ring drawn with Canvas — accent arc on a faint track. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    ringWidth: Float = 9f,
    content: @Composable () -> Unit = {}
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = ringWidth.dp.toPx(), cap = StrokeCap.Round)
            val inset = ringWidth.dp.toPx() / 2
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(
                color = Color.White.copy(alpha = 0.07f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize, style = stroke
            )
            drawArc(
                color = Obsidian.Accent,
                startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize, style = stroke
            )
        }
        content()
    }
}

/** Thin 4dp progress bar with rounded caps. */
@Composable
fun ThinProgressBar(progress: Float, modifier: Modifier = Modifier, dimmed: Boolean = false) {
    Box(
        modifier = modifier
            .height(4.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(Color.White.copy(alpha = 0.06f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(99.dp))
                .background(if (dimmed) Obsidian.Accent.copy(alpha = 0.55f) else Obsidian.Accent)
        )
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

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Obsidian.Surface2) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp).fillMaxWidth()) {
            Text("Reminder settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Obsidian.TextHi)
            Spacer(modifier = Modifier.height(16.dp))

            // Toggle reminder
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable reminders", fontWeight = FontWeight.SemiBold, color = Obsidian.TextHi)
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { reminderEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Obsidian.Accent,
                        checkedThumbColor = Obsidian.Bg
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = reminderTime,
                onValueChange = { reminderTime = it },
                label = { Text("Reminder time (HH:mm)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("09:00") },
                enabled = reminderEnabled,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Obsidian.StrokeSoft)
            Spacer(modifier = Modifier.height(16.dp))

            MicroLabel("Auto-sync from Health Connect", color = Obsidian.Accent)
            Spacer(modifier = Modifier.height(4.dp))
            if (!healthConnectAvailable) {
                Text(
                    "Connect Health Connect in Settings first.",
                    fontSize = 12.sp,
                    color = Obsidian.TextLow
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sync this habit automatically", fontWeight = FontWeight.SemiBold, color = Obsidian.TextHi)
                Switch(
                    checked = autoSyncEnabled,
                    onCheckedChange = { autoSyncEnabled = it },
                    enabled = healthConnectAvailable,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Obsidian.Accent,
                        checkedThumbColor = Obsidian.Bg
                    )
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
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
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
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Obsidian.Accent, contentColor = Obsidian.Bg)
            ) { Text("SAVE", letterSpacing = 2.sp, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
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

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Obsidian.Surface2) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp).fillMaxWidth()) {
            Text("New habit", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Obsidian.TextHi)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    label = { Text("Goal") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = increment,
                onValueChange = { increment = it },
                label = { Text("Increment") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = { Text("Amount added per click") },
                shape = RoundedCornerShape(14.dp)
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
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Obsidian.Accent, contentColor = Obsidian.Bg)
            ) { Text("CREATE HABIT", letterSpacing = 2.sp, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitCard(habit: HabitEntity, onClick: () -> Unit, onLongClick: () -> Unit, onReminderClick: () -> Unit = {}) {
    val progress = (habit.currentValue / habit.goalValue).coerceIn(0f, 1f)
    val monogram = habit.name.trim().take(2).uppercase()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(18.dp),
        color = Obsidian.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Obsidian.Stroke)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Monogram chip (streak badge preserved)
                Box(contentAlignment = Alignment.TopEnd) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(13.dp))
                            .border(1.dp, Obsidian.Stroke, RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            monogram,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Obsidian.Accent
                        )
                    }
                    if (habit.currentStreak >= 2) {
                        Box(
                            modifier = Modifier
                                .offset(x = 6.dp, y = (-6).dp)
                                .size(18.dp)
                                .background(Obsidian.Accent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                habit.currentStreak.toString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Obsidian.Bg
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(habit.name, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Obsidian.TextHi)
                    Row {
                        Text("${habit.currentValue}", fontSize = 12.sp, color = Obsidian.TextLow)
                        Text(" / ${habit.goalValue} ${habit.unit}", fontSize = 12.sp, color = Obsidian.TextFaint)
                    }
                }

                IconButton(onClick = onReminderClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Habit settings",
                        tint = Obsidian.TextLow,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (progress >= 1f) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Obsidian.Accent, modifier = Modifier.size(24.dp))
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${(progress * 100).toInt()}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Obsidian.TextMid
                        )
                        Text("%", fontSize = 11.sp, color = Obsidian.TextLow)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            ThinProgressBar(progress = progress, modifier = Modifier.fillMaxWidth(), dimmed = progress < 0.5f)
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

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Obsidian.Surface2) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .padding(bottom = 32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(habit.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Obsidian.TextHi)
            Spacer(modifier = Modifier.height(16.dp))

            if (habit.autoSyncEnabled) {
                MicroLabel("Syncs automatically from Health Connect", color = Obsidian.Accent)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${habit.currentValue}",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Obsidian.TextHi
                    )
                    Text(
                        " / ${habit.goalValue} ${habit.unit}",
                        fontSize = 15.sp,
                        color = Obsidian.TextLow,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Obsidian.Accent, contentColor = Obsidian.Bg)
                ) {
                    Text("CLOSE", letterSpacing = 2.sp, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Obsidian.Stroke),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Obsidian.TextMid)
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
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(15.dp))
                            .border(1.dp, Obsidian.Stroke, RoundedCornerShape(15.dp))
                            .clickable { value = (value - habit.incrementValue).coerceAtLeast(0f) },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Remove, null, tint = Obsidian.TextMid) }
                    Text(
                        value.toString(),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Obsidian.TextHi,
                        modifier = Modifier.padding(horizontal = 28.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(Obsidian.AccentDim, RoundedCornerShape(15.dp))
                            .border(1.dp, Obsidian.AccentBorder, RoundedCornerShape(15.dp))
                            .clickable { value += habit.incrementValue },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Add, null, tint = Obsidian.Accent) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Increment: +${habit.incrementValue} ${habit.unit}",
                    fontSize = 12.sp,
                    color = Obsidian.TextLow
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onSave(value, selectedDateMillis) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Obsidian.Accent, contentColor = Obsidian.Bg)
                ) {
                    Text("UPDATE PROGRESS", letterSpacing = 2.sp, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
        title = { Text("Select date") },
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

/** Pill-style tab (TODAY / ANALYTICS). */
@Composable
fun PillTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
            .border(
                1.dp,
                if (selected) Color.White.copy(alpha = 0.1f) else Obsidian.StrokeSoft,
                RoundedCornerShape(99.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = if (selected) Obsidian.TextHi else Obsidian.TextLow
        )
    }
}

@Composable
fun HabTrackApp(viewModel: HabitViewModel) {
    var selectedHabit by remember { mutableStateOf<HabitEntity?>(null) }
    var detailHabit by remember { mutableStateOf<HabitEntity?>(null) }
    var habitToDelete by remember { mutableStateOf<HabitEntity?>(null) }
    var habitForReminder by remember { mutableStateOf<HabitEntity?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    // Habit name pending Health Connect auto-sync setup (set by AddHabitScreen)
    var pendingAutoSync by remember { mutableStateOf<Pair<String, HealthMetric>?>(null) }
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

    // Once the newly created habit lands in the list, enable its auto-sync.
    LaunchedEffect(habits, pendingAutoSync) {
        pendingAutoSync?.let { (habitName, metric) ->
            habits.find { it.name == habitName && !it.autoSyncEnabled }?.let {
                viewModel.setHabitAutoSync(it, true, metric)
                viewModel.syncFromHealthConnect(context)
                pendingAutoSync = null
            }
        }
    }

    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
        return
    }

    // ── Full-screen: new habit ──
    if (showAdd) {
        AddHabitScreen(
            healthConnectAvailable = healthConnectAvailable,
            onBack = { showAdd = false },
            onSave = { n, g, u, inc, metric ->
                viewModel.addNewHabit(n, g, u, "#57E6C6", inc)
                if (metric != null) pendingAutoSync = n to metric
                showAdd = false
            }
        )
        return
    }

    // ── Full-screen: habit detail ──
    detailHabit?.let { d ->
        val fresh = habits.find { it.id == d.id }
        if (fresh == null) {
            // habit was deleted while open
            detailHabit = null
        } else {
            HabitDetailScreen(
                habit = fresh,
                onBack = { detailHabit = null },
                onLog = { selectedHabit = fresh }
            )
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
            return
        }
    }

    // "SUNDAY · JUL 5" style date label
    val dayLabel = java.text.SimpleDateFormat("EEEE · MMM d", java.util.Locale.getDefault())
        .format(java.util.Date()).uppercase()

    Scaffold(
        containerColor = Obsidian.Bg,
        topBar = {
            Column(
                modifier = Modifier
                    .background(Obsidian.Bg)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        MicroLabel(if (selectedTabIndex == 1) "LAST 30 DAYS" else dayLabel)
                        Text(
                            if (selectedTabIndex == 1) "analytics" else "today",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Obsidian.TextHi
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Obsidian.Surface, RoundedCornerShape(12.dp))
                            .border(1.dp, Obsidian.Stroke, RoundedCornerShape(12.dp))
                            .clickable { showSettings = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Obsidian.TextMid,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillTab("Today", selectedTabIndex == 0) { selectedTabIndex = 0 }
                    PillTab("Analytics", selectedTabIndex == 1) { selectedTabIndex = 1 }
                }
            }
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = { showAdd = true },
                    containerColor = Obsidian.Accent,
                    contentColor = Obsidian.Bg,
                    modifier = Modifier.padding(16.dp),
                    shape = RoundedCornerShape(18.dp)
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
                        .background(Obsidian.Bg)
                        .padding(horizontal = 20.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))

                        // ── Hero: daily progress ring + streak ──
                        val bestStreak = habits.maxOfOrNull { it.currentStreak } ?: 0
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF14181E), Color(0xFF0F1216))
                                    )
                                )
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Obsidian.Accent.copy(alpha = 0.14f), Color.Transparent),
                                        center = Offset(0.15f, 0f), radius = 900f
                                    )
                                )
                                .border(1.dp, Obsidian.AccentBorder, RoundedCornerShape(24.dp))
                                .padding(22.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(22.dp)
                            ) {
                                ProgressRing(
                                    progress = averageProgress / 100f,
                                    modifier = Modifier.size(118.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "${averageProgress.toInt()}%",
                                            fontSize = 27.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Obsidian.TextHi
                                        )
                                        MicroLabel("done")
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    MicroLabel("Daily progress")
                                    val completedCount = habits.count { it.currentValue >= it.goalValue }
                                    val remaining = habits.size - completedCount
                                    val statusText = when {
                                        habits.isEmpty() -> "Add habits to get started"
                                        remaining == 0 -> "All habits complete — great work"
                                        else -> "$completedCount of ${habits.size} habits on track"
                                    }
                                    Text(statusText, fontSize = 15.sp, color = Obsidian.TextMid, lineHeight = 21.sp)
                                    if (bestStreak >= 2) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(99.dp))
                                                .background(Color.White.copy(alpha = 0.05f))
                                                .border(1.dp, Obsidian.Stroke, RoundedCornerShape(99.dp))
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .rotate(45f)
                                                    .background(Obsidian.Accent)
                                            )
                                            Text(
                                                "$bestStreak-DAY STREAK",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.5.sp,
                                                color = Obsidian.TextMid
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Health Connect sync status ──
                        if (healthConnectAvailable) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 14.dp, start = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Obsidian.Accent, CircleShape)
                                )
                                MicroLabel("Health Connect · Synced")
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))
                        MicroLabel("Active habits")
                        Spacer(modifier = Modifier.height(7.dp))
                    }

                    items(habits) { habit ->
                        HabitCard(
                            habit = habit,
                            onClick = { detailHabit = habit },
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
                // Analytics Tab (padding(p) keeps content below the header)
                AnalyticsScreen(viewModel = viewModel, modifier = Modifier.padding(p))
            }
        }
    }

    // (AddHabitScreen replaced the old AddHabitSheet — see full-screen branch above)

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
            title = { Text("Delete habit") },
            text = { Text("Delete '${h.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteHabit(h)
                    habitToDelete = null
                }) {
                    Text("Delete", color = Color(0xFFF2B5B5))
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
