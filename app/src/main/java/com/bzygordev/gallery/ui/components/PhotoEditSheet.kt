package com.bzygordev.gallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.bzygordev.gallery.ui.PhotoAdjustments
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.shapes.Capsule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditSheet(
    initialAdjustments: PhotoAdjustments,
    onAdjustmentsChanged: (PhotoAdjustments) -> Unit,
    onDismiss: () -> Unit,
    backdrop: Backdrop
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var exposure by remember { mutableFloatStateOf(initialAdjustments.exposure) }
    var contrast by remember { mutableFloatStateOf(initialAdjustments.contrast) }
    var saturation by remember { mutableFloatStateOf(initialAdjustments.saturation) }
    var selectedFilter by remember { mutableStateOf(initialAdjustments.filterPreset) }

    val filters = listOf("Original", "Vivid", "Dramatic", "Mono", "Noir")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141416)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header: Cancel, Title, Done
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    onAdjustmentsChanged(PhotoAdjustments())
                    onDismiss()
                }) {
                    Text("Revert", color = Color.Gray, fontSize = 15.sp)
                }

                Text(
                    text = "Edit Photo",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                TextButton(onClick = {
                    onAdjustmentsChanged(
                        PhotoAdjustments(
                            exposure = exposure,
                            contrast = contrast,
                            saturation = saturation,
                            filterPreset = selectedFilter
                        )
                    )
                    onDismiss()
                }) {
                    Text("Done", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Presets Filter row
            Text("Filter Styles", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(filters) { filterName ->
                    val isSelected = filterName == selectedFilter
                    Box(
                        modifier = Modifier
                            .clip(Capsule())
                            .background(
                                if (isSelected) Color(0xFF007AFF) else Color(0xFF2C2C2E)
                            )
                            .clickable {
                                selectedFilter = filterName
                                onAdjustmentsChanged(
                                    PhotoAdjustments(
                                        exposure = exposure,
                                        contrast = contrast,
                                        saturation = saturation,
                                        filterPreset = filterName
                                    )
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = filterName,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Exposure Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Brightness6, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Exposure", color = Color.White, fontSize = 14.sp)
                }
                Text(String.format("%.1f", exposure), color = Color.Gray, fontSize = 13.sp)
            }
            Slider(
                value = exposure,
                onValueChange = {
                    exposure = it
                    onAdjustmentsChanged(
                        PhotoAdjustments(
                            exposure = it,
                            contrast = contrast,
                            saturation = saturation,
                            filterPreset = selectedFilter
                        )
                    )
                },
                valueRange = -1.0f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF007AFF),
                    inactiveTrackColor = Color(0xFF3A3A3C)
                )
            )

            // Contrast Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Contrast, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Contrast", color = Color.White, fontSize = 14.sp)
                }
                Text(String.format("%.1f", contrast), color = Color.Gray, fontSize = 13.sp)
            }
            Slider(
                value = contrast,
                onValueChange = {
                    contrast = it
                    onAdjustmentsChanged(
                        PhotoAdjustments(
                            exposure = exposure,
                            contrast = it,
                            saturation = saturation,
                            filterPreset = selectedFilter
                        )
                    )
                },
                valueRange = 0.5f..1.5f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF007AFF),
                    inactiveTrackColor = Color(0xFF3A3A3C)
                )
            )

            // Saturation Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ColorLens, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Saturation", color = Color.White, fontSize = 14.sp)
                }
                Text(String.format("%.1f", saturation), color = Color.Gray, fontSize = 13.sp)
            }
            Slider(
                value = saturation,
                onValueChange = {
                    saturation = it
                    onAdjustmentsChanged(
                        PhotoAdjustments(
                            exposure = exposure,
                            contrast = contrast,
                            saturation = it,
                            filterPreset = selectedFilter
                        )
                    )
                },
                valueRange = 0.0f..2.0f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF007AFF),
                    inactiveTrackColor = Color(0xFF3A3A3C)
                )
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}
