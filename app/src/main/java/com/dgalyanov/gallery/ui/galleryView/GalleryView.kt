package com.dgalyanov.gallery.ui.galleryView

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import com.dgalyanov.gallery.GalleryViewModel
import com.dgalyanov.gallery.ui.commonViews.FillingLoaderView
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.GalleryViewContent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun GalleryView(
  galleryViewModel: GalleryViewModel,
) {
  val mediaItemsList = galleryViewModel.selectedAlbumMediaItemsMap.values.toList()

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

  Box(
    modifier = Modifier
      .fillMaxSize()
  ) {
    if (shouldShowLoader) FillingLoaderView()

    Column(modifier = Modifier.fillMaxSize()) {
      GalleryViewHeader()

      if (mediaItemsList.isNotEmpty()) GalleryViewContent(mediaItemsList)
      else EmptyGalleryView()
    }
  }
}

