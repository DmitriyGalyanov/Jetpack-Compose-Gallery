package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.previewedAssetView

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import com.dgalyanov.gallery.dataClasses.AssetSize
import com.dgalyanov.gallery.dataClasses.Transformations
import com.dgalyanov.gallery.utils.modifiers.conditional

private fun getTopLeftContentContainerOffset(
  wrapSize: AssetSize, contentContainerSize: AssetSize
) = Offset(
  x = ((wrapSize.width - contentContainerSize.width) / 2).toFloat(),
  y = ((wrapSize.height - contentContainerSize.height) / 2).toFloat(),
)

// todo: support cropAreaExtraScale
private fun Modifier.drawContentWithContainerMask(
  wrapSize: AssetSize,
  contentContainerSize: AssetSize,
): Modifier {
  return this.drawWithContent {
    drawContent()

    val maskColor = Color(0, 0, 0, 150)

    val topLeftRectOffset = getTopLeftContentContainerOffset(wrapSize, contentContainerSize)

    val topLeftRectWidth =
      (if (topLeftRectOffset.y > 0) wrapSize.width else topLeftRectOffset.x).toFloat()
    val topLeftRectHeight =
      (if (topLeftRectOffset.x > 0) wrapSize.height else topLeftRectOffset.y).toFloat()

    drawRect(
      color = maskColor,
      topLeft = Offset(0F, 0F),
      size = Size(width = topLeftRectWidth, height = topLeftRectHeight),
    )

    val bottomRightRectWidth =
      (if (topLeftRectOffset.y > 0) wrapSize.width else topLeftRectOffset.x).toFloat()
    val bottomRightRectHeight =
      (if (topLeftRectOffset.x > 0) wrapSize.height else topLeftRectOffset.y).toFloat()

    drawRect(
      color = maskColor,
      topLeft = Offset(
        x = (if (topLeftRectOffset.x > 0) topLeftRectOffset.x + contentContainerSize.width else 0).toFloat(),
        y = (if (topLeftRectOffset.y > 0) topLeftRectOffset.y + contentContainerSize.height else 0).toFloat(),
      ),
      size = Size(width = bottomRightRectWidth, height = bottomRightRectHeight),
    )
  }
}

@Composable
internal fun GesturesTransformView(
  /**
   * it [transformable] applied
   */
  isEnabled: Boolean,

  /**
   * [Transformations] to be applied initially
   *
   * if null -> defaults to [minScale] for scale and [Offset.Zero] for offset
   */
  initialTransformations: Transformations?,

  minScale: Float,
  maxScale: Float = 3F,

  /**
   * visible content should occupy this size
   */
  actualContentSize: AssetSize,
  /**
   * visible content will be anchored to window of this size
   */
  contentContainerSize: AssetSize,
  /**
   * visible content will be translated ([GraphicsLayerScope.translationX], [GraphicsLayerScope.translationY])
   * using this offset
   */
  contentBaseOffset: Offset,

  /**
   * called when applied [Transformations] are [clamped][Transformations.toClamped]
   */
  onTransformationDidClamp: (transformations: Transformations) -> Unit,

  content: @Composable BoxScope.() -> Unit,
) {
  BoxWithConstraints(Modifier.fillMaxSize()) {
    val wrapSize = AssetSize(
      width = constraints.maxWidth.toDouble(),
      height = constraints.maxHeight.toDouble(),
    )

    var scale by remember { mutableFloatStateOf(initialTransformations?.scale ?: minScale) }
    var offset by remember { mutableStateOf(initialTransformations?.offset ?: Offset.Zero) }

    fun clampTransformations(): Transformations {
      val clampedTransformations = Transformations.toClamped(
        rawScale = scale,
        minScale = minScale,
        maxScale = maxScale,
        actualContentSize = actualContentSize,
        topLeftClampVector = getTopLeftContentContainerOffset(wrapSize, contentContainerSize),
        contentBaseOffset = contentBaseOffset,
        contentContainerSize = contentContainerSize,
        rawOffset = offset,
      )

      scale = clampedTransformations.scale
      offset = clampedTransformations.offset

      return clampedTransformations
    }

    val transformableState = rememberTransformableState { zoomChange, panChange, rotationChange ->
      scale *= zoomChange

      offset = Offset(
        x = (offset.x + scale * panChange.x),
        y = (offset.y + scale * panChange.y),
      )
    }

    LaunchedEffect(
      minScale,
      maxScale,
      transformableState.isTransformInProgress,
    ) {
      if (transformableState.isTransformInProgress) return@LaunchedEffect
      onTransformationDidClamp(clampTransformations())
    }

    Box(
      modifier = Modifier
        .background(Color.DarkGray)
        .fillMaxSize()
        .drawContentWithContainerMask(
          wrapSize = wrapSize,
          contentContainerSize = contentContainerSize,
        )
    ) {
      Box(
        content = content,
        modifier = Modifier
          .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationX = contentBaseOffset.x + offset.x
            translationY = contentBaseOffset.y + offset.y
          }
          .conditional(isEnabled) { transformable(state = transformableState) },
      )
    }
  }
}