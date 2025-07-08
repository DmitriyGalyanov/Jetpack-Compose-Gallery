package com.dgalyanov.gallery.galleryViewModel

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.video.OutputResults
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dgalyanov.gallery.cropper.AssetCropper
import com.dgalyanov.gallery.dataClasses.Asset
import com.dgalyanov.gallery.dataClasses.AssetAspectRatio
import com.dgalyanov.gallery.dataClasses.AssetSize
import com.dgalyanov.gallery.dataClasses.CreativityType
import com.dgalyanov.gallery.dataClasses.GalleryAsset
import com.dgalyanov.gallery.dataClasses.GalleryAssetId
import com.dgalyanov.gallery.dataClasses.GalleryAssetType
import com.dgalyanov.gallery.dataClasses.GalleryAssetsAlbum
import com.dgalyanov.gallery.galleryContentResolver.GalleryContentResolver
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.previewedAssetView.clampAssetTransformationsAndCropData
import com.dgalyanov.gallery.utils.GalleryLogFactory
import com.dgalyanov.gallery.utils.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// todo: come up with a better name
private typealias OnAssetsEmission = (
  assets: List<Asset>,
  aspectRatio: AssetAspectRatio,
  creativityType: CreativityType,
) -> Unit

private typealias OnNeuroStoriesProceedRequest = () -> Unit

