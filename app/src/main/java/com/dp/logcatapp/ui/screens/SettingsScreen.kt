package com.dp.logcatapp.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.net.toUri
import com.dp.logcat.LogcatUtil
import com.dp.logcatapp.BuildConfig
import com.dp.logcatapp.R
import com.dp.logcatapp.ui.common.Dialog
import com.dp.logcatapp.ui.common.DialogButton
import com.dp.logcatapp.ui.common.MaybeShowPermissionRequiredDialog
import com.dp.logcatapp.ui.common.WithTooltip
import com.dp.logcatapp.ui.screens.Preference.PreferenceRow
import com.dp.logcatapp.ui.screens.Preference.SectionDivider
import com.dp.logcatapp.ui.screens.Preference.SectionName
import com.dp.logcatapp.ui.theme.AppTypography
import com.dp.logcatapp.ui.theme.Shapes
import com.dp.logcatapp.ui.theme.isDynamicThemeAvailable
import com.dp.logcatapp.util.SettingsPrefKeys
import com.dp.logcatapp.util.findActivity
import com.dp.logcatapp.util.isReadLogsPermissionGranted
import com.dp.logcatapp.util.rememberBooleanSharedPreference
import com.dp.logcatapp.util.rememberIntSharedPreference
import com.dp.logcatapp.util.rememberStringSetSharedPreference
import com.dp.logcatapp.util.rememberStringSharedPreference
import com.dp.logcatapp.util.showToast
import java.text.NumberFormat

private const val REPO_URL = "https://github.com/darshanparajuli/LogcatReader"
private const val PLAY_URL = "https://play.google.com/store/apps/details?id=%s"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  modifier: Modifier,
) {
  val context = LocalContext.current
  Scaffold(
    modifier = modifier,
    contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical),
    topBar = {
      TopAppBar(
        navigationIcon = {
          WithTooltip(
            modifier = Modifier.windowInsetsPadding(
              WindowInsets.safeDrawing
                .only(WindowInsetsSides.Start)
            ),
            text = stringResource(R.string.navigate_up),
          ) {
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
          }
        },
        title = {
          Text(
            text = stringResource(R.string.settings),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .consumeWindowInsets(innerPadding),
      contentPadding = innerPadding,
    ) {
      item(
        key = "permission_status"
      ) {
        var showPermissionDialog by remember { mutableStateOf(false) }
        ReadLogsPermissionStatus(
          modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
              WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal,
              )
            )
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
          onClickPermissionNotGranted = {
            showPermissionDialog = true
          }
        )
        if (showPermissionDialog) {
          MaybeShowPermissionRequiredDialog(
            forceShow = true,
            onDismissed = {
              showPermissionDialog = false
            }
          )
        }
      }
      itemsIndexed(
        items = settingRows,
        key = { index, _ -> index },
      ) { index, item ->
        when (item) {
          is SectionName -> {
            Text(
              modifier = Modifier
                .windowInsetsPadding(
                  WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal,
                  )
                )
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
              text = stringResource(item.nameRes),
              color = MaterialTheme.colorScheme.tertiary,
              style = AppTypography.titleMedium,
            )
          }
          is PreferenceRow -> when (item.type) {
            PreferenceType.KeepScreenOn -> KeepScreenOn(
              modifier = Modifier
                .windowInsetsPadding(
                  WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal,
                  )
                ),
            )
            PreferenceType.FilterOnSearch -> FilterOnSearch(
              modifier = Modifier
                .windowInsetsPadding(
                  WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal,
                  )
                ),
            )
            PreferenceType.Theme -> Theme(
              modifier = Modifier
                .windowInsetsPadding(
                  WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal,
                  )
                ),
            )
            PreferenceType.DynamicColor -> DynamicColor(
              modifier = Modifier
                .windowInsetsPadding(
                  WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal,
                  )
                ),
            )
            PreferenceType.PollInterval -> PollInterval(
              modifier = Modifier
                .windowInsetsPadding(
                  WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal,
                  )
                ),
            )
            PreferenceType.Buffers -> Buffers(
              modifier = Modifier
                .windowInsetsPadding(
                  WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal,
                  )
                ),
            )
            PreferenceType.MaxLogs -> MaxLogs(
              modifier = Modifier
                .windowInsetsPadding(
                  WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal,
                  )
                ),
            )
            PreferenceType.SaveLocation -> SaveLocation(
              modifier = Modifier
                .windowInsetsPadding(
                  WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal,
                  )
                ),
            )
            PreferenceType.GithubRepoInfo -> GithubRepoInfo(
              modifier = Modifier
                .windowInsetsPadding(
                  WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal,
                  )
                ),
            )
            PreferenceType.AppInfo -> AppInfo(
              modifier = Modifier
                .windowInsetsPadding(
                  WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal,
                  )
                ),
            )
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
fun ReadLogsPermissionStatus(
  onClickPermissionNotGranted: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val hasReadLogsPermission = remember(context) {
    context.isReadLogsPermissionGranted()
  }
  ElevatedCard(
    modifier = modifier
      .clickable(
        enabled = !hasReadLogsPermission,
        onClick = onClickPermissionNotGranted,
      ),
    colors = CardDefaults.cardColors(
      containerColor = if (hasReadLogsPermission) {
        MaterialTheme.colorScheme.secondaryContainer
      } else {
        MaterialTheme.colorScheme.errorContainer
      },
    ),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Icon(
        imageVector = if (hasReadLogsPermission) {
          Icons.Default.CheckCircle
        } else {
          Icons.Default.Warning
        },
        contentDescription = null,
      )

      Text(
        text = if (hasReadLogsPermission) {
          stringResource(R.string.read_logs_permission_granted)
        } else {
          stringResource(R.string.read_logs_permission_not_granted)
        },
        style = AppTypography.titleMedium,
      )
    }
  }
}

