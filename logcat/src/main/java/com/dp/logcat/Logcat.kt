package com.dp.logcat

import android.os.Handler
import android.os.Looper
import java.io.*

class Logcat : Closeable {
    private var threadLogcat: Thread? = null
    private var logcatProcess: Process? = null
    private var handler: Handler = Handler(Looper.getMainLooper())
    private var listener: LogcatEventListener? = null

    private val runnable: Runnable = Runnable {
        val processBuilder = ProcessBuilder("logcat", "-v", "long")

        try {
            logcatProcess = processBuilder.start()
        } catch (e: IOException) {
            handler.post { listener?.onFailEvent() }
            return@Runnable
        }

        handler.post { listener?.onStartEvent() }

        val stderrThread = Thread(StreamHandler(logcatProcess?.errorStream, null))
        val stdoutThread = Thread(StreamHandler(logcatProcess?.inputStream, listener))
        stderrThread.start()
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

    fun start(listener: LogcatEventListener?): Boolean {
        if (threadLogcat != null) {
            throw IllegalStateException("logcat is already running")
        }
        this.listener = listener
        threadLogcat = Thread(runnable)
        threadLogcat?.start()
        return true
    }

    override fun close() {
        logcatProcess?.destroy()

        try {
            threadLogcat?.join(300)
        } catch (e: InterruptedException) {
        }

        threadLogcat = null
        logcatProcess = null
    }

    private class StreamHandler(inputStream: InputStream?,
                                val listener: LogcatEventListener?) : Runnable {
        val reader: BufferedReader?

        init {
            reader = BufferedReader(InputStreamReader(inputStream))
        }

        override fun run() {
            while (true) {
                try {
                    val metadata = reader?.readLine()?.trim() ?: break

                    if (metadata.startsWith("[")) {
                        val msg = reader.readLine()?.trim() ?: break
                        listener?.onLogEvent(LogFactory.createNewLog(metadata.trim(), msg))
                    }
                } catch (e: IOException) {
                    break
                }
            }
        }
    }
}