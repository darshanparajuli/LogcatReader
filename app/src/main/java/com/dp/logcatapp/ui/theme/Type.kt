package com.dp.logcatapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.font.FontWeight.Companion.Medium
import com.dp.logcatapp.R

val AppTypography = Typography()

val RobotoMonoFontFamily = FontFamily(
  Font(resId = R.font.roboto_mono_regular),
  Font(resId = R.font.roboto_mono_italic, style = Italic),
  Font(resId = R.font.roboto_mono_medium, weight = Medium),
  Font(resId = R.font.roboto_mono_medium_italic, weight = Medium, style = Italic),
  Font(resId = R.font.roboto_mono_bold, weight = Bold),
  Font(resId = R.font.roboto_mono_bold_italic, weight = Bold, style = Italic),
)
