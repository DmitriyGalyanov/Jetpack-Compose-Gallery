package com.dgalyanov.gallery.ui.galleryView

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel

@Composable
internal fun GalleryViewProvider(galleryViewModel: GalleryViewModel) {
  CompositionLocalProvider(
    GalleryViewModel.LocalGalleryViewModel provides galleryViewModel
  ) {
    GalleryView(galleryViewModel)
  }
}