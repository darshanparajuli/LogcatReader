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
  val DEFAULT_BUFFERS: Set<String> by lazy {
    getDefaultBuffers().also { buffers ->
      Logger.debug(Logcat::class, "Default buffers: $buffers")
    }
  }
  val AVAILABLE_BUFFERS: Array<String> by lazy {
    getAvailableBuffers().also { buffers ->
      Logger.debug(Logcat::class, "Available buffers: ${buffers.contentToString()}")
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

    val helpText = getBufferHelpText(stdoutList)

    val buffers = mutableListOf<String>()
    if (helpText.firstOrNull()?.run {
        contains("request alternate ring buffer", ignoreCase = true) &&
          endsWith(":")
      } == true
    ) {
      if (helpText.size >= 2) {
        buffers += helpText[1].split(" ")
      }
    }

    val pattern = "'[a-z]+'".toRegex()
    for (s in helpText) {
      pattern.findAll(s).forEach { match ->
        match.value.let {
          buffers += it.substring(1, it.length - 1)
        }
      }
    }

    buffers -= "default"
    buffers -= "all"

    return buffers.toTypedArray().sortedArray()
  }

  private fun getBufferHelpText(stdout: List<String>): List<String> {
    val startPattern = "^\\s+-b,?.*<buffer>\\s+".toRegex()
    val start = stdout.indexOfFirst {
      startPattern.find(it)?.range?.start == 0
    }
    if (start == -1) {
      return emptyList()
    }

    val endPattern = "^\\s+-[a-zA-Z],?\\s+".toRegex()
    var end = stdout.subList(start + 1, stdout.size).indexOfFirst {
      endPattern.find(it)?.range?.start == 0
    }
    if (end == -1) {
      end = stdout.size
    } else {
      end += start + 1
    }

    return stdout.subList(start, end).map { it.trim() }.toList()
  }
}