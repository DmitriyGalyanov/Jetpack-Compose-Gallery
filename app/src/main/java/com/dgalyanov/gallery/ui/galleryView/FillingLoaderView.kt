package com.dgalyanov.gallery.ui.galleryView

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun FillingLoaderView() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .zIndex(10F)
      .background(Color(0, 0, 0, 100))
      .clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() }
      ) { },
    contentAlignment = Alignment.Center
  ) {
    CircularProgressIndicator(modifier = Modifier.size(28.dp))
  }
}