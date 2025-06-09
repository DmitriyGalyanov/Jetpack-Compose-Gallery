package com.dgalyanov.gallery.ui.galleryView

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dgalyanov.gallery.galleryContentResolver.GalleryPermissionsHelper

@Composable
fun EmptyGalleryView() {
  val arePermissionsGranted = GalleryPermissionsHelper.arePermissionsGranted.collectAsState().value
  val didUserForbidPermissionsRequest =
    GalleryPermissionsHelper.didUserForbidPermissionsRequest.collectAsState().value

  Box(
    modifier = Modifier
      .fillMaxSize()
  ) {
    if (arePermissionsGranted) {
      Text("Gallery is empty", modifier = Modifier.align(Alignment.Center))
    } else {
      Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
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