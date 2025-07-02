package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.previewedAssetView.previewedAssetMediaView

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.dgalyanov.gallery.ui.utils.useDelayedShouldShowLoader

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
internal fun PreviewedImageView(uri: Uri) {
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