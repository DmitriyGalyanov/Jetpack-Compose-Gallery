package com.dgalyanov.gallery.galleryContentResolver.dataClasses

import android.net.Uri
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import com.dgalyanov.gallery.utils.GalleryLogFactory

data class GalleryMediaItem(
  val id: Long,
  val uri: Uri,
  val durationMs: Int,
) {
  companion object {
    const val NOT_SELECTED_INDEX = -1
  }

  val log = GalleryLogFactory("GalleryMediaItem", toString())

  private val _selectionIndex = mutableIntStateOf(NOT_SELECTED_INDEX)
  val selectionIndex get() = _selectionIndex.intValue
  fun setSelectionIndex(value: Int) {
    log("setSelectionIndex(value: $value) | current: ${_selectionIndex.intValue}")
    _selectionIndex.intValue = value
  }

  val isSelected = derivedStateOf { selectionIndex != NOT_SELECTED_INDEX }

  fun deselect() {
    log("deselect")
    setSelectionIndex(NOT_SELECTED_INDEX)
  }
}
