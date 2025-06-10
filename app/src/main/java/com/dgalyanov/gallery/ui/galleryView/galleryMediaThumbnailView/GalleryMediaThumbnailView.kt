package com.dgalyanov.gallery.ui.galleryView.galleryMediaThumbnailView

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.dgalyanov.gallery.GalleryViewModel
import com.dgalyanov.gallery.galleryContentResolver.dataClasses.GalleryMediaItem

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
internal fun GalleryMediaThumbnailView(
  item: GalleryMediaItem,
  size: Dp,
) {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current

  Box(
    modifier = Modifier
      .size(size)
      .clickable { galleryViewModel.onThumbnailClick(item) }) {
    GlideImage(
      item.uri,
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.size(size),
    )

    GalleryMediaThumbnailSelectionIndicator(item.selectionIndex)

    Text(item.durationMs.toString())
  }
}

