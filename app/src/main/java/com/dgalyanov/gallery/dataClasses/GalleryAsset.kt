package com.dgalyanov.gallery.dataClasses

import android.net.Uri
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import com.dgalyanov.gallery.utils.GalleryLogFactory

internal data class GalleryAsset(
  val id: Long,
  val uri: Uri,
  val durationMs: Int,
  private val rawHeight: Double,
  private val rawWidth: Double,
  private val orientationDegrees: Int,
) {
  companion object {
    const val NOT_SELECTED_INDEX = -1
  }

  var transformations: Transformations? = null

  val type = if (durationMs > 0) GalleryAssetType.Video else GalleryAssetType.Image

  val height = if (orientationDegrees % 180 == 0) rawHeight else rawWidth
  val width = if (orientationDegrees % 180 == 0) rawWidth else rawHeight

  val closestAspectRatio = AssetAspectRatio.getClosest(width = width, height = height)
  val actualNumericWidthToHeightRatio = width / height
  val isVertical = actualNumericWidthToHeightRatio < 1

  val log = GalleryLogFactory(GalleryAsset.toString(), toString())

  private val _selectionIndex = mutableIntStateOf(NOT_SELECTED_INDEX)
  val selectionIndex get() = _selectionIndex.intValue
  fun setSelectionIndex(value: Int) {
    log { "setSelectionIndex(value: $value) | current: ${_selectionIndex.intValue}" }
    _selectionIndex.intValue = value
  }

  val isSelected = derivedStateOf { selectionIndex != NOT_SELECTED_INDEX }

  fun deselect() {
    log { "deselect" }
    setSelectionIndex(NOT_SELECTED_INDEX)
  }
}
