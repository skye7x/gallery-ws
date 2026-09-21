package com.bzygordev.gallery.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Panorama
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bzygordev.gallery.data.MediaEntity
import com.bzygordev.gallery.ui.MediaFilter
import com.bzygordev.gallery.ui.PhotosViewMode
import com.bzygordev.gallery.ui.components.MediaItemCard
import com.bzygordev.gallery.ui.components.SdCardBadge
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.shapes.Capsule
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotosScreen(
    mediaList: List<MediaEntity>,
    photosViewMode: PhotosViewMode,
    activeFilter: MediaFilter,
    onFilterChanged: (MediaFilter) -> Unit,
    isSelectionMode: Boolean,
    selectedIds: Set<String>,
    onItemClick: (MediaEntity) -> Unit,
    onItemLongClick: (MediaEntity) -> Unit,
    isSquareAspect: Boolean,
    gridColumns: Int,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val isLight = !isSystemInDarkTheme()
    val accentColor = if (isLight) Color(0xFF007AFF) else Color(0xFF0A84FF)

    Column(modifier = modifier.fillMaxSize()) {
        // Quick Filters Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                Triple("All", MediaFilter.ALL, Icons.Default.PhotoLibrary),
                Triple("Favorites", MediaFilter.FAVORITES, Icons.Default.Favorite),
                Triple("Videos", MediaFilter.VIDEOS, Icons.Default.Videocam),
                Triple("Panoramas", MediaFilter.PANORAMAS, Icons.Default.Panorama),
                Triple("Screenshots", MediaFilter.SCREENSHOTS, Icons.Default.PhoneAndroid)
            )

            items(filters) { (title, filter, icon) ->
                val isSelected = activeFilter == filter
                LiquidButton(
                    onClick = { onFilterChanged(filter) }
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) accentColor else if (isLight) Color.DarkGray else Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) accentColor else if (isLight) Color.Black else Color.White
                    )
                }
            }
        }

        if (mediaList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "No Photos or Videos",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLight) Color.Black else Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Photos and videos on this device will show up here",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        } else if (photosViewMode == PhotosViewMode.YEARS || photosViewMode == PhotosViewMode.MONTHS) {
            // Grouped Editorial View
            EditorialGalleryView(
                mediaList = mediaList,
                viewMode = photosViewMode,
                onItemClick = onItemClick,
                backdrop = backdrop
            )
        } else {
            // Continuous Grid View (ALL or DAYS)
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 4.dp,
                    end = 4.dp,
                    top = 4.dp,
                    bottom = 96.dp // Bottom clearance for floating tabs
                ),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Optional header summary
                item(span = { GridItemSpan(gridColumns) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${mediaList.size} Items",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }
                }

                items(mediaList, key = { it.id }) { item ->
                    MediaItemCard(
                        item = item,
                        isSelected = selectedIds.contains(item.id),
                        isSelectionMode = isSelectionMode,
                        isSquareAspect = isSquareAspect,
                        onClick = { onItemClick(item) },
                        onLongClick = { onItemLongClick(item) },
                        modifier = Modifier.fillMaxWidth().animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorialGalleryView(
    mediaList: List<MediaEntity>,
    viewMode: PhotosViewMode,
    onItemClick: (MediaEntity) -> Unit,
    backdrop: Backdrop
) {
    val isLight = !isSystemInDarkTheme()
    val context = LocalContext.current

    // Group by the real capture date (list is already newest-first, so groups stay newest-first).
    val grouped = remember(mediaList, viewMode) {
        val format = SimpleDateFormat(
            if (viewMode == PhotosViewMode.YEARS) "yyyy" else "MMMM yyyy",
            Locale.getDefault()
        )
        mediaList.groupBy { format.format(Date(it.dateAdded)) }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        grouped.forEach { (period, items) ->
            item(key = period) {
                Column {
                    Text(
                        text = period,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLight) Color.Black else Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Hero feature item
                    val hero = items.firstOrNull()
                    if (hero != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF2C2C2E))
                                .clickable { onItemClick(hero) }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(hero.uri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = hero.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (hero.isOnSdCard) {
                                SdCardBadge(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp),
                                    badgeSize = 24.dp
                                )
                            }

                            // Title badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), Capsule())
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = hero.album,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Mini thumbnails row
                    if (items.size > 1) {
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(items.drop(1)) { subItem ->
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.DarkGray)
                                        .clickable { onItemClick(subItem) }
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(subItem.uri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = subItem.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (subItem.isOnSdCard) {
                                        SdCardBadge(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(4.dp),
                                            badgeSize = 16.dp
                                        )
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
