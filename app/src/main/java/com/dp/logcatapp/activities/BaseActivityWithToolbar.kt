package com.dp.logcatapp.activities

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import androidx.core.view.ViewCompat
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
    if (Build.VERSION.SDK_INT >= 21) {
      window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }
    super.onCreate(savedInstanceState)
  }

  protected fun setupToolbar() {
    toolbar = findViewById(getToolbarIdRes())
    if (Build.VERSION.SDK_INT == 19) {
      setAppBarPaddingForKitkat(toolbar.parent as ViewGroup)
    } else if (Build.VERSION.SDK_INT >= 21) {
      ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
        val viewGroup = view.parent as ViewGroup
        viewGroup.setPadding(0, insets.systemWindowInsetTop, 0, 0)
        insets.consumeSystemWindowInsets()
      }
    }
    setSupportActionBar(toolbar)
    supportActionBar?.title = getToolbarTitle()
  }

  @Suppress("DEPRECATION")
  private fun setAppBarPaddingForKitkat(viewGroup: ViewGroup) {
    val dm = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(dm)
    val topPadding = (dm.scaledDensity * 25).toInt()
    viewGroup.setPadding(0, topPadding, 0, 0)
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