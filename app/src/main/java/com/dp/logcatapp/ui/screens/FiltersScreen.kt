package com.dp.logcatapp.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dp.logcat.Log
import com.dp.logcat.LogPriority
import com.dp.logcatapp.R
import com.dp.logcatapp.db.FilterInfo
import com.dp.logcatapp.db.LogcatReaderDatabase
import com.dp.logcatapp.model.FilterType
import com.dp.logcatapp.ui.theme.AppTypography
import com.dp.logcatapp.util.findActivity
import kotlinx.coroutines.Dispatchers
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
    .map { filters -> filters.map { filter -> filter.toFilterListItem(context) } }
    .collectAsState(null)

  var showAddFilterDialog by remember { mutableStateOf(prepopulateFilterInfo != null) }
  val coroutineScope = rememberCoroutineScope()

  Scaffold(
    modifier = modifier,
    topBar = {
      var showDropDownMenu by remember { mutableStateOf(false) }
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
                coroutineScope.launch {
                  val filterDao = db.filterDao()
                  withContext(Dispatchers.IO) {
                    filterDao.deleteAll()
                  }
                }
              },
            )
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
        modifier = Modifier.size(48.dp),
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

    if (showAddFilterDialog) {
      AddFilterSheet(
        prepopulateFilterInfo = prepopulateFilterInfo,
        onDismiss = { showAddFilterDialog = false },
        onSave = { data ->
          showAddFilterDialog = false

          val keyword = data.keyword
          val tag = data.tag
          val pid = data.pid
          val tid = data.tid
          val exclude = data.exclude
          val selectedLogLevels = data.selectedLogLevels

          val filters = mutableListOf<FilterInfo>()
          if (keyword.isNotEmpty()) {
            filters += FilterInfo(
              type = FilterType.KEYWORD,
              content = keyword,
              exclude = exclude,
            )
          }
          if (tag.isNotEmpty()) {
            filters += FilterInfo(
              type = FilterType.TAG,
              content = tag,
              exclude = exclude,
            )
          }
          if (pid.isNotEmpty()) {
            filters += FilterInfo(
              type = FilterType.PID,
              content = pid,
              exclude = exclude,
            )
          }
          if (tid.isNotEmpty()) {
            filters += FilterInfo(
              type = FilterType.TID,
              content = tid,
              exclude = exclude,
            )
          }
          selectedLogLevels.forEach {
            filters += FilterInfo(
              type = FilterType.LOG_LEVELS,
              content = selectedLogLevels.map { it.label.first().toString() }
                .sorted()
                .joinToString(separator = ","),
              exclude = exclude,
            )
          }

          coroutineScope.launch {
            withContext(Dispatchers.IO) {
              db.filterDao().insert(*filters.toTypedArray())
            }
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
            modifier = Modifier.fillMaxWidth(),
            type = item.type,
            text = item.text,
            exclude = item.exclude,
            onClickRemove = {
              val filterInfo = item.filterInfo
              coroutineScope.launch {
                val filterDao = db.filterDao()
                withContext(Dispatchers.IO) {
                  filterDao.delete(filterInfo)
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
  val selectedLogLevels: Set<LogLevel>,
  val exclude: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddFilterSheet(
  prepopulateFilterInfo: PrepopulateFilterInfo?,
  onDismiss: () -> Unit,
  onSave: (FilterData) -> Unit,
  modifier: Modifier = Modifier,
) {
  val selectedLogLevels = remember {
    mutableStateMapOf<LogLevel, Boolean>().apply {
      prepopulateFilterInfo?.log?.priority?.let { p ->
        LogLevel.entries.find { it.label.startsWith(p) }?.let { level ->
          put(level, true)
        }
      }
    }
  }
  var keyword by remember { mutableStateOf("") }
  var tag by remember { mutableStateOf(prepopulateFilterInfo?.log?.tag.orEmpty()) }
  var pid by remember { mutableStateOf(prepopulateFilterInfo?.log?.pid.orEmpty()) }
  var tid by remember { mutableStateOf(prepopulateFilterInfo?.log?.tid.orEmpty()) }
  var exclude by remember { mutableStateOf(prepopulateFilterInfo?.exclude ?: false) }
  ModalBottomSheet(
    modifier = modifier,
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          modifier = Modifier.weight(1f),
          text = stringResource(R.string.filter),
          style = AppTypography.headlineMedium,
        )
        FilledTonalButton(
          onClick = {
            onSave(
              FilterData(
                keyword = keyword,
                tag = tag,
                pid = pid,
                tid = tid,
                selectedLogLevels = selectedLogLevels.filterValues { it }.keys.toSet(),
                exclude = exclude,
              )
            )
          },
          enabled = keyword.isNotEmpty() ||
            tag.isNotEmpty() ||
            pid.isNotEmpty() ||
            tid.isNotEmpty() ||
            selectedLogLevels.any { (_, selected) -> selected },
        ) {
          Text(
            stringResource(R.string.save),
            style = AppTypography.titleMedium,
          )
        }
      }
      InputField(
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(R.string.keyword),
        value = keyword,
        onValueChange = { keyword = it },
      )
      InputField(
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(R.string.tag),
        value = tag,
        onValueChange = { tag = it },
      )
      InputField(
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(R.string.process_id),
        value = pid,
        onValueChange = { pid = it },
      )
      InputField(
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(R.string.thread_id),
        value = tid,
        onValueChange = { tid = it },
      )
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
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
      Row(
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          modifier = Modifier.weight(1f),
          text = stringResource(R.string.exclude),
          style = AppTypography.bodyLarge,
        )
        Checkbox(
          checked = exclude,
          onCheckedChange = { exclude = it },
        )
      }
    }
  }
}

@Composable
private fun FilterItem(
  modifier: Modifier,
  type: String,
  text: String,
  exclude: Boolean,
  onClickRemove: () -> Unit,
) {
  ListItem(
    modifier = modifier,
    headlineContent = {
      Text(text)
    },
    overlineContent = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(type)
        if (exclude) {
          Text("(${stringResource(R.string.exclude).lowercase()})")
        }
      }
    },
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
  onValueChange: (String) -> Unit,
) {
  TextField(
    modifier = modifier,
    label = { Text(label) },
    value = value,
    onValueChange = onValueChange,
    colors = TextFieldDefaults.colors(
      focusedIndicatorColor = Color.Transparent,
      unfocusedIndicatorColor = Color.Transparent,
    ),
    shape = RoundedCornerShape(corner = CornerSize(8.dp)),
  )
}

private fun FilterInfo.toFilterListItem(
  context: Context,
): FilterListItem {
  val text: String
  val type: String
  when (this.type) {
    FilterType.LOG_LEVELS -> {
      type = context.getString(R.string.log_level)
      text = content.split(",")
        .joinToString(", ") { s ->
          when (s) {
            LogPriority.ASSERT -> "Assert"
            LogPriority.ERROR -> "Error"
            LogPriority.DEBUG -> "Debug"
            LogPriority.FATAL -> "Fatal"
            LogPriority.INFO -> "Info"
            LogPriority.VERBOSE -> "Verbose"
            LogPriority.WARNING -> "Warning"
            else -> ""
          }
        }
    }
    else -> {
      text = content
      type = when (this.type) {
        FilterType.KEYWORD -> context.getString(R.string.keyword)
        FilterType.TAG -> context.getString(R.string.tag)
        FilterType.PID -> context.getString(R.string.process_id)
        FilterType.TID -> context.getString(R.string.thread_id)
        else -> throw IllegalStateException("invalid type: ${this.type}")
      }
    }
  }
  return FilterListItem(
    type = type,
    text = text,
    exclude = exclude,
    filterInfo = this
  )
}

data class PrepopulateFilterInfo(
  val log: Log,
  val exclude: Boolean,
)

private data class FilterListItem(
  val type: String,
  val text: String,
  val exclude: Boolean,
  val filterInfo: FilterInfo,
)

private enum class LogLevel(val label: String) {
  Assert("Assert"),
  Debug("Debug"),
  Error("Error"),
  Fatal("Fatal"),
  Info("Info"),
  Verbose("Verbose"),
  Warning("Warning"),
}
