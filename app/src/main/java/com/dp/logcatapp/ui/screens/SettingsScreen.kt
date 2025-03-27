package com.dp.logcatapp.ui.screens

import android.content.Intent
import android.content.SharedPreferences
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.dp.logcatapp.BuildConfig
import com.dp.logcatapp.R
import com.dp.logcatapp.ui.common.Dialog
import com.dp.logcatapp.ui.screens.Preference.PreferenceRow
import com.dp.logcatapp.ui.screens.Preference.SectionDivider
import com.dp.logcatapp.ui.screens.Preference.SectionName
import com.dp.logcatapp.ui.theme.AppTypography
import com.dp.logcatapp.ui.theme.Shapes
import com.dp.logcatapp.util.PreferenceKeys
import com.dp.logcatapp.util.findActivity
import com.dp.logcatapp.util.showToast
import java.text.NumberFormat

private const val REPO_URL = "https://github.com/darshanparajuli/LogcatReader"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  modifier: Modifier,
) {
  val context = LocalContext.current
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        navigationIcon = {
          IconButton(
            onClick = {
              context.findActivity()?.finish()
            },
            colors = IconButtonDefaults.iconButtonColors(
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Default.ArrowBack,
              contentDescription = null,
            )
          }
        },
        title = {
          Text(stringResource(R.string.settings))
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
      )
    }
  ) { innerPadding ->
    val sharedPrefs = remember(context) {
      PreferenceManager.getDefaultSharedPreferences(context)
    }
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .consumeWindowInsets(innerPadding),
      contentPadding = innerPadding,
    ) {
      itemsIndexed(
        items = settingRows,
        key = { index, _ -> index },
      ) { index, item ->
        when (item) {
          is SectionName -> {
            Text(
              modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
              text = stringResource(item.nameRes),
              color = MaterialTheme.colorScheme.primary,
              style = AppTypography.titleMedium,
            )
          }
          is PreferenceRow -> when (item.type) {
            PreferenceType.KeepScreenOn -> KeepScreenOn(sharedPrefs)
            PreferenceType.Theme -> Theme(sharedPrefs)
            PreferenceType.PollInterval -> PollInterval(sharedPrefs)
            PreferenceType.MaxLogs -> MaxLogs(sharedPrefs)
            PreferenceType.SaveLocation -> SaveLocation(sharedPrefs)
            PreferenceType.GithubRepoInfo -> GithubRepoInfo()
            PreferenceType.AppInfo -> AppInfo()
          }
          SectionDivider -> {
            HorizontalDivider(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
              thickness = 2.dp,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun KeepScreenOn(
  preferences: SharedPreferences,
  modifier: Modifier = Modifier,
) {
  val preference = remember {
    BooleanPreferenceState(
      key = PreferenceKeys.General.KEY_KEEP_SCREEN_ON,
      default = PreferenceKeys.General.Default.KEY_KEEP_SCREEN_ON,
      preferences = preferences,
    )
  }
  ListItem(
    modifier = modifier
      .fillMaxWidth()
      .clickable {
        preference.value = !preference.value
      },
    headlineContent = {
      Text(stringResource(R.string.pref_general_keep_screen_on))
    },
    trailingContent = {
      Switch(
        checked = preference.value,
        onCheckedChange = null,
      )
    }
  )
}

@Composable
private fun Theme(
  preferences: SharedPreferences,
  modifier: Modifier = Modifier,
) {
  val preference = remember {
    StringPreferenceState(
      key = PreferenceKeys.Appearance.KEY_THEME,
      default = PreferenceKeys.Appearance.Default.THEME,
      preferences = preferences,
    )
  }
  var showSelectionDialog by remember { mutableStateOf(false) }
  val themeValues = stringArrayResource(R.array.pref_appearance_theme_values)
  val themeOptions = stringArrayResource(R.array.pref_appearance_theme_entries)
  ListItem(
    modifier = modifier
      .fillMaxWidth()
      .clickable {
        showSelectionDialog = true
      },
    headlineContent = {
      Text(stringResource(R.string.pref_theme_title))
    },
    supportingContent = {
      Text(themeOptions[preference.value.toInt()])
    },
  )
  if (showSelectionDialog) {
    SelectionDialog(
      title = stringResource(R.string.pref_theme_title),
      initialSelected = themeValues.indexOf(preference.value),
      options = themeOptions.toList(),
      onDismiss = {
        showSelectionDialog = false
      },
      onClickOk = { index ->
        showSelectionDialog = false
        preference.value = themeValues[index]
      }
    )
  }
}

@Composable
private fun PollInterval(
  preferences: SharedPreferences,
  modifier: Modifier = Modifier,
) {
  val preference = remember {
    StringPreferenceState(
      key = PreferenceKeys.Logcat.KEY_POLL_INTERVAL,
      default = PreferenceKeys.Logcat.Default.POLL_INTERVAL,
      preferences = preferences,
    )
  }
  var showInputDialog by remember { mutableStateOf(false) }
  ListItem(
    modifier = modifier
      .fillMaxWidth()
      .clickable {
        showInputDialog = true
      },
    headlineContent = {
      Text(stringResource(R.string.pref_poll_interval_title))
    },
    supportingContent = {
      Text("${preference.value} ms")
    },
  )
  if (showInputDialog) {
    val context = LocalContext.current
    InputDialog(
      label = stringResource(R.string.pref_poll_interval_title),
      initialValue = preference.value,
      onDismiss = { showInputDialog = false },
      onOk = { newValue ->
        try {
          val num = newValue.toLong()
          if (num <= 0) {
            context.showToast(context.getString(R.string.value_must_be_greater_than_0))
          } else {
            showInputDialog = false
            preference.value = newValue
          }
        } catch (_: NumberFormatException) {
          context.showToast(context.getString(R.string.value_must_be_a_positive_integer))
        }
      },
    )
  }
}

@Composable
private fun MaxLogs(
  preferences: SharedPreferences,
  modifier: Modifier = Modifier,
) {
  val preference = remember {
    StringPreferenceState(
      key = PreferenceKeys.Logcat.KEY_MAX_LOGS,
      default = PreferenceKeys.Logcat.Default.MAX_LOGS,
      preferences = preferences,
    )
  }
  var showInputDialog by remember { mutableStateOf(false) }
  ListItem(
    modifier = modifier
      .fillMaxWidth()
      .clickable {
        showInputDialog = true
      },
    headlineContent = {
      Text(stringResource(R.string.pref_logcat_max_recent_logs_to_keep_in_memory))
    },
    supportingContent = {
      val numberFormat = remember { NumberFormat.getInstance() }
      Text(numberFormat.format(preference.value.toInt()))
    },
  )
  if (showInputDialog) {
    val context = LocalContext.current
    InputDialog(
      label = stringResource(R.string.pref_logcat_max_recent_logs_to_keep_in_memory),
      initialValue = preference.value,
      onDismiss = { showInputDialog = false },
      onOk = { newValue ->
        try {
          val num = newValue.toLong()
          if (num < 1000) {
            context.showToast(context.getString(R.string.cannot_be_less_than_1000))
          } else {
            showInputDialog = false
            preference.value = newValue
          }
        } catch (_: NumberFormatException) {
          context.showToast(context.getString(R.string.not_a_valid_number))
        }
      },
    )
  }
}

@Composable
private fun SaveLocation(
  preferences: SharedPreferences,
  modifier: Modifier = Modifier,
) {
  val preference = remember {
    StringPreferenceState(
      key = PreferenceKeys.Logcat.KEY_SAVE_LOCATION,
      default = PreferenceKeys.Logcat.Default.SAVE_LOCATION,
      preferences = preferences,
    )
  }
  var showSelectionDialog by remember { mutableStateOf(false) }
  val isUsingInternal = preference.value.isEmpty()
  ListItem(
    modifier = modifier
      .fillMaxWidth()
      .clickable {
        showSelectionDialog = true
      },
    headlineContent = {
      Text(stringResource(R.string.save_location))
    },
    supportingContent = {
      Text(
        if (isUsingInternal) {
          stringResource(R.string.save_location_internal)
        } else {
          stringResource(R.string.save_location_custom)
        }
      )
    },
  )

  val context = LocalContext.current
  val documentTreeLauncher = rememberLauncherForActivityResult(OpenDocumentTree()) { uri ->
    if (uri != null) {
      context.contentResolver.takePersistableUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION or
          Intent.FLAG_GRANT_WRITE_URI_PERMISSION
      )
      preference.value = uri.toString()
    }
  }

  if (showSelectionDialog) {
    val options = stringArrayResource(R.array.save_location_options).toList()
    SelectionDialog(
      title = stringResource(R.string.save_location),
      initialSelected = if (isUsingInternal) 0 else 1,
      options = options.toList(),
      onDismiss = {
        showSelectionDialog = false
      },
      onClickOk = { index ->
        showSelectionDialog = false
        if (index == 0 && !isUsingInternal) {
          preference.value = ""
        } else if (index == 1 && isUsingInternal) {
          documentTreeLauncher.launch(null)
        }
      }
    )
  }
}

@Composable
private fun GithubRepoInfo(
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  ListItem(
    modifier = modifier
      .fillMaxWidth()
      .clickable {
        context.startActivity(Intent(Intent.ACTION_VIEW, REPO_URL.toUri()))
      },
    headlineContent = {
      Text(stringResource(R.string.pref_key_about_github_repo_title))
    },
    supportingContent = {
      Text(stringResource(R.string.pref_key_about_github_repo_summary))
    },
  )
}

@Composable
private fun AppInfo(
  modifier: Modifier = Modifier,
) {
  ListItem(
    modifier = modifier
      .fillMaxWidth(),
    headlineContent = {
      Text(stringResource(R.string.app_name))
    },
    supportingContent = {
      Text(stringResource(R.string.version_fmt).format(BuildConfig.VERSION_NAME))
    },
  )
}

@Composable
private fun InputDialog(
  label: String,
  initialValue: String,
  onDismiss: () -> Unit,
  onOk: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var value by remember {
    mutableStateOf(
      TextFieldValue(
        text = initialValue,
        selection = TextRange(start = 0, end = initialValue.length)
      )
    )
  }
  Dialog(
    modifier = modifier,
    onDismissRequest = onDismiss,
    title = { Text(label) },
    confirmButton = {
      TextButton(
        onClick = {
          onOk(value.text.trim())
        },
        enabled = value.text.trim().isNotEmpty(),
      ) { Text(stringResource(android.R.string.ok)) }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
      ) { Text(stringResource(android.R.string.cancel)) }
    },
    content = {
      val focusRequester = remember { FocusRequester() }
      LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
      }
      TextField(
        modifier = Modifier
          .fillMaxWidth()
          .focusRequester(focusRequester),
        value = value,
        onValueChange = { value = it },
        singleLine = true,
        maxLines = 1,
        shape = Shapes.medium,
        colors = TextFieldDefaults.colors(
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent,
        )
      )
    }
  )
}

@Composable
private fun SelectionDialog(
  title: String,
  initialSelected: Int,
  options: List<String>,
  onDismiss: () -> Unit,
  onClickOk: (index: Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selected by remember { mutableIntStateOf(initialSelected) }
  Dialog(
    modifier = modifier,
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(
        onClick = {
          onClickOk(selected)
        },
      ) { Text(stringResource(android.R.string.ok)) }
    },
    title = { Text(title) },
    content = {
      Column {
        options.fastForEachIndexed { index, option ->
          ListItem(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                selected = index
              },
            headlineContent = {
              Text(option)
            },
            leadingContent = {
              RadioButton(
                selected = selected == index,
                onClick = null,
              )
            }
          )
        }
      }
    },
  )
}

private val settingRows = listOf<Preference>(
  SectionName(R.string.pref_cat_general),
  PreferenceRow(PreferenceType.KeepScreenOn),
  SectionDivider,
  SectionName(R.string.pref_cat_appearance),
  PreferenceRow(PreferenceType.Theme),
  SectionDivider,
  SectionName(R.string.logcat),
  PreferenceRow(PreferenceType.PollInterval),
  PreferenceRow(PreferenceType.MaxLogs),
  PreferenceRow(PreferenceType.SaveLocation),
  SectionDivider,
  SectionName(R.string.about),
  PreferenceRow(PreferenceType.GithubRepoInfo),
  PreferenceRow(PreferenceType.AppInfo),
)

private enum class PreferenceType {
  KeepScreenOn,
  Theme,
  PollInterval,
  MaxLogs,
  SaveLocation,
  GithubRepoInfo,
  AppInfo,
  ;
}

private sealed interface Preference {
  data class SectionName(val nameRes: Int) : Preference
  data class PreferenceRow(val type: PreferenceType) : Preference
  data object SectionDivider : Preference
}

private class BooleanPreferenceState(
  val key: String,
  val default: Boolean,
  private val preferences: SharedPreferences,
) {
  private val _value = mutableStateOf(preferences.getBoolean(key, default))
  var value: Boolean
    get() = _value.value
    set(value) {
      preferences.edit { putBoolean(key, value) }
      _value.value = value
    }
}

private class StringPreferenceState(
  val key: String,
  val default: String,
  private val preferences: SharedPreferences,
) {
  private val _value = mutableStateOf(preferences.getString(key, default).orEmpty())
  var value: String
    get() = _value.value
    set(value) {
      preferences.edit { putString(key, value) }
      _value.value = value
    }
}
