package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewToolbar.galleryAlbumsSheetButton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.ui.styleConsts.GalleryStyleConsts
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GalleryAlbumsSheet(sheetState: SheetState, onDidDismiss: () -> Unit) {
  ModalBottomSheet(
    onDismissRequest = onDidDismiss,
    sheetState = sheetState,
  ) {
    GalleryAlbumsSheetContent(sheetState, onDidDismiss)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryAlbumsSheetContent(sheetState: SheetState, onDidDismiss: () -> Unit) {
  val scope = rememberCoroutineScope()

  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current

  val shouldShowLoader =
    remember { derivedStateOf { galleryViewModel.albumsList.isEmpty() && galleryViewModel.isFetchingAlbums } }

  fun hide() {
    scope.launch { sheetState.hide() }
      .invokeOnCompletion { onDidDismiss() }
  }

  Column {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = GalleryStyleConsts.COMMON_HORIZONTAL_PADDING)
    ) {
      Text("Cancel", modifier = Modifier.clickable(onClick = ::hide))

      Text(
        text = galleryViewModel.selectedAlbum.name,
        modifier = Modifier.align(Alignment.Center),
        fontWeight = FontWeight.Bold,
      )
    }

    if (shouldShowLoader.value) {
      Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) { CircularProgressIndicator() }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(GalleryStyleConsts.COMMON_HORIZONTAL_PADDING, 8.dp)
      ) {
        items(galleryViewModel.albumsList, { it.id }) {
          GalleryAlbumPreviewView(it) {
            galleryViewModel.selectAlbum(it)
            hide()
          }
        }
      }
    }
  }
}

