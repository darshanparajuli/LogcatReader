package com.dp.logcatapp.fragments.savedlogsviewer

import android.arch.lifecycle.ViewModel

internal class SavedLogsViewerViewModel : ViewModel() {
    var autoScroll = true
    var scrollPosition = 0
    var showedGrantPermissionInstruction = false
}