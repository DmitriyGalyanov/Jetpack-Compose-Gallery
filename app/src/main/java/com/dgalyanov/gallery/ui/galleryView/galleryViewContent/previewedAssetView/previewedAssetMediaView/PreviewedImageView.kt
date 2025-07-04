package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.previewedAssetView.previewedAssetMediaView

import android.net.Uri
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.dgalyanov.gallery.dataClasses.AssetSizeDp

@Composable
internal fun PreviewedImageView(uri: Uri, sizeDp: AssetSizeDp) {
  AsyncImage(
    model = uri,
    contentDescription = null,
    contentScale = ContentScale.Fit,
    modifier = Modifier
      .requiredSize(width = sizeDp.width, height = sizeDp.height),
  )
}