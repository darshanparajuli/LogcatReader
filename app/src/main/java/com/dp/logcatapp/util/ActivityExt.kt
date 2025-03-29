package com.dp.logcatapp.util

import android.app.Activity
import android.view.WindowManager

fun Activity.setKeepScreenOn(enabled: Boolean) {
  if (enabled) {
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  } else {
    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  }
}
