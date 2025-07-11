package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewToolbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewToolbar.galleryAlbumsSheetButton.GalleryAlbumsSheetButton
import com.dgalyanov.gallery.ui.theme.withCoercedFontScaleForText

internal val GALLERY_VIEW_TOOLBAR_HEIGHT = 48.dp

@Composable
internal fun GalleryViewToolbar() {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current
  val isMultiselectEnabled = galleryViewModel.isMultiselectEnabled

  val isSelectingDrafts = galleryViewModel.isSelectingDraft

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.Black)
      .requiredHeight(GALLERY_VIEW_TOOLBAR_HEIGHT)
      .padding(8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      GalleryAlbumsSheetButton {
        if (isSelectingDrafts) {
          galleryViewModel.setIsSelectingDraft(false)
          return@GalleryAlbumsSheetButton true
        }
        return@GalleryAlbumsSheetButton false
      }

      AnimatedVisibility(
        visible = galleryViewModel.selectedCreativityTypeHasDrafts,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        Text(
          "Drafts",
          color = if (isSelectingDrafts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
          fontSize = 16.withCoercedFontScaleForText(),
          fontWeight = FontWeight.W500,
          modifier = Modifier.clickable {
            galleryViewModel.setIsSelectingDraft(!isSelectingDrafts)
          }
        )
      }
    }

    AnimatedVisibility(
      visible = !isSelectingDrafts,
      enter = fadeIn(),
      exit = fadeOut(),
    ) {
      Text(
        text = "Multiselect: ${if (isMultiselectEnabled) "enabled" else "disabled"}",
        modifier = Modifier
          .clickable(onClick = galleryViewModel::toggleIsMultiselectEnabled)
      )
    }
  }
}
