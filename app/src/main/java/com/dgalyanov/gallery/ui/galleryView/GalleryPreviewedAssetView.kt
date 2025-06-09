package com.dgalyanov.gallery.ui.galleryView

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.dgalyanov.gallery.GalleryViewModel

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun GalleryFullSizeAssetView() {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current
  val previewedItem = galleryViewModel.previewedItem.collectAsState().value

  previewedItem?.let {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
    ) {
      GlideImage(
        it.uri,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.matchParentSize()
      )
    }
  }
}