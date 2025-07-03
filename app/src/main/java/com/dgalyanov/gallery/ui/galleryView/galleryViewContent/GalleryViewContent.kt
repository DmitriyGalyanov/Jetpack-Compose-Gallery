package com.dgalyanov.gallery.ui.galleryView.galleryViewContent

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyLayoutScrollScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dgalyanov.gallery.dataClasses.AssetAspectRatio
import com.dgalyanov.gallery.dataClasses.CreativityType
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.dataClasses.GalleryAsset
import com.dgalyanov.gallery.ui.galleryView.CreativityTypeSelector
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.assetThumbnailView.AssetThumbnailView
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewContentCameraItem.CameraSheetButton
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewToolbar.GALLERY_VIEW_TOOLBAR_HEIGHT
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewToolbar.GalleryViewToolbar
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.previewedAssetView.TransformableAssetView
import com.dgalyanov.gallery.ui.utils.modifiers.conditional
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val COLUMNS_AMOUNT = 3

private const val TOOLBAR_ITEM_KEY = "Toolbar"
private const val CAMERA_BUTTON_ITEM_KEY = "Camera"
private const val GRID_NON_THUMBNAILS_ITEMS_AMOUNT = 2

@Composable
internal fun GalleryViewContent(
  assets: List<GalleryAsset>, thumbnailAspectRatio: AssetAspectRatio, isPreviewEnabled: Boolean
) {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current

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
      onPreviewedAssetDidHide = galleryViewModel.exoPlayerController::pause,
      onPreviewedAssetDidUnhide = galleryViewModel.exoPlayerController::play,
    )
  }

  fun scrollToAssetByIndex(
    index: Int,
    /** ignored if [isPreviewEnabled] is `false` */
    shouldShowPreview: Boolean = true,
  ) {
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

    val previewedAssetIndex = assets.indexOf(galleryViewModel.nextPreviewedAsset)
    scrollToAssetByIndex(previewedAssetIndex)
  }

  Box(Modifier.conditional(isPreviewEnabled) { nestedScroll(nestedScrollConnection) }) {
    if (isPreviewEnabled) {
      galleryViewModel.previewedAsset?.let {
        TransformableAssetView(asset = it, Modifier.offset {
          IntOffset(0, nestedScrollConnection.previewedAssetOffset)
        })
      }
    }

    Column {
      if (isPreviewEnabled) {
        InnerSpacer(
          previewedAssetContainerHeightPx = previewedAssetContainerHeightPx,
          nestedScrollConnection = nestedScrollConnection
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
      ) {
        stickyHeader(key = TOOLBAR_ITEM_KEY) { GalleryViewToolbar() }

        item(key = CAMERA_BUTTON_ITEM_KEY) {
          CameraSheetButton(
            modifier = Modifier.size(width = thumbnailWidthDp, height = thumbnailHeightDp),
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
          )
        }

        itemsIndexed(assets, key = { _, item -> item.id }) { index, asset ->
          AssetThumbnailView(
            asset = asset, widthDp = thumbnailWidthDp, heightDp = thumbnailHeightDp
          ) {
            if (asset == galleryViewModel.previewedAsset) {
              scrollToAssetByIndex(index)
            }
            galleryViewModel.onThumbnailClick(asset)
          }
        }
      }
    }

    CreativityTypeSelector { selected: CreativityType, isByClick ->
      val assetToScrollToIndex = if (isByClick) {
        if (selected == galleryViewModel.selectedCreativityType) 0
        else {
          assets.indexOf(
            galleryViewModel.nextPreviewedAsset ?: galleryViewModel.previewedAsset
          )
        }
      } else -1

      galleryViewModel.selectedCreativityType = selected

      scope.launch {
        // todo: depend on device Performance Class
        delay(100)
        if (assetToScrollToIndex != -1) scrollToAssetByIndex(assetToScrollToIndex, false)
      }
    }
  }
}

@Composable
private fun InnerSpacer(
  previewedAssetContainerHeightPx: Int,
  nestedScrollConnection: GalleryViewContentNestedScrollConnection
) {
  val density = LocalDensity.current

  val spacerHeight by remember(density) {
    derivedStateOf {
      with(density) {
        (previewedAssetContainerHeightPx + nestedScrollConnection.previewedAssetOffset).toDp()
      }
    }
  }

  Spacer(Modifier.height(spacerHeight))
}

