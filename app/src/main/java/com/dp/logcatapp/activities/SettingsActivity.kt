package com.dp.logcatapp.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.dp.logcatapp.ui.screens.SettingsScreen
import com.dp.logcatapp.ui.theme.LogcatReaderTheme

class SettingsActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      LogcatReaderTheme {
        SettingsScreen(modifier = Modifier.fillMaxSize())
      }
    }
  }
}
