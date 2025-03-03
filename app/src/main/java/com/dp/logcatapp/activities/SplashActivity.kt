package com.dp.logcatapp.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.dp.logcatapp.Features

open class SplashActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        // prevent back press
      }
    })

    if (Features.useComposeForUi) {
      startActivity(Intent(this, ComposeMainActivity::class.java))
    } else {
      startActivity(Intent(this, MainActivity::class.java))
    }
    finish()
  }
}