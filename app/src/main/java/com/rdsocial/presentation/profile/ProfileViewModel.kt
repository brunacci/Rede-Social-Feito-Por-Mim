package com.rdsocial.presentation.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rdsocial.R
import com.rdsocial.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val profileImageBase64: String? = null,
    val selectedPhotoUri: Uri? = null,
    val newPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isSavingProfile: Boolean = false,
    val isChangingPassword: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val isBusy: Boolean get() = isSavingProfile || isChangingPassword
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUser()
    }

    fun loadUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUserWithProfile()
            _uiState.update {
                it.copy(
                    displayName = user?.displayName.orEmpty(),
                    email = user?.email.orEmpty(),
                    profileImageBase64 = user?.profileImageBase64,
                    errorMessage = null,
                )
            }
        }
    }

    fun onDisplayNameChange(value: String) {
        _uiState.update { it.copy(displayName = value, errorMessage = null, successMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(newPassword = value, errorMessage = null, successMessage = null) }
    }

    fun onPasswordVisibilityToggle() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onPhotoSelected(uri: Uri?) {
        _uiState.update { it.copy(selectedPhotoUri = uri, errorMessage = null, successMessage = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun saveProfile() {
        val current = _uiState.value
        val name = current.displayName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.error_validation_name)) }
            return
        }
        if (name.length > 80) {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.error_validation_name_length)) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSavingProfile = true, errorMessage = null, successMessage = null)
            }
            authRepository.updateProfile(
                displayName = name,
                photoUri = current.selectedPhotoUri,
            ).onSuccess {
                val refreshed = authRepository.getCurrentUserWithProfile()
                _uiState.update {
                    it.copy(
                        isSavingProfile = false,
                        selectedPhotoUri = null,
                        profileImageBase64 = refreshed?.profileImageBase64,
                        displayName = refreshed?.displayName.orEmpty(),
                        successMessage = appContext.getString(R.string.success_profile_updated),
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isSavingProfile = false,
                        errorMessage = e.localizedMessage ?: appContext.getString(R.string.error_generic),
                    )
                }
            }
        }
    }

    fun updatePassword() {
        val password = _uiState.value.newPassword
        if (password.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.error_validation_password_empty))
            }
            return
        }
        if (password.length < 6) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.error_validation_password_length))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isChangingPassword = true, errorMessage = null, successMessage = null)
            }
            authRepository.updatePassword(password).onSuccess {
                _uiState.update {
                    it.copy(
                        isChangingPassword = false,
                        newPassword = "",
                        successMessage = appContext.getString(R.string.success_password_updated),
                    )
                }
            }.onFailure { e ->
                val message = if (e.message?.contains("recent", ignoreCase = true) == true) {
                    appContext.getString(R.string.error_requires_recent_login)
                } else {
                    e.localizedMessage ?: appContext.getString(R.string.error_generic)
                }
                _uiState.update { it.copy(isChangingPassword = false, errorMessage = message) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
