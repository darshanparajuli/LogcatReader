package com.dp.logcatapp.util

object PreferenceKeys {

    const val MAIN_PREF_SCREEN = "pref_key_main_screen"

    object General {
        const val KEY_KEEP_SCREEN_ON = "pref_key_general_keep_screen_on"

        object Default {
            const val KEY_KEEP_SCREEN_ON = false
        }
    }

    object Appearance {

        const val KEY_THEME = "pref_key_appearance_theme"
        const val KEY_USE_BLACK_THEME = "pref_key_appearance_use_black_theme"

        object Theme {

            const val AUTO = "0"
            const val LIGHT = "1"
            const val DARK = "2"
        }

        object Default {

            const val THEME = Theme.AUTO
            const val USE_BLACK_THEME = false
        }
    }

    object Logcat {
        const val KEY_POLL_INTERVAL = "pref_key_logcat_poll_interval"
        const val KEY_BUFFERS = "pref_key_logcat_buffers"
        const val KEY_MAX_LOGS = "pref_key_logcat_max_logs"
        const val KEY_SAVE_LOCATION = "pref_key_logcat_save_location"

        object Default {
            const val POLL_INTERVAL = "250"
            val BUFFERS: Set<String> = getDefaultBufferValues()
            const val MAX_LOGS = com.dp.logcat.Logcat.INITIAL_LOG_CAPACITY.toString()
            const val SAVE_LOCATION = ""

            private fun getDefaultBufferValues(): Set<String> {
                val bufferValues = mutableSetOf<String>()
                com.dp.logcat.Logcat.DEFAULT_BUFFERS
                        .map { com.dp.logcat.Logcat.AVAILABLE_BUFFERS.indexOf(it) }
                        .filter { it != -1 }
                        .forEach { bufferValues += it.toString() }
                return bufferValues
            }
        }
    }

    object About {
        const val KEY_VERSION_NAME = "pref_key_about_version_name"
        const val KEY_GITHUB_PAGE = "pref_key_about_github_repo"
    }
}