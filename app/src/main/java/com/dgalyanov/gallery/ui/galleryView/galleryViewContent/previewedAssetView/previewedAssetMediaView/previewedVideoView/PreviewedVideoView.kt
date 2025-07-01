package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.previewedAssetView.previewedAssetMediaView.previewedVideoView

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.dgalyanov.gallery.galleryViewModel.ExoPlayerController
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.previewedAssetView.PreviewedVideoControlsView

/**
 * [PreviewedVideoControlsView] are extracted to allow rendering them outside of PlayerSurface which might be transformed
 */
@OptIn(UnstableApi::class)
@Composable
internal fun PreviewedVideoView(
  exoPlayerController: ExoPlayerController,
  aspectRatio: Float,
) {
  exoPlayerController.useSyncPlayWithLifecycle()

  ExoPlayerPlayerSurface(
    player = exoPlayerController.exoPlayer,
    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    aspectRatio = aspectRatio,
  )
}
