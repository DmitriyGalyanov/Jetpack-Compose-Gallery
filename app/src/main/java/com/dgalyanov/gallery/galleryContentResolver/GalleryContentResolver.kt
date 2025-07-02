package com.dgalyanov.gallery.galleryContentResolver

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.dgalyanov.gallery.utils.GalleryLogFactory
import com.dgalyanov.gallery.MainActivity
import com.dgalyanov.gallery.dataClasses.Asset
import com.dgalyanov.gallery.dataClasses.GalleryAssetsAlbum
import com.dgalyanov.gallery.dataClasses.GalleryAsset
import com.dgalyanov.gallery.dataClasses.GalleryAssetId
import java.io.File

// todo: mark appropriate functions as suspending
// todo: make singleton
internal class GalleryContentResolver {
  companion object {
    private val log = GalleryLogFactory("GalleryContentResolver")

    private lateinit var mainActivity: MainActivity
    val context: Context get() = mainActivity.applicationContext
    val contentResolver: ContentResolver get() = context.contentResolver

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
      albumId: Long = GalleryAssetsAlbum.RECENTS_ALBUM_ID,
      collectionUri: Uri = this.collectionUri,
      projection: Array<String> = arrayOf(MediaStore.Files.FileColumns._ID),
      orderAscending: Boolean = false,
      limit: Int? = null,
    ): Cursor? {
      val selectionCondition =
        if (albumId != GalleryAssetsAlbum.RECENTS_ALBUM_ID) "${MediaStore.Images.ImageColumns.BUCKET_ID} = ?" else null
      val selectionArgs =
        if (albumId != GalleryAssetsAlbum.RECENTS_ALBUM_ID) arrayOf("$albumId") else null

      return createQueryCursor(
        collectionUri = collectionUri,
        projection = projection,
        selectionCondition = selectionCondition,
        selectionArgs = selectionArgs,
        orderAscending = orderAscending,
        limit = limit,
      )
    }

    private val assetQueryProjection = arrayOf(
      MediaStore.Files.FileColumns._ID,
      MediaStore.Files.FileColumns.BUCKET_ID,
//      MediaStore.Files.FileColumns.DISPLAY_NAME,
      MediaStore.Files.FileColumns.DURATION,
      MediaStore.Files.FileColumns.WIDTH,
      MediaStore.Files.FileColumns.HEIGHT,
      MediaStore.Files.FileColumns.ORIENTATION,
    )

