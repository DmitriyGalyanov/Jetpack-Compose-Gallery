package com.dgalyanov.gallery.ui.galleryView.galleryViewContent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.GlideSubcomposition
import com.bumptech.glide.integration.compose.placeholder
import com.dgalyanov.gallery.GalleryViewModel

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
internal fun GalleryPreviewedAssetView(modifier: Modifier = Modifier) {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current
  val previewedItem = galleryViewModel.previewedItem.collectAsState().value

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(1f)
      .then(modifier)
  ) {
    previewedItem?.let {
      GlideImage(
        it.uri,
        contentDescription = null,
        contentScale = ContentScale.Fit,

        loading = placeholder { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) },
        modifier = Modifier
          .matchParentSize()
          .background(Color.DarkGray)
      )
    }
  }
}