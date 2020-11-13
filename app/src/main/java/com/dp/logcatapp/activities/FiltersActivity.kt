package com.dp.logcatapp.activities

import android.os.Bundle
import androidx.fragment.app.commit
import com.dp.logcat.Log
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.filters.FiltersFragment

class FiltersActivity : BaseActivityWithToolbar() {

  companion object {
    val TAG = FiltersActivity::class.qualifiedName
    val EXTRA_EXCLUSIONS = TAG + "_exclusions"
    val KEY_LOG = TAG + "_key_log"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_filters)
    setupToolbar()
    enableDisplayHomeAsUp()

    if (savedInstanceState == null) {
      supportFragmentManager.commit {
        replace(
          R.id.content_frame, FiltersFragment.newInstance(getLog(), isExclusions()),
          FiltersFragment.TAG
        )
      }
    }
  }

  private fun isExclusions() = intent != null && intent.getBooleanExtra(EXTRA_EXCLUSIONS, false)

  private fun getLog() = intent.getParcelableExtra<Log>(KEY_LOG)

  override fun getToolbarIdRes() = R.id.toolbar

  override fun getToolbarTitle(): String = if (isExclusions()) {
    getString(R.string.exclusions)
  } else {
    getString(R.string.filters)
  }
}