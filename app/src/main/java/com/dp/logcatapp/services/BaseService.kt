package com.dp.logcatapp.services

import android.app.Service
import com.dp.logcatapp.util.setTheme

abstract class BaseService : Service() {
    override fun onCreate() {
        setTheme()
        super.onCreate()
    }
}