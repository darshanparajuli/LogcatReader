package com.dp.logcatapp.util

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

  suspend fun run(): Boolean = coroutineScope {
    var dispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
    try {
      val processBuilder = ProcessBuilder("su")
      val process = withContext(dispatcher) { processBuilder.start() }

      val stdoutWriter = BufferedWriter(OutputStreamWriter(process.outputStream))
      val stdinReader = BufferedReader(InputStreamReader(process.inputStream))

      val marker = "RESULT>>>${UUID.randomUUID()}>>>"

      val stdoutResult = async(dispatcher) {
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
        } catch (_: Exception) {
        }

        result
      }

      stdoutWriter.writeCmd(cmd)
      stdoutWriter.writeCmd("echo \"$marker$?\"")

      val finalResult = stdoutResult.await()

      stdoutWriter.writeCmd("exit")

      withContext(dispatcher) {
        process.waitFor()
        process.destroy()
      }

      finalResult
    } catch (_: Exception) {
      false
    } finally {
      try {
        dispatcher.close()
      } catch (_: Exception) {
      }
    }
  }
}