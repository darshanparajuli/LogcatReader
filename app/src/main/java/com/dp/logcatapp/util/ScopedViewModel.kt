package com.dp.logcatapp.util

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

open class ScopedViewModel : ViewModel(), CoroutineScope {
    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = job + Main

    override fun onCleared() {
        job.cancel()
    }
}

open class ScopedAndroidViewModel(application: Application) : AndroidViewModel(application), CoroutineScope {
    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = job + Main

    override fun onCleared() {
        job.cancel()
    }
}