@Composable
private fun KeepScreenOn(
  modifier: Modifier = Modifier,
) {
  var preference by rememberBooleanSharedPreference(
    key = SettingsPrefKeys.General.KEY_KEEP_SCREEN_ON,
    default = SettingsPrefKeys.General.Default.KEY_KEEP_SCREEN_ON,
  )
  ListItem(
    modifier = modifier
      .fillMaxWidth()
      .clickable {
        preference = !preference
      },
    leadingContent = {
      Icon(Icons.Default.Visibility, contentDescription = null)
    },
    headlineContent = {
      Text(stringResource(R.string.pref_general_keep_screen_on))
    },
    trailingContent = {
      Switch(
        checked = preference,
        onCheckedChange = null,
      )
    }
  )
}

@Composable
private fun FilterOnSearch(
  modifier: Modifier = Modifier,
) {
  var preference by rememberBooleanSharedPreference(
    key = SettingsPrefKeys.General.KEY_FILTER_ON_SEARCH,
    default = SettingsPrefKeys.General.Default.KEY_FILTER_ON_SEARCH,
  )
  ListItem(
    modifier = modifier
      .fillMaxWidth()
      .clickable {
        preference = !preference
      },
    leadingContent = {
      Icon(Icons.Default.Visibility, contentDescription = null)
    },
    headlineContent = {
      Text(stringResource(R.string.pref_general_filter_on_search))
    },
    supportingContent = {
      Text(stringResource(R.string.pref_general_filter_on_search_desc))
    },
    trailingContent = {
      Switch(
        checked = preference,
        onCheckedChange = null,
      )
    }
  )
}

@Composable
private fun Theme(
  modifier: Modifier = Modifier,
) {
  var preference by rememberStringSharedPreference(
    key = SettingsPrefKeys.Appearance.KEY_THEME,
    default = SettingsPrefKeys.Appearance.Default.THEME,
  )
  var showSelectionDialog by rememberSaveable { mutableStateOf(false) }
  val themeValues = stringArrayResource(R.array.pref_appearance_theme_values)
  val themeOptions = stringArrayResource(R.array.pref_appearance_theme_entries)
  ListItem(
    modifier = modifier
      .fillMaxWidth()
      .clickable {
        showSelectionDialog = true
      },
    leadingContent = {
      Icon(Icons.Default.ColorLens, contentDescription = null)
    },
    headlineContent = {
      Text(stringResource(R.string.pref_theme_title))
    },
    supportingContent = {
      Text(themeOptions[preference!!.toInt()])
    },
  )
  if (showSelectionDialog) {
    SelectionDialog(
      title = stringResource(R.string.pref_theme_title),
      initialSelected = themeValues.indexOf(preference!!),
      options = themeOptions.toList(),
      onDismiss = {
        showSelectionDialog = false
      },
      onClickOk = { index ->
        showSelectionDialog = false
        preference = themeValues[index]
      }
    )
  }
}

@Composable
private fun DynamicColor(
  modifier: Modifier = Modifier,
) {
  var preference by rememberBooleanSharedPreference(
    key = SettingsPrefKeys.Appearance.KEY_DYNAMIC_COLOR,
    default = SettingsPrefKeys.Appearance.Default.DYNAMIC_COLOR,
  )
  ListItem(
    modifier = modifier
      .fillMaxWidth()
      .clickable {
        preference = !preference
      },
    leadingContent = {
      Icon(Icons.Default.Style, contentDescription = null)
    },
    headlineContent = {
      Text(stringResource(R.string.pref_dynamic_color))
    },
    trailingContent = {
      Switch(
        checked = preference,
        onCheckedChange = null,
      )
    }
  )
}

