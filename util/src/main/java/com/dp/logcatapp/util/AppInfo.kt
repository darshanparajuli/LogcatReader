package com.dp.logcatapp.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager.GET_META_DATA
import android.graphics.drawable.Drawable
import androidx.annotation.WorkerThread

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
