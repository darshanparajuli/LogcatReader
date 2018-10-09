package com.dp.logcatapp.fragments.savedlogsviewer

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.dp.logcat.Log
import com.dp.logcat.LogcatStreamReader
import com.dp.logcatapp.util.closeQuietly
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.IOException

internal class SavedLogsViewerViewModel(application: Application) : AndroidViewModel(application) {
    var autoScroll = true
    var scrollPosition = 0

    var logs = LogsLiveData(application)

    fun init(uri: Uri) {
        logs.load(uri)
    }
}

internal class LogsLiveData(private val application: Application) : LiveData<List<Log>>() {
    internal fun load(uri: Uri) {
        GlobalScope.launch(Main) {
            val logs = async(IO) {
                val logs = mutableListOf<Log>()

                try {
                    application.contentResolver.openInputStream(uri)?.let {
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
            }.await()

            value = logs
        }
    }
}