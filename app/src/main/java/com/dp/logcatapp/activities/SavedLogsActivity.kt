package com.dp.logcatapp.activities

import android.os.Bundle
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.savedlogs.SavedLogsFragment

class SavedLogsActivity : BaseActivityWithToolbar() {

    companion object {
        val TAG = SavedLogsActivity::class.qualifiedName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_logs)
        setupToolbar()
        enableDisplayHomeAsUp()

        if (savedInstanceState == null) {
            addFragment()
        }
    }

    private fun addFragment() {
        supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, SavedLogsFragment(), SavedLogsFragment.TAG)
                .commit()
    }

    override fun getToolbarIdRes(): Int = R.id.toolbar

    override fun getToolbarTitle(): String = getString(R.string.saved_logs)
}