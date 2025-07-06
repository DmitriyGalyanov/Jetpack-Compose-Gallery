package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewContentCameraItem

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.MirrorMode
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.OutputResults
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dgalyanov.gallery.utils.GalleryLogFactory
import com.dgalyanov.gallery.utils.MediaMetadataHelper
import com.dgalyanov.gallery.utils.postToMainThread
import java.util.concurrent.ExecutorService
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
  private val context: Context,
  private val cameraExecutor: ExecutorService,
  val lifecycleOwner: LifecycleOwner,
) {
  companion object {
    @Composable
    internal fun use(onDispose: (() -> Unit)? = null): CameraControl {
      val context = LocalContext.current
      val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
      val lifecycleOwner = LocalLifecycleOwner.current

      val cameraControl = remember {
        CameraControl(
          context = context,
          cameraExecutor = cameraExecutor,
          lifecycleOwner = lifecycleOwner,
        )
      }

      DisposableEffect(Unit) {
        onDispose {
          cameraControl.onDispose()
          onDispose?.invoke()
        }
      }

      return cameraControl
    }
  }

  private val log = GalleryLogFactory("CameraControl")

  private fun showToast(message: String) =
    postToMainThread { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }

  private fun checkIfPermissionsAreGranted(): Boolean =
    listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO).all {
      ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

  private val videoQualitySelector = QualitySelector.fromOrderedList(
    listOf(Quality.FHD, Quality.HD, Quality.SD),
    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
  )

  /**
   * todo: could be used with for more control (like flipping cameras while recording)
   * @see ProcessCameraProvider.getInstance
   * see https://ngengesenior.medium.com/seamlessly-switching-camera-lenses-during-video-recording-with-camerax-on-android-fcb597ed8236
   */
//  private val videoRecorder = Recorder.Builder()
//    .setExecutor(cameraExecutor)
//    .setQualitySelector(videoQualitySelector)
//    .build()
//  private val videoCapture = VideoCapture.Builder(videoRecorder)
//    .setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
//    .build()

  private val defaultCameraUseCases = CameraController.IMAGE_CAPTURE

  val cameraController = LifecycleCameraController(context).apply {
    setEnabledUseCases(defaultCameraUseCases)
    videoCaptureQualitySelector = videoQualitySelector
    videoCaptureMirrorMode = MirrorMode.MIRROR_MODE_ON_FRONT_ONLY
  }

  fun switchCamera() {
    log { "switchCamera()" }

    cameraController.cameraSelector = when (cameraController.cameraSelector) {
      CameraSelector.DEFAULT_BACK_CAMERA -> CameraSelector.DEFAULT_FRONT_CAMERA
      CameraSelector.DEFAULT_FRONT_CAMERA -> CameraSelector.DEFAULT_BACK_CAMERA
      else -> CameraSelector.DEFAULT_BACK_CAMERA
    }
  }

  // todo: see this https://developer.android.com/media/camera/camerax/take-photo/options
  fun takePicture(
    onImageSavedCallback: (outputFileResults: ImageCapture.OutputFileResults) -> Unit,
  ) {
    if (!checkIfPermissionsAreGranted()) {
      return log { "useCameraControl.takePicture called w/o Permissions" }
    }

    val contentValues = MediaMetadataHelper.createContentValuesForPicture()

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
      context.contentResolver,
      MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
      contentValues,
    ).build()

    val logTag = "takePicture()"
    val logDetails =
      "name: ${contentValues.get(MediaStore.MediaColumns.DISPLAY_NAME)}, contentValues: $contentValues, outputOptions: $outputOptions"
    log { "$logTag | $logDetails" }

    cameraController.takePicture(
      outputOptions,
      cameraExecutor,
      object : ImageCapture.OnImageSavedCallback {
        override fun onCaptureStarted() {
          log { "$logTag | onCaptureStarted() | $logDetails" }
          super.onCaptureStarted()
        }

        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
          log { "$logTag | onImageSaved(outputFileResults.savedUri: ${outputFileResults.savedUri}) | $logDetails" }
          onImageSavedCallback(outputFileResults)
        }

        override fun onError(exception: ImageCaptureException) {
          log { "$logTag | onError(exception: $exception) | $logDetails" }
        }
      },
    )
  }

  private var currentRecording by mutableStateOf<Recording?>(null)
  val isRecording by derivedStateOf { currentRecording != null }

  @SuppressLint("MissingPermission")
    /** permissions are checked with [checkIfPermissionsAreGranted] */
  fun startVideoRecording(
    onVideoRecordedSuccessfully: (outputResults: OutputResults) -> Unit,
  ) {
    val logTag = "startVideoRecording()"

    if (!checkIfPermissionsAreGranted()) return log { "$logTag called w/o Permissions" }
    if (isRecording) return log { "$logTag called while recording" }

    val contentValues = MediaMetadataHelper.createContentValuesForVideo()

    val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
      context.contentResolver,
      MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
    ).setContentValues(contentValues).build()

    val logDetails =
      "name: ${contentValues.get(MediaStore.MediaColumns.DISPLAY_NAME)}, contentValues: $contentValues, outputOptions: $mediaStoreOutputOptions"
    log { "$logTag | $logDetails" }
    fun logWithDetails(message: String) = log { "$logTag | $message | $logDetails" }

    cameraController.setEnabledUseCases(CameraController.VIDEO_CAPTURE)
    currentRecording = cameraController.startRecording(
      mediaStoreOutputOptions,
      AudioConfig.create(true),
      cameraExecutor,
    ) { event ->
//    currentRecording = videoCapture.output.prepareRecording(context, mediaStoreOutputOptions)
//      .withAudioEnabled()
//      .asPersistentRecording()
//      .start(cameraExecutor) { event ->
      when (event) {
        is VideoRecordEvent.Start -> {
          logWithDetails("Recording started")
        }

        is VideoRecordEvent.Finalize -> {
          if (event.error != VideoRecordEvent.Finalize.ERROR_NONE) {
            currentRecording?.stop()
            currentRecording = null

            logWithDetails("Video recording failed, cause: ${event.cause}")
//            showToast("Video recording failed")
          } else {
            log { "Video recording succeeded" }
//            showToast("Video recording succeeded")
            postToMainThread {
              onVideoRecordedSuccessfully(event.outputResults)
            }
          }
        }
      }
    }
  }

  fun finishVideoRecording() {
    currentRecording?.stop()
    currentRecording = null
  }

  private fun onDispose() {
    finishVideoRecording()

    cameraExecutor.shutdown()
  }
}
