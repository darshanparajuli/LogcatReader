package com.dp.logcat

import android.os.Build
import com.dp.logger.Logger
import com.logcat.collections.FixedCircularArray
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

class LogcatSession(
  initialCapacity: Int,
  private val buffers: Set<String>,
  @Volatile
  var pollIntervalMs: Long = 250L,
) {

  @Volatile private var record = false
  val isRecording: Boolean get() = record

  private val lock = ReentrantLock() // locks {
  private val recordBuffer = mutableListOf<Log>()
  private val allLogs = FixedCircularArray<Log>(
    capacity = initialCapacity,
    initialSize = 1000,
  )
  private val pendingLogs = FixedCircularArray<Log>(
    capacity = initialCapacity,
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
  }.buffer(capacity = Int.MAX_VALUE)

  @JvmInline
  value class Status(val success: Boolean)

  fun start(): Flow<Status> {
    Logger.debug(LogcatSession::class, "starting")
    check(!active) { "Logcat is already active!" }
    active = true
    val status = MutableSharedFlow<Status>(extraBufferCapacity = 1)
    logcatThread = thread {
      val process = startLogcatProcess()
      status.tryEmit(Status(process != null))
      if (process != null) {
        readLogs(process)
      }
      Logger.debug(LogcatSession::class, "stopped logcat thread")
    }
    pollerThread = thread {
      poll()
      Logger.debug(LogcatSession::class, "stopped polling thread")
    }
    return status
  }

  private fun Iterable<Log>.filtered(): List<Log> {
    return filter { e ->
      !exclusions.any { it.apply(e) }
    }.filter { e ->
      filters.all { it.apply(e) }
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
    } catch (e: IOException) {
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
      readerThread.join(5_000L)
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
    logcatThread?.join(5_000L)
    logcatThread = null
    pollerThread?.join(5_000L)
    pollerThread = null
    lock.withLock {
      allLogs.clear()
      pendingLogs.clear()
      recordBuffer.clear()
    }
    Logger.debug(LogcatSession::class, "stopped")
  }

  fun startRecording() {
    record = true
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

  fun stopRecording(): List<Log> {
    record = false
    return lock.withLock {
      val result = recordBuffer.toList()
      recordBuffer.clear()
      result
    }
  }
}
