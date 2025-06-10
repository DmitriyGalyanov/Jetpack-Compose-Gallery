package com.dgalyanov.gallery.ui.galleryView.galleryViewToolbar.galleryAlbumsSheetButton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.dgalyanov.gallery.GalleryViewModel
import com.dgalyanov.gallery.utils.galleryGenericLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GalleryAlbumsSheetButton() {
  val sheetState = rememberModalBottomSheetState()
  var isSheetDisplayed by remember { mutableStateOf(false) }

  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current

  Box {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxHeight()
        .clickable {
          galleryViewModel.refreshAlbumsList()

          isSheetDisplayed = true
        }) {
      Text(galleryViewModel.selectedAlbum.name, fontWeight = FontWeight.Bold)

      // todo: add Icon
    }

    if (isSheetDisplayed) {
      GalleryAlbumsSheet(sheetState = sheetState) {
        galleryGenericLog("GalleryAlbumsSheetButton.onDidDismiss")
        isSheetDisplayed = false
      }
    }
  }
}
