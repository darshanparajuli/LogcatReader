package com.dp.logcat

import android.os.Handler
import android.os.Looper
import java.io.*

class Logcat : Closeable {
    private var threadLogcat: Thread? = null
    private var logcatProcess: Process? = null
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var listener: LogcatEventListener? = null
    private val logs = mutableListOf<Log>()
    private val filters = mutableMapOf<String, (Log) -> Boolean>()

    fun start(listener: LogcatEventListener?) {
        threadLogcat ?: throw IllegalStateException("logcat is already running")
        this.listener = listener

        threadLogcat = Thread({ runLogcat() })
        threadLogcat?.start()
    }

    fun getLogs(): List<Log> {
        val list = mutableListOf<Log>()
        synchronized(logs) {
            list += logs.toList()
        }
        return list
    }

    fun getLogsFiltered() = getLogs().filter { log -> filters.values.all { it(log) } }

    fun addFilter(name: String, filter: (Log) -> Boolean) = filters.put(name, filter)

    fun removeFilter(name: String) = filters.remove(name)

    override fun close() {
        logcatProcess?.destroy()

        try {
            threadLogcat?.join(300)
        } catch (e: InterruptedException) {
        }

        threadLogcat = null
        logcatProcess = null

        logs.clear()
        filters.clear()
        listener = null
    }
    
    private fun runLogcat() {
        val processBuilder = ProcessBuilder("logcat", "-v", "long")

        try {
            logcatProcess = processBuilder.start()
        } catch (e: IOException) {
            handler.post { listener?.onStartFailedEvent() }
            return
        }

        handler.post { listener?.onStartEvent() }

        val stderrThread = Thread({ processStderr(logcatProcess?.errorStream) })
        stderrThread.start()

        val stdoutThread = Thread({ processStdout(logcatProcess?.inputStream) })
        stdoutThread.start()

        logcatProcess?.waitFor()

        try {
            stderrThread.join(2000)
        } catch (e: InterruptedException) {
        }
        try {
            stdoutThread.join(2000)
        } catch (e: InterruptedException) {
        }

        handler.post { listener?.onStopEvent() }
    }

    private fun processStderr(errStream: InputStream?) {
        val reader = BufferedReader(InputStreamReader(errStream))
        while (true) {
            try {
                reader.readLine() ?: break
            } catch (e: IOException) {
                break
            }
        }
    }

    private fun processStdout(inputStream: InputStream?) {
        val reader = BufferedReader(InputStreamReader(inputStream))
        while (true) {
            try {
                val metadata = reader.readLine()?.trim() ?: break
                if (metadata.startsWith("[")) {
                    val msg = reader.readLine()?.trim() ?: break
                    val log = LogFactory.createNewLog(metadata, msg)
                    synchronized(logs) {
                        logs += log
                    }

                    if (filters.values.all { it(log) }) {
                        listener?.onLogEvent(log)
                    }
                }
            } catch (e: IOException) {
                break
            }
        }
    }
}

