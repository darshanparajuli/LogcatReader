package com.dp.logcatapp.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_META_DATA
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.provider.OpenableColumns
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.preference.PreferenceManager
import com.dp.logcatapp.R
import com.dp.logger.Logger

fun Context.findActivity(): Activity? {
  return when (this) {
    is Activity -> this
    is ContextWrapper -> this.baseContext.findActivity()
    else -> null
  }
}

fun Context.getDefaultSharedPreferences(): SharedPreferences =
  PreferenceManager.getDefaultSharedPreferences(this)

// Bug find/workaround credit: https://github.com/drakeet/ToastCompat#why
fun Context.showToast(
  msg: CharSequence,
  length: Int = Toast.LENGTH_SHORT
) {
  val toast = Toast.makeText(this, msg, length)
  if (SDK_INT <= 25) {
    try {
      @SuppressLint("DiscouragedPrivateApi")
      val field = View::class.java.getDeclaredField("mContext")
      field.isAccessible = true
      @Suppress("DEPRECATION")
      field.set(toast.view, ToastViewContextWrapper(this))
    } catch (_: Exception) {
    }
  }
  toast.show()
}

private fun Context.setThemeAuto() {
  if (isSystemDarkThemeOn()) {
    setThemeDark()
  } else {
    setThemeLight()
  }
}

private fun Context.isSystemDarkThemeOn(): Boolean {
  return resources.configuration.uiMode and
    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

private fun Context.setThemeDark() {
  setTheme(R.style.DarkTheme)
}

private fun Context.setThemeLight() {
  setTheme(R.style.LightTheme)
}

fun Context.setTheme() {
  val prefs = getDefaultSharedPreferences()
  val theme = prefs.getString(
    SettingsPrefKeys.Appearance.KEY_THEME,
    SettingsPrefKeys.Appearance.Default.THEME
  )
  when (theme) {
    SettingsPrefKeys.Appearance.Theme.AUTO -> setThemeAuto()
    SettingsPrefKeys.Appearance.Theme.DARK -> setThemeDark()
    SettingsPrefKeys.Appearance.Theme.LIGHT -> setThemeLight()
  }
}

fun Context.getFileNameFromUri(uri: Uri): String? {
  return when (uri.scheme) {
    ContentResolver.SCHEME_CONTENT -> {
      try {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
          cursor.moveToFirst()
          cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            .takeIf { it != -1 }
            ?.let { index ->
              cursor.getString(index)
            }
        }
      } catch (_: Exception) {
        null
      }
    }
    ContentResolver.SCHEME_FILE -> {
      try {
        uri.toFile().name
      } catch (_: Exception) {
        null
      }
    }
    else -> null
  }
}

private class ToastViewContextWrapper(base: Context) : ContextWrapper(base) {
  override fun getApplicationContext(): Context =
    ToastViewApplicationContextWrapper(baseContext.applicationContext)
}

private class ToastViewApplicationContextWrapper(base: Context) : ContextWrapper(base) {
  override fun getSystemService(name: String): Any {
    return if (name == WINDOW_SERVICE) {
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
  @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
  override fun getDefaultDisplay(): Display =
    if (SDK_INT >= 30) context.display else base.defaultDisplay

  override fun addView(
    view: View?,
    params: ViewGroup.LayoutParams?
  ) {
    try {
      base.addView(view, params)
    } catch (_: WindowManager.BadTokenException) {
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

fun Context.isReadLogsPermissionGranted(): Boolean {
  return ContextCompat.checkSelfPermission(
    this,
    Manifest.permission.READ_LOGS
  ) == PackageManager.PERMISSION_GRANTED
}

data class AppInfo(
  val uid: String,
  val packageName: String,
  val name: String,
  // This is 0 if not available.
  val enabled: Boolean,
  val icon: Drawable,
  val isSystem: Boolean,
)

@WorkerThread
fun Context.getAppInfo(): Map<String, AppInfo> {
  val map = mutableMapOf<String, AppInfo>()
  packageManager.getInstalledApplications(GET_META_DATA).forEach { info ->
    map[info.uid.toString()] = AppInfo(
      uid = info.uid.toString(),
      packageName = info.packageName,
      name = info.loadLabel(packageManager).toString(),
      enabled = info.enabled,
      icon = info.loadIcon(packageManager),
      isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
    )
  }
  return map
}
