package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.model.AppUser
import com.example.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val user: AppUser) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUser: StateFlow<AppUser?> = authRepository.currentUser
    val demoAccounts = authRepository.demoAccounts

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _emailInput = MutableStateFlow("scanner@sih.gov.in")
    val emailInput: StateFlow<String> = _emailInput.asStateFlow()

    private val _passwordInput = MutableStateFlow("password123")
    val passwordInput: StateFlow<String> = _passwordInput.asStateFlow()

    fun onEmailChange(email: String) {
        _emailInput.value = email
    }

    fun onPasswordChange(pass: String) {
        _passwordInput.value = pass
    }

    fun selectDemoAccount(email: String, pass: String) {
        _emailInput.value = email
        _passwordInput.value = pass
        login()
    }

    fun login() {
        val email = _emailInput.value.trim()
        val pass = _passwordInput.value.trim()

        if (email.isEmpty()) {
            _uiState.value = AuthUiState.Error("Please enter your email address.")
            return
        }
        if (pass.isEmpty()) {
            _uiState.value = AuthUiState.Error("Please enter your password.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.login(email, pass)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = AuthUiState.Success(user)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.localizedMessage ?: "Authentication failed")
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    fun logout() {
        authRepository.logout()
        _uiState.value = AuthUiState.Idle
    }
}
