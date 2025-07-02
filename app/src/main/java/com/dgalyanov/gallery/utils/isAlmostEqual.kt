package com.dgalyanov.gallery.utils

import kotlin.math.abs

internal fun isAlmostEqual(num1: Double, num2: Double, epsilon: Double = 0.001): Boolean =
  abs(num1 - num2) < epsilon