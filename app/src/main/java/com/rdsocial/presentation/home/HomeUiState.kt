package com.rdsocial.presentation.home

data class HomePostUiModel(
    val id: String,
    val authorName: String,
    val city: String?,
    val description: String,
    val publishedAtLabel: String,
    val imageBase64: String,
)

data class HomeUiState(
    val searchQuery: String = "",
    val profileImageBase64: String? = null,
    val posts: List<HomePostUiModel> = emptyList(),
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val errorMessage: String? = null,
)
