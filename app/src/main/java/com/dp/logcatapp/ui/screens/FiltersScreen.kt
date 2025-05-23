package com.dp.logcatapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.dp.logcat.Log
import com.dp.logcat.LogcatSession
import com.dp.logcatapp.R
import com.dp.logcatapp.db.DateRange
import com.dp.logcatapp.db.FilterInfo
import com.dp.logcatapp.db.LogLevel
import com.dp.logcatapp.db.LogcatReaderDatabase
import com.dp.logcatapp.db.RegexEnabledFilterType
import com.dp.logcatapp.ui.common.Dialog
import com.dp.logcatapp.ui.common.DialogButton
import com.dp.logcatapp.ui.common.WithTooltip
import com.dp.logcatapp.ui.theme.AppTypography
import com.dp.logcatapp.ui.theme.Shapes
import com.dp.logcatapp.util.AppInfo
import com.dp.logcatapp.util.findActivity
import com.dp.logcatapp.util.rememberAppInfoByUidMap
import com.dp.logcatapp.util.showToast
import com.dp.logcatapp.util.toRegexOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar

private const val DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm"
private const val DATE_TIME_FORMAT_NO_YEAR = "MM-dd HH:mm"
private const val DATE_FORMAT = "yyyy-MM-dd"
private const val DATE_FORMAT_NO_YEAR = "MM-dd"
private const val TIME_FORMAT = "HH:mm"

