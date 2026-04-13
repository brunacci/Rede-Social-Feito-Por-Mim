package com.rdsocial.data.repository

import com.google.firebase.auth.FirebaseUser
import com.rdsocial.domain.model.AuthUser

internal fun FirebaseUser.toAuthUser(): AuthUser = AuthUser(
    uid = uid,
    email = email,
    displayName = displayName,
)
