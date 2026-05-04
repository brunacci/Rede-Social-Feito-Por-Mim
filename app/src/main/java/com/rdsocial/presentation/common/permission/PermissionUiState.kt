package com.rdsocial.presentation.common.permission

enum class PermissionStatus {
    Granted,
    Denied,
    PermanentlyDenied,
}

data class PermissionUiState(
    val galleryStatus: PermissionStatus = PermissionStatus.Denied,
    val cameraStatus: PermissionStatus = PermissionStatus.Denied,
    val locationStatus: PermissionStatus = PermissionStatus.Denied,
    val isLocationServiceEnabled: Boolean = true,
)
