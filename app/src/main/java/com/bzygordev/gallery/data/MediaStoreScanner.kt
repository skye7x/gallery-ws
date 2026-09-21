package com.bzygordev.gallery.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** Reads the real photo and video library from the system MediaStore (internal storage and SD card). */
class MediaStoreScanner(private val context: Context) {

    fun scan(favoriteIds: Set<String>): List<MediaEntity> {
        val sdCards = SdCardDetector(context)
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        val items = ArrayList<MediaEntity>()
        query(isVideo = false, sdCards = sdCards, dateFormat = dateFormat, favoriteIds = favoriteIds, out = items)
        query(isVideo = true, sdCards = sdCards, dateFormat = dateFormat, favoriteIds = favoriteIds, out = items)
        items.sortWith(compareByDescending<MediaEntity> { it.dateAdded }.thenBy { it.id })
        return items
    }

    @Suppress("DEPRECATION")
    private fun query(
        isVideo: Boolean,
        sdCards: SdCardDetector,
        dateFormat: SimpleDateFormat,
        favoriteIds: Set<String>,
        out: MutableList<MediaEntity>
    ) {
        val hasVolumeColumn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val collection: Uri =
            if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = ArrayList<String>().apply {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.DATE_ADDED)
            add(MediaStore.MediaColumns.DATE_MODIFIED)
            add(MediaStore.MediaColumns.WIDTH)
            add(MediaStore.MediaColumns.HEIGHT)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.Images.ImageColumns.BUCKET_ID)
            add(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)
            add(MediaStore.Images.ImageColumns.DATE_TAKEN)
            if (isVideo) add(MediaStore.Video.VideoColumns.DURATION)
            else add(MediaStore.Images.ImageColumns.ORIENTATION)
            if (hasVolumeColumn) add(MediaStore.MediaColumns.VOLUME_NAME)
            else add(MediaStore.MediaColumns.DATA)
        }

        try {
            context.contentResolver.query(
                collection,
                projection.toTypedArray(),
                "${MediaStore.MediaColumns.SIZE} > 0",
                null,
                null
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val addedCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val modifiedCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val widthCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
                val heightCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val bucketIdCol = c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_ID)
                val bucketNameCol = c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)
                val takenCol = c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN)
                val durationCol =
                    if (isVideo) c.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION) else -1
                val orientationCol =
                    if (isVideo) -1 else c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.ORIENTATION)
                val volumeCol =
                    if (hasVolumeColumn) c.getColumnIndexOrThrow(MediaStore.MediaColumns.VOLUME_NAME) else -1
                val dataCol =
                    if (hasVolumeColumn) -1 else c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)

                while (c.moveToNext()) {
                    val rawId = c.getLong(idCol)
                    val volume = if (volumeCol >= 0) c.getString(volumeCol) else null
                    val path = if (dataCol >= 0) c.getString(dataCol) else null
                    val onSdCard = sdCards.isOnSdCard(volume, path)

                    val name = c.getString(nameCol) ?: "Untitled"
                    val bucketName = c.getString(bucketNameCol) ?: "Unknown"
                    val bucketId = c.getString(bucketIdCol) ?: "0"

                    var width = c.getInt(widthCol)
                    var height = c.getInt(heightCol)
                    if (orientationCol >= 0) {
                        val rotation = c.getInt(orientationCol)
                        if (rotation == 90 || rotation == 270) {
                            val swap = width
                            width = height
                            height = swap
                        }
                    }

                    val taken = c.getLong(takenCol)
                    val modified = c.getLong(modifiedCol) * 1000L
                    val added = c.getLong(addedCol) * 1000L
                    val dateMillis = when {
                        taken > 0L -> taken
                        modified > 0L -> modified
                        else -> added
                    }

                    val type = when {
                        isVideo -> MediaType.VIDEO
                        bucketName.contains("screenshot", ignoreCase = true) ||
                            name.startsWith("screenshot", ignoreCase = true) -> MediaType.SCREENSHOT
                        name.contains("pano", ignoreCase = true) || isVeryWide(width, height) -> MediaType.PANORAMA
                        else -> MediaType.PHOTO
                    }

                    val id = "${if (isVideo) "video" else "image"}:${volume ?: "external"}:$rawId"
                    val storageTag = volume ?: if (onSdCard) "sd" else "internal"

                    out.add(
                        MediaEntity(
                            id = id,
                            uri = itemUri(isVideo, volume, rawId).toString(),
                            title = name,
                            dateAdded = dateMillis,
                            dateFormatted = dateFormat.format(dateMillis),
                            mediaType = type.name,
                            durationSeconds = if (durationCol >= 0) (c.getLong(durationCol) / 1000L).toInt() else null,
                            isFavorite = id in favoriteIds,
                            width = width,
                            height = height,
                            sizeBytes = c.getLong(sizeCol),
                            albumId = "$storageTag/$bucketId",
                            album = bucketName,
                            isOnSdCard = onSdCard
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            // Media permission was revoked; nothing to list.
        }
    }

    private fun isVeryWide(width: Int, height: Int): Boolean {
        if (width <= 0 || height <= 0) return false
        return max(width, height).toFloat() / min(width, height).toFloat() >= 3f
    }

    private fun itemUri(isVideo: Boolean, volume: String?, id: Long): Uri {
        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && volume != null) {
                if (isVideo) MediaStore.Video.Media.getContentUri(volume)
                else MediaStore.Images.Media.getContentUri(volume)
            } else {
                if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        return ContentUris.withAppendedId(collection, id)
    }
}
