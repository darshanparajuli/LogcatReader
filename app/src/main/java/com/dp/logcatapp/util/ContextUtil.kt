package com.dp.logcatapp.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun Context.findActivity(): Activity? {
  return when (this) {
    is Activity -> this
    is ContextWrapper -> this.baseContext.findActivity()
    else -> null
  }
}