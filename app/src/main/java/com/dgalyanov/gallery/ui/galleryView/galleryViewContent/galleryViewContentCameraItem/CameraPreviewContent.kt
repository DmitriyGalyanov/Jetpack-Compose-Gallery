package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewContentCameraItem

import android.app.Activity
import android.content.Context
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import android.widget.LinearLayout
import androidx.camera.core.CameraSelector
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner

private fun Context.getWindow(): Window? {
  var currentContext = this

  while (currentContext is android.content.ContextWrapper) {
    if (currentContext is Activity) return currentContext.window

    currentContext = currentContext.baseContext
  }

  return null
}

@Composable
internal fun CameraPreviewContent(
  cameraController: LifecycleCameraController,
  lifecycleOwner: LifecycleOwner,
  onDidSetScreenFlashWindow: (window: Window?) -> Unit,
  shouldFlashPreview: Boolean,
  modifier: Modifier = Modifier,
) {
  Box(modifier.background(Color.Black)) {
    AndroidView(
      modifier = modifier.fillMaxSize(),
      factory = { context ->
        PreviewView(context).apply {
          layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
          setBackgroundColor(android.graphics.Color.BLACK)

          implementationMode = PreviewView.ImplementationMode.PERFORMANCE
          scaleType = PreviewView.ScaleType.FILL_START

          val window = context.getWindow()
          setScreenFlashWindow(window)
          onDidSetScreenFlashWindow(window)

          controller = cameraController
          cameraController.bindToLifecycle(lifecycleOwner)
          cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
      },
      update = { view ->
        val window = view.context.getWindow()
        view.setScreenFlashWindow(window)
        onDidSetScreenFlashWindow(window)
      }
    )

    val flashAlpha by animateFloatAsState(
      targetValue = if (shouldFlashPreview) 0.9f else 0f,
      animationSpec = tween(durationMillis = 100, easing = LinearEasing),
    )
    if (flashAlpha > 0f) Box(
      modifier
        .fillMaxSize()
        .graphicsLayer { alpha = flashAlpha }
        .background(Color.White)
    )
  }
}
