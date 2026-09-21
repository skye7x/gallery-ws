package com.bzygordev.gallery.ui.components

import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bzygordev.gallery.data.MediaEntity
import com.bzygordev.gallery.data.isVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToLong

/** Camera settings read from the photo's own EXIF data. Any field can be missing. */
private data class ExifDetails(
    val camera: String?,
    val iso: String?,
    val shutter: String?,
    val aperture: String?,
    val focalLength: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoInfoSheet(
    item: MediaEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isLight = !isSystemInDarkTheme()
    val surfaceColor = if (isLight) Color(0xFFF2F2F7) else Color(0xFF1C1C1E)
    val textColor = if (isLight) Color.Black else Color.White
    val dividerColor = if (isLight) Color(0xFFE5E5EA) else Color(0xFF2C2C2E)

    val exif by produceState<ExifDetails?>(null, item.uri) {
        value = if (item.isVideo) null else withContext(Dispatchers.IO) { readExif(context, item.uri) }
    }

    val exifStats = listOfNotNull(
        exif?.iso?.let { it to "Sensitivity" },
        exif?.shutter?.let { it to "Shutter" },
        exif?.aperture?.let { it to "Aperture" },
        exif?.focalLength?.let { it to "Focal length" }
    )
    val fileStats = buildList {
        if (item.width > 0 && item.height > 0) add("${item.width} × ${item.height}" to "Resolution")
        add(Formatter.formatShortFileSize(context, item.sizeBytes) to "Size")
        val duration = item.durationSeconds
        if (item.isVideo && duration != null) {
            add(String.format(Locale.US, "%d:%02d", duration / 60, duration % 60) to "Duration")
        }
    }
    val cameraName = exif?.camera

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isLight) Color.White else Color(0xFF121212)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Information",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Date & file name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(surfaceColor)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = item.dateFormatted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Camera settings (when the file has them) and file details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(surfaceColor)
                    .padding(14.dp)
            ) {
                if (cameraName != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = cameraName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = dividerColor)
                    Spacer(Modifier.height(10.dp))
                }

                if (exifStats.isNotEmpty()) {
                    StatsRow(stats = exifStats, textColor = textColor)
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = dividerColor)
                    Spacer(Modifier.height(10.dp))
                }

                StatsRow(stats = fileStats, textColor = textColor)
            }

            Spacer(Modifier.height(12.dp))

            // Folder and storage location
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(surfaceColor)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (item.isOnSdCard) Icons.Default.SdCard else Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = item.album,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Text(
                        text = if (item.isOnSdCard) "SD card" else "Internal storage",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatsRow(
    stats: List<Pair<String, String>>,
    textColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        stats.forEach { (value, label) ->
            Column {
                Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColor)
                Text(label, fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun readExif(context: Context, uri: String): ExifDetails? {
    return try {
        context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
            val exif = ExifInterface(stream)

            val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()
            val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()
            val camera = when {
                model.isNullOrBlank() -> make?.takeIf { it.isNotBlank() }
                make.isNullOrBlank() || model.startsWith(make, ignoreCase = true) -> model
                else -> "$make $model"
            }

            val iso = (exif.getAttribute("PhotographicSensitivity")
                ?: exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS))
                ?.takeIf { it.isNotBlank() }
                ?.let { "ISO $it" }

            val exposure = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
            val shutter = when {
                exposure <= 0.0 -> null
                exposure < 1.0 -> "1/${(1.0 / exposure).roundToLong()}s"
                else -> String.format(Locale.US, "%.1fs", exposure)
            }

            val fNumber = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
            val aperture = if (fNumber > 0.0) String.format(Locale.US, "ƒ/%.1f", fNumber) else null

            val focal = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
            val focalLength = if (focal > 0.0) String.format(Locale.US, "%.0f mm", focal) else null

            ExifDetails(camera, iso, shutter, aperture, focalLength)
        }
    } catch (e: Exception) {
        // Unreadable or missing metadata: just show the file details.
        null
    }
}
