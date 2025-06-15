package com.dgalyanov.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.dgalyanov.gallery.galleryContentResolver.dataClasses.GalleryMediaItem

/**
 * used only for demo/example
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EmittedSelectionView(galleryViewModel: GalleryViewModel) {

  var emittedItems by remember { mutableStateOf(listOf<GalleryMediaItem>()) }

  LaunchedEffect(Unit) {
    galleryViewModel.setOnEmitSelection {
      emittedItems = it
    }
  }

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var isSheetDisplayed by remember { mutableStateOf(false) }
  LaunchedEffect(emittedItems) {
    if (emittedItems.isNotEmpty()) isSheetDisplayed = true
  }

  if (isSheetDisplayed) EmittedSelectionSheet(
    sheetState = sheetState,
    mediaItems = emittedItems,
  ) {
    isSheetDisplayed = false
    emittedItems = listOf()
  }
}

private val SPACE_BETWEEN_ITEMS = 4.dp
private val PADDING_HORIZONTAL = 12.dp
private const val ITEMS_IN_ROW = 3

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
private fun EmittedSelectionSheet(
  sheetState: SheetState,
  mediaItems: List<GalleryMediaItem>,
  onDidDismiss: () -> Unit,
) {
  ModalBottomSheet(
    sheetState = sheetState,
    onDismissRequest = onDidDismiss,
    containerColor = Color.Black
  ) {
    BoxWithConstraints(
      modifier = Modifier
        .padding(horizontal = PADDING_HORIZONTAL, vertical = 8.dp),
    ) {
      FlowRow(
        maxItemsInEachRow = ITEMS_IN_ROW,
        verticalArrangement = Arrangement.spacedBy(SPACE_BETWEEN_ITEMS),
        horizontalArrangement = Arrangement.spacedBy(SPACE_BETWEEN_ITEMS),
        modifier = Modifier
          .wrapContentSize()
          .verticalScroll(rememberScrollState())
      ) {
        mediaItems.map {
          GlideImage(
            model = it.uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
              .width(this@BoxWithConstraints.maxWidth / ITEMS_IN_ROW - SPACE_BETWEEN_ITEMS)
              .clip(RoundedCornerShape(8.dp))
              .background(Color.Gray)
              .aspectRatio(9F / 16F)
          )
        }
      }
    }
  }
}