package com.example.habtrack.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlin.coroutines.cancellation.CancellationException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Health Connect metrics HabTrack can sync into a habit's daily progress.
 */
enum class HealthMetric(
    val displayName: String,
    val defaultUnit: String,
    val defaultGoal: Float,
    val goalStep: Float
) {
    STEPS("Steps", "steps", 5000f, 500f),
    ACTIVE_CALORIES("Active Calories", "kcal", 400f, 50f),
    DISTANCE("Distance", "km", 3f, 0.5f),
    SLEEP("Sleep", "hours", 8f, 0.5f),
    EXERCISE("Exercise", "min", 30f, 5f),
    TOTAL_CALORIES("Total Calories", "kcal", 2000f, 100f),
    HYDRATION("Hydration", "L", 2f, 0.25f),
    FLOORS("Floors Climbed", "floors", 10f, 1f)
}

/**
 * Whether Health Connect can be used on this device right now.
 */
sealed class HealthConnectAvailability {
    data object Available : HealthConnectAvailability()
    data object NotInstalled : HealthConnectAvailability()
    data object UpdateRequired : HealthConnectAvailability()
}

/**
 * Wraps Health Connect availability checks, permission handling, and per-metric
 * "today" reads. Instances are cheap and short-lived (mirrors ApiKeyStore(context)).
 */
class HealthConnectManager(private val context: Context) {

