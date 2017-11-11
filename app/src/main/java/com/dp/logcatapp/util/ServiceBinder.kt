package com.dp.logcatapp.util

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import java.io.Closeable

class ServiceBinder(private val mClass: Class<*>,
                    private var mServiceConnection: ServiceConnection?) : Closeable {
    var isBound: Boolean = false
        private set

    fun bind(context: Context) {
        if (mServiceConnection == null) {
            throw IllegalStateException("This ServiceBinder has already been closed.")
        }

        context.bindService(Intent(context, mClass), mServiceConnection, Context.BIND_ABOVE_CLIENT)
        isBound = true
    }

    fun unbind(context: Context) {
        if (isBound) {
            if (mServiceConnection != null) {
                context.unbindService(mServiceConnection!!)
            }
            isBound = false
        }
    }

    override fun close() {
        mServiceConnection = null
    }
}