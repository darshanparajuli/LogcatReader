package com.dp.logcatapp.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.dp.logcatapp.ui.screens.SavedLogsScreen
import com.dp.logcatapp.ui.theme.LogcatReaderTheme

class SavedLogsActivity : BaseActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      LogcatReaderTheme {
        SavedLogsScreen(modifier = Modifier.fillMaxSize())
      }
    }
  }
}
