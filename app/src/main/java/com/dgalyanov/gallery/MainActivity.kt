package com.dgalyanov.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.dgalyanov.gallery.galleryContentResolver.GalleryContentResolver
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.ui.galleryView.GalleryViewProvider
import com.dgalyanov.gallery.ui.theme.GalleryTheme
import com.dgalyanov.gallery.utils.GalleryLogFactory
import com.dgalyanov.gallery.utils.PerformanceClass

private val log = GalleryLogFactory("MainActivity")

class MainActivity : ComponentActivity() {
  // todo: should use viewModelStore?
  private lateinit var galleryViewModel: GalleryViewModel
  private val ensuredGalleryViewModel
    get(): GalleryViewModel {
      if (!::galleryViewModel.isInitialized) {
        galleryViewModel = GalleryViewModel(applicationContext)
      }
      return galleryViewModel
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    log { "onCreate" }

    PerformanceClass.determinePerformanceClass(this)

    GalleryContentResolver.init(this.contentResolver)

    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(
        android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b),
      ),
    )

    setContent {
      ensuredGalleryViewModel.UseWindowMetricsSetterEffect()
      // force dark theme
      GalleryTheme(darkTheme = true) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPaddings ->
          LaunchedEffect(innerPaddings) {
            ensuredGalleryViewModel.updateInnerStaticPaddings(innerPaddings)
          }

          GalleryViewProvider(ensuredGalleryViewModel)

          EmittedSelectionView(ensuredGalleryViewModel)
        }
      }
    }
  }
}
