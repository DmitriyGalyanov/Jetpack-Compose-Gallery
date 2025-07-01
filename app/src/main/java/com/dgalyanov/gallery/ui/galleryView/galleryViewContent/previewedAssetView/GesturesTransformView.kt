package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.previewedAssetView

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dgalyanov.gallery.dataClasses.AssetSize
import com.dgalyanov.gallery.dataClasses.Transformations
import com.dgalyanov.gallery.utils.modifiers.BorderSide
import com.dgalyanov.gallery.utils.modifiers.conditional
import com.dgalyanov.gallery.utils.modifiers.drawBorders
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

private fun getTopLeftContentContainerOffset(
  wrapSize: AssetSize, contentContainerSize: AssetSize
) = Offset(
  x = ((wrapSize.width - contentContainerSize.width) / 2).toFloat(),
  y = ((wrapSize.height - contentContainerSize.height) / 2).toFloat(),
)

private const val TRANSFORMATION_CLAMP_ANIMATION_DURATION_MS = 250

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

    val scope = rememberCoroutineScope()
    val transformationsClampAnimationsJobs = remember { mutableListOf<Job>() }
    fun clearTransformationsClampAnimations() {
      if (transformationsClampAnimationsJobs.isEmpty()) return
      transformationsClampAnimationsJobs.forEach { it.cancel() }
      transformationsClampAnimationsJobs.clear()
    }

    fun clampTransformations(): Transformations {
      val clampedTransformations = Transformations.toClamped(
        rawScale = scale,
        minScale = minScale,
        maxScale = maxScale,
        actualContentSize = actualContentSize,
        contentContainerSize = contentContainerSize,
        rawOffset = offset,
      )

      val animatableScale = Animatable(scale)
      val animatableOffset = Animatable(offset, Offset.VectorConverter)

      clearTransformationsClampAnimations()
      transformationsClampAnimationsJobs += scope.launch {
        if (scale != clampedTransformations.scale) {
          animatableScale.animateTo(
            clampedTransformations.scale,
            animationSpec = tween(TRANSFORMATION_CLAMP_ANIMATION_DURATION_MS)
          ) { scale = this.value }
        }
      }
      transformationsClampAnimationsJobs += scope.launch {
        if (offset != clampedTransformations.offset) {
          animatableOffset.animateTo(
            clampedTransformations.offset,
            animationSpec = tween(TRANSFORMATION_CLAMP_ANIMATION_DURATION_MS)
          ) { offset = this.value }
        }
      }

      return clampedTransformations
    }

    val transformableState = rememberTransformableState { zoomChange, panChange, rotationChange ->
      clearTransformationsClampAnimations()

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
      val contentBaseOffset = getTopLeftContentContainerOffset(wrapSize, contentContainerSize)

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

      Grid(
        actualContentSize = actualContentSize,
        contentBaseOffset = contentBaseOffset,
        contentContainerSize = contentContainerSize,
        scale = scale,
        offset = offset,
        isVisible = transformableState.isTransformInProgress,
      )
    }
  }
}

private const val GRID_ROWS_AMOUNT = 3
private const val GRID_COLUMNS_AMOUNT = 3
private const val GRID_CELLS_AMOUNT = GRID_ROWS_AMOUNT * GRID_COLUMNS_AMOUNT

private val GRID_COLOR = Color(0, 0, 0)

