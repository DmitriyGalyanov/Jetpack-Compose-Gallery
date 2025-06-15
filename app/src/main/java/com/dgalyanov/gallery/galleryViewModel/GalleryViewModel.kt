package com.dgalyanov.gallery.galleryViewModel

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.video.OutputResults
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

internal class GalleryViewModel(context: Context) : ViewModel() {
  companion object {
    val LocalGalleryViewModel =
      staticCompositionLocalOf<GalleryViewModel> { error("CompositionLocal of GalleryViewModel not present") }

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

  private var density by mutableFloatStateOf(1F)

  private var windowWidthPx by mutableIntStateOf(0)
  val windowWidthDp by derivedStateOf { windowWidthPx / density }

  private var windowHeightPx by mutableIntStateOf(0)
  val windowHeightDp by derivedStateOf { windowHeightPx / density }

  fun updateWindowMetrics(density: Float, width: Int, height: Int) {
    log("updateWindowMetrics(density: $density, width: $width, height: $height)")

    this.density = density
    this.windowWidthPx = width
    this.windowHeightPx = height
  }
  /** Layout Data -- END */

  /** Albums -- START */
  var albumsList by mutableStateOf(listOf<GalleryMediaAlbum>())

  var isFetchingAlbums by mutableStateOf(false)
    private set

  fun refreshAlbumsList() {
    val logTag = "refreshAlbumsList()"
    log("$logTag | currentAlbumsListSize: ${albumsList.size}")

    isFetchingAlbums = true
    viewModelScope.launch(Dispatchers.IO) {
      albumsList = GalleryContentResolver.getMediaAlbums()
    }.invokeOnCompletion {
      isFetchingAlbums = false
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

//  val allMediaItemsList = MutableStateFlow(mapOf<Long, GalleryMediaItem>())
  /**
   * todo: think if should keep allMediaItemsList and filter by selected album
   */
  var selectedAlbumMediaItemsMap by mutableStateOf(mapOf<Long, GalleryMediaItem>())
    private set
//  private fun getAlbumMediaItemsMap(album: GalleryMediaAlbum): Map<Long, GalleryMediaItem> {
//    if (album == GalleryMediaAlbum.RecentsAlbum) {
//      return allMediaItemsList.value
//    }
//
//    val selectedAlbumMediaItems = mutableMapOf<Long, GalleryMediaItem>()
//    allMediaItemsList.value.values.forEach {
//      if (it.bucketId == selectedAlbum.id) selectedAlbumMediaItems[it.id] = it
//    }
//    return selectedAlbumMediaItems
//  }

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
      selectedAlbumMediaItemsMap = GalleryContentResolver.getAlbumMediaItems(selectedAlbum.id)
//      selectedAlbumMediaItemsMap.value = getAlbumMediaItemsMap(selectedAlbum)

      if (selectedItemsIds.isEmpty()) {
        selectItem(selectedAlbumMediaItemsMap.values.first())
      } else if (!isMultiselectEnabled.value) {
        val selectedItem = selectedAlbumMediaItemsMap[selectedItemsIds.first()]
        if (selectedItem != null) selectItem(selectedItem)
        else selectItem(selectedAlbumMediaItemsMap.values.first())
      } else {
        selectedItemsIds.toList().forEachIndexed { index, id ->
          selectedAlbumMediaItemsMap[id]?.setSelectionIndex(index)
        }
      }

      if (selectedItemsIds.isEmpty()) {
        selectItem(selectedAlbumMediaItemsMap.values.first())
      }
    }.invokeOnCompletion {
      isFetchingSelectedAlbumMediaFiles = false
      log("$logTag finished")
    }
  }
  /** Albums -- END */

  /** Items Selection -- START */
  /**
   * required since [selectedAlbumMediaItemsMap] changes should not affect Selection (if not specified explicitly)
   */
  private val selectedItemsIds = mutableListOf<Long>()

  private fun fixItemsSelection() {
    log("fixItemsSelection() | selectedItemsIds: $selectedItemsIds")
    selectedItemsIds.forEachIndexed { index, id ->
      selectedAlbumMediaItemsMap[id]?.setSelectionIndex(index)
    }
  }

  private fun clearSelectedItems(itemToRemain: GalleryMediaItem? = null) {
    val logTag =
      "clearSelectedItems(itemToRemain: $itemToRemain) | amountOnStart: ${selectedItemsIds.size}"
    log(logTag)

    selectedItemsIds.forEach {
      log("$logTag | iterating with $it")
      if (it != itemToRemain?.id) selectedAlbumMediaItemsMap[it]?.deselect()
    }
    selectedItemsIds.retainAll(listOf(itemToRemain?.id).toSet())

    fixItemsSelection()

    log("$logTag | amountOnEnd: ${selectedItemsIds.size}")
  }

  private val _isMultiselectEnabled = MutableStateFlow(false)
  val isMultiselectEnabled = _isMultiselectEnabled.asStateFlow()

  fun toggleIsMultiselectEnabled() {
    log("toggleIsMultiselectEnabled() | current: ${_isMultiselectEnabled.value}")

    if (_isMultiselectEnabled.value) clearSelectedItems(previewedItem)

    _isMultiselectEnabled.value = !_isMultiselectEnabled.value
  }

  var previewedItem by mutableStateOf<GalleryMediaItem?>(null)
    private set

  private fun selectItem(item: GalleryMediaItem) {
    log("selectItem(item: $item)")
    if (item.isSelected.value) return

    if (!_isMultiselectEnabled.value) clearSelectedItems()

    item.setSelectionIndex(selectedItemsIds.size)
    selectedItemsIds += item.id
    previewedItem = item

    fixItemsSelection()
  }

  private fun deselectItem(item: GalleryMediaItem) {
    log("deselectItem(item: $item)")
    if (!item.isSelected.value) return

    item.deselect()
    selectedItemsIds.remove(item.id)

    if (previewedItem == item) {
      val newPreviewedItem =
        if (selectedItemsIds.isEmpty()) null
        else selectedAlbumMediaItemsMap[selectedItemsIds.last()]
      previewedItem = newPreviewedItem
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

  val exoPlayerHolder = GalleryExoPlayerController(context)

  /** Selection Emission -- START */
  // todo: come up with a better name
  private var onEmitSelection: ((mediaItems: List<GalleryMediaItem>) -> Unit)? = null
  fun setOnEmitSelection(value: (mediaItems: List<GalleryMediaItem>) -> Unit) {
    log("setOnEmitSelection")
    onEmitSelection = value
  }

  fun emitCurrentlySelected() {
    val selectedItems = mutableListOf<GalleryMediaItem>()

    selectedItemsIds.forEach { selectedItemId ->
      val item = selectedAlbumMediaItemsMap[selectedItemId]
      // todo: think if should store all selected items
        ?: GalleryContentResolver.getGalleryMediaItemById(selectedItemId)
      if (item != null) selectedItems += item
    }

    log("emitCurrentlySelected() | selectedItems: $selectedItems")
    onEmitSelection?.let { it(selectedItems) }
  }

  fun emitCapturedImage(capturedImageFile: ImageCapture.OutputFileResults) {
    val logTag = "emitCapturedImage(capturedImageFile: $capturedImageFile)"
    log(logTag)

    val uri =
      capturedImageFile.savedUri ?: return log("$logTag | capturedImageFile has no savedUri")
    val mediaItem = GalleryContentResolver.getGalleryMediaItemByUri(uri)
      ?: return log("$logTag | couldn't get mediaItem")

    onEmitSelection?.let { it(listOf(mediaItem)) }
  }

  fun emitRecordedVideo(recordedVideoOutputResults: OutputResults) {
    val logTag = "emitRecordedVideo(recordedVideoOutputResults: $recordedVideoOutputResults)"
    log(logTag)

    val mediaItem =
      GalleryContentResolver.getGalleryMediaItemByUri(recordedVideoOutputResults.outputUri)
        ?: return log("$logTag | couldn't get mediaItem")

    onEmitSelection?.let { it(listOf(mediaItem)) }
  }
  /** Selection Emission -- END */
}