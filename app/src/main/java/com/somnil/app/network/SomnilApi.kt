package com.somnil.app.network

import com.somnil.app.network.model.LoginRequest
import com.somnil.app.network.model.LoginResponse
import com.somnil.app.network.model.RegisterRequest
import com.somnil.app.network.model.RegisterResponse
import com.somnil.app.network.model.TrainingDataUploadRequest
import com.somnil.app.network.model.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Somnil REST API interface.
 * All endpoints are relative to BASE_URL = http://www.pianotuner.top
 */
interface SomnilApi {

    /**
     * Register a new user.
     * POST /api/auth/register
     */
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    /**
     * Login with email + password.
     * POST /api/auth/login
     */
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    /**
     * Upload a night's training data to cloud.
     * POST /api/training/upload
     */
    @POST("api/training/upload")
    suspend fun uploadTrainingData(@Body request: TrainingDataUploadRequest): ApiResponse
}
