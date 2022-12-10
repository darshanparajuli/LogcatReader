package com.dp.logcatapp.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.TaskStackBuilder
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.os.Parcelable
import android.provider.OpenableColumns
import android.util.TypedValue
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.dp.logcatapp.R
import com.dp.logcatapp.activities.MainActivity
import com.dp.logcatapp.activities.SettingsActivity
import com.dp.logger.Logger
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Calendar
import java.util.Locale

//// BEGIN Activity

fun Activity.restartApp() {
  val taskBuilder = TaskStackBuilder.create(this)
    .addNextIntent(Intent(this, MainActivity::class.java))
    .addNextIntent(Intent(this, SettingsActivity::class.java))
  finish()
  overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
  taskBuilder.startActivities()
}

fun Activity.setKeepScreenOn(enabled: Boolean) {
  if (enabled) {
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  } else {
    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  }
}

//// END Activity

//// BEGIN Snackbar

fun showSnackbar(
  view: View?,
  msg: String,
  length: Int = Snackbar.LENGTH_SHORT
) {
  newSnakcbar(view, msg, length)?.show()
}

fun newSnakcbar(
  view: View?,
  msg: String,
  length: Int = Snackbar.LENGTH_SHORT
): Snackbar? {
  if (view != null) {
    return Snackbar.make(view, msg, length)
  }
  return null
}

//// END Snackbar

//// BEGIN Fragment

fun Fragment.inflateLayout(
  @LayoutRes layoutResId: Int,
  root: ViewGroup? = null,
  attachToRoot: Boolean = false
): View =
  activity!!.inflateLayout(layoutResId, root, attachToRoot)

//// END Fragment

//// BEGIN Context

private val typefaceCache = mutableMapOf<String, Typeface>()

// Bug find/workaround credit: https://github.com/drakeet/ToastCompat#why
fun Context.showToast(
  msg: CharSequence,
  length: Int = Toast.LENGTH_SHORT
) {
  val toast = Toast.makeText(this, msg, length)
  if (SDK_INT <= 25) {
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
  override fun getSystemService(name: String): Any {
    return if (name == Context.WINDOW_SERVICE) {
      ToastWindowManager(baseContext, baseContext.getSystemService(name) as WindowManager)
    } else {
      super.getSystemService(name)
    }
  }
}

private class ToastWindowManager(
  private val context: Context,
  private val base: WindowManager
) : WindowManager {
  @Suppress("DEPRECATION")
  override fun getDefaultDisplay(): Display =
    if (SDK_INT >= 30) context.display!! else base.defaultDisplay

  override fun addView(
    view: View?,
    params: ViewGroup.LayoutParams?
  ) {
    try {
      base.addView(view, params)
    } catch (e: WindowManager.BadTokenException) {
      Logger.error("Toast", "caught BadTokenException crash")
    }
  }

  override fun updateViewLayout(
    view: View?,
    params: ViewGroup.LayoutParams?
  ) =
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

fun Context.getAttributeDrawable(@AttrRes attrId: Int): Drawable? {
  val tv = TypedValue()
  theme.resolveAttribute(attrId, tv, true)
  return ContextCompat.getDrawable(this, tv.resourceId)
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
  val useBlackTheme = getDefaultSharedPreferences().getBoolean(
    PreferenceKeys
      .Appearance.KEY_USE_BLACK_THEME, PreferenceKeys.Appearance.Default.USE_BLACK_THEME
  )
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
  val theme = prefs.getString(
    PreferenceKeys.Appearance.KEY_THEME,
    PreferenceKeys.Appearance.Default.THEME
  )
  when (theme) {
    PreferenceKeys.Appearance.Theme.AUTO -> setThemeAuto()
    PreferenceKeys.Appearance.Theme.DARK -> setThemeDark()
    PreferenceKeys.Appearance.Theme.LIGHT -> setThemeLight()
  }
}

fun Context.getFileNameFromUri(uri: Uri): String {
  var name: String? = null
  if (uri.scheme == "content") {
    val cursor = contentResolver.query(uri, null, null, null, null)
    name = cursor?.use {
      if (it.moveToFirst()) {
        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index != -1) {
          it.getString(index)
        } else {
          null
        }
      } else {
        null
      }
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

fun Context.inflateLayout(
  @LayoutRes layoutResId: Int,
  root: ViewGroup? = null,
  attachToRoot: Boolean = false
): View =
  LayoutInflater.from(this).inflate(layoutResId, root, attachToRoot)

//// END Context

//// BEGIN String

@SuppressLint("DefaultLocale")
fun String.containsIgnoreCase(other: String) =
  lowercase(Locale.getDefault()).contains(other.lowercase(Locale.getDefault()))

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

inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? {
  if (SDK_INT >= TIRAMISU) {
    return getParcelable(key, T::class.java)
  } else {
    @Suppress("DEPRECATION")
    return getParcelable(key)
  }
}

//// END Misc