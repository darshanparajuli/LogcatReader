package com.dp.logcatapp.fragments.savedlogs

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.os.AsyncTask
import com.dp.logcat.LogcatStreamReader
import com.dp.logcatapp.fragments.logcatlive.LogcatLiveFragment
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.lang.ref.WeakReference

internal class SavedLogsViewModel(application: Application) : AndroidViewModel(application) {
    val fileNames: SavedLogsLiveData = SavedLogsLiveData(application)
    val selectedItems = mutableSetOf<Int>()
}

data class LogFileInfo(val name: String,
                       val size: Long,
                       val sizeStr: String,
                       val count: Long)

internal class SavedLogsResult {
    var totalSize = ""
    val fileNames = mutableListOf<LogFileInfo>()
}

internal class SavedLogsLiveData(private val application: Application) :
        LiveData<SavedLogsResult>() {

    init {
        load()
    }

    internal fun load() {
        val folder = File(application.filesDir, LogcatLiveFragment.LOGCAT_DIR)
        Loader(this).execute(folder)
    }

    fun update(fileNames: List<LogFileInfo>) {
        val savedLogsResult = SavedLogsResult()
        savedLogsResult.fileNames += fileNames

        val folder = File(application.filesDir, LogcatLiveFragment.LOGCAT_DIR)
        val totalSize = fileNames.sumByDouble { File(folder, it.name).length().toDouble() }
        if (totalSize > 0) {
            savedLogsResult.totalSize = sizeToString(totalSize)
        }

        value = savedLogsResult
    }


    class Loader(savedLogsLiveData: SavedLogsLiveData) : AsyncTask<File, Void, SavedLogsResult>() {

        private val ref: WeakReference<SavedLogsLiveData> = WeakReference(savedLogsLiveData)

        override fun doInBackground(vararg params: File?): SavedLogsResult {
            val savedLogsResult = SavedLogsResult()

            val files = params[0]!!.listFiles()

            var totalSize = 0.toDouble()
            if (files != null) {
                for (f in files) {
                    val size = f.length()
                    val count = countLogs(f)
                    val fileInfo = LogFileInfo(f.name, size, sizeToString(size.toDouble()), count)
                    savedLogsResult.fileNames += fileInfo
                    totalSize += fileInfo.size
                }
            }

            savedLogsResult.fileNames.sortBy { it.name }

            if (totalSize > 0) {
                savedLogsResult.totalSize = sizeToString(totalSize)
            }

            return savedLogsResult
        }

        private fun countLogs(file: File): Long {
            return try {
                val reader = LogcatStreamReader(FileInputStream(file))
                var count = 0L
                for (l in reader) ++count
                count
            } catch (e: IOException) {
                0L
            }
        }

        override fun onPostExecute(result: SavedLogsResult) {
            val savedLogsLiveData = ref.get()
            if (savedLogsLiveData != null) {
                savedLogsLiveData.value = result
            }
        }
    }

    companion object {
        fun sizeToString(size: Double): String {
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var unit = units[0]
            var totalSize = size
            for (i in 1 until units.size) {
                if (totalSize >= 1024) {
                    totalSize /= 1024
                    unit = units[i]
                } else {
                    break
                }
            }

            return "%.2f %s".format(totalSize, unit)
        }
    }
}