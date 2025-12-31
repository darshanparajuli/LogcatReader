package com.dp.logcat

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.dp.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
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

  suspend fun countLogs(file: File): Long = withContext(Dispatchers.IO) {
    try {
      LogcatStreamReader(FileInputStream(file))
        .asSequence().count().toLong()
    } catch (_: IOException) {
      0L
    } catch (_: SecurityException) {
      0L
    }
  }

  suspend fun countLogs(
    context: Context,
    file: DocumentFile
  ): Long = withContext(Dispatchers.IO) {
    try {
      val inputStream = context.contentResolver.openInputStream(file.uri)
      val reader = LogcatStreamReader(inputStream!!)
      reader.asSequence().count().toLong()
    } catch (_: IOException) {
      0L
    } catch (_: SecurityException) {
      0L
    }
  }

  private fun writeToFileHelper(
    logs: List<Log>,
    writer: BufferedWriter
  ) {
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