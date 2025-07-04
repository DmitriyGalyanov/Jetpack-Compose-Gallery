package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.previewedAssetView.previewedAssetMediaView

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.dgalyanov.gallery.dataClasses.AssetSizeDp
import com.dgalyanov.gallery.dataClasses.GalleryAsset
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.dataClasses.GalleryAssetType
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.previewedAssetView.previewedAssetMediaView.previewedVideoView.PreviewedVideoView

@Composable
internal fun FullSizeAssetMediaView(
  asset: GalleryAsset,
  nextAsset: GalleryAsset?,
  sizeDp: AssetSizeDp,
) {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current

  val exoPlayerController = galleryViewModel.exoPlayerController

  LaunchedEffect(asset, nextAsset) {
    val assetToApply = nextAsset ?: asset

    if (assetToApply.type == GalleryAssetType.Video) {
      exoPlayerController.setMedia(assetToApply.uri)
      if (nextAsset == null) {
        exoPlayerController.allowPlay()
        exoPlayerController.play()
        // making sure requests from other sources will not be fulfilled
      } else exoPlayerController.disallowPlay()
    } else {
      exoPlayerController.setMedia(null)
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      exoPlayerController.setMedia(null)
    }
  }

  if (asset.type == GalleryAssetType.Image) {
    PreviewedImageView(asset.uri, sizeDp = sizeDp)
  } else {
    Box(
      Modifier
        .requiredSize(width = sizeDp.width, height = sizeDp.height),
    ) {
      PreviewedVideoView(
        exoPlayerController = exoPlayerController,
        aspectRatio = asset.actualNumericWidthToHeightRatio.toFloat()
      )
    }
  }
}