package com.dp.logcatapp.fragments.savedlogs

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.support.v4.provider.DocumentFile
import androidx.core.net.toUri
import com.dp.logcat.Logcat
import com.dp.logcat.LogcatStreamReader
import com.dp.logcatapp.fragments.logcatlive.LogcatLiveFragment
import com.dp.logcatapp.util.PreferenceKeys
import com.dp.logcatapp.util.Utils
import com.dp.logcatapp.util.getDefaultSharedPreferences
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
                       val count: Long,
                       val uri: Uri)

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
        val folders = arrayListOf<String>()
        folders.add(File(application.filesDir, LogcatLiveFragment.LOGCAT_DIR).absolutePath)

        val customLocation = application.getDefaultSharedPreferences().getString(
                PreferenceKeys.Logcat.KEY_SAVE_LOCATION,
                ""
        )

        if (customLocation.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= 21) {
                folders.add(customLocation)
            } else {
                folders.add(File(customLocation, LogcatLiveFragment.LOGCAT_DIR).absolutePath)
            }
        }

        Loader(this).execute(*folders.toTypedArray())
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


    class Loader(savedLogsLiveData: SavedLogsLiveData) : AsyncTask<String, Void, SavedLogsResult>() {

        private val ref: WeakReference<SavedLogsLiveData> = WeakReference(savedLogsLiveData)

        override fun doInBackground(vararg params: String): SavedLogsResult {
            val savedLogsResult = SavedLogsResult()
            var totalSize = collectFiles(savedLogsResult, params[0])

            if (params.size == 2) {
                if (Build.VERSION.SDK_INT >= 21) {
                    val savedLogsLiveData = ref.get()
                    if (savedLogsLiveData != null) {
                        totalSize += collectFilesFromCustomLocation(savedLogsLiveData.application,
                                savedLogsResult, params[1])
                    }
                } else {
                    totalSize += collectFiles(savedLogsResult, params[1])
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

        private fun collectFiles(savedLogsResult: SavedLogsResult, path: String): Double {
            val files = File(path).listFiles()
            var totalSize = 0.toDouble()
            if (files != null) {
                for (f in files) {
                    val size = f.length()
                    val count = countLogs(f)
                    val fileInfo = LogFileInfo(f.name, size, Utils.sizeToString(size.toDouble()),
                            count, f.toUri())
                    savedLogsResult.logFiles += fileInfo
                    totalSize += fileInfo.size
                }
            }
            return totalSize
        }

        private fun collectFilesFromCustomLocation(context: Context,
                                                   savedLogsResult: SavedLogsResult,
                                                   path: String): Double {
            var totalSize = 0.0
            val uri = path.toUri()
            val folder = DocumentFile.fromTreeUri(context, uri)
            val files = folder.listFiles()
            if (files != null) {
                for (f in files) {
                    val size = f.length()
                    val count = countLogs(context, f)
                    val fileInfo = LogFileInfo(f.name, size, Utils.sizeToString(size.toDouble()),
                            count, f.uri)
                    savedLogsResult.logFiles += fileInfo
                    totalSize += fileInfo.size
                }
            }
            return totalSize
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

        private fun countLogs(context: Context, file: DocumentFile): Long {
            val logCount = Logcat.getLogCountFromHeader(context, file)
            if (logCount != -1L) {
                return logCount
            }

            return try {
                val inputStream = context.contentResolver.openInputStream(file.uri)
                val reader = LogcatStreamReader(inputStream)
                val logs = reader.asSequence().toList()

                if (!Logcat.writeToFile(context, logs, file.uri)) {
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