package com.rdsocial.domain.model

data class Post(
    val id: String,
    val imageBase64: String,
    val description: String,
    val city: String?,
    val authorId: String,
    val authorName: String?,
    val timestamp: Long,
)
