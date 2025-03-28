package com.dp.logcatapp.util

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

fun InputStream.closeQuietly() {
  try {
    close()
  } catch (_: IOException) {
  }
}

fun OutputStream.closeQuietly() {
  try {
    close()
  } catch (_: IOException) {
  }
}