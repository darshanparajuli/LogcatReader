package com.dp.logcatapp.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.content.FileProvider
import androidx.core.net.toFile
import com.dp.logcatapp.BuildConfig
import com.dp.logcatapp.R

object ShareUtils {
    fun shareSavedLogs(context: Context, uri: Uri, isCustom: Boolean) {
        val intent = Intent(Intent.ACTION_SEND)

        val shareUri = if (isCustom) {
            uri
        } else {
            FileProvider.getUriForFile(context,
                    "${context.packageName}.${BuildConfig.FILE_PROVIDER}", uri.toFile())
        }

        intent.setDataAndType(shareUri, "text/plain")
        intent.putExtra(Intent.EXTRA_STREAM, shareUri)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
    }
}