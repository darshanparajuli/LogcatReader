package com.dp.logcatapp.util

import android.content.Context
import android.graphics.Typeface
import android.support.v7.preference.PreferenceManager
import android.widget.Toast
import com.dp.logcatapp.R
import java.util.*

private val cache = mutableMapOf<String, Typeface>()

fun Context.showToast(msg: CharSequence, length: Int = Toast.LENGTH_SHORT) =
        Toast.makeText(this, msg, length).show()

fun Context.getRobotoTypeface(name: String) = getTypeface("Roboto/$name.ttf")

fun Context.getTypeface(assetPath: String): Typeface? {
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
