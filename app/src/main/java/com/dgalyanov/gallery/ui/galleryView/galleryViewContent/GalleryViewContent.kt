package com.dgalyanov.gallery.ui.galleryView.galleryViewContent

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyLayoutScrollScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgalyanov.gallery.dataClasses.CreativityType
import com.dgalyanov.gallery.dataClasses.GalleryAssetType
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.ui.galleryView.CreativityTypeSelector
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.assetThumbnailView.AssetThumbnailView
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewContentCameraItem.CameraSheetButton
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewToolbar.GALLERY_VIEW_TOOLBAR_HEIGHT
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewToolbar.GalleryViewToolbar
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.previewedAssetView.TransformableAssetView
import com.dgalyanov.gallery.ui.utils.modifiers.conditional
import com.dgalyanov.gallery.utils.GalleryLogFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val COLUMNS_AMOUNT = 3

private const val TOOLBAR_ITEM_KEY = "Toolbar"
private const val CAMERA_BUTTON_ITEM_KEY = "Camera"
private const val GRID_NON_THUMBNAILS_ITEMS_AMOUNT = 2

private val log = GalleryLogFactory("GalleryViewContent")

@Composable
internal fun GalleryViewContent() {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current

  val thumbnailAspectRatio = galleryViewModel.thumbnailAspectRatio
  val isPreviewEnabled = galleryViewModel.isPreviewEnabled

  val density = LocalDensity.current

  val thumbnailWidthPx = galleryViewModel.windowWidthPx / COLUMNS_AMOUNT
  val thumbnailHeightPx =
    (thumbnailWidthPx * thumbnailAspectRatio.heightToWidthNumericValue).toFloat()

  val thumbnailWidthDp = with(density) { thumbnailWidthPx.toDp() }
  val thumbnailHeightDp = with(density) { thumbnailHeightPx.toDp() }

  val previewedAssetContainerHeightPx = galleryViewModel.previewedAssetViewWrapSize.height.toInt()

  val scope = rememberCoroutineScope()
  val gridState = rememberLazyGridState()

  val gridFlingDecayAnimationSpec = rememberSplineBasedDecay<Float>()

  var isPreviewPlayable by remember { mutableStateOf(isPreviewEnabled) }
  val nestedScrollConnection = remember(previewedAssetContainerHeightPx) {
    GalleryViewContentNestedScrollConnection(
      previewedAssetContainerHeightPx = previewedAssetContainerHeightPx,
      scope = scope,
      gridState = gridState,
      /** LazyVerticalGrid uses [ScrollableDefaults.flingBehavior], which uses [rememberSplineBasedDecay] */
      gridFlingDecayAnimationSpec = gridFlingDecayAnimationSpec,
      gridNonThumbnailsItemsAmount = GRID_NON_THUMBNAILS_ITEMS_AMOUNT,
      gridColumnsAmount = COLUMNS_AMOUNT,
      gridItemHeightPx = thumbnailWidthPx,
      onPreviewedAssetWillHide = { isPreviewPlayable = false },
      onPreviewedAssetDidHide = { isPreviewPlayable = false },
      onPreviewedAssetDidShow = { isPreviewPlayable = true },
      onPreviewedAssetWillShow = { isPreviewPlayable = true },
    )
  }
  LaunchedEffect(isPreviewEnabled) {
    if (!isPreviewEnabled) nestedScrollConnection.hidePreviewedAsset()
  }

  fun scrollToAssetByIndex(
    index: Int,
    /** ignored if [isPreviewEnabled][GalleryViewModel.isPreviewEnabled] is `false` */
    shouldShowPreview: Boolean = true,
  ) {
    log { "scrollToAssetByIndex(index: $index, shouldShowPreview: $shouldShowPreview) | isPreviewEnabled: $isPreviewEnabled" }
    // minus 1 is to allow user to scroll backwards selecting first asset in a row
    val listItemIndex = index + GRID_NON_THUMBNAILS_ITEMS_AMOUNT - 1
    val stickyHeaderOffset = (GALLERY_VIEW_TOOLBAR_HEIGHT.value * density.density).toInt()

//            https://issuetracker.google.com/issues/240449680
//            https://issuetracker.google.com/issues/203855802
    if (isPreviewEnabled && shouldShowPreview) nestedScrollConnection.showPreviewedAsset()

    scope.launch {
      gridState.scroll {
        val distanceToSelectedItem = LazyLayoutScrollScope(gridState, this).calculateDistanceTo(
          listItemIndex, -stickyHeaderOffset
        )

        val animatable = Animatable(0F)
        var previouslyScrolledDistance = 0F

        animatable.animateTo(
          targetValue = distanceToSelectedItem.toFloat(),
          animationSpec = AUTO_SCROLL_FLOAT_ANIMATION_SPEC
        ) {
          val delta = this.value - previouslyScrolledDistance
          gridState.dispatchRawDelta(delta)
          previouslyScrolledDistance = this.value
        }
      }
    }
  }

  LaunchedEffect(galleryViewModel.nextPreviewedAsset) {
    if (galleryViewModel.nextPreviewedAsset == null) return@LaunchedEffect

    val previewedAssetIndex =
      galleryViewModel.selectedAlbumAssetsMap.values.indexOf(galleryViewModel.nextPreviewedAsset)
    scrollToAssetByIndex(previewedAssetIndex)
  }

  Box(
    Modifier
      .fillMaxSize()
      .conditional(isPreviewEnabled) { nestedScroll(nestedScrollConnection) }) {
    galleryViewModel.previewedAsset?.let {
      TransformableAssetView(
        asset = it,
        modifier = Modifier.offset {
          IntOffset(0, nestedScrollConnection.previewedAssetOffset)
        },
        isPlayable = isPreviewPlayable,
      )
    }

    LazyVerticalGrid(
      state = gridState,
      columns = GridCells.Fixed(COLUMNS_AMOUNT),
      horizontalArrangement = Arrangement.spacedBy(2.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
      contentPadding = PaddingValues(
        bottom = galleryViewModel.innerPaddings.calculateBottomPadding()
      ),
      modifier = Modifier.offset {
        IntOffset(
          x = 0,
          y = previewedAssetContainerHeightPx + nestedScrollConnection.previewedAssetOffset
        )
      }
    ) {
      stickyHeader(key = TOOLBAR_ITEM_KEY) { GalleryViewToolbar() }

      item(key = CAMERA_BUTTON_ITEM_KEY) {
        CameraSheetButton(
          modifier = Modifier
            .size(width = thumbnailWidthDp, height = thumbnailHeightDp)
            .background(Color.Black),
          onSheetGoingToDisplay = {
            scope.launch {
              // todo: pause before request
              /**
               * Delay is required to Request Pause after Lifecycle-based Play Request
               * (Permissions Request pauses current Lifecycle)
               */
              delay(10)
              galleryViewModel.exoPlayerController.pause()
            }
          },
          onSheetDidDismiss = galleryViewModel.exoPlayerController::play,
          enabled = !galleryViewModel.isMultiselectEnabled,
          isImageCapturingEnabled = galleryViewModel.allowedAssetsTypes.contains(GalleryAssetType.Image),
          isVideoRecordingEnabled = galleryViewModel.allowedAssetsTypes.contains(GalleryAssetType.Video),
          onDidTakePicture = {
            galleryViewModel.emitCapturedImage(it) {
              delay(30)
              galleryViewModel.populateAllAssetsMap()
            }
          },
          onDidRecordVideo = {
            galleryViewModel.emitRecordedVideo(it) {
              delay(30)
              galleryViewModel.populateAllAssetsMap()
            }
          },
        )
      }

      val assetsToShow = galleryViewModel.selectedAlbumAssetsMap.values.toList()
      if (assetsToShow.isEmpty()) item(span = { GridItemSpan(3) }) {
        Box(
          modifier = Modifier.offset(y = 34.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            "Album has no suitable assets",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
          )
        }
      }
      else itemsIndexed(assetsToShow, key = { _, item -> item.id }) { index, asset ->
        AssetThumbnailView(
          asset = asset,
          widthDp = thumbnailWidthDp,
          heightDp = thumbnailHeightDp,
        ) {
          if (asset == galleryViewModel.previewedAsset) {
            scrollToAssetByIndex(index)
          }
          galleryViewModel.onThumbnailClick(asset)
        }
      }
    }

    NeuroStoriesView(galleryViewModel.selectedCreativityType == CreativityType.NeuroStory)

    var scrollToPreviewedAssetJob by remember { mutableStateOf<Job?>(null) }
    CreativityTypeSelector { selected: CreativityType, isByClick ->
      log { "selected (isByClick: $isByClick) creativityType ($selected) (lastSelected is: ${galleryViewModel.selectedCreativityType})" }

      galleryViewModel.selectedCreativityType = selected

      scrollToPreviewedAssetJob?.cancel()
      scrollToPreviewedAssetJob = scope.launch {
        // todo: depend on device Performance Class
        if (!isByClick) delay(30)

        if (selected != galleryViewModel.selectedCreativityType) return@launch

        val assetToScrollToIndex = galleryViewModel.selectedAlbumAssetsMap.values.indexOf(
          galleryViewModel.nextPreviewedAsset ?: galleryViewModel.previewedAsset
        )
        scrollToAssetByIndex(assetToScrollToIndex, false)
      }
    }
  }
}
