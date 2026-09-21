package com.bzygordev.gallery.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaType {
    PHOTO,
    VIDEO,
    SCREENSHOT,
    PANORAMA
}

/** One photo or video from the device's MediaStore. */
@Entity(tableName = "media_items")
data class MediaEntity(
    @PrimaryKey val id: String,
    val uri: String,
    /** File name as stored on the device. */
    val title: String,
    /** Capture time (date taken), falling back to the file's modified/added time. Epoch millis. */
    val dateAdded: Long,
    val dateFormatted: String,
    val mediaType: String = MediaType.PHOTO.name,
    val durationSeconds: Int? = null,
    val isFavorite: Boolean = false,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0L,
    /** Stable id of the folder (bucket) on a specific storage volume. */
    val albumId: String,
    /** Folder name shown as the album title. */
    val album: String,
    /** True when the file lives on a removable SD card rather than internal storage. */
    val isOnSdCard: Boolean = false
)

val MediaEntity.albumKey: String get() = "album:$albumId"

val MediaEntity.isVideo: Boolean get() = mediaType == MediaType.VIDEO.name

/** Favorites are kept apart from the media index so they survive re-scans and SD card removal. */
@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val id: String)
