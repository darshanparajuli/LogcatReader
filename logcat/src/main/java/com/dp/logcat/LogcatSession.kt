package com.dp.logcat

import android.net.Uri
import android.os.Build
import com.dp.logger.Logger
import com.logcat.collections.FixedCircularArray
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.IOException
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
  @Volatile private var paused = false
  private val pauseWaiter = Object()

  var isPaused: Boolean
    get() = paused
    set(value) {
      synchronized(pauseWaiter) {
        paused = value
        if (!paused) {
          pauseWaiter.notify()
        }
      }
    }

  companion object {
    private val _uidOptionSupported = MutableStateFlow<Boolean?>(null)
    val uidOptionSupported = _uidOptionSupported.asStateFlow()

    init {
      // This is ok since we are simply executing a `logcat` process and getting the result from
      // it without accessing any Android APIs.
      @OptIn(DelicateCoroutinesApi::class)
      GlobalScope.launch(Dispatchers.IO) {
        _uidOptionSupported.value = isUidOptionSupportedHelper()
      }
    }

    suspend fun isUidOptionSupported(): Boolean {
      return uidOptionSupported.filterNotNull().first()
    }

    private fun isUidOptionSupportedHelper(): Boolean {
      return try {
        // Dump the log with `-v uid` cmdline option to see if it works.
        val process = ProcessBuilder(
          "logcat", "-v", "long", "-v", "uid", "-d",
        ).start()
        val stdoutReaderThread = thread {
          try {
            // Consume stdout. Without this, the process waits forever on some
            // devices/os versions.
            process.inputStream.bufferedReader().use {
              it.lineSequence().forEach { }
            }
          } catch (_: Exception) {
          }
        }
        val result = process.waitFor() == 0
        stdoutReaderThread.join(THREAD_JOIN_TIMEOUT)
        result
      } catch (_: Exception) {
        false
      }
    }
  }

  private var logcatProcess: Process? = null
  private var logcatThread: Thread? = null
  private var pollerThread: Thread? = null

  val isRecording: Boolean get() = lock.withLock { record }

  val logs: Flow<List<Log>> = channelFlow {
    withContext(Dispatchers.Default) {
      lock.withLock {
        trySend(allLogs.filtered())
        onNewLog = ::trySend
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
      // calling runBlocking is fine here since this function runs a separate non-ui thread, and not
      // in a coroutine.
      val uidSupported = runBlocking { isUidOptionSupported() }
      val process = startLogcatProcess(uidSupported)
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

  private fun startLogcatProcess(uidSupported: Boolean): Process? {
    val buffersArg = mutableListOf<String>()
    for (buffer in buffers) {
      buffersArg += "-b"
      buffersArg += buffer
    }
    val cmd = mutableListOf<String>()
    cmd += listOf("logcat", "-v", "long")
    if (uidSupported) {
      cmd += listOf("-v", "uid")
    }
    cmd += buffersArg
    return try {
      ProcessBuilder(cmd).start().also { process ->
        logcatProcess = process
      }
    } catch (e: IOException) {
      e.printStackTrace()
      Logger.debug(LogcatSession::class, "error starting logcat process")
      null
    }
  }

  private fun readLogs(process: Process) {
    try {
      val inputStream = process.inputStream
      val stdoutReaderThread = thread {
        try {
          LogcatStreamReader(inputStream).use { logs ->
            logs.forEach { log ->
              lock.withLock {
                pendingLogs += log
              }
            }
          }
        } catch (_: Exception) {
        }
        Logger.debug(LogcatSession::class, "stopped logcat reader thread")
      }

      // We don't care about the exit value as the process doesn't exit normally.
      process.waitFor()
      inputStream.close()
      stdoutReaderThread.join(THREAD_JOIN_TIMEOUT)
    } catch (_: Exception) {
      Logger.debug(LogcatSession::class, "error reading logs")
    }
  }

  private fun poll() {
    while (active) {
      synchronized(pauseWaiter) {
        while (paused) {
          try {
            pauseWaiter.wait()
          } catch (_: InterruptedException) {
          }
        }
      }

      lock.withLock {
        val pending = pendingLogs.toList()
        pendingLogs.clear()

        allLogs += pending

        // If recording is enabled, then add to record buffer.
        if (record) {
          recordBuffer += pending.filtered()
        }

        onNewLog?.invoke(pending.filtered())
      }
      Thread.sleep(pollIntervalMs)
    }
  }

  fun stop() {
    Logger.debug(LogcatSession::class, "stopping")
    active = false
    record = false
    isPaused = false
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
