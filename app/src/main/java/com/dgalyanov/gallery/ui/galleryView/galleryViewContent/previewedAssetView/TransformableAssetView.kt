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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.dgalyanov.gallery.dataClasses.AssetAspectRatio
import com.dgalyanov.gallery.dataClasses.AssetSize
import com.dgalyanov.gallery.dataClasses.CropData
import com.dgalyanov.gallery.dataClasses.GalleryAsset
import com.dgalyanov.gallery.dataClasses.GalleryAssetType
import com.dgalyanov.gallery.dataClasses.Transformations
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.previewedAssetView.previewedAssetMediaView.FullSizeAssetMediaView
import com.dgalyanov.gallery.utils.GalleryLogFactory
import com.dgalyanov.gallery.utils.galleryGenericLog
import kotlin.math.floor

val log = GalleryLogFactory("TransformableAssetView")

// todo: come up with a better name
private data class TransformableAssetViewValues(
  val minScale: Float,
  val maxScale: Float,
  val assetSizeScaledToFitWrap: AssetSize,
  val cropContainerSize: AssetSize,
) {
  companion object {
    fun get(
      asset: GalleryAsset,
      wrapSize: AssetSize,
      cropContainerAspectRatio: AssetAspectRatio,
    ): TransformableAssetViewValues {
      val assetSize = AssetSize(width = asset.width, height = asset.height)

      // width?
      val assetToWrapWidthRatio = assetSize.height / wrapSize.width

      val isAssetVertical = asset.isVertical

      val assetSizeScaledToFitWrap = run {
        val wrapAspectRatio = wrapSize.heightToWidthAspectRatio
        val assetAspectRatio = assetSize.heightToWidthAspectRatio

        val scaledToWrapAssetWidth = assetSize.width / assetToWrapWidthRatio

        val wrapToAssetAspectRatio = wrapAspectRatio / assetAspectRatio

        var requiredToFitScale = wrapToAssetAspectRatio
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

      val cropContainerHeightToWidthAspectRatio = cropContainerAspectRatio.heightToWidthNumericValue
      val cropContainerSize = AssetSize(
        width = (wrapSize.width * cropContainerHeightToWidthAspectRatio).coerceAtMost(wrapSize.width),
        height = (wrapSize.height / cropContainerHeightToWidthAspectRatio).coerceAtMost(wrapSize.height),
      )

      val minScaleToFillCropContainer = run {
        val minScaleBasedOnWidth =
          if (assetSizeScaledToFitWrap.width < cropContainerSize.width) {
            cropContainerSize.width / assetSizeScaledToFitWrap.width
          } else 1.0

        val basedOnWidthScaledAssetSize = AssetSize(
          width = assetSizeScaledToFitWrap.width * minScaleBasedOnWidth,
          height = assetSizeScaledToFitWrap.height * minScaleBasedOnWidth,
        )
        if (floor(basedOnWidthScaledAssetSize.height) >= floor(cropContainerSize.height) && floor(
            basedOnWidthScaledAssetSize.width
          ) >= floor(cropContainerSize.width)
        ) return@run minScaleBasedOnWidth

        val minScaleBasedOnHeight =
          if (assetSizeScaledToFitWrap.height < cropContainerSize.height) {
            cropContainerSize.height / assetSizeScaledToFitWrap.height
          } else 1.0
        return@run minScaleBasedOnHeight
      }

      return TransformableAssetViewValues(
        minScale = minScaleToFillCropContainer.toFloat(),
        maxScale = 3f,
        assetSizeScaledToFitWrap = assetSizeScaledToFitWrap,
        cropContainerSize = cropContainerSize,
      )
    }

    @Composable
    fun rememberBy(
      asset: GalleryAsset,
      wrapSize: AssetSize,
      cropContainerAspectRatio: AssetAspectRatio,
    ) = remember(asset, wrapSize, cropContainerAspectRatio) {
      get(
        asset = asset,
        wrapSize = wrapSize,
        cropContainerAspectRatio = cropContainerAspectRatio,
      )
    }
  }
}

/**
 * **CHANGES** [asset]'s [transformations][GalleryAsset.transformations] and [cropData][GalleryAsset.cropData]
 */
internal fun clampAssetTransformationsAndCropData(
  asset: GalleryAsset,
  wrapSize: AssetSize,
  cropContainerAspectRatio: AssetAspectRatio,
) {
  // capturing asset's initial state
  val logTag =
    "clampAssetTransformationsAndCropData(asset[captured]: $asset, wrapSize: $wrapSize, usedAspectRatio: $cropContainerAspectRatio)"

  val transformableAssetViewValues = TransformableAssetViewValues.get(
    asset = asset,
    wrapSize = wrapSize,
    cropContainerAspectRatio = cropContainerAspectRatio,
  )

  val rawTransformations = asset.transformations ?: Transformations.Empty
  val clampedTransformations = Transformations.toClamped(
    rawScale = rawTransformations.scale,
    rawOffset = rawTransformations.offset,
    minScale = transformableAssetViewValues.minScale,
    maxScale = transformableAssetViewValues.maxScale,
    displayedContentSize = transformableAssetViewValues.assetSizeScaledToFitWrap,
    cropContainerSize = transformableAssetViewValues.cropContainerSize,
  )

  asset.transformations = clampedTransformations
  asset.cropData = CropData.create(
    asset = asset,
    transformations = clampedTransformations,
    wrapSize = wrapSize,
    cropContainerSize = transformableAssetViewValues.cropContainerSize,
  )

  galleryGenericLog {
    "$logTag\n finished, clampedTransformations: ${asset.transformations}, clampedCropData: ${asset.cropData}"
  }
}

@Composable
internal fun TransformableAssetView(
  asset: GalleryAsset,
  modifier: Modifier = Modifier,
) {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current

  val wrapSize = galleryViewModel.previewedAssetViewWrapSize
  val wrapSizeDp = wrapSize.toDp(LocalDensity.current.density)

  val transformableAssetViewValues = TransformableAssetViewValues.rememberBy(
    asset = asset,
    wrapSize = wrapSize,
    cropContainerAspectRatio = galleryViewModel.usedAspectRatio,
  )

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
          minScale = transformableAssetViewValues.minScale,
          maxScale = transformableAssetViewValues.maxScale,
          displayedContentSize = transformableAssetViewValues.assetSizeScaledToFitWrap,
          cropContainerSize = transformableAssetViewValues.cropContainerSize,
          onTransformationsDidClamp = { transformations ->
            galleryGenericLog { "PreviewedAssetView | onTransformationDidClamp(transformations: $transformations)" }
            asset.transformations = transformations
            asset.cropData = CropData.create(
              asset = asset,
              transformations = transformations,
              wrapSize = wrapSize,
              cropContainerSize = transformableAssetViewValues.cropContainerSize,
            )
          }) {
          FullSizeAssetMediaView(asset = asset, nextAsset = nextPreviewedAsset)
        }
      }
    }

    PreviewedVideoControlsView(
      exoPlayerController = galleryViewModel.exoPlayerController,
      isVisible = asset.type == GalleryAssetType.Video && nextPreviewedAsset?.type != GalleryAssetType.Image
    )

    AspectRatioSelectorView(isVisible = asset.type == GalleryAssetType.Image)
  }
}
