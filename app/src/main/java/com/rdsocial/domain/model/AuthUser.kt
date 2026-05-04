package com.rdsocial.domain.model

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val profileImageBase64: String? = null,
)
