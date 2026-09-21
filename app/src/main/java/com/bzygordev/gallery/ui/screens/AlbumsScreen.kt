package com.bzygordev.gallery.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Panorama
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bzygordev.gallery.data.MediaEntity
import com.bzygordev.gallery.data.albumKey
import com.bzygordev.gallery.data.MediaType
import com.bzygordev.gallery.ui.GalleryViewModel
import com.bzygordev.gallery.ui.components.SdCardBadge
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.shapes.Capsule

data class AlbumItem(
    val key: String,
    val title: String,
    val count: Int,
    val coverUri: String?,
    val isOnSdCard: Boolean = false
)

@Composable
fun AlbumsScreen(
    mediaList: List<MediaEntity>,
    onSelectAlbum: (String) -> Unit,
    onSelectMediaType: (String) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val isLight = !isSystemInDarkTheme()
    val accentColor = if (isLight) Color(0xFF007AFF) else Color(0xFF0A84FF)
    val context = LocalContext.current
    var showNewAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumTitle by remember { mutableStateOf("") }

    // One album per real folder on each storage volume (so "Camera" on the SD card is its own album).
    val albums = remember(mediaList) {
        val list = mutableListOf(
            AlbumItem(
                GalleryViewModel.ALBUM_RECENTS,
                "Recents",
                mediaList.size,
                mediaList.firstOrNull()?.uri
            ),
            AlbumItem(
                GalleryViewModel.ALBUM_FAVORITES,
                "Favorites",
                mediaList.count { it.isFavorite },
                mediaList.firstOrNull { it.isFavorite }?.uri
            )
        )
        mediaList.groupBy { it.albumKey }.forEach { (key, items) ->
            val newest = items.first()
            list.add(AlbumItem(key, newest.album, items.size, newest.uri, newest.isOnSdCard))
        }
        list
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // "My Albums" Section
        item(key = "my_albums") {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Albums",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLight) Color.Black else Color.White
                    )
                    LiquidButton(
                        onClick = { showNewAlbumDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Album",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "New Album",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(albums) { album ->
                        Column(
                            modifier = Modifier
                                .width(150.dp)
                                .clickable { onSelectAlbum(album.key) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isLight) Color(0xFFE5E5EA) else Color(0xFF2C2C2E)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (album.coverUri != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(album.coverUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = album.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                if (album.isOnSdCard) {
                                    SdCardBadge(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp),
                                        badgeSize = 24.dp
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = album.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isLight) Color.Black else Color.White
                            )
                            Text(
                                text = "${album.count}",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // "Media Types" iOS list
        item(key = "media_types") {
            Column {
                Text(
                    text = "Media Types",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLight) Color.Black else Color.White,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                val mediaTypes = remember(mediaList) {
                    listOf(
                        Triple("Videos", mediaList.count { it.mediaType == MediaType.VIDEO.name }, Icons.Default.Videocam),
                        Triple("Panoramas", mediaList.count { it.mediaType == MediaType.PANORAMA.name }, Icons.Default.Panorama),
                        Triple("Screenshots", mediaList.count { it.mediaType == MediaType.SCREENSHOT.name }, Icons.Default.PhoneAndroid)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isLight) Color(0xFFF2F2F7) else Color(0xFF1C1C1E))
                ) {
                    mediaTypes.forEachIndexed { index, (name, count, icon) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectMediaType(name) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                text = name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = if (isLight) Color.Black else Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$count",
                                fontSize = 15.sp,
                                color = Color.Gray
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (index < mediaTypes.size - 1) {
                            HorizontalDivider(
                                color = if (isLight) Color(0xFFE5E5EA) else Color(0xFF2C2C2E),
                                modifier = Modifier.padding(start = 52.dp)
                            )
                        }
                    }
                }
            }
        }

        // "Utilities" iOS list
        item(key = "utilities") {
            Column {
                Text(
                    text = "Utilities",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLight) Color.Black else Color.White,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                val utilities = listOf(
                    Triple("Recently Deleted", 0, Icons.Default.DeleteOutline)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isLight) Color(0xFFF2F2F7) else Color(0xFF1C1C1E))
                ) {
                    utilities.forEachIndexed { index, (name, count, icon) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                text = name,
                                fontSize = 16.sp,
                                color = if (isLight) Color.Black else Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$count",
                                fontSize = 15.sp,
                                color = Color.Gray
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (index < utilities.size - 1) {
                            HorizontalDivider(
                                color = if (isLight) Color(0xFFE5E5EA) else Color(0xFF2C2C2E),
                                modifier = Modifier.padding(start = 52.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // New Album Dialog
    if (showNewAlbumDialog) {
        AlertDialog(
            onDismissRequest = { showNewAlbumDialog = false },
            title = { Text("New Album") },
            text = {
                Column {
                    Text("Enter a name for this album:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newAlbumTitle,
                        onValueChange = { newAlbumTitle = it },
                        placeholder = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newAlbumTitle.isNotBlank()) {
                            onSelectAlbum(newAlbumTitle.trim())
                            showNewAlbumDialog = false
                            newAlbumTitle = ""
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewAlbumDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
