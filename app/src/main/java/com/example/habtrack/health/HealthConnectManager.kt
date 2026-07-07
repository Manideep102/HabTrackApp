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

    fun getRequiredPermissions(): Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getReadPermission(FloorsClimbedRecord::class)
    )

    suspend fun hasAllPermissions(client: HealthConnectClient): Boolean {
        return client.permissionController.getGrantedPermissions().containsAll(getRequiredPermissions())
    }

    /**
     * Reads today's aggregated total for [metric], normalized into the unit
     * implied by [HealthMetric.defaultUnit] (steps count, kcal, km).
     * Never throws — a permission/availability problem just yields 0f so a
     * sync failure can't crash app start.
     */
    suspend fun readTodayValueFor(client: HealthConnectClient, metric: HealthMetric): Float {
        return try {
            when (metric) {
                HealthMetric.STEPS -> readTodaySteps(client)
                HealthMetric.ACTIVE_CALORIES -> readTodayActiveCalories(client)
                HealthMetric.DISTANCE -> readTodayDistanceKilometers(client)
                HealthMetric.SLEEP -> readTodaySleepHours(client)
                HealthMetric.EXERCISE -> readTodayExerciseMinutes(client)
                HealthMetric.TOTAL_CALORIES -> readTodayTotalCalories(client)
                HealthMetric.HYDRATION -> readTodayHydrationLiters(client)
                HealthMetric.FLOORS -> readTodayFloorsClimbed(client)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            0f
        }
    }

    private suspend fun readTodaySteps(client: HealthConnectClient): Float {
        val result = client.aggregate(
            AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), todayTimeRangeFilter())
        )
        return (result[StepsRecord.COUNT_TOTAL] ?: 0L).toFloat()
    }

    private suspend fun readTodayActiveCalories(client: HealthConnectClient): Float {
        val result = client.aggregate(
            AggregateRequest(setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL), todayTimeRangeFilter())
        )
        return (result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0).toFloat()
    }

    private suspend fun readTodayDistanceKilometers(client: HealthConnectClient): Float {
        val result = client.aggregate(
            AggregateRequest(setOf(DistanceRecord.DISTANCE_TOTAL), todayTimeRangeFilter())
        )
        return (result[DistanceRecord.DISTANCE_TOTAL]?.inKilometers ?: 0.0).toFloat()
    }

    // Sessions crossing midnight only count their after-midnight portion — HC clips
    // aggregates to the requested range, and we deliberately keep the same
    // start-of-day window as every other metric
    private suspend fun readTodaySleepHours(client: HealthConnectClient): Float {
        val result = client.aggregate(
            AggregateRequest(setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL), todayTimeRangeFilter())
        )
        return ((result[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMinutes() ?: 0L) / 60f)
    }

    private suspend fun readTodayExerciseMinutes(client: HealthConnectClient): Float {
        val result = client.aggregate(
            AggregateRequest(setOf(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL), todayTimeRangeFilter())
        )
        return (result[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes() ?: 0L).toFloat()
    }

    private suspend fun readTodayTotalCalories(client: HealthConnectClient): Float {
        val result = client.aggregate(
            AggregateRequest(setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL), todayTimeRangeFilter())
        )
        return (result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0).toFloat()
    }

    private suspend fun readTodayHydrationLiters(client: HealthConnectClient): Float {
        val result = client.aggregate(
            AggregateRequest(setOf(HydrationRecord.VOLUME_TOTAL), todayTimeRangeFilter())
        )
        return (result[HydrationRecord.VOLUME_TOTAL]?.inLiters ?: 0.0).toFloat()
    }

    private suspend fun readTodayFloorsClimbed(client: HealthConnectClient): Float {
        val result = client.aggregate(
            AggregateRequest(setOf(FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL), todayTimeRangeFilter())
        )
        return (result[FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL] ?: 0.0).toFloat()
    }

    private fun todayTimeRangeFilter(): TimeRangeFilter {
        val zone = ZoneId.systemDefault()
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        return TimeRangeFilter.between(startOfDay, Instant.now())
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
