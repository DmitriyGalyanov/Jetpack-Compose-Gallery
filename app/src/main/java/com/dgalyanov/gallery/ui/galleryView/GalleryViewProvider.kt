package com.dgalyanov.gallery.ui.galleryView

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.dgalyanov.gallery.GalleryViewModel

@Composable
internal fun GalleryViewProvider(galleryViewModel: GalleryViewModel, innerPaddings: PaddingValues) {
  LaunchedEffect(innerPaddings) {
    galleryViewModel.updateInnerPaddings(innerPaddings)
  }

  CompositionLocalProvider(
    GalleryViewModel.LocalGalleryViewModel provides galleryViewModel
  ) {
    GalleryView(galleryViewModel)
  }
}