package com.dp.logcat

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.os.ConditionVariable
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import com.dp.logger.MyLogger
import java.io.*
import kotlin.concurrent.thread

class Logcat : Closeable {
    var logcatCmd = arrayOf("logcat", "-v", "long")
    private var pollInterval: Long = 250L // in ms
    private var threadLogcat: Thread? = null
    private var logcatProcess: Process? = null
    private val handler: Handler = Handler(Looper.getMainLooper())

    @Volatile
    private var listener: LogcatEventListener? = null

    private var pollCondition = ConditionVariable()

    @Volatile
    private var paused = false
    private val pausePostLogsCondition = ConditionVariable()

    // must be synchronized
    private val logsLock = Any()
    private val logs = mutableListOf<Log>()
    private val filters = mutableMapOf<String, (Log) -> Boolean>()
    private val pendingLogs = mutableListOf<Log>()

    private var _activityInBackgroundLock = Any()
    private var activityInBackground: Boolean = false
        get() = synchronized(_activityInBackgroundLock) {
            field
        }
        set(value) {
            synchronized(_activityInBackgroundLock) {
                field = value
            }
        }
    private var activityInBackgroundCondition = ConditionVariable()

    private val lifeCycleObserver = object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        private fun onActivityInForeground() {
            MyLogger.logDebug(Logcat::class, "onActivityInForeground")
            postPendingLogs()
            activityInBackground = false
            activityInBackgroundCondition.open()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        private fun onActivityInBackground() {
            MyLogger.logDebug(Logcat::class, "onActivityInBackground")
            synchronized(logsLock) {
                activityInBackground = true
            }
        }
    }

    @Volatile
    private var isProcessAlive = false

    private fun postPendingLogs() {
        synchronized(logsLock) {
            if (pendingLogs.isNotEmpty()) {
                logs += pendingLogs

                if (pendingLogs.size == 1) {
                    val log = pendingLogs[0]
                    if (filters.values.all { it(log) }) {
                        handler.post { listener?.onLogEvent(log) }
                    }
                } else {
                    val dup = pendingLogs.filter { e ->
                        filters.values.all { it(e) }
                    }.toList()

                    if (dup.isNotEmpty()) {
                        handler.post { listener?.onLogEvents(dup) }
                    }
                }

                pendingLogs.clear()
            }
        }
    }

    fun start() {
        if (logcatProcess == null) {
            threadLogcat = thread { runLogcat() }
        } else {
            MyLogger.logInfo(Logcat::class, "Logcat is already running!")
        }
    }

    fun setEventListener(listener: LogcatEventListener?) {
        val pausedOld = paused
        pause()

        this.listener = listener

        if (!pausedOld) {
            resume()
        }
    }

    fun getLogs(): List<Log> {
        var list = listOf<Log>()
        synchronized(logsLock) {
            list += logs
        }
        return list
    }

    fun getLogsFiltered(): List<Log> {
        val logs = getLogs()
        synchronized(logsLock) {
            return logs.filter { log -> filters.values.all { it(log) } }
        }
    }

    fun addFilter(name: String, filter: (Log) -> Boolean) {
        synchronized(logsLock) {
            filters.put(name, filter)
        }
    }

    fun removeFilter(name: String) {
        synchronized(logsLock) {
            filters.remove(name)
        }
    }

    fun clearFilters() {
        synchronized(logsLock) {
            filters.clear()
        }
    }

    fun pause() {
        paused = true
    }

    fun resume() {
        paused = false
        pausePostLogsCondition.open()
        pollCondition.open()
    }

    fun bind(activity: AppCompatActivity?) {
        activity?.lifecycle?.addObserver(lifeCycleObserver)
    }

    fun unbind(activity: AppCompatActivity?) {
        activity?.lifecycle?.removeObserver(lifeCycleObserver)
    }

    fun stop() {
        logcatProcess?.destroy()

        try {
            threadLogcat?.join(300)
        } catch (e: InterruptedException) {
        }

        threadLogcat = null
        logcatProcess = null

        synchronized(logsLock) {
            logs.clear()
            pendingLogs.clear()
            filters.clear()
        }
    }

