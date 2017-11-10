package com.dp.logcatapp.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.dp.logcatapp.R

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setToolbar(R.id.toolbar, R.string.app_name)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.settings -> {
                startActivity(SettingsActivity.newIntent(this))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
