package com.dgalyanov.gallery.galleryViewModel

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.video.OutputResults
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
import com.dgalyanov.gallery.mocks.MockDrafts
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

internal typealias ImmutableAllDraftsMap = Map<CreativityType, Map<GalleryAssetId, GalleryAsset>>
internal typealias MutableAllDraftsMap = MutableMap<CreativityType, Map<GalleryAssetId, GalleryAsset>>

private typealias OnAssetsEmission = (
  assets: List<Asset>,
  aspectRatio: AssetAspectRatio,
  creativityType: CreativityType,
) -> Unit

private typealias OnNeuroStoriesProceedRequest = () -> Unit

internal data class StaticPaddings(
  val start: Dp = 0.dp,
  val top: Dp = 0.dp,
  val end: Dp = 0.dp,
  val bottom: Dp = 0.dp,
) {
  companion object {
    fun fromPaddingValues(paddingValues: PaddingValues): StaticPaddings = StaticPaddings(
      start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
      top = paddingValues.calculateTopPadding(),
      end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
      bottom = paddingValues.calculateBottomPadding(),
    )
  }
}

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

  init {
    log { "init" }
  }

  /** Layout Data -- START */
  var innerStaticPaddings by mutableStateOf(StaticPaddings())
    private set

  fun updateInnerStaticPaddings(newPaddingValues: PaddingValues) {
    log { "updateInnerStaticPaddings(newPaddingValues: $newPaddingValues) | current: $innerStaticPaddings" }
    val newStaticPaddings = StaticPaddings.fromPaddingValues(newPaddingValues)
    if (innerStaticPaddings == newStaticPaddings) return
    innerStaticPaddings = newStaticPaddings
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
    private set

  /**
   * w/o "_" clashes with internal setter
   */
  @Suppress("FunctionName")
  fun _setSelectedCreativityType(value: CreativityType) {
    log { "setSelectedCreativityType(value: $value)" }
    setIsSelectingDraft(false)
    selectedCreativityType = value
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

  /** Drafts -- START */
  private var _allDraftsMap by mutableStateOf<ImmutableAllDraftsMap?>(null)

  // will come from React
  fun setAllDraftsMap(drafts: ImmutableAllDraftsMap?) {
    log { "setAllDrafts(drafts: $drafts)" }
    _allDraftsMap = drafts
  }

  private fun getAppropriateDrafts(creativityType: CreativityType): Map<GalleryAssetId, GalleryAsset>? =
    _allDraftsMap?.get(creativityType)?.filter { (_, draft) ->
      creativityType.supportedAssetTypes.contains(draft.type)
    }

  val selectedCreativityTypeHasDrafts by derivedStateOf {
    getAppropriateDrafts(selectedCreativityType)?.isNotEmpty() ?: false
  }

  var isSelectingDraft by mutableStateOf(false)
    private set

  fun setIsSelectingDraft(value: Boolean) {
    log { "setIsSelectingDraft(value: $value) | current: $isSelectingDraft" }
    if (value == isSelectingDraft) return

    clearSelectedAssets()
    isSelectingDraft = value
  }
  /** Drafts -- END */

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

  /** assuming drafts don't exist when Gallery is empty */
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

      if (_allDraftsMap == null) {
        setAllDraftsMap(MockDrafts.createMockDrafts(allAssetsMap.values.toList()))
        showToast(context, "USING MOCK DRAFTS, THIS SHOULD NOT HAPPEN IN PRODUCTION")
      }
    }
  }

  /**
   * considers [isSelectingDraft], [allowedAssetsTypes]
   */
  val assetsToDisplay: List<GalleryAsset> by derivedStateOf {
    if (isSelectingDraft) {
      return@derivedStateOf getAppropriateDrafts(selectedCreativityType)?.values?.toList()
                            ?: listOf()
    }

    val shouldFilterByAssetType = allowedAssetsTypes.size != GalleryAssetType.entries.size

    val result = if (selectedAlbum.id == GalleryAssetsAlbum.RecentsAlbum.id) {
      if (shouldFilterByAssetType) allAssetsMap.filter { allowedAssetsTypes.contains(it.value.type) }
      else allAssetsMap
    } else allAssetsMap.filter {
      it.value.albumId == selectedAlbum.id && (!shouldFilterByAssetType || allowedAssetsTypes.contains(
        it.value.type
      ))
    }

    log { "calculated assetsToShow, allowedAssetsTypes: $allowedAssetsTypes, selectedAlbum: $selectedAlbum, allAssetsMap.size: ${allAssetsMap.size} result.size: ${result.size}" }
    return@derivedStateOf result.values.toList()
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

//    val allDraftsFlattenedMap = mutableMapOf<GalleryAssetId, GalleryAsset>().let {
//      _allDraftsMap?.forEach { (_, drafts) ->
//        drafts.forEach { (_, draft) ->
//          it[draft.id] = draft
//        }
//      }
//      it.toMap()
//    }

    selectedAssetsIds.forEach {
      log { "$logTag | iterating with $it" }
      if (it != assetToRemain?.id) {
        allAssetsMap[it]?.deselect()
//        allDraftsFlattenedMap[it]?.deselect()
        _allDraftsMap?.forEach { (_, drafts) ->
          drafts[it]?.deselect()
        }
      }
    }
    selectedAssetsIds.retainAll(listOf(assetToRemain?.id).toSet())

    updateSelectedAssetsIndices()

    log { "$logTag | amountOnEnd: ${selectedAssetsIds.size}" }
  }

  private val isMultiselectDisallowed by derivedStateOf { isSelectingDraft }

  var isMultiselectEnabled by mutableStateOf(false)
    private set

  @Suppress("FunctionName")
  private fun _setIsMultiselectEnabled(value: Boolean) {
    log { "_setIsMultiselectEnabled(value: $value) | isMultiselectDisallowed: $isMultiselectDisallowed" }

    if (value && isMultiselectDisallowed) return

    if (!value) clearSelectedAssets(
      if (allowedAssetsTypes.contains(previewedAsset?.type)) previewedAsset else null
    )
    isMultiselectEnabled = value
  }

  private fun subscribeToIsMultiselectAllowedJob(): Job = viewModelScope.launch {
    snapshotFlow { isMultiselectDisallowed }.cancellable().distinctUntilChanged()
      .collectLatest { result ->
        log { "collected latest isMultiselectDisallowed, result: $result" }
        _setIsMultiselectEnabled(false)
      }
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
//    if (asset.isSelected) return

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

  fun onThumbnailLongClick(asset: GalleryAsset) {
    log { "onThumbnailLongClick(asset: $asset)" }

    if (isMultiselectEnabled) onThumbnailClick(asset)
    else {
      selectAsset(asset)
      if (!isMultiselectDisallowed) _setIsMultiselectEnabled(true)
    }
  }

  private fun maybeSelectAppropriateAsset() {
    log { "maybeSelectAppropriateAsset() | isMultiselectEnabled: $isMultiselectEnabled, selectedAssetsIds.isEmpty(): ${selectedAssetsIds.isEmpty()}, allowedAssetsTypes: $allowedAssetsTypes" }
    if (assetsToDisplay.isNotEmpty() && (!isMultiselectEnabled || selectedAssetsIds.isEmpty())) {
      selectAsset(assetsToDisplay.first { allowedAssetsTypes.contains(it.type) })
    }
  }

  private fun subscribeToAssetsToShowChange(): Job = viewModelScope.launch {
    snapshotFlow { assetsToDisplay }.cancellable().distinctUntilChanged()
      .collectLatest { result ->
        log { "collected latest assetsToShow, result.size: ${result.size}, assetsToShow.size: ${assetsToDisplay.size}" }

        if (isMultiselectEnabled) deselectDisallowedAssets()
        else clearSelectedAssets()

        maybeSelectAppropriateAsset()
      }
  }

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
  private var onAssetsEmission: OnAssetsEmission? = null
  fun setOnAssetsEmission(value: OnAssetsEmission) {
    log { "setOnAssetsEmission(...)" }
    onAssetsEmission = value
  }

  private fun emitAssets(assets: List<Asset>, aspectRatio: AssetAspectRatio = usedAspectRatio) {
    log { "emitAssets(assets: $assets, aspectRatio: $aspectRatio)" }
    onAssetsEmission?.invoke(assets, aspectRatio, selectedCreativityType)
    resetAssetsSelection()
  }

  var isPreparingSelectedAssetsForEmission by mutableStateOf(false)
  fun emitCurrentlySelectedAssets() {
    viewModelScope.launch(Dispatchers.Main) { exoPlayerController.pause() }

    viewModelScope.launch(Dispatchers.IO) {
      log { "emitCurrentlySelected() | isPreparingSelectedAssetsForEmission: $isPreparingSelectedAssetsForEmission, selectedAssetsIds: $selectedAssetsIds" }
      if (isPreparingSelectedAssetsForEmission) return@launch
      isPreparingSelectedAssetsForEmission = true

      if (selectedAssetsIds.isEmpty()) return@launch

      if (isSelectingDraft) {
        val asset = getAppropriateDrafts(selectedCreativityType)?.get(selectedAssetsIds.first())
                    ?: return@launch
        emitAssets(listOf(asset), asset.closestAspectRatio)
        return@launch
      }

      val croppedSelectedAssets = selectedAssetsIds.map { selectedAssetId ->
        async {
          val asset =
            allAssetsMap[selectedAssetId]?.copy() ?: GalleryContentResolver.getGalleryAssetById(
              selectedAssetId
            )

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

  private val selectedAlbumAssetsMapChangeSubscriptionJob = subscribeToAssetsToShowChange()
  private val isMultiselectAllowedJob = subscribeToIsMultiselectAllowedJob()

  override fun onCleared() {
    super.onCleared()
    log { "onCleared()" }

    exoPlayerController.onDispose()

    selectedAlbumAssetsMapChangeSubscriptionJob.cancel()
    isMultiselectAllowedJob.cancel()
  }
}