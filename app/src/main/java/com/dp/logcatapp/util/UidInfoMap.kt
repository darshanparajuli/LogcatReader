package com.dp.logcatapp.util

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext

private object PackageChangeNotifier {
  private val _channel = Channel<Unit>(
    capacity = 1,
    onBufferOverflow = DROP_OLDEST,
  )
  val packageChanged: Flow<Unit> = _channel.receiveAsFlow()

  fun notifyPackageChanged() {
    _channel.trySend(Unit)
  }
}

@Composable
fun rememberAppInfoByUidMap(): Map<String, AppInfo> {
  val context = LocalContext.current
  var uidMap by remember(context) {
    mutableStateOf<Map<String, AppInfo>>(context.getAppInfo())
  }

  LaunchedEffect(context) {
    PackageChangeNotifier.packageChanged.collect {
      uidMap = withContext(Dispatchers.IO) { context.getAppInfo() }
    }
  }

  return uidMap
}

class PackageChangedBroadcastListener : BroadcastReceiver() {
  @SuppressLint("UnsafeProtectedBroadcastReceiver")
  override fun onReceive(
    context: Context,
    intent: Intent?,
  ) {
    PackageChangeNotifier.notifyPackageChanged()
  }
}
