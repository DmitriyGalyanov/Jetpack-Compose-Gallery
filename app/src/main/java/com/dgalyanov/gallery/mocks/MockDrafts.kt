package com.dgalyanov.gallery.mocks

import com.dgalyanov.gallery.dataClasses.CreativityType
import com.dgalyanov.gallery.dataClasses.GalleryAsset
import com.dgalyanov.gallery.dataClasses.GalleryAssetId
import com.dgalyanov.gallery.galleryViewModel.ImmutableAllDraftsMap
import com.dgalyanov.gallery.galleryViewModel.MutableAllDraftsMap

internal object MockDrafts {
  fun createMockDrafts(
    source: List<GalleryAsset>,
    draftsPerCreativityType: Int = 30,
    creativityTypesToPopulate: Set<CreativityType> = CreativityType.entries.toMutableSet().let {
      it.remove(CreativityType.Neuro)
      it.toSet()
    },
  ): ImmutableAllDraftsMap {
    val result: MutableAllDraftsMap = mutableMapOf()

    var usedSourceItemIndex = 0

    creativityTypesToPopulate.forEach { creativityType ->
      val drafts = mutableMapOf<GalleryAssetId, GalleryAsset>()
      val firstInCreativityTypeAssetIndex = usedSourceItemIndex

      for (
      itemToUseIndex in firstInCreativityTypeAssetIndex..<
        firstInCreativityTypeAssetIndex + draftsPerCreativityType
      ) {
        val asset = source.getOrNull(itemToUseIndex)?.copy(asDraft = true) ?: return@forEach
        drafts[asset.id] = asset
        usedSourceItemIndex++
      }

      result[creativityType] = drafts
    }

    return result
  }
}