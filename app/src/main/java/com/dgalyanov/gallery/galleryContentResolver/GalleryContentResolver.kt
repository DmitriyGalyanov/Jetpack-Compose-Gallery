package com.dgalyanov.gallery.galleryContentResolver

import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.dgalyanov.gallery.utils.GalleryLogFactory
import com.dgalyanov.gallery.MainActivity

class GalleryContentResolver {
  companion object {
    private val log = GalleryLogFactory("GalleryContentResolver")

    private lateinit var mainActivity: MainActivity
    private val context get() = mainActivity.applicationContext
    private val contentResolver get() = context.contentResolver

    fun init(mainActivity: MainActivity): Companion {
      if (::mainActivity.isInitialized) return this

      this.mainActivity = mainActivity
      return this
    }

    // do not call on main thread
    fun getMediaFiles(): List<GalleryMediaItem> {
      val requestStartTimeMs = System.currentTimeMillis()

      val mediaFiles = mutableListOf<GalleryMediaItem>()

      val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
      } else MediaStore.Files.getContentUri("external")

      val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DURATION,
      )

      val selection = null // "${MediaStore.Video.Media.DURATION} >= ?"
      val selectionArgs = null
//        arrayOf(
//        TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES).toString()
//      )

      val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} ASC"
      val query = contentResolver.query(
        collectionUri,
        projection,
        selection,
        selectionArgs,
        sortOrder
      )

      query?.use { cursor ->
        val idColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val durationColumnIndex =
          cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

        while (cursor.moveToNext()) {
          val id = cursor.getLong(idColumnIndex)
          val durationMs = cursor.getInt(durationColumnIndex)

          val contentUri: Uri = ContentUris.withAppendedId(collectionUri, id)

          mediaFiles += GalleryMediaItem(
            id,
            contentUri,
            durationMs,
          )
        }
      }

      log("getMediaFiles | delta: ${System.currentTimeMillis() - requestStartTimeMs} | Items Amount: ${mediaFiles.size}")
      return mediaFiles.toList()
    }
  }
}
