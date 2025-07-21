package com.dgalyanov.gallery.ui.galleryView.galleryViewContent

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import com.dgalyanov.gallery.utils.GalleryLogFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal const val AUTO_SCROLL_ANIMATION_DURATION_MS = 350

internal val AUTO_SCROLL_INT_ANIMATION_SPEC = tween<Int>(AUTO_SCROLL_ANIMATION_DURATION_MS)
internal val AUTO_SCROLL_FLOAT_ANIMATION_SPEC = tween<Float>(AUTO_SCROLL_ANIMATION_DURATION_MS)

internal class GalleryViewContentNestedScrollConnection(
  // todo: check if is slower than conditional nestedScroll Modifier
  private val getIsEnabled: () -> Boolean,
  private val previewedAssetContainerHeightPx: Int,
  private val scope: CoroutineScope,
  private val gridState: LazyGridState,
  private val gridFlingDecayAnimationSpec: DecayAnimationSpec<Float>,
  private val gridNonThumbnailsItemsAmount: Int,
  private val gridColumnsAmount: Int,
  private val gridItemHeightPx: Int,
  /**
   * invocation might be skipped if Preview is fully hidden via Scroll Gesture (before Fling)
   */
  private val onPreviewedAssetWillHide: (() -> Unit)? = null,
  /**
   * invocation might be skipped if Preview is fully shown via Scroll Gesture (before Fling)
   */
  private val onPreviewedAssetWillShow: (() -> Unit)? = null,
  private val onPreviewedAssetDidHide: (() -> Unit)? = null,
  private val onPreviewedAssetDidShow: (() -> Unit)? = null,
) : NestedScrollConnection {
  private val log = GalleryLogFactory("GalleryViewContentNestedScrollConnection")

  var previewedAssetOffset by mutableIntStateOf(0)
    private set

  private val animatedPreviewedAssetOffset = Animatable(previewedAssetOffset, Int.VectorConverter)
  private fun animatePreviewedAssetOffset(to: Int, afterAnimation: () -> Unit) {
    scope.launch {
      animatedPreviewedAssetOffset.snapTo(previewedAssetOffset)
      animatedPreviewedAssetOffset.animateTo(to, animationSpec = AUTO_SCROLL_INT_ANIMATION_SPEC) {
        previewedAssetOffset = this.value
      }
      afterAnimation()
    }
  }

  /** hidden, but not docked */
  private val isPreviewedAssetHidden by derivedStateOf { previewedAssetOffset == -previewedAssetContainerHeightPx }
  private var isPreviewedAssetOffsetUnlockedByCurrentFling = false

  private var isPreviewedAssetLockedAsHidden by mutableStateOf(isPreviewedAssetHidden)

  fun showPreviewedAsset() {
    log { "showPreviewedAsset()" }
    onPreviewedAssetWillShow?.invoke()
    animatePreviewedAssetOffset(0) {
      isPreviewedAssetLockedAsHidden = false
      onPreviewedAssetDidShow?.invoke()
    }
  }

  fun hidePreviewedAsset() {
    log { "hidePreviewedAsset()" }
    onPreviewedAssetWillHide?.invoke()
    animatePreviewedAssetOffset(-previewedAssetContainerHeightPx) {
      isPreviewedAssetLockedAsHidden = true
      onPreviewedAssetDidHide?.invoke()
    }
  }

  private fun requestMarkPreviewedAssetLockedAsHidden() {
    if (isPreviewedAssetOffsetUnlockedByCurrentFling) return
    isPreviewedAssetLockedAsHidden = true
    onPreviewedAssetDidHide?.invoke()
  }

  private fun dockPreviewedAssetToClosestPosition() {
    if (isPreviewedAssetHidden) return requestMarkPreviewedAssetLockedAsHidden()
    if (previewedAssetOffset == 0) {
      onPreviewedAssetDidShow?.invoke()
      return
    }

    if (previewedAssetOffset >= -previewedAssetContainerHeightPx / 2) showPreviewedAsset()
    else hidePreviewedAsset()
  }

  private fun getApproximateNonStickyGridOffset() =
    ((gridState.firstVisibleItemIndex - gridNonThumbnailsItemsAmount) / gridColumnsAmount) *
    gridItemHeightPx +
    gridState.firstVisibleItemScrollOffset

  override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
    if (!getIsEnabled()) return super.onPreScroll(available, source)

    if (!gridState.canScrollBackward) isPreviewedAssetLockedAsHidden = false

    log { "onPreScroll(available.y: ${available.y}, source: $source), approximateNonStickyGridOffset: ${getApproximateNonStickyGridOffset()}" }

    if (available.y > 0 && isPreviewedAssetLockedAsHidden) return super.onPreScroll(
      available,
      source,
    )

    val previousPreviewedAssetOffset = previewedAssetOffset

    previewedAssetOffset =
      (previewedAssetOffset + available.y.toInt()).coerceIn(-previewedAssetContainerHeightPx, 0)

    if (gridState.canScrollBackward && isPreviewedAssetHidden) requestMarkPreviewedAssetLockedAsHidden()

    return Offset(0F, (previewedAssetOffset - previousPreviewedAssetOffset).toFloat())
  }

  override fun onPostScroll(
    consumed: Offset,
    available: Offset,
    source: NestedScrollSource,
  ): Offset {
    if (!getIsEnabled()) return super.onPostScroll(consumed, available, source)

    if (!gridState.canScrollBackward) isPreviewedAssetLockedAsHidden = false
    return super.onPostScroll(consumed, available, source)
  }

  /**
   * required to allow fling-caused-scroll change [previewedAssetOffset],
   * which is desirable, but not crucial,
   * current implementation causes other problems,
   * thus it must stay commented and is a todo
   */
//  override suspend fun onPreFling(available: Velocity): Velocity {
//    if (!getIsEnabled()) return super.onPreFling(available)
//    val approximateNonStickyGridOffset = getApproximateNonStickyGridOffset()
//
//    val projectedVelocityLeftOver =
//      gridFlingDecayAnimationSpec.calculateTargetValue(
//        -approximateNonStickyGridOffset.toFloat(),
//        available.y
//      )
//    val nonAdjustedProjectedVelocityLeftOver =
//      gridFlingDecayAnimationSpec.calculateTargetValue(
//        0F,
//        available.y
//      )
//    galleryGenericLog { "onPreFling(available: $available) gridState.firstVisibleItemIndex: ${gridState.firstVisibleItemIndex}, gridState.firstVisibleItemScrollOffset: ${gridState.firstVisibleItemScrollOffset}, approximateNonStickyGridOffset: $approximateNonStickyGridOffset, projectedVelocityLeftOver: $projectedVelocityLeftOver, nonAdjustedProjectedVelocityLeftOver: $nonAdjustedProjectedVelocityLeftOver, " }
//
//
//    if (!gridState.canScrollBackward || projectedVelocityLeftOver > 0) {
//      isPreviewedAssetDockedToTop = false
//
//      isPreviewedAssetUndockedByCurrentFling = true
//    }
//    return super.onPreFling(available)
//  }

  override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
    if (!getIsEnabled()) return super.onPostFling(consumed, available)

    log { "onPostFling(consumed.y: ${consumed.y}, available.y: ${available.y})" }
    isPreviewedAssetOffsetUnlockedByCurrentFling = false
    dockPreviewedAssetToClosestPosition()
    return super.onPostFling(consumed, available)
  }
}