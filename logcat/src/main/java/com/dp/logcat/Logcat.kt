package com.dp.logcat

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.net.Uri
import android.os.ConditionVariable
import android.os.Handler
import android.os.Looper
import android.support.v4.provider.DocumentFile
import android.support.v7.app.AppCompatActivity
import com.dp.logger.Logger
import com.logcat.collections.FixedCircularArray
import java.io.*
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

class Logcat(initialCapacity: Int = INITIAL_LOG_CAPACITY) : Closeable {
    var logcatBuffers = DEFAULT_BUFFERS
    private val logcatCmd = arrayOf("logcat", "-v", "long")
    private var pollInterval: Long = 250L // in ms
    private var threadLogcat: Thread? = null
    private var logcatProcess: Process? = null
    private val handler: Handler = Handler(Looper.getMainLooper())

    private var recordStartIndex = -1

    @Volatile
    private var listener: LogcatEventListener? = null

    private var pollCondition = ConditionVariable()

    var exitCode: Int = -1
        private set

    private var _pausedLock = Any()
    private var paused: Boolean = false
        get() = synchronized(_pausedLock) {
            field
        }
        set(value) = synchronized(_pausedLock) {
            field = value
        }
    private val pausePostLogsCondition = ConditionVariable()

    // must be synchronized
    private val logsLock = ReentrantLock()
    private val pendingLogsFullCondition = logsLock.newCondition()
    private var logs = FixedCircularArray<Log>(initialCapacity, INITIAL_LOG_SIZE)
    private var pendingLogs = FixedCircularArray<Log>(initialCapacity, INITIAL_LOG_SIZE)
    private val filters = mutableMapOf<String, Filter>()
    private val exclusions = mutableMapOf<String, Filter>()

    private var _activityInBackgroundLock = Any()
    private var activityInBackground: Boolean = true
        get() = synchronized(_activityInBackgroundLock) {
            field
        }
        set(value) = synchronized(_activityInBackgroundLock) {
            field = value
        }

    private val activityInBackgroundCondition = ConditionVariable()

    private val lifeCycleObserver = object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        private fun onActivityInForeground() {
            Logger.logDebug(Logcat::class, "onActivityInForeground")

            if (!paused) {
                Logger.logDebug(Logcat::class, "Posting pending logs")
                postPendingLogs()
            }

            lockedBlock(logsLock) {
                activityInBackground = false
            }
            activityInBackgroundCondition.open()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        private fun onActivityInBackground() {
            Logger.logDebug(Logcat::class, "onActivityInBackground")
            lockedBlock(logsLock) {
                activityInBackground = true
            }
        }
    }

    @Volatile
    private var isProcessAlive = false

    private fun postPendingLogs() {
        lockedBlock(logsLock) {
            if (pendingLogs.isNotEmpty()) {
                logs.add(pendingLogs)

                if (pendingLogs.size == 1) {
                    val log = pendingLogs[0]
                    if (!exclusions.values.any { it.apply(log) } && filters.values.all { it.apply(log) }) {
                        handler.post { listener?.onLogEvent(log) }
                    }
                } else {
                    val filteredLogs = pendingLogs.filter { e ->
                        !exclusions.values.any { it.apply(e) } && filters.values.all { it.apply(e) }
                    }.toList()

                    if (filteredLogs.isNotEmpty()) {
                        handler.post { listener?.onLogEvents(filteredLogs) }
                    }
                }

                pendingLogs.clear()
                pendingLogsFullCondition.signal()
            }
        }
    }

