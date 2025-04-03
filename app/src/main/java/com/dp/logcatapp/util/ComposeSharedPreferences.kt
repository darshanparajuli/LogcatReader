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
fun rememberStringSharedPreferencesValue(
  key: String,
  default: String? = null,
): Preference<String?> {
  return rememberSharedPreferencesValue(
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
fun rememberIntSharedPreferencesValue(
  key: String,
  default: Int = -1,
): Preference<Int> {
  return rememberSharedPreferencesValue(
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
fun rememberBooleanSharedPreferencesValue(
  key: String,
  default: Boolean = false,
): Preference<Boolean> {
  return rememberSharedPreferencesValue(
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
fun rememberStringSetSharedPreferencesValue(
  key: String,
  default: Set<String>? = null,
): Preference<Set<String>?> {
  return rememberSharedPreferencesValue(
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
private fun <T> rememberSharedPreferencesValue(
  key: String,
  getFactory: (SharedPreferences) -> T,
  setFactory: (SharedPreferences, newValue: T) -> Unit,
): Preference<T> {
  val context = LocalContext.current
  val sharedPreferences = remember(context) { context.getDefaultSharedPreferences() }
  var value by remember(sharedPreferences) {
    mutableStateOf(
      Preference(
        currentValue = getFactory(sharedPreferences),
        setter = { newValue ->
          setFactory(sharedPreferences, newValue)
        },
        remover = {
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

data class Preference<T>(
  private val currentValue: T,
  private val setter: (T) -> Unit,
  private val remover: () -> Unit,
) {
  var value: T
    get() = currentValue
    set(newValue) {
      setter(newValue)
    }

  fun delete() {
    remover()
  }
}
