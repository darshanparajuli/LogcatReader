package com.dp.logcatapp.util

import android.os.Build
import android.os.Bundle
import android.os.Parcelable

inline fun <reified T : Parcelable> Bundle.getParcelableSafe(key: String?): T? {
  return if (Build.VERSION.SDK_INT >= 33) {
    getParcelable<T>(key, T::class.java)
  } else {
    @Suppress("DEPRECATION")
    getParcelable<T>(key)
  }
}