package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewToolbar.galleryAlbumsSheetButton

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.dgalyanov.gallery.dataClasses.GalleryAssetsAlbum
import com.dgalyanov.gallery.ui.theme.withCoercedFontScaleForNonText

private const val ALBUM_PREVIEW_IMAGE_SIZE = 80

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
internal fun GalleryAlbumPreviewView(album: GalleryAssetsAlbum, onClick: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    album.previewUri?.let {
      GlideImage(
        it,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .size(ALBUM_PREVIEW_IMAGE_SIZE.withCoercedFontScaleForNonText())
      )
    } ?: Box(
      modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(Color.DarkGray)
        .size(ALBUM_PREVIEW_IMAGE_SIZE.withCoercedFontScaleForNonText())
    )

    Column(
      modifier = Modifier.fillMaxHeight(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(album.name.ifEmpty { "Unnamed" }, fontWeight = FontWeight.Medium)
      Text(album.assetsAmount.toString())
    }
  }
}