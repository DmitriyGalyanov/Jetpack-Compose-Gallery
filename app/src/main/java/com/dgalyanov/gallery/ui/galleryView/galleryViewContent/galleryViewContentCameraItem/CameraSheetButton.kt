package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewContentCameraItem

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.camera.core.ImageCapture
import androidx.camera.video.OutputResults
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dgalyanov.gallery.R
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.ui.theme.withCoercedFontScaleForNonText
import com.dgalyanov.gallery.utils.galleryGenericLog
import com.dgalyanov.gallery.utils.openAppSettings
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraSheet(
  sheetState: SheetState,
  isImageCapturingEnabled: Boolean,
  isVideoRecordingEnabled: Boolean,
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
    BackHandler(cameraControl.isCapturingMedia) {
      // assuming Picture Capturing doesn't hang
//      if (cameraControl.isTakingPicture)
      if (cameraControl.isRecordingVideo) cameraControl.finishVideoRecording()
    }

    fun hideSheet(onCompletion: (() -> Unit)? = null) {
      scope.launch { sheetState.hide() }.invokeOnCompletion {
        onDidDismiss()
        onCompletion?.invoke()
      }
    }

    Box(
      modifier = Modifier
        // sheet's height is capped with available space
        .height(galleryViewModel.windowHeightDp.dp)
    ) {
      CameraPreviewContent(
        cameraController = cameraControl.cameraController,
        lifecycleOwner = cameraControl.lifecycleOwner,
        onDidSetScreenFlashWindow = cameraControl::onDidSetPreviewViewScreenFlashWindow,
        shouldFlashPreview = cameraControl.shouldFlashPreview,
        modifier = Modifier
          .align(Alignment.TopCenter)
          .fillMaxWidth()
          .clip(RoundedCornerShape(32F))
          .aspectRatio(9F / 16F),
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(8.dp)
          .align(Alignment.TopCenter),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        val topRowIconSize = 32.withCoercedFontScaleForNonText()
        IconButton(
          enabled = !cameraControl.isCapturingMedia,
          onClick = ::hideSheet,
        ) {
          Icon(
            contentDescription = "Bold cross",
            painter = painterResource(R.drawable.cross_bold),
            modifier = Modifier.size(topRowIconSize),
          )
        }

        IconButton(
          enabled = !cameraControl.isCapturingMedia,
          onClick = cameraControl::switchImageCaptureFlashMode,
        ) {
          val flashOffIconPainter = painterResource(R.drawable.flash_off)
          val flashOnIconPainter = painterResource(R.drawable.flash_on)
          // todo: use actual Flash_Auto Icon when it will be ready
          val flashAutoIconPainter = painterResource(R.drawable.flash_on)

          Icon(
            contentDescription = when (cameraControl.statefulImageCaptureFlashMode) {
              ImageCapture.FLASH_MODE_OFF -> "Flash is off"
              ImageCapture.FLASH_MODE_AUTO -> "Flash is in auto mode"
              ImageCapture.FLASH_MODE_ON -> "Flash is on"
              ImageCapture.FLASH_MODE_SCREEN -> "Screen would be used to enlighten the scene"
              else -> "Flash is off"
            },
            painter = when (cameraControl.statefulImageCaptureFlashMode) {
              ImageCapture.FLASH_MODE_OFF -> flashOffIconPainter
              ImageCapture.FLASH_MODE_AUTO -> flashAutoIconPainter
              ImageCapture.FLASH_MODE_ON -> flashOnIconPainter
              ImageCapture.FLASH_MODE_SCREEN -> flashOnIconPainter
              else -> flashOffIconPainter
            },
            modifier = Modifier.size(topRowIconSize),
          )
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter),
      ) {
        TextButton(
          onClick = cameraControl::switchCamera,
          enabled = !cameraControl.isCapturingMedia,
          modifier = Modifier.weight(1F)
        ) {
          Text("Switch Camera")
        }

        if (isImageCapturingEnabled) {
          TextButton(
            onClick = {
              // todo: update UI while loading
              cameraControl.takePicture { capturedImage ->
                hideSheet {
                  onDidTakePicture?.invoke(capturedImage)
                }
              }
            },
            enabled = !cameraControl.isCapturingMedia,
            modifier = Modifier.weight(1F)
          ) {
            Text("Capture Picture")
          }
        }

        if (isVideoRecordingEnabled) {
          TextButton(
            onClick = {
              // todo: update UI while loading
              if (cameraControl.isRecordingVideo) cameraControl.finishVideoRecording()
              else cameraControl.startVideoRecording { recordedVideoOutputResults ->
                hideSheet {
                  onDidRecordVideo?.invoke(recordedVideoOutputResults)
                }
              }
            },
            enabled = !cameraControl.isTakingPicture,
            modifier = Modifier.weight(1F)
          ) {
            // todo: show current recording duration
            Text(if (cameraControl.isRecordingVideo) "Stop recording" else "Record Video")
          }
        }
      }
    }
  }
}
