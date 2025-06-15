package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryPreviewedAssetView

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.dgalyanov.gallery.galleryContentResolver.dataClasses.GalleryMediaItem
import com.dgalyanov.gallery.utils.useDelayedShouldShowLoader

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun GalleryPreviewedImageView(uri: Uri) {
  Box {
    GlideImage(
      uri,
      contentDescription = null,
      contentScale = ContentScale.Fit,

      loading = placeholder {
        if (useDelayedShouldShowLoader(true, 30)) {
          CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
      },
    )
  }
}