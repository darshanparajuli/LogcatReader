package com.dp.logcatapp.fragments.savedlogs

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.os.AsyncTask
import com.dp.logcatapp.fragments.logcatlive.LogcatLiveFragment
import java.io.File
import java.lang.ref.WeakReference

internal class SavedLogsViewModel(application: Application) : AndroidViewModel(application) {
    val fileNames: SavedLogsLiveData = SavedLogsLiveData(application)
}

internal class SavedLogsResult {
    var totalSize = ""
    val fileNames = mutableListOf<String>()
}

internal class SavedLogsLiveData(private val application: Application) :
        LiveData<SavedLogsResult>() {

    init {
        load()
    }

    private fun load() {
        val folder = File(application.filesDir, LogcatLiveFragment.LOGCAT_DIR)
        Loader(this).execute(folder)
    }

    class Loader(savedLogsLiveData: SavedLogsLiveData) : AsyncTask<File, Void, SavedLogsResult>() {

        private val ref: WeakReference<SavedLogsLiveData> = WeakReference(savedLogsLiveData)

        override fun doInBackground(vararg params: File?): SavedLogsResult {
            val savedLogsResult = SavedLogsResult()

            val files = params[0]!!.listFiles()

            var totalSize = 0.toDouble()
            if (files != null) {
                for (f in files) {
                    savedLogsResult.fileNames += f.name
                    totalSize += f.length()
                }
            }

            if (totalSize > 0) {
                val units = arrayOf("B", "KB", "MB", "GB", "TB")
                var unit = units[0]
                for (i in 1 until units.size) {
                    if (totalSize >= 1024) {
                        totalSize /= 1024
                        unit = units[i]
                    } else {
                        break
                    }
                }

                savedLogsResult.totalSize = "%.2f %s".format(totalSize, unit)
            }

            return savedLogsResult
        }

        override fun onPostExecute(result: SavedLogsResult) {
            val savedLogsLiveData = ref.get()
            if (savedLogsLiveData != null) {
                savedLogsLiveData.value = result
            }
        }
    }
}