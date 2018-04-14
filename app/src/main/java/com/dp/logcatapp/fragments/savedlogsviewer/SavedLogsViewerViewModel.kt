package com.dp.logcatapp.fragments.savedlogsviewer

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.net.Uri
import android.os.AsyncTask
import com.dp.logcat.Log
import com.dp.logcat.LogcatStreamReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference

internal class SavedLogsViewerViewModel(application: Application) : AndroidViewModel(application) {
    var autoScroll = true
    var scrollPosition = 0

    lateinit var logs: LogsLiveData

    private var init = false
    fun init(uri: Uri) {
        if (!init) {
            logs = LogsLiveData(getApplication(), uri)
            init = true
        }
    }
}

internal class LogsLiveData(application: Application, uri: Uri) : LiveData<List<Log>>() {

    init {
        load(application, uri)
    }

    private fun load(application: Application, uri: Uri) {
        try {
            val inputStream = application.contentResolver.openInputStream(uri)
            Loader(this).execute(inputStream)
        } catch (e: FileNotFoundException) {
            // ignore
        }
    }

    class Loader(savedLogsLiveData: LogsLiveData) : AsyncTask<InputStream, Void, List<Log>>() {

        private val ref: WeakReference<LogsLiveData> = WeakReference(savedLogsLiveData)

        override fun doInBackground(vararg params: InputStream?): List<Log> {
            val logs = mutableListOf<Log>()

            val inputStream = params[0]
            if (inputStream != null) {
                try {
                    val reader = LogcatStreamReader(inputStream)
                    for (log in reader) {
                        logs += log
                    }
                } catch (e: IOException) {
                    // ignore
                }
            }

            return logs
        }

        override fun onPostExecute(result: List<Log>) {
            val savedLogsLiveData = ref.get()
            if (savedLogsLiveData != null) {
                savedLogsLiveData.value = result
            }
        }
    }
}