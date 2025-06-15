package com.dgalyanov.gallery.utils

import android.os.Handler
import android.os.Looper

internal val mainThreadHandler = Handler(Looper.getMainLooper())

internal fun postToMainThread(runnable: Runnable) {
  mainThreadHandler.post(runnable)
}

internal fun postToMainThreadDelayed(delayMs: Long, runnable: Runnable) {
  mainThreadHandler.postDelayed(runnable, delayMs)
}

internal fun removeCallbacksFromMainThread(runnable: Runnable) {
  mainThreadHandler.removeCallbacks(runnable)
}
