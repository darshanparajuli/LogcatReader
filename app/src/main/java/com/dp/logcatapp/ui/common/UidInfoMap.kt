package com.dp.logcatapp.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.dp.logcatapp.util.AppInfo
import com.dp.logcatapp.util.getAppInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun rememberAppInfoByUidMap(
  pollIntervalMs: Long? = null,
): Map<String, AppInfo> {
  val context = LocalContext.current
  var uidMap by remember(context) {
    mutableStateOf<Map<String, AppInfo>>(context.getAppInfo())
  }
  if (pollIntervalMs != null) {
    LaunchedEffect(context, pollIntervalMs) {
      while (isActive) {
        delay(pollIntervalMs)
        uidMap = context.getAppInfo()
      }
    }
  }
  return uidMap
}
