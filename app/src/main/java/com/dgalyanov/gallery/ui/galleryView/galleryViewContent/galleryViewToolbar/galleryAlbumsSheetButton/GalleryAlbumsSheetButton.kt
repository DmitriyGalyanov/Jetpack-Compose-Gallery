package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewToolbar.galleryAlbumsSheetButton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgalyanov.gallery.R
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
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
        }
    ) {
      Text(
        galleryViewModel.selectedAlbum.name,
        fontSize = 16.sp,
        fontWeight = FontWeight.W600,
        letterSpacing = 0.sp,
        modifier = Modifier.padding(end = 4.dp)
      )

      Icon(
        contentDescription = "bold bodyless Arrow pointing down",
        painter = painterResource(R.drawable.bodyless_arrow_down_bold),
        tint = LocalContentColor.current,
        modifier = Modifier.size(14.dp),
      )
    }

    if (isSheetDisplayed) {
      GalleryAlbumsSheet(sheetState = sheetState) {
        galleryGenericLog { "GalleryAlbumsSheetButton.onDidDismiss" }
        isSheetDisplayed = false
      }
    }
  }
}
