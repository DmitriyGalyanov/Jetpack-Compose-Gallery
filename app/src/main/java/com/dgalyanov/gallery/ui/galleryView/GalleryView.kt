package com.dgalyanov.gallery.ui.galleryView

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dgalyanov.gallery.galleryContentResolver.GalleryMediaItem
import com.dgalyanov.gallery.ui.galleryView.galleryMediaThumbnailView.GalleryMediaThumbnailView
import com.dgalyanov.gallery.utils.currentWindowContainerSize

private const val COLUMNS_AMOUNT = 3

@Composable
internal fun GalleryView(
  mediaItemsList: List<GalleryMediaItem>,
) {
  val thumbnailSize = (currentWindowContainerSize().width) / COLUMNS_AMOUNT

  if (mediaItemsList.isNotEmpty()) {
    Column(modifier = Modifier.fillMaxSize()) {
      GalleryFullSizeAssetView()

      GalleryViewToolbar()

      LazyVerticalGrid(
        columns = GridCells.Fixed(COLUMNS_AMOUNT),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(mediaItemsList, key = { it.id }) {
          GalleryMediaThumbnailView(
            it,
            thumbnailSize,
          )
        }
      }
    }
  } else EmptyGalleryView()
}

