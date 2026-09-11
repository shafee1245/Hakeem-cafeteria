package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserEntity
import com.example.data.repository.HakeemRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import com.google.firebase.messaging.FirebaseMessaging
import java.util.UUID
import kotlin.coroutines.resume

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import com.example.data.remote.SupabaseService

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Authenticated(val user: UserEntity) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(private val repository: HakeemRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val supabaseAuth = SupabaseService.client.auth

    init {
        viewModelScope.launch {
            supabaseAuth.sessionStatus.collect { status ->
                when (status) {
                    is io.github.jan.supabase.auth.status.SessionStatus.Authenticated -> {
                        val sessionUser = status.session.user ?: supabaseAuth.currentUserOrNull()
                        if (sessionUser != null) {
                            _uiState.value = AuthUiState.Loading
                            val userResult = repository.createOrUpdateUser(
                                id = sessionUser.id,
                                email = sessionUser.email ?: "",
                                fullName = sessionUser.userMetadata?.get("full_name")?.toString()?.trim('"') ?: "User"
                            )
                            userResult.onSuccess { user ->
                                _currentUser.value = user
                                _uiState.value = AuthUiState.Authenticated(user)
                                syncFcmToken(user.id)
                            }.onFailure { e ->
                                _uiState.value = AuthUiState.Error(com.example.utils.ErrorMapper.getUserFriendlyMessage(e))
                            }
                        }
                    }
                    is io.github.jan.supabase.auth.status.SessionStatus.NotAuthenticated -> {
                        _uiState.value = AuthUiState.Idle
                        _currentUser.value = null
                    }
                    else -> {
                        // Keep idle or loading for LoadingFromStorage
                    }
                }
            }
        }
    }

    fun login(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter email/register number and password")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val cleanIdentifier = identifier.trim()
            val emailToUse = if (cleanIdentifier.contains("@")) {
                cleanIdentifier
            } else {
                repository.getEmailByRegisterNumber(cleanIdentifier) ?: run {
                    _uiState.value = AuthUiState.Error("Register Number not found")
                    return@launch
                }
            }

            try {
                supabaseAuth.signInWith(Email) {
                    email = emailToUse
                    this.password = password
                }
                
                val sessionUser = supabaseAuth.currentUserOrNull() ?: throw Exception("Login failed")
                val userResult = repository.getUserById(sessionUser.id)
                if (userResult != null) {
                    _currentUser.value = userResult
                    _uiState.value = AuthUiState.Authenticated(userResult)
                    syncFcmToken(userResult.id)
                } else {
                    _uiState.value = AuthUiState.Error("Profile not found")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(com.example.utils.ErrorMapper.getUserFriendlyMessage(e))
            }
        }
    }

    fun loginWithGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                supabaseAuth.signInWith(IDToken) {
                    this.idToken = idToken
                    provider = Google
                }
                
                val sessionUser = supabaseAuth.currentUserOrNull() ?: throw Exception("Google Sign-In failed")
                val email = sessionUser.email ?: ""
                val displayName = sessionUser.userMetadata?.get("full_name")?.toString()?.trim('"') ?: "Google User"

                val userResult = repository.createOrUpdateUser(
                    id = sessionUser.id,
                    email = email,
                    fullName = displayName
                )
                userResult.onSuccess { user ->
                    _currentUser.value = user
                    _uiState.value = AuthUiState.Authenticated(user)
                    syncFcmToken(user.id)
                }.onFailure { e ->
                    _uiState.value = AuthUiState.Error(com.example.utils.ErrorMapper.getUserFriendlyMessage(e))
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(com.example.utils.ErrorMapper.getUserFriendlyMessage(e))
            }
        }
    }

    fun register(
        fullName: String,
        registerNumber: String,
        department: String,
        year: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ) {
        if (fullName.isBlank() || registerNumber.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in all required fields")
            return
        }
        if (password != confirmPassword) {
            _uiState.value = AuthUiState.Error("Passwords do not match")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            try {
                supabaseAuth.signUpWith(Email) {
                    this.email = email.trim()
                    this.password = password
                    data = kotlinx.serialization.json.buildJsonObject {
                        put("full_name", kotlinx.serialization.json.JsonPrimitive(fullName.trim()))
                    }
                }
                
                val sessionUser = supabaseAuth.currentUserOrNull() ?: throw Exception("Signup failed")

                // Save complete Student Profile in Supabase public.users Table
                val localResult = repository.registerStudent(
                    userId = sessionUser.id,
                    fullName = fullName.trim(),
                    registerNumber = registerNumber.trim(),
                    department = department.trim(),
                    year = year,
                    email = email.trim(),
                    phone = phone.trim()
                )

                localResult.onSuccess { user ->
                    _currentUser.value = user
                    _uiState.value = AuthUiState.Authenticated(user)
                    syncFcmToken(user.id)
                }.onFailure { error ->
                    _uiState.value = AuthUiState.Error(com.example.utils.ErrorMapper.getUserFriendlyMessage(error))
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(com.example.utils.ErrorMapper.getUserFriendlyMessage(e))
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                supabaseAuth.signOut()
            } catch (_: Exception) {}
            _currentUser.value = null
            _uiState.value = AuthUiState.Idle
        }
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }
    
    fun updateProfile(newName: String, newDept: String, newPhone: String, newRegNo: String, newYear: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateUserProfile(user.id, newName, newDept, newPhone, newRegNo, newYear)
            // Update local state directly so UI responds immediately
            val updatedUser = user.copy(
                fullName = newName,
                department = newDept,
                phone = newPhone,
                registerNumber = newRegNo,
                year = newYear
            )
            _currentUser.value = updatedUser
            _uiState.value = AuthUiState.Authenticated(updatedUser)
        }
    }
    
    private fun syncFcmToken(uid: String) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            viewModelScope.launch {
                try {
                    // Update FCM token mapping to the new Supabase UID
                    repository.saveFcmToken(uid, token)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
