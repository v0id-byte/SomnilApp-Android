package com.somnil.app.network.model

import com.google.gson.annotations.SerializedName

// ─── Auth ────────────────────────────────────────────────────────────────────

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("device_id") val deviceId: String
)

data class RegisterResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("user_id") val userId: String?,
    @SerializedName("message") val message: String?
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("device_id") val deviceId: String
)

data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("token") val token: String?,
    @SerializedName("user_id") val userId: String?,
    @SerializedName("message") val message: String?
)

// ─── Training Data Upload ─────────────────────────────────────────────────────

data class TrainingDataUploadRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("date") val date: Long,
    @SerializedName("avg_stalta") val avgStalta: Double,
    @SerializedName("std_stalta") val stdStalta: Double,
    @SerializedName("anxiety_count") val anxietyCount: Int,
    @SerializedName("total_duration_seconds") val totalDurationSeconds: Long,
    @SerializedName("training_phase") val trainingPhase: Int,
    @SerializedName("session_id") val sessionId: String
)

data class ApiResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?
)
