package com.dgalyanov.gallery.dataClasses

import androidx.compose.ui.geometry.Offset
import com.dgalyanov.gallery.utils.GalleryLogFactory
import kotlin.math.floor

internal class CropData private constructor(val offset: Offset, val finalSize: AssetSize) {
  companion object {
    val log = GalleryLogFactory("CropData.companion")

    private fun getOffset(
      asset: Asset,
      transformations: Transformations,
      assetToWrapAspectRatio: Double,
    ): Offset {
      val translateRelatedOffsetX =
        -(transformations.offset.x * assetToWrapAspectRatio) / transformations.scale
      val scaleRelatedOffsetX = (asset.width - asset.width / transformations.scale) / 2

      val translateRelatedOffsetY =
        -(transformations.offset.y * assetToWrapAspectRatio) / transformations.scale
      val scaleRelatedOffsetY = (asset.height - asset.height / transformations.scale) / 2

      return Offset(
        x = floor(translateRelatedOffsetX + scaleRelatedOffsetX).toFloat(),
        y = floor(translateRelatedOffsetY + scaleRelatedOffsetY).toFloat(),
      )
    }

    private fun getFinalSize(
      transformations: Transformations,
      cropContainerSize: AssetSize,
      assetToWrapAspectRatio: Double,
    ): AssetSize {
      val finalWidth =
        floor(cropContainerSize.width * assetToWrapAspectRatio / transformations.scale)
      val finalHeight =
       floor(cropContainerSize.height * assetToWrapAspectRatio / transformations.scale)

      return AssetSize(width = finalWidth, height = finalHeight)
    }

    fun create(
      asset: Asset,
      transformations: Transformations,
      wrapSize: AssetSize,
      cropContainerSize: AssetSize
    ): CropData {
      val assetToWrapAspectRatio = if (asset.isVertical) asset.height / wrapSize.height
      else asset.width / wrapSize.width

      val finalSize =
        getFinalSize(
          transformations = transformations,
          cropContainerSize = cropContainerSize,
          assetToWrapAspectRatio = assetToWrapAspectRatio
        )

      val offset = getOffset(
        asset = asset,
        transformations = transformations,
        assetToWrapAspectRatio = assetToWrapAspectRatio
      )

      return CropData(offset = offset, finalSize = finalSize)
    }
  }

  override fun toString() = "CropData(offset: $offset, finalSize: $finalSize)"
}
