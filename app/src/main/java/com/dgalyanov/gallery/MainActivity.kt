package com.dgalyanov.gallery

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.dgalyanov.gallery.galleryContentResolver.GalleryContentResolver
import com.dgalyanov.gallery.galleryContentResolver.GalleryPermissionsHelper
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.ui.galleryView.GalleryViewProvider
import com.dgalyanov.gallery.ui.theme.GalleryTheme
import com.dgalyanov.gallery.utils.GalleryLogFactory
import com.dgalyanov.gallery.utils.PerformanceClass

private val log = GalleryLogFactory("MainActivity")

class MainActivity : ComponentActivity() {
  private val displayMetrics: DisplayMetrics = DisplayMetrics()

  /** don't call before onCreate */
  private fun updateStoredDisplayMetrics() {
    windowManager.defaultDisplay.getMetrics(displayMetrics)
  }

  private lateinit var galleryViewModel: GalleryViewModel
  private val ensuredGalleryViewModel
    get(): GalleryViewModel {
      if (!::galleryViewModel.isInitialized) {
        galleryViewModel = GalleryViewModel(applicationContext)
      }
      return galleryViewModel
    }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray,
    deviceId: Int,
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
    log { "onRequestPermissionsResult(requestCode: $requestCode, permissions: $permissions, grantResults: $grantResults, deviceId: $deviceId)" }

    if (GalleryPermissionsHelper.onRequestPermissionsResult(requestCode, grantResults)) {
      ensuredGalleryViewModel.populateAllAssetsMap()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    log { "onCreate" }

    PerformanceClass.determinePerformanceClass(this)

    updateStoredDisplayMetrics()
    ensuredGalleryViewModel.updateWindowMetrics(
      density = displayMetrics.density,
      width = displayMetrics.widthPixels,
      height = displayMetrics.heightPixels,
    )

    GalleryContentResolver.init(this)

    GalleryPermissionsHelper.init(this).requestPermissionsIfNeeded()
    if (GalleryPermissionsHelper.arePermissionsGranted.value) {
      ensuredGalleryViewModel.populateAllAssetsMap()
    }

    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(
        android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b),
      ),
    )

    setContent {
      // force dark theme
      GalleryTheme(darkTheme = true) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPaddings ->
          LaunchedEffect(innerPaddings) {
            ensuredGalleryViewModel.updateInnerPaddings(innerPaddings)
          }

          GalleryViewProvider(ensuredGalleryViewModel)

          EmittedSelectionView(ensuredGalleryViewModel)
        }
      }
    }
  }

  override fun onWindowAttributesChanged(params: WindowManager.LayoutParams?) {
    super.onWindowAttributesChanged(params)
    log { "onWindowAttributesChanged(params: $params)" }

    updateStoredDisplayMetrics()
    ensuredGalleryViewModel.updateWindowMetrics(
      density = displayMetrics.density,
      width = displayMetrics.widthPixels,
      height = displayMetrics.heightPixels
    )
  }

  override fun onResume() {
    super.onResume()
    log { "onResume" }

    if (GalleryPermissionsHelper.checkIfPermissionsAreGranted()) {
      ensuredGalleryViewModel.populateAllAssetsMap()
    }
  }
}
