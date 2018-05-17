package com.dp.logcatapp.activities

import android.os.Bundle
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.filters.FiltersFragment
import com.dp.logcatapp.util.transaction

class FiltersActivity : BaseActivityWithToolbar() {

    companion object {
        val TAG = FiltersActivity::class.qualifiedName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filters)
        setupToolbar()
        enableDisplayHomeAsUp()

        if (savedInstanceState == null) {
            supportFragmentManager.transaction {
                replace(R.id.content_frame, FiltersFragment(), FiltersFragment.TAG)
            }
        }
    }

    override fun getToolbarIdRes() = R.id.toolbar

    override fun getToolbarTitle(): String = getString(R.string.filters)
}