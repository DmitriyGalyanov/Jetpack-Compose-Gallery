package com.dgalyanov.gallery.ui.galleryView.galleryViewContent.previewedAssetView

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.dgalyanov.gallery.R
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.ui.theme.withCoercedFontScaleForText

private val SELECTOR_WIDTH = 72.dp
private val SELECTOR_BORDER_RADIUS = 6.dp
private const val BUTTON_PADDING_VERTICAL = 8
private val SELECTOR_BACKGROUND_COLOR = Color(0, 0, 0, 178)
private val SELECTOR_PADDING_START = 12.dp
private val SELECTOR_PADDING_END = 12.dp
private val BUTTON_PADDING_VALUES = PaddingValues(
  top = BUTTON_PADDING_VERTICAL.dp, bottom = BUTTON_PADDING_VERTICAL.dp,
  start = SELECTOR_PADDING_START,
  end = SELECTOR_PADDING_END,
)

@Composable
private fun Modifier.buttonModifier(onClick: () -> Unit): Modifier {
  val interactionSource = remember { MutableInteractionSource() }

  val buttonModifier = Modifier
    .width(SELECTOR_WIDTH)
    .clip(RoundedCornerShape(SELECTOR_BORDER_RADIUS))
    .indication(interactionSource = interactionSource, indication = ripple(true))
    .background(SELECTOR_BACKGROUND_COLOR)
    .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    .padding(BUTTON_PADDING_VALUES)

  return this.then(buttonModifier)
}

private const val FONT_SIZE = 12
private val FONT_WEIGHT = FontWeight.W500

@Composable
private fun AspectRatioSelectorText(
  text: String,
  modifier: Modifier = Modifier,
  shouldComposeDivider: Boolean = false,
) {
  Column(
    Modifier
      .width(SELECTOR_WIDTH)
      .then(modifier)
  ) {
    Text(
      text = text,
      fontSize = FONT_SIZE.withCoercedFontScaleForText(),
      fontWeight = FONT_WEIGHT,
    )
    if (shouldComposeDivider) {
      Spacer(Modifier.height(4.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(Color.Gray)
      )
    }
  }
}

@Composable
internal fun AspectRatioSelectorView(isVisible: Boolean) {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current
  var isExpanded by remember { mutableStateOf(false) }

  val density = LocalDensity.current.density
  var toggleExpansionButtonHeight by remember { mutableLongStateOf(0) }

  AnimatedVisibility(
    isVisible,
    enter = fadeIn(animationSpec = GalleryViewModel.PREVIEWED_ASSET_SELECTION_RELATED_ANIMATIONS_SPEC),
    exit = fadeOut(animationSpec = GalleryViewModel.PREVIEWED_ASSET_SELECTION_RELATED_ANIMATIONS_SPEC),
    modifier = Modifier.absoluteOffset(x = 12.dp, y = 12.dp),
  ) {
    galleryViewModel.usedAspectRatio.heightToWidthKey.let {
      Box(
        modifier = Modifier
          .buttonModifier { isExpanded = !isExpanded }
          .onSizeChanged {
            toggleExpansionButtonHeight =
              (it.height + BUTTON_PADDING_VERTICAL * 2 * density).toLong()
          }
      ) {
        Row(
          Modifier.width(SELECTOR_WIDTH),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = it,
            fontSize = FONT_SIZE.withCoercedFontScaleForText(),
            fontWeight = FONT_WEIGHT,
          )

          Spacer(Modifier.width(8.dp))

          val iconRotationDegree by animateFloatAsState(
            if (isExpanded) 180f else 0f,
            animationSpec = tween(durationMillis = 300),
          )
          Icon(
            contentDescription = "thin bodyless arrow pointing down",
            painter = painterResource(R.drawable.bodyless_arrow_down_thin),
            tint = LocalContentColor.current,
            modifier = Modifier
              .requiredSize(20.dp)
              .graphicsLayer {
                rotationX = iconRotationDegree
              },
          )
        }
      }

      Popup(
        onDismissRequest = { if (isExpanded) isExpanded = false },
        offset = IntOffset(toggleExpansionButtonHeight + 4),
        /** workaround to not re-expand when clicked on toggle button when expanded */
        properties = PopupProperties(focusable = isExpanded)
      ) {
        AnimatedVisibility(
          isExpanded,
          enter = expandVertically(),
          exit = shrinkVertically(),
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SELECTOR_BACKGROUND_COLOR)
        ) {
          Column {
            galleryViewModel.availableAspectRatios.forEachIndexed { index, aspectRatio ->
              val isFirst = index == 0
              val isLast = index == galleryViewModel.availableAspectRatios.size - 1
              AspectRatioSelectorText(
                text = aspectRatio.heightToWidthKey,
                modifier = Modifier
                  .clickable {
                    galleryViewModel.userSelectedAspectRatio = aspectRatio
                    isExpanded = false
                  }
                  .padding(
                    top = (4 * (if (isFirst) 2 else 1)).dp,
                    bottom = (4 * (if (isLast) 2 else 1)).dp,
                    start = SELECTOR_PADDING_START,
                    end = SELECTOR_PADDING_END
                  ),
                shouldComposeDivider = !isLast,
              )
            }
          }
        }
      }
    }
  }
}