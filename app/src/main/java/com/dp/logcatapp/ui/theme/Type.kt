package com.dp.logcatapp.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.font.FontWeight.Companion.Light
import androidx.compose.ui.text.font.FontWeight.Companion.Medium
import com.dp.logcatapp.R

val AppTypography = androidx.compose.material3.Typography()

val RobotoFontFamily = FontFamily(
  Font(resId = R.font.roboto_regular),
  Font(resId = R.font.roboto_italic, style = Italic),
  Font(resId = R.font.roboto_light, weight = Light),
  Font(resId = R.font.roboto_medium, weight = Medium),
  Font(resId = R.font.roboto_medium_italic, weight = Medium, style = Italic),
  Font(resId = R.font.roboto_bold, weight = Bold),
  Font(resId = R.font.roboto_bold_italic, weight = Bold, style = Italic),
)

val RobotoMonoFontFamily = FontFamily(
  Font(resId = R.font.roboto_mono_regular),
  Font(resId = R.font.roboto_mono_italic, style = Italic),
  Font(resId = R.font.roboto_mono_medium, weight = Medium),
  Font(resId = R.font.roboto_mono_medium_italic, weight = Medium, style = Italic),
  Font(resId = R.font.roboto_mono_bold, weight = Bold),
  Font(resId = R.font.roboto_mono_bold_italic, weight = Bold, style = Italic),
)

val RobotoCondensedFontFamily = FontFamily(
  Font(resId = R.font.roboto_condensed_regular),
  Font(resId = R.font.roboto_condensed_italic, style = Italic),
  Font(resId = R.font.roboto_condensed_bold, weight = Bold),
  Font(resId = R.font.roboto_condensed_bold_italic, weight = Bold, style = Italic),
)