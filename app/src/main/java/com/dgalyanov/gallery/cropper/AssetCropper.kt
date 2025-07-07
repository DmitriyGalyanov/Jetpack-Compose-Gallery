package com.dgalyanov.gallery.cropper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.RectF
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.dgalyanov.gallery.dataClasses.Asset
import com.dgalyanov.gallery.dataClasses.GalleryAsset
import com.dgalyanov.gallery.dataClasses.GalleryAssetType
import com.dgalyanov.gallery.galleryContentResolver.GalleryContentResolver
import com.dgalyanov.gallery.utils.GalleryLogFactory
import com.dgalyanov.gallery.utils.MediaMetadataHelper
import com.dgalyanov.gallery.utils.isAlmostEqual
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal object AssetCropper {
  private val log = GalleryLogFactory("AssetCropper")

  private fun warnIfOnMainThread(methodName: String) {
    val isOnMainThread = Looper.myLooper() == Looper.getMainLooper()
    if (isOnMainThread) Log.w("Gallery.AssetCropper", "$methodName runs on MainThread")
  }

  private fun saveBitmapToGallery(context: Context, bitmap: Bitmap): GalleryAsset? {
    val startTime = System.currentTimeMillis()
    val logTag = "saveBitmapToGallery(bitmap: $bitmap)"
    warnIfOnMainThread("saveBitmapToGallery")

    val contentValues = MediaMetadataHelper.createContentValuesForPicture().also {
      it.put(MediaStore.Images.Media.IS_PENDING, 1)
    }

    val resolver = context.contentResolver

    val insertionUri = resolver.insert(
      MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
      contentValues,
    )
    if (insertionUri == null) {
      log { "$logTag | couldn't create insertionUri" }
      return null
    }

    resolver.openOutputStream(insertionUri)?.use {
      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)

      contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
      if (Build.VERSION.SDK_INT >= 30) resolver.update(insertionUri, contentValues, null)
    }

    val resolvedGalleryAsset = GalleryContentResolver.getGalleryAssetByUri(insertionUri)
    log { "$logTag | contentValues: $contentValues, insertionUri: $insertionUri, time (ms) taken: ${System.currentTimeMillis() - startTime},\n resolvedGalleryAsset: $resolvedGalleryAsset" }
    return resolvedGalleryAsset
  }

  private const val TEMP_FILE_PREFIX = "Gallery_Cropped_File_"
  private fun createTempFile(context: Context, mimeType: String): File? {
    val startTime = System.currentTimeMillis()
    val logTag = "createTempFile(..., mimeType: $mimeType)"
    warnIfOnMainThread("createTempFile")

    val externalCacheDir = context.externalCacheDir
    val internalCacheDir = context.cacheDir
    if (externalCacheDir == null && internalCacheDir == null) {
      log { "$logTag both external and internal cache directories are not available, aborting" }
      return null
    }

    val cacheDir = run {
      return@run if (externalCacheDir == null) internalCacheDir
      else if (internalCacheDir == null) externalCacheDir
      else if (externalCacheDir.freeSpace > internalCacheDir.freeSpace) externalCacheDir
      else internalCacheDir
    }

    val file = File.createTempFile(TEMP_FILE_PREFIX, mimeType, cacheDir)
    log { "$logTag finished, time (ms) taken: ${System.currentTimeMillis() - startTime}, created file: $file" }
    return file
  }

  private fun saveBitmapToFile(bitmap: Bitmap, context: Context): File? {
    val startTime = System.currentTimeMillis()
    warnIfOnMainThread("saveBitmapToFile")

    val file = createTempFile(context, mimeType = ".jpg")
    file?.outputStream()?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }

    log { "saveBitmapToFile finished | time (ms) taken: ${System.currentTimeMillis() - startTime}, resultingFile: $file" }
    return file
  }

  private fun saveFileToGallery(context: Context, file: File): GalleryAsset? {
    val startTime = System.currentTimeMillis()
    val logTag = "saveFileToGallery(file: $file)"
    warnIfOnMainThread("saveFileToGallery")

    val resolver = context.contentResolver

    val type = resolver.getType(file.toUri())
    // todo: handle videos
    if (type == null || !type.contains("image")) {
      log { "$logTag | received unsupported file, MIME Type: $type" }
      return null
    }

    val contentValues = MediaMetadataHelper.createContentValuesForPicture().also {
      it.put(MediaStore.MediaColumns.MIME_TYPE, type)
      it.put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val insertionUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    if (insertionUri == null) {
      log { "$logTag | couldn't create insertionUri" }
      return null
    }

    resolver.openOutputStream(insertionUri)?.use { outputStream ->
      resolver.openInputStream(file.toUri())?.use { inputStream ->
        inputStream.copyTo(outputStream)

        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        if (Build.VERSION.SDK_INT >= 30) {
          resolver.update(insertionUri, contentValues, null)
        }
      }
    }

    val resolvedGalleryAsset = GalleryContentResolver.getGalleryAssetByUri(insertionUri)
    log { "$logTag | type: $type, contentValues: $contentValues, insertionUri: $insertionUri, time (ms) taken: ${System.currentTimeMillis() - startTime},\n resolvedGalleryAsset: $resolvedGalleryAsset" }
    return resolvedGalleryAsset
  }

  private const val IMAGE_GREATEST_SIZE_MAX_LENGTH: Int = 1280
  private fun getDownscaleRatio(finalWidth: Int, finalHeight: Int): Int {
    val greatestSide = if (finalHeight >= finalWidth) "height" else "width"
    var downscaleRatio = 1

    // Calculate the largest downscaleRatio value that is a power of 2 and keeps both
    // height and width larger than the IMAGE_GREATEST_SIZE_MAX_LENGTH.
    if (greatestSide == "height") {
      val halfHeight = finalHeight / 2
      while ((halfHeight / downscaleRatio) >= IMAGE_GREATEST_SIZE_MAX_LENGTH) {
        downscaleRatio *= 2
      }
    }
    if (greatestSide == "width") {
      val halfWidth = finalWidth / 2
      while ((halfWidth / downscaleRatio) >= IMAGE_GREATEST_SIZE_MAX_LENGTH) {
        downscaleRatio *= 2
      }
    }

    log { "getDownscaleRatio(finalWidth: $finalWidth, finalHeight: $finalHeight) | result: $downscaleRatio" }
    return downscaleRatio
  }

  private class ExifTransformationInfo(val rotationDegree: Int, val scaleX: Int, val scaleY: Int) {
    companion object {
      private fun fromUri(
        context: Context, uri: Uri
      ): ExifTransformationInfo? =
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
          when (ExifInterface(inputStream).getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED
          )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> ExifTransformationInfo(90, 1, 1)

            ExifInterface.ORIENTATION_ROTATE_270 -> ExifTransformationInfo(270, 1, 1)

            ExifInterface.ORIENTATION_TRANSPOSE, ExifInterface.ORIENTATION_TRANSVERSE -> ExifTransformationInfo(
              270, -1, -1
            )

            ExifInterface.ORIENTATION_ROTATE_180 -> ExifTransformationInfo(180, 1, 1)

            ExifInterface.ORIENTATION_FLIP_VERTICAL -> ExifTransformationInfo(0, 1, -1)

            else -> ExifTransformationInfo(0, 1, 1)
          }
        }

      fun getMaybeTransformedBitmap(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val startTime = System.currentTimeMillis()
        val logTag = "getMaybeTransformedBitmap(...)"
        warnIfOnMainThread("getMaybeTransformedBitmap")

        val exifTransformationInfo = fromUri(context, uri)
        val rotationMatrix = if (exifTransformationInfo?.hasTransformations == true) {
          Matrix().apply {
            postRotate(exifTransformationInfo.rotationDegree.toFloat())
            postScale(
              exifTransformationInfo.scaleX.toFloat(),
              exifTransformationInfo.scaleY.toFloat(),
            )
          }
        } else null
        if (rotationMatrix != null) {
          log { "$logTag rotation fix is required" }
          val result = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            rotationMatrix,
            true,
          )
          log { "$logTag fixed rotation, time (ms) taken: ${System.currentTimeMillis() - startTime}" }
          return result
        } else {
          log { "$logTag rotation fix is not required, returning provided bitmap, time (ms) taken to find out: ${System.currentTimeMillis() - startTime}" }
          return bitmap
        }
      }
    }

    val hasTransformations: Boolean = rotationDegree != 0 || scaleX != 1 || scaleY != 1
    val isRotationMultipleOf90: Boolean = rotationDegree > 0 && rotationDegree % 90 == 0

    override fun toString() =
      "{ hasTransformations: $hasTransformations, isRotationMultipleOf90: $isRotationMultipleOf90, rotationDegree: $rotationDegree, scaleX: $scaleX, scaleY: $scaleY }"
  }

  private suspend fun getCroppedImage(
    asset: GalleryAsset,
    context: Context,
    shouldSaveToGallery: Boolean = false,
  ): Asset? = withContext(Dispatchers.IO) {
    val startTime = System.currentTimeMillis()
    val logTag =
      "crop(asset: { type: ${asset.type}, cropData: ${asset.cropData}, width: ${asset.width}, height: ${asset.height}, orientationDegrees: ${asset.orientationDegrees} })"
    warnIfOnMainThread("cropImage")

    if (asset.type != GalleryAssetType.Image) {
      log { "$logTag | received non-image Asset, aborted" }
      return@withContext null
    }

    val cropData = asset.cropData
    if (cropData == null) {
      log { "$logTag | received Asset w/o cropData, returning Asset" }
      return@withContext asset
    }

    val offset = cropData.offset
    val croppedSize = cropData.finalSize

    val downscaleRatio = getDownscaleRatio(croppedSize.width.toInt(), croppedSize.height.toInt())
    val bitmapOptions = BitmapFactory.Options().apply { inSampleSize = downscaleRatio }

    val resolver = context.contentResolver

    val bitmapDecodeStartTime = System.currentTimeMillis()
    var didApplyDownscaleRatio = false
    var bitmap = resolver.openInputStream(asset.uri)?.use {
      val decodedBitmap = BitmapFactory.decodeStream(it, null, bitmapOptions)
      didApplyDownscaleRatio = decodedBitmap != null
      decodedBitmap
    } ?: ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, asset.uri))
    log { "$logTag, decoded bitmap, time (ms) taken: ${System.currentTimeMillis() - bitmapDecodeStartTime}" }

    bitmap = ExifTransformationInfo.getMaybeTransformedBitmap(context, asset.uri, bitmap)

    val bitmapWidth = bitmap.width
    val bitmapHeight = bitmap.height

    val ratioToApplyToCropValues = if (didApplyDownscaleRatio) downscaleRatio else 1
    val finalOffsetX = offset.x / ratioToApplyToCropValues
    val finalOffsetY = offset.y / ratioToApplyToCropValues
    val finalWidth = croppedSize.width / ratioToApplyToCropValues
    val finalHeight = croppedSize.height / ratioToApplyToCropValues

    log { "$logTag | bitmapWidth: $bitmapWidth, bitmapHeight: $bitmapHeight, ratioToApplyToCropValues: $ratioToApplyToCropValues, finalOffsetX: $finalOffsetX, finalOffsetY: $finalOffsetY, finalWidth: $finalWidth, finalHeight: $finalHeight |" }

    val requiresVerticalWhitespace = finalHeight + finalOffsetY > bitmapHeight
    val requiresHorizontalWhitespace = finalWidth + finalOffsetX > bitmapWidth
    if (requiresVerticalWhitespace || requiresHorizontalWhitespace) {
      // todo: check if Whitespace Addition works correctly
      val whitespacesAdditionStartTime = System.currentTimeMillis()
      log { "$logTag whitespaces addition is required" }

      val heightWithWhitespaces =
        (if (requiresVerticalWhitespace) finalHeight + finalOffsetY else bitmapHeight).toInt()
      val widthWithWhitespaces =
        (if (requiresHorizontalWhitespace) finalWidth + finalOffsetX else bitmapWidth).toInt()

      val bitmapWithWhitespaces = createBitmap(widthWithWhitespaces, heightWithWhitespaces)

      val canvasWithWhitespaces = Canvas(bitmapWithWhitespaces)

      val whitespaceOffsetLeft = (widthWithWhitespaces - bitmapWidth).toFloat() / 2
      val whitespaceOffsetRight = whitespaceOffsetLeft + bitmapWidth
      val whitespaceOffsetTop = (heightWithWhitespaces - bitmapHeight).toFloat() / 2
      val whitespaceOffsetBottom = whitespaceOffsetTop + bitmapHeight

      val mutableBitmapCopy = if (bitmap.config != Bitmap.Config.HARDWARE) bitmap else bitmap.copy(
        Bitmap.Config.ARGB_8888, true
      )
      canvasWithWhitespaces.drawBitmap(
        mutableBitmapCopy, null, RectF(
          whitespaceOffsetLeft, whitespaceOffsetTop, whitespaceOffsetRight, whitespaceOffsetBottom
        ), null
      )

      bitmap = bitmapWithWhitespaces
      log {
        "$logTag added whitespaces, time (ms) taken: ${System.currentTimeMillis() - whitespacesAdditionStartTime}\n requiresVerticalWhitespace: $requiresVerticalWhitespace, requiresHorizontalWhitespace: $requiresHorizontalWhitespace, heightWithWhitespaces: $heightWithWhitespaces, widthWithWhitespaces: $widthWithWhitespaces, whitespaceOffsetLeft: $whitespaceOffsetLeft, whitespaceOffsetRight: $whitespaceOffsetRight, whitespaceOffsetTop: $whitespaceOffsetTop, whitespaceOffsetBottom: $whitespaceOffsetBottom"
      }
    } else {
      if (finalOffsetX == 0f && finalOffsetY == 0f && isAlmostEqual(
          finalWidth, bitmap.width.toDouble(), 1.0
        ) && isAlmostEqual(finalHeight, bitmap.height.toDouble(), 1.0)
      ) {
        log { "$logTag crop is not required" }
      } else {
        val cropStartTime = System.currentTimeMillis()
        log { "$logTag crop is required" }
        bitmap = Bitmap.createBitmap(
          bitmap,
          finalOffsetX.coerceAtLeast(0f).toInt(),
          finalOffsetY.coerceAtLeast(0f).toInt(),
          finalWidth.toInt(),
          finalHeight.toInt(),
        )
        log { "$logTag cropped, time (ms) taken: ${System.currentTimeMillis() - cropStartTime}" }
      }
    }

    val resultAsset = if (shouldSaveToGallery) saveBitmapToGallery(context, bitmap)
    else {
      saveBitmapToFile(bitmap, context)?.let {
        GalleryContentResolver.createAssetFromFile(it)
      }
    }

    log { "$logTag | time (ms) taken: ${System.currentTimeMillis() - startTime} resultAsset: $resultAsset" }
    return@withContext resultAsset
  }

  suspend fun getCroppedAsset(
    asset: GalleryAsset, context: Context, shouldSaveToGallery: Boolean = false
  ): Asset? = withContext(Dispatchers.IO) {
    when (asset.type) {
      GalleryAssetType.Image -> getCroppedImage(
        asset = asset, context = context, shouldSaveToGallery = shouldSaveToGallery
      )

      // todo: support Video Cropping
      else -> asset
    }
  }
}