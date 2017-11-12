package com.dp.logcatapp.util

import android.app.Activity
import android.app.TaskStackBuilder
import android.content.Intent
import com.dp.logcatapp.activities.BlackBgActivity
import com.dp.logcatapp.activities.DarkBgActivity
import com.dp.logcatapp.activities.MainActivity
import com.dp.logcatapp.activities.SettingsActivity

fun Activity.restartApp() {
    val taskBuilder = TaskStackBuilder.create(this)
    val useBlackTheme = getDefaultSharedPreferences()
            .getBoolean(PreferenceKeys.Appearance.KEY_USE_BLACK_THEME,
                    PreferenceKeys.Appearance.Default.USE_BLACK_THEME)

    if (isDarkThemeOn()) {
        if (useBlackTheme) {
            taskBuilder.addNextIntent(Intent(this, BlackBgActivity::class.java))
        } else {
            taskBuilder.addNextIntent(Intent(this, DarkBgActivity::class.java))
        }
    } else {
        taskBuilder.addNextIntent(Intent(this, MainActivity::class.java))
    }

    taskBuilder.addNextIntent(Intent(this, SettingsActivity::class.java))
    finish()
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    taskBuilder.startActivities()
}