package com.dgalyanov.gallery.dataClasses

import androidx.compose.ui.unit.dp

internal class AssetSizeDp(widthPx: Double, heightPx: Double, density: Float) {
  val width = (widthPx / density).dp
  val height = (heightPx / density).dp

  override fun toString(): String {
    return "AssetSizeDp(width: $width, height: $height)"
  }
}

internal data class AssetSize(val width: Double, val height: Double) {
  fun toDp(density: Float) = AssetSizeDp(widthPx = width, heightPx = height, density = density)

  operator fun times(multiplier: Float) =
    AssetSize(width = this.width * multiplier, height = this.height * multiplier)
}
