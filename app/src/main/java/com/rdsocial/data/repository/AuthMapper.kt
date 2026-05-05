package com.rdsocial.data.repository

import com.google.firebase.auth.FirebaseUser
import com.rdsocial.domain.model.AuthUser

internal fun FirebaseUser.toAuthUser(profileImageBase64: String? = null): AuthUser = AuthUser(
    uid = uid,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl?.toString(),
    profileImageBase64 = profileImageBase64,
)
