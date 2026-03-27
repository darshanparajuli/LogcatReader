package com.dp.logcatapp.util

import java.io.Closeable
import java.io.IOException

fun Closeable.closeQuietly() {
  try {
    close()
  } catch (_: IOException) {
    // shhhh! ;)
  }
}
