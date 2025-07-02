package com.dgalyanov.gallery.utils

import android.content.ContentValues
import android.media.MediaFormat.MIMETYPE_VIDEO_MPEG4
import android.os.Build
import android.provider.MediaStore
import com.dgalyanov.gallery.R
import java.text.SimpleDateFormat
import java.util.Locale

internal object MediaMetadataHelper {
  private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

  private fun createPictureDisplayName(): String =
    SimpleDateFormat(FILENAME_FORMAT, Locale.ENGLISH).format(System.currentTimeMillis())

  fun createContentValuesForPicture(): ContentValues {
    val name = createPictureDisplayName()
    return ContentValues().apply {
      put(MediaStore.MediaColumns.DISPLAY_NAME, name)
      put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
        val appName = R.string.app_name
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$appName")
      }
    }
  }

  private fun createVideoDisplayName(): String = "VIDEO_${
    SimpleDateFormat(FILENAME_FORMAT, Locale.ENGLISH).format(System.currentTimeMillis())
  }.mp4"

  fun createContentValuesForVideo(): ContentValues {
    val name = createVideoDisplayName()
    return ContentValues().apply {
      put(MediaStore.MediaColumns.DISPLAY_NAME, name)
      put(MediaStore.MediaColumns.MIME_TYPE, MIMETYPE_VIDEO_MPEG4)
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
        val appName = R.string.app_name
        put(MediaStore.Images.Media.RELATIVE_PATH, "Movies/$appName")
      }
    }
  }
}
