package com.rdsocial.domain.repository

import android.net.Uri
import com.rdsocial.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUser(): AuthUser?

    suspend fun getCurrentUserWithProfile(): AuthUser?

    fun authState(): Flow<AuthUser?>

    suspend fun signIn(email: String, password: String): Result<Unit>

    suspend fun signUp(displayName: String, email: String, password: String): Result<Unit>

    suspend fun updateProfile(displayName: String, photoUri: Uri?): Result<Unit>

    suspend fun updatePassword(newPassword: String): Result<Unit>

    suspend fun signOut()
}
