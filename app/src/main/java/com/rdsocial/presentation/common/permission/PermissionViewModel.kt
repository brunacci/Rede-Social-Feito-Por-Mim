package com.rdsocial.presentation.common.permission

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class PermissionViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionUiState())
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

    fun updateGalleryStatus(status: PermissionStatus) {
        _uiState.update { it.copy(galleryStatus = status) }
    }

    fun updateCameraStatus(status: PermissionStatus) {
        _uiState.update { it.copy(cameraStatus = status) }
    }

    fun updateLocationStatus(status: PermissionStatus) {
        _uiState.update { it.copy(locationStatus = status) }
    }

    fun updateLocationService(enabled: Boolean) {
        _uiState.update { it.copy(isLocationServiceEnabled = enabled) }
    }
}
