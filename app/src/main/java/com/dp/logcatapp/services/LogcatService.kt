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
import android.os.Binder
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import com.dp.logcat.Logcat
import com.dp.logcatapp.R
import com.dp.logcatapp.activities.MainActivity
import com.dp.logcatapp.util.PreferenceKeys
import com.dp.logcatapp.util.getDefaultSharedPreferences

class LogcatService : BaseService() {

    companion object {
        private const val NOTIFICAION_CHANNEL = "logcat_channel_01"
        private const val NOTIFICAION_ID = 1
    }

    private val localBinder = LocalBinder()
    val logcat: Logcat = Logcat()

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            createNotificationChannel()
        }

        initLogcatPrefs()
        logcat.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICAION_ID, createStartNotification())
        return START_STICKY
    }

    private fun createStartNotification(): Notification {
        val startIntent = Intent(this, MainActivity::class.java)
        startIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        val pendingIntent = PendingIntent.getActivity(this, 0, startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        val exitIntent = Intent(this, MainActivity::class.java)
        exitIntent.putExtra(MainActivity.EXIT_EXTRA, true)
        val closePendingIntent = PendingIntent.getActivity(this, 1, exitIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(this, NOTIFICAION_CHANNEL)
                .setSmallIcon(R.drawable.ic_android_white_24dp)
                .setColor(ContextCompat.getColor(applicationContext, R.color.color_primary))
                .setContentTitle(getString(R.string.app_name))
                .setTicker(getString(R.string.app_name))
                .setContentText(getString(R.string.logcat_service))
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setAutoCancel(false)
                .addAction(NotificationCompat.Action.Builder(R.drawable.ic_clear_white_18dp,
                        getString(R.string.exit), closePendingIntent)
                        .build())

        if (Build.VERSION.SDK_INT < 21) {
            builder.setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
        }

        return builder.build()
    }

    @TargetApi(26)
    private fun createNotificationChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val nc = NotificationChannel(NOTIFICAION_CHANNEL,
                getString(R.string.logcat_service_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT)
        nm.createNotificationChannel(nc)
    }

    @TargetApi(26)
    private fun deleteNotificationChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.deleteNotificationChannel(NOTIFICAION_CHANNEL)
    }

    override fun onBind(intent: Intent?) = localBinder

    override fun onDestroy() {
        super.onDestroy()
        logcat.close()

        if (Build.VERSION.SDK_INT >= 26) {
            deleteNotificationChannel()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        super.onSharedPreferenceChanged(sharedPreferences, key)
        when (key) {
            PreferenceKeys.Logcat.KEY_POLL_INTERVAL -> {
                val pollInterval = sharedPreferences.getString(key,
                        PreferenceKeys.Logcat.Default.POLL_INTERVAL).trim().toLong()
                logcat.setPollInterval(pollInterval)
            }
            PreferenceKeys.Logcat.KEY_BUFFERS -> handleBufferUpdate(sharedPreferences, key)
        }
    }

    private fun handleBufferUpdate(sharedPreferences: SharedPreferences, key: String) {
        val bufferValues = sharedPreferences.getStringSet(key, PreferenceKeys.Logcat.Default.BUFFERS)
        val buffers = resources.getStringArray(R.array.pref_logcat_log_buffers)

        logcat.stop()
        logcat.logcatBuffers = bufferValues.map { e -> buffers[e.toInt()].toLowerCase() }.toSet()
        logcat.start()
    }

    private fun initLogcatPrefs() {
        val sharedPreferences = getDefaultSharedPreferences()
        val bufferValues = sharedPreferences.getStringSet(PreferenceKeys.Logcat.KEY_BUFFERS,
                PreferenceKeys.Logcat.Default.BUFFERS)
        val pollInterval = sharedPreferences.getString(PreferenceKeys.Logcat.KEY_POLL_INTERVAL,
                PreferenceKeys.Logcat.Default.POLL_INTERVAL).trim().toLong()

        logcat.setPollInterval(pollInterval)

        val buffers = resources.getStringArray(R.array.pref_logcat_log_buffers)
        logcat.logcatBuffers = bufferValues.map { e -> buffers[e.toInt()].toLowerCase() }.toSet()
    }

    inner class LocalBinder : Binder() {
        fun getLogcatService() = this@LogcatService
    }
}