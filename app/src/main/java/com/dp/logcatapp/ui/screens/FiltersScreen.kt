package com.dp.logcatapp.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import androidx.core.text.isDigitsOnly
import coil3.compose.AsyncImage
import com.dp.logcat.Log
import com.dp.logcat.LogcatSession
import com.dp.logcatapp.R
import com.dp.logcatapp.db.FilterInfo
import com.dp.logcatapp.db.LogcatReaderDatabase
import com.dp.logcatapp.ui.theme.AppTypography
import com.dp.logcatapp.util.AppInfo
import com.dp.logcatapp.util.findActivity
import com.dp.logcatapp.util.rememberAppInfoByUidMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FiltersScreen(
  modifier: Modifier,
  prepopulateFilterInfo: PrepopulateFilterInfo?,
) {
  val context = LocalContext.current
  val db = remember(context) { LogcatReaderDatabase.getInstance(context) }

  val filters by db.filterDao()
    .filters()
    .collectAsState(null)

  var showAddFilterDialog by remember { mutableStateOf(prepopulateFilterInfo != null) }
  var showEditFilterDialog by remember { mutableStateOf<FilterInfo?>(null) }
  var showPackageSelector by remember { mutableStateOf(false) }
  val coroutineScope = rememberCoroutineScope()

  Scaffold(
    modifier = modifier,
    topBar = {
      var showDropDownMenu by remember { mutableStateOf(false) }
      TopAppBar(
        navigationIcon = {
          val insetPadding = WindowInsets.displayCutout
            .only(WindowInsetsSides.Left)
            .asPaddingValues()
          IconButton(
            modifier = Modifier.padding(insetPadding),
            onClick = {
              context.findActivity()?.finish()
            },
            colors = IconButtonDefaults.iconButtonColors(
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
          ) {
            Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
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
            .only(WindowInsetsSides.Right)
            .asPaddingValues()
          Row(
            modifier = Modifier.padding(insetPadding)
          ) {
            IconButton(
              onClick = { showPackageSelector = true },
              colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
            ) {
              Icon(Icons.Default.Apps, contentDescription = null)
            }
            IconButton(
              onClick = { showDropDownMenu = true },
              colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
            ) {
              Icon(Icons.Default.MoreVert, contentDescription = null)
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
    },
    floatingActionButton = {
      FloatingActionButton(
        modifier = Modifier
          .safeDrawingPadding()
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
    },
  ) { innerPadding ->
    val filters = filters

    showEditFilterDialog?.let { filterInfo ->
      AddFilterSheet(
        initialTag = filterInfo.tag,
        initialKeyword = filterInfo.message,
        initialPackageName = filterInfo.packageName,
        initialPid = filterInfo.pid?.toString(),
        initialTid = filterInfo.tid?.toString(),
        initialExclude = filterInfo.exclude,
        initialLogLevels = filterInfo.logLevels?.split(",")?.mapNotNull { level ->
          LogLevel.entries.find { it.name.first().toString() == level }
        }?.toSet().orEmpty(),
        onDismiss = { showEditFilterDialog = null },
        onSave = { data ->
          showEditFilterDialog = null

          val keyword = data.keyword
          val tag = data.tag
          val pid = data.pid
          val tid = data.tid
          val exclude = data.exclude
          val packageName = data.packageName
          val selectedLogLevels = data.selectedLogLevels

          val filterInfo = filterInfo.copy(
            tag = tag.takeIf { it.isNotEmpty() },
            message = keyword.takeIf { it.isNotEmpty() },
            pid = pid.takeIf { it.isNotEmpty() }?.toIntOrNull(),
            tid = tid.takeIf { it.isNotEmpty() }?.toIntOrNull(),
            packageName = packageName.takeIf { it.isNotEmpty() },
            logLevels = if (selectedLogLevels.isEmpty()) {
              null
            } else {
              selectedLogLevels.map { it.label.first().toString() }
                .sorted()
                .joinToString(separator = ",")
            },
            exclude = exclude,
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
      AddFilterSheet(
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

          val filterInfo = FilterInfo(
            tag = tag.takeIf { it.isNotEmpty() },
            message = keyword.takeIf { it.isNotEmpty() },
            pid = pid.takeIf { it.isNotEmpty() }?.toIntOrNull(),
            tid = tid.takeIf { it.isNotEmpty() }?.toIntOrNull(),
            packageName = packageName.takeIf { it.isNotEmpty() },
            logLevels = if (selectedLogLevels.isEmpty()) {
              null
            } else {
              selectedLogLevels.map { it.label.first().toString() }
                .sorted()
                .joinToString(separator = ",")
            },
            exclude = exclude,
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
      val currentPackageNameFilters = remember(filters) {
        filters.orEmpty().mapNotNull { it.packageName }.toSet()
      }
      PackageSelectorSheet(
        initialSelected = currentPackageNameFilters,
        onDismiss = {
          showPackageSelector = false
        },
        savingInProgress = savingInProgress,
        onSelected = { selected ->
          savingInProgress = true
          val filters = selected
            .filter { it !in currentPackageNameFilters }
            .map {
              FilterInfo(packageName = it)
            }
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
              .clickable {
                showEditFilterDialog = item
              }
              .safeDrawingPadding(),
            tag = item.tag,
            message = item.message,
            pid = item.pid?.toString(),
            tid = item.tid?.toString(),
            priorities = item.logLevels,
            exclude = item.exclude,
            packageName = item.packageName,
            onClickRemove = {
              coroutineScope.launch {
                val filterDao = db.filterDao()
                withContext(Dispatchers.IO) {
                  filterDao.delete(item)
                }
              }
            }
          )
          HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  }
}

private data class FilterData(
  val keyword: String,
  val tag: String,
  val pid: String,
  val tid: String,
  val packageName: String,
  val selectedLogLevels: Set<LogLevel>,
  val exclude: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddFilterSheet(
  onDismiss: () -> Unit,
  onSave: (FilterData) -> Unit,
  modifier: Modifier = Modifier,
  initialKeyword: String? = null,
  initialTag: String? = null,
  initialPackageName: String? = null,
  initialPid: String? = null,
  initialTid: String? = null,
  initialExclude: Boolean? = null,
  initialLogLevels: Set<LogLevel> = emptySet(),
) {
  val selectedLogLevels = remember {
    mutableStateMapOf<LogLevel, Boolean>().apply {
      LogLevel.entries.filter { it in initialLogLevels }.forEach { level ->
        put(level, true)
      }
    }
  }
  var keyword by remember { mutableStateOf(initialKeyword.orEmpty()) }
  var tag by remember { mutableStateOf(initialTag.orEmpty()) }
  var packageName by remember { mutableStateOf(initialPackageName.orEmpty()) }
  var pid by remember { mutableStateOf(initialPid.orEmpty()) }
  var tid by remember { mutableStateOf(initialTid.orEmpty()) }
  var exclude by remember { mutableStateOf(initialExclude ?: false) }
  ModalBottomSheet(
    modifier = modifier,
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
                keyword = keyword,
                tag = tag,
                pid = pid,
                tid = tid,
                packageName = packageName,
                selectedLogLevels = selectedLogLevels.filterValues { it }.keys.toSet(),
                exclude = exclude,
              )
            )
          },
          enabled = keyword.isNotEmpty() ||
            tag.isNotEmpty() ||
            pid.isNotEmpty() ||
            tid.isNotEmpty() ||
            packageName.isNotEmpty() ||
            selectedLogLevels.any { (_, selected) -> selected },
        ) {
          Text(
            stringResource(R.string.save),
            style = AppTypography.titleMedium,
          )
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
      InputField(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        label = stringResource(R.string.keyword),
        value = keyword,
        onValueChange = { keyword = it },
      )
      Spacer(modifier = Modifier.height(16.dp))
      InputField(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        label = stringResource(R.string.tag),
        maxLines = 1,
        value = tag,
        onValueChange = { tag = it },
      )
      Spacer(modifier = Modifier.height(16.dp))
      val uidSupported by LogcatSession.uidOptionSupported.collectAsState()
      if (uidSupported == true) {
        InputField(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          label = stringResource(R.string.package_name),
          value = packageName,
          onValueChange = { packageName = it },
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
              selectedLogLevels[logLevel] = !selectedLogLevels.getOrElse(logLevel) { false }
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
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PackageSelectorSheet(
  initialSelected: Set<String>,
  onDismiss: () -> Unit,
  onSelected: (Set<String>) -> Unit,
  modifier: Modifier = Modifier,
  savingInProgress: Boolean = false,
) {
  var selected by remember { mutableStateOf<Set<String>>(initialSelected) }
  ModalBottomSheet(
    modifier = modifier.displayCutoutPadding(),
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    val appInfoMap by rememberUpdatedState(rememberAppInfoByUidMap())
    var filtered by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
      snapshotFlow { appInfoMap }
        .filterNotNull()
        .map { info -> info.values }
        .collect { apps ->
          snapshotFlow { searchQuery }
            .collect { query ->
              if (query.isEmpty()) {
                filtered = apps
                  .sortedBy { it.packageName }
                  .sortedBy { it.name }
              } else {
                filtered = apps
                  .filter { info ->
                    info.packageName.startsWith(query, ignoreCase = true) ||
                      info.name?.startsWith(query, ignoreCase = true) == true
                  }
                  .sortedBy { it.packageName }
                  .sortedBy { it.name }
              }
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
          onSelected(selected)
        },
        enabled = selected.isNotEmpty() && selected != initialSelected && !savingInProgress,
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
        items = filtered,
        key = { it.packageName },
      ) { app ->
        ListItem(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              if (app.packageName in selected) {
                selected -= app.packageName
              } else {
                selected += app.packageName
              }
            },
          leadingContent = {
            AsyncImage(
              model = app.icon,
              modifier = Modifier.size(32.dp),
              contentDescription = null,
            )
          },
          headlineContent = {
            if (app.name != null) {
              Text(app.name)
            } else {
              Text(app.packageName)
            }
          },
          supportingContent = if (app.name != null) {
            {
              Text(app.packageName)
            }
          } else null,
          trailingContent = {
            Checkbox(
              checked = app.packageName in selected,
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
  priorities: String?,
  exclude: Boolean,
  onClickRemove: () -> Unit,
) {
  ListItem(
    modifier = modifier,
    headlineContent = {
      Column {
        @Composable
        fun FilterRow(
          label: String,
          value: String,
          quote: Boolean = false,
        ) {
          Row {
            Text(
              text = "$label:",
              fontWeight = FontWeight.Bold,
              style = AppTypography.bodySmall,
              modifier = Modifier.alignByBaseline(),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (quote) {
                "'$value'"
              } else value,
              style = AppTypography.bodyMedium,
              modifier = Modifier.alignByBaseline(),
            )
          }
        }
        if (!tag.isNullOrEmpty()) {
          FilterRow(
            label = stringResource(R.string.tag),
            value = tag,
            quote = true,
          )
        }
        if (!message.isNullOrEmpty()) {
          FilterRow(
            label = stringResource(R.string.message),
            value = message,
            quote = true,
          )
        }
        if (!packageName.isNullOrEmpty()) {
          FilterRow(
            label = stringResource(R.string.package_name),
            value = packageName,
            quote = true,
          )
        }
        val priorityMap = remember {
          LogLevel.entries.associate {
            it.label.first().lowercase().toString() to it.label
          }
        }
        if (!priorities.isNullOrEmpty()) {
          FilterRow(
            label = stringResource(R.string.log_priority),
            value = priorities.split(",").map {
              priorityMap.getValue(it.lowercase())
            }.fastJoinToString(separator = ", "),
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
      }
    },
    overlineContent = if (exclude) {
      {
        Text(stringResource(R.string.exclude))
      }
    } else null,
    trailingContent = {
      IconButton(
        onClick = onClickRemove,
        colors = IconButtonDefaults.iconButtonColors(
          containerColor = Color.Transparent,
          contentColor = MaterialTheme.colorScheme.onSurface,
        )
      ) {
        Icon(imageVector = Icons.Default.Clear, contentDescription = null)
      }
    }
  )
}

@Composable
private fun InputField(
  modifier: Modifier,
  label: String,
  value: String,
  maxLines: Int = Int.MAX_VALUE,
  onValueChange: (String) -> Unit,
  keyboardType: KeyboardType = KeyboardOptions.Default.keyboardType,
) {
  TextField(
    modifier = modifier,
    label = { Text(label) },
    value = value,
    onValueChange = onValueChange,
    maxLines = maxLines,
    singleLine = maxLines == 1,
    colors = TextFieldDefaults.colors(
      focusedIndicatorColor = Color.Transparent,
      unfocusedIndicatorColor = Color.Transparent,
    ),
    shape = RoundedCornerShape(corner = CornerSize(8.dp)),
    keyboardOptions = KeyboardOptions.Default.copy(
      keyboardType = keyboardType,
    ),
  )
}

data class PrepopulateFilterInfo(
  val log: Log,
  val packageName: String?,
  val exclude: Boolean,
)

private enum class LogLevel(
  val label: String
) {
  Assert("Assert"),
  Debug("Debug"),
  Error("Error"),
  Fatal("Fatal"),
  Info("Info"),
  Verbose("Verbose"),
  Warning("Warning"),
}
