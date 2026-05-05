package com.rdsocial.presentation.home

import androidx.lifecycle.viewModelScope
import com.rdsocial.domain.model.Post
import com.rdsocial.domain.repository.AuthRepository
import com.rdsocial.domain.repository.PostRepository
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val pageSize = 5

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var lastTimestamp: Long? = null
    private var searchDebounceJob: Job? = null

    init {
        refresh()
        loadCurrentUserProfile()
    }

    fun loadCurrentUserProfile() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUserWithProfile()
            _uiState.update { it.copy(profileImageBase64 = user?.profileImageBase64) }
        }
    }

    fun onSearchQueryChange(value: String) {
        _uiState.update { it.copy(searchQuery = value, errorMessage = null) }
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(350)
            refresh()
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isInitialLoading = it.posts.isEmpty(),
                    isRefreshing = true,
                    errorMessage = null,
                )
            }
            lastTimestamp = null

            postRepository.getPostsPage(
                pageSize = pageSize.toLong(),
                searchCity = _uiState.value.searchQuery.trim(),
                lastTimestamp = null,
            ).onSuccess { posts ->
                lastTimestamp = posts.lastOrNull()?.timestamp
                _uiState.update {
                    it.copy(
                        posts = posts.map(::toUiModel),
                        isInitialLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        canLoadMore = posts.size == pageSize,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = error.localizedMessage ?: "Erro ao carregar o feed",
                    )
                }
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoadingMore || !_uiState.value.canLoadMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
            postRepository.getPostsPage(
                pageSize = pageSize.toLong(),
                searchCity = _uiState.value.searchQuery.trim(),
                lastTimestamp = lastTimestamp,
            ).onSuccess { posts ->
                lastTimestamp = posts.lastOrNull()?.timestamp ?: lastTimestamp
                _uiState.update {
                    it.copy(
                        posts = it.posts + posts.map(::toUiModel),
                        isLoadingMore = false,
                        canLoadMore = posts.size == pageSize,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        errorMessage = error.localizedMessage ?: "Erro ao carregar mais posts",
                    )
                }
            }
        }
    }

    private fun toUiModel(post: Post): HomePostUiModel = HomePostUiModel(
        id = post.id,
        authorName = post.authorName ?: "Usuario",
        city = post.city,
        description = post.description,
        publishedAtLabel = formatTimestamp(post.timestamp),
        imageBase64 = post.imageBase64,
    )

    private fun formatTimestamp(timestamp: Long): String {
        val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0)
        val minute = 60_000L
        val hour = 60 * minute
        val day = 24 * hour
        return when {
            diff < hour -> "Ha ${diff / minute} min"
            diff < day -> "Ha ${diff / hour} h"
            else -> "Ha ${diff / day} d"
        }
    }
}
