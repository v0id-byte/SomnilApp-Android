package com.somnil.app.service

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SleepStageRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android Health Connect integration - equivalent to iOS HealthKitManager.
 * Mirrors the iOS SleepSession writing functionality.
 */
@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val healthConnectClient: HealthConnectClient? by lazy {
        try {
            if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                HealthConnectClient.getOrCreate(context)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    val isAvailable: Boolean
        get() = healthConnectClient != null

    val permissions = setOf(
        HealthPermission.getReadPermission<SleepSessionRecord>(),
        HealthPermission.getWritePermission<SleepSessionRecord>(),
        HealthPermission.getReadPermission<SleepStageRecord>(),
        HealthPermission.getWritePermission<SleepStageRecord>()
    )

    /**
     * Write a sleep session to Health Connect.
     * Mirrors iOS: HKHealthStore().save(sleepSession)
     */
    suspend fun writeSleepSession(
        startTime: Instant,
        endTime: Instant,
        stages: Map<String, Double> // stage name -> duration seconds
    ): Boolean {
        val client = healthConnectClient ?: return false

        return try {
            // Write the main sleep session record
            val sessionRecord = SleepSessionRecord(
                startTime = startTime,
                endTime = endTime,
                startZoneOffset = null,
                endZoneOffset = null
            )

            val inserted = client.insertRecords(listOf(sessionRecord))
            inserted.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Read sleep sessions from Health Connect.
     */
    suspend fun readSleepSessions(
        startTime: Instant,
        endTime: Instant
    ): List<SleepSessionRecord> {
        val client = healthConnectClient ?: return emptyList()

        return try {
            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            client.readRecords(request).records
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Read sleep sessions for the past N days.
     */
    suspend fun readRecentSleepSessions(days: Int = 30): List<SleepSessionRecord> {
        val end = Instant.now()
        val start = end.minus(days.toLong(), ChronoUnit.DAYS)
        return readSleepSessions(start, end)
    }
}
