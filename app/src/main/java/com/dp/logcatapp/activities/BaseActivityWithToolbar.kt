package com.dp.logcatapp.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.AppBarLayout
import android.support.v4.app.NavUtils
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.preference.PreferenceManager
import android.support.v7.widget.Toolbar
import android.util.DisplayMetrics
import android.view.MenuItem
import com.dp.logcatapp.R
import com.dp.logcatapp.util.setTheme

@SuppressLint("Registered")
abstract class BaseActivityWithToolbar : AppCompatActivity() {

    companion object {
        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    protected lateinit var toolbar: Toolbar
        private set
    protected val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        PreferenceManager.setDefaultValues(this, R.xml.settings, false)
        setTheme()
        super.onCreate(savedInstanceState)
    }

    protected fun setupToolbar() {
        toolbar = findViewById(getToolbarIdRes())
        if (Build.VERSION.SDK_INT == 19) {
            setAppBarPaddingForKitkat(toolbar.parent as AppBarLayout)
        } else if (Build.VERSION.SDK_INT >= 21) {
            ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
                val appBarLayout = view.parent as AppBarLayout
                appBarLayout.setPadding(0, insets.systemWindowInsetTop, 0, 0)
                insets.consumeSystemWindowInsets()
            }
        }
        setSupportActionBar(toolbar)
        supportActionBar?.title = getToolbarTitle()
    }

    private fun setAppBarPaddingForKitkat(appBarLayout: AppBarLayout) {
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)
        val topPadding = (dm.scaledDensity * 25).toInt()
        appBarLayout.setPadding(0, topPadding, 0, 0)
    }

    protected abstract fun getToolbarIdRes(): Int

    protected abstract fun getToolbarTitle(): String

    protected fun enableDisplayHomeAsUp() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            val upIntent = NavUtils.getParentActivityIntent(this)
            upIntent?.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(upIntent)
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}