package com.dp.logcatapp.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration

@Composable
fun rememberAppInfoByUidMap(
  refreshInterval: Duration? = null,
): Map<String, AppInfo> {
  val context = LocalContext.current
  var uidMap by remember(context) {
    mutableStateOf<Map<String, AppInfo>>(context.getAppInfo())
  }
  if (refreshInterval != null) {
    LaunchedEffect(context, refreshInterval) {
      while (isActive) {
        delay(refreshInterval)
        uidMap = context.getAppInfo()
      }
    }
  }
  return uidMap
}
