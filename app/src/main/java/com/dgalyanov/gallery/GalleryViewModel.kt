package com.dgalyanov.gallery

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dgalyanov.gallery.galleryContentResolver.GalleryContentResolver
import com.dgalyanov.gallery.galleryContentResolver.dataClasses.GalleryMediaAlbum
import com.dgalyanov.gallery.galleryContentResolver.dataClasses.GalleryMediaItem
import com.dgalyanov.gallery.utils.GalleryLogFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class GalleryViewModel : ViewModel() {
  companion object {
    val LocalGalleryViewModel = staticCompositionLocalOf { GalleryViewModel() }

    private const val MULTISELECT_LIMIT = 10
  }

  private val log = GalleryLogFactory("GalleryViewModel")

  /** Layout Data -- START */
  var innerPaddings by mutableStateOf(PaddingValues())
    private set

  fun updateInnerPaddings(newPaddingValues: PaddingValues) {
    log("updateInnerPaddings(newPaddingValues: $newPaddingValues) | current: $innerPaddings")
    if (innerPaddings == newPaddingValues) return
    innerPaddings = newPaddingValues
  }

  var containerWidthPx by mutableIntStateOf(0)
    private set
  private var density by mutableFloatStateOf(1F)
  val containerWidthDp by derivedStateOf { containerWidthPx / density }

  fun updateContainerWidth(width: Int, density: Float) {
    log("updateContainerWidth(width: $width, density: $density) | currentWidth: $containerWidthPx, currentDensity: $density | currentDensityAdjustedContainerWidth: $containerWidthDp")
    if (containerWidthPx == width) return

    this.density = density
    this.containerWidthPx = width
  }
  /** Layout Data -- END */

  /** Albums -- START */
  var albumsList by mutableStateOf(listOf<GalleryMediaAlbum>())

  var isRefreshingAlbums by mutableStateOf(false)
    private set

  fun refreshAlbumsList() {
    val logTag = "refreshAlbumsList()"
    log("$logTag | currentAlbumsListSize: ${albumsList.size}")

    isRefreshingAlbums = true
    viewModelScope.launch(Dispatchers.IO) {
      albumsList = GalleryContentResolver.getMediaAlbums()
    }.invokeOnCompletion {
      isRefreshingAlbums = false
      log("$logTag finished | newAlbumsListSize: ${albumsList.size}")
    }
  }

  var selectedAlbum by mutableStateOf(GalleryMediaAlbum.RecentsAlbum)
    private set

  fun selectAlbum(album: GalleryMediaAlbum) {
    log("selectAlbum(album: $album) | current: $selectedAlbum")
    if (selectedAlbum == album) return
    selectedAlbum = album
    getSelectedAlbumMediaFiles()
  }

  /**
   * todo: think if should cache lists of concrete albums or just re-fetch
   */
  val selectedAlbumMediaItemsMap = MutableStateFlow(mapOf<Long, GalleryMediaItem>())

  var isFetchingSelectedAlbumMediaFiles by mutableStateOf(false)
    private set

  /**
   * multiple requests should not be issued simultaneously
   */
  fun getSelectedAlbumMediaFiles() {
    val logTag = "getSelectedAlbumMediaFiles()"
    log(logTag)

    isFetchingSelectedAlbumMediaFiles = true

    viewModelScope.launch(Dispatchers.IO) {
      selectedAlbumMediaItemsMap.value = GalleryContentResolver.getAlbumMediaItems(selectedAlbum.id)

      if (selectedItemsIds.isEmpty()) {
        selectItem(selectedAlbumMediaItemsMap.value.values.first())
      } else if (!isMultiselectEnabled.value) {
        val selectedItem = selectedAlbumMediaItemsMap.value[selectedItemsIds.first()]
        if (selectedItem != null) selectItem(selectedItem)
        else selectItem(selectedAlbumMediaItemsMap.value.values.first())
      } else {
        selectedItemsIds.toList().forEachIndexed { index, id ->
          selectedAlbumMediaItemsMap.value[id]?.setSelectionIndex(index)
        }
      }

      if (selectedItemsIds.isEmpty()) {
        selectItem(selectedAlbumMediaItemsMap.value.values.first())
      }
    }.invokeOnCompletion {
      isFetchingSelectedAlbumMediaFiles = false
      log("$logTag finished")
    }
  }
  /** Albums -- END */

  /** Items Selection -- START */
  private val selectedItemsIds = mutableListOf<Long>()

  private fun fixItemsSelection() {
    log("fixItemsSelection() | selectedItemsIds: $selectedItemsIds")
    selectedItemsIds.forEachIndexed { index, id ->
      selectedAlbumMediaItemsMap.value[id]?.setSelectionIndex(index)
    }
  }

  private fun clearSelectedItems(itemToRemain: GalleryMediaItem? = null) {
    val logTag =
      "clearSelectedItems(itemToRemain: $itemToRemain) | amountOnStart: ${selectedItemsIds.size}"
    log(logTag)

    selectedItemsIds.forEach {
      log("$logTag | iterating with $it")
      if (it != itemToRemain?.id) selectedAlbumMediaItemsMap.value[it]?.deselect()
    }
    selectedItemsIds.retainAll(listOf(itemToRemain?.id).toSet())

    fixItemsSelection()

    log("$logTag | amountOnEnd: ${selectedItemsIds.size}")
  }

  private val _isMultiselectEnabled = MutableStateFlow(false)
  val isMultiselectEnabled = _isMultiselectEnabled.asStateFlow()

  fun toggleIsMultiselectEnabled() {
    log("toggleIsMultiselectEnabled() | current: ${_isMultiselectEnabled.value}")

    if (_isMultiselectEnabled.value) clearSelectedItems(previewedItem.value)

    _isMultiselectEnabled.value = !_isMultiselectEnabled.value
  }

  private val _previewedItem = MutableStateFlow<GalleryMediaItem?>(null)
  val previewedItem = _previewedItem.asStateFlow()
  private fun setPreviewedItem(value: GalleryMediaItem?) {
    log("setPreviewedItem(item: $value) | current: ${_previewedItem.value}")
    _previewedItem.value = value
  }

  private fun selectItem(item: GalleryMediaItem) {
    log("selectItem(item: $item)")
    if (item.isSelected.value) return

    if (!_isMultiselectEnabled.value) clearSelectedItems()

    item.setSelectionIndex(selectedItemsIds.size)
    selectedItemsIds += item.id
    setPreviewedItem(item)

    fixItemsSelection()
  }

  private fun deselectItem(item: GalleryMediaItem) {
    log("deselectItem(item: $item)")
    if (!item.isSelected.value) return

    item.deselect()
    selectedItemsIds.remove(item.id)

    if (_previewedItem.value == item) {
      val newPreviewedItem =
        if (selectedItemsIds.isEmpty()) null
        else selectedAlbumMediaItemsMap.value[selectedItemsIds.last()]
      setPreviewedItem(newPreviewedItem)
    }

    fixItemsSelection()
  }

  fun onThumbnailClick(item: GalleryMediaItem) {
    log("onThumbnailClick(item: $item)")

    if (_isMultiselectEnabled.value) {
      if (selectedItemsIds.size == MULTISELECT_LIMIT) return

      if (item.isSelected.value) {
        if (selectedItemsIds.size > 1) deselectItem(item)
      } else {
        selectItem(item)
      }
    } else {
      selectItem(item)
    }
  }
  /** Items Selection -- END */
}