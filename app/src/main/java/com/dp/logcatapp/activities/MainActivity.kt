package com.dp.logcatapp.activities

import android.Manifest
import android.app.ComponentCaller
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

class MainActivity : ComponentActivity() {

  private var stopRecordingSignal = Channel<Unit>(
    capacity = 1,
    onBufferOverflow = DROP_OLDEST,
  )

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()

    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    if (handleExitNotificationAction(intent)) {
      return
    }

    if (intent.shouldStopRecording()) {
      stopRecordingSignal.trySend(Unit)
    }

    if (Build.VERSION.SDK_INT >= 33) {
      registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
          // TODO: show help text regarding why we need notifications permission.
        }
        val logcatServiceIntent = Intent(this, LogcatService::class.java)
        startForegroundService(logcatServiceIntent)
      }.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      val logcatServiceIntent = Intent(this, LogcatService::class.java)
      if (Build.VERSION.SDK_INT >= 26) {
        startForegroundService(logcatServiceIntent)
      } else {
        startService(logcatServiceIntent)
      }
    }

    setContent {
      val stopRecordingSignalFlow = remember(stopRecordingSignal) {
        stopRecordingSignal.receiveAsFlow()
      }
      LogcatReaderTheme {
        DeviceLogsScreen(
          modifier = Modifier.fillMaxSize(),
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

  override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
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
      stopService(Intent(this, LogcatService::class.java))
      ActivityCompat.finishAfterTransition(this)
      true
    } else {
      false
    }

  companion object {
    const val EXIT_EXTRA = "exit_extra"
    const val STOP_RECORDING_EXTRA = "stop_recording_extra"
  }
}
