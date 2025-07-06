package com.dgalyanov.gallery.ui.galleryView

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dgalyanov.gallery.galleryContentResolver.GalleryPermissionsHelper
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewContentCameraItem.CameraSheetButton

@Composable
internal fun EmptyGalleryView() {
  val arePermissionsGranted = GalleryPermissionsHelper.arePermissionsGranted.collectAsState().value
  val didUserForbidPermissionsRequest =
    GalleryPermissionsHelper.didUserForbidPermissionsRequest.collectAsState().value

  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current
  val isLoading = galleryViewModel.isFetchingAllAssets

  Box(modifier = Modifier.fillMaxSize()) {
    if (arePermissionsGranted) {
      if (isLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
      else {
        CameraSheetButton(
          modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(4.dp)),
          isImageCapturingEnabled = true,
          isVideoRecordingEnabled = true,
          onDidTakePicture = {
            galleryViewModel.populateAllAssetsMap()
          },
          onDidRecordVideo = {
            galleryViewModel.populateAllAssetsMap()
          },
        ) {
          Text(
            "Gallery is empty, but you could use camera (click!) to fix that!",
            modifier = Modifier.align(Alignment.Center),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            textDecoration = TextDecoration.Underline,
          )
        }
      }
    } else {
      Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text("Permissions are not granted")

        Spacer(Modifier.size(8.dp))

        if (didUserForbidPermissionsRequest) {
          Button(GalleryPermissionsHelper::openAppSettings) {
            Text("Open App Settings")
          }
        } else {
          Button(GalleryPermissionsHelper::requestPermissionsIfNeeded) {
            Text("Grant Permissions")
          }
        }
      }
    }
  }
}