package com.dp.logcatapp.util

import android.app.Activity
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.support.annotation.LayoutRes
import android.support.v4.app.Fragment
import android.support.v7.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.dp.logcatapp.R
import com.dp.logcatapp.activities.MainActivity
import com.dp.logcatapp.activities.SettingsActivity
import java.util.*

//// BEGIN Activity

fun Activity.restartApp() {
    val taskBuilder = TaskStackBuilder.create(this)
            .addNextIntent(Intent(this, MainActivity::class.java))
            .addNextIntent(Intent(this, SettingsActivity::class.java))
    finish()
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    taskBuilder.startActivities()
}

//// END Activity


//// BEGIN Fragment

fun Fragment.inflateLayout(@LayoutRes layoutResId: Int): View = LayoutInflater.from(activity)
        .inflate(layoutResId, null, false)

//// END Fragment


//// BEGIN Context

private val cache = mutableMapOf<String, Typeface>()

fun Context.showToast(msg: CharSequence, length: Int = Toast.LENGTH_SHORT) =
        Toast.makeText(this, msg, length).show()

fun Context.getTypeface(name: String): Typeface? {
    val assetPath = "fonts/$name.ttf"
    var typeface = cache[assetPath]
    if (typeface == null) {
        typeface = Typeface.createFromAsset(assets, assetPath)
        cache[assetPath] = typeface
    }
    return typeface
}

fun Context.getDefaultSharedPreferences() =
        PreferenceManager.getDefaultSharedPreferences(this)

private fun isDarkThemeTime() = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) !in 7..17

fun Context.isDarkThemeOn(): Boolean {
    val theme = getDefaultSharedPreferences()
            .getString(PreferenceKeys.Appearance.KEY_THEME, PreferenceKeys.Appearance.Default.THEME)
    return theme == PreferenceKeys.Appearance.Theme.DARK ||
            (theme == PreferenceKeys.Appearance.Theme.AUTO && isDarkThemeTime())
}

private fun Context.setThemeAuto(useBlackTheme: Boolean) {
    if (isDarkThemeTime()) {
        setThemeDark(useBlackTheme)
    } else {
        setThemeLight()
    }
}

private fun Context.setThemeDark(useBlackTheme: Boolean) {
    if (useBlackTheme) {
        setTheme(R.style.BlackTheme)
    } else {
        setTheme(R.style.DarkTheme)
    }
}

private fun Context.setThemeLight() {
    setTheme(R.style.LightTheme)
}

fun Context.setTheme() {
    val prefs = getDefaultSharedPreferences()
    val theme = prefs.getString(PreferenceKeys.Appearance.KEY_THEME,
            PreferenceKeys.Appearance.Default.THEME)
    val useBlackTheme = prefs.getBoolean(PreferenceKeys.Appearance.KEY_USE_BLACK_THEME,
            PreferenceKeys.Appearance.Default.USE_BLACK_THEME)
    when (theme) {
        PreferenceKeys.Appearance.Theme.AUTO -> setThemeAuto(useBlackTheme)
        PreferenceKeys.Appearance.Theme.DARK -> setThemeDark(useBlackTheme)
        PreferenceKeys.Appearance.Theme.LIGHT -> setThemeLight()
    }
}

//// END Context


//// BEGIN String

fun String.containsIgnoreCase(other: String) = toLowerCase().contains(other.toLowerCase())

//// END String