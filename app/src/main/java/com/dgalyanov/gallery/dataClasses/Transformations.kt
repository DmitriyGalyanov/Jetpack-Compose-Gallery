package com.dgalyanov.gallery.dataClasses

import androidx.compose.ui.geometry.Offset
import kotlin.math.round

internal data class Transformations(val scale: Float, val offset: Offset) {
  companion object {
    val Empty = Transformations(scale = 1f, offset = Offset.Zero)

    fun toClamped(
      rawScale: Float,
      minScale: Float,
      maxScale: Float,
      displayedContentSize: AssetSize,
      cropContainerSize: AssetSize,
      rawOffset: Offset,
    ): Transformations {
      val coercedMaxScale = maxScale.coerceAtLeast(minScale)
      val scale = rawScale.coerceIn(minScale, coercedMaxScale)

      val scaledContentSize = AssetSize(
        width = displayedContentSize.width * scale,
        height = displayedContentSize.height * scale,
      )

      val leftEdgeX = (displayedContentSize.width * (scale - 1)) / 2
      val rightEdgeX = leftEdgeX + cropContainerSize.width

      var clampedX = rawOffset.x
      var clampedY = rawOffset.y

      val doesContentIntersectLeftEdge =
        rawOffset.x + scaledContentSize.width >= leftEdgeX && rawOffset.x <= leftEdgeX
      val doesContentIntersectRightEdge =
        rawOffset.x + scaledContentSize.width >= rightEdgeX && rawOffset.x <= rightEdgeX
      val shouldAdjustX = !doesContentIntersectLeftEdge || !doesContentIntersectRightEdge

      val shouldAdjustXToLeftEdge = shouldAdjustX && rawOffset.x > leftEdgeX
      if (shouldAdjustXToLeftEdge) clampedX = leftEdgeX.toFloat()

      val shouldAdjustXToRightEdge =
        shouldAdjustX && !shouldAdjustXToLeftEdge && rawOffset.x - scaledContentSize.width < rightEdgeX
      if (shouldAdjustXToRightEdge) clampedX = (rightEdgeX - scaledContentSize.width).toFloat()
      // HORIZONTAL ADJUSTMENT -- END

      // VERTICAL ADJUSTMENT -- START
      val topEdgeY = (displayedContentSize.height * (scale - 1)) / 2
      val bottomEdgeY = topEdgeY + cropContainerSize.height

      val doesContentIntersectTopEdge =
        rawOffset.y <= topEdgeY && scaledContentSize.height + rawOffset.y >= topEdgeY
      val doesContentIntersectBottomEdge =
        rawOffset.y + scaledContentSize.height >= bottomEdgeY && rawOffset.y <= bottomEdgeY
      val shouldAdjustY = !doesContentIntersectBottomEdge || !doesContentIntersectTopEdge

      val shouldAdjustYToTopEdge = shouldAdjustY && rawOffset.y > topEdgeY
      if (shouldAdjustYToTopEdge) clampedY = topEdgeY.toFloat()

      val shouldAdjustYToBottomEdge =
        shouldAdjustY && !shouldAdjustYToTopEdge && rawOffset.y - scaledContentSize.height < bottomEdgeY
      if (shouldAdjustYToBottomEdge) clampedY = (bottomEdgeY - scaledContentSize.height).toFloat()

      return Transformations(
        scale = scale, offset = Offset(x = round(clampedX), y = round(clampedY))
      )
    }
  }
}