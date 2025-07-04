package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewContentCameraItem

import androidx.activity.compose.LocalActivity
import androidx.camera.core.ImageCapture
import androidx.camera.video.OutputResults
import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.utils.galleryGenericLog
import com.dgalyanov.gallery.utils.openAppSettings
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun CameraSheetButton(
  modifier: Modifier = Modifier,
  labelModifier: Modifier = Modifier,
  onSheetGoingToDisplay: (() -> Unit)? = null,
  onSheetDidDismiss: (() -> Unit)? = null,
  enabled: Boolean = true,
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
  }
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
      }, contentAlignment = Alignment.Center
  ) {
    label(labelModifier)

    if (isSheetDisplayed) {
      CameraSheet(
        sheetState = sheetState,
        onDidTakePicture = onDidTakePicture,
        onDidRecordVideo = onDidRecordVideo,
      ) {
        isSheetDisplayed = false
        onSheetDidDismiss?.invoke()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraSheet(
  sheetState: SheetState,
  onDidTakePicture: ((capturedImage: ImageCapture.OutputFileResults) -> Unit)?,
  onDidRecordVideo: ((recordedVideoOutputResults: OutputResults) -> Unit)?,
  onDidDismiss: () -> Unit,
) {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current

  val scope = rememberCoroutineScope()
  val cameraControl = CameraControl.use()

  ModalBottomSheet(
    sheetState = sheetState,
    onDismissRequest = onDidDismiss,
    containerColor = Color.Black,
    sheetGesturesEnabled = false,
    // partially fixes incorrect corners on PreviewView with implementationMode.PERFORMANCE
    shape = RoundedCornerShape(0.dp),
    dragHandle = null,
  ) {
    Box(
      modifier = Modifier
        // sheet's height is capped with available space
        .height(galleryViewModel.windowHeightDp.dp)
    ) {
      CameraPreviewContent(
        cameraController = cameraControl.cameraController,
        lifecycleOwner = cameraControl.lifecycleOwner,
        modifier = Modifier
          .align(Alignment.TopCenter)
          .fillMaxWidth()
          .clip(RoundedCornerShape(32F))
          .aspectRatio(9F / 16F),
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter),
      ) {
        TextButton(
          onClick = cameraControl::switchCamera,
          enabled = !cameraControl.isRecording,
          modifier = Modifier.weight(1F)
        ) {
          Text("Switch Camera")
        }

        TextButton(
          onClick = {
            // todo: update UI while loading
            cameraControl.takePicture { capturedImage ->
              scope.launch { sheetState.hide() }.invokeOnCompletion {
                onDidDismiss()
                onDidTakePicture?.invoke(capturedImage)
              }
            }
          }, enabled = !cameraControl.isRecording, modifier = Modifier.weight(1F)
        ) {
          Text("Capture Picture")
        }

        TextButton(
          {
            // todo: update UI while loading
            if (!cameraControl.isRecording) cameraControl.startVideoRecording { recordedVideoOutputResults ->
              scope.launch { sheetState.hide() }.invokeOnCompletion {
                onDidDismiss()
                onDidRecordVideo?.invoke(recordedVideoOutputResults)
              }
            } else cameraControl.finishVideoRecording()
          },
          modifier = Modifier.weight(1F),
        ) {
          // todo: show current recording duration
          Text(if (cameraControl.isRecording) "Stop recording" else "Record Video")
        }
      }
    }
  }
}
