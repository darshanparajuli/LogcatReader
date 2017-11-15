package com.dp.logcatapp.activities

import android.os.Bundle
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.settings.SettingsFragment

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setToolbar(R.id.toolbar, R.string.settings)
        enableDisplayHomeAsUp()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.content_frame, SettingsFragment(), SettingsFragment.TAG)
                    .commit()
        }
    }

    companion object {
        val TAG = SettingsActivity::class.qualifiedName
    }

}