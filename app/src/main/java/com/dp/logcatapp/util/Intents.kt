package com.dp.logcatapp.util

import android.content.Intent
import android.os.Build
import android.os.Parcelable

inline fun <reified T : Parcelable> Intent.getParcelableExtraSafe(name: String): T? {
  return if (Build.VERSION.SDK_INT >= 33) {
    getParcelableExtra<T>(name, T::class.java)
  } else {
    @Suppress("DEPRECATION")
    getParcelableExtra<T>(name)
  }
}