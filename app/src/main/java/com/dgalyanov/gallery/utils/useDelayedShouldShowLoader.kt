package com.dgalyanov.gallery.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun useDelayedShouldShowLoader(condition: Boolean, delayMs: Long = 100): Boolean {
  var shouldShowLoader by remember { mutableStateOf(false) }

  var loaderJob by remember { mutableStateOf<Job?>(null) }
  val scope = rememberCoroutineScope()
  DisposableEffect(condition) {
    loaderJob = scope.launch {
      if (condition) {
        delay(delayMs)
        shouldShowLoader = true
      } else {
        loaderJob?.cancel()
        shouldShowLoader = false
      }
    }
    onDispose { loaderJob?.cancel() }
  }

  return shouldShowLoader
}
