package com.dp.logcatapp.util

import android.app.Activity
import android.app.TaskStackBuilder
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.support.annotation.LayoutRes
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.support.v4.content.FileProvider
import android.support.v7.preference.PreferenceManager
import android.view.*
import android.widget.Toast
import com.dp.logcatapp.R
import com.dp.logcatapp.activities.MainActivity
import com.dp.logcatapp.activities.SettingsActivity
import com.dp.logger.Logger
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
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


//// BEGIN Snackbar

fun showSnackbar(view: View?, msg: String, length: Int = Snackbar.LENGTH_SHORT) {
    newSnakcbar(view, msg, length)?.show()
}

fun newSnakcbar(view: View?, msg: String, length: Int = Snackbar.LENGTH_SHORT): Snackbar? {
    if (view != null) {
        return Snackbar.make(view, msg, length)
    }
    return null
}

//// END Snackbar

//// BEGIN Fragment

fun Fragment.inflateLayout(@LayoutRes layoutResId: Int, root: ViewGroup? = null,
                           attachToRoot: Boolean = false): View =
        activity!!.inflateLayout(layoutResId, root, attachToRoot)

//// END Fragment


//// BEGIN FragmentManager

inline fun FragmentManager.transaction(allowStateLoss: Boolean = false,
                                       now: Boolean = false,
                                       body: FragmentTransaction.() -> Unit) {
    val transaction = beginTransaction()
    transaction.body()
    if (allowStateLoss) {
        if (now) {
            transaction.commitNowAllowingStateLoss()
        } else {
            transaction.commitAllowingStateLoss()
        }
    } else {
        if (now) {
            transaction.commitNow()
        } else {
            transaction.commit()
        }
    }
}

//// END FragmentManager


//// BEGIN Context

private val typefaceCache = mutableMapOf<String, Typeface>()

// Bug find/workaround credit: https://github.com/drakeet/ToastCompat#why
fun Context.showToast(msg: CharSequence, length: Int = Toast.LENGTH_SHORT) {
    val toast = Toast.makeText(this, msg, length)
    if (Build.VERSION.SDK_INT <= 25) {
        try {
            val field = View::class.java.getDeclaredField("mContext")
            field.isAccessible = true
            field.set(toast.view, ToastViewContextWrapper(this))
        } catch (e: Exception) {
        }
    }
    toast.show()
}

private class ToastViewContextWrapper(base: Context) : ContextWrapper(base) {
    override fun getApplicationContext(): Context =
            ToastViewApplicationContextWrapper(baseContext.applicationContext)
}

private class ToastViewApplicationContextWrapper(base: Context) : ContextWrapper(base) {
    override fun getSystemService(name: String?): Any {
        return if (name == Context.WINDOW_SERVICE) {
            ToastWindowManager(baseContext.getSystemService(name) as WindowManager)
        } else {
            super.getSystemService(name)
        }
    }
}

private class ToastWindowManager(val base: WindowManager) : WindowManager {
    override fun getDefaultDisplay(): Display = base.defaultDisplay

    override fun addView(view: View?, params: ViewGroup.LayoutParams?) {
        try {
            base.addView(view, params)
        } catch (e: WindowManager.BadTokenException) {
            Logger.logError("Toast", "caught BadTokenException crash")
        }
    }

    override fun updateViewLayout(view: View?, params: ViewGroup.LayoutParams?) =
            base.updateViewLayout(view, params)

    override fun removeView(view: View?) = base.removeView(view)

    override fun removeViewImmediate(view: View?) = base.removeViewImmediate(view)
}

fun Context.getTypeface(name: String): Typeface? {
    val assetPath = "fonts/$name.ttf"
    var typeface = typefaceCache[assetPath]
    if (typeface == null) {
        typeface = Typeface.createFromAsset(assets, assetPath)
        typefaceCache[assetPath] = typeface
    }
    return typeface
}

fun Context.getDefaultSharedPreferences(): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(this)

private fun isDarkThemeTime() = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) !in 7..17

fun Context.isDarkThemeOn(): Boolean {
    val theme = getDefaultSharedPreferences()
            .getString(PreferenceKeys.Appearance.KEY_THEME, PreferenceKeys.Appearance.Default.THEME)
    return theme == PreferenceKeys.Appearance.Theme.DARK ||
            (theme == PreferenceKeys.Appearance.Theme.AUTO && isDarkThemeTime())
}

private fun Context.setThemeAuto() {
    if (isDarkThemeTime()) {
        setThemeDark()
    } else {
        setThemeLight()
    }
}

private fun Context.setThemeDark() {
    val useBlackTheme = getDefaultSharedPreferences().getBoolean(PreferenceKeys
            .Appearance.KEY_USE_BLACK_THEME, PreferenceKeys.Appearance.Default.USE_BLACK_THEME)
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
    when (theme) {
        PreferenceKeys.Appearance.Theme.AUTO -> setThemeAuto()
        PreferenceKeys.Appearance.Theme.DARK -> setThemeDark()
        PreferenceKeys.Appearance.Theme.LIGHT -> setThemeLight()
    }
}

fun Context.getUriFromFile(file: File): Uri = FileProvider.getUriForFile(this,
        "$packageName.file_provider", file)

fun Context.getFileNameFromUri(uri: Uri): String {
    var name: String? = null
    if (uri.scheme == "content") {
        val cursor = contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        } finally {
            cursor?.close()
        }
    }

    if (name == null) {
        name = uri.path!!
        val lastSlashIndex = name.lastIndexOf('/')
        if (lastSlashIndex != -1) {
            name = name.substring(lastSlashIndex + 1)
        }
    }

    return name
}

fun Context.inflateLayout(@LayoutRes layoutResId: Int, root: ViewGroup? = null,
                          attachToRoot: Boolean = false): View =
        LayoutInflater.from(this).inflate(layoutResId, root, attachToRoot)

//// END Context


//// BEGIN String

fun String.containsIgnoreCase(other: String) = toLowerCase().contains(other.toLowerCase())

//// END String


//// BEGIN Misc

fun InputStream.closeQuietly() {
    try {
        close()
    } catch (e: IOException) {
    }
}

fun OutputStream.closeQuietly() {
    try {
        close()
    } catch (e: IOException) {
    }
}

//// END Misc