    private fun getAssetsFromCursor(
      cursor: Cursor,
      /** if false -> gets all */
      shouldGetOnlyFirst: Boolean = false
    ): MutableMap<GalleryAssetId, GalleryAsset> {
      val assets = mutableMapOf<GalleryAssetId, GalleryAsset>()

      val idColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
      val albumIdColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
      val durationColumnIndex =
        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)
      val rawWidthColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
      val rawHeightColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
      val orientationDegreesColumnIndex =
        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.ORIENTATION)

      fun extractAssetFromCurrentCursorPosition() {
        val id = cursor.getLong(idColumnIndex)

        assets[id] = GalleryAsset(
          id = id,
          albumId = cursor.getLong(albumIdColumnIndex),
          uri = ContentUris.withAppendedId(collectionUri, id),
          durationMs = cursor.getInt(durationColumnIndex),
          rawWidth = cursor.getDouble(rawWidthColumnIndex),
          rawHeight = cursor.getDouble(rawHeightColumnIndex),
          orientationDegrees = cursor.getInt(orientationDegreesColumnIndex),
        )
      }
      if (shouldGetOnlyFirst) {
        if (cursor.moveToFirst()) extractAssetFromCurrentCursorPosition()
      } else {
        while (cursor.moveToNext()) extractAssetFromCurrentCursorPosition()
      }

      cursor.close()
      return assets
    }

    /** do not call on main thread */
    fun getAlbumAssets(albumId: Long): Map<GalleryAssetId, GalleryAsset> {
      val logTag = "getAlbumAssets(albumId: $albumId)"
      log { logTag }
      val requestStartTimeMs = System.currentTimeMillis()

      val cursor = createAlbumQueryCursor(
        albumId = albumId,
        projection = assetQueryProjection,
        orderAscending = false,
      )

      val assets = cursor?.let { getAssetsFromCursor(it) } ?: mapOf()

      log { "$logTag finished | timeTaken: ${System.currentTimeMillis() - requestStartTimeMs} | Assets Amount: ${assets.size}" }
      // consumer should not mutate it
      return assets
    }

    private fun getAlbumAssetsCount(albumId: Long) = createAlbumQueryCursor(albumId)?.count ?: 0

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
          log { "getAlbumPreviewMediaUri | cursor.count: ${cursor.count}" }
          if (cursor.moveToFirst()) {
            val mediaId = cursor.getLong(cursor.getColumnIndexOrThrow(idColumnName))
            return ContentUris.withAppendedId(contentUri, mediaId)
          }
        }
      }

      return null
    }

    /** do not call on main thread */
    fun getMediaAlbums(): List<GalleryAssetsAlbum> {
      val logTag = "getMediaAlbums()"
      log { logTag }
      val requestStartTimeMs = System.currentTimeMillis()

      val albums = mutableMapOf<GalleryAssetId, GalleryAssetsAlbum>()

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

          log { "$logTag | met a bucket for the first time | buckedId: $bucketId, bucketName: $bucketName" }

          albums[bucketId] = GalleryAssetsAlbum(
            bucketId,
            bucketName,
            getAlbumPreviewMediaUri(bucketId),
            getAlbumAssetsCount(bucketId),
          )
        }
      }

      val result = albums.values.toMutableList()

      var recentsAlbumAssetsAmount = 0
      result.forEach { recentsAlbumAssetsAmount += it.assetsAmount }
      result.add(
        0,
        GalleryAssetsAlbum.updateRecentsAlbum(
          getAlbumPreviewMediaUri(GalleryAssetsAlbum.RECENTS_ALBUM_ID),
          assetsAmount = recentsAlbumAssetsAmount
        )
      )

      log { "$logTag finished | Albums Amount: ${result.size} | bucketsCount: $bucketsCount, skippedBucketsCount: $skippedBucketsCount | timeTaken: ${System.currentTimeMillis() - requestStartTimeMs}" }
      return result.toList()
    }

    fun getGalleryAssetByUri(uri: Uri): GalleryAsset? {
      val requestStartTimeMs = System.currentTimeMillis()
      val logTag = "getGalleryAssetByUri(uri: $uri)"

      val cursor = createQueryCursor(uri, assetQueryProjection)
      val asset = cursor?.let {
        getAssetsFromCursor(cursor, shouldGetOnlyFirst = true).values.firstOrNull()
      }
      log { "$logTag | ${if (asset != null) "success" else "failure"}, asset: $asset | timeTaken: ${System.currentTimeMillis() - requestStartTimeMs}" }
      return asset
    }

    fun getGalleryAssetById(id: GalleryAssetId): GalleryAsset? {
      log { "getGalleryAssetById(id: $id)" }
      return getGalleryAssetByUri(ContentUris.withAppendedId(collectionUri, id))
    }

    fun createAssetFromFile(file: File): Asset {
      val uri = file.toUri()

      val retriever = MediaMetadataRetriever()
      val durationMs =
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toInt() ?: 0
      val type = contentResolver.getType(uri)
      val isImage = type != null && type.contains("image")

      val rawWidth =
        retriever.extractMetadata(if (isImage) MediaMetadataRetriever.METADATA_KEY_IMAGE_WIDTH else MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
          ?.toDouble() ?: 0.0
      val rawHeight =
        retriever.extractMetadata(if (isImage) MediaMetadataRetriever.METADATA_KEY_IMAGE_HEIGHT else MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
          ?.toDouble() ?: 0.0

      val orientationDegrees = contentResolver.openInputStream(uri)?.use {
        return@use when (ExifInterface(it).getAttributeInt(
          ExifInterface.TAG_ORIENTATION,
          ExifInterface.ORIENTATION_UNDEFINED
        )) {
          ExifInterface.ORIENTATION_ROTATE_90 -> 90
          ExifInterface.ORIENTATION_ROTATE_180 -> 180
          ExifInterface.ORIENTATION_ROTATE_270 -> 270
          else -> 0
        }
      } ?: 0

      retriever.release()

      return Asset(
        uri = uri,
        durationMs = durationMs,
        rawWidth = rawWidth,
        rawHeight = rawHeight,
        orientationDegrees = orientationDegrees,
      )
    }
  }
}
