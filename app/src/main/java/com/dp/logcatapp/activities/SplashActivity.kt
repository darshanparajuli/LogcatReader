package com.dp.logcatapp.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

open class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onBackPressed() {
        // prevent back press
    }
}