package com.dp.logcatapp.fragments.logcatlive

import android.arch.lifecycle.ViewModel

class LogcatLiveViewModel : ViewModel() {
    var autoScroll: Boolean = true
    var scrollPosition: Int = 0
}