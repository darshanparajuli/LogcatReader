package com.dp.logcatapp.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.dp.logcatapp.services.LogcatService
import com.dp.logcatapp.ui.screens.HomeScreen
import com.dp.logcatapp.ui.theme.LogcatReaderTheme

class ComposeMainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()

    // Start service.
    val logcatServiceIntent = Intent(this, LogcatService::class.java)
    if (Build.VERSION.SDK_INT >= 26) {
      startForegroundService(logcatServiceIntent)
    } else {
      startService(logcatServiceIntent)
    }

    setContent {
      LogcatReaderTheme {
        Surface {
          HomeScreen(
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }
  }
}
