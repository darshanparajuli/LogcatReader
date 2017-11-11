package com.dp.logcatapp.services.logcat

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.IBinder
import android.support.v7.preference.PreferenceManager
import com.dp.logcat.Logcat
import com.dp.logcat.LogcatEventListener

class LogcatService : Service() {
    private val localBinder = LocalBinder()
    private var sharedPreferences: SharedPreferences? = null
    private var onNewLogListener: OnNewLogListener? = null
    private val logcat: Logcat = Logcat()

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onBind(intent: Intent?): IBinder {
        return localBinder
    }

    fun startLogcat(listener: LogcatEventListener): Boolean {
        return logcat.start(listener) == true
    }

    fun stopLogcat() {
        logcat.close()
    }

    fun startRecording() {
    }

    fun stopRecording() {
    }

    fun setOnNewLogListener(onNewLogListener: OnNewLogListener) {
        this.onNewLogListener = onNewLogListener
    }

    override fun onDestroy() {
        super.onDestroy()
        logcat.close()
    }

    inner class LocalBinder : Binder() {
        fun getLogcatService(): LogcatService {
            return this@LogcatService
        }
    }
}