package com.dp.logcatapp.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dp.logcatapp.Features

open class SplashActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (Features.useComposeForUi) {
      startActivity(Intent(this, ComposeMainActivity::class.java))
    } else {
      startActivity(Intent(this, MainActivity::class.java))
    }
    finish()
  }

  @SuppressLint("MissingSuperCall")
  override fun onBackPressed() {
    // prevent back press
  }
}