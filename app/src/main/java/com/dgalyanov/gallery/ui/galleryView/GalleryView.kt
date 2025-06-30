package com.dgalyanov.gallery.ui.galleryView

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.ui.commonViews.FillingLoaderView
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.GalleryViewContent
import com.dgalyanov.gallery.utils.useDelayedShouldShowLoader

@Composable
internal fun GalleryView(
  galleryViewModel: GalleryViewModel,
) {
  val assets = galleryViewModel.selectedAlbumAssetsMap.values.toList()

  val shouldShowLoader =
    useDelayedShouldShowLoader(galleryViewModel.isFetchingSelectedAlbumMediaFiles)

  Box(
    modifier = Modifier
      .fillMaxSize()
  ) {
    if (shouldShowLoader) FillingLoaderView()

    Column(modifier = Modifier.fillMaxSize()) {
      GalleryViewHeader()

      if (assets.isNotEmpty()) GalleryViewContent(assets)
      else EmptyGalleryView()
    }
  }
}

