package com.example.habtrack.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Health Connect metrics HabTrack can sync into a habit's daily progress.
 */
enum class HealthMetric(val displayName: String, val defaultUnit: String) {
    STEPS("Steps", "steps"),
    ACTIVE_CALORIES("Active Calories", "kcal"),
    DISTANCE("Distance", "km")
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
    } catch (_: Exception) {
        HealthConnectAvailability.NotInstalled
    }

    fun getRequiredPermissions(): Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class)
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
            }
        } catch (e: Exception) {
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
        } catch (_: Exception) { null }
    }
}
