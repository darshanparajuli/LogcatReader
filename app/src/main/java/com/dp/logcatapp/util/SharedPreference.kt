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
    getFactory = { sharedPrefs ->
      sharedPrefs.getString(key, default)
    },
    setFactory = { sharedPrefs, newValue ->
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
    getFactory = { sharedPrefs ->
      sharedPrefs.getInt(key, default)
    },
    setFactory = { sharedPrefs, newValue ->
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
    getFactory = { sharedPrefs ->
      sharedPrefs.getBoolean(key, default)
    },
    setFactory = { sharedPrefs, newValue ->
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
    getFactory = { sharedPrefs ->
      sharedPrefs.getStringSet(key, default)
    },
    setFactory = { sharedPrefs, newValue ->
      sharedPrefs.edit { putStringSet(key, newValue) }
    }
  )
}

@Composable
private fun <T> rememberSharedPreference(
  key: String,
  getFactory: (SharedPreferences) -> T,
  setFactory: (SharedPreferences, newValue: T) -> Unit,
): SharedPreference<T> {
  val context = LocalContext.current
  val sharedPreferences = remember(context) { context.getDefaultSharedPreferences() }
  var value by remember(sharedPreferences) {
    mutableStateOf(
      SharedPreference(
        currentValue = getFactory(sharedPreferences),
        setter = { newValue ->
          setFactory(sharedPreferences, newValue)
        },
        deleter = {
          sharedPreferences.edit { clear() }
        }
      )
    )
  }
  DisposableEffect(sharedPreferences) {
    val listener = OnSharedPreferenceChangeListener { prefs, k ->
      if (k == key) {
        value = value.copy(
          currentValue = getFactory(prefs)
        )
      }
    }
    sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    onDispose {
      sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
  }
  return value
}

data class SharedPreference<T>(
  private val currentValue: T,
  private val setter: (T) -> Unit,
  private val deleter: () -> Unit,
) {
  var value: T
    get() = currentValue
    set(newValue) {
      setter(newValue)
    }

  fun delete() {
    deleter()
  }
}
