package com.dgalyanov.gallery.ui.galleryView

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dgalyanov.gallery.GalleryViewModel

@Composable
fun GalleryViewToolbar() {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current
  val isMultiselectEnabled = galleryViewModel.isMultiselectEnabled.collectAsState().value

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, Color.LightGray)
      .padding(8.dp)
      .height(40.dp)
  ) {
    Button(
      galleryViewModel::toggleIsMultiselectEnabled,
      modifier = Modifier.align(Alignment.CenterEnd)
    ) {
      Text("Multiselect: ${if (isMultiselectEnabled) "enabled" else "disabled"}")
    }
  }
}
