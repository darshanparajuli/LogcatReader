package com.dp.logcatapp.fragments.filters

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dp.logcat.LogPriority
import com.dp.logcatapp.R
import com.dp.logcatapp.db.FilterInfo
import com.dp.logcatapp.db.MyDB
import com.dp.logcatapp.util.ScopedAndroidViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FiltersViewModel(application: Application) : ScopedAndroidViewModel(application) {
  private val context = application

  private lateinit var filters: MutableLiveData<List<FilterListItem>>

  private var loadFiltersJob: Job? = null

  fun getFilters(isExclusions: Boolean): LiveData<List<FilterListItem>> {
    if (this::filters.isInitialized) {
      return filters
    }

    filters = MutableLiveData()
    reloadFilters(isExclusions)
    return filters
  }

  fun addFilters(
    filters: List<FilterInfo>,
    isExclusions: Boolean
  ) {
    val dao = MyDB.getInstance(context).filterDao()
    launch {
      withContext(IO) {
        dao.insert(*filters.toTypedArray())
      }

      reloadFilters(isExclusions)
    }
  }

  fun deleteFilter(
    filter: FilterInfo,
    isExclusions: Boolean
  ) {
    val dao = MyDB.getInstance(context).filterDao()
    launch {
      withContext(IO) {
        dao.delete(filter)
      }

      reloadFilters(isExclusions)
    }
  }

  fun deleteAllFilters(isExclusions: Boolean) {
    val dao = MyDB.getInstance(context).filterDao()
    launch {
      withContext(IO) {
        dao.deleteAll(isExclusions)
      }

      reloadFilters(isExclusions)
    }
  }

  private fun reloadFilters(isExclusions: Boolean) {
    val dao = MyDB.getInstance(context).filterDao()

    loadFiltersJob?.cancel()
    loadFiltersJob = launch {
      filters.value = withContext(IO) {
        val list = if (isExclusions) {
          dao.getExclusions()
        } else {
          dao.getFilters()
        }

        list.map {
          val displayText: String
          val type: String
          when (it.type) {
            FilterType.LOG_LEVELS -> {
              type = context.getString(R.string.log_level)
              displayText = it.content.split(",")
                .joinToString(", ") { s ->
                  when (s) {
                    LogPriority.ASSERT -> "Assert"
                    LogPriority.ERROR -> "Error"
                    LogPriority.DEBUG -> "Debug"
                    LogPriority.FATAL -> "Fatal"
                    LogPriority.INFO -> "Info"
                    LogPriority.VERBOSE -> "Verbose"
                    LogPriority.WARNING -> "Warning"
                    else -> ""
                  }
                }
            }
            else -> {
              displayText = it.content
              type = when (it.type) {
                FilterType.KEYWORD -> context.getString(R.string.keyword)
                FilterType.TAG -> context.getString(R.string.tag)
                FilterType.PID -> context.getString(R.string.process_id)
                FilterType.TID -> context.getString(R.string.thread_id)
                else -> throw IllegalStateException("invalid type: ${it.type}")
              }
            }
          }
          FilterListItem(type, displayText, it)
        }
      }
    }
  }
}