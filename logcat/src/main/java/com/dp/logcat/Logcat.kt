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
import java.util.*
import kotlin.concurrent.thread

class Logcat : Closeable {
    var logcatBuffers = DEFAULT_BUFFERS
    private val logcatCmd = arrayOf("logcat", "-v", "long")
    private var pollInterval: Long = 250L // in ms
    private var threadLogcat: Thread? = null
    private var logcatProcess: Process? = null
    private val handler: Handler = Handler(Looper.getMainLooper())

    @Volatile
    private var listener: LogcatEventListener? = null

    private var pollCondition = ConditionVariable()

    var exitCode: Int = -1
        private set

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

    var isBound = false
        private set

    private val lifeCycleObserver = object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        private fun onActivityInForeground() {
            MyLogger.logDebug(Logcat::class, "onActivityInForeground")

            if (!paused) {
                MyLogger.logDebug(Logcat::class, "Posting pending logs")
                postPendingLogs()
            }

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
            paused = false
            exitCode = -1
            threadLogcat = thread { runLogcat() }
        } else {
            MyLogger.logInfo(Logcat::class, "Logcat is already running!")
        }
    }

    fun stop() {
        logcatProcess?.destroy()

        try {
            threadLogcat?.join(5000)
        } catch (e: InterruptedException) {
        }

        threadLogcat = null
        logcatProcess = null

        synchronized(logsLock) {
            logs.clear()
            pendingLogs.clear()
        }
    }

    fun restart() {
        stop()
        start()
    }

    fun exitSuccess() = exitCode == 0

    fun setEventListener(listener: LogcatEventListener?) {
        val pausedOld = paused
        pause()

        this.listener = listener

        if (!pausedOld) {
            resume()
        }
    }

    fun getLogs(): List<Log> {
        synchronized(logsLock) {
            return logs.toList()
        }
    }

    fun getLogsFiltered(): List<Log> {
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
        if (paused) {
            paused = false
            pausePostLogsCondition.open()
            pollCondition.open()
        }
    }

    fun bind(activity: AppCompatActivity?) {
        activity?.lifecycle?.addObserver(lifeCycleObserver)
        isBound = true
    }

    fun unbind(activity: AppCompatActivity?) {
        activity?.lifecycle?.removeObserver(lifeCycleObserver)
        isBound = false
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
        val buffers = mutableListOf<String>()
        if (logcatBuffers.isNotEmpty() && AVAILABLE_BUFFERS.isNotEmpty()) {
            for (buffer in logcatBuffers) {
                buffers += "-b"
                buffers += buffer
            }
        }
        val processBuilder = ProcessBuilder(*logcatCmd, *buffers.toTypedArray())

        try {
            logcatProcess = processBuilder.start()
            isProcessAlive = true
        } catch (e: IOException) {
            return
        }

        val errorStream = logcatProcess?.errorStream
        val inputStream = logcatProcess?.inputStream

        val postThread = thread { postLogsPeriodically() }
        val stderrThread = thread { processStderr(errorStream) }
        val stdoutThread = thread { processStdout(inputStream) }

        exitCode = logcatProcess?.waitFor() ?: -1

        isProcessAlive = false

        pollCondition.open()
        activityInBackgroundCondition.open()

        logcatProcess = null

        try {
            stderrThread.join(5000)
        } catch (e: InterruptedException) {
        }
        try {
            stdoutThread.join(5000)
        } catch (e: InterruptedException) {
        }
        try {
            postThread.join(5000)
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
        val reader: BufferedReader
        try {
            reader = BufferedReader(InputStreamReader(errStream))
            while (isProcessAlive) {
                reader.readLine() ?: break
            }
        } catch (e: Exception) {
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

        val reader: BufferedReader
        try {
            reader = BufferedReader(InputStreamReader(inputStream))
            loop@ while (isProcessAlive) {
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
                        }
                    } catch (e: Exception) {
                        MyLogger.logDebug(Logcat::class, "${e.message}: $metadata")
                    } finally {
                        msgBuffer.setLength(0)
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    companion object {
        val DEFAULT_BUFFERS: Set<String>
        val AVAILABLE_BUFFERS: Array<String>

        init {
            DEFAULT_BUFFERS = getDefaultBuffers()
            AVAILABLE_BUFFERS = getAvailabeBuffers()

            MyLogger.logDebug(Logcat::class, "Available buffers: " +
                    Arrays.toString(AVAILABLE_BUFFERS))
            MyLogger.logDebug(Logcat::class, "Default buffers: $DEFAULT_BUFFERS")
        }

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

        private fun getDefaultBuffers(): Set<String> {
            val result = mutableSetOf<String>()

            val stdoutList = mutableListOf<String>()
            CommandUtils.runCmd(cmd = *arrayOf("logcat", "-g"), stdoutList = stdoutList)

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

        private fun parseBufferNames(s: String, names: MutableSet<String>): Boolean {
            var startIndex = s.indexOf("'")
            if (startIndex == -1) {
                return true
            }

            var nextIndex = s.indexOf("'", startIndex + 1)
            if (nextIndex == -1) {
                return true
            }

            val periodIndex = s.indexOf(".")
            while (periodIndex == -1 || (startIndex < periodIndex && nextIndex < periodIndex)) {
                val name = s.substring(startIndex + 1, nextIndex)
                if (name != "all" && name != "default") {
                    names += name
                }

                startIndex = s.indexOf("'", nextIndex + 1)
                if (startIndex == -1) {
                    break
                }

                nextIndex = s.indexOf("'", startIndex + 1)
                if (nextIndex == -1) {
                    break
                }
            }

            return periodIndex != -1
        }

        private fun getAvailabeBuffers(): Array<String> {
            val result = mutableSetOf<String>()

            val stdoutList = mutableListOf<String>()
            CommandUtils.runCmd(cmd = *arrayOf("logcat", "-h"),
                    stdoutList = stdoutList, redirectStderr = true)

            var bFound = false
            for (s in stdoutList) {
                val trimmed = s.trim()
                if (bFound) {
                    if (parseBufferNames(trimmed, result)) {
                        break
                    }
                } else {
                    if (trimmed.startsWith("-b")) {
                        bFound = true
                        if (parseBufferNames(trimmed, result)) {
                            break
                        }
                    }
                }
            }

            return result.toTypedArray().sortedArray()
        }
    }
}


