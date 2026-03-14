package com.example.flashlearn.data.remote

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val access_token: String,
    val refresh_token: String
)

data class RegisterRequest(
    val email: String,
    val password: String
)

data class RegisterResponse(
    val id: Long,
    val email: String,
    val message: String
)