@OptIn(
  ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
  ExperimentalFoundationApi::class
)
@Composable
fun FiltersScreen(
  modifier: Modifier,
  prepopulateFilterInfo: PrepopulateFilterInfo?,
  viewModel: FiltersScreenViewModel = viewModel(),
) {
  val context = LocalContext.current
  val db = remember(context) { LogcatReaderDatabase.getInstance(context) }

  val appInfoMap by rememberUpdatedState(rememberAppInfoByUidMap())
  val filters by db.filterDao()
    .filters()
    .collectAsState(null)

  var showAddFilterDialog by rememberSaveable { mutableStateOf(prepopulateFilterInfo != null) }
  var showPackageSelector by rememberSaveable { mutableStateOf(false) }
  val coroutineScope = rememberCoroutineScope()

  if (viewModel.selectedFilters.isNotEmpty()) {
    BackHandler { viewModel.selectedFilters = emptySet() }
  }

  Scaffold(
    modifier = modifier,
    contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical),
    topBar = {
      var showDropDownMenu by remember { mutableStateOf(false) }
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
              Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
            }
          }
        },
        title = {
          Text(
            text = stringResource(R.string.filters),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
          )
        },
        actions = {
          val insetPadding = WindowInsets.displayCutout
            .only(WindowInsetsSides.End)
            .asPaddingValues()
          Row(
            modifier = Modifier.padding(insetPadding)
          ) {
            val uidOptionSupported by LogcatSession.uidOptionSupported.collectAsState()
            if (uidOptionSupported == true) {
              WithTooltip(
                text = stringResource(R.string.filter_by_apps)
              ) {
                IconButton(
                  onClick = { showPackageSelector = true },
                  colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  ),
                ) {
                  Icon(Icons.Default.Apps, contentDescription = null)
                }
              }
            }
            WithTooltip(
              text = stringResource(R.string.more_options)
            ) {
              IconButton(
                onClick = { showDropDownMenu = true },
                colors = IconButtonDefaults.iconButtonColors(
                  contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
              ) {
                Icon(Icons.Default.MoreVert, contentDescription = null)
              }
            }
            DropdownMenu(
              expanded = showDropDownMenu,
              onDismissRequest = { showDropDownMenu = false },
            ) {
              DropdownMenuItem(
                leadingIcon = {
                  Icon(Icons.Default.ClearAll, contentDescription = null)
                },
                text = {
                  Text(
                    text = stringResource(R.string.clear),
                  )
                },
                enabled = !filters.isNullOrEmpty(),
                onClick = {
                  showDropDownMenu = false
                  coroutineScope.launch {
                    val filterDao = db.filterDao()
                    withContext(Dispatchers.IO) {
                      filterDao.deleteAll()
                    }
                  }
                },
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
      )
      AnimatedVisibility(
        visible = viewModel.selectedFilters.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        SelectFiltersAppBar(
          title = viewModel.selectedFilters.size.toString(),
          onClickClose = { viewModel.selectedFilters = emptySet() },
          onClickSelectAll = {
            viewModel.selectedFilters = filters.orEmpty().toSet()
          },
          onClickEnable = {
            val selectedFilters = viewModel.selectedFilters.toSet()
            viewModel.selectedFilters = emptySet()
            coroutineScope.launch {
              withContext(Dispatchers.IO) {
                db.filterDao().update(
                  *selectedFilters.map { it.copy(enabled = true) }
                    .toTypedArray()
                )
              }
            }
          },
          onClickDisable = {
            val selectedFilters = viewModel.selectedFilters.toSet()
            viewModel.selectedFilters = emptySet()
            coroutineScope.launch {
              withContext(Dispatchers.IO) {
                db.filterDao().update(
                  *selectedFilters.map { it.copy(enabled = false) }
                    .toTypedArray()
                )
              }
            }
          },
          onClickDelete = {
            val selectedFilters = viewModel.selectedFilters.toSet()
            viewModel.selectedFilters = emptySet()
            coroutineScope.launch {
              withContext(Dispatchers.IO) {
                db.filterDao().delete(*selectedFilters.toTypedArray())
              }
            }
          }
        )
      }
    },
    floatingActionButton = {
      AnimatedVisibility(
        visible = viewModel.selectedFilters.isEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        FloatingActionButton(
          modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .size(48.dp),
          onClick = {
            showAddFilterDialog = true
          }
        ) {
          Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null
          )
        }
      }
    },
  ) { innerPadding ->
    val filters = filters

    viewModel.showEditFilterDialog?.let { filterInfo ->
      AddOrEditFilterSheet(
        initialTag = filterInfo.tag,
        initialKeyword = filterInfo.message,
        initialPackageName = filterInfo.packageName,
        initialPid = filterInfo.pid?.toString(),
        initialTid = filterInfo.tid?.toString(),
        initialExclude = filterInfo.exclude,
        initialLogLevels = filterInfo.logLevels.orEmpty(),
        initialEnabled = filterInfo.enabled,
        initialRegexEnabledTypes = filterInfo.regexEnabledFilterTypes.orEmpty(),
        initialDateRange = filterInfo.dateRange,
        onDismiss = { viewModel.showEditFilterDialog = null },
        onSave = { data ->
          viewModel.showEditFilterDialog = null

          val keyword = data.keyword
          val tag = data.tag
          val pid = data.pid
          val tid = data.tid
          val exclude = data.exclude
          val packageName = data.packageName
          val enabled = data.enabled
          val selectedLogLevels = data.selectedLogLevels
          val regexEnabledTypes = data.regexEnabledTypes

          val filterInfo = filterInfo.copy(
            tag = tag.takeIf { it.isNotEmpty() },
            message = keyword.takeIf { it.isNotEmpty() },
            pid = pid.takeIf { it.isNotEmpty() }?.toIntOrNull(),
            tid = tid.takeIf { it.isNotEmpty() }?.toIntOrNull(),
            packageName = packageName.takeIf { it.isNotEmpty() },
            logLevels = if (selectedLogLevels.isEmpty()) {
              null
            } else {
              selectedLogLevels
            },
            exclude = exclude,
            enabled = enabled ?: filterInfo.enabled,
            regexEnabledFilterTypes = regexEnabledTypes,
            dateRange = data.dateRange,
          )

          coroutineScope.launch {
            withContext(Dispatchers.IO) {
              db.filterDao().update(filterInfo)
            }
          }
        }
      )
    }
    if (showAddFilterDialog) {
      AddOrEditFilterSheet(
        initialTag = prepopulateFilterInfo?.log?.tag,
        initialPackageName = prepopulateFilterInfo?.packageName,
        initialPid = prepopulateFilterInfo?.log?.pid,
        initialTid = prepopulateFilterInfo?.log?.tid,
        initialExclude = prepopulateFilterInfo?.exclude,
        initialLogLevels = prepopulateFilterInfo?.log?.priority?.let { p ->
          LogLevel.entries.find { it.label.startsWith(p) }?.let { level ->
            setOf(level)
          }
        }.orEmpty(),
        onDismiss = { showAddFilterDialog = false },
        onSave = { data ->
          showAddFilterDialog = false

          val keyword = data.keyword
          val tag = data.tag
          val pid = data.pid
          val tid = data.tid
          val exclude = data.exclude
          val packageName = data.packageName
          val selectedLogLevels = data.selectedLogLevels
          val regexEnabledTypes = data.regexEnabledTypes

          val filterInfo = FilterInfo(
            tag = tag.takeIf { it.isNotEmpty() },
            message = keyword.takeIf { it.isNotEmpty() },
            pid = pid.takeIf { it.isNotEmpty() }?.toIntOrNull(),
            tid = tid.takeIf { it.isNotEmpty() }?.toIntOrNull(),
            packageName = packageName.takeIf { it.isNotEmpty() },
            logLevels = if (selectedLogLevels.isEmpty()) {
              null
            } else {
              selectedLogLevels
            },
            exclude = exclude,
            regexEnabledFilterTypes = regexEnabledTypes,
            dateRange = data.dateRange,
          )

          coroutineScope.launch {
            withContext(Dispatchers.IO) {
              db.filterDao().insert(filterInfo)
            }
          }
        }
      )
    }

    if (showPackageSelector) {
      var savingInProgress by remember { mutableStateOf(false) }
      PackageSelectorSheet(
        installedApps = appInfoMap?.values?.toList().orEmpty(),
        onDismiss = {
          showPackageSelector = false
        },
        savingInProgress = savingInProgress,
        onSelected = { selected ->
          savingInProgress = true
          val filters = selected.map { FilterInfo(packageName = it) }
          coroutineScope.launch {
            withContext(Dispatchers.IO) {
              db.filterDao().insert(*filters.toTypedArray())
            }
            showPackageSelector = false
          }
        }
      )
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .consumeWindowInsets(innerPadding),
      contentPadding = innerPadding,
    ) {
      if (filters == null) {
        item(key = "loading-indicator") {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .safeDrawingPadding()
              .padding(16.dp),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(48.dp),
            )
          }
        }
      } else {
        itemsIndexed(
          items = filters,
          key = { index, _ -> index },
        ) { index, item ->
          FilterItem(
            modifier = Modifier
              .fillMaxWidth()
              .combinedClickable(
                onLongClick = {
                  viewModel.selectedFilters += item
                },
                onClick = {
                  if (viewModel.selectedFilters.isEmpty()) {
                    viewModel.showEditFilterDialog = item
                  } else {
                    if (item in viewModel.selectedFilters) {
                      viewModel.selectedFilters -= item
                    } else {
                      viewModel.selectedFilters += item
                    }
                  }
                }
              )
              .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                  WindowInsetsSides.Horizontal,
                )
              ),
            tag = item.tag,
            message = item.message,
            pid = item.pid?.toString(),
            tid = item.tid?.toString(),
            priorities = item.logLevels,
            exclude = item.exclude,
            packageName = item.packageName,
            enabled = item.enabled,
            selectable = viewModel.selectedFilters.isNotEmpty(),
            selected = item in viewModel.selectedFilters,
            regexEnabledFilterType = item.regexEnabledFilterTypes.orEmpty(),
            dateRange = item.dateRange,
          )
          HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectFiltersAppBar(
  title: String,
  onClickClose: () -> Unit,
  onClickSelectAll: () -> Unit,
  onClickEnable: () -> Unit,
  onClickDisable: () -> Unit,
  onClickDelete: () -> Unit,
) {
  TopAppBar(
    navigationIcon = {
      WithTooltip(
        modifier = Modifier.windowInsetsPadding(
          WindowInsets.safeDrawing
            .only(WindowInsetsSides.Start)
        ),
        text = stringResource(R.string.close)
      ) {
        IconButton(
          onClick = onClickClose,
          colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
          )
        }
      }
    },
    title = {
      Text(
        text = title,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
      )
    },
    actions = {
      Row(
        modifier = Modifier.windowInsetsPadding(
          WindowInsets.safeDrawing
            .only(WindowInsetsSides.End)
        ),
      ) {
        WithTooltip(
          text = stringResource(R.string.enable),
        ) {
          IconButton(
            onClick = onClickEnable,
            colors = IconButtonDefaults.iconButtonColors(
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
          ) {
            Icon(
              imageVector = Icons.Default.FilterList,
              contentDescription = null,
            )
          }
        }
        WithTooltip(
          text = stringResource(R.string.disable),
        ) {
          IconButton(
            onClick = onClickDisable,
            colors = IconButtonDefaults.iconButtonColors(
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
          ) {
            Icon(
              imageVector = Icons.Default.FilterListOff,
              contentDescription = null,
            )
          }
        }
        WithTooltip(
          text = stringResource(R.string.select_all),
        ) {
          IconButton(
            onClick = onClickSelectAll,
            colors = IconButtonDefaults.iconButtonColors(
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
          ) {
            Icon(
              imageVector = Icons.Default.SelectAll,
              contentDescription = null,
            )
          }
        }
        Box {
          var showDropDownMenu by remember { mutableStateOf(false) }
          WithTooltip(
            text = stringResource(R.string.more_options),
          ) {
            IconButton(
              onClick = {
                showDropDownMenu = true
              },
              colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
            ) {
              Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
              )
            }
          }
          DropdownMenu(
            expanded = showDropDownMenu,
            onDismissRequest = { showDropDownMenu = false },
          ) {
            DropdownMenuItem(
              leadingIcon = {
                Icon(Icons.Default.Delete, contentDescription = null)
              },
              text = {
                Text(
                  text = stringResource(R.string.delete),
                )
              },
              onClick = onClickDelete,
            )
          }
        }
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer,
      titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ),
  )
}

private data class FilterData(
  val keyword: String,
  val tag: String,
  val pid: String,
  val tid: String,
  val packageName: String,
  val selectedLogLevels: Set<LogLevel>,
  val exclude: Boolean,
  val enabled: Boolean?,
  val regexEnabledTypes: Set<RegexEnabledFilterType>,
  val dateRange: DateRange?,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddOrEditFilterSheet(
  onDismiss: () -> Unit,
  onSave: (FilterData) -> Unit,
  modifier: Modifier = Modifier,
  initialKeyword: String? = null,
  initialTag: String? = null,
  initialPackageName: String? = null,
  initialPid: String? = null,
  initialTid: String? = null,
  initialExclude: Boolean? = null,
  initialDateRange: DateRange? = null,
  initialLogLevels: Set<LogLevel> = emptySet(),
  initialEnabled: Boolean? = null,
  initialRegexEnabledTypes: Set<RegexEnabledFilterType> = emptySet(),
) {
  var selectedLogLevels by rememberSaveable {
    mutableStateOf<Map<LogLevel, Boolean>>(
      LogLevel.entries.filter { it in initialLogLevels }.associate { level ->
        Pair(level, true)
      }
    )
  }

  var message by rememberSaveable { mutableStateOf(initialKeyword.orEmpty()) }
  var tag by rememberSaveable { mutableStateOf(initialTag.orEmpty()) }
  var packageName by rememberSaveable { mutableStateOf(initialPackageName.orEmpty()) }
  var messageRegexError by rememberSaveable { mutableStateOf(false) }
  var tagRegexError by rememberSaveable { mutableStateOf(false) }
  var packageNameRegexError by rememberSaveable { mutableStateOf(false) }
  var pid by rememberSaveable { mutableStateOf(initialPid.orEmpty()) }
  var tid by rememberSaveable { mutableStateOf(initialTid.orEmpty()) }
  var exclude by rememberSaveable { mutableStateOf(initialExclude ?: false) }
  var enabledState by rememberSaveable { mutableStateOf(initialEnabled) }
  var regexEnabledTypes by rememberSaveable { mutableStateOf(initialRegexEnabledTypes) }
  var showDateRangeSheet by rememberSaveable { mutableStateOf(false) }
  var dateRange by rememberSaveable { mutableStateOf<DateRange?>(initialDateRange) }

  ModalBottomSheet(
    modifier = modifier.statusBarsPadding(),
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Column(
      modifier = Modifier
        .verticalScroll(rememberScrollState())
        .padding(vertical = 16.dp),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          modifier = Modifier.weight(1f),
          text = stringResource(R.string.filter),
          style = AppTypography.headlineMedium,
        )
        Button(
          onClick = {
            onSave(
              FilterData(
                keyword = message,
                tag = tag,
                pid = pid,
                tid = tid,
                packageName = packageName,
                selectedLogLevels = selectedLogLevels.filterValues { it }.keys.toSet(),
                exclude = exclude,
                enabled = enabledState,
                regexEnabledTypes = regexEnabledTypes,
                dateRange = dateRange,
              )
            )
          },
          enabled = (message.isNotEmpty() ||
            tag.isNotEmpty() ||
            pid.isNotEmpty() ||
            tid.isNotEmpty() ||
            packageName.isNotEmpty() ||
            dateRange != null ||
            selectedLogLevels.any { (_, selected) -> selected }) &&
            !messageRegexError && !tagRegexError && !packageNameRegexError,
        ) {
          Text(
            stringResource(R.string.save),
            style = AppTypography.titleMedium,
          )
        }
      }
      Spacer(modifier = Modifier.height(16.dp))

      val messageRegexEnabled = RegexEnabledFilterType.Message in regexEnabledTypes
      InputField(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        label = stringResource(R.string.message),
        value = message,
        onValueChange = {
          message = it
          if (messageRegexEnabled) {
            messageRegexError = message.toRegexOrNull() == null
          }
        },
        regexEnabled = messageRegexEnabled,
        onClickRegex = {
          if (messageRegexEnabled) {
            regexEnabledTypes -= RegexEnabledFilterType.Message
            messageRegexError = false
          } else {
            regexEnabledTypes += RegexEnabledFilterType.Message
            messageRegexError = message.toRegexOrNull() == null
          }
        },
        isError = messageRegexError,
      )
      Spacer(modifier = Modifier.height(16.dp))

      val tagRegexEnabled = RegexEnabledFilterType.Tag in regexEnabledTypes
      InputField(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        label = stringResource(R.string.tag),
        maxLines = 1,
        value = tag,
        onValueChange = {
          tag = it
          if (tagRegexEnabled) {
            tagRegexError = tag.toRegexOrNull() == null
          }
        },
        regexEnabled = tagRegexEnabled,
        onClickRegex = {
          if (tagRegexEnabled) {
            regexEnabledTypes -= RegexEnabledFilterType.Tag
            tagRegexError = false
          } else {
            regexEnabledTypes += RegexEnabledFilterType.Tag
            tagRegexError = tag.toRegexOrNull() == null
          }
        },
        isError = tagRegexError,
      )
      Spacer(modifier = Modifier.height(16.dp))
      val uidSupported by LogcatSession.uidOptionSupported.collectAsState()
      if (uidSupported == true) {
        val packageNameRegexEnabled = RegexEnabledFilterType.PackageName in regexEnabledTypes
        InputField(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          label = stringResource(R.string.package_name),
          value = packageName,
          onValueChange = {
            packageName = it
            if (packageNameRegexEnabled) {
              packageNameRegexError = packageName.toRegexOrNull() == null
            }
          },
          regexEnabled = packageNameRegexEnabled,
          onClickRegex = {
            if (packageNameRegexEnabled) {
              regexEnabledTypes -= RegexEnabledFilterType.PackageName
              packageNameRegexError = false
            } else {
              regexEnabledTypes += RegexEnabledFilterType.PackageName
              packageNameRegexError = packageName.toRegexOrNull() == null
            }
          },
          isError = packageNameRegexError,
        )
        Spacer(modifier = Modifier.height(16.dp))
      }
      InputField(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        label = stringResource(R.string.process_id),
        maxLines = 1,
        value = pid,
        onValueChange = {
          if (it.isDigitsOnly()) {
            pid = it
          }
        },
        keyboardType = KeyboardType.Number,
      )
      Spacer(modifier = Modifier.height(16.dp))
      InputField(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        label = stringResource(R.string.thread_id),
        maxLines = 1,
        value = tid,
        onValueChange = {
          if (it.isDigitsOnly()) {
            tid = it
          }
        },
        keyboardType = KeyboardType.Number,
      )
      Spacer(modifier = Modifier.height(16.dp))
      DateInputField(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        value = remember(dateRange) { formatDateRange(dateRange) },
        hint = "Date/time range",
        onClickSelectDateRange = {
          showDateRangeSheet = true
        }
      )
      Spacer(modifier = Modifier.height(16.dp))
      FlowRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
      ) {
        LogLevel.entries.forEach { logLevel ->
          FilterChip(
            selected = selectedLogLevels.getOrElse(logLevel) { false },
            onClick = {
              selectedLogLevels =
                selectedLogLevels + Pair(logLevel, !selectedLogLevels.getOrElse(logLevel) { false })
            },
            label = {
              Text(logLevel.label)
            }
          )
        }
      }
      ListItem(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            exclude = !exclude
          },
        headlineContent = {
          Text(stringResource(R.string.exclude))
        },
        trailingContent = {
          Checkbox(
            checked = exclude,
            onCheckedChange = null,
          )
        },
        colors = ListItemDefaults.colors(
          containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
      )
      enabledState?.let { enabled ->
        ListItem(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              enabledState = !enabled
            },
          headlineContent = {
            Text(stringResource(R.string.enabled))
          },
          trailingContent = {
            Checkbox(
              checked = enabled,
              onCheckedChange = null,
            )
          },
          colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
          )
        )
      }
    }
  }
  if (showDateRangeSheet) {
    SelectDateRangeDialog(
      initialDateRange = dateRange,
      onDismiss = { showDateRangeSheet = false },
      onDone = { result ->
        showDateRangeSheet = false
        dateRange = result
      }
    )
  }
}

