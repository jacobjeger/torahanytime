package com.torahanytime.audio.ui.auth

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.torahanytime.audio.data.api.ApiClient
import com.torahanytime.audio.data.api.AuthManager
import com.torahanytime.audio.data.model.LoginRequest
import com.torahanytime.audio.ui.theme.TATBlue
import com.torahanytime.audio.ui.theme.TATOrange
import com.torahanytime.audio.ui.theme.TATTextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val api = ApiClient.api

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _error.value = "Please enter email and password"
            return
        }
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val response = api.login(LoginRequest(email = email, password = password))
                if (response.status == "success" && response.token != null) {
                    AuthManager.saveLogin(
                        token = response.token,
                        userId = response.id ?: "",
                        email = response.email ?: email,
                        expiration = response.expiration ?: 0
                    )
                    _loginSuccess.value = true
                } else {
                    _error.value = response.message ?: "Login failed"
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Login failed"
                _error.value = if ("401" in msg || "400" in msg) "Invalid email or password"
                else "Connection error. Please try again."
            }
            _loading.value = false
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onSkip: () -> Unit,
    vm: LoginViewModel = viewModel()
) {
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val loginSuccess by vm.loginSuccess.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(loginSuccess) {
        if (loginSuccess) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "TorahAnytime",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TATBlue
        )

        Spacer(Modifier.height(4.dp))

        Text(
            "Sign in to sync your favorites,\nplaylists, and listen history",
            fontSize = 13.sp,
            color = TATTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .focusable(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            shape = MaterialTheme.shapes.medium
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier
                .fillMaxWidth()
                .focusable(),
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None
                else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    vm.login(email, password)
                }
            ),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "Hide" else "Show"
                    )
                }
            },
            shape = MaterialTheme.shapes.medium
        )

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                error ?: "",
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { vm.login(email, password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .focusable(),
            enabled = !loading,
            colors = ButtonDefaults.buttonColors(containerColor = TATBlue)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Sign In", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(12.dp))

        TextButton(
            onClick = onSkip,
            modifier = Modifier.focusable()
        ) {
            Text("Continue without signing in", color = TATTextSecondary, fontSize = 13.sp)
        }
    }
}
