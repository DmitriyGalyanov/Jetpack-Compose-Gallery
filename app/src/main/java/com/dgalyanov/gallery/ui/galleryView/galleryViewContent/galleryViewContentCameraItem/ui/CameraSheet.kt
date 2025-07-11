package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewContentCameraItem.ui

import androidx.activity.compose.BackHandler
import androidx.camera.core.ImageCapture
import androidx.camera.video.OutputResults
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dgalyanov.gallery.R
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewContentCameraItem.CameraControl
import com.dgalyanov.gallery.ui.theme.withCoercedFontScaleForNonText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CameraSheet(
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
          .aspectRatio(9F / 16F),
      ) {
        CameraCaptureButton(
          cameraControl = cameraControl,
          onPressEnd = {
            if (!isImageCapturingEnabled || cameraControl.isCapturingMedia) return@CameraCaptureButton
            // todo: update UI while processing captured image
            cameraControl.takePicture { capturedImage ->
              hideSheet {
                onDidTakePicture?.invoke(capturedImage)
              }
            }
          },
          onLongPressStart = {
            if (!isVideoRecordingEnabled || cameraControl.isTakingPicture) return@CameraCaptureButton
            cameraControl.startVideoRecording { recordedVideoOutputResults ->
              hideSheet {
                onDidRecordVideo?.invoke(recordedVideoOutputResults)
              }
            }
          },
          onLongPressEnd = {
            // todo: update UI while processing captured video
            cameraControl.finishVideoRecording()
          },
        )
      }

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
            contentDescription = "Bold cross (Close Camera)",
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
          .align(Alignment.BottomCenter)
          .padding(horizontal = 8.dp),
      ) {
        IconButton(
          enabled = !cameraControl.isCapturingMedia,
          onClick = cameraControl::switchCamera,
        ) {
          val padding = 8
          Icon(
            contentDescription = "Switch Camera",
            painter = painterResource(R.drawable.switch_icon),
            modifier = Modifier
              .clip(CircleShape)
              .size((24 + (padding * 2)).withCoercedFontScaleForNonText())
              .background(Color(0x21, 0x21, 0x21))
              .padding(padding.withCoercedFontScaleForNonText())
          )
        }
      }
    }
  }
}