@Composable
private fun PollInterval(
  modifier: Modifier = Modifier,
) {
  var preference by rememberIntSharedPreference(
    key = SettingsPrefKeys.Logcat.KEY_POLL_INTERVAL,
    default = SettingsPrefKeys.Logcat.Default.POLL_INTERVAL,
  )
  var showInputDialog by rememberSaveable { mutableStateOf(false) }
  ListItem(
    modifier = modifier
      .fillMaxWidth()
      .clickable {
        showInputDialog = true
      },
    leadingContent = {
      Icon(Icons.Default.Poll, contentDescription = null)
    },
    headlineContent = {
      Text(stringResource(R.string.pref_poll_interval_title))
    },
    supportingContent = {
      Text("${preference} ms")
    },
  )
  if (showInputDialog) {
    val context = LocalContext.current
    InputDialog(
      label = stringResource(R.string.pref_poll_interval_title),
      initialValue = preference.toString(),
      onDismiss = { showInputDialog = false },
      keyboardType = KeyboardType.Number,
      onOk = { newValue ->
        try {
          val num = newValue.toInt()
          if (num <= 0) {
            context.showToast(context.getString(R.string.value_must_be_greater_than_0))
          } else {
            showInputDialog = false
            preference = num
          }
        } catch (_: NumberFormatException) {
          context.showToast(context.getString(R.string.value_must_be_a_positive_integer))
        }
      },
    )
  }
}

@Composable
private fun Buffers(
  modifier: Modifier = Modifier,
) {
  var preference by rememberStringSetSharedPreference(
    key = SettingsPrefKeys.Logcat.KEY_BUFFERS,
    default = SettingsPrefKeys.Logcat.Default.BUFFERS,
  )
  val availableBuffers = LogcatUtil.AVAILABLE_BUFFERS
  var showBuffersSheet by rememberSaveable { mutableStateOf(false) }

  ListItem(
    modifier = modifier
      .fillMaxWidth()
      .clickable {
        showBuffersSheet = true
      },
    leadingContent = {
      Icon(Icons.Default.Checklist, contentDescription = null)
    },
    headlineContent = {
      Text(stringResource(R.string.pref_logcat_buffer_title))
    },
    supportingContent = {
      val summary = preference!!.map { e -> availableBuffers[e.toInt()] }
        .sorted()
        .joinToString(", ")
      Text(summary)
    },
  )

  if (showBuffersSheet) {
    MultiSelectDialog(
      title = stringResource(R.string.pref_logcat_buffer_title),
      initialSelected = availableBuffers.mapIndexedNotNull { index, b ->
        if (index.toString() in preference!!) index else null
      }.toSet(),
      options = availableBuffers.toList(),
      onDismiss = {
        showBuffersSheet = false
      },
      onClickOk = { selected ->
        showBuffersSheet = false
        if (selected.isNotEmpty()) {
          preference = selected.map { it.toString() }.toSet()
        }
      }
    )
  }
}

