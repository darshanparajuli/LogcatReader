package com.dp.logcatapp.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.logcatlive.LogcatLiveFragment
import com.dp.logcatapp.services.LogcatService
import com.dp.logcatapp.util.PreferenceKeys
import com.dp.logcatapp.util.getDefaultSharedPreferences
import com.dp.logcatapp.util.setKeepScreenOn
import com.dp.logcatapp.util.showToast

class MainActivity : BaseActivityWithToolbar() {
  private var canExit = false

  private val exitRunnable = Runnable {
    canExit = false
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setupToolbar()

    if (checkShouldTheAppExit(intent)) {
      return
    }

    val logcatServiceIntent = Intent(this, LogcatService::class.java)
    if (Build.VERSION.SDK_INT >= 26) {
      startForegroundService(logcatServiceIntent)
    } else {
      startService(logcatServiceIntent)
    }

    if (savedInstanceState == null) {
      val stopRecording = intent?.getBooleanExtra(
        STOP_RECORDING_EXTRA,
        false
      ) == true
      supportFragmentManager.beginTransaction()
        .replace(
          R.id.content_frame, LogcatLiveFragment.newInstance(stopRecording),
          LogcatLiveFragment.TAG
        )
        .commit()
    }
  }

  override fun getToolbarIdRes(): Int = R.id.toolbar

  override fun getToolbarTitle(): String = getString(R.string.device_logs)

  private fun checkShouldTheAppExit(intent: Intent?): Boolean =
    if (intent?.getBooleanExtra(EXIT_EXTRA, false) == true) {
      canExit = true
      ActivityCompat.finishAfterTransition(this)
      true
    } else {
      false
    }

  private fun handleStopRecordingIntent(intent: Intent?) {
    if (intent?.getBooleanExtra(STOP_RECORDING_EXTRA, false) == true) {
      val fragment = supportFragmentManager.findFragmentByTag(LogcatLiveFragment.TAG)
      if (fragment != null) {
        (fragment as LogcatLiveFragment).tryStopRecording()
      }
    }
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    checkShouldTheAppExit(intent)
    handleStopRecordingIntent(intent)
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

  override fun onDestroy() {
    super.onDestroy()
    if (canExit) {
      stopService(Intent(this, LogcatService::class.java))
    }
  }

  override fun onBackPressed() {
    if (canExit) {
      handler.removeCallbacks(exitRunnable)
      super.onBackPressed()
    } else {
      canExit = true
      showToast(getString(R.string.press_back_again_to_exit))
      handler.postDelayed(exitRunnable, EXIT_DOUBLE_PRESS_DELAY)
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.main, menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
    R.id.action_settings -> {
      startActivity(Intent(this, SettingsActivity::class.java))
      true
    }
    else -> super.onOptionsItemSelected(item)
  }

  companion object {
    val TAG = MainActivity::class.qualifiedName
    val EXIT_EXTRA = TAG + "_extra_exit"
    val STOP_RECORDING_EXTRA = TAG + "_stop_recording_extra"
    private const val EXIT_DOUBLE_PRESS_DELAY: Long = 2000
  }
}
