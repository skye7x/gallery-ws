package com.bzygordev.gallery.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bzygordev.gallery.data.MediaEntity
import com.bzygordev.gallery.data.isVideo
import com.bzygordev.gallery.ui.PhotoAdjustments
import com.bzygordev.gallery.ui.components.PhotoEditSheet
import com.bzygordev.gallery.ui.components.PhotoInfoSheet
import com.bzygordev.gallery.ui.components.SdCardBadge
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.emptyBackdrop
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule

@Composable
fun PhotoDetailViewer(
    photo: MediaEntity,
    adjustments: PhotoAdjustments,
    onAdjustmentsChanged: (PhotoAdjustments) -> Unit,
    showInfoSheet: Boolean,
    onShowInfoSheet: (Boolean) -> Unit,
    showEditSheet: Boolean,
    onShowEditSheet: (Boolean) -> Unit,
    onToggleFavorite: () -> Unit,
    onDeletePhoto: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop = emptyBackdrop()
) {
    val context = LocalContext.current
    var showControls by remember { mutableStateOf(true) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Zoom & Pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale > 1f) {
            val maxOffset = 500f * (scale - 1f)
            offset = Offset(
                x = (offset.x + offsetChange.x).coerceIn(-maxOffset, maxOffset),
                y = (offset.y + offsetChange.y).coerceIn(-maxOffset, maxOffset)
            )
        } else {
            offset = Offset.Zero
        }
    }

    // Dynamic ColorMatrix for live editing adjustments
    val colorMatrix = remember(adjustments) {
        val sat = adjustments.saturation
        val r = 0.213f * (1 - sat)
        val g = 0.715f * (1 - sat)
        val b = 0.072f * (1 - sat)
        val baseValues = floatArrayOf(
            r + sat, g, b, 0f, adjustments.exposure * 60f,
            r, g + sat, b, 0f, adjustments.exposure * 60f,
            r, g, b + sat, 0f, adjustments.exposure * 60f,
            0f, 0f, 0f, 1f, 0f
        )
        val matrix = ColorMatrix(baseValues)
        // Preset overrides
        when (adjustments.filterPreset) {
            "Mono" -> matrix.setToSaturation(0f)
            "Noir" -> matrix.setToSaturation(0f)
            "Vivid" -> matrix.setToSaturation(1.5f)
            "Dramatic" -> matrix.setToSaturation(0.7f)
            else -> {}
        }
        matrix
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Image Canvas with zoom & pan
        Box(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformState)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                            }
                        },
                        onTap = {
                            showControls = !showControls
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photo.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = photo.title,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.colorMatrix(colorMatrix),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )
        }

        // Videos open in the system video player
        if (photo.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable {
                        val playIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(Uri.parse(photo.uri), "video/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try {
                            context.startActivity(playIntent)
                        } catch (e: ActivityNotFoundException) {
                            // No video player installed.
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play video",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Floating Top Liquid Glass Bar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            blur(16f.dp.toPx())
                            lens(12f.dp.toPx(), 16f.dp.toPx())
                        },
                        highlight = { Highlight.Default.copy(alpha = 0.3f) },
                        shadow = { Shadow(radius = 8.dp, color = Color.Black.copy(alpha = 0.1f)) },
                        onDrawSurface = { drawRect(Color(0xFF1C1C1E).copy(alpha = 0.65f)) }
                    )
                    .clip(Capsule())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = photo.dateFormatted,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = photo.album,
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        if (photo.isOnSdCard) {
                            Spacer(Modifier.width(4.dp))
                            SdCardBadge(badgeSize = 14.dp)
                        }
                    }
                }

                IconButton(onClick = { onShowInfoSheet(true) }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Color.White
                    )
                }
            }
        }

        // Floating Bottom Liquid Glass Action Bar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            blur(20f.dp.toPx())
                            lens(16f.dp.toPx(), 20f.dp.toPx())
                        },
                        highlight = { Highlight.Default.copy(alpha = 0.4f) },
                        shadow = { Shadow(radius = 16.dp, color = Color.Black.copy(alpha = 0.15f)) },
                        onDrawSurface = { drawRect(Color(0xFF1C1C1E).copy(alpha = 0.7f)) }
                    )
                    .clip(Capsule())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Share
                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = if (photo.isVideo) "video/*" else "image/*"
                            putExtra(Intent.EXTRA_STREAM, Uri.parse(photo.uri))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share"))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White
                    )
                }

                // Favorite
                IconButton(
                    onClick = onToggleFavorite
                ) {
                    Icon(
                        imageVector = if (photo.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (photo.isFavorite) Color(0xFFFF2D55) else Color.White
                    )
                }

                // Edit
                IconButton(
                    onClick = { onShowEditSheet(true) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.White
                    )
                }

                // Delete
                IconButton(
                    onClick = { showDeleteConfirmation = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF3B30)
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(if (photo.isVideo) "Delete Video?" else "Delete Photo?") },
            text = { Text("This permanently deletes the file from your device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeletePhoto()
                    }
                ) {
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Info Sheet
    if (showInfoSheet) {
        PhotoInfoSheet(
            item = photo,
            onDismiss = { onShowInfoSheet(false) }
        )
    }

    // Edit Sheet
    if (showEditSheet) {
        PhotoEditSheet(
            initialAdjustments = adjustments,
            onAdjustmentsChanged = onAdjustmentsChanged,
            onDismiss = { onShowEditSheet(false) },
            backdrop = backdrop
        )
    }
}
