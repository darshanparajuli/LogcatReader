package com.dp.logcatapp.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.preference.PreferenceManager
import com.dp.logcatapp.R
import com.dp.logcatapp.util.mainHandler
import com.dp.logcatapp.util.setTheme

@SuppressLint("Registered")
abstract class BaseActivityWithToolbar : AppCompatActivity() {

  companion object {
    init {
      AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }
  }

  lateinit var toolbar: Toolbar
    private set
  protected val handler = mainHandler()

  override fun onCreate(savedInstanceState: Bundle?) {
    PreferenceManager.setDefaultValues(this, R.xml.settings, false)
    setTheme()
    WindowCompat.setDecorFitsSystemWindows(window, false)
    super.onCreate(savedInstanceState)
  }

  protected fun setupToolbar() {
    toolbar = findViewById(getToolbarIdRes())
    ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
      val insetTypes = Type.displayCutout() or Type.systemBars()
      insets.getInsets(insetTypes)
      WindowInsetsCompat.CONSUMED
    }
    setSupportActionBar(toolbar)
    supportActionBar?.title = getToolbarTitle()
  }

  protected abstract fun getToolbarIdRes(): Int

  protected abstract fun getToolbarTitle(): String

  protected fun enableDisplayHomeAsUp() {
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
      android.R.id.home -> {
          val upIntent = NavUtils.getParentActivityIntent(this)
          if (upIntent != null) {
              if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                  TaskStackBuilder.create(this)
                    .addNextIntentWithParentStack(upIntent)
                    .startActivities()
              } else {
                  NavUtils.navigateUpTo(this, upIntent)
              }
          }
          true
      }
    else -> super.onOptionsItemSelected(item)
  }
}