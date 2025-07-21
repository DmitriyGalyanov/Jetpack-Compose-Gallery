package com.dgalyanov.gallery.ui.galleryView.galleryViewContent

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dgalyanov.gallery.R
import com.dgalyanov.gallery.galleryViewModel.GalleryViewModel
import com.dgalyanov.gallery.ui.theme.withCoercedFontScaleForText

private const val BANNER_ASPECT_RATIO = 106f / 478f

@Composable
internal fun NeuroStoriesView(getIsVisible: () -> Boolean) {
  val galleryViewModel = GalleryViewModel.LocalGalleryViewModel.current

  val bannersPainters = listOf(
    painterResource(R.drawable.neuro_stories_banner_0),
    painterResource(R.drawable.neuro_stories_banner_1),
    painterResource(R.drawable.neuro_stories_banner_2),
  )

  BoxWithConstraints(Modifier.fillMaxSize()) {
    val offsetX by animateFloatAsState(if (getIsVisible()) 0f else constraints.maxWidth.toFloat())

    Box(
      Modifier
        .fillMaxSize()
        .graphicsLayer { translationX = offsetX }
        .background(Color(0x0B, 0x0B, 0x0B))
        .padding(
          start = 12.dp,
          end = 12.dp,
          top = 16.dp,
          bottom = galleryViewModel.innerStaticPaddings.bottom + 62.dp,
        )
    ) {
      Box(
        Modifier
          .fillMaxSize()
          .clip(RoundedCornerShape(24.dp))
          .border(1.dp, Color(0x28, 0x28, 0x28), RoundedCornerShape(24.dp))
          .background(Color(0x21, 0x21, 0x21))
      ) {
        Row(
          Modifier.padding(horizontal = 15.dp, vertical = 15.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          bannersPainters.forEach {
            Image(
              painter = it,
              contentDescription = null,
              modifier = Modifier
                .weight(1f)
                .aspectRatio(BANNER_ASPECT_RATIO)
                .clip(RoundedCornerShape(20.dp))
            )
          }
        }

        Box(
          Modifier
            .fillMaxSize()
            .background(
              brush = Brush.verticalGradient(
                colors = listOf(
                  Color(0, 0, 0, 0),
                  Color(0, 0, 0, 255),
                )
              )
            )
        )

        Column(
          Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 12.dp, vertical = 24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "Create your set",
            fontSize = 18.withCoercedFontScaleForText(),
            letterSpacing = 0.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
          )

          Button(onClick = galleryViewModel.onNeuroStoriesProceedRequest) { Text("Proceed") }
        }
      }
    }
  }
}