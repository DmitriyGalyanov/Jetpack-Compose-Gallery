package com.dgalyanov.gallery.ui.galleryView

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.ui.commonViews.FillingLoaderView
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.GalleryViewContent

@Composable
internal fun GalleryView(
  galleryViewModel: GalleryViewModel,
) {
  val shouldShowLoader =
    galleryViewModel.isFetchingAllAssets || galleryViewModel.isPreparingSelectedAssetsForEmission

  Box(
    modifier = Modifier.fillMaxSize()
  ) {
    FillingLoaderView(shouldShowLoader)

    Column(modifier = Modifier.fillMaxSize()) {
      GalleryViewHeader()

      if (galleryViewModel.selectedAlbumAssetsMap.values.isEmpty()) EmptyGalleryView()
      else GalleryViewContent()
    }
  }
}

