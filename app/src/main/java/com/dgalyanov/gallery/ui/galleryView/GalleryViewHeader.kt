package com.dgalyanov.gallery.ui.galleryView

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.ui.styleConsts.GalleryStyleConsts

@Composable
internal fun GalleryViewHeader() {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.Black)
      .zIndex(1F)
      .height(galleryViewModel.innerPaddings.calculateTopPadding() + 40.dp)
      .padding(
        top = galleryViewModel.innerPaddings.calculateTopPadding(),
        start = GalleryStyleConsts.COMMON_HORIZONTAL_PADDING,
        end = GalleryStyleConsts.COMMON_HORIZONTAL_PADDING,
      ),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text("Gallery", fontWeight = FontWeight.Bold)

    Text(
      "Emit selected",
      modifier = Modifier.clickable(onClick = galleryViewModel::emitCurrentlySelected)
    )
  }
}