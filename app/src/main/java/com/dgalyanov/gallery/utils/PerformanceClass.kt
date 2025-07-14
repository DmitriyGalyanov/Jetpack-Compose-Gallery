package com.dgalyanov.gallery.utils

import android.content.Context
import android.os.Build
import androidx.core.performance.DefaultDevicePerformance
import androidx.core.performance.play.services.PlayServicesDevicePerformance

internal object PerformanceClass {
  private val log = GalleryLogFactory("PerformanceClass")

  /**
   * - -1 if not determined yet ([determinePerformanceClass] hasn't been called)
   *
   * - 1 on slowest Devices or Devices which PerformanceClass couldn't be determined
   * - 2 on medium performing Devices
   * - 3 on well performing Devices
   * - 4 on greatly performing Devices
   */
  var performanceClass: Int = -1
    private set

  /**
   * performanceClass couldn't change while Device is booted, so it must be determined exactly once
   */
  fun determinePerformanceClass(context: Context) {
    if (performanceClass != -1) return

    val deviceReportedPerformance = DefaultDevicePerformance()
    val playServicesReportedPerformance = PlayServicesDevicePerformance(context)

    val rawPerformanceClass = if (deviceReportedPerformance.mediaPerformanceClass > 0) {
      deviceReportedPerformance.mediaPerformanceClass
    } else playServicesReportedPerformance.mediaPerformanceClass
    performanceClass = when {
      (rawPerformanceClass >= Build.VERSION_CODES.VANILLA_ICE_CREAM) -> 4
      (rawPerformanceClass >= Build.VERSION_CODES.TIRAMISU) -> 3
      (rawPerformanceClass >= Build.VERSION_CODES.S) -> 2
      else -> 1
    }

    log {
      "determinePerformanceClass | " +
      "deviceReportedPerformance: ${deviceReportedPerformance.mediaPerformanceClass} " +
      "playServicesReportedPerformance: ${playServicesReportedPerformance.mediaPerformanceClass} " +
      "performanceClass: $performanceClass"
    }
  }
}