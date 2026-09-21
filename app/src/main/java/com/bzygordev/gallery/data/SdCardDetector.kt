package com.bzygordev.gallery.data

import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import java.util.Locale

/**
 * Works out whether a MediaStore item lives on a removable SD card.
 *
 * - Android 10+: MediaStore reports a volume name per item. Internal storage is
 *   "external_primary"; a removable card uses its lower-cased volume UUID.
 * - Android 9 and below: there is no volume column, so the file path is matched
 *   against the mount root of each removable volume.
 */
class SdCardDetector(context: Context) {

    private val volumeNames: Set<String>
    private val roots: List<String>

    init {
        val storage = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

        volumeNames = storage.storageVolumes
            .filter { it.isRemovable && !it.isPrimary }
            .mapNotNull { it.uuid?.lowercase(Locale.ROOT) }
            .toSet()

        roots = context.getExternalFilesDirs(null)
            .filterNotNull()
            .filter { dir -> runCatching { Environment.isExternalStorageRemovable(dir) }.getOrDefault(false) }
            .mapNotNull { dir ->
                dir.absolutePath.substringBefore("/Android/data", "").ifEmpty { null }
            }
    }

    fun isOnSdCard(volumeName: String?, path: String?): Boolean {
        if (volumeName != null && volumeName.lowercase(Locale.ROOT) in volumeNames) return true
        if (path != null && roots.any { path.startsWith("$it/") }) return true
        return false
    }
}
