package com.dgalyanov.gallery.dataClasses

import android.net.Uri

internal open class Asset(
  val uri: Uri,
  val durationMs: Int,
  protected val rawHeight: Double,
  protected val rawWidth: Double,
  val orientationDegrees: Int,
) {
  val type = if (durationMs > 0) GalleryAssetType.Video else GalleryAssetType.Image

  val height = if (orientationDegrees % 180 == 0) rawHeight else rawWidth
  val width = if (orientationDegrees % 180 == 0) rawWidth else rawHeight

  val actualNumericWidthToHeightRatio = width / height
  val isVertical = actualNumericWidthToHeightRatio < 1
}