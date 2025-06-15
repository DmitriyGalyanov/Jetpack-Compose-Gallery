package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryPreviewedAssetView

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.galleryContentResolver.dataClasses.GalleryMediaType
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryPreviewedAssetView.exoPlayerPlayerView.ExoPlayerPlayerView

@Composable
internal fun GalleryPreviewedAssetView(modifier: Modifier = Modifier) {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current
  val previewedItem = galleryViewModel.previewedItem

  val exoPlayerHolder = galleryViewModel.exoPlayerHolder

  LaunchedEffect(previewedItem) {
    if (previewedItem?.mediaType == GalleryMediaType.Video) {
      exoPlayerHolder.setMedia(previewedItem.uri)
      exoPlayerHolder.play()
    } else {
      exoPlayerHolder.setMedia(null)
    }
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(1f)
      .then(modifier)
  ) {
    Box(
      Modifier
        .background(Color.DarkGray)
        .fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      previewedItem?.let {
        AnimatedVisibility(
          it.mediaType == GalleryMediaType.Image,
          enter = fadeIn(animationSpec = tween(700)),
          exit = fadeOut(animationSpec = tween(0)),
        ) {
          GalleryPreviewedImageView(it.uri)
        }

        AnimatedVisibility(
          it.mediaType == GalleryMediaType.Video,
          enter = fadeIn(animationSpec = tween(700)),
          exit = fadeOut(animationSpec = tween(0))
        ) {
          ExoPlayerPlayerView(playerController = exoPlayerHolder)
        }
      }
    }
  }
}