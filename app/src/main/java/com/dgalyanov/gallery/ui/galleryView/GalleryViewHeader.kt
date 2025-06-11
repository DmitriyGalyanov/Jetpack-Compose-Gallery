package com.dgalyanov.gallery.ui.galleryView

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dgalyanov.gallery.GalleryViewModel

@Composable
internal fun GalleryViewHeader() {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.Black)
      .zIndex(1F)
      .height(galleryViewModel.innerPaddings.calculateTopPadding() + 40.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(Modifier.height(galleryViewModel.innerPaddings.calculateTopPadding()))
    Text("Gallery", fontWeight = FontWeight.Bold)
  }
}