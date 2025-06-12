package com.dgalyanov.gallery.ui.galleryView.galleryViewToolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dgalyanov.gallery.GalleryViewModel
import com.dgalyanov.gallery.ui.galleryView.galleryViewToolbar.galleryAlbumsSheetButton.GalleryAlbumsSheetButton

internal val GALLERY_VIEW_TOOLBAR_HEIGHT = 48.dp

@Composable
internal fun GalleryViewToolbar() {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current
  val isMultiselectEnabled = galleryViewModel.isMultiselectEnabled.collectAsState().value

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.Black)
      .requiredHeight(GALLERY_VIEW_TOOLBAR_HEIGHT)
      .padding(8.dp),
  ) {
    GalleryAlbumsSheetButton()

    Text(
      text = "Multiselect: ${if (isMultiselectEnabled) "enabled" else "disabled"}",
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .clickable(onClick = galleryViewModel::toggleIsMultiselectEnabled)
    )
  }
}
