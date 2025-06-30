package com.dgalyanov.gallery.dataClasses

import com.dgalyanov.gallery.utils.GalleryLogFactory
import kotlin.math.abs

/**
 * Instances are named as "_<HEIGHT_UNITS>x<WIDTH_UNITS>"
 */
@Suppress("EnumEntryName")
internal enum class AssetAspectRatio(
  private val heightUnits: Double,
  private val widthUnits: Double,
) {
  _16x9(heightUnits = 16.0, widthUnits = 9.0),
  _3x2(heightUnits = 3.0, widthUnits = 2.0),
  _4x3(heightUnits = 4.0, widthUnits = 3.0),
  _5x4(heightUnits = 5.0, widthUnits = 4.0),
  _1x1(heightUnits = 1.0, widthUnits = 1.0),
  _4x5(heightUnits = 4.0, widthUnits = 5.0),
  _3x4(heightUnits = 3.0, widthUnits = 4.0),
  _2x3(heightUnits = 2.0, widthUnits = 3.0),
  _9x16(heightUnits = 9.0, widthUnits = 16.0);

  val heightToWidthKey = "${heightUnits.toInt()}x${widthUnits.toInt()}"
  private val widthToHeightKey = "${widthUnits.toInt()}x${heightUnits.toInt()}"

  val heightToWidthNumericValue = heightUnits / widthUnits
  private val widthToHeightNumericValue = widthUnits / heightUnits

  override fun toString(): String {
    return "name: $name, heightUnits: $heightUnits, widthUnits: $widthUnits, heightToWidthKey: $heightToWidthKey, widthToHeightKey: $widthToHeightKey, heightToWidthNumericValue: $heightToWidthNumericValue, widthToHeightNumericValue: $widthToHeightNumericValue"
  }

  companion object {
    private val log = GalleryLogFactory("AspectRatio")

    private fun getClosestAvailableNumber(
      value: Double,
      availableValues: List<Double>,
      shouldPreferLargerValue: Boolean = false
    ): Double? {
      log { "getClosestAvailableNumber(value: $value, availableValues: $availableValues, shouldPreferLargerValue: $shouldPreferLargerValue)" }
      var smallestAbsDelta = Double.POSITIVE_INFINITY
      var valueWithSmallestDelta: Double? = null

      for (availableValue in availableValues) {
        if (availableValue == value) return value

        val delta = value - availableValue
        val absDelta = abs(delta)
        val isLargerThanPassedValue = delta < 0

        if (
          valueWithSmallestDelta == null ||
          absDelta < smallestAbsDelta ||
          (absDelta == smallestAbsDelta && shouldPreferLargerValue && isLargerThanPassedValue)
        ) {
          smallestAbsDelta = absDelta
          valueWithSmallestDelta = availableValue
        }
      }

      log { "getClosestAvailableNumber(value: $value, availableValues: $availableValues, shouldPreferLargerValue: $shouldPreferLargerValue) | result: $valueWithSmallestDelta" }
      return valueWithSmallestDelta
    }

    private fun isAlmostEqual(num1: Double, num2: Double, epsilon: Double = 0.001): Boolean {
      log { "isAlmostEqual(num1: $num1, num2: $num2) |r: ${abs(num1 - num2)}" }

      return abs(num1 - num2) < epsilon
    }

    private val allAvailableWidthToHeightNumericValues =
      entries.map { it.heightToWidthNumericValue }

    fun getClosest(
      width: Double,
      height: Double,
      availableValues: List<Double> = allAvailableWidthToHeightNumericValues
    ): AssetAspectRatio {
      log { "getClosest(height: $height, width: $width)" }
      val closestAvailableDouble = getClosestAvailableNumber(
        value = width / height,
        availableValues = availableValues
      ) ?: 1.0
      return entries.first { isAlmostEqual(closestAvailableDouble, it.heightToWidthNumericValue) }
    }
  }
}