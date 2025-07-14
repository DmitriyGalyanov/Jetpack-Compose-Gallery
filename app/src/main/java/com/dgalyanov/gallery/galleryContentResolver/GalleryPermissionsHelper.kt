package com.dgalyanov.gallery.galleryContentResolver

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.dgalyanov.gallery.utils.GalleryLogFactory
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale

// todo: rename to PermissionsHelper
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

  @OptIn(ExperimentalPermissionsApi::class)
  @Composable
  fun useMediaAccessPermissionsState(
    onPermissionsResult: (Map<String, Boolean>) -> Unit = { },
    onGranted: () -> Unit,
  ): MultiplePermissionsState {
    val logTag = "useMediaAccessPermissionsState"

    val permissionsState =
      rememberMultiplePermissionsState(requiredPermissions) {
        log { "$logTag.onPermissionResult($it)" }
        onPermissionsResult(it)
      }
    log { "$logTag.permissionsState: ${permissionsState.permissions.map { "{ permission: ${it.permission}, status: { isGranted: ${it.status.isGranted}, shouldShowRationale: ${it.status.shouldShowRationale} }}" }}" }

    LaunchedEffect(permissionsState.allPermissionsGranted) {
      if (!permissionsState.allPermissionsGranted) {
        permissionsState.launchMultiplePermissionRequest()
        return@LaunchedEffect
      }
      onGranted()
    }

    return permissionsState
  }
}
