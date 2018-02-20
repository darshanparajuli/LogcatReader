package com.dp.logcatapp.fragments.savedlogs

import android.annotation.SuppressLint
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.content.Context
import android.os.AsyncTask
import com.dp.logcatapp.fragments.logcatlive.LogcatLiveFragment

internal class SavedLogsViewModel(application: Application) : AndroidViewModel(application) {
    val fileNames: SavedLogsLiveData = SavedLogsLiveData(application)
}

internal class SavedLogsLiveData(val context: Context) : LiveData<List<String>>() {

    init {
        load()
    }

    @SuppressLint("StaticFieldLeak")
    private fun load() {
        object : AsyncTask<Void, Void, List<String>>() {
            override fun doInBackground(vararg params: Void?): List<String> {
                val fileNames = mutableListOf<String>()
                val files = LogcatLiveFragment.LOGCAT_DIR.listFiles()
                if (files != null) {
                    for (f in files) {
                        fileNames += f.name
                    }
                }
                return fileNames
            }

            override fun onPostExecute(result: List<String>?) {
                value = result
            }
        }.execute()
    }
}