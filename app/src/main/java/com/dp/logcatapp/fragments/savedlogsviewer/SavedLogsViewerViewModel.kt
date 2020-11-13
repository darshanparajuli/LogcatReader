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

internal class SavedLogsViewerViewModel(application: Application) : ScopedAndroidViewModel(
  application
) {
  var autoScroll = true
  var scrollPosition = 0

  private var logs = MutableLiveData<SavedLogsResult>()

  fun init(uri: Uri) {
    launch {
      logs.value = withContext(IO) { load(getApplication(), uri) }
    }
  }

  private suspend fun load(
    context: Context,
    uri: Uri
  ) = coroutineScope {
    val logs = mutableListOf<Log>()
    var availableBytes = 0

    try {
      context.contentResolver.openInputStream(uri)?.let {
        try {
          availableBytes = it.available()
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
      return@coroutineScope SavedLogsResult.FileOpenError
    }

    if (logs.isEmpty() && availableBytes > 0) {
      return@coroutineScope SavedLogsResult.FileParseError
    }

    SavedLogsResult.Success(logs)
  }

  fun getLogs(): LiveData<SavedLogsResult> = logs

  sealed class SavedLogsResult {
    data class Success(val logs: List<Log>) : SavedLogsResult()
    object FileOpenError : SavedLogsResult()
    object FileParseError : SavedLogsResult()
  }
}
