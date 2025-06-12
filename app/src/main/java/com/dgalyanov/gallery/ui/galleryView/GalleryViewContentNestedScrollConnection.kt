package com.dgalyanov.gallery.ui.galleryView

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal const val AUTO_SCROLL_ANIMATION_DURATION_MS = 500

internal val AUTO_SCROLL_INT_ANIMATION_SPEC = tween<Int>(AUTO_SCROLL_ANIMATION_DURATION_MS)
internal val AUTO_SCROLL_FLOAT_ANIMATION_SPEC = tween<Float>(AUTO_SCROLL_ANIMATION_DURATION_MS)

internal class GalleryViewContentNestedScrollConnection(
  private val previewedAssetHeight: Int,
  private val scope: CoroutineScope,
) : NestedScrollConnection {
  var previewedAssetOffset by mutableIntStateOf(0)
    private set

  private val animatedPreviewedAssetOffset = Animatable(previewedAssetOffset, Int.VectorConverter)
  private fun animatePreviewedAssetOffset(to: Int) {
    scope.launch {
      animatedPreviewedAssetOffset.snapTo(previewedAssetOffset)
      animatedPreviewedAssetOffset.animateTo(to, animationSpec = AUTO_SCROLL_INT_ANIMATION_SPEC) {
        previewedAssetOffset = this.value
      }
    }
  }

  fun showPreviewedAsset() {
    animatePreviewedAssetOffset(0)
  }

  override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
    val previousPreviewedAssetOffset = previewedAssetOffset

    previewedAssetOffset =
      (previewedAssetOffset + available.y.toInt()).coerceIn(-previewedAssetHeight, 0)

    return Offset(0f, (previewedAssetOffset - previousPreviewedAssetOffset).toFloat())
  }
}