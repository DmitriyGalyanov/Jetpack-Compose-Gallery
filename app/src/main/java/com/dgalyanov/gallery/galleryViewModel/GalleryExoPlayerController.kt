package com.dgalyanov.gallery.galleryViewModel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.dgalyanov.gallery.utils.GalleryLogFactory
import com.dgalyanov.gallery.utils.postToMainThread

class GalleryExoPlayerController(context: Context) {
  companion object {
    private const val DEFAULT_IS_MUTED = false
  }

  val log = GalleryLogFactory("ExoPlayerHolder")

  val exoPlayer = ExoPlayer.Builder(context).build().apply {
    volume = if (DEFAULT_IS_MUTED) 0F else 1F
    repeatMode = Player.REPEAT_MODE_ALL
  }

  private fun applySettingsToExoPlayer() =
    postToMainThread { exoPlayer.volume = if (isMuted) 0F else 1F }

  var isMuted by mutableStateOf(DEFAULT_IS_MUTED)
    private set

  fun toggleMute() {
    isMuted = !isMuted
    applySettingsToExoPlayer()
  }

  private var videoUri: Uri? = null
  fun setMedia(videoUri: Uri?) {
    log("setMedia(videoUri: $videoUri) | current: $videoUri")
    if (this.videoUri == videoUri) return
    this.videoUri = videoUri

    if (videoUri == null) return exoPlayer.stop()

    exoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
  }

  var statefulPlayWhenReady by mutableStateOf(false)
    private set

  fun play() {
    if (videoUri == null) return

    // todo: check if it's ok to call prepare on every play request
    exoPlayer.prepare()
    exoPlayer.play()
    statefulPlayWhenReady = true
  }

  fun pause() {
    exoPlayer.pause()
    statefulPlayWhenReady = false
  }

  @Composable
  fun useSyncPlayWithLifecycle(): Boolean {
    var isPausedByLifecycle by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
      val observer = object : DefaultLifecycleObserver {
        override fun onPause(owner: LifecycleOwner) {
          super.onPause(owner)

          if (exoPlayer.playWhenReady) {
            isPausedByLifecycle = true
            pause()
          }
        }

        override fun onResume(owner: LifecycleOwner) {
          super.onResume(owner)

          if (isPausedByLifecycle) play()
          isPausedByLifecycle = false
        }
      }

      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return isPausedByLifecycle
  }

  fun togglePlayPause() {
    if (exoPlayer.playWhenReady) pause()
    else play()
  }

  fun onDispose() {
    exoPlayer.release()
  }
}