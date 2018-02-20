package com.dp.logcatapp.fragments.logcatlive

import android.arch.lifecycle.ViewModel

internal class LogcatLiveViewModel : ViewModel() {
    var autoScroll = true
    var scrollPosition = 0
    var showedGrantPermissionInstruction = false
    var stopRecording = false
}