package com.dp.logcatapp.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.logcat_live.LogcatLiveFragment
import com.dp.logcatapp.services.logcat.LogcatService

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setToolbar(R.id.toolbar, R.string.app_name)

        val logcatServiceIntent = Intent(this, LogcatService::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(logcatServiceIntent)
        } else {
            startService(logcatServiceIntent)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.content_frame, LogcatLiveFragment(), LogcatLiveFragment.TAG)
                    .commit()
        }
    }

    companion object {
        val TAG = MainActivity::class.qualifiedName
    }
}
