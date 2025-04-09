package com.dp.logcatapp.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.dp.logcat.Log
import com.dp.logcatapp.ui.screens.FiltersScreen
import com.dp.logcatapp.ui.screens.PrepopulateFilterInfo
import com.dp.logcatapp.ui.theme.LogcatReaderTheme
import com.dp.logcatapp.util.getParcelableExtraSafe

class FiltersActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      LogcatReaderTheme {
        FiltersScreen(
          modifier = Modifier.fillMaxSize(),
          prepopulateFilterInfo = intent.getParcelableExtraSafe<Log>(EXTRA_LOG)?.let { log ->
            PrepopulateFilterInfo(
              log = log,
              packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME),
              exclude = intent.getBooleanExtra(EXTRA_EXCLUDE, false),
            )
          },
        )
      }
    }
  }

  companion object {
    const val EXTRA_LOG = "extra_log"
    const val EXTRA_PACKAGE_NAME = "extra_package_name"
    const val EXTRA_EXCLUDE = "extra_exclude"
  }
}
