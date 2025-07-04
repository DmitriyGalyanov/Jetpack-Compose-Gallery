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
import com.dgalyanov.gallery.dataClasses.AssetSizeDp
import com.dgalyanov.gallery.dataClasses.Transformations
import com.dgalyanov.gallery.ui.utils.modifiers.BorderSide
import com.dgalyanov.gallery.ui.utils.modifiers.conditional
import com.dgalyanov.gallery.ui.utils.modifiers.drawBorders
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

private fun getCropContainerTopLeftOffset(
  wrapSize: AssetSize, cropContainerSize: AssetSize
) = Offset(
  x = ((wrapSize.width - cropContainerSize.width) / 2).toFloat(),
  y = ((wrapSize.height - cropContainerSize.height) / 2).toFloat(),
)

private const val TRANSFORMATION_CLAMP_ANIMATION_DURATION_MS = 250

// todo: support cropAreaExtraScale
private fun Modifier.drawContentWithCropContainerMask(
  wrapSize: AssetSize,
  cropContainerSize: AssetSize,
): Modifier = this.drawWithContent {
  drawContent()

  val maskColor = Color(0, 0, 0, 150)

  val topLeftRectOffset = getCropContainerTopLeftOffset(wrapSize, cropContainerSize)

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
      x = (if (topLeftRectOffset.x > 0) topLeftRectOffset.x + cropContainerSize.width else 0).toFloat(),
      y = (if (topLeftRectOffset.y > 0) topLeftRectOffset.y + cropContainerSize.height else 0).toFloat(),
    ),
    size = Size(width = bottomRightRectWidth, height = bottomRightRectHeight),
  )
}

