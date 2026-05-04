package com.rdsocial.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.rdsocial.data.image.Base64ImageEncoder
import com.rdsocial.domain.model.AuthUser
import com.rdsocial.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthRepository {

    override fun getCurrentUser(): AuthUser? =
        firebaseAuth.currentUser?.toAuthUser()

    override suspend fun getCurrentUserWithProfile(): AuthUser? {
        val user = firebaseAuth.currentUser ?: return null
        val snapshot = firestore.collection(USERS_COLLECTION).document(user.uid).get().await()
        val profileImageBase64 = snapshot.getString(FIELD_PROFILE_IMAGE_BASE64)
        return user.toAuthUser(profileImageBase64 = profileImageBase64)
    }

    override fun authState(): Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toAuthUser())
        }
        firebaseAuth.addAuthStateListener(listener)
        trySend(firebaseAuth.currentUser?.toAuthUser())
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun signUp(displayName: String, email: String, password: String): Result<Unit> =
        runCatching {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = firebaseAuth.currentUser ?: error("currentUser null after signUp")
            val request = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            user.updateProfile(request).await()
        }

    override suspend fun updateProfile(displayName: String, photoUri: Uri?): Result<Unit> = runCatching {
        val user = firebaseAuth.currentUser ?: error("user not authenticated")

        val profileImageBase64 = if (photoUri != null) {
            Base64ImageEncoder.encodeUriToBase64Jpeg(context, photoUri).getOrElse { throw it }
        } else {
            null
        }

        val request = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        user.updateProfile(request).await()

        val payload = hashMapOf<String, Any>("displayName" to displayName)
        if (profileImageBase64 != null) {
            payload[FIELD_PROFILE_IMAGE_BASE64] = profileImageBase64
        }
        firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .set(payload, SetOptions.merge())
            .await()
    }

    override suspend fun updatePassword(newPassword: String): Result<Unit> = runCatching {
        val user = firebaseAuth.currentUser ?: error("user not authenticated")
        user.updatePassword(newPassword).await()
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val FIELD_PROFILE_IMAGE_BASE64 = "profileImageBase64"
    }
}