    override fun close() {
        stop()
        listener = null
    }

    fun setPollInterval(interval: Long) {
        this.pollInterval = interval
        pollCondition.open()
    }

    private fun runLogcat() {
        val processBuilder = ProcessBuilder(*logcatCmd)

        try {
            logcatProcess = processBuilder.start()
            isProcessAlive = true
            paused = false
        } catch (e: IOException) {
            handler.post { listener?.onStartFailedEvent() }
            return
        }

        handler.post { listener?.onStartEvent() }

        val postThread = thread { postLogsPeriodically() }
        val stderrThread = thread { processStderr(logcatProcess?.errorStream) }
        val stdoutThread = thread { processStdout(logcatProcess?.inputStream) }

        val error = logcatProcess?.waitFor() != 0

        isProcessAlive = false

        pollCondition.open()
        activityInBackgroundCondition.open()

        logcatProcess = null
        handler.post { listener?.onStopEvent(error) }

        try {
            stderrThread.join(2000)
        } catch (e: InterruptedException) {
        }
        try {
            stdoutThread.join(2000)
        } catch (e: InterruptedException) {
        }
        try {
            postThread.join(2000)
        } catch (e: InterruptedException) {
        }
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        synchronized(logsLock) {
            logs.forEach { log -> stringBuilder.append(log) }
        }
        return stringBuilder.toString()
    }

    private fun processStderr(errStream: InputStream?) {
        val reader = BufferedReader(InputStreamReader(errStream))
        while (isProcessAlive) {
            try {
                reader.readLine() ?: break
            } catch (e: IOException) {
                break
            }
        }
    }

    private fun postLogsPeriodically() {
        while (isProcessAlive) {
            if (paused) {
                pausePostLogsCondition.block()
                pausePostLogsCondition.close()
                if (!isProcessAlive) {
                    break
                }
            }

            if (activityInBackground) {
                activityInBackgroundCondition.block()
                activityInBackgroundCondition.close()
                if (!isProcessAlive) {
                    break
                }
            }

            val t0 = System.currentTimeMillis()

            postPendingLogs()

            val diff = System.currentTimeMillis() - t0
            val sleepTime = pollInterval - diff
            if (sleepTime > 0) {
                pollCondition.block(sleepTime)
                pollCondition.close()
            }
        }
    }

    private fun processStdout(inputStream: InputStream?) {
        val msgBuffer = StringBuilder()

        val reader = BufferedReader(InputStreamReader(inputStream))
        loop@ while (isProcessAlive) {
            try {
                val metadata = reader.readLine()?.trim() ?: break
                if (metadata.startsWith("[")) {
                    var msg = reader.readLine() ?: break
                    msgBuffer.append(msg)

                    msg = reader.readLine() ?: break
                    while (msg.isNotEmpty()) {
                        msgBuffer.append("\n")
                                .append(msg)

                        msg = reader.readLine() ?: break@loop
                    }

                    try {
                        val log = LogFactory.createNewLog(metadata, msgBuffer.toString())
                        synchronized(logsLock) {
                            pendingLogs += log
//                            MyLogger.logDebug(Logcat::class, "size: " + logs.size +
//                                    ", pending size: " + pendingLogs.size)
                        }
                    } catch (e: Exception) {
                        MyLogger.logDebug(Logcat::class, "${e.message}: $metadata")
                    } finally {
                        msgBuffer.setLength(0)
                    }
                }
            } catch (e: IOException) {
                break
            }
        }
    }

    companion object {
        fun writeToFile(logs: List<Log>, file: File): Boolean {
            var writer: BufferedWriter? = null
            return try {
                writer = BufferedWriter(FileWriter(file))
                for (log in logs) {
                    writer.write(log.toString())
                }
                writer.flush()
                true
            } catch (e: IOException) {
                false
            } finally {
                writer?.close()
            }
        }
    }
}


