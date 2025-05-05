package com.dp.logcat

import android.os.Parcelable
import androidx.core.text.isDigitsOnly
import kotlinx.parcelize.Parcelize

@Parcelize
data class Log(
  val id: Int,
  val date: String,
  val time: String,
  val uid: Uid?,
  val pid: String,
  val tid: String,
  val priority: String,
  val tag: String,
  val msg: String,
) : Parcelable {

  @Parcelize
  data class Uid(
    val value: String,
  ) : Parcelable {
    val isNum = value.isDigitsOnly()
  }

  fun metadataToString() = "[$date $time $uid:$pid:$tid $priority/$tag]"

  override fun toString(): String = "${metadataToString()}\n$msg\n\n"

  companion object {
    fun parse(
      id: Int,
      metadata: String,
      msg: String,
    ): Log {
      val date: String
      val time: String
      val uid: String?
      val pid: String
      val tid: String
      val priority: String
      val tag: String

      val trimmedMetadata = metadata.substring(1, metadata.length - 1).trim()
      var startIndex = 0

      var index = trimmedMetadata.indexOf(' ', startIndex)
      date = trimmedMetadata.substring(startIndex, index)
      startIndex = index + 1

      index = trimmedMetadata.indexOf(' ', startIndex)
      time = trimmedMetadata.substring(startIndex, index)
      startIndex = index + 1

      // NOTE(dparajuli): skip spaces
      while (trimmedMetadata[startIndex] == ' ') {
        startIndex++
      }

      val hasUid = trimmedMetadata.substring(
        startIndex = startIndex,
        endIndex = trimmedMetadata.indexOf(char = '/', startIndex = startIndex)
      ).count { it == ':' } == 2
      if (hasUid) {
        index = trimmedMetadata.indexOf(':', startIndex)
        uid = trimmedMetadata.substring(startIndex, index)
        startIndex = index + 1

        // NOTE(dparajuli): skip spaces
        while (trimmedMetadata[startIndex] == ' ') {
          startIndex++
        }
      } else {
        uid = null
      }

      index = trimmedMetadata.indexOf(':', startIndex)
      pid = trimmedMetadata.substring(startIndex, index)
      startIndex = index + 1

      // NOTE(dparajuli): skip spaces
      while (trimmedMetadata[startIndex] == ' ') {
        startIndex++
      }

      index = trimmedMetadata.indexOf(' ', startIndex)
      tid = trimmedMetadata.substring(startIndex, index)
      startIndex = index + 1

      index = trimmedMetadata.indexOf('/', startIndex)
      priority = trimmedMetadata.substring(startIndex, index)
      startIndex = index + 1

      tag = trimmedMetadata.substring(startIndex, trimmedMetadata.length).trim()

      return Log(
        id = id,
        date = date,
        time = time,
        uid = uid?.let { Uid(it) },
        pid = pid,
        tid = tid,
        priority = priority,
        tag = tag,
        msg = msg
      )
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
