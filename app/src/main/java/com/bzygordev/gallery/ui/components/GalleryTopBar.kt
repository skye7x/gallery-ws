package com.bzygordev.gallery.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bzygordev.gallery.ui.PhotosViewMode
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.Capsule

@Composable
fun GalleryTopBar(
    selectedTab: Int,
    photosViewMode: PhotosViewMode,
    onPhotosViewModeChanged: (PhotosViewMode) -> Unit,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onToggleSelectionMode: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    isSquareAspect: Boolean,
    onToggleSquareAspect: () -> Unit,
    gridColumns: Int,
    onCycleColumns: () -> Unit,
    selectedAlbum: String?,
    selectedAlbumOnSdCard: Boolean = false,
    onBackFromAlbum: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val isLight = !isSystemInDarkTheme()
    val accentColor = if (isLight) Color(0xFF007AFF) else Color(0xFF0A84FF)
    var showOptionsMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (selectedAlbum != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackFromAlbum
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = accentColor
                        )
                    }
                    Text(
                        text = selectedAlbum,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLight) Color.Black else Color.White
                    )
                    if (selectedAlbumOnSdCard) {
                        Spacer(Modifier.width(8.dp))
                        SdCardBadge(badgeSize = 22.dp)
                    }
                }
            } else {
                Text(
                    text = when (selectedTab) {
                        0 -> "Library"
                        1 -> "For You"
                        2 -> "Albums"
                        3 -> "Search"
                        else -> "Gallery"
                    },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLight) Color.Black else Color.White
                )
            }

            // Top action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedTab == 0) {
                    if (isSelectionMode) {
                        TextButton(
                            onClick = onToggleSelectionMode
                        ) {
                            Text("Done", color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Select button
                        TextButton(
                            onClick = onToggleSelectionMode
                        ) {
                            Text("Select", color = accentColor, fontWeight = FontWeight.SemiBold)
                        }

                        // More options menu
                        Box {
                            IconButton(onClick = { showOptionsMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "Options",
                                    tint = accentColor
                                )
                            }
                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isSquareAspect) "Original Aspect Ratio" else "Square Grid") },
                                    leadingIcon = { Icon(Icons.Default.GridView, null) },
                                    onClick = {
                                        onToggleSquareAspect()
                                        showOptionsMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Columns: $gridColumns (Tap to change)") },
                                    leadingIcon = { Icon(Icons.Default.GridOn, null) },
                                    onClick = {
                                        onCycleColumns()
                                        showOptionsMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Segmented Control in Photos tab (Years / Months / Days / All Photos)
        if (selectedTab == 0 && selectedAlbum == null && !isSelectionMode) {
            val modes = listOf("Years", "Months", "Days", "All")
            val currentModeIndex = when (photosViewMode) {
                PhotosViewMode.YEARS -> 0
                PhotosViewMode.MONTHS -> 1
                PhotosViewMode.DAYS -> 2
                PhotosViewMode.ALL -> 3
            }

            LiquidSegmentedControl(
                items = modes,
                selectedIndex = currentModeIndex,
                onItemSelected = { index ->
                    onPhotosViewModeChanged(
                        when (index) {
                            0 -> PhotosViewMode.YEARS
                            1 -> PhotosViewMode.MONTHS
                            2 -> PhotosViewMode.DAYS
                            else -> PhotosViewMode.ALL
                        }
                    )
                },
                backdrop = backdrop,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Selection mode banner
        if (isSelectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedCount == 0) "Select Items" else "$selectedCount Selected",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isLight) Color.Gray else Color.LightGray
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onSelectAll) {
                        Text("Select All", fontSize = 13.sp, color = accentColor)
                    }
                    if (selectedCount > 0) {
                        TextButton(onClick = onDeleteSelected) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Delete", fontSize = 13.sp, color = Color.Red)
                        }
                    }
                }
            }
        }
    }
}
