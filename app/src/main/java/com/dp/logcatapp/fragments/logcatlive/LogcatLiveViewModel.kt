package com.dp.logcatapp.fragments.logcatlive

import android.arch.lifecycle.ViewModel

class LogcatLiveViewModel : ViewModel() {
    var autoScroll = true
    var scrollPosition = 0
    var paused = false
}