package com.dachkaboiz.betterbudget_bestbudget.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dachkaboiz.betterbudget_bestbudget.data.repository.UserRepository
import androidx.lifecycle.viewModelScope
import com.dachkaboiz.betterbudget_bestbudget.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class UserViewModel(
    private val repository: UserRepository
) : ViewModel() {

    // Login state
    private val _loginState = MutableStateFlow<AuthState>(AuthState.Idle)
    val loginState: StateFlow<AuthState> = _loginState

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    // Register state
    private val _registerState = MutableStateFlow<AuthState>(AuthState.Idle)
    val registerState: StateFlow<AuthState> = _registerState

    // -----------------------------
    // LOGIN
    // -----------------------------
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = AuthState.Loading

            val success = repository.loginUser(email, password)

            _loginState.value = if (success) {
                AuthState.Success
            } else {
                AuthState.Error("Invalid email or password")
            }
        }
    }
    fun loadUser(email: String) {
        viewModelScope.launch {
            val result = repository.getUserByEmail(email)
            _user.postValue(result)
        }
    }
    suspend fun getUserByEmail(email: String) =
        repository.getUserByEmail(email)
    fun updateUserProfile(
        firstName: String?,
        lastName: String?,
        email: String?,
        birthDate: Long?,
        profilePicUri: String?,
        age: Int?
    ) {
        val current = user.value ?: return

        val updatedUser = current.copy(
            firstName = firstName ?: current.firstName,
            surname = lastName ?: current.surname,
            email = email ?: current.email,
            birthDate = birthDate ?: current.birthDate,
            age = age ?: current.age,
            profilePicUri = profilePicUri?: current.profilePicUri ,
            password = current.password
        )

        viewModelScope.launch {
            repository.updateUser(updatedUser)
            _user.postValue(updatedUser) //
        }
    }



    // -----------------------------
    // REGISTER
    // -----------------------------


    fun register(user: User) {
        viewModelScope.launch {
            _registerState.value = AuthState.Loading

            // Check if email already exists
            if (repository.userExists(user.email)) {
                _registerState.value = AuthState.Error("Email already exists")
                return@launch
            }

            // Insert new user
            val result = repository.insertUser(user)

            _registerState.value = result.fold(
                onSuccess = { AuthState.Success },
                onFailure = { AuthState.Error(it.message ?: "Unknown error") }
            )
        }
    }




        fun deleteUser(email: String) {
            viewModelScope.launch {
                repository.deleteUserByEmail(email)
            }
        }



    fun resetStates() {
        _loginState.value = AuthState.Idle
        _registerState.value = AuthState.Idle
    }
}