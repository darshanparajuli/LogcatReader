package com.dp.logcatapp.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.dp.logcatapp.ui.screens.SavedLogsViewerScreen
import com.dp.logcatapp.ui.theme.LogcatReaderTheme

class SavedLogsViewerActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    if (intent.data == null) {
      finish()
      return
    }

    setContent {
      LogcatReaderTheme {
        SavedLogsViewerScreen(
          modifier = Modifier.fillMaxSize(),
          uri = requireNotNull(intent.data),
        )
      }
    }
  }
}
