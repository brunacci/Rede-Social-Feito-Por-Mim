package com.rdsocial.presentation.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rdsocial.R
import com.rdsocial.presentation.common.permission.PermissionStatus

@Composable
fun CreatePostDialog(
    galleryStatus: PermissionStatus,
    locationStatus: PermissionStatus,
    isLocationServiceEnabled: Boolean,
    onRequestGalleryPermission: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onDismiss: () -> Unit,
    onPublishSuccess: () -> Unit,
    viewModel: CreatePostViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLocationField by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            viewModel.resetAfterPublishSuccess()
            onPublishSuccess()
        }
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        viewModel.onImageSelected(uri)
    }

    AlertDialog(
        onDismissRequest = {
            if (!uiState.isPublishing) onDismiss()
        },
        title = { Text(stringResource(R.string.create_post_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(
                    onClick = {
                        if (galleryStatus == PermissionStatus.Granted) {
                            imagePickerLauncher.launch("image/*")
                        } else {
                            onRequestGalleryPermission()
                        }
                    },
                    enabled = !uiState.isPublishing,
                ) {
                    Text(
                        text = if (uiState.imageUri != null) {
                            stringResource(R.string.create_post_image_selected)
                        } else {
                            stringResource(R.string.create_post_select_image)
                        },
                    )
                }

                uiState.imageUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = stringResource(R.string.create_post_image_preview_description),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }

                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    label = { Text(stringResource(R.string.create_post_description_label)) },
                    enabled = !uiState.isPublishing,
                )

                TextButton(
                    onClick = {
                        showLocationField = true
                        if (locationStatus == PermissionStatus.Granted && isLocationServiceEnabled) {
                            viewModel.loadCityFromLocation()
                        } else {
                            onRequestLocationPermission()
                        }
                    },
                    enabled = !uiState.isPublishing && !uiState.isLoadingCity,
                ) {
                    if (uiState.isLoadingCity) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    }
                    Text(stringResource(R.string.create_post_use_current_location))
                }

                if (showLocationField || !uiState.city.isNullOrBlank()) {
                    Text(
                        text = uiState.city.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                uiState.errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                uiState.successMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !uiState.isPublishing,
                onClick = viewModel::publish,
            ) {
                if (uiState.isPublishing) {
                    CircularProgressIndicator()
                } else {
                    Text(stringResource(R.string.create_post_publish))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !uiState.isPublishing,
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
