package com.dp.logcat

import android.os.Handler
import android.os.Looper
import java.io.*
import kotlin.concurrent.thread

class Logcat : Closeable {
    private var threadLogcat: Thread? = null
    private var logcatProcess: Process? = null
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var listener: LogcatEventListener? = null

    // must be synchronized
    private val lock = Any()
    private val logs = mutableListOf<Log>()
    private val filters = mutableMapOf<String, (Log) -> Boolean>()

    fun start() {
        if (threadLogcat != null) {
            throw IllegalStateException("logcat is already running")
        }

        threadLogcat = thread { runLogcat() }
    }

    fun setEventListener(listener: LogcatEventListener) {
        this.listener = listener
    }

    fun getLogs(): List<Log> {
        val list = mutableListOf<Log>()
        synchronized(lock) {
            list += logs.toList()
        }
        return list
    }

    fun getLogsFiltered(): List<Log> {
        val logs = getLogs()
        synchronized(lock) {
            return logs.filter { log -> filters.values.all { it(log) } }
        }
    }

    fun addFilter(name: String, filter: (Log) -> Boolean) {
        synchronized(lock) {
            filters.put(name, filter)
        }
    }

    fun removeFilter(name: String) {
        synchronized(lock) {
            filters.remove(name)
        }
    }

    fun stop() {
        logcatProcess?.destroy()

        try {
            threadLogcat?.join(300)
        } catch (e: InterruptedException) {
        }

        threadLogcat = null
        logcatProcess = null

        synchronized(lock) {
            logs.clear()
            filters.clear()
        }
    }

    override fun close() {
        stop()
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

        val stderrThread = thread { processStderr(logcatProcess?.errorStream) }
        val stdoutThread = thread { processStdout(logcatProcess?.inputStream) }

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
                    try {
                        val log = LogFactory.createNewLog(metadata, msg)
                        var passedFilter = false
                        synchronized(lock) {
                            logs += log
                            passedFilter = filters.values.all { it(log) }
                        }

                        if (passedFilter) {
                            listener?.onLogEvent(log)
                        }

                        // skip next line since it's empty
                        reader.readLine() ?: break
                    } catch (e: Exception) {
                        android.util.Log.d("LogcatLib", "${e.message}: $metadata")
                    }
                }
            } catch (e: IOException) {
                break
            }
        }
    }
}

