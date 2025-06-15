package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryPreviewedAssetView.exoPlayerPlayerView

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.dgalyanov.gallery.galleryViewModel.GalleryExoPlayerController

@OptIn(UnstableApi::class)
@Composable
internal fun ExoPlayerPlayerView(
  playerController: GalleryExoPlayerController,
  resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
) {
  val isPausedByLifecycle = playerController.useSyncPlayWithLifecycle()

  Box(
    Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    ExoPlayerPlayerSurface(
      player = playerController.exoPlayer,
      resizeMode = resizeMode,
    ) { playerController.togglePlayPause() }

    AnimatedVisibility(
      !playerController.statefulPlayWhenReady && !isPausedByLifecycle,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      Text(
        "Paused",
        modifier = Modifier
          .clip(RoundedCornerShape(4.dp))
          .align(Alignment.Center)
          .background(Color(0, 0, 0, 100))
          .padding(horizontal = 4.dp)
      )
    }

    TextButton(
      playerController::toggleMute,
      modifier = Modifier.align(Alignment.BottomEnd)
    ) {
      Text(if (playerController.isMuted) "unmute" else "mute", fontSize = 18.sp)
    }
  }
}
