package com.dp.logcatapp.util

import android.Manifest
import android.content.Context
import com.dp.logcatapp.BuildConfig
import java.io.IOException

object RootUtils {

    const val GRANT_PERMISSION_CMD = "adb shell pm grant ${BuildConfig.APPLICATION_ID} " +
            Manifest.permission.READ_LOGS

    fun runCmdAsRoot(vararg cmd: String): Boolean {
        val processBuilder = ProcessBuilder("su", "-c", *cmd)
        var process: Process? = null
        return try {
            process = processBuilder.start()
            process.waitFor() != 0
        } catch (e: IOException) {
            false
        } finally {
            process?.destroy()
        }
    }

    fun grantReadLogsPermission(context: Context): Boolean {
        return if (runCmdAsRoot("pm", "grant", context.packageName,
                Manifest.permission.READ_LOGS)) {
            android.os.Process.killProcess(android.os.Process.myPid())
            true
        } else {
            false
        }
    }
}
