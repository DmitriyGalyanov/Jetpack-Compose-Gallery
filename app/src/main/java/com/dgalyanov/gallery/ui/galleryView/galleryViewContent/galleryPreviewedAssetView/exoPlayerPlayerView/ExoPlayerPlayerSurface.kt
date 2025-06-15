package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryPreviewedAssetView.exoPlayerPlayerView

import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.dgalyanov.gallery.R

@OptIn(UnstableApi::class)
@Composable
internal fun ExoPlayerPlayerSurface(
  player: ExoPlayer,
  resizeMode: Int,
  onClick: () -> Unit,
) {
  AndroidView(
    factory = { context ->
      val view = LayoutInflater.from(context).inflate(
        R.layout.exo_player_player_surface_player_view, null
      ) as PlayerView

      view.apply {
        setEnableComposeSurfaceSyncWorkaround(true)

        setOnClickListener { onClick() }

        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)

        clipToOutline = true
        useController = false

        keepScreenOn = true

        setPlayer(player)

        view.resizeMode = resizeMode
      }
    },
    onReset = {},
    // aspectRatio works but it must be changeable (TODO)
    modifier = Modifier.aspectRatio(9f / 16f),
    update = { view ->
      view.resizeMode = resizeMode
    }
  )
}