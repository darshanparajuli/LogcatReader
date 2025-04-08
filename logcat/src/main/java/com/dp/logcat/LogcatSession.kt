package com.dp.logcat

import android.net.Uri
import android.os.Build
import com.dp.logger.Logger
import com.logcat.collections.FixedCircularArray
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.consumeAsFlow
import java.io.BufferedWriter
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

private const val THREAD_JOIN_TIMEOUT = 5_000L // 5 seconds

class LogcatSession(
  capacity: Int,
  private val buffers: Set<String>,
  @Volatile
  var pollIntervalMs: Long = 250,
) {

  @Volatile private var record = false
  private var recordThread: Thread? = null

  private val lock = ReentrantLock() // locks {
  private val recordBuffer = LinkedBlockingQueue<List<Log>>()
  private var recordingFileInfo: RecordingFileInfo? = null
  private val allLogs = FixedCircularArray<Log>(
    capacity = capacity,
    initialSize = 10_000,
  )
  private val pendingLogs = FixedCircularArray<Log>(
    capacity = capacity,
    initialSize = 1000,
  )
  private var onNewLog: ((List<Log>) -> Unit)? = null
  private val filters = mutableListOf<Filter>()
  private val exclusions = mutableListOf<Filter>()
  // }

  @Volatile private var active = false

  private var logcatProcess: Process? = null
  private var logcatThread: Thread? = null
  private var pollerThread: Thread? = null

  val isRecording: Boolean get() = lock.withLock { record }

  val logs: Flow<List<Log>> = channelFlow {
    lock.withLock {
      trySend(allLogs.filtered())
      onNewLog = { logs ->
        trySend(logs.filtered())
      }
    }
    awaitClose {
      lock.withLock {
        onNewLog = null
      }
    }
  }.buffer(capacity)

  @JvmInline
  value class Status(val success: Boolean)

  fun start(): Flow<Status> {
    Logger.debug(LogcatSession::class, "starting")
    check(!active) { "Logcat is already active!" }
    active = true
    val status = Channel<Status>(capacity = 1)
    logcatThread = thread {
      val process = startLogcatProcess()
      status.trySend(Status(process != null))
      if (process != null) {
        readLogs(process)
      }
      Logger.debug(LogcatSession::class, "stopped logcat thread")
    }
    pollerThread = thread {
      poll()
      Logger.debug(LogcatSession::class, "stopped polling thread")
    }
    return status.consumeAsFlow()
  }

  private fun Iterable<Log>.filtered(): List<Log> {
    return filter { e ->
      !exclusions.any { it.apply(e) }
    }.filter { e ->
      filters.isEmpty() || filters.any { it.apply(e) }
    }
  }

  private fun startLogcatProcess(): Process? {
    val buffersArg = mutableListOf<String>()
    for (buffer in buffers) {
      buffersArg += "-b"
      buffersArg += buffer
    }
    return try {
      ProcessBuilder(
        "logcat", "-v", "long",
        *buffersArg.toTypedArray()
      ).start().also { process ->
        logcatProcess = process
      }
    } catch (_: IOException) {
      Logger.debug(LogcatSession::class, "error starting logcat process")
      null
    }
  }

  private fun readLogs(process: Process) {
    try {
      val inputStream = process.inputStream
      val readerThread = thread {
        LogcatStreamReader(inputStream).use { logs ->
          try {
            logs.forEach { log ->
              lock.withLock {
                pendingLogs += log
              }
            }
          } catch (_: InterruptedIOException) {
          } catch (_: IOException) {
          }
        }
        Logger.debug(LogcatSession::class, "stopped logcat reader thread")
      }

      // We don't care about the exit value as the process doesn't exit normally.
      process.waitFor()
      inputStream.close()
      readerThread.join(THREAD_JOIN_TIMEOUT)
    } catch (_: Exception) {
      Logger.debug(LogcatSession::class, "error reading logs")
    }
  }

  private fun poll() {
    while (active) {
      lock.withLock {
        val pending = pendingLogs.toList()
        pendingLogs.clear()

        allLogs += pending

        // If recording is enabled, then add to record buffer.
        if (record) {
          recordBuffer += pending
        }

        onNewLog?.invoke(pending)
      }
      Thread.sleep(pollIntervalMs)
    }
  }

  fun stop() {
    Logger.debug(LogcatSession::class, "stopping")
    active = false
    record = false
    if (Build.VERSION.SDK_INT >= 26) {
      logcatProcess?.destroyForcibly()
    } else {
      logcatProcess?.destroy()
    }
    logcatProcess = null
    logcatThread?.join(THREAD_JOIN_TIMEOUT)
    logcatThread = null
    pollerThread?.join(THREAD_JOIN_TIMEOUT)
    pollerThread = null
    recordThread?.let { thread ->
      thread.interrupt()
      thread.join(THREAD_JOIN_TIMEOUT)
    }
    recordThread = null
    lock.withLock {
      allLogs.clear()
      pendingLogs.clear()
      recordBuffer.clear()
      recordingFileInfo = null
    }
    Logger.debug(LogcatSession::class, "stopped")
  }

  fun startRecording(
    recordingFileInfo: RecordingFileInfo,
    writer: BufferedWriter,
  ) {
    if (record) {
      return
    }

    lock.withLock {
      this.recordingFileInfo = recordingFileInfo
      record = true
      recordThread = thread {
        while (record) {
          val logs = try {
            recordBuffer.take()
          } catch (_: InterruptedException) {
            break
          }

          try {
            logs.forEach { log ->
              writer.write(log.toString())
            }
            writer.flush()
          } catch (_: Exception) {
            break
          }
        }

        try {
          writer.flush()
          writer.close()
        } catch (_: Exception) {
        }
      }
    }
  }

  fun stopRecording(): RecordingFileInfo? {
    return lock.withLock {
      record = false
      recordThread?.let { thread ->
        thread.interrupt()
        thread.join(THREAD_JOIN_TIMEOUT)
      }
      recordThread = null
      recordBuffer.clear()
      val result = recordingFileInfo
      recordingFileInfo = null
      result
    }
  }

  fun getAllLogs(): List<Log> = lock.withLock {
    allLogs.toList()
  }

  fun setFilters(filters: List<Filter>, exclusion: Boolean = false) {
    lock.withLock {
      if (exclusion) {
        exclusions.clear()
        exclusions += filters
      } else {
        this.filters.clear()
        this.filters += filters
      }
    }
  }

  data class RecordingFileInfo(
    val fileName: String,
    val uri: Uri,
    val isCustomLocation: Boolean,
  )
}
