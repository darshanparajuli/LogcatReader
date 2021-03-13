package com.dp.logcatapp.services

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.dp.logcat.Logcat
import com.dp.logcatapp.R
import com.dp.logcatapp.activities.MainActivity
import com.dp.logcatapp.util.PreferenceKeys
import com.dp.logcatapp.util.getDefaultSharedPreferences
import com.dp.logcatapp.util.showToast
import java.util.Locale

class LogcatService : BaseService() {

  companion object {
    val TAG = LogcatService::class.qualifiedName
    private const val NOTIFICATION_CHANNEL = "logcat_channel_01"
    private const val NOTIFICATION_ID = 1
  }

  lateinit var logcat: Logcat
    private set
  var restartedLogcat = false

  var paused = false
  var recording = false

  override fun onCreate() {
    super.onCreate()
    if (Build.VERSION.SDK_INT >= 26) {
      createNotificationChannel()
    }

    initLogcat()
  }

  override fun onPreRegisterSharedPreferenceChangeListener() {
    val defaultBuffers = PreferenceKeys.Logcat.Default.BUFFERS
    if (defaultBuffers.isNotEmpty() && Logcat.AVAILABLE_BUFFERS.isNotEmpty()) {
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
    startForeground(NOTIFICATION_ID, createNotification(recording))
    return START_STICKY
  }

  fun updateNotification(showStopRecording: Boolean) {
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.notify(NOTIFICATION_ID, createNotification(showStopRecording))
  }

  private fun createNotification(addStopRecordingAction: Boolean): Notification {
    val startIntent = Intent(this, MainActivity::class.java)
    startIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
    val contentIntent = PendingIntent.getActivity(
      this, 0, startIntent,
      PendingIntent.FLAG_UPDATE_CURRENT
    )

    val exitIntent = Intent(this, MainActivity::class.java)
    exitIntent.putExtra(MainActivity.EXIT_EXTRA, true)
    exitIntent.action = "exit"
    val exitPendingIntent = PendingIntent.getActivity(
      this, 1, exitIntent,
      PendingIntent.FLAG_UPDATE_CURRENT
    )

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
      val stopRecordingIntent = Intent(this, MainActivity::class.java)
      stopRecordingIntent.putExtra(MainActivity.STOP_RECORDING_EXTRA, true)
      stopRecordingIntent.action = "stop recording"
      val stopRecordingPendingIntent = PendingIntent.getActivity(
        this, 2,
        stopRecordingIntent, PendingIntent.FLAG_UPDATE_CURRENT
      )
      val stopRecordingAction = NotificationCompat.Action.Builder(
        R.drawable.ic_stop_white_24dp,
        getString(R.string.stop_recording), stopRecordingPendingIntent
      )
        .build()

      builder.addAction(stopRecordingAction)
    }

    if (Build.VERSION.SDK_INT < 21) {
      builder.setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
    }

    return builder.build()
  }

  @TargetApi(26)
  private fun createNotificationChannel() {
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val nc = NotificationChannel(
      NOTIFICATION_CHANNEL,
      getString(R.string.logcat_service_channel_name), NotificationManager.IMPORTANCE_LOW
    )
    nc.enableLights(false)
    nc.enableVibration(false)
    nm.createNotificationChannel(nc)
  }

  @TargetApi(26)
  private fun deleteNotificationChannel() {
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.deleteNotificationChannel(NOTIFICATION_CHANNEL)
  }

  override fun onDestroy() {
    super.onDestroy()
    logcat.close()

    if (Build.VERSION.SDK_INT >= 26) {
      deleteNotificationChannel()
    }
  }

  override fun onSharedPreferenceChanged(
    sharedPreferences: SharedPreferences,
    key: String
  ) {
    super.onSharedPreferenceChanged(sharedPreferences, key)
    when (key) {
      PreferenceKeys.Logcat.KEY_POLL_INTERVAL -> {
        val pollInterval = sharedPreferences.getString(
          key,
          PreferenceKeys.Logcat.Default.POLL_INTERVAL
        )!!.trim().toLong()
        logcat.setPollInterval(pollInterval)
      }
      PreferenceKeys.Logcat.KEY_BUFFERS -> handleBufferUpdate(sharedPreferences, key)
      PreferenceKeys.Logcat.KEY_MAX_LOGS -> {
        val newCapacity = sharedPreferences.getString(
          PreferenceKeys.Logcat.KEY_MAX_LOGS,
          PreferenceKeys.Logcat.Default.MAX_LOGS
        )!!.trim().toInt()

        showToast(getString(R.string.restarting_logcat))

        logcat.stop()
        restartedLogcat = true
        logcat.setMaxLogsCount(newCapacity)
        logcat.start()
      }
    }
  }

  private fun handleBufferUpdate(
    sharedPreferences: SharedPreferences,
    key: String
  ) {
    val bufferValues = sharedPreferences.getStringSet(key, PreferenceKeys.Logcat.Default.BUFFERS)!!
    val buffers = Logcat.AVAILABLE_BUFFERS

    showToast(getString(R.string.restarting_logcat))

    restartedLogcat = true
    logcat.logcatBuffers = bufferValues.map { e -> buffers[e.toInt()].toLowerCase() }.toSet()
    logcat.restart()
  }

  private fun initLogcat() {
    val sharedPreferences = getDefaultSharedPreferences()
    val bufferValues = sharedPreferences.getStringSet(
      PreferenceKeys.Logcat.KEY_BUFFERS,
      PreferenceKeys.Logcat.Default.BUFFERS
    )!!
    val pollInterval = sharedPreferences.getString(
      PreferenceKeys.Logcat.KEY_POLL_INTERVAL,
      PreferenceKeys.Logcat.Default.POLL_INTERVAL
    )!!.trim().toLong()
    val maxLogs = sharedPreferences.getString(
      PreferenceKeys.Logcat.KEY_MAX_LOGS,
      PreferenceKeys.Logcat.Default.MAX_LOGS
    )!!.trim().toInt()

    logcat = Logcat(maxLogs)
    logcat.setPollInterval(pollInterval)

    val buffers = Logcat.AVAILABLE_BUFFERS
    logcat.logcatBuffers = bufferValues.mapNotNull { e ->
      buffers.getOrNull(e.toInt())
        ?.toLowerCase(Locale.getDefault())
    }.toSet().ifEmpty {
      sharedPreferences.edit {
        putStringSet(
          PreferenceKeys.Logcat.KEY_BUFFERS,
          PreferenceKeys.Logcat.Default.BUFFERS
        )
      }
      Logcat.DEFAULT_BUFFERS
    }
    logcat.start()
  }
}