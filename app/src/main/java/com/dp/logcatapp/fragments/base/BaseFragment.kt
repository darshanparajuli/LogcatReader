package com.dp.logcatapp.fragments.base

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dp.logcatapp.util.mainHandler

open class BaseFragment : Fragment() {

  protected val scope
    get() = viewLifecycleOwner.lifecycleScope

  protected lateinit var handler: Handler
    private set

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handler = mainHandler()
  }

  override fun onDestroy() {
    super.onDestroy()
    handler.removeCallbacksAndMessages(null)
  }

  protected fun runOnUIThread(runnable: () -> Unit) {
    if (Thread.currentThread() == Looper.getMainLooper().thread) {
      runnable()
    } else {
      handler.post(runnable)
    }
  }
}
