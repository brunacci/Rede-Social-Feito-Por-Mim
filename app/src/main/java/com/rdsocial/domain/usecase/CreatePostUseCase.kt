package com.rdsocial.domain.usecase

import android.net.Uri
import com.rdsocial.domain.repository.PostRepository
import javax.inject.Inject

class CreatePostUseCase @Inject constructor(
    private val postRepository: PostRepository,
) {
    suspend operator fun invoke(
        imageUri: Uri,
        description: String,
        city: String?,
    ): Result<Unit> = postRepository.createPost(
        imageUri = imageUri,
        description = description,
        city = city,
    )
}
