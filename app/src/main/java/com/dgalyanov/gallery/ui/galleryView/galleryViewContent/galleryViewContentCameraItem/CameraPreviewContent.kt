package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewContentCameraItem

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.camera.core.CameraSelector
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner

@Composable
internal fun CameraPreviewContent(
  cameraController: LifecycleCameraController,
  lifecycleOwner: LifecycleOwner,
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

          controller = cameraController
          cameraController.bindToLifecycle(lifecycleOwner)
          cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
      },
    )
  }
}
