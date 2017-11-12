package com.dp.logcatapp.fragments.logcatlive

import android.arch.lifecycle.ViewModel
import com.dp.logcat.Log

class LogcatLiveViewModel : ViewModel() {
    val logs: ArrayList<Log> = ArrayList()
    var isRecording: Boolean = false
}