    fun getAvailability(): HealthConnectAvailability = try {
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.Available
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UpdateRequired
            else -> HealthConnectAvailability.NotInstalled
        }
    } catch (_: Throwable) {
        // Throwable, not Exception: OEM ROMs and stale installs can surface
        // linkage Errors (NoClassDefFoundError etc.), which must not crash us
        HealthConnectAvailability.NotInstalled
    }

    /** The Health Connect read permission string backing a single [metric]. */
    fun readPermissionFor(metric: HealthMetric): String = when (metric) {
        HealthMetric.STEPS -> HealthPermission.getReadPermission(StepsRecord::class)
        HealthMetric.ACTIVE_CALORIES -> HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
        HealthMetric.DISTANCE -> HealthPermission.getReadPermission(DistanceRecord::class)
        HealthMetric.SLEEP -> HealthPermission.getReadPermission(SleepSessionRecord::class)
        HealthMetric.EXERCISE -> HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        HealthMetric.TOTAL_CALORIES -> HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
        HealthMetric.HYDRATION -> HealthPermission.getReadPermission(HydrationRecord::class)
        HealthMetric.FLOORS -> HealthPermission.getReadPermission(FloorsClimbedRecord::class)
    }

    // Derived from readPermissionFor so the requested set can never drift from what
    // each metric actually reads.
    fun getRequiredPermissions(): Set<String> =
        HealthMetric.entries.map { readPermissionFor(it) }.toSet()

    /** Permissions the user has actually granted — the sync gates per-metric on this. */
    suspend fun grantedPermissions(client: HealthConnectClient): Set<String> =
        client.permissionController.getGrantedPermissions()

    suspend fun hasAllPermissions(client: HealthConnectClient): Boolean {
        return client.permissionController.getGrantedPermissions().containsAll(getRequiredPermissions())
    }

    /**
     * Reads today's aggregated total for [metric], normalized into the unit
     * implied by [HealthMetric.defaultUnit] (steps count, kcal, km).
     * Never throws — a permission/availability problem just yields 0f so a
     * sync failure can't crash app start.
     */
    suspend fun readTodayValueFor(client: HealthConnectClient, metric: HealthMetric): Float =
        readValueFor(client, metric, todayTimeRangeFilter())

    /**
     * Reads [metric]'s aggregated total for an arbitrary [range]. Same normalization and
     * never-throws contract as [readTodayValueFor]; the today path is just this with a
     * start-of-day → now range.
     */
    private suspend fun readValueFor(
        client: HealthConnectClient,
        metric: HealthMetric,
        range: TimeRangeFilter
    ): Float {
        return try {
            when (metric) {
                HealthMetric.STEPS -> readSteps(client, range)
                HealthMetric.ACTIVE_CALORIES -> readActiveCalories(client, range)
                HealthMetric.DISTANCE -> readDistanceKilometers(client, range)
                HealthMetric.SLEEP -> readSleepHours(client, range)
                HealthMetric.EXERCISE -> readExerciseMinutes(client, range)
                HealthMetric.TOTAL_CALORIES -> readTotalCalories(client, range)
                HealthMetric.HYDRATION -> readHydrationLiters(client, range)
                HealthMetric.FLOORS -> readFloorsClimbed(client, range)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            0f
        }
    }

    /**
     * Today's totals for the last [days] days (excluding today), one entry per day, keyed by
     * date in the device's zone. Only days with a value > 0 are returned so empty days don't
     * pollute history. Used for the one-time 30-day backfill; the HC read grant covers the
     * past 30 days.
     */
    suspend fun readHistoryFor(
        client: HealthConnectClient,
        metric: HealthMetric,
        days: Int = 30
    ): Map<LocalDate, Float> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val result = LinkedHashMap<LocalDate, Float>()
        for (offset in days downTo 1) {
            val date = today.minusDays(offset.toLong())
            val value = readValueFor(client, metric, dayTimeRangeFilter(date, zone))
            if (value > 0f) result[date] = value
        }
        return result
    }

    private suspend fun readSteps(client: HealthConnectClient, range: TimeRangeFilter): Float {
        val result = client.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), range))
        return (result[StepsRecord.COUNT_TOTAL] ?: 0L).toFloat()
    }

    private suspend fun readActiveCalories(client: HealthConnectClient, range: TimeRangeFilter): Float {
        val result = client.aggregate(AggregateRequest(setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL), range))
        return (result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0).toFloat()
    }

    private suspend fun readDistanceKilometers(client: HealthConnectClient, range: TimeRangeFilter): Float {
        val result = client.aggregate(AggregateRequest(setOf(DistanceRecord.DISTANCE_TOTAL), range))
        return (result[DistanceRecord.DISTANCE_TOTAL]?.inKilometers ?: 0.0).toFloat()
    }

    // Sessions crossing midnight only count their after-midnight portion — HC clips
    // aggregates to the requested range, and we deliberately keep the same
    // start-of-day window as every other metric
    private suspend fun readSleepHours(client: HealthConnectClient, range: TimeRangeFilter): Float {
        val result = client.aggregate(AggregateRequest(setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL), range))
        return ((result[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMinutes() ?: 0L) / 60f)
    }

    private suspend fun readExerciseMinutes(client: HealthConnectClient, range: TimeRangeFilter): Float {
        val result = client.aggregate(AggregateRequest(setOf(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL), range))
        return (result[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes() ?: 0L).toFloat()
    }

    private suspend fun readTotalCalories(client: HealthConnectClient, range: TimeRangeFilter): Float {
        val result = client.aggregate(AggregateRequest(setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL), range))
        return (result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0).toFloat()
    }

    private suspend fun readHydrationLiters(client: HealthConnectClient, range: TimeRangeFilter): Float {
        val result = client.aggregate(AggregateRequest(setOf(HydrationRecord.VOLUME_TOTAL), range))
        return (result[HydrationRecord.VOLUME_TOTAL]?.inLiters ?: 0.0).toFloat()
    }

    private suspend fun readFloorsClimbed(client: HealthConnectClient, range: TimeRangeFilter): Float {
        val result = client.aggregate(AggregateRequest(setOf(FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL), range))
        return (result[FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL] ?: 0.0).toFloat()
    }

    private fun todayTimeRangeFilter(): TimeRangeFilter {
        val zone = ZoneId.systemDefault()
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        return TimeRangeFilter.between(startOfDay, Instant.now())
    }

    private fun dayTimeRangeFilter(date: LocalDate, zone: ZoneId): TimeRangeFilter {
        val startOfDay = date.atStartOfDay(zone).toInstant()
        val startOfNextDay = date.plusDays(1).atStartOfDay(zone).toInstant()
        return TimeRangeFilter.between(startOfDay, startOfNextDay)
    }

    companion object {
        /** Returns null if Health Connect isn't available on this device. */
        fun getClient(context: Context): HealthConnectClient? = try {
            if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE)
                HealthConnectClient.getOrCreate(context)
            else null
        } catch (_: Throwable) { null }
    }
}
