package com.dp.logcatapp.fragments.logcatlive

import android.arch.lifecycle.ViewModel
import com.dp.logcat.Log

class LogcatLiveViewModel : ViewModel() {
    val logs = mutableListOf<Log>()
    var isRecording: Boolean = false
}