package com.dp.logcatapp.util

import android.Manifest
import android.content.Context
import com.dp.jshellsession.Config
import com.dp.jshellsession.JShellSession

object RootUtils {

    fun grantReadLogsPermission(context: Context): Boolean {
        val cmd = "pm grant ${context.packageName} ${Manifest.permission.READ_LOGS}"
        try {
            val config = Config.Builder()
                    .setShellCommand("su")
                    .build()
            return JShellSession(config).use {
                it.run(cmd).exitSuccess()
            }
        } catch (e: Exception) {
            return false
        }
    }
}