private fun formatDateRange(dateRange: DateRange?): AnnotatedString {
  return if (dateRange == null) {
    AnnotatedString("")
  } else {
    try {
      val dateTimeFormatNoYear = SimpleDateFormat(DATE_TIME_FORMAT_NO_YEAR)
      val start = dateRange.start?.let { dateTimeFormatNoYear.format(it) } ?: "n/a"
      val end = dateRange.end?.let { dateTimeFormatNoYear.format(it) } ?: "n/a"
      buildAnnotatedString {
        pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold))
        append(start)
        pop()
        append(" — ")
        pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold))
        append(end)
        pop()
      }
    } catch (_: ParseException) {
      AnnotatedString("")
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectDateRangeDialog(
  initialDateRange: DateRange?,
  onDismiss: () -> Unit,
  onDone: (DateRange) -> Unit,
  modifier: Modifier = Modifier,
) {
  val dateRegex = remember { "(\\d\\d?)-(\\d\\d?)".toRegex() }
  val timeRegex = remember { "(\\d\\d?):(\\d\\d?)".toRegex() }
  val dateFormat = remember { SimpleDateFormat(DATE_FORMAT).apply { isLenient = false } }
  val timeFormat = remember { SimpleDateFormat(TIME_FORMAT).apply { isLenient = false } }
  val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

  val (initialStartDate, initialStartTime, initialEndDate, initialEndTime) = remember {
    if (initialDateRange == null) {
      arrayOf("", "", "", "")
    } else {
      val dateFormatNoYear = SimpleDateFormat(DATE_FORMAT_NO_YEAR)
      val timeFormat = SimpleDateFormat(TIME_FORMAT)
      try {
        when {
          initialDateRange.start != null && initialDateRange.end != null -> {
            val startDateFormatted = dateFormatNoYear.format(initialDateRange.start)
            val startTimeFormatted = timeFormat.format(initialDateRange.start)
            val endDateFormatted = dateFormatNoYear.format(initialDateRange.end)
            val endTimeFormatted = timeFormat.format(initialDateRange.end)
            arrayOf(startDateFormatted, startTimeFormatted, endDateFormatted, endTimeFormatted)
          }
          initialDateRange.start != null -> {
            val startDateFormatted = dateFormatNoYear.format(initialDateRange.start)
            val startTimeFormatted = timeFormat.format(initialDateRange.start)
            arrayOf(startDateFormatted, startTimeFormatted, "", "")
          }
          initialDateRange.end != null -> {
            val endDateFormatted = dateFormatNoYear.format(initialDateRange.end)
            val endTimeFormatted = timeFormat.format(initialDateRange.end)
            arrayOf("", "", endDateFormatted, endTimeFormatted)
          }
          else -> arrayOf("", "", "", "")
        }
      } catch (_: ParseException) {
        arrayOf("", "", "", "")
      }
    }
  }
  var startDateText by rememberSaveable { mutableStateOf(initialStartDate) }
  var startTimeText by rememberSaveable { mutableStateOf(initialStartTime) }
  var startDateError by rememberSaveable { mutableStateOf(false) }
  var startTimeError by rememberSaveable { mutableStateOf(false) }

  var endDateText by rememberSaveable { mutableStateOf(initialEndDate) }
  var endTimeText by rememberSaveable { mutableStateOf(initialEndTime) }
  var endDateError by rememberSaveable { mutableStateOf(false) }
  var endTimeError by rememberSaveable { mutableStateOf(false) }

  fun validateDate(s: String): Boolean {
    return if (dateRegex.matches(s)) {
      try {
        dateFormat.parse("${currentYear}-$s")
        true
      } catch (_: ParseException) {
        false
      }
    } else {
      false
    }
  }

  fun validateTime(s: String): Boolean {
    return if (timeRegex.matches(s)) {
      try {
        timeFormat.parse(s)
        true
      } catch (_: ParseException) {
        false
      }
    } else {
      false
    }
  }

  val context = LocalContext.current
  Dialog(
    title = stringResource(R.string.select_date_and_time),
    primaryButton = DialogButton(
      text = stringResource(R.string.done),
      enabled = (startDateText.isNotEmpty() || endDateText.isNotEmpty() ||
        startTimeText.isNotEmpty() || endTimeText.isNotEmpty()) &&
        !startDateError && !startTimeError &&
        !endDateError && !endTimeError,
      onClick = {
        val cal = Calendar.getInstance()
        // `MONTH` starts at 0.
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val dateTimeFormat = SimpleDateFormat(DATE_TIME_FORMAT).apply { isLenient = false }
        try {
          val start = when {
            startDateText.isNotEmpty() && startTimeText.isNotEmpty() -> {
              dateTimeFormat.parse("$currentYear-$startDateText $startTimeText")
            }
            startDateText.isNotEmpty() -> {
              dateFormat.parse("$currentYear-$startDateText 00:00")
            }
            startTimeText.isNotEmpty() -> {
              dateTimeFormat.parse("$currentYear-$currentMonth-$currentDay $startTimeText")
            }
            else -> null
          }

          val end = when {
            endDateText.isNotEmpty() && endTimeText.isNotEmpty() -> {
              dateTimeFormat.parse("$currentYear-$endDateText $endTimeText")
            }
            endDateText.isNotEmpty() -> {
              dateFormat.parse("$currentYear-$endDateText 00:00")
            }
            endTimeText.isNotEmpty() -> {
              dateTimeFormat.parse("$currentYear-$currentMonth-$currentDay $endTimeText")
            }
            else -> null
          }
          onDone(DateRange(start = start, end = end))
        } catch (_: ParseException) {
          context.showToast(context.getString(R.string.error))
        }
      }
    ),
    secondaryButton = DialogButton(
      text = stringResource(android.R.string.cancel),
      onClick = onDismiss,
    ),
    modifier = modifier,
    onDismissRequest = onDismiss,
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      InputField(
        modifier = Modifier.weight(1f),
        label = stringResource(R.string.start_date),
        hint = DATE_FORMAT_NO_YEAR,
        value = startDateText,
        isError = startDateText.isNotEmpty() && startDateError,
        onValueChange = { value ->
          startDateText = value
          startDateError = value.isNotEmpty() && !validateDate(value)
        },
      )

      InputField(
        modifier = Modifier.weight(1f),
        label = stringResource(R.string.start_time),
        hint = TIME_FORMAT,
        value = startTimeText,
        isError = startTimeText.isNotEmpty() && startTimeError,
        onValueChange = { value ->
          startTimeText = value
          startTimeError = value.isNotEmpty() && !validateTime(value)
        },
      )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      InputField(
        modifier = Modifier.weight(1f),
        label = stringResource(R.string.end_date),
        hint = DATE_FORMAT_NO_YEAR,
        value = endDateText,
        isError = endDateText.isNotEmpty() && endDateError,
        onValueChange = { value ->
          endDateText = value
          endDateError = value.isNotEmpty() && !validateDate(value)
        },
      )

      InputField(
        modifier = Modifier.weight(1f),
        label = stringResource(R.string.end_time),
        hint = TIME_FORMAT,
        value = endTimeText,
        isError = endTimeText.isNotEmpty() && endTimeError,
        onValueChange = { value ->
          endTimeText = value
          endTimeError = value.isNotEmpty() && !validateTime(value)
        },
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PackageSelectorSheet(
  installedApps: List<AppInfo>,
  onDismiss: () -> Unit,
  onSelected: (Set<String>) -> Unit,
  modifier: Modifier = Modifier,
  savingInProgress: Boolean = false,
  viewModel: FiltersScreenViewModel = viewModel(),
) {
  ModalBottomSheet(
    modifier = modifier.statusBarsPadding(),
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filteredApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    LaunchedEffect(installedApps, viewModel) {
      snapshotFlow { searchQuery }
        .collect { query ->
          if (query.isEmpty()) {
            filteredApps = installedApps
              .sortedBy { it.packageName }
              .sortedBy { it.name }
          } else {
            filteredApps = installedApps
              .filter { info ->
                info.packageName.startsWith(query, ignoreCase = true) ||
                  info.name.startsWith(query, ignoreCase = true)
              }
              .sortedBy { it.packageName }
              .sortedBy { it.name }
          }
        }
    }

    Row(
      modifier = Modifier.padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        modifier = Modifier.weight(1f),
        text = stringResource(R.string.select_apps_to_filter),
        style = AppTypography.headlineMedium,
      )
      Button(
        onClick = {
          onSelected(viewModel.selectedPackageNames)
        },
        enabled = viewModel.selectedPackageNames.isNotEmpty() && !savingInProgress,
      ) {
        if (savingInProgress) {
          CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
          )
        } else {
          Text(
            stringResource(R.string.done),
            style = AppTypography.titleMedium,
          )
        }
      }
    }

    TextField(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      placeholder = { Text(stringResource(R.string.search)) },
      value = searchQuery,
      onValueChange = { searchQuery = it },
      singleLine = true,
      colors = TextFieldDefaults.colors(
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
      ),
      shape = RoundedCornerShape(corner = CornerSize(8.dp)),
    )

    LazyColumn {
      items(
        items = filteredApps,
        key = { it.packageName },
      ) { app ->
        ListItem(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              if (app.packageName in viewModel.selectedPackageNames) {
                viewModel.selectedPackageNames -= app.packageName
              } else {
                viewModel.selectedPackageNames += app.packageName
              }
            },
          overlineContent = if (app.isSystem) {
            {
              Text(stringResource(R.string.system_app))
            }
          } else null,
          leadingContent = {
            AsyncImage(
              model = app.icon,
              modifier = Modifier.size(32.dp),
              contentDescription = null,
            )
          },
          headlineContent = {
            Text(app.name)
          },
          supportingContent = if (app.name != app.packageName) {
            {
              Text(app.packageName)
            }
          } else null,
          trailingContent = {
            Checkbox(
              checked = app.packageName in viewModel.selectedPackageNames,
              onCheckedChange = null,
            )
          },
          colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
          )
        )
      }
    }
  }
}

