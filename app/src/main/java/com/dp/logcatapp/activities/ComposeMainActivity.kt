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
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import com.dp.logcatapp.activities.MainActivity.Companion.EXIT_EXTRA
import com.dp.logcatapp.activities.MainActivity.Companion.STOP_RECORDING_EXTRA
import com.dp.logcatapp.services.LogcatService
import com.dp.logcatapp.ui.screens.HomeScreen
import com.dp.logcatapp.ui.theme.LogcatReaderTheme
import com.dp.logcatapp.util.PreferenceKeys
import com.dp.logcatapp.util.getDefaultSharedPreferences
import com.dp.logcatapp.util.setKeepScreenOn

class ComposeMainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    if (checkShouldTheAppExit(intent)) {
      return
    }

    // TODO(darshan): Handle back press properly.
    // Maybe don't stop the service if recording is active?

    val stopRecording = intent?.shouldStopRecording()
    // TODO(darshan): handle stop recording

    if (Build.VERSION.SDK_INT >= 33) {
      registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
          // TODO(darshan): show help text regarding why we need notifications permission.
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
      LogcatReaderTheme {
        HomeScreen(
          modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    setKeepScreenOn(
      getDefaultSharedPreferences().getBoolean(
        PreferenceKeys.General.KEY_KEEP_SCREEN_ON,
        PreferenceKeys.General.Default.KEY_KEEP_SCREEN_ON
      )
    )
  }

  override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
    super.onNewIntent(intent)
    checkShouldTheAppExit(intent)
    handleStopRecordingIntent(intent)
  }

  private fun Intent?.shouldStopRecording(): Boolean {
    return this?.getBooleanExtra(STOP_RECORDING_EXTRA, false) == true
  }

  private fun handleStopRecordingIntent(intent: Intent?) {
    if (intent.shouldStopRecording()) {
      // TODO(darshan): stop recording
    }
  }

  private fun checkShouldTheAppExit(intent: Intent?): Boolean =
    if (intent?.getBooleanExtra(EXIT_EXTRA, false) == true) {
      ActivityCompat.finishAfterTransition(this)
      true
    } else {
      false
    }

  override fun onDestroy() {
    super.onDestroy()
    stopService(Intent(this, LogcatService::class.java))
  }
}
