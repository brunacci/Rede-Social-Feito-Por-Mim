package com.rdsocial.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rdsocial.R
import com.rdsocial.data.image.Base64ImageEncoder
import com.rdsocial.domain.model.Post
import com.rdsocial.domain.repository.PostRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class FirebasePostRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : PostRepository {

    override suspend fun createPost(
        imageUri: Uri,
        description: String,
        city: String?,
    ): Result<Unit> = runCatching {
        val user = firebaseAuth.currentUser ?: error("user not authenticated")
        firebaseAuth.currentUser?.getIdToken(false)?.await()

        val imageBase64 = Base64ImageEncoder.encodeUriToBase64Jpeg(context, imageUri).getOrElse { err ->
            error(err.message ?: context.getString(R.string.error_post_image_unreadable))
        }

        val timestamp = System.currentTimeMillis()
        val postId = firestore.collection(POSTS_COLLECTION).document().id

        val postData = hashMapOf(
            "id" to postId,
            "imageBase64" to imageBase64,
            "description" to description,
            "city" to city,
            "cityLower" to city?.trim()?.lowercase(),
            "authorId" to user.uid,
            "authorName" to user.displayName,
            "timestamp" to timestamp,
        )

        firestore.collection(POSTS_COLLECTION)
            .document(postId)
            .set(postData)
            .await()
    }

    override suspend fun getPostsPage(
        pageSize: Long,
        searchCity: String?,
        lastTimestamp: Long?,
    ): Result<List<Post>> = runCatching {
        val cityQuery = searchCity?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        val baseQuery = if (cityQuery == null) {
            firestore.collection(POSTS_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
        } else {
            firestore.collection(POSTS_COLLECTION)
                .whereEqualTo("cityLower", cityQuery)
                .orderBy("timestamp", Query.Direction.DESCENDING)
        }

        val pagedQuery = lastTimestamp?.let { baseQuery.startAfter(it) } ?: baseQuery
        val snapshot = pagedQuery.limit(pageSize).get().await()
        snapshot.documents.mapNotNull { doc ->
            val id = doc.getString("id") ?: doc.id
            val imageBase64 = doc.getString("imageBase64") ?: return@mapNotNull null
            if (imageBase64.isBlank()) return@mapNotNull null
            val description = doc.getString("description") ?: ""
            val docCity = doc.getString("city")
            val authorId = doc.getString("authorId") ?: ""
            val authorName = doc.getString("authorName")
            val docTimestamp = doc.getLong("timestamp") ?: 0L
            Post(
                id = id,
                imageBase64 = imageBase64,
                description = description,
                city = docCity,
                authorId = authorId,
                authorName = authorName,
                timestamp = docTimestamp,
            )
        }
    }

    private companion object {
        const val POSTS_COLLECTION = "posts"
    }
}
