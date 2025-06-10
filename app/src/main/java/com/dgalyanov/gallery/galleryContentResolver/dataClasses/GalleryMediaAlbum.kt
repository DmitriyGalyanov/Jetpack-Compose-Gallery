package com.dgalyanov.gallery.galleryContentResolver.dataClasses

import android.net.Uri
import androidx.compose.runtime.mutableStateOf

data class GalleryMediaAlbum(
  val id: Long,
  val name: String,
  var previewUri: Uri?,
  val itemsAmount: Int,
) {
  companion object {
    const val RECENTS_ALBUM_ID = -1L
    private const val RECENTS_ALBUM_NAME = "Recents"

    private var _RecentsAlbum = mutableStateOf(
      GalleryMediaAlbum(
        id = RECENTS_ALBUM_ID,
        name = RECENTS_ALBUM_NAME,
        previewUri = null,
        itemsAmount = 0
      )
    )
    val RecentsAlbum get() = _RecentsAlbum.value

    private fun createRecentsAlbum(previewUri: Uri?, itemsAmount: Int) = GalleryMediaAlbum(
      id = RECENTS_ALBUM_ID,
      name = RECENTS_ALBUM_NAME,
      previewUri = previewUri,
      itemsAmount = itemsAmount
    )

    fun updateRecentsAlbum(previewUri: Uri?, itemsAmount: Int): GalleryMediaAlbum {
      _RecentsAlbum.value = createRecentsAlbum(previewUri, itemsAmount)
      return _RecentsAlbum.value
    }
  }
}
