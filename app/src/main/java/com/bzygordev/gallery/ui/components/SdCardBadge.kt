package com.bzygordev.gallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Small marker shown on photos and albums that are stored on the SD card. */
@Composable
fun SdCardBadge(
    modifier: Modifier = Modifier,
    badgeSize: Dp = 18.dp
) {
    Box(
        modifier = modifier
            .size(badgeSize)
            .background(Color.Black.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.SdCard,
            contentDescription = "On SD card",
            tint = Color.White,
            modifier = Modifier.size(badgeSize * 0.66f)
        )
    }
}
