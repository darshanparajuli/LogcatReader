package com.dp.logcatapp.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.dp.logcat.LogcatSession
import com.dp.logcat.LogcatUtil
import com.dp.logcatapp.R
import com.dp.logcatapp.activities.ComposeMainActivity
import com.dp.logcatapp.util.PreferenceKeys
import com.dp.logcatapp.util.getDefaultSharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class LogcatService : BaseService() {

  companion object {
    private const val NOTIFICATION_CHANNEL = "logcat_channel_01"
    private const val NOTIFICATION_ID = 1
  }

  sealed interface LogcatSessionStatus {
    data class Started(val session: LogcatSession) : LogcatSessionStatus
    data object FailedToStart : LogcatSessionStatus

    val sessionOrNull: LogcatSession?
      get() = (this as? Started)?.session
  }

  private val _logcatSession = MutableStateFlow<LogcatSessionStatus?>(null)
  val logcatSessionStatus = _logcatSession.asStateFlow()

  private val restartTrigger = Channel<Unit>(capacity = 1, onBufferOverflow = DROP_OLDEST)

  override fun onCreate() {
    super.onCreate()
    if (VERSION.SDK_INT >= 26) {
      createNotificationChannel()
    }

    lifecycleScope.launch {
      startNewLogcatSession()
      restartTrigger.consumeEach {
        val current = _logcatSession.getAndUpdate { null }
        current?.sessionOrNull?.let { session ->
          withContext(Dispatchers.Default) { session.stop() }
        }
        startNewLogcatSession()
      }
    }
  }

  fun restartLogcatSession() {
    restartTrigger.trySend(Unit)
  }

  private suspend fun startNewLogcatSession() {
    val logcatSession = newLogcatSession()
    if (logcatSession.start().first().success) {
      _logcatSession.value = LogcatSessionStatus.Started(logcatSession)
    } else {
      _logcatSession.value = LogcatSessionStatus.FailedToStart
    }
  }

  override fun onPreRegisterSharedPreferenceChangeListener() {
    val defaultBuffers = PreferenceKeys.Logcat.Default.BUFFERS
    if (defaultBuffers.isNotEmpty() && LogcatUtil.AVAILABLE_BUFFERS.isNotEmpty()) {
      val buffers = getDefaultSharedPreferences()
        .getStringSet(PreferenceKeys.Logcat.KEY_BUFFERS, emptySet())
      if (buffers == null || buffers.isEmpty()) {
        getDefaultSharedPreferences().edit {
          putStringSet(PreferenceKeys.Logcat.KEY_BUFFERS, defaultBuffers)
        }
      }
    }
  }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int
  ): Int {
    super.onStartCommand(intent, flags, startId)
    startForeground(
      NOTIFICATION_ID,
      createNotification(_logcatSession.value?.sessionOrNull?.isRecording == true)
    )
    return START_STICKY
  }

  fun updateNotification(showStopRecording: Boolean) {
    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    nm.notify(NOTIFICATION_ID, createNotification(showStopRecording))
  }

  private fun createNotification(addStopRecordingAction: Boolean): Notification {
    val startIntent = Intent(this, ComposeMainActivity::class.java)
    startIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
    val contentIntent = if (VERSION.SDK_INT >= VERSION_CODES.M) {
      PendingIntent.getActivity(
        this, 0, startIntent,
        FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE,
      )
    } else {
      PendingIntent.getActivity(
        this, 0, startIntent,
        FLAG_UPDATE_CURRENT,
      )
    }

    val exitIntent = Intent(this, ComposeMainActivity::class.java)
    exitIntent.putExtra(ComposeMainActivity.EXIT_EXTRA, true)
    exitIntent.action = "exit"
    val exitPendingIntent = if (VERSION.SDK_INT >= VERSION_CODES.M) {
      PendingIntent.getActivity(
        this, 1, exitIntent,
        FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
      )
    } else {
      PendingIntent.getActivity(
        this, 1, exitIntent,
        FLAG_UPDATE_CURRENT
      )
    }

    val exitAction = NotificationCompat.Action.Builder(
      R.drawable.ic_clear_white_24dp,
      getString(R.string.exit), exitPendingIntent
    )
      .build()

    val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
      .setSmallIcon(R.drawable.ic_perm_device_information_white_24dp)
      .setColor(ContextCompat.getColor(applicationContext, R.color.color_primary))
      .setContentTitle(getString(R.string.app_name))
      .setTicker(getString(R.string.app_name))
      .setContentText(getString(R.string.logcat_service))
      .setWhen(System.currentTimeMillis())
      .setContentIntent(contentIntent)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .setAutoCancel(false)
      .addAction(exitAction)

    if (addStopRecordingAction) {
      val stopRecordingIntent = Intent(this, ComposeMainActivity::class.java)
      stopRecordingIntent.putExtra(ComposeMainActivity.STOP_RECORDING_EXTRA, true)
      stopRecordingIntent.action = "stop recording"
      val stopRecordingPendingIntent = if (VERSION.SDK_INT >= VERSION_CODES.M) {
        PendingIntent.getActivity(
          this, 2,
          stopRecordingIntent,
          FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
        )
      } else {
        PendingIntent.getActivity(
          this, 2,
          stopRecordingIntent,
          FLAG_UPDATE_CURRENT,
        )
      }
      val stopRecordingAction = NotificationCompat.Action.Builder(
        R.drawable.ic_stop_white_24dp,
        getString(R.string.stop_recording), stopRecordingPendingIntent
      )
        .build()

      builder.addAction(stopRecordingAction)
    }

    builder.setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))

    return builder.build()
  }

  @RequiresApi(26)
  private fun createNotificationChannel() {
    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    val nc = NotificationChannel(
      NOTIFICATION_CHANNEL,
      getString(R.string.logcat_service_channel_name), NotificationManager.IMPORTANCE_LOW
    )
    nc.enableLights(false)
    nc.enableVibration(false)
    nm.createNotificationChannel(nc)
  }

  @RequiresApi(26)
  private fun deleteNotificationChannel() {
    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    nm.deleteNotificationChannel(NOTIFICATION_CHANNEL)
  }

  override fun onDestroy() {
    super.onDestroy()
    _logcatSession.update { status ->
      status?.sessionOrNull?.stop()
      null
    }

    if (VERSION.SDK_INT >= 26) {
      deleteNotificationChannel()
    }
  }

  override fun onSharedPreferenceChanged(
    sharedPreferences: SharedPreferences,
    key: String?
  ) {
    super.onSharedPreferenceChanged(sharedPreferences, key)
    when (key) {
      PreferenceKeys.Logcat.KEY_POLL_INTERVAL -> {
        val pollInterval = sharedPreferences.getInt(
          key,
          PreferenceKeys.Logcat.Default.POLL_INTERVAL
        )
        _logcatSession.value?.sessionOrNull?.apply { pollIntervalMs = pollInterval.toLong() }
      }
      PreferenceKeys.Logcat.KEY_BUFFERS,
      PreferenceKeys.Logcat.KEY_MAX_LOGS -> {
        restartTrigger.trySend(Unit)
      }
    }
  }

  private fun newLogcatSession(): LogcatSession {
    val sharedPreferences = getDefaultSharedPreferences()
    val bufferValues = sharedPreferences.getStringSet(
      PreferenceKeys.Logcat.KEY_BUFFERS,
      PreferenceKeys.Logcat.Default.BUFFERS
    )!!
    val pollInterval = sharedPreferences.getInt(
      PreferenceKeys.Logcat.KEY_POLL_INTERVAL,
      PreferenceKeys.Logcat.Default.POLL_INTERVAL
    )
    val maxLogs = sharedPreferences.getInt(
      PreferenceKeys.Logcat.KEY_MAX_LOGS,
      PreferenceKeys.Logcat.Default.MAX_LOGS
    )

    val buffers = LogcatUtil.AVAILABLE_BUFFERS
    val logcatBuffers = bufferValues.mapNotNull { e ->
      buffers.getOrNull(e.toInt())
        ?.lowercase(Locale.getDefault())
    }.toSet().ifEmpty {
      sharedPreferences.edit {
        putStringSet(
          PreferenceKeys.Logcat.KEY_BUFFERS,
          PreferenceKeys.Logcat.Default.BUFFERS
        )
      }
      LogcatUtil.DEFAULT_BUFFERS
    }

    return LogcatSession(
      capacity = maxLogs,
      buffers = logcatBuffers,
      pollIntervalMs = pollInterval.toLong()
    )
  }
}