    fun start() {
        if (logcatProcess == null) {
            paused = false
            exitCode = -1
            threadLogcat = thread(block = { runLogcat() }, name = "logcat")
        } else {
            Logger.logInfo(Logcat::class, "Logcat is already running!")
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

        lockedBlock(logsLock) {
            logs.clear()
            pendingLogs.clear()
        }
    }

    fun clearLogs(onClear: (() -> Unit)? = null) {
        val wasPaused = paused
        pause()

        lockedBlock(logsLock) {
            logs.clear()
            pendingLogs.clear()
        }

        if (onClear != null) {
            onClear()
        }

        if (!wasPaused) {
            resume()
        }
    }

    fun restart() {
        stop()
        start()
    }

    fun exitSuccess() = exitCode == 0

    fun isRunning() = isProcessAlive

    fun setEventListener(listener: LogcatEventListener?) {
        val wasPaused = paused
        pause()

        lockedBlock(logsLock) {
            this.listener = listener
        }

        if (!wasPaused) {
            resume()
        }
    }

    fun getLogsFiltered(): List<Log> {
        lockedBlock(logsLock) {
            if (exclusions.isEmpty() && filters.isEmpty()) {
                return logs.toList()
            } else {
                return logs.filter { log ->
                    !exclusions.values.any { it.apply(log) } && filters.values.all { it.apply(log) }
                }
            }
        }
    }

    fun addExclusion(name: String, filter: Filter) {
        lockedBlock(logsLock) {
            exclusions[name] = filter
        }
    }

    fun removeExclusion(name: String) {
        lockedBlock(logsLock) {
            exclusions.remove(name)
        }
    }

    fun clearExclusions() {
        lockedBlock(logsLock) {
            exclusions.clear()
        }
    }

    fun addFilter(name: String, filter: Filter) {
        lockedBlock(logsLock) {
            filters[name] = filter
        }
    }

    fun removeFilter(name: String) {
        lockedBlock(logsLock) {
            filters.remove(name)
        }
    }

    fun clearFilters() {
        lockedBlock(logsLock) {
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

    fun startRecording() {
        lockedBlock(logsLock) {
            recordStartIndex = logs.size - 1
        }
    }

    fun stopRecording(): List<Log> {
        lockedBlock(logsLock) {
            val result = mutableListOf<Log>()
            if (recordStartIndex >= 0) {
                for (i in recordStartIndex until logs.size) {
                    result += logs[i]
                }
            }
            recordStartIndex = -1
            return result.filter { log -> filters.values.all { it.apply(log) } }
        }
    }

    fun bind(activity: AppCompatActivity?) {
        activity?.lifecycle?.addObserver(lifeCycleObserver)
    }

    fun unbind(activity: AppCompatActivity?) {
        activity?.lifecycle?.removeObserver(lifeCycleObserver)
    }

    override fun close() {
        stop()
        lockedBlock(logsLock) {
            listener = null
        }
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

        val postThread = thread(block = { postLogsPeriodically() }, name = "logcat-post")
        val stderrThread = thread(block = { processStderr(errorStream) }, name = "logcat-stderr")
        val stdoutThread = thread(block = { processStdout(inputStream) }, name = "logcat-stdout")

        exitCode = try {
            logcatProcess?.waitFor() ?: -1
        } catch (e: InterruptedException) {
            -1
        }

        isProcessAlive = false

        pollCondition.open()
        activityInBackgroundCondition.open()

        logcatProcess = null

        val waitTime = 1000L
        try {
            stderrThread.join(waitTime)
        } catch (e: InterruptedException) {
        }
        try {
            stdoutThread.join(waitTime)
        } catch (e: InterruptedException) {
        }
        try {
            postThread.join(waitTime)
        } catch (e: InterruptedException) {
        }
    }

    fun setMaxLogsCount(maxLogsCount: Int) {
        lockedBlock(logsLock) {
            logs = FixedCircularArray(maxLogsCount, INITIAL_LOG_SIZE)
            pendingLogs = FixedCircularArray(maxLogsCount, INITIAL_LOG_SIZE)
        }
    }

    private inline fun <R> lockedBlock(lock: Lock, block: () -> R): R {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        lockedBlock(logsLock) {
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
        try {
            LogcatStreamReader(inputStream!!).use {
                for (log in it) {
                    lockedBlock(logsLock) {
                        pendingLogs.add(log)

                        if (pendingLogs.isFull()) {
                            pendingLogsFullCondition.awaitUninterruptibly()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // do nothing
        }
    }

    companion object {
        val DEFAULT_BUFFERS: Set<String>
        val AVAILABLE_BUFFERS: Array<String>
        const val INITIAL_LOG_CAPACITY = 250_000
        const val INITIAL_LOG_SIZE = 10_000
        private const val LOG_FILE_HEADER_FMT = "<<< log_count = %d >>>"

        init {
            DEFAULT_BUFFERS = getDefaultBuffers()
            AVAILABLE_BUFFERS = getAvailableBuffers()

            Logger.logDebug(Logcat::class, "Available buffers: " +
                    Arrays.toString(AVAILABLE_BUFFERS))
            Logger.logDebug(Logcat::class, "Default buffers: $DEFAULT_BUFFERS")
        }

        fun getLogCountFromHeader(file: File): Long {
            try {
                return getLogCountFromHeader(FileInputStream(file))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return -1L
        }

        fun getLogCountFromHeader(context: Context, file: DocumentFile): Long {
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

        fun writeToFile(logs: List<Log>, file: File): Boolean {
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

        fun writeToFile(context: Context, logs: List<Log>, uri: Uri): Boolean {
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

        private fun writeToFileHelper(logs: List<Log>, writer: BufferedWriter) {
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

        private fun getAvailableBuffers(): Array<String> {
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


