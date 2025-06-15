package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewContentCameraItem

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dgalyanov.gallery.R
import com.dgalyanov.gallery.utils.GalleryLogFactory
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

/** todo: enable Volume down button receiver used to trigger shutter */
// https://github.com/android/camera-samples/blob/main/CameraXBasic/app/src/main/java/com/android/example/cameraxbasic/fragments/CameraFragment.kt
//private val volumeDownReceiver = object : BroadcastReceiver() {
//  override fun onReceive(context: Context, intent: Intent) {
//    when (intent.getIntExtra(KEY_EVENT_EXTRA, KeyEvent.KEYCODE_UNKNOWN)) {
//      // When the volume down button is pressed, simulate a shutter button click
//      KeyEvent.KEYCODE_VOLUME_DOWN -> {
//        cameraUiContainerBinding?.cameraCaptureButton?.simulateClick()
//      }
//    }
//  }
//}

internal class CameraControl(
  val cameraController: LifecycleCameraController,
  val lifecycleOwner: LifecycleOwner,
  val switchCamera: () -> Unit,
  val takePicture: (onImageSaved: (outputFileResults: ImageCapture.OutputFileResults) -> Unit) -> Unit,
)

private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

// todo: refactor to a class
@Composable
internal fun useCameraControl(onDispose: () -> Unit): CameraControl {
  val log = remember { GalleryLogFactory("UseCameraControl") }

  val context = LocalContext.current

  val cameraController = remember {
    log("creating cameraController")

    LifecycleCameraController(context).apply {
      setEnabledUseCases(
        CameraController.IMAGE_CAPTURE
//          or
//          todo: enable VIDEO_CAPTURE when going to take a Video as Docs suggest
//          CameraController.VIDEO_CAPTURE,
      )
    }
  }

  fun switchCamera() {
    log("switchCamera()")

    cameraController.cameraSelector = when (cameraController.cameraSelector) {
      CameraSelector.DEFAULT_BACK_CAMERA -> CameraSelector.DEFAULT_FRONT_CAMERA
      CameraSelector.DEFAULT_FRONT_CAMERA -> CameraSelector.DEFAULT_BACK_CAMERA
      else -> CameraSelector.DEFAULT_BACK_CAMERA
    }
  }

  val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
  DisposableEffect(Unit) {
    onDispose {
      log("disposing")

      cameraExecutor.shutdown()
      onDispose()
    }
  }

//  val resources = LocalResources.current
  val resources = LocalContext.current.resources
  fun takePicture(onImageSavedCallback: (outputFileResults: ImageCapture.OutputFileResults) -> Unit) {
    val name = SimpleDateFormat(FILENAME_FORMAT, Locale.ENGLISH).format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
      put(MediaStore.MediaColumns.DISPLAY_NAME, name)
      put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
        val appName = resources.getString(R.string.app_name)
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$appName")
      }
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
      context.contentResolver,
      MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
      contentValues,
    ).build()

    val logTag = "takePicture()"
    val logDetails = "name: $name, contentValues: $contentValues, outputOptions: $outputOptions"
    log("$logTag | $logDetails")

    cameraController.takePicture(
      outputOptions,
      cameraExecutor,
      object : ImageCapture.OnImageSavedCallback {
        override fun onCaptureStarted() {
          log("$logTag | onCaptureStarted() | $logDetails")
          super.onCaptureStarted()
        }

        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
          log("$logTag | onImageSaved(outputFileResults.savedUri: ${outputFileResults.savedUri}) | $logDetails")
          onImageSavedCallback(outputFileResults)
        }

        override fun onError(exception: ImageCaptureException) {
          log("$logTag | onError(exception: $exception) | $logDetails")
        }
      }
    )
  }

  // todo
//  fun recordVideo() {}

  return CameraControl(
    cameraController = cameraController,
    lifecycleOwner = LocalLifecycleOwner.current,
    switchCamera = ::switchCamera,
    takePicture = ::takePicture,
  )
}
