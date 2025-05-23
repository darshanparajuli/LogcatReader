package com.dp.logcatapp.util

object Utils {
  fun bytesToString(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var unit = units[0]
    var totalSize = bytes.toDouble()
    for (i in 1 until units.size) {
      if (totalSize >= 1024) {
        totalSize /= 1024
        unit = units[i]
      } else {
        break
      }
    }

    return "%.2f %s".format(totalSize, unit)
  }
}
