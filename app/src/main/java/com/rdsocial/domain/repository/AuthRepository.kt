package com.rdsocial.domain.repository

import com.rdsocial.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUser(): AuthUser?

    fun authState(): Flow<AuthUser?>

    suspend fun signIn(email: String, password: String): Result<Unit>

    suspend fun signUp(displayName: String, email: String, password: String): Result<Unit>

    suspend fun signOut()
}
