package com.dp.logcatapp.util

object PreferenceKeys {

    const val MAIN_PREF_SCREEN = "pref_key_main_screen"

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

        object Buffers {
            const val CRASH = "0"
            const val EVENTS = "1"
            const val MAIN = "2"
            const val RADIO = "3"
            const val SYSTEM = "4"
        }

        object Default {
            const val POLL_INTERVAL = "250"
            val BUFFERS = setOf(Buffers.CRASH, Buffers.MAIN, Buffers.SYSTEM)
        }
    }

    object About {
        const val KEY_VERSION_NAME = "pref_key_version_name"
    }
}