package com.dp.logcatapp.services

import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import com.dp.logcatapp.util.getDefaultSharedPreferences
import com.dp.logcatapp.util.setTheme

abstract class BaseService : LifecycleService(),
  SharedPreferences.OnSharedPreferenceChangeListener {
  private val localBinder = LocalBinder()

  override fun onCreate() {
    setTheme()
    super.onCreate()
    onPreRegisterSharedPreferenceChangeListener()
    getDefaultSharedPreferences().registerOnSharedPreferenceChangeListener(this)
  }

  protected open fun onPreRegisterSharedPreferenceChangeListener() {
  }

  override fun onDestroy() {
    getDefaultSharedPreferences().unregisterOnSharedPreferenceChangeListener(this)
    super.onDestroy()
  }

  override fun onBind(intent: Intent): IBinder? {
    super.onBind(intent)
    return localBinder
  }

  override fun onSharedPreferenceChanged(
    sharedPreferences: SharedPreferences,
    key: String
  ) {
  }

  inner class LocalBinder : Binder() {
    @Suppress("UNCHECKED_CAST")
    fun <T : BaseService> getService() = this@BaseService as T
  }
}

inline fun <reified T : BaseService> IBinder.getService() =
  (this as BaseService.LocalBinder).getService<T>()