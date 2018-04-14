package com.dp.logcatapp.fragments.savedlogs

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.os.AsyncTask
import com.dp.logcat.Logcat
import com.dp.logcat.LogcatStreamReader
import com.dp.logcatapp.fragments.logcatlive.LogcatLiveFragment
import com.dp.logcatapp.util.Utils
import com.dp.logger.Logger
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
    var totalLogCount = 0L
    val logFiles = mutableListOf<LogFileInfo>()
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

    fun update(fileInfos: List<LogFileInfo>) {
        val savedLogsResult = SavedLogsResult()
        savedLogsResult.logFiles += fileInfos
        savedLogsResult.totalLogCount = fileInfos.foldRight(0L, { logFileInfo, acc ->
            acc + logFileInfo.count
        })

        val folder = File(application.filesDir, LogcatLiveFragment.LOGCAT_DIR)
        val totalSize = fileInfos.sumByDouble { File(folder, it.name).length().toDouble() }
        if (totalSize > 0) {
            savedLogsResult.totalSize = Utils.sizeToString(totalSize)
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
                    val fileInfo = LogFileInfo(f.name, size, Utils.sizeToString(size.toDouble()), count)
                    savedLogsResult.logFiles += fileInfo
                    totalSize += fileInfo.size
                }
            }

            savedLogsResult.totalLogCount = savedLogsResult.logFiles
                    .foldRight(0L, { logFileInfo, acc ->
                        acc + logFileInfo.count
                    })
            savedLogsResult.logFiles.sortBy { it.name }

            if (totalSize > 0) {
                savedLogsResult.totalSize = Utils.sizeToString(totalSize)
            }

            return savedLogsResult
        }

        private fun countLogs(file: File): Long {
            val logCount = Logcat.getLogCountFromHeader(file)
            if (logCount != -1L) {
                return logCount
            }

            return try {
                val reader = LogcatStreamReader(FileInputStream(file))
                val logs = reader.asSequence().toList()

                if (!Logcat.writeToFile(logs, file)) {
                    Logger.logDebug(SavedLogsViewModel::class, "Failed to write log header")
                }

                logs.size.toLong()
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
}