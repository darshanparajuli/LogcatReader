package com.dp.logcatapp.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.dp.logcatapp.ui.screens.SavedLogsViewerScreen
import com.dp.logcatapp.ui.theme.LogcatReaderTheme

class SavedLogsViewerActivity : BaseActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      LogcatReaderTheme {
        SavedLogsViewerScreen(
          modifier = Modifier.fillMaxSize(),
          uri = intent.data,
        )
      }
    }
  }
}
