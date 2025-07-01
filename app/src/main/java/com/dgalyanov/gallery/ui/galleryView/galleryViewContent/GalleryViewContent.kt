package com.dgalyanov.gallery.ui.galleryView.galleryViewContent

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyLayoutScrollScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.dataClasses.GalleryAsset
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.assetThumbnailView.AssetThumbnailView
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewContentCameraItem.CameraSheetButton
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewToolbar.GALLERY_VIEW_TOOLBAR_HEIGHT
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewToolbar.GalleryViewToolbar
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.previewedAssetView.PreviewedAssetView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val COLUMNS_AMOUNT = 3

private const val TOOLBAR_ITEM_KEY = "Toolbar"
private const val CAMERA_BUTTON_ITEM_KEY = "Camera"
private const val GRID_NON_THUMBNAILS_ITEMS_AMOUNT = 2

@Composable
internal fun GalleryViewContent(assets: List<GalleryAsset>) {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current

  val thumbnailSize = galleryViewModel.windowWidthDp.dp / COLUMNS_AMOUNT

  val density = LocalDensity.current

  val previewedAssetContainerHeightPx = galleryViewModel.previewedAssetContainerHeightPx

  val scope = rememberCoroutineScope()
  val gridState = rememberLazyGridState()

  val gridFlingDecayAnimationSpec = rememberSplineBasedDecay<Float>()

  val nestedScrollConnection =
    remember(previewedAssetContainerHeightPx) {
      GalleryViewContentNestedScrollConnection(
        previewedAssetContainerHeightPx = previewedAssetContainerHeightPx,
        scope = scope,
        gridState = gridState,
        /** LazyVerticalGrid uses [ScrollableDefaults.flingBehavior], which uses [rememberSplineBasedDecay] */
        gridFlingDecayAnimationSpec = gridFlingDecayAnimationSpec,
        gridNonThumbnailsItemsAmount = GRID_NON_THUMBNAILS_ITEMS_AMOUNT,
        gridColumnsAmount = COLUMNS_AMOUNT,
        gridItemHeightPx = (thumbnailSize * density.density).value.toInt(),
        onPreviewedAssetDidHide = galleryViewModel.exoPlayerController::pause,
        onPreviewedAssetDidUnhide = galleryViewModel.exoPlayerController::play,
      )
    }

  fun scrollToAssetByIndex(index: Int) {
    // minus 1 is to allow user to scroll backwards selecting first asset in a row
    val listItemIndex = index + GRID_NON_THUMBNAILS_ITEMS_AMOUNT - 1
    val stickyHeaderOffset = (GALLERY_VIEW_TOOLBAR_HEIGHT.value * density.density).toInt()

//            https://issuetracker.google.com/issues/240449680
//            https://issuetracker.google.com/issues/203855802
    nestedScrollConnection.showPreviewedAsset()

    scope.launch {
      gridState.scroll {
        val distanceToSelectedItem =
          LazyLayoutScrollScope(gridState, this).calculateDistanceTo(
            listItemIndex,
            -stickyHeaderOffset
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

  Box(Modifier.nestedScroll(nestedScrollConnection)) {
    PreviewedAssetView(Modifier.offset {
      IntOffset(0, nestedScrollConnection.previewedAssetOffset)
    })

    Column {
      InnerSpacer(
        previewedAssetContainerHeightPx = previewedAssetContainerHeightPx,
        nestedScrollConnection = nestedScrollConnection
      )

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
            modifier = Modifier
              .height(thumbnailSize)
              .fillMaxWidth(),
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

        items(assets, key = { it.id }) { asset ->
          AssetThumbnailView(
            asset = asset,
            size = thumbnailSize,
          ) {
            galleryViewModel.onThumbnailClick(asset)
          }
        }
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

