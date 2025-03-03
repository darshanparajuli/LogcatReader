package com.dp.logcatapp.fragments.logcatlive

import android.app.Application
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dp.logcat.Log
import com.dp.logcat.Logcat
import com.dp.logcatapp.db.FilterInfo
import com.dp.logcatapp.db.MyDB
import com.dp.logcatapp.db.SavedLogInfo
import com.dp.logcatapp.fragments.logcatlive.LogcatLiveFragment.Companion.LOGCAT_DIR
import com.dp.logcatapp.util.PreferenceKeys
import com.dp.logcatapp.util.ScopedAndroidViewModel
import com.dp.logcatapp.util.Utils
import com.dp.logcatapp.util.getDefaultSharedPreferences
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class LogcatLiveViewModel(application: Application) : ScopedAndroidViewModel(application) {
  var autoScroll = true
  var scrollPosition = 0
  var showedGrantPermissionInstruction = false
  var stopRecording = false

  private val fileSaveNotifier = MutableLiveData<SaveInfo>()
  var alreadySaved = true
    private set

  val filters: Flow<List<FilterInfo>>
    get() = MyDB.getInstance(getApplication()).filterDao().filters()

  fun getFileSaveNotifier(): LiveData<SaveInfo> = fileSaveNotifier

  fun save(f: () -> List<Log>?) {
    launch {
      alreadySaved = false
      fileSaveNotifier.value = SaveInfo(SaveInfo.IN_PROGRESS)
      fileSaveNotifier.value = withContext(IO) { saveAsync(f) }
      alreadySaved = true
    }
  }

  private suspend fun saveAsync(f: () -> List<Log>?): SaveInfo = coroutineScope {
    val saveInfo = SaveInfo(SaveInfo.ERROR_SAVING)

    val logs = f().orEmpty()
    if (logs.isEmpty()) {
      saveInfo.result = SaveInfo.ERROR_EMPTY_LOGS
    } else {
      getUri()?.let { uri ->
        saveInfo.uri = uri

        val context = getApplication<Application>()
        val isUsingCustomLocation = Utils.isUsingCustomSaveLocation(context)
        val result = if (isUsingCustomLocation) {
          Logcat.writeToFile(context, logs, uri)
        } else {
          Logcat.writeToFile(logs, uri.toFile())
        }

        saveInfo.result = SaveInfo.ERROR_SAVING
        if (result) {
          val fileName = if (isUsingCustomLocation) {
            DocumentFile.fromSingleUri(context, uri)?.name
          } else {
            uri.toFile().name
          }

          if (fileName != null) {
            val db = MyDB.getInstance(context)
            db.savedLogsDao().insert(
              SavedLogInfo(
                fileName,
                uri.toString(), isUsingCustomLocation
              )
            )

            saveInfo.result = SaveInfo.SUCCESS
            saveInfo.fileName = fileName
          }
        }
      }
    }

    saveInfo
  }

  private fun getUri(): Uri? {
    val timeStamp = SimpleDateFormat("MM-dd-yyyy_HH-mm-ss", Locale.getDefault())
      .format(Date())
    val fileName = "logcat_$timeStamp"

    val context = getApplication<Application>()
    val saveLocationPref = context.getDefaultSharedPreferences().getString(
      PreferenceKeys.Logcat.KEY_SAVE_LOCATION,
      PreferenceKeys.Logcat.Default.SAVE_LOCATION
    )!!

    return if (saveLocationPref.isEmpty()) {
      val file = File(context.filesDir, LOGCAT_DIR)
      file.mkdirs()
      File(file, "$fileName.txt").toUri()
    } else {
      val documentFile = DocumentFile.fromTreeUri(context, Uri.parse(saveLocationPref))
      val file = documentFile?.createFile("text/plain", fileName)
      file?.uri
    }
  }
}

internal data class SaveInfo(
  var result: Int,
  var fileName: String? = null,
  var uri: Uri? = null
) {
  companion object {
    const val SUCCESS = 0
    const val ERROR_EMPTY_LOGS = 1
    const val ERROR_SAVING = 2
    const val IN_PROGRESS = 4
  }
}