// todo: add Content Modes (post, story, reels, aiFilters)
internal class GalleryViewModel(
  @SuppressLint("StaticFieldLeak") private val context: Context,
) : ViewModel() {
  companion object {
    val LocalGalleryViewModel =
      staticCompositionLocalOf<GalleryViewModel> { error("CompositionLocal of GalleryViewModel not present") }

    private const val MULTISELECT_LIMIT = 10

    // todo: check if should use PerformanceClass to determine this Value
    const val PREVIEWED_ASSET_SELECTION_VISUAL_DELAY_MS = 150
    val PREVIEWED_ASSET_SELECTION_RELATED_ANIMATIONS_SPEC = tween<Float>(
      PREVIEWED_ASSET_SELECTION_VISUAL_DELAY_MS
    )
  }

  private val log = GalleryLogFactory("GalleryViewModel")

  /** Layout Data -- START */
  var innerPaddings by mutableStateOf(PaddingValues())
    private set

  fun updateInnerPaddings(newPaddingValues: PaddingValues) {
    log { "updateInnerPaddings(newPaddingValues: $newPaddingValues) | current: $innerPaddings" }
    if (innerPaddings == newPaddingValues) return
    innerPaddings = newPaddingValues
  }

  private var density by mutableFloatStateOf(1F)

  var windowWidthPx by mutableIntStateOf(0)

  private val previewedAssetContainerWidthPx by derivedStateOf { windowWidthPx }
  private val previewedAssetContainerAspectRatio = AssetAspectRatio._1x1
  private val previewedAssetContainerHeightPx by derivedStateOf {
    (previewedAssetContainerWidthPx * previewedAssetContainerAspectRatio.heightToWidthNumericValue).toInt()
  }

  val previewedAssetViewWrapSize by derivedStateOf {
    AssetSize(
      width = previewedAssetContainerWidthPx.toDouble(),
      height = previewedAssetContainerHeightPx.toDouble(),
    )
  }

  private var windowHeightPx by mutableIntStateOf(0)
  val windowHeightDp by derivedStateOf { windowHeightPx / density }

  fun updateWindowMetrics(density: Float, width: Int, height: Int) {
    log { "updateWindowMetrics(density: $density, width: $width, height: $height)" }

    this.density = density
    this.windowWidthPx = width
    this.windowHeightPx = height
  }
  /** Layout Data -- END */

  /** CreativityType -- START */
  val availableCreativityTypes = CreativityType.entries
  var selectedCreativityType by mutableStateOf(availableCreativityTypes.first())

  }

  val thumbnailAspectRatio by derivedStateOf { selectedCreativityType.thumbnailAspectRatio }

  val isPreviewEnabled by derivedStateOf { selectedCreativityType.isPreviewEnabled }

  /**
   * enabled at all, independent of [selectedCreativityType]
   *
   * given we can't determine Asset's [Type][GalleryAssetType] before fetch
   * –> all Assets are stored in [allAssetsMap] (even the ones with "disabled" [types][GalleryAssetType])
   */
  private val enabledAssetsTypes = GalleryAssetType.entries

  /**
   * enabled for [selectedCreativityType]
   */
  val allowedAssetsTypes by derivedStateOf { selectedCreativityType.supportedAssetTypes }

  /** CreativityType -- END */

  /** Albums -- START */
  var albumsList by mutableStateOf(listOf<GalleryAssetsAlbum>())

  var isFetchingAlbums by mutableStateOf(false)
    private set

  fun refreshAlbumsList() {
    val logTag = "refreshAlbumsList()"
    log { "$logTag | currentAlbumsListSize: ${albumsList.size}" }

    isFetchingAlbums = true
    viewModelScope.launch(Dispatchers.IO) {
      albumsList = GalleryContentResolver.getMediaAlbums()
    }.invokeOnCompletion {
      isFetchingAlbums = false
      log { "$logTag finished | newAlbumsListSize: ${albumsList.size}" }
    }
  }

  var selectedAlbum by mutableStateOf(GalleryAssetsAlbum.RecentsAlbum)
    private set

  fun selectAlbum(album: GalleryAssetsAlbum) {
    log { "selectAlbum(album: $album) | current: $selectedAlbum" }
    if (selectedAlbum == album) return
    selectedAlbum = album
  }

  var isFetchingAllAssets by mutableStateOf(false)
    private set
  private var allAssetsMap by mutableStateOf(mapOf<GalleryAssetId, GalleryAsset>())
  val isGalleryEmpty by derivedStateOf { allAssetsMap.isEmpty() }

  fun populateAllAssetsMap() {
    val startTimeMs = System.currentTimeMillis()
    val logTag = "populateAllAssetsMap()"
    log { "$logTag start" }

    if (isFetchingAllAssets) {
      log { "$logTag cancelled since isPopulatingAllAssetsMap already" }
      return
    }
    isFetchingAllAssets = true

    viewModelScope.launch(Dispatchers.IO) {
      allAssetsMap = GalleryContentResolver.getAlbumAssets(GalleryAssetsAlbum.RECENTS_ALBUM_ID)
    }.invokeOnCompletion {
      resetAssetsSelection()

      isFetchingAllAssets = false
      log { "$logTag finished, time taken: ${System.currentTimeMillis() - startTimeMs}" }
    }
  }

  /**
   * filtered with [allowedAssetsTypes]
   */
  val selectedAlbumAssetsMap by derivedStateOf {
    val shouldFilterByAssetType = allowedAssetsTypes.size != GalleryAssetType.entries.size
    log { "shouldFilterByAssetType:$shouldFilterByAssetType" }

    val result = if (selectedAlbum.id == GalleryAssetsAlbum.RecentsAlbum.id) {
      if (shouldFilterByAssetType) allAssetsMap.filter { allowedAssetsTypes.contains(it.value.type) }
      else allAssetsMap
    } else allAssetsMap.filter {
      it.value.albumId == selectedAlbum.id && (!shouldFilterByAssetType || allowedAssetsTypes.contains(
        it.value.type
      ))
    }

    log { "calculated selectedAlbumAssetsMap, allowedAssetsTypes: $allowedAssetsTypes, selectedAlbum: $selectedAlbum, allAssetsMap.size: ${allAssetsMap.size} result.size: ${result.size}" }
    return@derivedStateOf result
  }

  /** Albums -- END */

  /** Assets Selection -- START */
  /** Aspect Ratio -- START */
  var availableAspectRatios by mutableStateOf(AssetAspectRatio.entries)
  private var autoSelectedAspectRatio by mutableStateOf<AssetAspectRatio?>(null)
  var userSelectedAspectRatio by mutableStateOf<AssetAspectRatio?>(null)
  val usedAspectRatio by derivedStateOf {
    userSelectedAspectRatio ?: autoSelectedAspectRatio ?: previewedAsset?.closestAspectRatio
    ?: AssetAspectRatio._1x1
  }

  /** Aspect Ratio -- END */
  private val selectedAssetsIds = mutableStateSetOf<GalleryAssetId>()
  val anAssetIsSelected by derivedStateOf { selectedAssetsIds.isNotEmpty() }

  private fun updateSelectedAssetsIndices() {
    log { "updateSelectedAssetsIndices() | selectedAssetsIds: $selectedAssetsIds" }
    selectedAssetsIds.forEachIndexed { index, id ->
      allAssetsMap[id]?.setSelectionIndex(index)
    }
  }

  private fun clearSelectedAssets(assetToRemain: GalleryAsset? = null) {
    val logTag =
      "clearSelectedAssets(assetToRemain: $assetToRemain) | amountOnStart: ${selectedAssetsIds.size}"
    log { logTag }

    selectedAssetsIds.forEach {
      log { "$logTag | iterating with $it" }
      if (it != assetToRemain?.id) allAssetsMap[it]?.deselect()
    }
    selectedAssetsIds.retainAll(listOf(assetToRemain?.id).toSet())

    updateSelectedAssetsIndices()

    log { "$logTag | amountOnEnd: ${selectedAssetsIds.size}" }
  }

  var isMultiselectEnabled by mutableStateOf(false)
    private set

  @Suppress("FunctionName")
  private fun _setIsMultiselectEnabled(value: Boolean) {
    log { "_setIsMultiselectEnabled(value: $value)" }
    if (!value) clearSelectedAssets(
      if (allowedAssetsTypes.contains(previewedAsset?.type)) previewedAsset else null
    )
    isMultiselectEnabled = value
  }

  fun toggleIsMultiselectEnabled() {
    log { "toggleIsMultiselectEnabled() | current: $isMultiselectEnabled" }
    _setIsMultiselectEnabled(!isMultiselectEnabled)
  }

  var previewedAsset by mutableStateOf<GalleryAsset?>(null)
    private set

  var nextPreviewedAsset by mutableStateOf<GalleryAsset?>(null)
    private set

  private var previewedAssetSelectionJob: Job? = null

  @Suppress("FunctionName")
  private fun _setPreviewedAsset(asset: GalleryAsset?) {
    log { "_setPreviewedAsset(asset: $asset) | current: $previewedAsset, nextPreviewedAsset: $nextPreviewedAsset, previewedAssetSelectionJob?.isActive: ${previewedAssetSelectionJob?.isActive}" }
    if (asset == null) {
      previewedAssetSelectionJob?.cancel()
      previewedAsset = null
      nextPreviewedAsset = null
      return
    }

    nextPreviewedAsset = asset

    previewedAssetSelectionJob?.cancel()
    previewedAssetSelectionJob = viewModelScope.launch {
      if (previewedAsset != null) {
        delay(PREVIEWED_ASSET_SELECTION_VISUAL_DELAY_MS.toLong())
      }
      if (!isActive) return@launch

      previewedAsset = asset
      if (!isMultiselectEnabled) {
        autoSelectedAspectRatio = asset.closestAspectRatio
      }
      nextPreviewedAsset = null
    }
  }

  private fun selectAsset(asset: GalleryAsset) {
    log { "selectAsset(asset: $asset)" }
    if (asset.isSelected) return

    if (!isMultiselectEnabled) clearSelectedAssets()

    asset.setSelectionIndex(selectedAssetsIds.size)
    selectedAssetsIds += asset.id

    _setPreviewedAsset(asset)

    updateSelectedAssetsIndices()
  }

  private fun deselectAsset(asset: GalleryAsset) {
    log { "deselectAsset(asset: $asset)" }
    if (!asset.isSelected) return

    asset.deselect()
    selectedAssetsIds.remove(asset.id)

    if (previewedAsset == asset) {
      val newPreviewedAsset = if (selectedAssetsIds.isEmpty()) null
      else allAssetsMap[selectedAssetsIds.last()]
      _setPreviewedAsset(newPreviewedAsset)
    }

    updateSelectedAssetsIndices()
  }

  private fun deselectDisallowedAssets() {
    val logTag = "deselectDisallowedAssets()"
    log { "$logTag | currentlySelectedAssets: ${selectedAssetsIds.map { allAssetsMap[it] }}, allowedAssetsTypes: $allowedAssetsTypes" }

    selectedAssetsIds.mapNotNull {
      val asset = allAssetsMap[it]
      return@mapNotNull if (allowedAssetsTypes.contains(asset?.type)) null
      else asset
    }.forEach(action = ::deselectAsset)

    log { "$logTag finished | currentlySelectedAssetsIds: ${selectedAssetsIds.map { allAssetsMap[it] }}" }
  }

  fun onThumbnailClick(asset: GalleryAsset) {
    log { "onThumbnailClick(asset: $asset)" }

    if (isMultiselectEnabled) {
      if (asset.isSelected) {
        when {
          asset != previewedAsset -> _setPreviewedAsset(asset)
          asset == previewedAsset && selectedAssetsIds.size > 1 -> deselectAsset(asset)
        }
      } else if (selectedAssetsIds.size < MULTISELECT_LIMIT) selectAsset(asset)
    } else {
      selectAsset(asset)
    }
  }

  private fun maybeSelectAppropriateAsset() {
    log { "maybeSelectAppropriateAsset() | isMultiselectEnabled: $isMultiselectEnabled, selectedAssetsIds.isEmpty(): ${selectedAssetsIds.isEmpty()}, allowedAssetsTypes: $allowedAssetsTypes" }
    if (selectedAlbumAssetsMap.isNotEmpty() && (!isMultiselectEnabled || selectedAssetsIds.isEmpty())) {
      selectAsset(selectedAlbumAssetsMap.values.first { allowedAssetsTypes.contains(it.type) })
    }
  }

  private fun subscribeToSelectedAlbumAssetsMapChange(): Job {
    log { "subscribeToSelectedAlbumAssetsMapChange()" }
    return viewModelScope.launch {
      snapshotFlow { selectedAlbumAssetsMap }.cancellable().distinctUntilChanged()
        .collectLatest { result ->
          log { "collected latest selectedAlbumAssetsMap update, result.size: ${result.size}, selectedAlbumAssetsMap.size: ${selectedAlbumAssetsMap.size}" }

          if (isMultiselectEnabled) deselectDisallowedAssets()
          else clearSelectedAssets()

          maybeSelectAppropriateAsset()
        }
    }
  }

  private val selectedAlbumAssetsMapChangeSubscriptionJob =
    subscribeToSelectedAlbumAssetsMapChange()

  private fun resetAssetsSelection() {
    log { "resetAssetsSelection()" }
    _setIsMultiselectEnabled(false)
    maybeSelectAppropriateAsset()
  }

  /** Assets Selection -- END */

  val exoPlayerController = ExoPlayerController(context)

  var onNeuroStoriesProceedRequest: OnNeuroStoriesProceedRequest = {
    showToast(context, "NeuroStoriesProceedRequest")
  }
    private set

  // will be exposed to React
  fun setOnNeuroStoriesProceedRequest(value: OnNeuroStoriesProceedRequest) {
    log { "setOnNeuroStoriesProceedRequest(...)" }
    onNeuroStoriesProceedRequest = value
  }

  /** Selection Emission -- START */
  // todo: emit aspectRatio and selectedCreativityType
  private var onAssetsEmission: OnAssetsEmission? = null
  fun setOnAssetsEmission(value: OnAssetsEmission) {
    log { "setOnAssetsEmission(...)" }
    onAssetsEmission = value
  }

  private fun emitAssets(assets: List<Asset>) {
    log { "emitAssets(assets: $assets)" }
    onAssetsEmission?.invoke(assets, usedAspectRatio, selectedCreativityType)
    resetAssetsSelection()
  }

  var isPreparingSelectedAssetsForEmission by mutableStateOf(false)
  fun emitCurrentlySelectedAssets() {
    viewModelScope.launch(Dispatchers.Main) { exoPlayerController.pause() }

    viewModelScope.launch(Dispatchers.IO) {
      log { "emitCurrentlySelected() | isPreparingSelectedAssetsForEmission: $isPreparingSelectedAssetsForEmission, selectedAssetsIds: $selectedAssetsIds" }
      if (isPreparingSelectedAssetsForEmission) return@launch
      isPreparingSelectedAssetsForEmission = true

      val croppedSelectedAssets = selectedAssetsIds.map { selectedAssetId ->
        async {
          val asset = allAssetsMap[selectedAssetId] ?: GalleryContentResolver.getGalleryAssetById(
            selectedAssetId
          )?.copy()

          if (asset != null) {
            clampAssetTransformationsAndCropData(
              asset = asset,
              wrapSize = previewedAssetViewWrapSize,
              cropContainerAspectRatio = usedAspectRatio,
            )
            return@async AssetCropper.getCroppedAsset(
              asset = asset,
              context = context,
            )
          } else return@async null
        }
      }.awaitAll().filterNotNull()
      emitAssets(croppedSelectedAssets)
    }.invokeOnCompletion { isPreparingSelectedAssetsForEmission = false }
  }

  fun emitCapturedImage(
    capturedImageFile: ImageCapture.OutputFileResults,
    afterCompletion: (suspend CoroutineScope.() -> Unit)? = null,
  ) {
    val startTime = System.currentTimeMillis()
    val logTag = "emitCapturedImage(...)"
    log { logTag }

    capturedImageFile.savedUri?.let { uri ->
      GalleryContentResolver.getGalleryAssetByUri(uri)?.let { asset ->
        emitAssets(listOf(asset))
      } ?: log { "$logTag | couldn't get asset" }
    } ?: log { "$logTag | capturedImageFile has no savedUri" }

    log { "$logTag finished, time (ms) taken: ${System.currentTimeMillis() - startTime}" }
    if (afterCompletion != null) viewModelScope.launch { afterCompletion() }
  }

  fun emitRecordedVideo(
    recordedVideoOutputResults: OutputResults,
    afterCompletion: (suspend CoroutineScope.() -> Unit)? = null,
  ) {
    val startTime = System.currentTimeMillis()
    val logTag = "emitRecordedVideo(recordedVideoOutputResults: $recordedVideoOutputResults)"
    log { logTag }

    GalleryContentResolver.getGalleryAssetByUri(recordedVideoOutputResults.outputUri)
      ?.let { asset ->
        emitAssets(listOf(asset))
      } ?: log { "$logTag | couldn't get asset" }

    log { "$logTag finished, time (ms) taken: ${System.currentTimeMillis() - startTime}" }
    if (afterCompletion != null) viewModelScope.launch { afterCompletion() }
  }

  /** Selection Emission -- END */

  override fun onCleared() {
    super.onCleared()
    log { "onCleared()" }
    exoPlayerController.onDispose()
    selectedAlbumAssetsMapChangeSubscriptionJob.cancel()
  }
}