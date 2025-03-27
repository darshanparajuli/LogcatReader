package com.dp.logcat

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.dp.logger.Logger
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object LogcatUtil {
  private val POSSIBLE_BUFFERS = setOf(
    "main", "system", "radio", "events", "crash", "security", "kernel",
  )

  val DEFAULT_BUFFERS: Set<String> by lazy {
    getDefaultBuffers().also { buffers ->
      Logger.debug(LogcatUtil::class, "Default buffers: $buffers")
    }
  }
  val AVAILABLE_BUFFERS: Array<String> by lazy {
    getAvailableBuffers().also { buffers ->
      Logger.debug(LogcatUtil::class, "Available buffers: ${buffers.contentToString()}")
    }
  }

  private const val LOG_FILE_HEADER_FMT = "<<< log_count = %d >>>"

  fun getLogCountFromHeader(file: File): Long {
    try {
      return getLogCountFromHeader(FileInputStream(file))
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return -1L
  }

  fun getLogCountFromHeader(
    context: Context,
    file: DocumentFile
  ): Long {
    try {
      val fis = context.contentResolver.openInputStream(file.uri)
      return getLogCountFromHeader(fis!!)
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return -1L
  }

  private fun getLogCountFromHeader(inputStream: InputStream): Long {
    var reader: BufferedReader? = null
    try {
      reader = BufferedReader(InputStreamReader(inputStream))
      val header = reader.readLine()
      if (header.startsWith("<<<")) {
        var startIndex = header.indexOf('=')
        if (startIndex != -1) {
          startIndex += 2
          val endIndex = header.indexOf(' ', startIndex)
          if (endIndex != -1) {
            val value = header.substring(startIndex, endIndex)
            return value.toLong()
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      reader?.close()
    }
    return -1L
  }

  fun writeToFile(
    logs: List<Log>,
    file: File
  ): Boolean {
    var writer: BufferedWriter? = null
    return try {
      writer = BufferedWriter(FileWriter(file, false))
      writeToFileHelper(logs, writer)
      true
    } catch (e: IOException) {
      e.printStackTrace()
      false
    } finally {
      writer?.close()
    }
  }

  fun writeToFile(
    context: Context,
    logs: List<Log>,
    uri: Uri
  ): Boolean {
    var writer: BufferedWriter? = null
    return try {
      val fos = context.contentResolver.openOutputStream(uri)
      writer = BufferedWriter(OutputStreamWriter(fos))
      writeToFileHelper(logs, writer)
      true
    } catch (e: IOException) {
      e.printStackTrace()
      false
    } finally {
      writer?.close()
    }
  }

  private fun writeToFileHelper(
    logs: List<Log>,
    writer: BufferedWriter
  ) {
    writer.write(LOG_FILE_HEADER_FMT.format(logs.size))
    writer.newLine()
    for (log in logs) {
      writer.write(log.toString())
    }
    writer.flush()
  }

  private fun getDefaultBuffers(): Set<String> {
    val result = mutableSetOf<String>()

    val stdoutList = mutableListOf<String>()
    CommandUtils.runCmd(cmd = listOf("logcat", "-g"), stdoutList = stdoutList)

    for (s in stdoutList) {
      val colonIndex = s.indexOf(":")
      if (colonIndex != -1) {
        if (s.startsWith("/")) {
          val sub = s.substring(0, colonIndex)
          val lastSlashIndex = sub.lastIndexOf("/")
          if (lastSlashIndex != -1) {
            result += sub.substring(lastSlashIndex + 1)
          }
        } else {
          result += s.substring(0, colonIndex)
        }
      }
    }

    return result
  }

  private fun getAvailableBuffers(): Array<String> {
    val stdoutList = mutableListOf<String>()
    CommandUtils.runCmd(
      cmd = listOf("logcat", "-h"),
      stdoutList = stdoutList, redirectStderr = true
    )

    val bufferHelpIndex = stdoutList.indexOfFirst { it.trim().startsWith("-b") }
    var bufferHelpEndIndex = bufferHelpIndex
    if (bufferHelpIndex != -1) {
      bufferHelpEndIndex += 1
      while (bufferHelpEndIndex < stdoutList.size && !stdoutList[bufferHelpEndIndex].trim()
          .startsWith("-")
      ) {
        bufferHelpEndIndex += 1
      }

      val stdout = stdoutList.subList(bufferHelpIndex, bufferHelpEndIndex)
        .joinToString(separator = "\n")
      return POSSIBLE_BUFFERS.filter { buffer ->
        stdout.contains(buffer)
      }.toTypedArray()
        .sortedArray()
    }

    return emptyArray()
  }
}