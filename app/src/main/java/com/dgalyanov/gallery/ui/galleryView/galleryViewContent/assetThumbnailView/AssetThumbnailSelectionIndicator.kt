package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.assetThumbnailView

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dgalyanov.gallery.R
import com.dgalyanov.gallery.dataClasses.GalleryAsset
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.ui.theme.withCoercedFontScaleForNonText
import com.dgalyanov.gallery.ui.theme.withCoercedFontScaleForText
import com.dgalyanov.gallery.ui.utils.modifiers.conditional

private const val WRAP_SIZE = 20

@Composable
internal fun AssetThumbnailSelectionIndicator(selectionIndex: Int) {
  val isMultiselectEnabled = GalleryViewModel.LocalGalleryViewModel.current.isMultiselectEnabled

  val isSelected = selectionIndex != GalleryAsset.NOT_SELECTED_INDEX

  Box(
    Modifier
      .fillMaxSize()
      .conditional(isSelected) { background(Color(255, 255, 255, 120)) },
  ) {
    Box(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .offset((-6).dp, 6.dp)
        .clip(CircleShape)
        .conditional(isMultiselectEnabled) { border(2.dp, Color.White, CircleShape) }
        .size(WRAP_SIZE.withCoercedFontScaleForNonText())
        .conditional(isSelected && isMultiselectEnabled) { background(Color.White) }
    ) {
      if (isSelected) {
        if (isMultiselectEnabled) Text(
          text = (selectionIndex + 1).toString(),
          modifier = Modifier.fillMaxSize(),
          textAlign = TextAlign.Center,
          fontSize = 11.withCoercedFontScaleForText(),
          fontWeight = FontWeight.Black,
          lineHeight = WRAP_SIZE.withCoercedFontScaleForText(),
          color = Color.Black,
        ) else Icon(
          contentDescription = "Transparent checkmark in an accent-colored circle (this asset is selected in non-multiselect mode)",
          painter = painterResource(R.drawable.single_asset_selection_indicator),
          modifier = Modifier.requiredSize((WRAP_SIZE + 4).withCoercedFontScaleForNonText()),
        )
      }
    }
  }
}
