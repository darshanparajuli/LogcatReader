package com.dp.logcatapp.util

import android.Manifest
import com.dp.logcatapp.BuildConfig
import java.io.IOException

object RootUtils {

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

    fun grantReadLogsPermission() = runCmdAsRoot("pm", "grant", BuildConfig.APPLICATION_ID,
            Manifest.permission.READ_LOGS)

}
