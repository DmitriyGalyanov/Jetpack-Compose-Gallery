package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.previewedAssetView.previewedAssetMediaView

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.dgalyanov.gallery.dataClasses.GalleryAsset
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.dataClasses.GalleryAssetType
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.previewedAssetView.previewedAssetMediaView.previewedVideoView.PreviewedVideoView

@Composable
internal fun PreviewedAssetMediaView(asset: GalleryAsset?, nextAsset: GalleryAsset?) {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current

  val exoPlayerHolder = galleryViewModel.exoPlayerHolder

  LaunchedEffect(asset, nextAsset) {
    val assetToApply = nextAsset ?: asset

    if (assetToApply?.type == GalleryAssetType.Video) {
      exoPlayerHolder.setMedia(assetToApply.uri)
      if (nextAsset == null) {
        exoPlayerHolder.allowPlay()
        exoPlayerHolder.play()
        // making sure requests from other sources will not be fulfilled
      } else exoPlayerHolder.disallowPlay()
    } else {
      exoPlayerHolder.setMedia(null)
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      exoPlayerHolder.setMedia(null)
    }
  }

  asset?.let {
    if (it.type == GalleryAssetType.Image) {
      PreviewedImageView(it.uri)
    } else {
      PreviewedVideoView(
        exoPlayerController = exoPlayerHolder,
        aspectRatio = it.actualNumericWidthToHeightRatio.toFloat()
      )
    }
  }
}