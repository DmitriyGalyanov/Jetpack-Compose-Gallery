package com.dgalyanov.gallery.ui.galleryView

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dgalyanov.gallery.dataClasses.CreativityType
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.ui.utils.modifiers.conditional
import com.dgalyanov.gallery.utils.GalleryLogFactory
import kotlinx.coroutines.launch

private val log = GalleryLogFactory("CreativityTypeSelector")

@Composable
internal fun BoxScope.CreativityTypeSelector(
  onDidSelect: (selected: CreativityType, isByClick: Boolean) -> Unit,
) {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current

  if (galleryViewModel.availableCreativityTypes.size < 2) return

  val scope = rememberCoroutineScope()

  val itemsOffsetsX = remember { mutableStateMapOf<CreativityType, Float>() }

  val baseOffset = galleryViewModel.windowWidthPx / 2
  val anchors by remember {
    derivedStateOf {
      DraggableAnchors {
        galleryViewModel.availableCreativityTypes.map {
          it at (baseOffset - (itemsOffsetsX[it] ?: 0f))
        }
      }
    }
  }
  val draggableState = remember {
    AnchoredDraggableState(
      initialValue = galleryViewModel.selectedCreativityType,
      anchors = anchors,
    )
  }
  var didSetAnchors by remember { mutableStateOf(false) }
  LaunchedEffect(anchors) {
    log { "updating anchors from ${draggableState.anchors} to $anchors" }
    draggableState.updateAnchors(anchors)
    didSetAnchors = true
  }
  LaunchedEffect(draggableState.targetValue) {
    if (didSetAnchors) onDidSelect(draggableState.targetValue, false)
  }

  val itemInteractionSource = remember { MutableInteractionSource() }

  val overscrollEffect = rememberOverscrollEffect()

  Row(
    modifier = Modifier
      .align(Alignment.BottomStart)
      .offset {
        IntOffset(
          x = draggableState.requireOffset().toInt(),
          y = (-galleryViewModel.innerStaticPaddings.bottom - 12.dp).toPx().toInt()
        )
      }
      .clip(RoundedCornerShape(4.dp))
      .background(Color(0, 0, 0, 150))
      .anchoredDraggable(
        state = draggableState,
        orientation = Orientation.Horizontal,
        overscrollEffect = overscrollEffect,
//        flingBehavior
      )
      .overscroll(overscrollEffect)) {
    galleryViewModel.availableCreativityTypes.forEach { creativityType ->
      val isSelected = galleryViewModel.selectedCreativityType == creativityType

      Text(
        text = creativityType.name,
        modifier = Modifier
          .conditional(itemsOffsetsX[creativityType] == null) {
            onPlaced {
              val offset = it.positionInParent().x + it.size.width / 2
              log { "setting itemsOffsets[$creativityType] to $offset" }
              itemsOffsetsX[creativityType] = offset
            }
          }
          .padding(8.dp)
          .clickable(
            interactionSource = itemInteractionSource, indication = null
          ) {
            if (galleryViewModel.selectedCreativityType != creativityType) {
              scope.launch {
                draggableState.animateTo(targetValue = creativityType)
              }
            }

            onDidSelect(creativityType, true)
          },
        color = if (isSelected) Color.White else Color.LightGray,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
      )
    }
  }
}
