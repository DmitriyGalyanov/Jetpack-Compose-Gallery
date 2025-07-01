package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.previewedAssetView

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.dgalyanov.gallery.dataClasses.AssetSize
import com.dgalyanov.gallery.dataClasses.GalleryAssetType
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.previewedAssetView.previewedAssetMediaView.PreviewedAssetMediaView
import com.dgalyanov.gallery.utils.galleryGenericLog
import kotlin.math.floor

@Composable
internal fun PreviewedAssetView(modifier: Modifier) {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current
  val asset = galleryViewModel.previewedAsset ?: return

  val density = LocalDensity.current

  val wrapSize = AssetSize(
    width = galleryViewModel.previewedAssetContainerWidthPx.toDouble(),
    height = galleryViewModel.previewedAssetContainerHeightPx.toDouble(),
  )
  val wrapSizeDp = wrapSize.toDp(density)

  val assetSize = AssetSize(width = asset.width, height = asset.height)

  // width?
  val assetToWrapWidthRatio = assetSize.height / wrapSize.width

  val isAssetVertical = asset.isVertical

  val scaledToFitWrapAssetSize = run {
    val assetAspectRatio = assetSize.aspectRatio
    val wrapAspectRatio = wrapSize.aspectRatio

    val scaledToWrapAssetWidth = assetSize.width / assetToWrapWidthRatio

    val containerToChildrenAspectRatio = wrapAspectRatio / assetAspectRatio

    var requiredToFitScale = containerToChildrenAspectRatio
    if (assetAspectRatio > 1) {
      if (scaledToWrapAssetWidth < wrapSize.width) {
        requiredToFitScale = wrapSize.width / scaledToWrapAssetWidth
      }
    }

    return@run AssetSize(
      width = if (isAssetVertical) wrapSize.width / requiredToFitScale else wrapSize.width,
      height = if (isAssetVertical) wrapSize.height else wrapSize.height / requiredToFitScale,
    )
  }

  /**
   * asset left/top offset relative to wrap
   * without any transformations applied
   */
  val assetBaseOffset = Offset(
    x = ((wrapSize.width - scaledToFitWrapAssetSize.width) / 2).toFloat(),
    y = ((wrapSize.height - scaledToFitWrapAssetSize.height) / 2).toFloat(),
  )

  val usedAspectRatio = galleryViewModel.usedAspectRatio.heightToWidthNumericValue
  val finalContentContainerSize = AssetSize(
    width = (wrapSize.width * usedAspectRatio).coerceAtMost(wrapSize.width),
    height = (wrapSize.height / usedAspectRatio).coerceAtMost(wrapSize.height),
  )

  /**
   * minimal scale required to make asset fill cropped area (finalContentContainerSize)
   */
  val minScale = run {
    val minScaleBasedOnWidth =
      if (scaledToFitWrapAssetSize.width < finalContentContainerSize.width) {
        finalContentContainerSize.width / scaledToFitWrapAssetSize.width
      } else 1.0

    val basedOnWidthScaledAssetSize = AssetSize(
      width = scaledToFitWrapAssetSize.width * minScaleBasedOnWidth,
      height = scaledToFitWrapAssetSize.height * minScaleBasedOnWidth,
    )
    if (floor(basedOnWidthScaledAssetSize.height) >= floor(finalContentContainerSize.height) && floor(
        basedOnWidthScaledAssetSize.width
      ) >= floor(finalContentContainerSize.width)
    ) return@run minScaleBasedOnWidth;

    val minScaleBasedOnHeight =
      if (scaledToFitWrapAssetSize.height < finalContentContainerSize.height) finalContentContainerSize.height / scaledToFitWrapAssetSize.height
      else 1.0;
    return@run minScaleBasedOnHeight;
  }

  fun handleCropUpdate() {
    // TODO:  
  }

  fun handleAspectRatioSelection() {
    // TODO:
  }

  fun handleFixedAspectRatioChange() {
    // TODO:
  }
  // todo: subscribe handleFixedAspectRatioKeyChange

  Box(
    modifier
      .clipToBounds()
      .size(width = wrapSizeDp.width, height = wrapSizeDp.height),
  ) {
    val nextPreviewedAsset = galleryViewModel.nextPreviewedAsset
    key(asset) {
      val animatedAlpha = remember { Animatable(0.0f) }

      LaunchedEffect(nextPreviewedAsset) {
        if (nextPreviewedAsset != null) {
          animatedAlpha.animateTo(
            if (nextPreviewedAsset.type == GalleryAssetType.Video) 0.0f else 0.3f,
            animationSpec = GalleryViewModel.PREVIEWED_ASSET_SELECTION_RELATED_ANIMATIONS_SPEC,
          )
        } else {
          animatedAlpha.animateTo(
            1f,
            animationSpec = GalleryViewModel.PREVIEWED_ASSET_SELECTION_RELATED_ANIMATIONS_SPEC,
          )
        }
      }

      Box(Modifier.graphicsLayer { alpha = animatedAlpha.value }) {
        GesturesTransformView(
          isEnabled = asset.type == GalleryAssetType.Image,
          initialTransformations = asset.transformations,
          minScale = minScale.toFloat(),
          actualContentSize = scaledToFitWrapAssetSize,
          contentBaseOffset = assetBaseOffset,
          contentContainerSize = finalContentContainerSize,
          onTransformationDidClamp = {
            galleryGenericLog { "PreviewedAssetView | onTransformationDidClamp($it)" }
            asset.transformations = it
          },
        ) {
          PreviewedAssetMediaView(asset = asset, nextAsset = nextPreviewedAsset)
        }
      }
    }

    PreviewedVideoControlsView(
      exoPlayerController = galleryViewModel.exoPlayerController,
      isVisible = asset.type == GalleryAssetType.Video &&
        nextPreviewedAsset?.type != GalleryAssetType.Image
    )

    AspectRatioSelectorView(isVisible = asset.type == GalleryAssetType.Image)
  }
}
