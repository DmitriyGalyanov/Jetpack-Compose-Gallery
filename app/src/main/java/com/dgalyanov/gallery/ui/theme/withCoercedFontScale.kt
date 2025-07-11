package com.dgalyanov.gallery.ui.theme

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal const val DEFAULT_FONT_SIZE = 14

private val DEFAULT_MIN_FONT_SCALE = null
private const val DEFAULT_MAX_FONT_SCALE = 1.3f

/**
 * Applies coerced [userPreferredFontScale] [FontScale][Density.fontScale]
 */
internal fun Int.withCoercedFontScale(
  userPreferredFontScale: Float,
  minFontScale: Float? = DEFAULT_MIN_FONT_SCALE,
  maxFontScale: Float? = DEFAULT_MAX_FONT_SCALE,
  isForText: Boolean,
): Float {
  val fontScaleToApply = userPreferredFontScale.coerceIn(minFontScale, maxFontScale)

  /**
   * dividing by [userPreferredFontScale] since [Text] applies it internally
   */
  return this / (if (isForText) userPreferredFontScale else 1f) * fontScaleToApply
}

/**
 * Applies coerced [userPreferredFontScale] [FontScale][Density.fontScale]
 */
internal fun Float.withCoercedFontScale(
  userPreferredFontScale: Float,
  minFontScale: Float? = DEFAULT_MIN_FONT_SCALE,
  maxFontScale: Float? = DEFAULT_MAX_FONT_SCALE,
  isForText: Boolean,
): Float {
  val fontScaleToApply = userPreferredFontScale.coerceIn(minFontScale, maxFontScale)

  /**
   * dividing by [userPreferredFontScale] since [Text] applies it internally
   */
  return this / (if (isForText) userPreferredFontScale else 1f) * fontScaleToApply
}


/**
 * Applies coerced [User Preferred][LocalDensity] [FontScale][Density.fontScale]
 */
@Composable
internal fun Int.withCoercedFontScaleForText(
  minFontScale: Float? = DEFAULT_MIN_FONT_SCALE,
  maxFontScale: Float? = DEFAULT_MAX_FONT_SCALE,
): TextUnit {
  val userPreferredFontScale = LocalDensity.current.fontScale

  return remember(userPreferredFontScale, minFontScale, maxFontScale) {
    this.withCoercedFontScale(
      userPreferredFontScale = userPreferredFontScale,
      minFontScale = minFontScale,
      maxFontScale = maxFontScale,
      isForText = true,
    ).sp
  }
}

/**
 * Applies coerced [User Preferred][LocalDensity] [FontScale][Density.fontScale]
 */
@Composable
internal fun Float.withCoercedFontScaleForText(
  minFontScale: Float? = DEFAULT_MIN_FONT_SCALE,
  maxFontScale: Float? = DEFAULT_MAX_FONT_SCALE,
): TextUnit {
  val userPreferredFontScale = LocalDensity.current.fontScale

  return remember(userPreferredFontScale, minFontScale, maxFontScale) {
    this.withCoercedFontScale(
      userPreferredFontScale = userPreferredFontScale,
      minFontScale = minFontScale,
      maxFontScale = maxFontScale,
      isForText = true,
    ).sp
  }
}


/**
 * Applies coerced [User Preferred][LocalDensity] [FontScale][Density.fontScale]
 */
@Composable
internal fun Int.withCoercedFontScaleForNonText(
  minFontScale: Float? = DEFAULT_MIN_FONT_SCALE,
  maxFontScale: Float? = DEFAULT_MAX_FONT_SCALE,
): Dp {
  val userPreferredFontScale = LocalDensity.current.fontScale

  return remember(userPreferredFontScale, minFontScale, maxFontScale) {
    this.withCoercedFontScale(
      userPreferredFontScale = userPreferredFontScale,
      minFontScale = minFontScale,
      maxFontScale = maxFontScale,
      isForText = false,
    ).dp
  }
}

/**
 * Applies coerced [User Preferred][LocalDensity] [FontScale][Density.fontScale]
 */
@Composable
internal fun Float.withCoercedFontScaleForNonText(
  minFontScale: Float? = DEFAULT_MIN_FONT_SCALE,
  maxFontScale: Float? = DEFAULT_MAX_FONT_SCALE,
): Dp {
  val userPreferredFontScale = LocalDensity.current.fontScale

  return remember(userPreferredFontScale, minFontScale, maxFontScale) {
    this.withCoercedFontScale(
      userPreferredFontScale = userPreferredFontScale,
      minFontScale = minFontScale,
      maxFontScale = maxFontScale,
      isForText = false,
    ).dp
  }
}