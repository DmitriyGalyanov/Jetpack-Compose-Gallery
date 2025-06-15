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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
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
import com.dgalyanov.gallery.galleryContentResolver.dataClasses.GalleryMediaItem
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryMediaThumbnailView.GalleryMediaThumbnailView
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryPreviewedAssetView.GalleryPreviewedAssetView
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewContentCameraItem.CameraSheetButton
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewToolbar.GALLERY_VIEW_TOOLBAR_HEIGHT
import com.dgalyanov.gallery.ui.galleryView.galleryViewContent.galleryViewToolbar.GalleryViewToolbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val COLUMNS_AMOUNT = 3

private const val TOOLBAR_ITEM_KEY = "Toolbar"
private const val CAMERA_BUTTON_ITEM_KEY = "Camera"
private const val GRID_NON_THUMBNAILS_ITEMS_AMOUNT = 2

@Composable
internal fun GalleryViewContent(mediaItemsList: List<GalleryMediaItem>) {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current

  val thumbnailSize = galleryViewModel.windowWidthDp.dp / COLUMNS_AMOUNT

  val density = LocalDensity.current

  val previewedAssetHeightDp = galleryViewModel.windowWidthDp.dp
  val previewedAssetHeightPx = with(density) { previewedAssetHeightDp.roundToPx() }

  val scope = rememberCoroutineScope()
  val gridState = rememberLazyGridState()

  val gridFlingDecayAnimationSpec = rememberSplineBasedDecay<Float>()

  val nestedScrollConnection =
    remember(previewedAssetHeightPx) {
      GalleryViewContentNestedScrollConnection(
        previewedAssetHeightPx = previewedAssetHeightPx,
        scope = scope,
        gridState = gridState,
        /** LazyVerticalGrid uses [ScrollableDefaults.flingBehavior], which uses [rememberSplineBasedDecay] */
        gridFlingDecayAnimationSpec = gridFlingDecayAnimationSpec,
        gridNonThumbnailsItemsAmount = GRID_NON_THUMBNAILS_ITEMS_AMOUNT,
        gridColumnsAmount = COLUMNS_AMOUNT,
        gridItemHeightPx = (thumbnailSize * density.density).value.toInt(),
        onPreviewedAssetDidHide = galleryViewModel.exoPlayerHolder::pause,
        onPreviewedAssetDidUnhide = galleryViewModel.exoPlayerHolder::play,
      )
    }

  Box(Modifier.nestedScroll(nestedScrollConnection)) {
    GalleryPreviewedAssetView(Modifier.offset {
      IntOffset(
        0,
        nestedScrollConnection.previewedAssetOffset,
      )
    })

    Column {
      InnerSpacer(
        previewedAssetHeightPx = previewedAssetHeightPx,
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
                /**
                 * Delay is required to Request Pause after Lifecycle-based Play Request
                 * (Permissions Request pauses current Lifecycle)
                 */
                delay(10)
                galleryViewModel.exoPlayerHolder.pause()
              }
            },
            onSheetDidDismiss = galleryViewModel.exoPlayerHolder::play,
          )
        }

        itemsIndexed(mediaItemsList, key = { _, item -> item.id }) { index, item ->
          GalleryMediaThumbnailView(
            item = item,
            size = thumbnailSize,
          ) {
            // minus 1 is to allow user to scroll backwards selecting first item in a row
            val listItemIndex = index + GRID_NON_THUMBNAILS_ITEMS_AMOUNT - 1
            val stickyHeaderOffset = (GALLERY_VIEW_TOOLBAR_HEIGHT.value * density.density).toInt()

            galleryViewModel.onThumbnailClick(item)

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
        }
      }
    }
  }
}

@Composable
private fun InnerSpacer(
  previewedAssetHeightPx: Int,
  nestedScrollConnection: GalleryViewContentNestedScrollConnection
) {
  val density = LocalDensity.current

  val spacerHeight by remember(density) {
    derivedStateOf {
      with(density) {
        (previewedAssetHeightPx + nestedScrollConnection.previewedAssetOffset).toDp()
      }
    }
  }

  Spacer(Modifier.height(spacerHeight))
}

