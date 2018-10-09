package com.dp.logcatapp.fragments.savedlogs

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.net.toFile
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.dp.logcat.Logcat
import com.dp.logcat.LogcatStreamReader
import com.dp.logcatapp.db.MyDB
import com.dp.logcatapp.db.SavedLogInfo
import com.dp.logcatapp.fragments.logcatlive.LogcatLiveFragment
import com.dp.logcatapp.util.Utils
import com.dp.logger.Logger
import kotlinx.coroutines.experimental.Dispatchers.IO
import kotlinx.coroutines.experimental.Dispatchers.Main
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.io.File
import java.io.FileInputStream
import java.io.IOException

internal class SavedLogsViewModel(application: Application) : AndroidViewModel(application) {
    val fileNames: SavedLogsLiveData = SavedLogsLiveData(application)
    val selectedItems = mutableSetOf<Int>()
}

data class LogFileInfo(val info: SavedLogInfo,
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

    private var job: Job? = null

    init {
        load()
    }

    fun update(fileInfoList: List<LogFileInfo>) {
        val savedLogsResult = SavedLogsResult()
        savedLogsResult.logFiles += fileInfoList
        savedLogsResult.totalLogCount = fileInfoList.foldRight(0L) { logFileInfo, acc ->
            acc + logFileInfo.count
        }

        val folder = File(application.filesDir, LogcatLiveFragment.LOGCAT_DIR)
        val totalSize = fileInfoList.map { File(folder, it.info.fileName).length() }.sum()
        if (totalSize > 0) {
            savedLogsResult.totalSize = Utils.bytesToString(totalSize)
        }

        value = savedLogsResult
    }

    fun load() {
        job?.cancel()
        job = GlobalScope.launch(Main) {
            val db = MyDB.getInstance(application)
            val result = async(IO) { loadAsync(db) }.await()
            value = result
        }
    }

    private fun loadAsync(db: MyDB): SavedLogsResult {
        val savedLogsResult = SavedLogsResult()
        var totalSize = 0L

        updateDBWithExistingInternalLogFiles(db)

        val savedLogInfoList = db.savedLogsDao().getAllSync()
        for (info in savedLogInfoList) {
            if (info.isCustom && Build.VERSION.SDK_INT >= 21) {
                val file = DocumentFile.fromSingleUri(application, Uri.parse(info.path))
                if (file == null || file.name == null) {
                    Logger.logDebug(this::class, "file name is null")
                    continue
                }

                val size = file.length()
                val count = countLogs(application, file)
                val fileInfo = LogFileInfo(info, size, Utils.bytesToString(size), count)
                savedLogsResult.logFiles += fileInfo
                totalSize += fileInfo.size
            } else {
                val file = Uri.parse(info.path).toFile()
                val size = file.length()
                val count = countLogs(file)
                val fileInfo = LogFileInfo(info, size, Utils.bytesToString(size), count)
                savedLogsResult.logFiles += fileInfo
                totalSize += fileInfo.size
            }
        }

        savedLogsResult.totalLogCount = savedLogsResult.logFiles
                .foldRight(0L) { logFileInfo, acc ->
                    acc + logFileInfo.count
                }
        savedLogsResult.logFiles.sortBy { it.info.fileName }

        if (totalSize > 0) {
            savedLogsResult.totalSize = Utils.bytesToString(totalSize)
        }

        return savedLogsResult
    }

    private fun updateDBWithExistingInternalLogFiles(db: MyDB) {
        val files = File(application.cacheDir, LogcatLiveFragment.LOGCAT_DIR).listFiles()
        if (files != null) {
            val savedLogInfoArray = files.map {
                SavedLogInfo(it.name, it.absolutePath, false)
            }.toTypedArray()
            db.savedLogsDao().insert(*savedLogInfoArray)
        }
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
            val reader = LogcatStreamReader(inputStream!!)
            val logs = reader.asSequence().toList()

            if (!Logcat.writeToFile(context, logs, file.uri)) {
                Logger.logDebug(SavedLogsViewModel::class, "Failed to write log header")
            }

            logs.size.toLong()
        } catch (e: IOException) {
            0L
        }
    }
}