package com.dp.logcatapp

import android.app.Application
import com.dp.logger.Logger

class LogcatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Logger.init("LogcatReader")
    }
}