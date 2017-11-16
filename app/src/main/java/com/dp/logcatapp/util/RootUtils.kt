package com.dp.logcatapp.util

import android.Manifest
import android.content.Context
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

    fun grantReadLogsPermission(context: Context) = runCmdAsRoot("pm", "grant",
            context.packageName, Manifest.permission.READ_LOGS)

}
