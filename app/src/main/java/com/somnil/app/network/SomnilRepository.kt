package com.somnil.app.network

import com.somnil.app.network.model.ApiResponse
import com.somnil.app.network.model.LoginRequest
import com.somnil.app.network.model.LoginResponse
import com.somnil.app.network.model.RegisterRequest
import com.somnil.app.network.model.RegisterResponse
import com.somnil.app.network.model.TrainingDataUploadRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository wrapping SomnilApi with mock fallback for offline / dev testing.
 */
@Singleton
class SomnilRepository @Inject constructor() {

    // ─── Auth ────────────────────────────────────────────────────────────────

    /**
     * Register a new user.
     * Falls back to mock success when network unavailable.
     */
    suspend fun register(email: String, password: String, deviceId: String): RegisterResponse {
        return try {
            ApiClient.somnilApi.register(RegisterRequest(email, password, deviceId))
        } catch (e: Exception) {
            // Mock fallback for offline dev
            RegisterResponse(
                success = true,
                userId = "mock_${System.currentTimeMillis()}",
                message = "Mock register (offline)"
            )
        }
    }

    /**
     * Login with email + password.
     * Falls back to mock success when network unavailable.
     */
    suspend fun login(email: String, password: String, deviceId: String): LoginResponse {
        return try {
            ApiClient.somnilApi.login(LoginRequest(email, password, deviceId))
        } catch (e: Exception) {
            // Mock fallback for offline dev
            LoginResponse(
                success = true,
                token = "mock_token_${System.currentTimeMillis()}",
                userId = "mock_user",
                message = "Mock login (offline)"
            )
        }
    }

    // ─── Training Data ───────────────────────────────────────────────────────

    /**
     * Upload a night's training data to cloud.
     * Falls back to mock success when network unavailable.
     */
    suspend fun uploadTrainingData(
        userId: String,
        date: Long,
        avgStalta: Double,
        stdStalta: Double,
        anxietyCount: Int,
        totalDurationSeconds: Long,
        trainingPhase: Int,
        sessionId: String
    ): ApiResponse {
        return try {
            ApiClient.somnilApi.uploadTrainingData(
                TrainingDataUploadRequest(
                    userId = userId,
                    date = date,
                    avgStalta = avgStalta,
                    stdStalta = stdStalta,
                    anxietyCount = anxietyCount,
                    totalDurationSeconds = totalDurationSeconds,
                    trainingPhase = trainingPhase,
                    sessionId = sessionId
                )
            )
        } catch (e: Exception) {
            // Mock fallback for offline dev
            ApiResponse(
                success = true,
                message = "Mock upload (offline)"
            )
        }
    }
}
