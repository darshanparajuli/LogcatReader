package com.dp.logcatapp.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.dp.logcat.Log
import com.dp.logcatapp.ui.screens.FiltersScreen
import com.dp.logcatapp.ui.theme.LogcatReaderTheme
import com.dp.logcatapp.util.getParcelableExtraSafe

class ComposeFiltersActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      LogcatReaderTheme {
        FiltersScreen(
          modifier = Modifier.fillMaxSize(),
          filterLog = intent.getParcelableExtraSafe<Log>(EXTRA_LOG),
        )
      }
    }
  }

  companion object {
    const val EXTRA_LOG = "key_log"
  }
}
