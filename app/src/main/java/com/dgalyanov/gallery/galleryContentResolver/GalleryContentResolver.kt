package com.dgalyanov.gallery.galleryContentResolver

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.dgalyanov.gallery.utils.GalleryLogFactory
import com.dgalyanov.gallery.MainActivity
import com.dgalyanov.gallery.galleryContentResolver.dataClasses.GalleryMediaAlbum
import com.dgalyanov.gallery.galleryContentResolver.dataClasses.GalleryMediaItem

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

    private val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else MediaStore.Files.getContentUri("external")

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createQueryBundle(
      selectionCondition: String?,
      selectionArgs: Array<String>?,
      orderBy: String,
      orderAscending: Boolean,
      limit: Int?,
      offset: Int,
    ) = Bundle().apply {
      if (limit != null) putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
      putInt(ContentResolver.QUERY_ARG_OFFSET, offset)

      putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(orderBy))
      putInt(
        ContentResolver.QUERY_ARG_SORT_DIRECTION,
        if (orderAscending) ContentResolver.QUERY_SORT_DIRECTION_ASCENDING
        else ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
      )

      if (selectionCondition != null) putString(
        ContentResolver.QUERY_ARG_SQL_SELECTION,
        selectionCondition
      )
      if (selectionArgs != null) putStringArray(
        ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
        selectionArgs
      )
    }

    private fun createQueryCursor(
      collectionUri: Uri = this.collectionUri,
      projection: Array<String>,
      selectionCondition: String? = null,
      selectionArgs: Array<String>? = null,
      orderBy: String = MediaStore.Files.FileColumns.DATE_ADDED,
      orderAscending: Boolean = true,
      limit: Int? = null,
      offset: Int = 0,
    ): Cursor? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val selection = createQueryBundle(
        selectionCondition = selectionCondition,
        selectionArgs = selectionArgs,
        orderBy = orderBy,
        orderAscending = orderAscending,
        limit = limit,
        offset = offset,
      )

      contentResolver.query(collectionUri, projection, selection, null)
    } else {
      val orderDirection = if (orderAscending) "ASC" else "DESC"
      val order = "$orderBy $orderDirection LIMIT $limit OFFSET $offset"

      contentResolver.query(collectionUri, projection, selectionCondition, selectionArgs, order)
    }

    private fun createAlbumQueryCursor(
      albumId: Long = GalleryMediaAlbum.RECENTS_ALBUM_ID,
      collectionUri: Uri = this.collectionUri,
      projection: Array<String> = arrayOf(MediaStore.Files.FileColumns._ID),
      orderAscending: Boolean = false,
      limit: Int? = null,
    ): Cursor? {
      val selectionCondition =
        if (albumId != GalleryMediaAlbum.RECENTS_ALBUM_ID) "${MediaStore.Images.ImageColumns.BUCKET_ID} = ?" else null
      val selectionArgs =
        if (albumId != GalleryMediaAlbum.RECENTS_ALBUM_ID) arrayOf("$albumId") else null

      return createQueryCursor(
        collectionUri = collectionUri,
        projection = projection,
        selectionCondition = selectionCondition,
        selectionArgs = selectionArgs,
        orderAscending = orderAscending,
        limit = limit,
      )
    }

    private val mediaItemQueryProjection = arrayOf(
      MediaStore.Files.FileColumns._ID,
      MediaStore.Files.FileColumns.DURATION,
    )

    private fun getMediaItemsFromCursor(
      cursor: Cursor,
      /** if false -> gets all */
      shouldGetOnlyFirst: Boolean = false
    ): MutableMap<Long, GalleryMediaItem> {
      val mediaItems = mutableMapOf<Long, GalleryMediaItem>()

      val idColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
      val durationColumnIndex =
        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)

      fun extractMediaItemFromCurrentCursorPosition() {
        val id = cursor.getLong(idColumnIndex)

        mediaItems[id] = GalleryMediaItem(
          id = id,
          uri = ContentUris.withAppendedId(collectionUri, id),
          durationMs = cursor.getInt(durationColumnIndex),
        )
      }
      if (shouldGetOnlyFirst) {
        if (cursor.moveToFirst()) extractMediaItemFromCurrentCursorPosition()
      } else {
        while (cursor.moveToNext()) extractMediaItemFromCurrentCursorPosition()
      }

      cursor.close()
      return mediaItems
    }

    /** do not call on main thread */
    fun getAlbumMediaItems(albumId: Long): Map<Long, GalleryMediaItem> {
      val logTag = "getAlbumMediaFiles(albumId: $albumId)"
      log(logTag)
      val requestStartTimeMs = System.currentTimeMillis()

      val cursor = createAlbumQueryCursor(
        albumId = albumId,
        projection = mediaItemQueryProjection,
        orderAscending = false,
      )

      val mediaItems = cursor?.let { getMediaItemsFromCursor(it) } ?: mapOf()

      log("$logTag finished | timeTaken: ${System.currentTimeMillis() - requestStartTimeMs} | Items Amount: ${mediaItems.size}")
      // consumer should not mutate it
      return mediaItems
    }

    private fun getAlbumItemsCount(albumId: Long) = createAlbumQueryCursor(albumId)?.count ?: 0

    private fun getAlbumPreviewMediaUri(albumId: Long): Uri? {
      val mediaTypesToTry = listOf(
        Pair(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media._ID),
        Pair(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media._ID)
      )

      for ((contentUri, idColumnName) in mediaTypesToTry) {
        createAlbumQueryCursor(
          albumId = albumId,
          collectionUri = contentUri,
          projection = arrayOf(idColumnName),
          limit = 1
        )?.use { cursor ->
          log("getAlbumPreviewMediaUri | cursor.count: ${cursor.count}")
          if (cursor.moveToFirst()) {
            val mediaId = cursor.getLong(cursor.getColumnIndexOrThrow(idColumnName))
            return ContentUris.withAppendedId(contentUri, mediaId)
          }
        }
      }

      return null
    }

    /** do not call on main thread */
    fun getMediaAlbums(): List<GalleryMediaAlbum> {
      val logTag = "getMediaAlbums()"
      log(logTag)
      val requestStartTimeMs = System.currentTimeMillis()

      val albums = mutableMapOf<Long, GalleryMediaAlbum>()

      val projection = arrayOf(
        MediaStore.Files.FileColumns.BUCKET_ID,
        MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
      )

      var bucketsCount = 0
      var skippedBucketsCount = 0

      createQueryCursor(
        projection = projection,
        orderBy = MediaStore.Files.FileColumns.DATE_MODIFIED,
      )?.use { cursor ->
        bucketsCount = cursor.count

        val bucketIdColumnIndex =
          cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
        val bucketDisplayNameColumnIndex =
          cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)

        while (cursor.moveToNext()) {
          val bucketId = cursor.getLong(bucketIdColumnIndex)

          if (albums.containsKey(bucketId)) {
            skippedBucketsCount++
            continue
          }

          val bucketName = cursor.getString(bucketDisplayNameColumnIndex) ?: "Unnamed album"

          log("$logTag | met a bucket for the first time | buckedId: $bucketId, bucketName: $bucketName")

          albums[bucketId] = GalleryMediaAlbum(
            bucketId,
            bucketName,
            getAlbumPreviewMediaUri(bucketId),
            getAlbumItemsCount(bucketId),
          )
        }
      }

      val result = albums.values.toMutableList()

      var recentsAlbumItemsAmount = 0
      result.forEach { recentsAlbumItemsAmount += it.itemsAmount }
      result.add(
        0,
        GalleryMediaAlbum.updateRecentsAlbum(
          getAlbumPreviewMediaUri(GalleryMediaAlbum.RECENTS_ALBUM_ID),
          itemsAmount = recentsAlbumItemsAmount
        )
      )

      log("$logTag finished | Albums Amount: ${result.size} | bucketsCount: $bucketsCount, skippedBucketsCount: $skippedBucketsCount | timeTaken: ${System.currentTimeMillis() - requestStartTimeMs}")
      return result.toList()
    }

    fun getGalleryMediaItemByUri(uri: Uri): GalleryMediaItem? {
      val requestStartTimeMs = System.currentTimeMillis()
      val logTag = "getGalleryMediaItemByUri(uri: $uri)"

      val cursor = createQueryCursor(uri, mediaItemQueryProjection)
      val mediaItem = cursor?.let {
        getMediaItemsFromCursor(cursor, shouldGetOnlyFirst = true).values.firstOrNull()
      }
      log("$logTag | ${if (mediaItem != null) "success" else "failure"}, item: $mediaItem | timeTaken: ${System.currentTimeMillis() - requestStartTimeMs}")
      return mediaItem
    }

    fun getGalleryMediaItemById(id: Long): GalleryMediaItem? {
      log("getGalleryMediaItemById(id: $id)")
      return getGalleryMediaItemByUri(ContentUris.withAppendedId(collectionUri, id))
    }
  }
}
