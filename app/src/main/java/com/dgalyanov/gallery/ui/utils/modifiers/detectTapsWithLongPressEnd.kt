package com.dgalyanov.gallery.ui.utils.modifiers

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal suspend fun AwaitPointerEventScope.detectTapsWithLongPressEnd(
  scope: CoroutineScope,
  longPressTimeoutMs: Long = 300L,
  onPressStart: (() -> Unit)? = null,
  /**
   * doesn't check is Offset in bounds
   */
  onPressEnd: (() -> Unit)? = null,
  onLongPressStart: (() -> Unit)? = null,
  onLongPressEnd: (() -> Unit)? = null,
) {
  val down = awaitFirstDown().also { it.consume() }

  onPressStart?.invoke()

  var wasOnLongPressStartInvoked = false

  val onLongPressIsEnabled = onLongPressStart != null || onLongPressEnd != null
  val longPressAwaitJob = if (onLongPressIsEnabled) scope.launch {
    delay(longPressTimeoutMs)
    if (down.pressed) {
      onLongPressStart?.invoke()
      wasOnLongPressStartInvoked = true
    }
  } else null

  do {
    val event = awaitPointerEvent()
    event.changes.forEach { it.consume() }
  } while (event.changes.any { it.pressed })

  longPressAwaitJob?.cancel()
  if (wasOnLongPressStartInvoked) onLongPressEnd?.invoke()
  else onPressEnd?.invoke()
}
