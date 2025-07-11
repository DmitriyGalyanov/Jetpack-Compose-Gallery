package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewContentCameraItem

import androidx.activity.compose.LocalActivity
import androidx.camera.core.ImageCapture
import androidx.camera.video.OutputResults
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewContentCameraItem.ui.CameraCaptureButton
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewContentCameraItem.ui.CameraSheet
import com.dgalyanov.gallery.utils.galleryGenericLog
import com.dgalyanov.gallery.utils.openAppSettings
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun CameraSheetButton(
  modifier: Modifier = Modifier,
  labelModifier: Modifier = Modifier,
  onSheetGoingToDisplay: (() -> Unit)? = null,
  onSheetDidDismiss: (() -> Unit)? = null,
  enabled: Boolean = true,
  isImageCapturingEnabled: Boolean,
  isVideoRecordingEnabled: Boolean,
  /**
   * called after picture is added to MediaStore and Sheet is dismissed
   */
  onDidTakePicture: ((capturedImage: ImageCapture.OutputFileResults) -> Unit)? = null,
  /**
   * called after Video is added to MediaStore and Sheet is dismissed
   */
  onDidRecordVideo: ((recordedVideoOutputResults: OutputResults) -> Unit)? = null,
  label: @Composable BoxScope.(modifier: Modifier) -> Unit = {
    // todo: add Icon
    Text("Camera", modifier = it)
  },
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var isSheetDisplayed by remember { mutableStateOf(false) }
  fun displaySheet() {
    isSheetDisplayed = true
    onSheetGoingToDisplay?.invoke()
  }

  val cameraPermissionsState = rememberMultiplePermissionsState(
    listOf(
      android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO
    )
  ) {
    galleryGenericLog { "after permissions request $it" }
    if (it.values.all { isGranted -> isGranted }) displaySheet()
  }

  val activity = LocalActivity.current

  Box(
    modifier = modifier
      .alpha(if (enabled) 1f else 0.6f)
      .clickable(enabled = enabled) {
        if (cameraPermissionsState.allPermissionsGranted) {
          displaySheet()
        } else if (!cameraPermissionsState.shouldShowRationale) {
          cameraPermissionsState.launchMultiplePermissionRequest()
        } else if (activity != null) {
          openAppSettings(activity)
        }
      },
    contentAlignment = Alignment.Center,
  ) {
    label(labelModifier)

    if (isSheetDisplayed) {
      CameraSheet(
        sheetState = sheetState,
        isImageCapturingEnabled = isImageCapturingEnabled,
        isVideoRecordingEnabled = isVideoRecordingEnabled,
        onDidTakePicture = onDidTakePicture,
        onDidRecordVideo = onDidRecordVideo,
      ) {
        isSheetDisplayed = false
        onSheetDidDismiss?.invoke()
      }
    }
  }
}
