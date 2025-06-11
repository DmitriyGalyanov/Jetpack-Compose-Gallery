package com.dgalyanov.gallery.ui.galleryView

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dgalyanov.gallery.GalleryViewModel
import com.dgalyanov.gallery.galleryContentResolver.dataClasses.GalleryMediaItem
import com.dgalyanov.gallery.ui.galleryView.galleryMediaThumbnailView.GalleryMediaThumbnailView
import com.dgalyanov.gallery.ui.galleryView.galleryViewToolbar.GalleryViewToolbar

private class GalleryViewContentNestedScrollConnection(
  private val previewedAssetHeight: Int
) : NestedScrollConnection {
  var previewedAssetOffset by mutableIntStateOf(0)
    private set

  override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
    val previousPreviewedAssetOffset = previewedAssetOffset

    previewedAssetOffset =
      (previewedAssetOffset + available.y.toInt()).coerceIn(-previewedAssetHeight, 0)

    return Offset(0f, (previewedAssetOffset - previousPreviewedAssetOffset).toFloat())
  }
}

private const val COLUMNS_AMOUNT = 3

private const val TOOLBAR_ITEM_KEY = "Toolbar"

@Composable
internal fun GalleryViewContent(mediaItemsList: List<GalleryMediaItem>) {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current

  val thumbnailSize = galleryViewModel.containerWidthDp.dp / COLUMNS_AMOUNT

  val previewedAssetHeightDp = galleryViewModel.containerWidthDp.dp
  val previewedAssetHeightPx = with(LocalDensity.current) { previewedAssetHeightDp.roundToPx() }

  val nestedScrollConnection =
    remember(previewedAssetHeightPx) {
      GalleryViewContentNestedScrollConnection(
        previewedAssetHeightPx
      )
    }

  Box(Modifier.nestedScroll(nestedScrollConnection)) {
    GalleryPreviewedAssetView(Modifier.offset {
      IntOffset(
        0,
        nestedScrollConnection.previewedAssetOffset,
      )
    })

    Column {
      InnerSpacer(
        previewedAssetHeightPx = previewedAssetHeightPx,
        nestedScrollConnection = nestedScrollConnection
      )

      LazyVerticalGrid(
        columns = GridCells.Fixed(COLUMNS_AMOUNT),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(
          bottom = galleryViewModel.innerPaddings.calculateBottomPadding()
        ),
      ) {
        stickyHeader(key = TOOLBAR_ITEM_KEY) { GalleryViewToolbar() }

        items(mediaItemsList, key = { it.id }) {
          GalleryMediaThumbnailView(
            item = it,
            size = thumbnailSize,
          )
        }
      }
    }
  }
}

@Composable
private fun InnerSpacer(
  previewedAssetHeightPx: Int,
  nestedScrollConnection: GalleryViewContentNestedScrollConnection
) {
  val density = LocalDensity.current

  val spacerHeight by remember(density) {
    derivedStateOf {
      with(density) {
        (previewedAssetHeightPx + nestedScrollConnection.previewedAssetOffset).toDp()
      }
    }
  }

  Spacer(Modifier.height(spacerHeight))
}

