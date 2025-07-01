package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.assetThumbnailView

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.dataClasses.GalleryAsset
import com.dgalyanov.gallery.utils.modifiers.conditional

@Composable
internal fun AssetThumbnailSelectionIndicator(selectionIndex: Int) {
  val isMultiselectEnabled =
    GalleryViewModel.LocalGalleryViewModel.current.isMultiselectEnabled

  val isSelected = selectionIndex != GalleryAsset.NOT_SELECTED_INDEX

  Box(
    Modifier
      .fillMaxSize()
      .conditional(isSelected) { background(Color(255, 255, 255, 120)) }
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .offset((-8).dp, 8.dp)
        .clip(CircleShape)
        .conditional(isMultiselectEnabled) { border(2.dp, Color.White, CircleShape) }
        .size(18.dp)
        .conditional(isSelected) { background(Color.White) }
    ) {
      if (isSelected) {
        Text(
          text = if (isMultiselectEnabled) (selectionIndex + 1).toString() else "✓",
          modifier = Modifier.offset(y = (-2.5).dp),
          fontSize = 10.sp,
          color = Color.Black,
        )
      }
    }
  }
}

@Preview
@Composable
private fun MultiselectIndicatorPreview() {
  val selectionIndex = 10

  val context = LocalContext.current
  val galleryViewModel = remember {
    val gvm = GalleryViewModel(context)
    gvm.toggleIsMultiselectEnabled()
    return@remember gvm
  }

  CompositionLocalProvider(GalleryViewModel.LocalGalleryViewModel provides galleryViewModel) {
    AssetThumbnailSelectionIndicator(selectionIndex)
    Box(
      Modifier
        .size(150.dp)
        .background(Color.Magenta)
    ) {
      AssetThumbnailSelectionIndicator(selectionIndex)
    }
  }
}