package com.dgalyanov.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.dgalyanov.gallery.galleryContentResolver.GalleryContentResolver
import com.dgalyanov.gallery.galleryContentResolver.GalleryPermissionsHelper
import com.dgalyanov.gallery.ui.galleryView.GalleryView
import com.dgalyanov.gallery.ui.theme.GalleryTheme
import com.dgalyanov.gallery.utils.GalleryLogFactory

private val log = GalleryLogFactory("MainActivity")

class MainActivity : ComponentActivity() {
  private val galleryViewModel = GalleryViewModel()

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray,
    deviceId: Int
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
    log("onRequestPermissionsResult(requestCode: $requestCode, permissions: $permissions, grantResults: $grantResults, deviceId: $deviceId)")

    if (GalleryPermissionsHelper.onRequestPermissionsResult(
        requestCode,
        grantResults
      )
    ) {
      galleryViewModel.populateMediaFilesList()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    log("onCreate")

    GalleryContentResolver.init(this)

    GalleryPermissionsHelper.init(this).requestPermissionsIfNeeded()
    if (GalleryPermissionsHelper.arePermissionsGranted.value) {
      galleryViewModel.populateMediaFilesList()
    }

    enableEdgeToEdge()

    setContent {
      CompositionLocalProvider(
        GalleryViewModel.LocalGalleryViewModel provides galleryViewModel
      ) {
        GalleryTheme {
          Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
              GalleryView(galleryViewModel.mediaFilesList)
            }
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    log("onResume")

    if (!GalleryPermissionsHelper.arePermissionsGranted.value
      && GalleryPermissionsHelper.checkIfPermissionsAreGranted()
    ) {
      galleryViewModel.populateMediaFilesList()
    }
  }
}