@Composable
private fun Grid(
  actualContentSize: AssetSize,
  contentBaseOffset: Offset,
  contentContainerSize: AssetSize,
  scale: Float,
  offset: Offset,
  isVisible: Boolean,
) {
  val density = LocalDensity.current

  val animatedAlpha = animateFloatAsState(
    if (isVisible) 1f else 0f,
    animationSpec = tween(
      durationMillis = 200,
      delayMillis = if (isVisible) 0 else 250
    )
  )

  // todo: optimize
  val modifier = run {
    val scaledContentSize = AssetSize(
      width = actualContentSize.width * scale,
      height = actualContentSize.height * scale,
    )

    /** Horizontal -- START */
    val contentContainerLeftBorderX = (actualContentSize.width * (scale - 1)) / 2
    val wrapLeftBorderX = contentContainerLeftBorderX - contentBaseOffset.x

    val contentContainerRightBorderX = contentContainerLeftBorderX + contentContainerSize.width
    val wrapRightBorderX = contentContainerRightBorderX + contentBaseOffset.x

    val offsetContentLeftBorderX = offset.x
    val offsetContentRightBorderX = offset.x + scaledContentSize.width

    val visibleContentLeftBorderX = max(wrapLeftBorderX, offsetContentLeftBorderX.toDouble())
    val visibleContentRightBorderX = min(wrapRightBorderX, offsetContentRightBorderX)
    val visibleContentWidthPx = visibleContentRightBorderX - visibleContentLeftBorderX

    val gridWidthDp = (visibleContentWidthPx / density.density).dp
    /** Horizontal -- END */

    /** Vertical -- START */
    val contentContainerTopBorderY = (actualContentSize.height * (scale - 1)) / 2
    val wrapTopBorderY = contentContainerTopBorderY - contentBaseOffset.y

    val contentContainerBottomBorderY = contentContainerTopBorderY + contentContainerSize.height
    val wrapBottomBorderY = contentContainerBottomBorderY + contentBaseOffset.y

    val offsetContentTopBorderY = offset.y
    val offsetContentBottomBorderY = offset.y + scaledContentSize.height

    val visibleContentTopBorderY = max(wrapTopBorderY, offsetContentTopBorderY.toDouble())
    val visibleContentBottomBorderY = min(wrapBottomBorderY, offsetContentBottomBorderY)
    val visibleContentHeightPx = visibleContentBottomBorderY - visibleContentTopBorderY

    val gridHeightDp = (visibleContentHeightPx / density.density).dp
    /** Vertical -- END */

    return@run Modifier
      .width(gridWidthDp)
      .height(gridHeightDp)
      .graphicsLayer {
        translationX = contentBaseOffset.x - contentContainerLeftBorderX.toFloat() +
          visibleContentLeftBorderX.toFloat()
        translationY = contentBaseOffset.y - contentContainerTopBorderY.toFloat() +
          visibleContentTopBorderY.toFloat()

        alpha = animatedAlpha.value
      }
  }

  FlowRow(
    maxLines = GRID_ROWS_AMOUNT, maxItemsInEachRow = GRID_COLUMNS_AMOUNT,
    modifier = modifier,
  ) {
    repeat(GRID_CELLS_AMOUNT) { index ->
      val borderSidesToPaint = remember {
        val isOnLeftEdge = index % GRID_COLUMNS_AMOUNT == 0;
        val isOnRightEdge = (index + 1) % GRID_COLUMNS_AMOUNT == 0;
        val isOnTopEdge = index < GRID_COLUMNS_AMOUNT;
        val isOnBottomEdge = index >= GRID_CELLS_AMOUNT - GRID_COLUMNS_AMOUNT;

        val result = BorderSide.entries.toMutableList()
        if (isOnLeftEdge) result -= BorderSide.Left
        if (isOnRightEdge) result -= BorderSide.Right
        if (isOnTopEdge) result -= BorderSide.Top
        if (isOnBottomEdge) result -= BorderSide.Bottom

        return@remember result.toList()
      }

      Box(
        modifier = Modifier
          // using `1f / ...` might cause size overflow (resulting in incorrect amount of cells per unit)
          .fillMaxWidth(0.995f / GRID_COLUMNS_AMOUNT)
          .fillMaxHeight(0.995f / GRID_ROWS_AMOUNT)
          .drawBorders(color = GRID_COLOR, width = 4f, sides = borderSidesToPaint)
      ) {
//        Text(
//          "sides: $paintedBorderSides",
//          fontSize = 8.sp,
//          lineHeight = (8 * 1.2).sp,
//          color = Color.White,
//          modifier = Modifier.background(Color(0, 0, 0, 150))
//        )
      }
    }
  }
}
