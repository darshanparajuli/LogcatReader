package com.dp.logcatapp.util

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

open class ScopedViewModel : ViewModel(), CoroutineScope {
  override val coroutineContext: CoroutineContext
    get() = viewModelScope.coroutineContext
}

open class ScopedAndroidViewModel(application: Application) : AndroidViewModel(application),
  CoroutineScope {

  override val coroutineContext: CoroutineContext
    get() = viewModelScope.coroutineContext
}
