package com.dp.logcatapp.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.savedlogs.SavedLogsFragment

class SavedLogsActivity : BaseActivityWithToolbar() {

    companion object {
        val TAG = SavedLogsActivity::class.qualifiedName
        private const val REQ_READ_STORAGE_PERMISSION = 234
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_logs)
        setupToolbar()
        enableDisplayHomeAsUp()

        if (savedInstanceState == null) {
            if (isReadExtStoragePermissionGranted()) {
                addFragment()
            } else {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        REQ_READ_STORAGE_PERMISSION)
            }
        }
    }

    private fun isReadExtStoragePermissionGranted() =
            ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun addFragment() {
        supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, SavedLogsFragment(), SavedLogsFragment.TAG)
                .commit()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQ_READ_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    addFragment()
                } else {
                    finish()
                }
            }
        }
    }

    override fun getToolbarIdRes(): Int = R.id.toolbar

    override fun getToolbarTitle(): String = getString(R.string.saved_logs)
}