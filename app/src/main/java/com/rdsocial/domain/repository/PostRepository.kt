package com.rdsocial.domain.repository

import android.net.Uri
import com.rdsocial.domain.model.Post

interface PostRepository {
    suspend fun createPost(
        imageUri: Uri,
        description: String,
        city: String?,
    ): Result<Unit>

    suspend fun getPostsPage(
        pageSize: Long,
        searchCity: String?,
        lastTimestamp: Long?,
    ): Result<List<Post>>
}
