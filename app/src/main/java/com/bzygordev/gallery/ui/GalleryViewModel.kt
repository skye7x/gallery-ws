package com.bzygordev.gallery.ui

import android.app.Application
import android.content.IntentSender
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bzygordev.gallery.data.DeleteOutcome
import com.bzygordev.gallery.data.GalleryDatabase
import com.bzygordev.gallery.data.GalleryRepository
import com.bzygordev.gallery.data.MediaEntity
import com.bzygordev.gallery.data.MediaType
import com.bzygordev.gallery.data.albumKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PhotosViewMode {
    YEARS,
    MONTHS,
    DAYS,
    ALL
}

enum class MediaFilter {
    ALL,
    FAVORITES,
    VIDEOS,
    SCREENSHOTS,
    PANORAMAS
}

/** Title (and storage) of the album currently opened in the Albums tab. */
data class SelectedAlbumInfo(
    val title: String,
    val isOnSdCard: Boolean
)

data class PhotoAdjustments(
    val exposure: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val warmth: Float = 0f,
    val filterPreset: String = "Original"
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = GalleryDatabase.getDatabase(application)
    private val repository = GalleryRepository(database, application)

    val allMedia: StateFlow<List<MediaEntity>> = repository.allMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var refreshJob: Job? = null

    // Re-scan whenever the system photo library changes (new photo, SD card mounted, ...).
    private val mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refresh()
        }
    }

    // Delete requests that need the user's confirmation in a system dialog.
    private val _deleteRequests = MutableSharedFlow<IntentSender>(extraBufferCapacity = 1)
    val deleteRequests: SharedFlow<IntentSender> = _deleteRequests.asSharedFlow()
    private var deleteTargets: List<MediaEntity> = emptyList()
    private var deleteRemaining: List<MediaEntity> = emptyList()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _photosViewMode = MutableStateFlow(PhotosViewMode.ALL)
    val photosViewMode: StateFlow<PhotosViewMode> = _photosViewMode.asStateFlow()

    private val _mediaFilter = MutableStateFlow(MediaFilter.ALL)
    val mediaFilter: StateFlow<MediaFilter> = _mediaFilter.asStateFlow()

    private val _gridColumns = MutableStateFlow(3)
    val gridColumns: StateFlow<Int> = _gridColumns.asStateFlow()

    private val _isSquareAspect = MutableStateFlow(true)
    val isSquareAspect: StateFlow<Boolean> = _isSquareAspect.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private val _activePhoto = MutableStateFlow<MediaEntity?>(null)
    val activePhoto: StateFlow<MediaEntity?> = _activePhoto.asStateFlow()

    private val _showInfoSheet = MutableStateFlow(false)
    val showInfoSheet: StateFlow<Boolean> = _showInfoSheet.asStateFlow()

    private val _showEditSheet = MutableStateFlow(false)
    val showEditSheet: StateFlow<Boolean> = _showEditSheet.asStateFlow()

    private val _adjustments = MutableStateFlow(PhotoAdjustments())
    val adjustments: StateFlow<PhotoAdjustments> = _adjustments.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedAlbum = MutableStateFlow<String?>(null)
    val selectedAlbum: StateFlow<String?> = _selectedAlbum.asStateFlow()

    val filteredMedia: StateFlow<List<MediaEntity>> = combine(
        allMedia,
        mediaFilter,
        selectedAlbum,
        searchQuery
    ) { media, filter, album, query ->
        media.filter { item ->
            val matchesFilter = when (filter) {
                MediaFilter.ALL -> true
                MediaFilter.FAVORITES -> item.isFavorite
                MediaFilter.VIDEOS -> item.mediaType == MediaType.VIDEO.name
                MediaFilter.SCREENSHOTS -> item.mediaType == MediaType.SCREENSHOT.name
                MediaFilter.PANORAMAS -> item.mediaType == MediaType.PANORAMA.name
            }
            val matchesAlbum = when (album) {
                null, ALBUM_RECENTS -> true
                ALBUM_FAVORITES -> item.isFavorite
                else -> item.albumKey == album
            }
            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.album.contains(query, ignoreCase = true) ||
                    item.dateFormatted.contains(query, ignoreCase = true)
            matchesFilter && matchesAlbum && matchesQuery
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedAlbumInfo: StateFlow<SelectedAlbumInfo?> = combine(allMedia, selectedAlbum) { media, key ->
        when (key) {
            null -> null
            ALBUM_RECENTS -> SelectedAlbumInfo("Recents", false)
            ALBUM_FAVORITES -> SelectedAlbumInfo("Favorites", false)
            else -> {
                val first = media.firstOrNull { it.albumKey == key }
                if (first != null) SelectedAlbumInfo(first.album, first.isOnSdCard) else SelectedAlbumInfo(key, false)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** The device's biggest folders, offered as one-tap searches. */
    val searchSuggestions: StateFlow<List<String>> = allMedia
        .map { media ->
            media.groupingBy { it.album }.eachCount()
                .entries
                .sortedByDescending { it.value }
                .map { it.key }
                .take(10)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        val resolver = application.contentResolver
        resolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mediaObserver)
        resolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, mediaObserver)
        refresh()
    }

    /** Re-reads the device photo library (debounced so bursts of changes cause a single scan). */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            delay(300)
            repository.refresh()
        }
    }

    override fun onCleared() {
        getApplication<Application>().contentResolver.unregisterContentObserver(mediaObserver)
        super.onCleared()
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setPhotosViewMode(mode: PhotosViewMode) {
        _photosViewMode.value = mode
    }

    fun setMediaFilter(filter: MediaFilter) {
        _mediaFilter.value = filter
    }

    fun setGridColumns(cols: Int) {
        _gridColumns.value = cols
    }

    fun toggleSquareAspect() {
        _isSquareAspect.value = !_isSquareAspect.value
    }

    fun setSelectionMode(enabled: Boolean) {
        _isSelectionMode.value = enabled
        if (!enabled) {
            _selectedIds.value = emptySet()
        }
    }

    fun toggleItemSelection(id: String) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedIds.value = current
    }

    fun selectAll() {
        _selectedIds.value = filteredMedia.value.map { it.id }.toSet()
    }

    fun deselectAll() {
        _selectedIds.value = emptySet()
    }

    fun setActivePhoto(photo: MediaEntity?) {
        _activePhoto.value = photo
        _adjustments.value = PhotoAdjustments()
        _showInfoSheet.value = false
        _showEditSheet.value = false
    }

    fun setShowInfoSheet(show: Boolean) {
        _showInfoSheet.value = show
    }

    fun setShowEditSheet(show: Boolean) {
        _showEditSheet.value = show
    }

    fun updateAdjustments(newAdjustments: PhotoAdjustments) {
        _adjustments.value = newAdjustments
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedAlbum(album: String?) {
        _selectedAlbum.value = album
    }

    fun toggleFavorite(photo: MediaEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(photo.id, photo.isFavorite)
            if (_activePhoto.value?.id == photo.id) {
                _activePhoto.value = photo.copy(isFavorite = !photo.isFavorite)
            }
        }
    }

    fun deletePhoto(photo: MediaEntity) {
        requestDelete(listOf(photo))
    }

    fun deleteSelected() {
        val ids = _selectedIds.value
        requestDelete(allMedia.value.filter { it.id in ids })
    }

    private fun requestDelete(items: List<MediaEntity>) {
        if (items.isEmpty()) return
        if (deleteTargets.isEmpty()) deleteTargets = items
        viewModelScope.launch {
            val outcome = try {
                repository.delete(items)
            } catch (e: SecurityException) {
                finishDelete()
                return@launch
            }
            when (outcome) {
                is DeleteOutcome.Deleted -> finishDelete()
                is DeleteOutcome.NeedsConfirmation -> {
                    deleteRemaining = outcome.remaining
                    _deleteRequests.emit(outcome.intentSender)
                }
            }
        }
    }

    /** Called with the result of the system delete confirmation dialog. */
    fun onDeleteResult(confirmed: Boolean) {
        val remaining = deleteRemaining
        deleteRemaining = emptyList()
        if (!confirmed) {
            deleteTargets = emptyList()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: the single dialog covered the whole batch.
            finishDelete()
        } else {
            // Android 10: files are confirmed one at a time; continue with the rest.
            requestDelete(remaining)
        }
    }

    private fun finishDelete() {
        val deletedIds = deleteTargets.map { it.id }.toSet()
        deleteTargets = emptyList()
        val activeId = _activePhoto.value?.id
        if (activeId != null && activeId in deletedIds) {
            _activePhoto.value = null
        }
        setSelectionMode(false)
        refresh()
    }

    companion object {
        const val ALBUM_RECENTS = "recents"
        const val ALBUM_FAVORITES = "favorites"
    }
}
