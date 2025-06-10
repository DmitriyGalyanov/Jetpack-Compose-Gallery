package com.dgalyanov.gallery.ui.galleryView

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.dgalyanov.gallery.GalleryViewModel
import com.dgalyanov.gallery.ui.galleryView.galleryMediaThumbnailView.GalleryMediaThumbnailView
import com.dgalyanov.gallery.ui.galleryView.galleryViewToolbar.GalleryViewToolbar
import com.dgalyanov.gallery.utils.currentWindowContainerSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val COLUMNS_AMOUNT = 3

@Composable
internal fun GalleryView(
  galleryViewModel: GalleryViewModel,
) {
  val thumbnailSize = (currentWindowContainerSize().width) / COLUMNS_AMOUNT

  val mediaItemsList =
    galleryViewModel.selectedAlbumMediaItemsMap.collectAsState().value.values.toList()

  var shouldShowLoader by remember { mutableStateOf(false) }
  var loaderJob by remember { mutableStateOf<Job?>(null) }
  DisposableEffect(galleryViewModel.isFetchingSelectedAlbumMediaFiles) {
    if (galleryViewModel.isFetchingSelectedAlbumMediaFiles) {
      loaderJob = galleryViewModel.viewModelScope.launch {
        delay(100)
        shouldShowLoader = true
      }
    } else {
      loaderJob?.cancel()
      shouldShowLoader = false
    }
    onDispose { loaderJob?.cancel() }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    if (shouldShowLoader) FillingLoaderView()

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
}

