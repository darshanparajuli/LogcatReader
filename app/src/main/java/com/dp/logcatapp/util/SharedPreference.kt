package com.dp.logcatapp.util

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

@Composable
fun rememberStringSharedPreference(
  key: String,
  default: String? = null,
): SharedPreference<String?> {
  return rememberSharedPreference(
    key = key,
    getter = { sharedPrefs ->
      sharedPrefs.getString(key, default)
    },
    setter = { sharedPrefs, newValue ->
      sharedPrefs.edit { putString(key, newValue) }
    }
  )
}

@Composable
fun rememberIntSharedPreference(
  key: String,
  default: Int = -1,
): SharedPreference<Int> {
  return rememberSharedPreference(
    key = key,
    getter = { sharedPrefs ->
      sharedPrefs.getInt(key, default)
    },
    setter = { sharedPrefs, newValue ->
      sharedPrefs.edit { putInt(key, newValue) }
    }
  )
}

@Composable
fun rememberBooleanSharedPreference(
  key: String,
  default: Boolean = false,
): SharedPreference<Boolean> {
  return rememberSharedPreference(
    key = key,
    getter = { sharedPrefs ->
      sharedPrefs.getBoolean(key, default)
    },
    setter = { sharedPrefs, newValue ->
      sharedPrefs.edit { putBoolean(key, newValue) }
    }
  )
}

@Composable
fun rememberStringSetSharedPreference(
  key: String,
  default: Set<String>? = null,
): SharedPreference<Set<String>?> {
  return rememberSharedPreference(
    key = key,
    getter = { sharedPrefs ->
      sharedPrefs.getStringSet(key, default)
    },
    setter = { sharedPrefs, newValue ->
      sharedPrefs.edit { putStringSet(key, newValue) }
    }
  )
}

@Composable
private fun <T> rememberSharedPreference(
  key: String,
  getter: (SharedPreferences) -> T,
  setter: (SharedPreferences, newValue: T) -> Unit,
): SharedPreference<T> {
  val context = LocalContext.current
  val sharedPreferences = remember(context) { context.getDefaultSharedPreferences() }
  var currentValue by remember(sharedPreferences) {
    mutableStateOf(getter(sharedPreferences))
  }
  DisposableEffect(sharedPreferences, key) {
    val listener = OnSharedPreferenceChangeListener { prefs, k ->
      if (k == key) {
        currentValue = getter(prefs)
      }
    }
    sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    onDispose {
      sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
  }
  return SharedPreference(
    getter = { currentValue },
    setter = { newValue ->
      setter(sharedPreferences, newValue)
    },
    deleter = {
      sharedPreferences.edit { remove(key) }
    }
  )
}

data class SharedPreference<T>(
  private val getter: () -> T,
  private val setter: (T) -> Unit,
  private val deleter: () -> Unit,
) {
  var value: T
    get() = getter()
    set(newValue) {
      setter(newValue)
    }

  fun delete() {
    deleter()
  }
}
