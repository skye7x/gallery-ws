package com.bzygordev.gallery.data

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface DeleteOutcome {
    /** Everything requested is gone. */
    data object Deleted : DeleteOutcome

    /** The system needs the user to confirm before [remaining] can be deleted. */
    data class NeedsConfirmation(
        val intentSender: IntentSender,
        val remaining: List<MediaEntity>
    ) : DeleteOutcome
}

class GalleryRepository(
    private val database: GalleryDatabase,
    context: Context
) {
    private val appContext = context.applicationContext
    private val mediaDao = database.mediaDao()
    private val scanner = MediaStoreScanner(appContext)
    private val syncLock = Mutex()

    val allMedia: Flow<List<MediaEntity>> = mediaDao.getAllMedia()

    /** Re-reads the device library and updates the local index only if something actually changed. */
    suspend fun refresh() = withContext(Dispatchers.IO) {
        syncLock.withLock {
            val scanned = scanner.scan(mediaDao.getFavoriteIds().toHashSet())
            val existing = mediaDao.getAllOnce()
            if (existing.size != scanned.size || existing.toHashSet() != scanned.toHashSet()) {
                database.withTransaction {
                    mediaDao.clearMedia()
                    mediaDao.insertAll(scanned)
                }
            }
        }
    }

    suspend fun toggleFavorite(id: String, currentFavorite: Boolean) {
        database.withTransaction {
            if (currentFavorite) {
                mediaDao.removeFavorite(id)
            } else {
                mediaDao.addFavorite(FavoriteEntity(id))
            }
            mediaDao.setMediaFavorite(id, !currentFavorite)
        }
    }

    /**
     * Deletes the real files from the device.
     *
     * - Android 11+: one system dialog confirms the whole batch.
     * - Android 10: files not created by this app need a per-file system confirmation.
     * - Android 9 and below: deleted directly (needs the storage permission).
     */
    suspend fun delete(items: List<MediaEntity>): DeleteOutcome = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext DeleteOutcome.Deleted
        val resolver = appContext.contentResolver
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val request = MediaStore.createDeleteRequest(resolver, items.map { Uri.parse(it.uri) })
                DeleteOutcome.NeedsConfirmation(request.intentSender, items)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> deleteOnAndroid10(resolver, items)
            else -> {
                items.forEach { resolver.delete(Uri.parse(it.uri), null, null) }
                DeleteOutcome.Deleted
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deleteOnAndroid10(resolver: ContentResolver, items: List<MediaEntity>): DeleteOutcome {
        items.forEachIndexed { index, item ->
            try {
                resolver.delete(Uri.parse(item.uri), null, null)
            } catch (e: RecoverableSecurityException) {
                return DeleteOutcome.NeedsConfirmation(
                    intentSender = e.userAction.actionIntent.intentSender,
                    remaining = items.drop(index)
                )
            }
        }
        return DeleteOutcome.Deleted
    }
}
