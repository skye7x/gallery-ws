package com.bzygordev.gallery.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bzygordev.gallery.data.MediaType
import com.bzygordev.gallery.ui.components.GalleryTopBar
import com.bzygordev.gallery.ui.screens.AlbumsScreen
import com.bzygordev.gallery.ui.screens.ForYouScreen
import com.bzygordev.gallery.ui.screens.PhotoDetailViewer
import com.bzygordev.gallery.ui.screens.PhotosScreen
import com.bzygordev.gallery.ui.screens.SearchScreen
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import com.kyant.backdrop.catalog.components.LiquidBottomTabs

@Composable
fun GalleryMainScreen(
    viewModel: GalleryViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val allMedia by viewModel.allMedia.collectAsStateWithLifecycle()
    val filteredMedia by viewModel.filteredMedia.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val photosViewMode by viewModel.photosViewMode.collectAsStateWithLifecycle()
    val mediaFilter by viewModel.mediaFilter.collectAsStateWithLifecycle()
    val gridColumns by viewModel.gridColumns.collectAsStateWithLifecycle()
    val isSquareAspect by viewModel.isSquareAspect.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val activePhoto by viewModel.activePhoto.collectAsStateWithLifecycle()
    val showInfoSheet by viewModel.showInfoSheet.collectAsStateWithLifecycle()
    val showEditSheet by viewModel.showEditSheet.collectAsStateWithLifecycle()
    val adjustments by viewModel.adjustments.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedAlbum by viewModel.selectedAlbum.collectAsStateWithLifecycle()
    val selectedAlbumInfo by viewModel.selectedAlbumInfo.collectAsStateWithLifecycle()
    val searchSuggestions by viewModel.searchSuggestions.collectAsStateWithLifecycle()

    val isLight = !isSystemInDarkTheme()
    val backgroundColor = if (isLight) Color(0xFFFFFFFF) else Color(0xFF000000)

    var showDeleteSelectedDialog by remember { mutableStateOf(false) }

    // Deleting real files needs the user's confirmation in a system dialog.
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onDeleteResult(result.resultCode == Activity.RESULT_OK)
    }
    LaunchedEffect(viewModel) {
        viewModel.deleteRequests.collect { intentSender ->
            deleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        }
    }

    // Pick up photos added, removed or moved while the app was in the background.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    // Root liquid backdrop layer
    val rootBackdrop = rememberLayerBackdrop()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Main layout
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            GalleryTopBar(
                selectedTab = selectedTab,
                photosViewMode = photosViewMode,
                onPhotosViewModeChanged = { viewModel.setPhotosViewMode(it) },
                isSelectionMode = isSelectionMode,
                selectedCount = selectedIds.size,
                onToggleSelectionMode = { viewModel.setSelectionMode(!isSelectionMode) },
                onSelectAll = { viewModel.selectAll() },
                onDeselectAll = { viewModel.deselectAll() },
                onDeleteSelected = { showDeleteSelectedDialog = true },
                isSquareAspect = isSquareAspect,
                onToggleSquareAspect = { viewModel.toggleSquareAspect() },
                gridColumns = gridColumns,
                onCycleColumns = {
                    val nextCols = if (gridColumns == 3) 4 else if (gridColumns == 4) 5 else 3
                    viewModel.setGridColumns(nextCols)
                },
                selectedAlbum = selectedAlbumInfo?.title,
                selectedAlbumOnSdCard = selectedAlbumInfo?.isOnSdCard == true,
                onBackFromAlbum = { viewModel.setSelectedAlbum(null) },
                backdrop = rootBackdrop,
                modifier = Modifier.statusBarsPadding()
            )

            // Current Screen Content (recorded into backdrop for floating bottom tabs)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .layerBackdrop(rootBackdrop)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it / 4 } + fadeIn() togetherWith slideOutHorizontally { -it / 4 } + fadeOut()
                        } else {
                            slideInHorizontally { -it / 4 } + fadeIn() togetherWith slideOutHorizontally { it / 4 } + fadeOut()
                        }
                    },
                    label = "tab_content"
                ) { tab ->
                    when (tab) {
                    0 -> {
                        PhotosScreen(
                            mediaList = filteredMedia,
                            photosViewMode = photosViewMode,
                            activeFilter = mediaFilter,
                            onFilterChanged = { viewModel.setMediaFilter(it) },
                            isSelectionMode = isSelectionMode,
                            selectedIds = selectedIds,
                            onItemClick = { item ->
                                if (isSelectionMode) {
                                    viewModel.toggleItemSelection(item.id)
                                } else {
                                    viewModel.setActivePhoto(item)
                                }
                            },
                            onItemLongClick = { item ->
                                viewModel.setSelectionMode(true)
                                viewModel.toggleItemSelection(item.id)
                            },
                            isSquareAspect = isSquareAspect,
                            gridColumns = gridColumns,
                            backdrop = rootBackdrop
                        )
                    }
                    1 -> {
                        ForYouScreen(
                            mediaList = allMedia,
                            onItemClick = { viewModel.setActivePhoto(it) },
                            backdrop = rootBackdrop
                        )
                    }
                    2 -> {
                        if (selectedAlbum != null) {
                            // Show album drilldown grid
                            PhotosScreen(
                                mediaList = filteredMedia,
                                photosViewMode = PhotosViewMode.ALL,
                                activeFilter = MediaFilter.ALL,
                                onFilterChanged = {},
                                isSelectionMode = isSelectionMode,
                                selectedIds = selectedIds,
                                onItemClick = { item ->
                                    if (isSelectionMode) {
                                        viewModel.toggleItemSelection(item.id)
                                    } else {
                                        viewModel.setActivePhoto(item)
                                    }
                                },
                                onItemLongClick = { item ->
                                    viewModel.setSelectionMode(true)
                                    viewModel.toggleItemSelection(item.id)
                                },
                                isSquareAspect = isSquareAspect,
                                gridColumns = gridColumns,
                                backdrop = rootBackdrop
                            )
                        } else {
                            AlbumsScreen(
                                mediaList = allMedia,
                                onSelectAlbum = { albumName ->
                                    viewModel.setSelectedAlbum(albumName)
                                },
                                onSelectMediaType = { typeName ->
                                    when (typeName) {
                                        "Videos" -> viewModel.setMediaFilter(MediaFilter.VIDEOS)
                                        "Screenshots" -> viewModel.setMediaFilter(MediaFilter.SCREENSHOTS)
                                        "Panoramas" -> viewModel.setMediaFilter(MediaFilter.PANORAMAS)
                                        else -> viewModel.setMediaFilter(MediaFilter.ALL)
                                    }
                                    viewModel.setSelectedTab(0)
                                },
                                backdrop = rootBackdrop
                            )
                        }
                    }
                    3 -> {
                        SearchScreen(
                            searchQuery = searchQuery,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            results = filteredMedia,
                            suggestions = searchSuggestions,
                            onItemClick = { viewModel.setActivePhoto(it) },
                            backdrop = rootBackdrop
                        )
                    }
                }
                }
            }
        }

        // Floating iOS Liquid Glass Bottom Tabs
        if (activePhoto == null && !isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                LiquidBottomTabs(
                    selectedTabIndex = { selectedTab },
                    onTabSelected = { tab ->
                        viewModel.setSelectedTab(tab)
                        if (tab != 2) {
                            viewModel.setSelectedAlbum(null)
                        }
                    },
                    backdrop = rootBackdrop,
                    tabsCount = 4,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val tabs = listOf(
                        "Photos" to Icons.Default.PhotoLibrary,
                        "For You" to Icons.Default.AutoAwesome,
                        "Albums" to Icons.Default.Folder,
                        "Search" to Icons.Default.Search
                    )
                    tabs.forEachIndexed { index, (label, icon) ->
                        val isSelected = index == selectedTab
                        val tabColor = if (isSelected) {
                            if (isLight) Color(0xFF007AFF) else Color(0xFF0A84FF)
                        } else {
                            if (isLight) Color(0xFF8E8E93) else Color(0xFF98989D)
                        }
                        LiquidBottomTab(
                            onClick = {
                                viewModel.setSelectedTab(index)
                                if (index != 2) {
                                    viewModel.setSelectedAlbum(null)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = tabColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = tabColor
                            )
                    }
                }
                }
            }
        }

        // Full Screen Photo Detail Viewer Overlay
        AnimatedVisibility(
            visible = activePhoto != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val photo = activePhoto
            if (photo != null) {
                PhotoDetailViewer(
                    photo = photo,
                    adjustments = adjustments,
                    onAdjustmentsChanged = { viewModel.updateAdjustments(it) },
                    showInfoSheet = showInfoSheet,
                    onShowInfoSheet = { viewModel.setShowInfoSheet(it) },
                    showEditSheet = showEditSheet,
                    onShowEditSheet = { viewModel.setShowEditSheet(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(photo) },
                    onDeletePhoto = { viewModel.deletePhoto(photo) },
                    onDismiss = { viewModel.setActivePhoto(null) }
                )
            }
        }
    }

    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = { Text(if (selectedIds.size == 1) "Delete 1 item?" else "Delete ${selectedIds.size} items?") },
            text = { Text("The selected photos and videos will be permanently deleted from your device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteSelectedDialog = false
                        viewModel.deleteSelected()
                    }
                ) {
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
