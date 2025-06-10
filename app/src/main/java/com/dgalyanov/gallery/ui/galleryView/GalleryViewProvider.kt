package com.dgalyanov.gallery.ui.galleryView

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import com.dgalyanov.gallery.GalleryViewModel

@Composable
internal fun GalleryViewProvider(galleryViewModel: GalleryViewModel) {
  CompositionLocalProvider(
    GalleryViewModel.LocalGalleryViewModel provides galleryViewModel
  ) {
    GalleryView(galleryViewModel)
  }
}