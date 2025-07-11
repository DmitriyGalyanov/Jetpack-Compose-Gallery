package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewContentCameraItem.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewContentCameraItem.CameraControl
import com.dgalyanov.gallery.ui.utils.modifiers.detectTapsWithLongPressEnd


@Composable
internal fun BoxScope.CameraCaptureButton(
  cameraControl: CameraControl,
  onPressEnd: () -> Unit,
  onLongPressStart: () -> Unit,
  onLongPressEnd: () -> Unit,
) {
  val scope = rememberCoroutineScope()

  val density = LocalDensity.current.density
  val outerRingScale by animateFloatAsState(
    targetValue = if (cameraControl.isRecordingVideo) 1.7f else 1f,
    animationSpec = tween(250),
  )

  Canvas(
    Modifier
      .size(72.dp)
      .offset(y = (-12).dp)
      .align(Alignment.BottomCenter)
      .pointerInput(Unit) {
        awaitEachGesture {
          detectTapsWithLongPressEnd(
            scope = scope,
            onPressEnd = onPressEnd,
            onLongPressStart = onLongPressStart,
            onLongPressEnd = onLongPressEnd,
          )
        }
      }
  ) {
    val paddingBetweenOuterRingAndCenterCircle = 2f * density
    val outerRingStrokeWidth = 4f * density

    val canvasSize = size.width

    // draw inner Circle
    drawCircle(
      color = Color.White,
      radius = (canvasSize - paddingBetweenOuterRingAndCenterCircle * 2 - outerRingStrokeWidth * 2) / 2
    )

    scale(outerRingScale) {
      val outerRingRadius = (canvasSize - outerRingStrokeWidth) / 2

      // draw base outer circle
      drawCircle(
        color = Color.White,
        radius = outerRingRadius,
        style = Stroke(width = outerRingStrokeWidth, cap = StrokeCap.Round),
      )

      val currentRecordingToMaxAllowedDuration =
        cameraControl.currentRecordingDurationMs / CameraControl.MAX_VIDEO_DURATION_MS

      // draw animated circle
      rotate(-90f) {
        drawArc(
          color = Color(0x79, 0x3f, 0xf3),
          startAngle = 0f,
          sweepAngle = 360f * currentRecordingToMaxAllowedDuration,
          useCenter = false,
          topLeft = Offset(outerRingStrokeWidth / 2, outerRingStrokeWidth / 2),
          size = Size(outerRingRadius * 2, outerRingRadius * 2),
          style = Stroke(width = outerRingStrokeWidth, cap = StrokeCap.Round),
        )
      }
    }
  }
}
