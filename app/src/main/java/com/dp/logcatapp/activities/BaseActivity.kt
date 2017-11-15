package com.dp.logcatapp.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.annotation.IdRes
import android.support.annotation.StringRes
import android.support.v4.app.NavUtils
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.preference.PreferenceManager
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import com.dp.logcatapp.R
import com.dp.logcatapp.util.isDarkThemeOn
import com.dp.logcatapp.util.setTheme

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity() {

    companion object {
        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        }
    }

    protected var toolbar: Toolbar? = null
        private set
    protected val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        PreferenceManager.setDefaultValues(this, R.xml.settings, false)
        setTheme()
        super.onCreate(savedInstanceState)
    }

    protected fun setToolbar(@IdRes id: Int, @StringRes title: Int) {
        toolbar = findViewById(id)
        if (isDarkThemeOn()) {
            toolbar?.popupTheme = R.style.DarkToolbarPopupTheme
        } else {
            toolbar?.popupTheme = R.style.LightToolbarPopupTheme
        }
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle(title)
    }

    protected fun enableDisplayHomeAsUp() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            val upIntent = NavUtils.getParentActivityIntent(this)
            upIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(upIntent)
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}