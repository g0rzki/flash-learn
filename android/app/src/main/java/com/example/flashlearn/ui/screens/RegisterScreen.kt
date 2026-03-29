package com.example.flashlearn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.flashlearn.R
import com.example.flashlearn.data.remote.RegisterRequest
import com.example.flashlearn.data.remote.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var apiError by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val errEmailRequired = stringResource(R.string.error_email_required)
    val errEmailInvalid = stringResource(R.string.error_email_invalid)
    val errPasswordRequired = stringResource(R.string.error_password_required)
    val errPasswordMinLength = stringResource(R.string.error_password_min_length)
    val errConfirmPasswordRequired = stringResource(R.string.error_confirm_password_required)
    val errPasswordsMismatch = stringResource(R.string.error_passwords_mismatch)
    val errEmailTaken = stringResource(R.string.error_email_taken)
    val errInvalidData = stringResource(R.string.error_invalid_data)
    val errServerCodeFmt = stringResource(R.string.error_server_code)
    val errConnection = stringResource(R.string.error_connection)

    fun validateEmail(): Boolean {
        emailError = when {
            email.isBlank() -> errEmailRequired
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> errEmailInvalid
            else -> null
        }
        return emailError == null
    }

    fun validatePassword(): Boolean {
        passwordError = when {
            password.isBlank() -> errPasswordRequired
            password.length < 8 -> errPasswordMinLength
            else -> null
        }
        return passwordError == null
    }

    fun validateConfirmPassword(): Boolean {
        confirmPasswordError = when {
            confirmPassword.isBlank() -> errConfirmPasswordRequired
            confirmPassword != password -> errPasswordsMismatch
            else -> null
        }
        return confirmPasswordError == null
    }

    fun register() {
        apiError = null
        val isEmailValid = validateEmail()
        val isPasswordValid = validatePassword()
        val isConfirmValid = validateConfirmPassword()

        if (isEmailValid && isPasswordValid && isConfirmValid) {
            isLoading = true
            scope.launch {
                try {
                    RetrofitClient.authApi.register(RegisterRequest(email, password))
                    isLoading = false
                    onRegisterSuccess()
                } catch (e: HttpException) {
                    isLoading = false
                    apiError = when (e.code()) {
                        409 -> errEmailTaken
                        400 -> errInvalidData
                        else -> String.format(errServerCodeFmt, e.code())
                    }
                } catch (e: Exception) {
                    isLoading = false
                    apiError = errConnection
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        Text(
            text = stringResource(R.string.register_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                if (emailError != null) validateEmail()
            },
            label = { Text(stringResource(R.string.label_email)) },
            isError = emailError != null,
            supportingText = emailError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                if (passwordError != null) validatePassword()
            },
            label = { Text(stringResource(R.string.label_password)) },
            isError = passwordError != null,
            supportingText = passwordError?.let { { Text(it) } },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                if (confirmPasswordError != null) validateConfirmPassword()
            },
            label = { Text(stringResource(R.string.label_confirm_password)) },
            isError = confirmPasswordError != null,
            supportingText = confirmPasswordError?.let { { Text(it) } },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (apiError != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = apiError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { register() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.btn_register))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text(stringResource(R.string.register_have_account))
        }
        }
    }
}