@Composable
private fun FilterItem(
  modifier: Modifier,
  tag: String?,
  message: String?,
  packageName: String?,
  pid: String?,
  tid: String?,
  priorities: Set<LogLevel>?,
  regexEnabledFilterType: Set<RegexEnabledFilterType>,
  dateRange: DateRange?,
  exclude: Boolean,
  enabled: Boolean,
  selectable: Boolean = false,
  selected: Boolean = false,
) {
  val textStyle = LocalTextStyle.current.let { style ->
    style.copy(
      color = if (enabled) {
        style.color
      } else {
        ListItemDefaults.colors().disabledHeadlineColor
      },
    )
  }
  CompositionLocalProvider(LocalTextStyle provides textStyle) {
    ListItem(
      modifier = modifier,
      headlineContent = {
        Column {
          @Composable
          fun FilterRow(
            label: String,
            value: String,
            quote: Boolean = false,
            regex: Boolean = false,
          ) {
            val textStyle = LocalTextStyle.current
            Row {
              Text(
                text = if (regex) "$label (.*):" else "$label:",
                fontWeight = FontWeight.Bold,
                style = textStyle.merge(AppTypography.bodySmall),
                modifier = Modifier.alignByBaseline(),
              )
              Spacer(modifier = Modifier.width(4.dp))
              val maybeQuoted = if (quote) {
                "'$value'"
              } else value
              Text(
                text = maybeQuoted,
                style = textStyle.merge(AppTypography.bodyMedium),
                modifier = Modifier.alignByBaseline(),
              )
            }
          }
          if (!tag.isNullOrEmpty()) {
            FilterRow(
              label = stringResource(R.string.tag),
              value = tag,
              quote = true,
              regex = RegexEnabledFilterType.Tag in regexEnabledFilterType,
            )
          }
          if (!message.isNullOrEmpty()) {
            FilterRow(
              label = stringResource(R.string.message),
              value = message,
              quote = true,
              regex = RegexEnabledFilterType.Message in regexEnabledFilterType,
            )
          }
          if (!packageName.isNullOrEmpty()) {
            FilterRow(
              label = stringResource(R.string.package_name),
              value = packageName,
              quote = true,
              regex = RegexEnabledFilterType.PackageName in regexEnabledFilterType,
            )
          }
          if (dateRange != null) {
            val dateTimeFormatNoYear = SimpleDateFormat(DATE_TIME_FORMAT_NO_YEAR)
            FilterRow(
              label = "Date/time range",
              value = buildString {
                if (dateRange.start != null) {
                  append(dateTimeFormatNoYear.format(dateRange.start))
                } else {
                  append("n/a")
                }
                append(" — ")
                if (dateRange.end != null) {
                  append(dateTimeFormatNoYear.format(dateRange.end))
                } else {
                  append("n/a")
                }
              }
            )
          }
          if (!pid.isNullOrEmpty()) {
            FilterRow(
              label = stringResource(R.string.process_id),
              value = pid,
            )
          }
          if (!tid.isNullOrEmpty()) {
            FilterRow(
              label = stringResource(R.string.thread_id),
              value = tid,
            )
          }
          if (!priorities.isNullOrEmpty()) {
            FilterRow(
              label = stringResource(R.string.log_priority),
              value = priorities.map { it.label }.sorted()
                .fastJoinToString(separator = ", "),
            )
          }
        }
      },
      overlineContent = if (exclude) {
        {
          Text(stringResource(R.string.exclude))
        }
      } else null,
      trailingContent = if (selectable) {
        {
          Checkbox(
            checked = selected,
            onCheckedChange = null,
          )
        }
      } else null,
    )
  }
}

