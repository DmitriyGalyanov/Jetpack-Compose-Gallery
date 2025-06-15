package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewContentCameraItem

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dgalyanov.gallery.GalleryViewModel
import com.dgalyanov.gallery.ui.styleConsts.GalleryStyleConsts
import com.dgalyanov.gallery.utils.galleryGenericLog
import com.dgalyanov.gallery.utils.openAppSettings
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun CameraSheetButton(modifier: Modifier = Modifier) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var isSheetDisplayed by remember { mutableStateOf(false) }

  val cameraPermissionsState = rememberMultiplePermissionsState(
    listOf(
      android.Manifest.permission.CAMERA,
      android.Manifest.permission.RECORD_AUDIO
    )
  ) {
    galleryGenericLog("after permissions request $it")
    if (it.values.all { isGranted -> isGranted }) isSheetDisplayed = true
  }

  val activity = LocalActivity.current

  Box(
    modifier = modifier
      .background(Color.Black)
      .clickable {
        if (cameraPermissionsState.allPermissionsGranted) {
          isSheetDisplayed = true
        } else if (!cameraPermissionsState.shouldShowRationale) {
          cameraPermissionsState.launchMultiplePermissionRequest()
        } else if (activity != null) {
          openAppSettings(activity)
        }
      },
    contentAlignment = Alignment.Center
  ) {
    // todo: add Icon
    Text("Camera")

    if (isSheetDisplayed) {
      CameraSheet(sheetState = sheetState) {
        isSheetDisplayed = false
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraSheet(sheetState: SheetState, onDidDismiss: () -> Unit) {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current

  val cameraControl = useCameraControl(
    onDispose = galleryViewModel::getSelectedAlbumMediaFiles
  )

  val scope = rememberCoroutineScope()

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
            cameraControl.takePicture { capturedImage ->
              scope.launch { sheetState.hide() }
                .invokeOnCompletion {
                  onDidDismiss()
                  galleryViewModel.emitCapturedImage(capturedImage)
                }
            }
          },
          enabled = !cameraControl.isRecording,
          modifier = Modifier.weight(1F)
        ) {
          Text("Capture Picture")
        }

        TextButton(
          {
            if (!cameraControl.isRecording) cameraControl.startVideoRecording { recordedVideoOutputResults ->
              scope.launch { sheetState.hide() }
                .invokeOnCompletion {
                  onDidDismiss()
                  galleryViewModel.emitRecordedVideo(recordedVideoOutputResults)
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
