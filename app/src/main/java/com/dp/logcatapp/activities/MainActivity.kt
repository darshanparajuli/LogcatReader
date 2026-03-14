package com.dp.logcatapp.activities

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.dp.logcatapp.services.LogcatService
import com.dp.logcatapp.ui.screens.DeviceLogsScreen
import com.dp.logcatapp.ui.theme.LogcatReaderTheme
import com.dp.logcatapp.util.SettingsPrefKeys
import com.dp.logcatapp.util.getDefaultSharedPreferences
import com.dp.logcatapp.util.setKeepScreenOn
import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class MainActivity : BaseActivity() {

  private var stopRecordingSignal = Channel<Unit>(
    capacity = 1,
    onBufferOverflow = DROP_OLDEST,
  )
  private var isRecording = false

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()

    super.onCreate(savedInstanceState)

    if (handleExitNotificationAction(intent)) {
      return
    }

    if (intent.shouldStopRecording()) {
      stopRecordingSignal.trySend(Unit)
    }

    serviceStarterRefHashcode = System.identityHashCode(this)
    if (Build.VERSION.SDK_INT >= 33) {
      registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
          // TODO: show help text regarding why we need notifications permission.
        }
        LogcatService.start(this@MainActivity)
      }.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      LogcatService.start(this@MainActivity)
    }

    setContent {
      val stopRecordingSignalFlow = remember(stopRecordingSignal) {
        stopRecordingSignal.receiveAsFlow()
      }
      LogcatReaderTheme {
        DeviceLogsScreen(
          modifier = Modifier.fillMaxSize(),
          onRecordingStatusChanged = { isRecording = it },
          stopRecordingSignal = stopRecordingSignalFlow,
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    setKeepScreenOn(
      getDefaultSharedPreferences().getBoolean(
        SettingsPrefKeys.General.KEY_KEEP_SCREEN_ON,
        SettingsPrefKeys.General.Default.KEY_KEEP_SCREEN_ON
      )
    )
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    if (handleExitNotificationAction(intent)) {
      return
    }
    handleStopRecordingIntent(intent)
  }

  private fun Intent?.shouldStopRecording(): Boolean {
    return this?.getBooleanExtra(STOP_RECORDING_EXTRA, false) == true
  }

  private fun Intent?.shouldExit(): Boolean {
    return this?.getBooleanExtra(EXIT_EXTRA, false) == true
  }

  private fun handleStopRecordingIntent(intent: Intent?) {
    if (intent.shouldStopRecording()) {
      stopRecordingSignal.trySend(Unit)
    }
  }

  private fun handleExitNotificationAction(intent: Intent?): Boolean =
    if (intent.shouldExit()) {
      // Stop logcat service first.
      LogcatService.stop(this)
      ActivityCompat.finishAfterTransition(this)
      true
    } else {
      false
    }

  override fun onDestroy() {
    super.onDestroy()
    // Do not stop the service if recording is active!
    if (!isRecording) {
      // In cases where the app is closed and opened really quickly, `onDestroy` gets called on the
      // finishing activity _after_ current activity's `onCreate` function, which leads to
      // LogcatService ultimately getting stopped. To work around this, we check to see if the
      // destroying activity is the same as the one that started the service, and stop the service
      // accordingly.
      if (serviceStarterRefHashcode == System.identityHashCode(this)) {
        LogcatService.stop(this)
      }
    }
  }

  companion object {
    const val EXIT_EXTRA = "exit_extra"
    const val STOP_RECORDING_EXTRA = "stop_recording_extra"

    private var serviceStarterRefHashcode = -1
  }
}
