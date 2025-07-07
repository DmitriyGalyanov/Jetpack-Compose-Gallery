package com.dgalyanov.gallery.galleryViewModel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import com.dgalyanov.gallery.utils.GalleryLogFactory

private typealias OnVolumeChanged = (prevVolumeLevel: Int, volumeLevel: Int) -> Unit

internal class VolumeEventsReceiver private constructor(
  private val context: Context,
  private val onVolumeChanged: OnVolumeChanged,
  private val onRingerModeChange: (ringerMode: Int) -> Unit,
) : BroadcastReceiver() {
  companion object {
    private val log = GalleryLogFactory("VolumeEventsReceiver")

    // https://android.googlesource.com/platform/frameworks/base/+/8bd69610aafc6995126965d1d23b771fe02a9084/media/java/android/media/AudioManager.java#106
    private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"

    fun createAndRegister(
      context: Context,
      onVolumeChanged: OnVolumeChanged,
      onRingerModeChange: (ringerMode: Int) -> Unit,
    ): VolumeEventsReceiver {
      val receiver = VolumeEventsReceiver(
        context = context,
        onVolumeChanged = onVolumeChanged,
        onRingerModeChange = onRingerModeChange,
      )

      val intentFilter =
        IntentFilter(VOLUME_CHANGED_ACTION).apply { addAction(AudioManager.RINGER_MODE_CHANGED_ACTION) }
      context.registerReceiver(receiver, intentFilter)

      return receiver
    }
  }

  fun unregister() = context.unregisterReceiver(this)

  override fun onReceive(context: Context?, intent: Intent) {
    log {
      "intent: $intent, intent.action: ${intent.action}, intent.extras (stringified): ${
        intent.extras?.keySet()?.map { key -> "[$key]: ${intent.getIntExtra(key, -1)}" }
      }"
    }

    when (intent.action) {
      VOLUME_CHANGED_ACTION -> {
        val prevVolumeLevel = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", -1)
        val newVolumeLevel = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", 0)
        onVolumeChanged(prevVolumeLevel, newVolumeLevel)
      }

      AudioManager.RINGER_MODE_CHANGED_ACTION -> {
        val newRingerMode = intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, 0)
        onRingerModeChange(newRingerMode)
      }
    }
  }
}