package com.dgalyanov.gallery.utils

import android.util.Log

internal const val GALLERY_BASE_LOG_TAG = "Gallery"

private const val DEBUG = false

@Suppress("FunctionName")
internal fun GalleryLogFactory(
  tag: String,
  messagePrefix: String = "",
): (message: String) -> Unit {
  // todo: use message getter
  return fun(message: String) {
    if (!DEBUG) return
    // long Tags cause Formatting Problems in Studio's Logcat
    Log.d("${GALLERY_BASE_LOG_TAG}_$tag", "$messagePrefix $message")
  }
}

internal val galleryGenericLog = GalleryLogFactory("${GALLERY_BASE_LOG_TAG}_generic")
