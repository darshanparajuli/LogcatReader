package com.dp.logcatapp.util

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import kotlinx.coroutines.flow.drop

@Composable
fun rememberStringSharedPreference(
  key: String,
  default: String? = null,
): MutableState<String?> {
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
): MutableState<Int> {
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
): MutableState<Boolean> {
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
): MutableState<Set<String>?> {
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
): MutableState<T> {
  val context = LocalContext.current
  val sharedPreferences = remember(context) { context.getDefaultSharedPreferences() }
  val currentValue = remember(sharedPreferences, key) {
    mutableStateOf(getter(sharedPreferences))
  }
  DisposableEffect(sharedPreferences, key) {
    val listener = OnSharedPreferenceChangeListener { prefs, k ->
      if (k == key) {
        currentValue.value = getter(prefs)
      }
    }
    sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    onDispose {
      sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
  }
  LaunchedEffect(key, sharedPreferences) {
    snapshotFlow { currentValue.value }
      .drop(1)
      .collect { newValue ->
        setter(sharedPreferences, newValue)
      }
  }
  return currentValue
}
