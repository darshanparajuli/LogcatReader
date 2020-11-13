package com.dp.logcatapp.fragments.savedlogs

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.net.toFile
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dp.logcat.Logcat
import com.dp.logcat.LogcatStreamReader
import com.dp.logcatapp.db.MyDB
import com.dp.logcatapp.db.SavedLogInfo
import com.dp.logcatapp.fragments.logcatlive.LogcatLiveFragment
import com.dp.logcatapp.util.ScopedAndroidViewModel
import com.dp.logcatapp.util.Utils
import com.dp.logger.Logger
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException

internal class SavedLogsViewModel(application: Application) : ScopedAndroidViewModel(application) {
  private val fileNames = MutableLiveData<SavedLogsResult>()
  val selectedItems = mutableSetOf<Int>()

  private var job: Job? = null

  init {
    load()
  }

  fun load() {
    job?.cancel()
    job = launch {
      val db = MyDB.getInstance(getApplication())
      val result = withContext(IO) { loadAsync(db) }
      fileNames.value = result
    }
  }

  private suspend fun loadAsync(db: MyDB): SavedLogsResult = coroutineScope {
    val savedLogsResult = SavedLogsResult()
    var totalSize = 0L

    updateDBWithExistingInternalLogFiles(db)

    val application = getApplication<Application>()
    val savedLogInfoList = db.savedLogsDao().getAllSync()
    for (info in savedLogInfoList) {
      if (info.isCustom && Build.VERSION.SDK_INT >= 21) {
        val file = DocumentFile.fromSingleUri(application, Uri.parse(info.path))
        if (file == null || file.name == null) {
          Logger.debug(this::class, "file name is null")
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

    savedLogsResult
  }

  private fun updateDBWithExistingInternalLogFiles(db: MyDB) {
    val files =
      File(getApplication<Application>().cacheDir, LogcatLiveFragment.LOGCAT_DIR).listFiles()
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
        Logger.debug(SavedLogsViewModel::class, "Failed to write log header")
      }

      logs.size.toLong()
    } catch (e: IOException) {
      0L
    }
  }

  private fun countLogs(
    context: Context,
    file: DocumentFile
  ): Long {
    val logCount = Logcat.getLogCountFromHeader(context, file)
    if (logCount != -1L) {
      return logCount
    }

    return try {
      val inputStream = context.contentResolver.openInputStream(file.uri)
      val reader = LogcatStreamReader(inputStream!!)
      val logs = reader.asSequence().toList()

      if (!Logcat.writeToFile(context, logs, file.uri)) {
        Logger.debug(SavedLogsViewModel::class, "Failed to write log header")
      }

      logs.size.toLong()
    } catch (e: IOException) {
      0L
    }
  }

  fun getFileNames(): LiveData<SavedLogsResult> = fileNames
}

data class LogFileInfo(
  val info: SavedLogInfo,
  val size: Long,
  val sizeStr: String,
  val count: Long
)

internal class SavedLogsResult {
  var totalSize = ""
  var totalLogCount = 0L
  val logFiles = mutableListOf<LogFileInfo>()
}
