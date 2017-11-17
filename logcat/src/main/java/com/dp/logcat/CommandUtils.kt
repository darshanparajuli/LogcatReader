package com.dp.logcat

import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.concurrent.thread

object CommandUtils {

    fun runCmd(vararg cmd: String, stdoutList: MutableList<String>? = null,
               stderrList: MutableList<String>? = null, redirectStderr: Boolean = false): Int {
        val processBuilder = ProcessBuilder(*cmd)
                .redirectErrorStream(redirectStderr)
        var process: Process? = null
        try {
            process = processBuilder.start()
            val stderrstream = process.errorStream
            val stdoutStream = process.inputStream

            val stdoutThread = thread {
                val reader: BufferedReader
                try {
                    reader = BufferedReader(InputStreamReader(stdoutStream))
                    while (true) {
                        val line = reader.readLine() ?: break
                        stdoutList?.add(line)
                    }
                } catch (e: Exception) {
                }
            }
            val stderrThread = thread {
                val reader: BufferedReader
                try {
                    reader = BufferedReader(InputStreamReader(stderrstream))
                    while (true) {
                        val line = reader.readLine() ?: break
                        stderrList?.add(line)
                    }
                } catch (e: Exception) {
                }
            }

            val exitCode = process.waitFor()

            try {
                stderrThread.join(1000)
            } catch (e: InterruptedException) {
            }

            try {
                stdoutThread.join(1000)
            } catch (e: InterruptedException) {
            }

            return exitCode
        } catch (e: Exception) {
            return -1
        } finally {
            process?.destroy()
        }
    }

}