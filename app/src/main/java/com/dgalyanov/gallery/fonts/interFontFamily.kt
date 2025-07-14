package com.dgalyanov.gallery.fonts

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.dgalyanov.gallery.R

/**
 * single Weight takes approximately 0.3 MB, only add (here and in [res.font][R.font]) required ones
 */
internal val interFontFamily = FontFamily(
//  Font(R.font.inter_thin, FontWeight.Thin),
//  Font(R.font.inter_extralight, FontWeight.ExtraLight),
//  Font(R.font.inter_light, FontWeight.Light),
  Font(R.font.inter, FontWeight.Normal), // required
  Font(R.font.inter_medium, FontWeight.Medium), // required
  Font(R.font.inter_semibold, FontWeight.SemiBold), // required
  Font(R.font.inter_bold, FontWeight.Bold), // required
//  Font(R.font.inter_extrabold, FontWeight.ExtraBold),
  Font(R.font.inter_black, FontWeight.Black), // required
)
