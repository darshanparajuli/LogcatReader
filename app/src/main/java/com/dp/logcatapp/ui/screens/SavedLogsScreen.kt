package com.dp.logcatapp.ui.screens

import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.dp.logcat.LogcatStreamReader
import com.dp.logcat.LogcatUtil.countLogs
import com.dp.logcatapp.R
import com.dp.logcatapp.activities.SavedLogsViewerActivity
import com.dp.logcatapp.db.LogcatReaderDatabase
import com.dp.logcatapp.db.SavedLogInfo
import com.dp.logcatapp.ui.common.Dialog
import com.dp.logcatapp.ui.common.LOGCAT_DIR
import com.dp.logcatapp.ui.theme.AppTypography
import com.dp.logcatapp.ui.theme.Shapes
import com.dp.logcatapp.util.ShareUtils
import com.dp.logcatapp.util.Utils
import com.dp.logcatapp.util.closeQuietly
import com.dp.logcatapp.util.findActivity
import com.dp.logcatapp.util.rememberIntSharedPreference
import com.dp.logcatapp.util.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter

private const val SORT_BY_KEY = "sort_by_pref"
private val SORT_BY_DEFAULT = SortBy.Timestamp.ordinal
private const val SORT_ORDER_KEY = "sort_order_pref"
private val SORT_ORDER_DEFAULT = SortOrder.Dsc.ordinal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SavedLogsScreen(
  modifier: Modifier,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  val sortByPref = rememberIntSharedPreference(
    key = SORT_BY_KEY,
    default = SORT_BY_DEFAULT,
  )
  val sortOrderPref = rememberIntSharedPreference(
    key = SORT_ORDER_KEY,
    default = SORT_ORDER_DEFAULT,
  )

  var savedLogs by remember { mutableStateOf<SavedLogsResult?>(null) }
  val db = remember(context) { LogcatReaderDatabase.getInstance(context) }
  LaunchedEffect(context, db) {
    updateDbWithExistingInternalLogFiles(context, db)
    savedLogs(context, db).collect { result ->
      snapshotFlow {
        Pair(SortBy.entries[sortByPref.value], SortOrder.entries[sortOrderPref.value])
      }.collect { (sortBy, sortOrder) ->
        savedLogs = result.copy(
          logFiles = result.logFiles.sortedWith(SortByComparator(sortBy, sortOrder))
        )
      }
    }
  }

  var selected by remember { mutableStateOf<Set<LogFileInfo>>(emptySet()) }
  var renameLog by remember { mutableStateOf<LogFileInfo?>(null) }
  var exportLog by remember { mutableStateOf<LogFileInfo?>(null) }
  var showSortSheet by remember { mutableStateOf(false) }

  if (selected.isNotEmpty()) {
    BackHandler {
      selected = emptySet()
    }
  }

  Scaffold(
    modifier = modifier,
    topBar = {
      // For smart-casting.
      val savedLogs = savedLogs
      AppBar(
        title = stringResource(R.string.saved_logs),
        subtitle = if (savedLogs != null) {
          if (savedLogs.logFiles.isNotEmpty()) {
            if (savedLogs.totalSize.isEmpty()) {
              savedLogs.logFiles.size.toString()
            } else {
              val totalLogCountFmt = if (savedLogs.totalLogCount == 1L) {
                stringResource(R.string.log_count_fmt)
              } else {
                stringResource(R.string.log_count_fmt_plural)
              }
              val totalLogCountStr = totalLogCountFmt.format(savedLogs.totalLogCount)
              "${savedLogs.logFiles.size} ($totalLogCountStr, ${savedLogs.totalSize})"
            }
          } else null
        } else null,
        sortEnabled = savedLogs?.logFiles?.isNotEmpty() == true,
        onClickSort = { showSortSheet = true },
      )
      AnimatedVisibility(
        visible = selected.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        SelectLogsAppBar(
          title = selected.size.toString(),
          singleLogSelected = selected.size == 1,
          onClickClose = {
            selected = emptySet()
          },
          onClickRename = {
            renameLog = selected.first()
          },
          onClickExport = {
            exportLog = selected.first()
          },
          onClickShare = {
            val fileInfo = requireNotNull(savedLogs).logFiles.first()
            ShareUtils.shareSavedLogs(
              context = context,
              uri = fileInfo.info.path.toUri(),
              isCustom = fileInfo.info.isCustom,
            )
          },
          onClickSelectAll = {
            selected = savedLogs?.logFiles?.toSet().orEmpty()
          },
          onClickDelete = {
            val deleteList = selected.toList()
            selected = emptySet()
            coroutineScope.launch {
              deleteLogs(logs = deleteList, db = db, context = context)
            }
          },
        )
      }
    },
  ) { innerPadding ->
    val savedLogs = savedLogs
    val logFormat = stringResource(R.string.log_count_fmt)
    val logsFormat = stringResource(R.string.log_count_fmt_plural)

    renameLog?.let { logFileInfo ->
      RenameLogDialog(
        renameLog = logFileInfo,
        onDismiss = { renameLog = null },
        onConfirm = { newName ->
          renameLog = null
          selected = emptySet()
          coroutineScope.launch {
            val success = rename(
              newName = newName,
              fileInfo = logFileInfo.info,
              db = db,
            )
            if (!success) {
              context.showToast(context.getString(R.string.error))
            }
          }
        }
      )
    }

    exportLog?.let { logFileInfo ->
      var useSingleLineExportFormat by remember { mutableStateOf(false) }
      var savingInProgress by remember { mutableStateOf(false) }
      val exportLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument(mimeType = "text/plain")
      ) { result ->
        if (result != null) {
          val fileInfo = exportLog ?: return@rememberLauncherForActivityResult
          coroutineScope.launch {
            try {
              val src = context.contentResolver.openInputStream(fileInfo.info.path.toUri())
              val dest = context.contentResolver.openOutputStream(result)
              if (saveLogs(src!!, dest!!, useSingleLineExportFormat)) {
                context.showToast(context.getString(R.string.saved))
              } else {
                context.showToast(context.getString(R.string.error_saving))
              }
            } catch (_: IOException) {
              context.showToast(context.getString(R.string.error_saving))
            } finally {
              exportLog = null
            }
          }
        } else {
          exportLog = null
        }
      }

      ExportBottomSheet(
        savingInProgress = savingInProgress,
        onDismiss = { exportLog = null },
        onClickSingle = {
          useSingleLineExportFormat = true
          savingInProgress = true
          exportLauncher.launch(logFileInfo.info.fileName)
        },
        onClickDefault = {
          savingInProgress = true
          useSingleLineExportFormat = false
          exportLauncher.launch(logFileInfo.info.fileName)
        }
      )
    }

    if (showSortSheet) {
      SortOptionsSheet(
        initialSortBy = SortBy.entries[sortByPref.value],
        initialSortOrder = SortOrder.entries[sortOrderPref.value],
        onClickDone = { sortBy, sortOrder ->
          sortByPref.value = sortBy.ordinal
          sortOrderPref.value = sortOrder.ordinal
          showSortSheet = false
        },
        onDismiss = {
          showSortSheet = false
        }
      )
    }

    if (savedLogs != null) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .consumeWindowInsets(innerPadding),
        contentPadding = innerPadding,
      ) {
        items(
          items = savedLogs.logFiles,
          key = { item -> item.info.path },
        ) { item ->
          ListItem(
            modifier = Modifier
              .fillMaxWidth()
              .animateItem()
              .then(
                if (selected.isEmpty()) {
                  Modifier.combinedClickable(
                    onLongClick = {
                      selected += item
                    },
                    onClick = {
                      val intent = Intent(context, SavedLogsViewerActivity::class.java)
                      intent.setDataAndType(item.info.path.toUri(), "text/plain")
                      context.startActivity(intent)
                    }
                  )
                } else {
                  Modifier.clickable {
                    if (item in selected) {
                      selected -= item
                    } else {
                      selected += item
                    }
                  }
                }
              )
              .safeDrawingPadding(),
            headlineContent = {
              Text(item.info.fileName)
            },
            supportingContent = {
              val logsFormatToUse = if (item.count == 1L) {
                logFormat
              } else {
                logsFormat
              }
              Row {
                Text(logsFormatToUse.format(item.count))
                Spacer(modifier = Modifier.width(8.dp))
                Text(item.sizeStr)
                Spacer(modifier = Modifier.width(8.dp))
                DateTime(
                  timestamp = item.timestamp,
                  spaceInBetween = 4.dp,
                )
              }
            },
            trailingContent = if (selected.isNotEmpty()) {
              {
                Checkbox(
                  checked = item in selected,
                  onCheckedChange = null,
                )
              }
            } else null,
          )
          HorizontalDivider(modifier = Modifier.fillMaxWidth())
        }
      }
    } else {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .consumeWindowInsets(innerPadding)
          .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(48.dp),
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBar(
  title: String,
  subtitle: String?,
  sortEnabled: Boolean,
  onClickSort: () -> Unit,
) {
  val context = LocalContext.current
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
        Icon(
          imageVector = Icons.AutoMirrored.Default.ArrowBack,
          contentDescription = null,
        )
      }
    },
    title = {
      Column {
        Text(
          text = title,
          overflow = TextOverflow.Ellipsis,
          maxLines = 1,
        )
        if (subtitle != null) {
          Text(
            text = subtitle,
            style = AppTypography.titleSmall,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
          )
        }
      }
    },
    actions = {
      val insetPadding = WindowInsets.displayCutout
        .only(WindowInsetsSides.Right)
        .asPaddingValues()
      Row(
        modifier = Modifier.padding(insetPadding)
      ) {
        IconButton(
          onClick = onClickSort,
          enabled = sortEnabled,
          colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
        ) {
          Icon(Icons.AutoMirrored.Default.Sort, contentDescription = null)
        }
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer,
      titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ),
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectLogsAppBar(
  title: String,
  singleLogSelected: Boolean,
  onClickClose: () -> Unit,
  onClickRename: () -> Unit,
  onClickExport: () -> Unit,
  onClickShare: () -> Unit,
  onClickSelectAll: () -> Unit,
  onClickDelete: () -> Unit,
) {
  TopAppBar(
    navigationIcon = {
      val insetPadding = WindowInsets.displayCutout
        .only(WindowInsetsSides.Left)
        .asPaddingValues()
      IconButton(
        modifier = Modifier.padding(insetPadding),
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
    },
    title = {
      Text(
        text = title,
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
        AnimatedVisibility(
          visible = singleLogSelected,
          enter = fadeIn() + scaleIn(initialScale = 0.9f),
          exit = fadeOut() + scaleOut(targetScale = 0.9f),
        ) {
          Row {
            IconButton(
              onClick = onClickRename,
              colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
            ) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
              )
            }
            IconButton(
              onClick = onClickExport,
              colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
            ) {
              Icon(
                imageVector = Icons.Default.SaveAs,
                contentDescription = null,
              )
            }
            IconButton(
              onClick = onClickShare,
              colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
            ) {
              Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
              )
            }
          }
        }
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
        Box {
          var showDropDownMenu by remember { mutableStateOf(false) }
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

@Composable
private fun RenameLogDialog(
  renameLog: LogFileInfo,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  val fileInfo = renameLog.info
  var name by remember {
    mutableStateOf(
      TextFieldValue(
        text = fileInfo.fileName,
        selection = TextRange(
          start = 0,
          end = fileInfo.fileName.length,
        )
      )
    )
  }
  Dialog(
    onDismissRequest = onDismiss,
    title = {
      Text(stringResource(R.string.rename))
    },
    content = {
      TextField(
        modifier = Modifier.fillMaxWidth(),
        value = name,
        onValueChange = { name = it },
        colors = TextFieldDefaults.colors(
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(corner = CornerSize(8.dp)),
      )
    },
    confirmButton = {
      TextButton(
        onClick = {
          onConfirm(name.text)
        },
        enabled = name.text.isNotBlank(),
      ) {
        Text(stringResource(android.R.string.ok))
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
      ) {
        Text(stringResource(android.R.string.cancel))
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportBottomSheet(
  savingInProgress: Boolean,
  onDismiss: () -> Unit,
  onClickDefault: () -> Unit,
  onClickSingle: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    ListItem(
      modifier = Modifier.fillMaxWidth(),
      headlineContent = {
        Text(
          modifier = Modifier.weight(1f),
          text = stringResource(R.string.select_export_format),
          style = AppTypography.titleMedium,
        )
      },
      colors = ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
      ),
    )

    if (savingInProgress) {
      CircularProgressIndicator(
        modifier = Modifier
          .padding(16.dp)
          .size(48.dp)
          .align(Alignment.CenterHorizontally),
      )
    } else {
      ListItem(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            onClickDefault()
          },
        headlineContent = {
          Text(text = stringResource(R.string.export_format_default))
        },
        colors = ListItemDefaults.colors(
          containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
      )
      ListItem(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            onClickSingle()
          },
        headlineContent = {
          Text(text = stringResource(R.string.export_format_single_line))
        },
        colors = ListItemDefaults.colors(
          containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortOptionsSheet(
  initialSortBy: SortBy,
  initialSortOrder: SortOrder,
  onClickDone: (SortBy, SortOrder) -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Column(
      modifier = Modifier
        .verticalScroll(rememberScrollState())
        .padding(bottom = 16.dp)
    ) {

      var sortBy by remember { mutableStateOf(initialSortBy) }
      var sortOrder by remember { mutableStateOf(initialSortOrder) }

      Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          modifier = Modifier.weight(1f),
          text = stringResource(R.string.sort_by),
          style = AppTypography.headlineMedium,
        )
        FilledTonalButton(
          onClick = {
            onClickDone(sortBy, sortOrder)
          },
        ) {
          Text(
            stringResource(R.string.done),
            style = AppTypography.titleMedium,
          )
        }
      }

      SortBy.entries.fastForEach { entry ->
        ListItem(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              sortBy = entry
            },
          leadingContent = {
            RadioButton(
              selected = sortBy == entry,
              onClick = null,
            )
          },
          headlineContent = {
            Text(
              text = when (entry) {
                SortBy.Name -> stringResource(R.string.name)
                SortBy.Timestamp -> stringResource(R.string.timestamp)
                SortBy.LogCount -> stringResource(R.string.log_count)
                SortBy.Size -> stringResource(R.string.size)
              }
            )
          },
          colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
          ),
        )
      }

      SingleChoiceSegmentedButtonRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
      ) {
        SegmentedButton(
          selected = sortOrder == SortOrder.Asc,
          onClick = {
            sortOrder = SortOrder.Asc
          },
          shape = Shapes.medium.copy(topEnd = CornerSize(0), bottomEnd = CornerSize(0)),
        ) {
          Text(stringResource(R.string.ascending))
        }
        SegmentedButton(
          selected = sortOrder == SortOrder.Dsc,
          onClick = {
            sortOrder = SortOrder.Dsc
          },
          shape = Shapes.medium.copy(topStart = CornerSize(0), bottomStart = CornerSize(0)),
        ) {
          Text(stringResource(R.string.descending))
        }
      }
    }
  }
}

@Composable
private fun DateTime(
  timestamp: Long,
  spaceInBetween: Dp,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(spaceInBetween),
  ) {
    val date = if (Build.VERSION.SDK_INT >= 24) {
      remember(context, timestamp) {
        SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT)
          .format(timestamp)
      }
    } else {
      remember(context, timestamp) {
        DateFormat.getDateFormat(context).format(timestamp)
      }
    }
    val time = if (Build.VERSION.SDK_INT >= 24) {
      remember(context, timestamp) {
        SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT)
          .format(timestamp)
      }
    } else {
      remember(context, timestamp) {
        DateFormat.getTimeFormat(context).format(timestamp)
      }
    }
    Text(date)
    Text(time)
  }
}

private suspend fun deleteLogs(
  logs: List<LogFileInfo>,
  db: LogcatReaderDatabase,
  context: Context,
) {
  val dao = db.savedLogsDao()
  withContext(Dispatchers.IO) {
    val deleted = logs
      .filter {
        with(it.info.path.toUri()) {
          if (it.info.isCustom) {
            val file = DocumentFile.fromSingleUri(context, this)
            file != null && file.delete()
          } else {
            this.toFile().delete()
          }
        }
      }
      .map { it.info.fileName }
      .toSet()

    val deletedSavedLogInfoList = logs
      .map { it.info }
      .filter { it.fileName in deleted }
      .toTypedArray()
    dao.delete(*deletedSavedLogInfoList)
  }
}

private suspend fun savedLogs(context: Context, db: LogcatReaderDatabase): Flow<SavedLogsResult> {
  return db.savedLogsDao().savedLogs()
    .map { savedLogs ->
      val logFiles = coroutineScope {
        savedLogs.map { info ->
          async(Dispatchers.IO) {
            if (info.isCustom) {
              val file = DocumentFile.fromSingleUri(context, info.path.toUri())
              if (file == null || file.name == null) {
                return@async null
              }

              val size = file.length()
              val count = countLogs(context, file)

              LogFileInfo(
                info = info,
                size = size,
                sizeStr = Utils.bytesToString(size),
                count = count,
                timestamp = info.timestamp ?: file.lastModified(),
              )
            } else {
              val file = info.path.toUri().toFile()
              val size = file.length()
              val count = countLogs(file)

              LogFileInfo(
                info = info,
                size = size,
                sizeStr = Utils.bytesToString(size),
                count = count,
                timestamp = info.timestamp ?: file.lastModified(),
              )
            }
          }
        }.awaitAll().filterNotNull()
      }

      val totalLogCount = logFiles.foldRight(0L) { logFileInfo, acc ->
        acc + logFileInfo.count
      }

      val totalSize = logFiles.sumOf { it.size }
      if (totalSize > 0) {
        Utils.bytesToString(totalSize)
      }

      SavedLogsResult(
        totalSize = if (totalSize > 0) {
          Utils.bytesToString(totalSize)
        } else "",
        totalLogCount = totalLogCount,
        logFiles = logFiles,
      )
    }
}

private suspend fun updateDbWithExistingInternalLogFiles(
  context: Context,
  db: LogcatReaderDatabase
) {
  withContext(Dispatchers.IO) {
    val files = File(context.cacheDir, LOGCAT_DIR).listFiles()
    if (!files.isNullOrEmpty()) {
      db.savedLogsDao().insert(
        *files.map { file ->
          SavedLogInfo(
            fileName = file.name,
            path = file.absolutePath,
            isCustom = false,
            timestamp = file.lastModified(),
          )
        }.toTypedArray()
      )
    }
  }
}

private suspend fun rename(
  fileInfo: SavedLogInfo,
  newName: String,
  db: LogcatReaderDatabase,
): Boolean {
  val file = fileInfo.path.toUri().toFile()
  val newFile = File(file.parent, newName)
  val renameSuccessful = withContext(Dispatchers.IO) {
    file.renameTo(newFile)
  }
  if (renameSuccessful) {
    withContext(Dispatchers.IO) {
      db.withTransaction {
        val dao = db.savedLogsDao()
        dao.delete(fileInfo)
        dao.insert(
          fileInfo.copy(
            fileName = newName,
            path = newFile.toUri().toString(),
          )
        )
      }
    }
    return true
  } else {
    return false
  }
}

private suspend fun saveLogs(
  src: InputStream,
  dest: OutputStream,
  useSingleLine: Boolean,
): Boolean {
  return withContext(Dispatchers.IO) {
    try {
      if (useSingleLine) {
        val bufferedWriter = BufferedWriter(OutputStreamWriter(dest))
        LogcatStreamReader(src).use {
          for (log in it) {
            val metadata = log.metadataToString()
            val msgTokens = log.msg.split("\n")
            for (msg in msgTokens) {
              bufferedWriter.write(metadata)
              bufferedWriter.write(" ")
              bufferedWriter.write(msg)
              bufferedWriter.newLine()
            }
          }
        }
      } else {
        src.copyTo(dest)
      }
      true
    } catch (_: IOException) {
      false
    } finally {
      src.closeQuietly()
      dest.closeQuietly()
    }
  }
}

private class SortByComparator(
  private val sortBy: SortBy,
  private val sortOrder: SortOrder,
) : Comparator<LogFileInfo> {
  override fun compare(
    o1: LogFileInfo,
    o2: LogFileInfo
  ): Int {
    val result = when (sortBy) {
      SortBy.Name -> o1.info.fileName.compareTo(o2.info.fileName)
      SortBy.Timestamp -> o1.timestamp.compareTo(o2.timestamp)
      SortBy.LogCount -> o1.count.compareTo(o2.count)
      SortBy.Size -> o1.size.compareTo(o2.size)
    }

    return when (sortOrder) {
      SortOrder.Asc -> result
      SortOrder.Dsc -> -result
    }
  }
}

private enum class SortOrder {
  Asc,
  Dsc,
}

private enum class SortBy {
  Name,
  Timestamp,
  LogCount,
  Size,
}

data class LogFileInfo(
  val info: SavedLogInfo,
  val size: Long,
  val sizeStr: String,
  val count: Long,
  val timestamp: Long,
)

data class SavedLogsResult(
  val totalSize: String,
  val totalLogCount: Long,
  val logFiles: List<LogFileInfo>,
)
