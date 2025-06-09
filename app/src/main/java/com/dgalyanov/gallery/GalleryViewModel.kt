package com.dgalyanov.gallery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dgalyanov.gallery.galleryContentResolver.GalleryContentResolver
import com.dgalyanov.gallery.galleryContentResolver.GalleryMediaItem
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

  var mediaFilesList by mutableStateOf(listOf<GalleryMediaItem>())
    private set

  fun populateMediaFilesList() {
    viewModelScope.launch(Dispatchers.IO) {
      mediaFilesList = GalleryContentResolver.getMediaFiles()
      log(mediaFilesList.size.toString())
      if (_previewedItem.value == null) selectItem(mediaFilesList.first())
    }
  }

  /** Selection -- START */
  private val selectedItems = mutableListOf<GalleryMediaItem>()

  private fun fixSelectedItemsIndices() {
    log("fixSelectedItemsIndices")
    selectedItems.forEachIndexed { index, item -> item.setSelectionIndex(index) }
  }

  private fun clearSelectedItems(itemToRemain: GalleryMediaItem? = null) {
    val logTag =
      "clearSelectedItems(itemToRemain: $itemToRemain) | amountOnStart: ${selectedItems.size}"
    log(logTag)

    selectedItems.forEach {
      log("$logTag | iterating with $it")
      if (it != itemToRemain) it.deselect()
    }
    selectedItems.retainAll(listOf(itemToRemain).toSet())
    fixSelectedItemsIndices()

    log("$logTag | amountOnEnd: ${selectedItems.size}")
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
    log("setPreviewedItem(item: $value)")
    _previewedItem.value = value
  }

  private fun selectItem(item: GalleryMediaItem) {
    log("selectItem(item: $item)")
    if (item.isSelected.value) return

    if (!_isMultiselectEnabled.value) clearSelectedItems()

    item.setSelectionIndex(selectedItems.size)
    selectedItems += item
    setPreviewedItem(item)

    fixSelectedItemsIndices()
  }

  private fun deselectItem(item: GalleryMediaItem) {
    log("deselectItem(item: $item)")
    if (!item.isSelected.value) return

    item.deselect()
    selectedItems.remove(item)

    if (_previewedItem.value == item) {
      setPreviewedItem(if (selectedItems.isEmpty()) null else selectedItems.last())
    }

    fixSelectedItemsIndices()
  }

  fun onThumbnailClick(item: GalleryMediaItem) {
    log("onThumbnailClick(item: $item)")

    if (_isMultiselectEnabled.value) {
      if (selectedItems.size == MULTISELECT_LIMIT) return

      if (item.isSelected.value) {
        if (selectedItems.size > 1) deselectItem(item)
      } else {
        selectItem(item)
      }
    } else {
      selectItem(item)
    }
  }
  /** Selection -- END */
}