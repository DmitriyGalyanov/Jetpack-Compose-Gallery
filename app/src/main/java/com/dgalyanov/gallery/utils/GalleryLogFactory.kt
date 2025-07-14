package com.dgalyanov.gallery.utils

import android.util.Log

internal const val GALLERY_BASE_LOG_TAG = "Gallery"

private const val DEBUG = true

@Suppress("FunctionName")
internal fun GalleryLogFactory(
  tag: String,
  messagePrefix: String = "",
  isEnabled: Boolean = true,
): (messageGetter: () -> String) -> Unit {
  return fun(messageGetter: () -> String) {
    if (!DEBUG || !isEnabled) return
    // long Tags cause Formatting Problems in Studio's Logcat
    Log.d("${GALLERY_BASE_LOG_TAG}.$tag", "$messagePrefix ${messageGetter()}")
  }
}

internal val galleryGenericLog = GalleryLogFactory("${GALLERY_BASE_LOG_TAG}_generic")
