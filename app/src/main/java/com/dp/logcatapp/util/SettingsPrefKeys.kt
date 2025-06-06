package com.dp.logcatapp.util

import com.dp.logcat.LogcatUtil

object SettingsPrefKeys {

  object General {
    const val KEY_KEEP_SCREEN_ON = "pref_key_general_keep_screen_on"
    const val KEY_FILTER_ON_SEARCH = "pref_key_general_filter_on_search"

    object Default {
      const val KEY_KEEP_SCREEN_ON = false
      const val KEY_FILTER_ON_SEARCH = false
    }
  }

  object Appearance {

    const val KEY_THEME = "pref_key_appearance_color"
    const val KEY_DYNAMIC_COLOR = "pref_key_appearance_dynamic_color"

    object Theme {

      const val AUTO = "0"
      const val LIGHT = "1"
      const val DARK = "2"
    }

    object Default {

      const val THEME = Theme.AUTO
      const val DYNAMIC_COLOR = true
    }
  }

  object Logcat {
    const val KEY_POLL_INTERVAL = "pref_key_logcat_poll_interval_integer"
    const val KEY_BUFFERS = "pref_key_logcat_buffers"
    const val KEY_MAX_LOGS = "pref_key_logcat_max_logs_integer"
    const val KEY_SAVE_LOCATION = "pref_key_logcat_save_location"

    object Default {
      const val POLL_INTERVAL = 250
      val BUFFERS: Set<String> = getDefaultBufferValues()
      const val MAX_LOGS = 300_000
      const val SAVE_LOCATION = ""

      private fun getDefaultBufferValues(): Set<String> {
        val bufferValues = mutableSetOf<String>()
        LogcatUtil.DEFAULT_BUFFERS.map { LogcatUtil.AVAILABLE_BUFFERS.indexOf(it) }
          .filter { it != -1 }
          .forEach { bufferValues += it.toString() }
        return bufferValues
      }
    }
  }
}