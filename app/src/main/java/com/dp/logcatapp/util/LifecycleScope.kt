package com.dp.logcatapp.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

class LifecycleScope : DefaultLifecycleObserver, CoroutineScope {
    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = job + Main

    override fun onDestroy(owner: LifecycleOwner) {
        job.cancel()
    }
}