@Composable
private fun DateInputField(
  modifier: Modifier,
  hint: String,
  value: AnnotatedString,
  onClickSelectDateRange: () -> Unit,
) {
  TextField(
    modifier = modifier.focusable(false),
    value = TextFieldValue(value),
    onValueChange = {},
    placeholder = { Text(text = hint) },
    readOnly = true,
    colors = TextFieldDefaults.colors(
      focusedIndicatorColor = Color.Transparent,
      unfocusedIndicatorColor = Color.Transparent,
      errorIndicatorColor = Color.Transparent,
      errorTextColor = MaterialTheme.colorScheme.error,
    ),
    shape = Shapes.medium,
    trailingIcon = {
      WithTooltip(
        text = stringResource(R.string.select_date_and_time),
      ) {
        IconButton(
          onClick = onClickSelectDateRange,
        ) {
          Icon(Icons.Default.DateRange, contentDescription = null)
        }
      }
    },
  )
}

@Composable
private fun InputField(
  modifier: Modifier,
  label: String,
  value: String,
  maxLines: Int = Int.MAX_VALUE,
  onValueChange: (String) -> Unit,
  regexEnabled: Boolean = false,
  onClickRegex: (() -> Unit)? = null,
  keyboardType: KeyboardType = KeyboardOptions.Default.keyboardType,
  isError: Boolean = false,
  hint: String? = null,
) {
  TextField(
    modifier = modifier,
    label = { Text(label) },
    value = value,
    onValueChange = onValueChange,
    placeholder = hint?.let {
      {
        Text(
          text = it,
          style = LocalTextStyle.current.merge(AppTypography.bodySmall),
        )
      }
    },
    maxLines = maxLines,
    singleLine = maxLines == 1,
    isError = isError,
    colors = TextFieldDefaults.colors(
      focusedIndicatorColor = Color.Transparent,
      unfocusedIndicatorColor = Color.Transparent,
      errorIndicatorColor = Color.Transparent,
      errorTextColor = MaterialTheme.colorScheme.error,
    ),
    shape = Shapes.medium,
    keyboardOptions = KeyboardOptions.Default.copy(
      keyboardType = keyboardType,
    ),
    trailingIcon = if (onClickRegex != null) {
      {
        val textButtonColors = ButtonDefaults.textButtonColors()
        WithTooltip(
          text = stringResource(R.string.regex)
        ) {
          TextButton(
            onClick = onClickRegex,
            colors = ButtonDefaults.textButtonColors(
              contentColor = if (regexEnabled) {
                textButtonColors.contentColor
              } else {
                textButtonColors.disabledContentColor
              },
            ),
          ) {
            Text(".*")
          }
        }
      }
    } else null,
  )
}

data class PrepopulateFilterInfo(
  val log: Log,
  val packageName: String?,
  val exclude: Boolean,
)

class FiltersScreenViewModel : ViewModel() {
  var selectedFilters by mutableStateOf<Set<FilterInfo>>(emptySet())
  var showEditFilterDialog by mutableStateOf<FilterInfo?>(null)
  var selectedPackageNames by mutableStateOf<Set<String>>(emptySet())
}
