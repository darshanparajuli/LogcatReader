package com.dp.logcatapp.activities

import android.os.Bundle
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.settings.SettingsFragment

class SettingsActivity : BaseActivityWithToolbar() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_settings)
    setupToolbar()
    enableDisplayHomeAsUp()

    if (savedInstanceState == null) {
      supportFragmentManager.beginTransaction()
        .replace(R.id.content_frame, SettingsFragment(), SettingsFragment.TAG)
        .commit()
    }
  }

  override fun getToolbarIdRes(): Int = R.id.toolbar

  override fun getToolbarTitle(): String = getString(R.string.settings)

  companion object {
    val TAG = SettingsActivity::class.qualifiedName
  }
}