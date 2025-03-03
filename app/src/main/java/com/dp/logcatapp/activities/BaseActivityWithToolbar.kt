package com.dp.logcatapp.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.displayCutout
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.updateLayoutParams
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
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
  }

  protected fun setupToolbar() {
    toolbar = findViewById(getToolbarIdRes())
    ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, windowInsets ->
      val insets = windowInsets.getInsets(systemBars() or displayCutout())
      v.updateLayoutParams<MarginLayoutParams> {
        topMargin = insets.top
        leftMargin = insets.left
        rightMargin = insets.right
      }
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
        if (shouldUpRecreateTask(upIntent)) {
          TaskStackBuilder.create(this)
            .addNextIntentWithParentStack(upIntent)
            .startActivities()
        } else {
          navigateUpTo(upIntent)
        }
      }
      true
    }
    else -> super.onOptionsItemSelected(item)
  }
}