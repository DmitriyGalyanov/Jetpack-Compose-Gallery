package com.dgalyanov.gallery.dataClasses

internal enum class CreativityType(
  val supportedAssetTypes: Set<GalleryAssetType>,
  val thumbnailAspectRatio: AssetAspectRatio,
  val isPreviewEnabled: Boolean,
) {
  Post(
    supportedAssetTypes = GalleryAssetType.entries.toSet(),
    thumbnailAspectRatio = AssetAspectRatio._1x1,
    isPreviewEnabled = true,
  ),
  Story(
    supportedAssetTypes = GalleryAssetType.entries.toSet(),
    thumbnailAspectRatio = AssetAspectRatio._16x9,
    isPreviewEnabled = false,
  ),
  Reel(
    supportedAssetTypes = GalleryAssetType.entries.toMutableSet().let {
      it.remove(GalleryAssetType.Image)
      it.toSet()
    },
    thumbnailAspectRatio = AssetAspectRatio._16x9,
    isPreviewEnabled = false,
  ),
  Neuro(
    supportedAssetTypes = GalleryAssetType.entries.toMutableSet().let {
      it.remove(GalleryAssetType.Video)
      it.toSet()
    },
    thumbnailAspectRatio = AssetAspectRatio._1x1,
    isPreviewEnabled = true
  ),
  NeuroStories(
    supportedAssetTypes = GalleryAssetType.entries.toSet(),
    thumbnailAspectRatio = AssetAspectRatio._1x1,
    isPreviewEnabled = false,
  ),
}
