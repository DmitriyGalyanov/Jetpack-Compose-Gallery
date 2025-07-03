package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.assetThumbnailView

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.dgalyanov.gallery.dataClasses.GalleryAsset
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
internal fun AssetThumbnailView(
  asset: GalleryAsset,
  widthDp: Dp,
  heightDp: Dp,
  onClick: () -> Unit,
) {
  Box(
    modifier = Modifier
      .size(width = widthDp, height = heightDp)
      .clickable(onClick = onClick)
  ) {
    GlideImage(
      model = asset.uri,
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .size(width = widthDp, height = heightDp)
        .background(Color.Gray),
    )

    AssetThumbnailSelectionIndicator(asset.selectionIndex)

    if (asset.durationMs > 0) {
      Text(
        asset.durationMs.milliseconds.toString(DurationUnit.SECONDS),
        fontSize = 12.sp,
        lineHeight = (12 * 1.2).sp,
        modifier = Modifier
          .offset((-4).dp, (-4).dp)
          .clip(RoundedCornerShape(2.dp))
          .align(Alignment.BottomEnd)
          .background(Color(0, 0, 0, 120))
          .padding(horizontal = 2.dp)
      )
    }
  }
}

