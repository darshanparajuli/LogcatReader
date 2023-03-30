package com.dp.logcat

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

private var logCounter = 0

@Parcelize
data class Log(
  val id: Int,
  val date: String,
  val time: String,
  val pid: String,
  val tid: String,
  val priority: String,
  val tag: String,
  val msg: String
) : Parcelable {

  fun metadataToString() = "[$date $time $pid:$tid $priority/$tag]"

  override fun toString(): String = "${metadataToString()}\n$msg\n\n"

  companion object {
    fun parse(
      metadata: String,
      msg: String
    ): Log {
      val date: String
      val time: String
      val pid: String
      val tid: String
      val priority: String
      val tag: String

      val trimmed = metadata.substring(1, metadata.length - 1).trim()
      var startIndex = 0

      var index = trimmed.indexOf(' ', startIndex)
      date = trimmed.substring(startIndex, index)
      startIndex = index + 1

      index = trimmed.indexOf(' ', startIndex)
      time = trimmed.substring(startIndex, index)
      startIndex = index + 1

      // NOTE(dparajuli): skip spaces
      while (trimmed[startIndex] == ' ') {
        startIndex++
      }

      index = trimmed.indexOf(':', startIndex)
      pid = trimmed.substring(startIndex, index)
      startIndex = index + 1

      // NOTE(dparajuli): skip spaces
      while (trimmed[startIndex] == ' ') {
        startIndex++
      }

      index = trimmed.indexOf(' ', startIndex)
      tid = trimmed.substring(startIndex, index)
      startIndex = index + 1

      index = trimmed.indexOf('/', startIndex)
      priority = trimmed.substring(startIndex, index)
      startIndex = index + 1

      tag = trimmed.substring(startIndex, trimmed.length).trim()

      return Log(logCounter++, date, time, pid, tid, priority, tag, msg)
    }
  }
}

object LogPriority {
  const val ASSERT = "A"
  const val DEBUG = "D"
  const val ERROR = "E"
  const val FATAL = "F"
  const val INFO = "I"
  const val VERBOSE = "V"
  const val WARNING = "W"
}
