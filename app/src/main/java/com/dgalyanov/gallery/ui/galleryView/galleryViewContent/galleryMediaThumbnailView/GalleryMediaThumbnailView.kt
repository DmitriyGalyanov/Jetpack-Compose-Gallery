package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryMediaThumbnailView

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.dgalyanov.gallery.galleryContentResolver.dataClasses.GalleryMediaItem
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
internal fun GalleryMediaThumbnailView(
  item: GalleryMediaItem,
  size: Dp,
  onClick: () -> Unit,
) {
  Box(
    modifier = Modifier
      .size(size)
      .clickable(onClick = onClick)
  ) {
    GlideImage(
      item.uri,
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.size(size),
    )

    GalleryMediaThumbnailSelectionIndicator(item.selectionIndex)

    if (item.durationMs > 0) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(2.dp))
          .align(Alignment.BottomEnd)
          .background(Color(0, 0, 0, 120))
          .padding(horizontal = 2.dp)
      ) {
        Text(item.durationMs.milliseconds.toString(DurationUnit.SECONDS))
      }
    }
  }
}

