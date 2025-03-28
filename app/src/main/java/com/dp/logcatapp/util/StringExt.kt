package com.dp.logcatapp.util

import android.annotation.SuppressLint
import java.util.Locale

@SuppressLint("DefaultLocale")
fun String.containsIgnoreCase(other: String) =
  lowercase(Locale.getDefault()).contains(other.lowercase(Locale.getDefault()))
