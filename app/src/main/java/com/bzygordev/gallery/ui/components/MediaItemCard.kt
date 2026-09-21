package com.bzygordev.gallery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bzygordev.gallery.data.MediaEntity
import com.bzygordev.gallery.data.MediaType

@Composable
fun MediaItemCard(
    item: MediaEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isSquareAspect: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(item.uri) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .crossfade(true)
            .build()
    }

    val hasSize = item.width > 0 && item.height > 0

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (isSquareAspect) 4.dp else 12.dp))
            .then(
                if (isSquareAspect || !hasSize) Modifier.aspectRatio(1f)
                else Modifier.aspectRatio(item.width.toFloat() / item.height.toFloat())
            )
            .background(Color(0xFF1C1C1E))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle gradient overlay for readability of badges
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.7f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.45f)
                    )
                )
        )

        // Media Type Badge (Video duration or Panorama)
        if (item.mediaType == MediaType.VIDEO.name && item.durationSeconds != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                val mins = item.durationSeconds / 60
                val secs = item.durationSeconds % 60
                Text(
                    text = String.format("%d:%02d", mins, secs),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Favorite Heart Badge
        if (item.isFavorite) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorite",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // SD card marker
        if (item.isOnSdCard) {
            SdCardBadge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
            )
        }

        // Selection Mode Checkmark
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = fadeIn() + scaleIn(initialScale = 0.5f),
            exit = fadeOut() + scaleOut(targetScale = 0.5f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color(0xFF007AFF)
                        else Color.Black.copy(alpha = 0.4f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
