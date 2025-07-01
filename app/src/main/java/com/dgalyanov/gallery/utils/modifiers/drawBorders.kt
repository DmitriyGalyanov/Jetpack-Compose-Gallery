package com.dgalyanov.gallery.utils.modifiers

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

internal enum class BorderSide {
  Top, Bottom, Left, Right,
}

private fun drawBorderLine(
  color: Color,
  width: Float,
  side: BorderSide,
  drawScope: DrawScope,
) = drawScope.drawLine(
  color = color, strokeWidth = width, start = when (side) {
    BorderSide.Top -> Offset.Zero
    BorderSide.Bottom -> Offset(x = 0f, y = drawScope.size.height)
    BorderSide.Left -> Offset.Zero
    BorderSide.Right -> Offset(x = drawScope.size.width, y = 0f)
  }, end = when (side) {
    BorderSide.Top -> Offset(x = drawScope.size.width, y = 0f)
    BorderSide.Bottom -> Offset(x = drawScope.size.width, y = drawScope.size.height)
    BorderSide.Left -> Offset(x = 0f, y = drawScope.size.height)
    BorderSide.Right -> Offset(x = drawScope.size.width, y = drawScope.size.height)
  }
)

internal fun Modifier.drawBorders(
  color: Color,
  width: Float,
  sides: List<BorderSide>,
): Modifier = this.drawBehind {
  sides.distinct().forEach {
    drawBorderLine(
      color = color,
      width = width,
      side = it,
      drawScope = this,
    )
  }
}

internal fun Modifier.drawBorder(
  color: Color,
  width: Float,
  side: BorderSide,
) = this.drawBorders(color = color, width = width, sides = listOf(side))