package com.dp.logcatapp.util

object Utils {
    fun sizeToString(size: Double): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var unit = units[0]
        var totalSize = size
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