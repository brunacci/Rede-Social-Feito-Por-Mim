package com.rdsocial.presentation.home

import android.app.Activity
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rdsocial.R
import com.rdsocial.presentation.common.permission.PermissionStatus
import com.rdsocial.presentation.common.permission.PermissionUtils
import com.rdsocial.presentation.common.permission.PermissionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    permissionViewModel: PermissionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(context) { context as? Activity }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionState by permissionViewModel.uiState.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    var wasRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isRefreshing) {
        if (wasRefreshing && !uiState.isRefreshing && uiState.posts.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
        wasRefreshing = uiState.isRefreshing
    }

    var galleryRequestedOnce by rememberSaveable { mutableStateOf(false) }
    var locationRequestedOnce by rememberSaveable { mutableStateOf(false) }
    var cameraRequestedOnce by rememberSaveable { mutableStateOf(false) }
    var showCreatePostDialog by rememberSaveable { mutableStateOf(false) }

    val galleryPermission = PermissionUtils.galleryPermission()
    val locationPermissions = PermissionUtils.locationPermissions()
    val cameraPermission = PermissionUtils.cameraPermission()

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        permissionViewModel.updateGalleryStatus(
            PermissionUtils.resolveStatus(
                activity = activity,
                context = context,
                permission = galleryPermission,
                hasRequestedBefore = galleryRequestedOnce,
            ),
        )
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionViewModel.updateLocationStatus(
            PermissionUtils.resolveLocationStatus(
                activity = activity,
                context = context,
                hasRequestedBefore = locationRequestedOnce,
            ),
        )
        permissionViewModel.updateLocationService(PermissionUtils.isLocationServiceEnabled(context))
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        permissionViewModel.updateCameraStatus(
            PermissionUtils.resolveStatus(
                activity = activity,
                context = context,
                permission = cameraPermission,
                hasRequestedBefore = cameraRequestedOnce,
            ),
        )
    }

    LaunchedEffect(
        lifecycleOwner,
        galleryRequestedOnce,
        locationRequestedOnce,
        cameraRequestedOnce,
    ) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            permissionViewModel.updateGalleryStatus(
                PermissionUtils.resolveStatus(
                    activity = activity,
                    context = context,
                    permission = galleryPermission,
                    hasRequestedBefore = galleryRequestedOnce,
                ),
            )
            permissionViewModel.updateLocationStatus(
                PermissionUtils.resolveLocationStatus(
                    activity = activity,
                    context = context,
                    hasRequestedBefore = locationRequestedOnce,
                ),
            )
            permissionViewModel.updateCameraStatus(
                PermissionUtils.resolveStatus(
                    activity = activity,
                    context = context,
                    permission = cameraPermission,
                    hasRequestedBefore = cameraRequestedOnce,
                ),
            )
            permissionViewModel.updateLocationService(
                PermissionUtils.isLocationServiceEnabled(context),
            )
        }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadCurrentUserProfile()
        }
    }

    val allRuntimePermissionsGranted =
        permissionState.galleryStatus == PermissionStatus.Granted &&
            permissionState.cameraStatus == PermissionStatus.Granted &&
            permissionState.locationStatus == PermissionStatus.Granted

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    HomeProfileToolbarIcon(
                        profileImageBase64 = uiState.profileImageBase64,
                        onNavigateToProfile = onNavigateToProfile,
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreatePostDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.action_create_post),
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                label = { Text(stringResource(R.string.home_search_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
            )

            if (!allRuntimePermissionsGranted) {
                PermissionStatusCard(
                    galleryStatus = permissionState.galleryStatus,
                    cameraStatus = permissionState.cameraStatus,
                    locationStatus = permissionState.locationStatus,
                    isLocationServiceEnabled = permissionState.isLocationServiceEnabled,
                    onRequestGallery = {
                        galleryRequestedOnce = true
                        galleryLauncher.launch(galleryPermission)
                    },
                    onRequestCamera = {
                        cameraRequestedOnce = true
                        cameraLauncher.launch(cameraPermission)
                    },
                    onRequestLocation = {
                        locationRequestedOnce = true
                        locationLauncher.launch(locationPermissions)
                    },
                    onOpenSettings = { PermissionUtils.openAppSettings(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }

            val pullToRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                state = pullToRefreshState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
            ) {
                when {
                    uiState.isInitialLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.posts.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.home_empty_state),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            uiState.errorMessage?.let { message ->
                                Text(
                                    text = message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(uiState.posts, key = { it.id }) { post ->
                                HomePostCard(post = post)
                            }
                            uiState.errorMessage?.let { message ->
                                item {
                                    Text(
                                        text = message,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                    )
                                }
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    if (uiState.isLoadingMore) {
                                        CircularProgressIndicator()
                                    } else if (uiState.canLoadMore) {
                                        TextButton(onClick = viewModel::loadMore) {
                                            Text(stringResource(R.string.action_load_more))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePostDialog) {
        CreatePostDialog(
            galleryStatus = permissionState.galleryStatus,
            locationStatus = permissionState.locationStatus,
            isLocationServiceEnabled = permissionState.isLocationServiceEnabled,
            onRequestGalleryPermission = {
                galleryRequestedOnce = true
                galleryLauncher.launch(galleryPermission)
            },
            onRequestLocationPermission = {
                locationRequestedOnce = true
                locationLauncher.launch(locationPermissions)
            },
            onDismiss = { showCreatePostDialog = false },
            onPublishSuccess = {
                showCreatePostDialog = false
                viewModel.refresh()
            },
        )
    }
}

@Composable
private fun PermissionStatusCard(
    galleryStatus: PermissionStatus,
    cameraStatus: PermissionStatus,
    locationStatus: PermissionStatus,
    isLocationServiceEnabled: Boolean,
    onRequestGallery: () -> Unit,
    onRequestCamera: () -> Unit,
    onRequestLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.home_permissions_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(
                    R.string.home_permission_gallery_status,
                    galleryStatus.toReadableText(),
                ),
            )
            Text(
                text = stringResource(
                    R.string.home_permission_camera_status,
                    cameraStatus.toReadableText(),
                ),
            )
            Text(
                text = stringResource(
                    R.string.home_permission_location_status,
                    locationStatus.toReadableText(),
                ),
            )
            if (!isLocationServiceEnabled) {
                Text(
                    text = stringResource(R.string.home_location_service_disabled),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onRequestGallery) {
                    Text(stringResource(R.string.action_request_gallery))
                }
                TextButton(onClick = onRequestCamera) {
                    Text(stringResource(R.string.action_request_camera))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onRequestLocation) {
                    Text(stringResource(R.string.action_request_location))
                }
                if (
                    galleryStatus == PermissionStatus.PermanentlyDenied ||
                    cameraStatus == PermissionStatus.PermanentlyDenied ||
                    locationStatus == PermissionStatus.PermanentlyDenied
                ) {
                    TextButton(onClick = onOpenSettings) {
                        Text(stringResource(R.string.action_open_settings))
                    }
                }
            }
        }
    }
}

private fun PermissionStatus.toReadableText(): String = when (this) {
    PermissionStatus.Granted -> "Concedida"
    PermissionStatus.Denied -> "Negada"
    PermissionStatus.PermanentlyDenied -> "Negada permanentemente"
}

@Composable
private fun HomeProfileToolbarIcon(
    profileImageBase64: String?,
    onNavigateToProfile: () -> Unit,
) {
    val avatarBitmap = remember(profileImageBase64) {
        if (!profileImageBase64.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(profileImageBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }
    IconButton(onClick = onNavigateToProfile) {
        if (avatarBitmap != null) {
            Image(
                bitmap = avatarBitmap,
                contentDescription = stringResource(R.string.profile_photo_content_description),
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = stringResource(R.string.action_go_profile),
            )
        }
    }
}

@Composable
private fun HomePostCard(post: HomePostUiModel) {
    val imageBitmap = remember(post.imageBase64) {
        if (!post.imageBase64.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(post.imageBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = post.authorName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                post.city?.let { city ->
                    Text(
                        text = city,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = post.description,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = post.publishedAtLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = stringResource(R.string.home_post_image_content_description),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}
