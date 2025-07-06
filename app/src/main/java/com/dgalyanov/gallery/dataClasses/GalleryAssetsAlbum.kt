package com.dgalyanov.gallery.dataClasses

import android.net.Uri
import androidx.compose.runtime.mutableStateOf

internal data class GalleryAssetsAlbum(
  val id: Long,
  val name: String,
  var previewUri: Uri?,
  val assetsAmount: Int,
) {
  companion object {
    const val RECENTS_ALBUM_ID = -1L
    private const val RECENTS_ALBUM_NAME = "Recents"

    private var _RecentsAlbum = mutableStateOf(
      GalleryAssetsAlbum(
        id = RECENTS_ALBUM_ID,
        name = RECENTS_ALBUM_NAME,
        previewUri = null,
        assetsAmount = 0,
      )
    )
    val RecentsAlbum get() = _RecentsAlbum.value

    private fun createRecentsAlbum(previewUri: Uri?, assetsAmount: Int) = GalleryAssetsAlbum(
      id = RECENTS_ALBUM_ID,
      name = RECENTS_ALBUM_NAME,
      previewUri = previewUri,
      assetsAmount = assetsAmount,
    )

    fun updateRecentsAlbum(previewUri: Uri?, assetsAmount: Int): GalleryAssetsAlbum {
      _RecentsAlbum.value = createRecentsAlbum(previewUri, assetsAmount)
      return _RecentsAlbum.value
    }
  }
}
