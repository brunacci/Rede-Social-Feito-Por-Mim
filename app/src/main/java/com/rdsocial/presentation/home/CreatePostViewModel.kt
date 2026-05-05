package com.rdsocial.presentation.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rdsocial.R
import com.rdsocial.domain.repository.LocationRepository
import com.rdsocial.domain.usecase.CreatePostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreatePostUiState(
    val imageUri: Uri? = null,
    val description: String = "",
    val city: String? = null,
    val isLoadingCity: Boolean = false,
    val isPublishing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val createPostUseCase: CreatePostUseCase,
    private val locationRepository: LocationRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun onImageSelected(uri: Uri?) {
        _uiState.update {
            it.copy(
                imageUri = uri,
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update {
            it.copy(
                description = value,
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun resetAfterPublishSuccess() {
        _uiState.value = CreatePostUiState()
    }

    fun loadCityFromLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCity = true, errorMessage = null) }
            locationRepository.getCurrentCityNameOrNull()
                .onSuccess { city ->
                    _uiState.update {
                        if (city.isNullOrBlank()) {
                            it.copy(
                                isLoadingCity = false,
                                city = null,
                                errorMessage = appContext.getString(R.string.error_city_optional),
                            )
                        } else {
                            it.copy(
                                city = city,
                                isLoadingCity = false,
                                errorMessage = null,
                            )
                        }
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoadingCity = false,
                            city = null,
                            errorMessage = appContext.getString(R.string.error_city_optional),
                        )
                    }
                }
        }
    }

    fun publish() {
        val snapshot = _uiState.value
        val imageUri = snapshot.imageUri
        val description = snapshot.description.trim()
        if (imageUri == null) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.error_post_image_required))
            }
            return
        }
        if (description.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.error_post_description_required))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isPublishing = true, errorMessage = null, successMessage = null)
            }
            createPostUseCase(
                imageUri = imageUri,
                description = description,
                city = snapshot.city,
            )
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isPublishing = false,
                            successMessage = appContext.getString(R.string.success_post_published),
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isPublishing = false,
                            errorMessage = e.localizedMessage
                                ?: appContext.getString(R.string.error_generic),
                        )
                    }
                }
        }
    }
}
