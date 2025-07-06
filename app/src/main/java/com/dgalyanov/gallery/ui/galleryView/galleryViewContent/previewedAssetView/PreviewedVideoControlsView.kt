package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.previewedAssetView

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgalyanov.gallery.galleryViewModel.ExoPlayerController
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel

@Composable
internal fun PreviewedVideoControlsView(
  exoPlayerController: ExoPlayerController,
  isVisible: Boolean,
) {
  AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn(animationSpec = GalleryViewModel.PREVIEWED_ASSET_SELECTION_RELATED_ANIMATIONS_SPEC),
    exit = fadeOut(animationSpec = GalleryViewModel.PREVIEWED_ASSET_SELECTION_RELATED_ANIMATIONS_SPEC),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = exoPlayerController::togglePlayPause,
        )
    ) {
      AnimatedVisibility(
        visible = exoPlayerController.statefulHasMedia &&
                  exoPlayerController.isPlayAllowed &&
                  !exoPlayerController.statefulPlayWhenReady &&
                  !exoPlayerController.isPausedByLifecycle,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
          .clip(RoundedCornerShape(4.dp))
          .align(Alignment.Center)
      ) {
        Text(
          "Paused", modifier = Modifier
            .background(Color(0, 0, 0, 100))
            .padding(horizontal = 4.dp)
        )
      }

      TextButton(
        onClick = exoPlayerController::toggleMute,
        modifier = Modifier.align(Alignment.BottomEnd),
      ) {
        Text(if (exoPlayerController.isMuted) "unmute" else "mute", fontSize = 18.sp)
      }
    }
  }
}