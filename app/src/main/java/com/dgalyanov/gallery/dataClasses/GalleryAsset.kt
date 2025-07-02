package com.dgalyanov.gallery.dataClasses

import android.net.Uri
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import com.dgalyanov.gallery.utils.GalleryLogFactory

typealias GalleryAssetId = Long

internal class GalleryAsset(
  val id: GalleryAssetId,
  val albumId: Long,
  uri: Uri,
  durationMs: Int,
  rawHeight: Double,
  rawWidth: Double,
  orientationDegrees: Int,
) : Asset(
  uri = uri,
  durationMs = durationMs,
  rawHeight = rawHeight,
  rawWidth = rawWidth,
  orientationDegrees = orientationDegrees,
) {
  companion object {
    const val NOT_SELECTED_INDEX = -1
  }

  var transformations: Transformations? = null

  var cropData: CropData? = null

  val closestAspectRatio by lazy { AssetAspectRatio.getClosest(width = width, height = height) }

  val log = GalleryLogFactory(GalleryAsset.toString(), toString())

  private val _selectionIndex = mutableIntStateOf(NOT_SELECTED_INDEX)
  val selectionIndex get() = _selectionIndex.intValue
  fun setSelectionIndex(value: Int) {
    log { "setSelectionIndex(value: $value) | current: ${_selectionIndex.intValue}" }
    _selectionIndex.intValue = value
  }

  val isSelected by derivedStateOf { selectionIndex != NOT_SELECTED_INDEX }

  fun deselect() {
    log { "deselect" }
    setSelectionIndex(NOT_SELECTED_INDEX)
  }

  override fun toString() =
    "GalleryAsset(id: $id, albumId: $albumId, uri: $uri, durationMs: $durationMs, type: $type,\ntransformations: $transformations, cropData: $cropData,\norientationDegrees: $orientationDegrees, rawHeight: $rawHeight, rotatedHeight: $height, rawWidth: $rawWidth, rotatedWidth: $width,\nclosestAspectRatio: $closestAspectRatio,\nactualNumericWidthToHeightRatio: $actualNumericWidthToHeightRatio, isVertical: $isVertical)"
}
