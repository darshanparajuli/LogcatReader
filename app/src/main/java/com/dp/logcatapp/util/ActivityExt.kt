package com.dp.logcatapp.util

import android.app.Activity
import android.app.TaskStackBuilder
import android.content.Intent
import com.dp.logcatapp.activities.MainActivity
import com.dp.logcatapp.activities.SettingsActivity

fun Activity.restartApp() {
    val taskBuilder = TaskStackBuilder.create(this)
            .addNextIntent(Intent(this, MainActivity::class.java))
            .addNextIntent(Intent(this, SettingsActivity::class.java))
    finish()
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    taskBuilder.startActivities()
}