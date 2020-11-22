package com.dp.logcatapp.util

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID
import java.util.concurrent.Executors

class SuCommander(private val cmd: String) {

  private fun BufferedWriter.writeCmd(cmd: String) {
    write(cmd)
    newLine()
    flush()
  }

  suspend fun run() = coroutineScope {
    try {
      val processBuilder = ProcessBuilder("su")
      val process = withContext(IO) { processBuilder.start() }

      val stdoutWriter = BufferedWriter(OutputStreamWriter(process.outputStream))
      val stdinReader = BufferedReader(InputStreamReader(process.inputStream))
      val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

      val marker = "RESULT>>>${UUID.randomUUID()}>>>"

      val stdoutStderrDispatcherContext = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
      val stdoutResult = async(stdoutStderrDispatcherContext) {
        var result = false

        try {
          while (true) {
            val line = stdinReader.readLine()?.trim() ?: break

            val index = line.indexOf(marker)
            if (index != -1) {
              result = line.substring(index + marker.length) == "0"
              break
            }
          }
        } catch (e: Exception) {
        }

        result
      }

      val stderrReaderResult = async(stdoutStderrDispatcherContext) {
        try {
          while (true) {
            stderrReader.readLine()?.trim() ?: break
          }
        } catch (e: Exception) {
        }
      }

      stdoutWriter.writeCmd(cmd)
      stdoutWriter.writeCmd("echo \"$marker$?\"")

      val finalResult = stdoutResult.await()

      stdoutWriter.writeCmd("exit")

      withContext(IO) { process.waitFor() }
      process.destroy()

      stderrReaderResult.await()

      finalResult
    } catch (e: Exception) {
      false
    }
  }
}