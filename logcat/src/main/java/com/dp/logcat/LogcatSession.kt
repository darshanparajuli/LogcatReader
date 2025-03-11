package com.dp.logcat

import android.os.Build
import com.dp.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Lazily
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.shareIn
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

class LogcatSession(
  coroutineScope: CoroutineScope,
  private val buffers: Set<String>,
) {

  private val lock = ReentrantLock()
  private var pollIntervalMs = 250L

  init {
    checkNotNull(coroutineScope.coroutineContext[Job]).invokeOnCompletion { cause ->
      stop()
    }
  }

  private val pendingLogs = mutableListOf<Log>()
  private val _allLogs = mutableListOf<Log>()

  val allLogs: List<Log>
    get() = lock.withLock {
      _allLogs.toList()
    }

  @Volatile private var active = false

  private var logcatProcess: Process? = null
  private var logcatThread: Thread? = null
  private var pollerThread: Thread? = null

  val logs: SharedFlow<List<Log>> = channelFlow {
    start { logs ->
      val result = trySend(logs)
      if (result.isFailure) {
        Logger.debug(LogcatSession::class, "failed to send new log")
      }
    }
    awaitClose()
  }.buffer(capacity = Int.MAX_VALUE)
    .shareIn(
      scope = coroutineScope,
      started = Lazily,
      replay = Int.MAX_VALUE, // Replay _all_ logs.
    )

  private fun start(onReceivedNewLogs: (logs: List<Log>) -> Unit) {
    Logger.debug(LogcatSession::class, "starting")
    check(!active) { "Logcat is already active!" }
    active = true
    logcatThread = thread {
      runLogcat()
      Logger.debug(LogcatSession::class, "stopped logcat thread")
    }
    pollerThread = thread {
      poll(onReceivedNewLogs)
      Logger.debug(LogcatSession::class, "stopped polling thread")
    }
  }

  private fun runLogcat() {
    val buffersArg = mutableListOf<String>()
    for (buffer in buffers) {
      buffersArg += "-b"
      buffersArg += buffer
    }
    val process = ProcessBuilder(
      "logcat", "-v", "long",
      *buffersArg.toTypedArray()
    ).start()
    logcatProcess = process

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
  }

  private fun poll(onReceivedNewLogs: (logs: List<Log>) -> Unit) {
    while (active) {
      val pending = lock.withLock {
        _allLogs += pendingLogs
        val pending = pendingLogs.toList()
        pendingLogs.clear()
        pending
      }
      onReceivedNewLogs(pending)
      Thread.sleep(pollIntervalMs)
    }
  }

  fun stop() {
    Logger.debug(LogcatSession::class, "stopping")
    active = false
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
    Logger.debug(LogcatSession::class, "stopped")
  }
}
