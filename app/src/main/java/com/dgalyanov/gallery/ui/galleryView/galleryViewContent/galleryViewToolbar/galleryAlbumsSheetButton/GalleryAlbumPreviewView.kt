package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewToolbar.galleryAlbumsSheetButton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.dgalyanov.gallery.dataClasses.GalleryAssetsAlbum


private val ALBUM_PREVIEW_IMAGE_SIZE = 80.dp

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
internal fun GalleryAlbumPreviewView(album: GalleryAssetsAlbum, onClick: () -> Unit) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    // todo: add placeholder
    album.previewUri?.let {
      GlideImage(
        it,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .size(ALBUM_PREVIEW_IMAGE_SIZE)
      )
    }

    Spacer(Modifier.width(8.dp))

    Column(modifier = Modifier.fillMaxHeight()) {
      Text(album.name, fontWeight = FontWeight.Medium)
      Text(album.assetsAmount.toString())
    }
  }
}