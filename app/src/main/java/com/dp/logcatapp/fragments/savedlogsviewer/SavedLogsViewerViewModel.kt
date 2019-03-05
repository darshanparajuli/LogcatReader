package com.dp.logcatapp.fragments.savedlogsviewer

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dp.logcat.Log
import com.dp.logcat.LogcatStreamReader
import com.dp.logcatapp.util.ScopedAndroidViewModel
import com.dp.logcatapp.util.closeQuietly
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

internal class SavedLogsViewerViewModel(application: Application) : ScopedAndroidViewModel(application) {
    var autoScroll = true
    var scrollPosition = 0

    private var logs = MutableLiveData<List<Log>>()

    fun init(uri: Uri) {
        launch {
            logs.value = withContext(IO) { load(getApplication(), uri) }
        }
    }

    private suspend fun load(context: Context, uri: Uri) = coroutineScope {
        val logs = mutableListOf<Log>()

        try {
            context.contentResolver.openInputStream(uri)?.let {
                try {
                    val reader = LogcatStreamReader(it)
                    for (log in reader) {
                        logs += log
                    }
                } catch (e: IOException) {
                    // ignore
                } finally {
                    it.closeQuietly()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // ignore
        }

        logs
    }

    fun getLogs(): LiveData<List<Log>> = logs
}
