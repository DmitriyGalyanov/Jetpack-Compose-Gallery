package com.dgalyanov.gallery.dataClasses

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

internal class AssetSizeDp(widthPx: Double, heightPx: Double, density: Density) {
  val width: Dp
  val height: Dp

  init {
    with(density) {
      width = widthPx.toFloat().toDp()
      height = heightPx.toFloat().toDp()
    }
  }

  override fun toString(): String {
    return "width: $width, height: $height"
  }
}

internal data class AssetSize(val width: Double, val height: Double) {
  fun toDp(density: Density) = AssetSizeDp(widthPx = width, heightPx = height, density)

  val aspectRatio = height / width
}