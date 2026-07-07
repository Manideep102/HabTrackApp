package com.example.habtrack.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habtrack.ai.HabitInsightsService
import com.example.habtrack.data.ApiKeyStore
import com.example.habtrack.data.DailyCompletion
import com.example.habtrack.data.HabitEntity
import com.example.habtrack.data.HabitRepository
import com.example.habtrack.health.HealthConnectAvailability
import com.example.habtrack.health.HealthConnectManager
import com.example.habtrack.health.HealthMetric
import com.example.habtrack.notifications.HabitReminderScheduler
import com.example.habtrack.utils.HabitStrengthCalculator
import com.example.habtrack.utils.VolumeStatsCalculator
import com.example.habtrack.utils.HabitHeatmapCalculator
import com.example.habtrack.utils.HabitCorrelationCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * HabitViewModel: Manages the UI state for the habit tracker.
 * It communicates with the Repository to perform database operations.
 */
class HabitViewModel(private val repository: HabitRepository) : ViewModel() {

    // 1. Collect habits from the database and convert to a StateFlow for Compose
    val habitListState: StateFlow<List<HabitEntity>> = repository.allHabits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 2. Analytics: Overall completion rate
    val completionRate: StateFlow<Float> = habitListState
        .map { habits ->
            if (habits.isEmpty()) 0f
            else {
                val completedCount = habits.count { (it.currentValue / it.goalValue) >= 1f }
                (completedCount.toFloat() / habits.size) * 100f
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // 3. Analytics: Average progress across all habits
    val averageProgress: StateFlow<Float> = habitListState
        .map { habits ->
            if (habits.isEmpty()) 0f
            else {
                habits.map { (it.currentValue / it.goalValue).coerceIn(0f, 1f) }
                    .average()
                    .toFloat() * 100f
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // 4. Analytics: Total habits count
    val totalHabits: StateFlow<Int> = habitListState
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // 5. Analytics: Get habit progress data for charts
    val habitProgressData: StateFlow<List<HabitProgress>> = habitListState
        .map { habits ->
            habits.map { habit ->
                HabitProgress(
                    name = habit.name,
                    progress = (habit.currentValue / habit.goalValue).coerceIn(0f, 1f) * 100f,
                    color = habit.colorHex
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 6. Analytics: Habit strength data with streaks
    val habitStrengthData: StateFlow<List<HabitStrength>> = habitListState
        .map { habits ->
            habits.map { habit ->
                val isCompleted = (habit.currentValue / habit.goalValue) >= 1f
                HabitStrength(
                    id = habit.id,
                    name = habit.name,
                    strengthScore = habit.strengthScore,
                    currentStreak = habit.currentStreak,
                    totalCompletions = habit.totalCompletions,
                    strengthLevel = HabitStrengthCalculator.getStrengthLevel(habit.strengthScore),
                    strengthEmoji = HabitStrengthCalculator.getStrengthEmoji(habit.strengthScore),
                    color = habit.colorHex
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 7. Analytics: Volume statistics data
    val volumeStatsData: StateFlow<List<HabitVolumeStats>> = habitListState
        .map { habits ->
            habits.map { habit ->
                HabitVolumeStats(
                    id = habit.id,
                    name = habit.name,
                    todayVolume = VolumeStatsCalculator.getTodayVolume(habit),
                    weeklyVolume = VolumeStatsCalculator.getWeeklyVolume(habit),
                    monthlyVolume = VolumeStatsCalculator.getMonthlyVolume(habit),
                    yearlyVolume = VolumeStatsCalculator.getYearlyVolume(habit),
                    personalRecord = VolumeStatsCalculator.getPersonalRecord(habit),
                    averageDailyVolume = VolumeStatsCalculator.getAverageDailyVolume(habit),
                    unit = habit.unit,
                    color = habit.colorHex
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 8. Analytics: Completion heatmap data (initially empty, will be populated from daily completions)
    val heatmapData: StateFlow<List<HabitHeatmapData>> = habitListState
        .map { habits ->
            habits.map { habit ->
                // For now, create empty heatmap data structure
                // This will be populated with actual daily completion data when that's implemented
                HabitHeatmapData(
                    habitId = habit.id,
                    habitName = habit.name,
                    cells = emptyList(),
                    currentStreak = habit.currentStreak,
                    longestStreak = habit.currentStreak,
                    completionPercentage = (habit.currentValue / habit.goalValue).coerceIn(0f, 1f) * 100f
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 9. Analytics: Habit correlations (shows relationships between habits)
    val correlationData: StateFlow<List<HabitCorrelationCalculator.HabitCorrelation>> = habitListState
        .map { habits ->
            // Build completion data for correlation analysis
            // For now, use strength score as proxy for completion pattern
            val completionData = habits.associate { habit ->
                val pattern = listOf(
                    habit.strengthScore / 100f,
                    (habit.totalCompletions / maxOf(habit.goalValue, 1f)),
                    habit.currentStreak.toFloat() / 30f // normalize to 30-day period
                )
                habit.id to pattern
            }

            val habitNames = habits.associate { it.id to it.name }

            // Find correlations above threshold
            HabitCorrelationCalculator.findSignificantCorrelations(
                completionData,
                habitNames,
                threshold = 0.3f
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 10. Analytics: Habit clusters (groups of related habits)
    val clusterData: StateFlow<Map<String, List<Pair<Int, String>>>> = habitListState
        .map { habits ->
            val completionData = habits.associate { habit ->
                val pattern = listOf(
                    habit.strengthScore / 100f,
                    (habit.totalCompletions / maxOf(habit.goalValue, 1f)),
                    habit.currentStreak.toFloat() / 30f
                )
                habit.id to pattern
            }

            val habitNames = habits.associate { it.id to it.name }

            HabitCorrelationCalculator.clusterHabits(
                completionData,
                habitNames,
                threshold = 0.5f
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // 11. Analytics: Monthly completions data for calendar view
    val monthlyCompletionsData: StateFlow<Map<Int, Pair<List<Pair<Int, Boolean>>, List<Pair<Int, Float>>>>> =
        combine(habitListState, repository.getRecentCompletions()) { habits, completions ->
            val zone = java.time.ZoneId.systemDefault()
            val currentMonth = java.time.YearMonth.now()

            // Latest record per habit per day of the current month; a day can have
            // several rows because every progress update inserts a new one
            val latestByHabitAndDay = completions
                .filter {
                    java.time.YearMonth.from(
                        java.time.Instant.ofEpochMilli(it.dateTime).atZone(zone).toLocalDate()
                    ) == currentMonth
                }
                .groupBy { it.habitId }
                .mapValues { (_, rows) ->
                    rows
                        .groupBy { java.time.Instant.ofEpochMilli(it.dateTime).atZone(zone).toLocalDate().dayOfMonth }
                        .mapValues { (_, dayRows) -> dayRows.maxBy { it.timestamp } }
                }

            habits.associate { habit ->
                val byDay = latestByHabitAndDay[habit.id].orEmpty()
                val completedDays = byDay.map { (day, row) -> day to row.isCompleted }
                val progressDays = byDay.map { (day, row) ->
                    val fraction = if (habit.goalValue > 0f) row.progressValue / habit.goalValue else 0f
                    day to fraction.coerceIn(0f, 1f)
                }
                habit.id to (completedDays to progressDays)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /**
     * Adds a new habit to the database.
     * Uses the HabitEntity structure with default values for progress and icons.
     */
    fun addNewHabit(name: String, goal: Float, unit: String, color: String, increment: Float = 1f) {
        viewModelScope.launch {
            val habit = HabitEntity(
                name = name,
                goalValue = goal,
                currentValue = 0f,
                unit = unit,
                colorHex = color,
                iconName = "bolt",
                incrementValue = increment
            )
            repository.insertHabit(habit)
        }
    }

    /**
     * Updates the progress value of an existing habit and records it in daily completions.
     * Only updates currentValue if the date is today; for past dates, only records in DailyCompletion.
     */
    fun updateHabitProgress(habit: HabitEntity, newValue: Float, dateMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            // Determine if the selected date is today
            val isToday = isDateToday(dateMillis)
            
            // Update habit's current value ONLY if updating today's progress
            if (isToday) {
                val newPR = maxOf(habit.personalRecord, newValue)
                repository.updateHabit(habit.copy(currentValue = newValue, personalRecord = newPR))
            }
            
            // Always record the daily completion
            val isCompleted = (newValue / habit.goalValue) >= 1f
            val dailyCompletion = com.example.habtrack.data.DailyCompletion(
                habitId = habit.id,
                dateTime = dateMillis,
                isCompleted = isCompleted,
                progressValue = newValue
            )
            repository.recordDailyCompletion(dailyCompletion)
        }
    }
    
    /**
     * Helper function to check if a timestamp is today's date
     */
    private fun isDateToday(dateMillis: Long): Boolean {
        val (startOfToday, endOfToday) = todayRangeMillis()
        return dateMillis in startOfToday..endOfToday
    }

    /**
     * Returns (startOfToday, endOfToday) in epoch millis using the device's local calendar.
     */
    private fun todayRangeMillis(): Pair<Long, Long> {
        val calendar = java.util.Calendar.getInstance()

        // Start of today (00:00:00.000)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis

        // End of today (23:59:59.999)
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.add(java.util.Calendar.MILLISECOND, -1)
        val endOfToday = calendar.timeInMillis

        return startOfToday to endOfToday
    }

    /**
     * Deletes a habit from the database.
     */
    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    /**
     * Updates a habit and reschedules reminder if enabled
     */
    fun updateHabitWithReminder(
        habit: HabitEntity,
        context: Context
    ) {
        viewModelScope.launch {
            repository.updateHabit(habit)

            // Handle reminder scheduling
            if (habit.reminderEnabled) {
                HabitReminderScheduler.scheduleReminder(
                    context = context,
                    habitId = habit.id,
                    habitName = habit.name,
                    reminderTime = habit.reminderTime
                )
            } else {
                HabitReminderScheduler.cancelReminder(context, habit.id)
            }
        }
    }

    /**
     * Enables or disables reminder for a habit
     */
    fun setHabitReminder(
        habit: HabitEntity,
        reminderTime: String,
        enabled: Boolean,
        context: Context
    ) {
        val updatedHabit = habit.copy(
            reminderTime = reminderTime,
            reminderEnabled = enabled
        )
        updateHabitWithReminder(updatedHabit, context)
    }

    /**
     * Updates habit progress and recalculates strength score
     */
    fun updateHabitProgressWithStrength(habit: HabitEntity, newValue: Float) {
        viewModelScope.launch {
            val isCompleted = (newValue / habit.goalValue) >= 1f
            val newStrength = HabitStrengthCalculator.calculateStrength(habit, isCompleted)
            val newStreak = HabitStrengthCalculator.calculateStreak(habit, isCompleted)

            val newPR = maxOf(habit.personalRecord, newValue)
            val updatedHabit = habit.copy(
                currentValue = newValue,
                strengthScore = newStrength,
                currentStreak = newStreak,
                totalCompletions = habit.totalCompletions + 1,
                lastCompletedDate = if (isCompleted) System.currentTimeMillis() else habit.lastCompletedDate,
                personalRecord = newPR
            )
            repository.updateHabit(updatedHabit)
        }
    }

    // AI Insights
    private val _insightsState = MutableStateFlow<InsightsState>(InsightsState.Idle)
    val insightsState: StateFlow<InsightsState> = _insightsState

    /**
     * Summarizes current habit data and asks Claude for a short coaching insight.
     */
    fun generateInsights(context: Context) {
        viewModelScope.launch {
            val apiKey = ApiKeyStore(context).getApiKey()
            if (apiKey.isNullOrBlank()) {
                _insightsState.value = InsightsState.Error("Add your Anthropic API key in Settings first.")
                return@launch
            }

            val summary = buildHabitSummary()
            if (summary == null) {
                _insightsState.value = InsightsState.Error("Add a few habits first to get insights.")
                return@launch
            }

            _insightsState.value = InsightsState.Loading
            val result = HabitInsightsService.generateInsights(apiKey, summary)
            _insightsState.value = result.fold(
                onSuccess = { InsightsState.Success(it) },
                onFailure = { InsightsState.Error(it.message ?: "Something went wrong. Please try again.") }
            )
        }
    }

    private fun buildHabitSummary(): String? {
        val habits = habitListState.value
        if (habits.isEmpty()) return null

        val lines = mutableListOf<String>()
        lines += "Overall completion rate: ${completionRate.value.toInt()}%, " +
            "average progress: ${averageProgress.value.toInt()}% across ${habits.size} habits."

        habitStrengthData.value.forEach { s ->
            lines += "- ${s.name}: current streak ${s.currentStreak} days, " +
                "strength ${s.strengthScore.toInt()}/100 (${s.strengthLevel}), " +
                "total completions ${s.totalCompletions}."
        }

        val correlations = correlationData.value
        if (correlations.isNotEmpty()) {
            lines += "Detected correlations between habits:"
            correlations.take(5).forEach { c ->
                lines += "- ${c.habit1Name} and ${c.habit2Name}: ${c.interpretation} " +
                    "(score ${"%.2f".format(c.correlationScore)})."
            }
        }

        return lines.joinToString("\n")
    }

    // Health Connect Sync
    private val _healthSyncState = MutableStateFlow<HealthSyncState>(HealthSyncState.Idle)
    val healthSyncState: StateFlow<HealthSyncState> = _healthSyncState

    /**
     * Checks Health Connect availability/permissions and, if ready, refreshes every
     * auto-sync-enabled habit's currentValue from today's Health Connect totals.
     * Safe to call on every app start - habits without auto-sync are untouched.
     */
    fun syncFromHealthConnect(context: Context) {
        viewModelScope.launch {
            val manager = HealthConnectManager(context)
            if (manager.getAvailability() !is HealthConnectAvailability.Available) {
                _healthSyncState.value = HealthSyncState.Unavailable
                return@launch
            }

            val client = HealthConnectManager.getClient(context)
            if (client == null) {
                _healthSyncState.value = HealthSyncState.Unavailable
                return@launch
            }

            if (!manager.hasAllPermissions(client)) {
                _healthSyncState.value = HealthSyncState.PermissionRequired
                return@launch
            }

            _healthSyncState.value = HealthSyncState.Syncing
            val syncedHabits = habitListState.value.filter { it.autoSyncEnabled && it.autoSyncMetric != null }
            for (habit in syncedHabits) {
                val metric = HealthMetric.entries.find { it.name == habit.autoSyncMetric } ?: continue
                val newValue = manager.readTodayValueFor(client, metric)
                applyHealthConnectValue(habit, newValue)
            }
            _healthSyncState.value = HealthSyncState.Success(syncedHabits.size)
        }
    }

    /**
     * Sets [habit]'s currentValue from Health Connect (authoritative for synced habits -
     * not additive with manual taps) and writes/updates today's DailyCompletion row.
     *
     * Streak/strength/totalCompletions are only recalculated the first time a habit
     * crosses into "completed" for the day (checked via today's existing DailyCompletion
     * row), so re-syncing on a later app open the same day doesn't double-count them.
     */
    private suspend fun applyHealthConnectValue(habit: HabitEntity, newValue: Float) {
        val (startOfDay, endOfDay) = todayRangeMillis()
        val existingToday = repository.getDailyCompletionsBetween(habit.id, startOfDay, endOfDay).firstOrNull()
        val wasAlreadyCompletedToday = existingToday?.isCompleted == true

        val isCompleted = (newValue / habit.goalValue) >= 1f
        val newPR = maxOf(habit.personalRecord, newValue)

        val updatedHabit = if (isCompleted && !wasAlreadyCompletedToday) {
            val newStrength = HabitStrengthCalculator.calculateStrength(habit, isCompleted)
            val newStreak = HabitStrengthCalculator.calculateStreak(habit, isCompleted)
            habit.copy(
                currentValue = newValue,
                strengthScore = newStrength,
                currentStreak = newStreak,
                totalCompletions = habit.totalCompletions + 1,
                lastCompletedDate = System.currentTimeMillis(),
                personalRecord = newPR,
                lastUpdated = System.currentTimeMillis()
            )
        } else {
            habit.copy(currentValue = newValue, personalRecord = newPR, lastUpdated = System.currentTimeMillis())
        }
        repository.updateHabit(updatedHabit)

        if (existingToday != null) {
            repository.updateDailyCompletion(existingToday.copy(isCompleted = isCompleted, progressValue = newValue))
        } else {
            repository.recordDailyCompletion(
                DailyCompletion(
                    habitId = habit.id,
                    dateTime = System.currentTimeMillis(),
                    isCompleted = isCompleted,
                    progressValue = newValue
                )
            )
        }
    }

    /**
     * Enables/disables Health Connect auto-sync for a habit and records which metric feeds it.
     * Prefills the unit with the metric's default only if the user hasn't already
     * customized it away from a previous auto-fill default; goalValue is never touched.
     */
    fun setHabitAutoSync(habit: HabitEntity, enabled: Boolean, metric: HealthMetric?) {
        viewModelScope.launch {
            val defaultUnits = HealthMetric.entries.map { it.defaultUnit }
            val newUnit = if (enabled && metric != null && (habit.unit.isBlank() || habit.unit in defaultUnits)) {
                metric.defaultUnit
            } else {
                habit.unit
            }
            repository.updateHabit(
                habit.copy(
                    autoSyncEnabled = enabled,
                    autoSyncMetric = if (enabled) metric?.name else null,
                    unit = newUnit
                )
            )
        }
    }
}

/**
 * UI state for the AI-generated habit insights card.
 */
sealed class InsightsState {
    data object Idle : InsightsState()
    data object Loading : InsightsState()
    data class Success(val text: String) : InsightsState()
    data class Error(val message: String) : InsightsState()
}

/**
 * UI state for the Health Connect sync process.
 */
sealed class HealthSyncState {
    data object Idle : HealthSyncState()
    data object Syncing : HealthSyncState()
    data class Success(val syncedCount: Int) : HealthSyncState()
    data object PermissionRequired : HealthSyncState()
    data object Unavailable : HealthSyncState()
    data class Error(val message: String) : HealthSyncState()
}

/**
 * Data class for analytics progress tracking
 */
data class HabitProgress(
    val name: String,
    val progress: Float,
    val color: String
)

/**
 * Data class for habit strength tracking
 */
data class HabitStrength(
    val id: Int,
    val name: String,
    val strengthScore: Float,
    val currentStreak: Int,
    val totalCompletions: Int,
    val strengthLevel: String,
    val strengthEmoji: String,
    val color: String
)

/**
 * Data class for volume statistics tracking
 */
data class HabitVolumeStats(
    val id: Int,
    val name: String,
    val todayVolume: Float,
    val weeklyVolume: Float,
    val monthlyVolume: Float,
    val yearlyVolume: Float,
    val personalRecord: Float,
    val averageDailyVolume: Float,
    val unit: String,
    val color: String
)

/**
 * Data class for completion heatmap tracking
 */
data class HabitHeatmapData(
    val habitId: Int,
    val habitName: String,
    val cells: List<HabitHeatmapCalculator.HeatmapCell>,
    val currentStreak: Int,
    val longestStreak: Int,
    val completionPercentage: Float
)