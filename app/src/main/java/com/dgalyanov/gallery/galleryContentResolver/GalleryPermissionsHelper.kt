package com.dgalyanov.gallery.galleryContentResolver

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import com.dgalyanov.gallery.utils.GalleryLogFactory
import com.dgalyanov.gallery.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object GalleryPermissionsHelper {
  private val log = GalleryLogFactory("GalleryPermissionsHelper")

  private val requiredPermissions = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
    listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
  } else {
    listOf(
      Manifest.permission.READ_MEDIA_IMAGES,
      Manifest.permission.READ_MEDIA_VIDEO,
    )
  }

  private val _arePermissionsGranted = MutableStateFlow(false)
  val arePermissionsGranted = _arePermissionsGranted.asStateFlow()

  private val _didUserForbidPermissionsRequest = MutableStateFlow(false)
  val didUserForbidPermissionsRequest = _didUserForbidPermissionsRequest.asStateFlow()

  private lateinit var mainActivity: MainActivity
  fun init(mainActivity: MainActivity): GalleryPermissionsHelper {
    if (::mainActivity.isInitialized) return this

    log { "init" }

    this.mainActivity = mainActivity

    _arePermissionsGranted.value = checkIfPermissionsAreGranted()

    return this
  }

  fun checkIfPermissionsAreGranted(): Boolean {
    val logTag = "checkIfPermissionsAreGranted"
    log { logTag }

    requiredPermissions.forEach {
      if (
        mainActivity.applicationContext.checkSelfPermission(it)
        != PackageManager.PERMISSION_GRANTED
      ) {
        log { "$logTag | $it is not granted" }
        _arePermissionsGranted.value = false
        return _arePermissionsGranted.value
      }
    }

    log { "$logTag | permissions are granted" }
    _arePermissionsGranted.value = true
    return _arePermissionsGranted.value
  }

  private const val READ_STORAGE_REQUEST_CODE = 0

  private fun requestPermissions() {
    log { "requestPermissions" }
    mainActivity.requestPermissions(requiredPermissions.toTypedArray(), READ_STORAGE_REQUEST_CODE)
  }

  fun requestPermissionsIfNeeded() {
    if (checkIfPermissionsAreGranted()) return

    requestPermissions()
  }

  fun openAppSettings() = com.dgalyanov.gallery.utils.openAppSettings(mainActivity)

  fun onRequestPermissionsResult(
    requestCode: Int,
    grantResults: IntArray
  ): Boolean {
    log { "onRequestPermissionsResult(requestCode: $requestCode, grantResults: $grantResults) | readStorageRequestCode: $READ_STORAGE_REQUEST_CODE" }

    if (requestCode == READ_STORAGE_REQUEST_CODE) {
      if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        _arePermissionsGranted.value = true
        return _arePermissionsGranted.value
      } else {
        requiredPermissions.forEach {
          if (!shouldShowRequestPermissionRationale(mainActivity, it)) {
            _didUserForbidPermissionsRequest.value = true
            return false
          }
        }
      }
    }

    return checkIfPermissionsAreGranted()
  }
}
