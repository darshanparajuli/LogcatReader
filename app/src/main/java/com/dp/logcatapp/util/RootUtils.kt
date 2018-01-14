package com.dp.logcatapp.util

import android.Manifest
import android.content.Context
import com.dp.jshellsession.Config
import com.dp.jshellsession.JShellSession
import java.io.IOException

object RootUtils {

    fun grantReadLogsPermission(context: Context): Boolean {
        var jshellSession: JShellSession? = null
        try {
            val cmd = "pm grant ${context.packageName} ${Manifest.permission.READ_LOGS}"
            val config = Config.Builder()
                    .setShellCommand("su")
                    .build()
            jshellSession = JShellSession(config)
            return jshellSession.run(cmd).exitSuccess()
        } catch (e: IOException) {
            return false
        } finally {
            jshellSession?.close()
        }
    }
}