@Composable
private fun MaxLogs(
  modifier: Modifier = Modifier,
) {
  var preference by rememberIntSharedPreference(
    key = SettingsPrefKeys.Logcat.KEY_MAX_LOGS,
    default = SettingsPrefKeys.Logcat.Default.MAX_LOGS,
  )
  var showInputDialog by rememberSaveable { mutableStateOf(false) }
  ListItem(
    modifier = modifier
      .fillMaxWidth()
      .clickable {
        showInputDialog = true
      },
    leadingContent = {
      Icon(Icons.Default.Memory, contentDescription = null)
    },
    headlineContent = {
      Text(stringResource(R.string.pref_logcat_max_recent_logs_to_keep_in_memory))
    },
    supportingContent = {
      val numberFormat = remember { NumberFormat.getInstance() }
      Text(numberFormat.format(preference))
    },
  )
  if (showInputDialog) {
    val context = LocalContext.current
    InputDialog(
      label = stringResource(R.string.pref_logcat_max_recent_logs_to_keep_in_memory),
      initialValue = preference.toString(),
      onDismiss = { showInputDialog = false },
      keyboardType = KeyboardType.Number,
      onOk = { newValue ->
        try {
          val num = newValue.toInt()
          if (num < 1000) {
            context.showToast(context.getString(R.string.cannot_be_less_than_1000))
          } else {
            showInputDialog = false
            preference = num
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
  modifier: Modifier = Modifier,
) {
  var preference by rememberStringSharedPreference(
    key = SettingsPrefKeys.Logcat.KEY_SAVE_LOCATION,
    default = SettingsPrefKeys.Logcat.Default.SAVE_LOCATION,
  )
  var showSelectionDialog by rememberSaveable { mutableStateOf(false) }
  val isUsingInternal = preference!!.isEmpty()
  ListItem(
    modifier = modifier
      .fillMaxWidth()
      .clickable {
        showSelectionDialog = true
      },
    leadingContent = {
      Icon(Icons.Default.Folder, contentDescription = null)
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
      preference = uri.toString()
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
          preference = ""
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
    leadingContent = {
      Icon(Icons.Default.Code, contentDescription = null)
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
  val appName = stringResource(R.string.app_name)
  val context = LocalContext.current
  ListItem(
    modifier = modifier
      .fillMaxWidth()
      .clickable {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
          type = "text/plain"
          putExtra(Intent.EXTRA_TITLE, appName)
          putExtra(Intent.EXTRA_TEXT, PLAY_URL.format(context.packageName))
        }
        context.startActivity(Intent.createChooser(sendIntent, appName))
      },
    leadingContent = {
      Icon(Icons.Default.Adb, contentDescription = null)
    },
    headlineContent = {
      Text(stringResource(R.string.app_name))
    },
    supportingContent = {
      Text(stringResource(R.string.version_fmt).format(BuildConfig.VERSION_NAME))
    },
    trailingContent = {
      Icon(Icons.Default.Share, contentDescription = null)
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
  keyboardType: KeyboardType = KeyboardOptions.Default.keyboardType,
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
    title = label,
    primaryButton = DialogButton(
      text = stringResource(android.R.string.ok),
      onClick = {
        onOk(value.text.trim())
      },
      enabled = value.text.trim().isNotEmpty(),
    ),
    secondaryButton = DialogButton(
      text = stringResource(android.R.string.cancel),
      onClick = onDismiss,
    ),
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
        keyboardOptions = KeyboardOptions.Default.copy(
          keyboardType = keyboardType,
          imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
          onDone = {
            val trimmed = value.text.trim()
            if (trimmed.isNotEmpty()) {
              onOk(trimmed)
            }
          }
        ),
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
    primaryButton = DialogButton(
      text = stringResource(android.R.string.ok),
      onClick = {
        onClickOk(selected)
      },
    ),
    title = title,
    content = {
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
    },
  )
}

@Composable
private fun MultiSelectDialog(
  title: String,
  initialSelected: Set<Int>,
  options: List<String>,
  onDismiss: () -> Unit,
  onClickOk: (selected: Set<Int>) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selected by remember { mutableStateOf(initialSelected) }
  Dialog(
    modifier = modifier,
    onDismissRequest = onDismiss,
    primaryButton = DialogButton(
      text = stringResource(android.R.string.ok),
      onClick = {
        onClickOk(selected)
      },
    ),
    title = title,
    content = {
      options.fastForEachIndexed { index, option ->
        ListItem(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              if (index in selected) {
                selected -= index
              } else {
                selected += index
              }
            },
          headlineContent = {
            Text(option)
          },
          leadingContent = {
            Checkbox(
              checked = index in selected,
              onCheckedChange = null,
            )
          }
        )
      }
    },
  )
}

private val settingRows = listOfNotNull<Preference>(
  SectionName(R.string.pref_cat_general),
  PreferenceRow(PreferenceType.KeepScreenOn),
  PreferenceRow(PreferenceType.FilterOnSearch),
  SectionDivider,
  SectionName(R.string.pref_cat_appearance),
  PreferenceRow(PreferenceType.Theme),
  if (isDynamicThemeAvailable()) {
    PreferenceRow(PreferenceType.DynamicColor)
  } else null,
  SectionDivider,
  SectionName(R.string.logcat),
  PreferenceRow(PreferenceType.PollInterval),
  if (LogcatUtil.AVAILABLE_BUFFERS.isNotEmpty()) {
    PreferenceRow(PreferenceType.Buffers)
  } else null,
  PreferenceRow(PreferenceType.MaxLogs),
  PreferenceRow(PreferenceType.SaveLocation),
  SectionDivider,
  SectionName(R.string.about),
  PreferenceRow(PreferenceType.GithubRepoInfo),
  PreferenceRow(PreferenceType.AppInfo),
)

private enum class PreferenceType {
  KeepScreenOn,
  FilterOnSearch,
  Theme,
  DynamicColor,
  PollInterval,
  Buffers,
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
