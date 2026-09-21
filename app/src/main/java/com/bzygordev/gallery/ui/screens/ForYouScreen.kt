package com.bzygordev.gallery.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.bzygordev.gallery.data.isVideo
import com.bzygordev.gallery.ui.components.SdCardBadge
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.shapes.Capsule
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ForYouScreen(
    mediaList: List<MediaEntity>,
    onItemClick: (MediaEntity) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val isLight = !isSystemInDarkTheme()
    val context = LocalContext.current

    // Memory hero: the newest photo in the library.
    val memoryPhoto = remember(mediaList) { mediaList.firstOrNull { !it.isVideo } ?: mediaList.firstOrNull() }
    val memoryMonth = remember(memoryPhoto) {
        memoryPhoto?.let { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(it.dateAdded)) }.orEmpty()
    }

    // "On This Day": a photo taken on today's date in a previous year, with how many years ago.
    val onThisDay = remember(mediaList) {
        val today = Calendar.getInstance()
        val taken = Calendar.getInstance()
        mediaList.firstNotNullOfOrNull { item ->
            taken.timeInMillis = item.dateAdded
            val yearsAgo = today.get(Calendar.YEAR) - taken.get(Calendar.YEAR)
            if (yearsAgo > 0 &&
                taken.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                taken.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
            ) item to yearsAgo else null
        }
    }

    // Ken-burns zoom animation for memory hero
    val infiniteTransition = rememberInfiniteTransition(label = "ken_burns")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Memories Section
        item(key = "memories") {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Memories",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLight) Color.Black else Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Curated for You",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF007AFF)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (memoryPhoto != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.Black)
                            .clickable { onItemClick(memoryPhoto) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(memoryPhoto.uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = memoryPhoto.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(scale)
                        )

                        // Vignette and gradient
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.0f to Color.Black.copy(alpha = 0.2f),
                                        0.5f to Color.Transparent,
                                        1.0f to Color.Black.copy(alpha = 0.8f)
                                    )
                                )
                        )

                        if (memoryPhoto.isOnSdCard) {
                            SdCardBadge(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(14.dp),
                                badgeSize = 28.dp
                            )
                        }

                        // Top sound / memory icon
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Soundtrack",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Bottom Title and Play
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(18.dp)
                        ) {
                            Text(
                                text = memoryMonth,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = memoryPhoto.album,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        // Play Button
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(18.dp)
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.9f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Memory",
                                tint = Color.Black,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }

        // Featured Photos Section
        item(key = "featured") {
            Column {
                Text(
                    text = "Featured Photos",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLight) Color.Black else Color.White
                )
                Spacer(Modifier.height(12.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(mediaList.take(6)) { item ->
                        Box(
                            modifier = Modifier
                                .width(220.dp)
                                .height(300.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF2C2C2E))
                                .clickable { onItemClick(item) }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item.uri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0.6f to Color.Transparent,
                                            1.0f to Color.Black.copy(alpha = 0.75f)
                                        )
                                    )
                            )

                            if (item.isOnSdCard) {
                                SdCardBadge(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp),
                                    badgeSize = 24.dp
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = item.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = item.album,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // "On This Day" throwback (only when there really is a photo from this date in a past year)
        if (onThisDay != null) {
            item(key = "on_this_day") {
                val (throwbackPhoto, yearsAgo) = onThisDay
                Column {
                    Text(
                        text = "On This Day",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLight) Color.Black else Color.White
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isLight) Color(0xFFF2F2F7) else Color(0xFF1C1C1E)
                            )
                            .clickable { onItemClick(throwbackPhoto) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(throwbackPhoto.uri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = throwbackPhoto.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (throwbackPhoto.isOnSdCard) {
                                SdCardBadge(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(4.dp),
                                    badgeSize = 16.dp
                                )
                            }
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (yearsAgo == 1) "1 Year Ago Today" else "$yearsAgo Years Ago Today",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLight) Color.Black else Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${throwbackPhoto.title} • ${throwbackPhoto.album}",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}
