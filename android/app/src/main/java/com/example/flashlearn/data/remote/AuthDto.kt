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