@Composable
internal fun GesturesTransformView(
  /**
   * if should apply [transformable]
   */
  isEnabled: Boolean,

  /**
   * [Transformations] to be applied initially
   *
   * if null -> defaults to [minScale] for scale and [Offset.Zero] for offset
   */
  initialTransformations: Transformations?,

  minScale: Float,
  maxScale: Float,

  /**
   * visible content should occupy this size
   */
  displayedContentSize: AssetSize,
  /**
   * visible content will be anchored to window of this size
   */
  cropContainerSize: AssetSize,

  /**
   * called when applied [Transformations] are [clamped][Transformations.toClamped]
   */
  onTransformationsDidClamp: (transformations: Transformations) -> Unit,

  content: @Composable BoxScope.(contentSizeDp: AssetSizeDp) -> Unit,
) {
  BoxWithConstraints(Modifier.fillMaxSize()) {
    val wrapSize = AssetSize(
      width = constraints.maxWidth.toDouble(),
      height = constraints.maxHeight.toDouble(),
    )

    var displayedContentScale by remember {
      mutableFloatStateOf(
        initialTransformations?.scale ?: minScale
      )
    }
    var displayedContentOffset by remember {
      mutableStateOf(
        initialTransformations?.offset ?: Offset.Zero
      )
    }

    val scope = rememberCoroutineScope()
    val transformationsClampAnimationsJobs = remember { mutableListOf<Job>() }
    fun clearTransformationsClampAnimations() {
      if (transformationsClampAnimationsJobs.isEmpty()) return
      transformationsClampAnimationsJobs.forEach { it.cancel() }
      transformationsClampAnimationsJobs.clear()
    }

    fun clampTransformations(): Transformations {
      val clampedTransformations = Transformations.toClamped(
        rawScale = displayedContentScale,
        minScale = minScale,
        maxScale = maxScale,
        displayedContentSize = displayedContentSize,
        cropContainerSize = cropContainerSize,
        rawOffset = displayedContentOffset,
      )

      val animatableScale = Animatable(displayedContentScale)
      val animatableOffset = Animatable(displayedContentOffset, Offset.VectorConverter)

      clearTransformationsClampAnimations()
      // todo: animate velocity
      transformationsClampAnimationsJobs += scope.launch {
        if (displayedContentScale != clampedTransformations.scale) {
          animatableScale.animateTo(
            clampedTransformations.scale,
            animationSpec = tween(TRANSFORMATION_CLAMP_ANIMATION_DURATION_MS)
          ) { displayedContentScale = this.value }
        }
      }
      transformationsClampAnimationsJobs += scope.launch {
        if (displayedContentOffset != clampedTransformations.offset) {
          animatableOffset.animateTo(
            clampedTransformations.offset,
            animationSpec = tween(TRANSFORMATION_CLAMP_ANIMATION_DURATION_MS)
          ) { displayedContentOffset = this.value }
        }
      }

      return clampedTransformations
    }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
      clearTransformationsClampAnimations()

      displayedContentScale *= zoomChange

      val scaleSpringModifier = 3

      val scaledDisplayedContentSize = displayedContentSize * displayedContentScale

      val baseNextOffsetX = displayedContentOffset.x + panChange.x * displayedContentScale

      val leftXBound = (displayedContentSize.width * (displayedContentScale - 1)) / 2
      val rightXBound = leftXBound + cropContainerSize.width
      val isNextBaseOffsetXOutOfLeftXBound = baseNextOffsetX > leftXBound
      val isNextBaseOffsetXOutOfRightXBound =
        baseNextOffsetX + scaledDisplayedContentSize.width < rightXBound
      val shouldSpringifyX = isNextBaseOffsetXOutOfLeftXBound || isNextBaseOffsetXOutOfRightXBound
      val springifiedNextOffsetX =
        if (shouldSpringifyX) displayedContentOffset.x + panChange.x * displayedContentScale / scaleSpringModifier
        else baseNextOffsetX

      val baseNextOffsetY = displayedContentOffset.y + panChange.y * displayedContentScale
      val topYBound = (displayedContentSize.height * (displayedContentScale - 1)) / 2
      val bottomYBound = topYBound + cropContainerSize.height
      val isNextBaseOffsetYOutOfTopYBound = baseNextOffsetY > topYBound
      val isNextBaseOffsetYOutOfBottomYBound =
        baseNextOffsetY + scaledDisplayedContentSize.height < bottomYBound
      val shouldSpringifyY = isNextBaseOffsetYOutOfTopYBound || isNextBaseOffsetYOutOfBottomYBound
      val springifiedNextOffsetY =
        if (shouldSpringifyY) displayedContentOffset.y + panChange.y * displayedContentScale / scaleSpringModifier
        else baseNextOffsetY

      displayedContentOffset = Offset(x = springifiedNextOffsetX, y = springifiedNextOffsetY)
    }

    LaunchedEffect(
      minScale,
      maxScale,
      transformableState.isTransformInProgress,
    ) {
      if (transformableState.isTransformInProgress) return@LaunchedEffect
      onTransformationsDidClamp(clampTransformations())
    }

    Box(
      modifier = Modifier
        .background(Color.DarkGray)
        .fillMaxSize()
        .drawContentWithCropContainerMask(
          wrapSize = wrapSize,
          cropContainerSize = cropContainerSize,
        )
    ) {
      val cropContainerTopLeftOffset = getCropContainerTopLeftOffset(wrapSize, cropContainerSize)

      Box(
        content = {
          content(displayedContentSize.toDp(density = LocalDensity.current.density))
        },
        modifier = Modifier
          .graphicsLayer {
            scaleX = displayedContentScale
            scaleY = displayedContentScale
            translationX = cropContainerTopLeftOffset.x + displayedContentOffset.x
            translationY = cropContainerTopLeftOffset.y + displayedContentOffset.y
          }
          .conditional(isEnabled) { transformable(state = transformableState) },
      )

      Grid(
        displayedContentSize = displayedContentSize,
        cropContainerTopLeftOffset = cropContainerTopLeftOffset,
        cropContainerSize = cropContainerSize,
        displayedContentScale = displayedContentScale,
        displayedContentOffset = displayedContentOffset,
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
  displayedContentSize: AssetSize,
  cropContainerTopLeftOffset: Offset,
  cropContainerSize: AssetSize,
  displayedContentScale: Float,
  displayedContentOffset: Offset,
  isVisible: Boolean,
) {
  val density = LocalDensity.current

  val animatedAlpha = animateFloatAsState(
    if (isVisible) 1f else 0f, animationSpec = tween(
      durationMillis = 200, delayMillis = if (isVisible) 0 else 250
    )
  )

  // todo: optimize
  val modifier = run {
    val scaledDisplayedContentSize = displayedContentSize * displayedContentScale

    /** Horizontal -- START */
    val contentContainerLeftBorderX = (displayedContentSize.width * (displayedContentScale - 1)) / 2
    val wrapLeftBorderX = contentContainerLeftBorderX - cropContainerTopLeftOffset.x

    val contentContainerRightBorderX = contentContainerLeftBorderX + cropContainerSize.width
    val wrapRightBorderX = contentContainerRightBorderX + cropContainerTopLeftOffset.x

    val offsetContentLeftBorderX = displayedContentOffset.x
    val offsetContentRightBorderX = displayedContentOffset.x + scaledDisplayedContentSize.width

    val visibleContentLeftBorderX = max(wrapLeftBorderX, offsetContentLeftBorderX.toDouble())
    val visibleContentRightBorderX = min(wrapRightBorderX, offsetContentRightBorderX)
    val visibleContentWidthPx = visibleContentRightBorderX - visibleContentLeftBorderX

    val gridWidthDp = (visibleContentWidthPx / density.density).dp
    /** Horizontal -- END */

    /** Vertical -- START */
    val contentContainerTopBorderY = (displayedContentSize.height * (displayedContentScale - 1)) / 2
    val wrapTopBorderY = contentContainerTopBorderY - cropContainerTopLeftOffset.y

    val contentContainerBottomBorderY = contentContainerTopBorderY + cropContainerSize.height
    val wrapBottomBorderY = contentContainerBottomBorderY + cropContainerTopLeftOffset.y

    val offsetContentTopBorderY = displayedContentOffset.y
    val offsetContentBottomBorderY = displayedContentOffset.y + scaledDisplayedContentSize.height

    val visibleContentTopBorderY = max(wrapTopBorderY, offsetContentTopBorderY.toDouble())
    val visibleContentBottomBorderY = min(wrapBottomBorderY, offsetContentBottomBorderY)
    val visibleContentHeightPx = visibleContentBottomBorderY - visibleContentTopBorderY

    val gridHeightDp = (visibleContentHeightPx / density.density).dp
    /** Vertical -- END */

    return@run Modifier
      .width(gridWidthDp)
      .height(gridHeightDp)
      .graphicsLayer {
        translationX =
          cropContainerTopLeftOffset.x - contentContainerLeftBorderX.toFloat() + visibleContentLeftBorderX.toFloat()
        translationY =
          cropContainerTopLeftOffset.y - contentContainerTopBorderY.toFloat() + visibleContentTopBorderY.toFloat()

        alpha = animatedAlpha.value
      }
  }

  FlowRow(
    maxLines = GRID_ROWS_AMOUNT, maxItemsInEachRow = GRID_COLUMNS_AMOUNT,
    modifier = modifier,
  ) {
    repeat(GRID_CELLS_AMOUNT) { index ->
      val borderSidesToPaint: List<BorderSide> = remember {
        val isOnLeftEdge = index % GRID_COLUMNS_AMOUNT == 0
        val isOnRightEdge = (index + 1) % GRID_COLUMNS_AMOUNT == 0
        val isOnTopEdge = index < GRID_COLUMNS_AMOUNT
        val isOnBottomEdge = index >= GRID_CELLS_AMOUNT - GRID_COLUMNS_AMOUNT

        val result = BorderSide.entries.toMutableList()
        if (isOnLeftEdge) result -= BorderSide.Left
        if (isOnRightEdge) result -= BorderSide.Right
        if (isOnTopEdge) result -= BorderSide.Top
        if (isOnBottomEdge) result -= BorderSide.Bottom

        return@remember result
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
