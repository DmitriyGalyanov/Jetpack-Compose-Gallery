package com.dgalyanov.gallery.ui.commonViews

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
internal fun FillingLoaderView(visible: Boolean, enterDelayMs: Int = 200) {
  val visibleState = remember { MutableTransitionState(false) }
    .apply { targetState = visible }

  AnimatedVisibility(
    visibleState = visibleState,
    enter = fadeIn(animationSpec = tween(delayMillis = enterDelayMs, durationMillis = 300)),
    exit = fadeOut(animationSpec = tween(delayMillis = 0, durationMillis = 300)),
    modifier = Modifier
      .fillMaxSize()
      .zIndex(10F)
      .clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
      ) { }
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.background(Color(0, 0, 0, 100))
    ) {
      CircularProgressIndicator(modifier = Modifier.size(28.dp))
    }
  }
}