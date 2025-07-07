package com.dgalyanov.gallery.ui.galleryView

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dgalyanov.gallery.dataClasses.CreativityType
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.ui.styleConsts.GalleryStyleConsts
import com.dgalyanov.gallery.ui.utils.modifiers.conditional

@Composable
internal fun GalleryViewHeader() {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current

  val density = LocalDensity.current.density
  var emissionButtonSize by remember { mutableStateOf<IntSize?>(null) }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.Black)
      .zIndex(1F)
      .height(galleryViewModel.innerPaddings.calculateTopPadding() + 40.dp)
      .padding(
        top = galleryViewModel.innerPaddings.calculateTopPadding(),
        start = GalleryStyleConsts.COMMON_HORIZONTAL_PADDING,
        end = GalleryStyleConsts.COMMON_HORIZONTAL_PADDING,
      ),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text("Gallery", fontWeight = FontWeight.Bold)

    AnimatedContent(
      targetState = galleryViewModel.isPreparingSelectedAssetsForEmission,
      contentAlignment = Alignment.Center,
      transitionSpec = {
        val animationSpec = tween<Float>(300)
        fadeIn(animationSpec = animationSpec).togetherWith(fadeOut(animationSpec = animationSpec))
      },
    ) { shouldShowLoader ->
      if (shouldShowLoader) {
        Box(
          // todo: replace with a better (?) size workaround
          modifier = Modifier.conditional(emissionButtonSize != null) {
            size(
              width = (emissionButtonSize!!.width.toFloat() / density).dp,
              height = (emissionButtonSize!!.height.toFloat() / density).dp
            )
          },
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        }
      } else if (
        galleryViewModel.anAssetIsSelected ||
        galleryViewModel.selectedCreativityType == CreativityType.NeuroStory
      ) {
        Text(
          "Proceed",
          modifier = Modifier
            .clickable(onClick = {
              if (galleryViewModel.selectedCreativityType == CreativityType.NeuroStory) {
                galleryViewModel.onNeuroStoriesProceedRequest()
              } else galleryViewModel.emitCurrentlySelectedAssets()
            })
            .onSizeChanged { emissionButtonSize = it })
      }
    }
  }
}