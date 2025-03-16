package com.dp.logcatapp.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import androidx.annotation.WorkerThread
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction.Press
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dp.logcat.Filter
import com.dp.logcat.Log
import com.dp.logcat.LogPriority
import com.dp.logcat.LogcatUtil
import com.dp.logcatapp.R
import com.dp.logcatapp.activities.FiltersActivity
import com.dp.logcatapp.activities.SavedLogsActivity
import com.dp.logcatapp.activities.SavedLogsViewerActivity
import com.dp.logcatapp.activities.SettingsActivity
import com.dp.logcatapp.db.FilterInfo
import com.dp.logcatapp.db.MyDB
import com.dp.logcatapp.db.SavedLogInfo
import com.dp.logcatapp.fragments.filters.FilterType
import com.dp.logcatapp.fragments.logcatlive.LogcatLiveFragment.Companion.LOGCAT_DIR
import com.dp.logcatapp.services.LogcatService
import com.dp.logcatapp.services.getService
import com.dp.logcatapp.ui.screens.SearchHitKey.LogComponent
import com.dp.logcatapp.ui.theme.AppTypography
import com.dp.logcatapp.ui.theme.LogPriorityColors
import com.dp.logcatapp.ui.theme.LogcatReaderTheme
import com.dp.logcatapp.ui.theme.RobotoMonoFontFamily
import com.dp.logcatapp.ui.theme.currentSearchHitColor
import com.dp.logcatapp.util.PreferenceKeys
import com.dp.logcatapp.util.ServiceBinder
import com.dp.logcatapp.util.ShareUtils
import com.dp.logcatapp.util.containsIgnoreCase
import com.dp.logcatapp.util.getDefaultSharedPreferences
import com.dp.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "HomeScreen"
private const val SNAP_SCROLL_HIDE_DELAY_MS = 2000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  modifier: Modifier,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current

  val logcatService = rememberLogcatServiceConnection()
  val lazyListState = rememberLazyListState()

  var snapToBottom by remember { mutableStateOf(true) }
  val snapUpInteractionSource = remember { MutableInteractionSource() }
  val snapDownInteractionSource = remember { MutableInteractionSource() }
  val snapScrollInfo = rememberSnapScrollInfo(
    lazyListState = lazyListState,
    snapToBottom = snapToBottom,
    snapUpInteractionSource = snapUpInteractionSource,
    snapDownInteractionSource = snapDownInteractionSource,
  )

  val logsState = remember { mutableStateListOf<Log>() }
  var searchInProgress by remember { mutableStateOf(false) }
  var showDropDownMenu by remember { mutableStateOf(false) }
  var showSearchBar by remember { mutableStateOf(false) }
  var logcatPaused by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  val searchHitsMap = remember { mutableStateMapOf<SearchHitKey, Pair<Int, Int>>() }
  var sortedHitsByLogIdsState by remember { mutableStateOf<List<Int>>(emptyList()) }
  var currentSearchHitIndex by remember { mutableIntStateOf(-1) }
  var currentSearchHitLogId by remember { mutableIntStateOf(-1) }
  var showHitCount by remember { mutableStateOf(false) }
  var recordStatus by remember { mutableStateOf<RecordStatus>(RecordStatus.Idle) }
  val snackbarHostState = remember { SnackbarHostState() }
  var savedLogsSheetState by remember {
    mutableStateOf<SavedLogsBottomSheetState>(SavedLogsBottomSheetState.Hide)
  }

  val restartTrigger = remember { Channel<Boolean>(capacity = 1) }
  if (logcatService != null) {
    val logcatSession = logcatService.logcatSession
    val db = remember(context) { MyDB.getInstance(context) }
    LaunchedEffect(logcatSession) {
      restartTrigger.consumeAsFlow()
        .onStart { emit(false) }
        .collectLatest { restart ->
          if (restart) {
            withContext(Dispatchers.Default) {
              logcatSession.restart()
            }
          }
          if (logcatSession.isRecording) {
            recordStatus = RecordStatus.RecordingInProgress
          }
          db.filterDao().filters()
            .collectLatest { filters ->
              logcatSession.setFilters(
                filters = filters.filterNot { it.exclude }.map(::LogFilter),
                exclusion = false
              )
              logcatSession.setFilters(
                filters = filters.filter { it.exclude }.map(::LogFilter),
                exclusion = true
              )
              logsState.clear()
              logcatSession.logs.collect { logs ->
                if (logcatPaused) {
                  snapshotFlow { logcatPaused }.first { !it }
                }
                logsState += logs
              }
            }
        }
    }
  }

  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = stringResource(R.string.app_name),
            )
            Text(
              text = logsState.size.toString(),
              style = AppTypography.titleSmall,
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        actions = {
          IconButton(
            onClick = {
              showSearchBar = true
            },
            colors = IconButtonDefaults.iconButtonColors(
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
          ) {
            Icon(Icons.Default.Search, contentDescription = null)
          }
          IconButton(
            onClick = {
              logcatPaused = !logcatPaused
            },
            enabled = recordStatus is RecordStatus.Idle,
            colors = IconButtonDefaults.iconButtonColors(
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
          ) {
            if (logcatPaused) {
              Icon(Icons.Default.PlayArrow, contentDescription = null)
            } else {
              Icon(Icons.Default.Pause, contentDescription = null)
            }
          }

          val startedRecordingMessage = stringResource(R.string.started_recording)
          val saveFailedMessage = stringResource(R.string.failed_to_save_logs)
          val noNewLogsMessage = stringResource(R.string.no_new_logs)
          LaunchedEffect(Unit) {
            var lastShownSnackBar: Job? = null
            snapshotFlow { recordStatus }
              .collect { status ->
                when (status) {
                  RecordStatus.Idle -> Unit
                  RecordStatus.RecordingInProgress -> {
                    lastShownSnackBar?.cancel()
                    lastShownSnackBar = launch {
                      snackbarHostState.showSnackbar(
                        message = startedRecordingMessage,
                        withDismissAction = true,
                        duration = SnackbarDuration.Short,
                      )
                    }
                  }
                  is RecordStatus.SaveRecordedLogs -> {
                    saveLogsToFile(context, status.logs).collect { result ->
                      when (result) {
                        SaveResult.InProgress -> Unit
                        is SaveResult.Success -> {
                          lastShownSnackBar?.cancel()
                          lastShownSnackBar = null
                          recordStatus = RecordStatus.Idle
                          savedLogsSheetState = SavedLogsBottomSheetState.Show(
                            fileName = result.fileName,
                            uri = result.uri,
                            isCustomLocation = result.isCustomLocation,
                          )
                        }
                        is SaveResult.Failure -> {
                          recordStatus = RecordStatus.Idle
                          lastShownSnackBar?.cancel()
                          if (result.emptyLogs) {
                            lastShownSnackBar = launch {
                              snackbarHostState.showSnackbar(
                                message = noNewLogsMessage,
                                withDismissAction = true,
                                duration = SnackbarDuration.Short,
                              )
                            }
                          } else {
                            lastShownSnackBar = launch {
                              snackbarHostState.showSnackbar(
                                message = saveFailedMessage,
                                withDismissAction = true,
                                duration = SnackbarDuration.Short,
                              )
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
          }

          IconButton(
            onClick = {
              when (recordStatus) {
                RecordStatus.Idle -> {
                  logcatService?.logcatSession?.startRecording()
                  recordStatus = RecordStatus.RecordingInProgress
                }
                RecordStatus.RecordingInProgress -> {
                  val logs = logcatService?.logcatSession?.stopRecording() ?: emptyList()
                  recordStatus = RecordStatus.SaveRecordedLogs(logs = logs)
                }
                else -> {
                  // Do nothing.
                }
              }
            },
            enabled = !logcatPaused && logcatService != null &&
              recordStatus !is RecordStatus.SaveRecordedLogs,
            colors = IconButtonDefaults.iconButtonColors(
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
          ) {
            when (recordStatus) {
              RecordStatus.Idle -> {
                Icon(Icons.Default.FiberManualRecord, contentDescription = null)
              }
              RecordStatus.RecordingInProgress -> {
                Icon(Icons.Default.Stop, contentDescription = null)
              }
              is RecordStatus.SaveRecordedLogs -> {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp,
                )
              }
            }
          }
          Box {
            IconButton(
              onClick = {
                showDropDownMenu = true
              },
              colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
            ) {
              Icon(Icons.Default.MoreVert, contentDescription = null)
            }
            DropdownMenu(
              expanded = showDropDownMenu,
              onDismissRequest = { showDropDownMenu = false }
            ) {
              DropdownMenuItem(
                leadingIcon = {
                  Icon(Icons.Default.Clear, contentDescription = null)
                },
                text = {
                  Text(
                    text = stringResource(R.string.clear),
                    style = AppTypography.bodyLarge,
                  )
                },
                onClick = {
                  logsState.clear()
                  showDropDownMenu = false
                }
              )
              DropdownMenuItem(
                leadingIcon = {
                  Icon(Icons.Default.FilterList, contentDescription = null)
                },
                text = {
                  Text(
                    text = stringResource(R.string.filters),
                    style = AppTypography.bodyLarge,
                  )
                },
                onClick = {
                  showDropDownMenu = false
                  val intent = Intent(context, FiltersActivity::class.java)
                  intent.putExtra(FiltersActivity.EXTRA_EXCLUSIONS, false)
                  context.startActivity(intent)
                }
              )
              DropdownMenuItem(
                leadingIcon = {
                  Icon(Icons.Default.FilterList, contentDescription = null)
                },
                text = {
                  Text(
                    text = stringResource(R.string.exclusions),
                    style = AppTypography.bodyLarge,
                  )
                },
                onClick = {
                  showDropDownMenu = false
                  val intent = Intent(context, FiltersActivity::class.java)
                  intent.putExtra(FiltersActivity.EXTRA_EXCLUSIONS, true)
                  context.startActivity(intent)
                }
              )
              DropdownMenuItem(
                leadingIcon = {
                  Icon(Icons.Default.Save, contentDescription = null)
                },
                text = {
                  Text(
                    text = stringResource(R.string.save),
                    style = AppTypography.bodyLarge,
                  )
                },
                onClick = {
                  showDropDownMenu = false
                  // TODO
                },
                enabled = logcatService != null,
              )
              DropdownMenuItem(
                leadingIcon = {
                  Icon(Icons.Default.FolderOpen, contentDescription = null)
                },
                text = {
                  Text(
                    text = stringResource(R.string.saved_logs),
                    style = AppTypography.bodyLarge,
                  )
                },
                onClick = {
                  showDropDownMenu = false
                  context.startActivity(Intent(context, SavedLogsActivity::class.java))
                }
              )
              DropdownMenuItem(
                leadingIcon = {
                  Icon(Icons.Default.RestartAlt, contentDescription = null)
                },
                text = {
                  Text(
                    text = stringResource(R.string.restart_logcat),
                    style = AppTypography.bodyLarge,
                  )
                },
                onClick = {
                  showDropDownMenu = false
                  restartTrigger.trySend(true)
                },
                enabled = logcatService != null && recordStatus is RecordStatus.Idle,
              )
              DropdownMenuItem(
                leadingIcon = {
                  Icon(Icons.Default.Settings, contentDescription = null)
                },
                text = {
                  Text(
                    text = stringResource(R.string.settings),
                    style = AppTypography.bodyLarge,
                  )
                },
                onClick = {
                  showDropDownMenu = false
                  context.startActivity(Intent(context, SettingsActivity::class.java))
                }
              )
            }
          }
        }
      )
      AnimatedVisibility(
        visible = showSearchBar,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(focusRequester) {
          focusRequester.requestFocus()
        }

        TopAppBar(
          modifier = Modifier.fillMaxWidth(),
          navigationIcon = {
            IconButton(
              onClick = {
                showSearchBar = false
                searchHitsMap.clear()
                sortedHitsByLogIdsState = emptyList()
                currentSearchHitIndex = -1
                currentSearchHitLogId = -1
                focusManager.clearFocus()
              },
            ) {
              Icon(imageVector = Icons.Default.Close, contentDescription = null)
            }
          },
          title = {
            TextField(
              modifier = Modifier.focusRequester(focusRequester),
              value = requireNotNull(searchQuery),
              onValueChange = { query ->
                searchQuery = query
              },
              maxLines = 1,
              singleLine = true,
              placeholder = {
                Row(modifier = Modifier.fillMaxHeight()) {
                  Text(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    text = "Search",
                  )
                }
              },
              colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                unfocusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
              textStyle = LocalTextStyle.current.copy(
                fontSize = 18.sp,
              ),
              suffix = {
                if (searchInProgress) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                  )
                } else if (showHitCount) {
                  val current = currentSearchHitIndex.takeIf { it != -1 }?.let { it + 1 } ?: 0
                  Text(
                    text = "$current/${searchHitsMap.size}",
                    style = AppTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                }
              }
            )
          },
          actions = {
            IconButton(
              onClick = {
                focusManager.clearFocus()
                if (currentSearchHitIndex - 1 >= 0) {
                  currentSearchHitIndex -= 1
                } else {
                  currentSearchHitIndex = searchHitsMap.size - 1
                }
              },
              enabled = searchHitsMap.isNotEmpty(),
              colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
            ) {
              Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = null)
            }
            IconButton(
              onClick = {
                focusManager.clearFocus()
                currentSearchHitIndex = (currentSearchHitIndex + 1) % searchHitsMap.size
              },
              enabled = searchHitsMap.isNotEmpty(),
              colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
            ) {
              Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
        )
      }
    },
    floatingActionButton = {
      AnimatedVisibility(
        visible = snapScrollInfo.isScrollSnapperVisible,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
      ) {
        Column {
          FloatingActionButton(
            modifier = Modifier.size(48.dp),
            onClick = {
              coroutineScope.launch {
                lazyListState.scrollToItem(0)
              }
            },
            interactionSource = snapUpInteractionSource,
          ) {
            Icon(
              imageVector = Icons.Filled.ArrowUpward,
              contentDescription = null
            )
          }
          Spacer(modifier = Modifier.height(12.dp))
          FloatingActionButton(
            modifier = Modifier.size(48.dp),
            onClick = {
              coroutineScope.launch {
                if (lazyListState.layoutInfo.totalItemsCount > 0) {
                  if (!showSearchBar || searchQuery.isEmpty()) {
                    snapToBottom = true
                  }
                  lazyListState.scrollToItem(lazyListState.layoutInfo.totalItemsCount - 1)
                }
              }
            },
            interactionSource = snapDownInteractionSource,
          ) {
            Icon(
              imageVector = Icons.Filled.ArrowDownward,
              contentDescription = null
            )
          }
        }
      }
    },
    snackbarHost = {
      SnackbarHost(hostState = snackbarHostState)
    }
  ) { innerPadding ->
    if (showSearchBar) {
      LaunchedEffect(Unit) {
        snapshotFlow { searchQuery }
          .collectLatest { searchQuery ->
            delay(100L)
            showHitCount = searchQuery.isNotEmpty()
            if (searchQuery.isNotEmpty()) {
              searchInProgress = true
              var scrolled = false
              snapshotFlow { logsState.toList() }
                .collect { logs ->
                  val (map, sortedHitsByLogId) = withContext(Dispatchers.Default) {
                    val map = mutableMapOf<SearchHitKey, Pair<Int, Int>>()
                    logs.forEach { log ->
                      val msgIndex = log.msg.indexOf(string = searchQuery, ignoreCase = true)
                      val tagIndex = log.tag.indexOf(string = searchQuery, ignoreCase = true)
                      if (msgIndex != -1) {
                        map[SearchHitKey(logId = log.id, component = LogComponent.MSG)] =
                          Pair(msgIndex, msgIndex + searchQuery.length)
                      }
                      if (tagIndex != -1) {
                        map[SearchHitKey(logId = log.id, component = LogComponent.TAG)] =
                          Pair(tagIndex, tagIndex + searchQuery.length)
                      }
                    }
                    Pair(map, map.keys.map { it.logId }.sorted())
                  }
                  searchHitsMap.clear()
                  searchHitsMap.putAll(map)
                  sortedHitsByLogIdsState = sortedHitsByLogId

                  if (!scrolled) {
                    searchInProgress = false
                    if (sortedHitsByLogIdsState.isNotEmpty()) {
                      currentSearchHitIndex = 0
                      currentSearchHitLogId = sortedHitsByLogIdsState.first()
                      snapToBottom = false
                      scrolled = true
                    } else {
                      currentSearchHitIndex = -1
                      currentSearchHitLogId = -1
                    }
                  }
                }
            } else {
              searchInProgress = false
              searchHitsMap.clear()
              sortedHitsByLogIdsState = emptyList()
              currentSearchHitIndex = -1
              currentSearchHitLogId = -1
            }
          }
      }
      if (searchQuery.isNotEmpty()) {
        LaunchedEffect(lazyListState, searchQuery) {
          snapshotFlow { sortedHitsByLogIdsState to currentSearchHitIndex }
            .filter { (_, index) -> index != -1 }
            .distinctUntilChangedBy { (_, index) -> index }
            .collectLatest { (hits, index) ->
              if (index < hits.size) {
                currentSearchHitLogId = hits[index]
                lazyListState.scrollToItem(currentSearchHitLogId)
              }
            }
        }
      }
    }

    if (savedLogsSheetState is SavedLogsBottomSheetState.Show) {
      val saveInfo = savedLogsSheetState as SavedLogsBottomSheetState.Show
      ModalBottomSheet(
        onDismissRequest = {
          savedLogsSheetState = SavedLogsBottomSheetState.Hide
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
      ) {
        ListItem(
          modifier = Modifier.fillMaxWidth(),
          headlineContent = {
            Text(
              modifier = Modifier.weight(1f),
              text = stringResource(R.string.saved_as_filename).format(saveInfo.fileName),
              style = AppTypography.titleMedium,
            )
          },
          colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
          ),
        )
        ListItem(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              savedLogsSheetState = SavedLogsBottomSheetState.Hide
              val intent = Intent(context, SavedLogsViewerActivity::class.java)
              intent.setDataAndType(saveInfo.uri, "text/plain")
              context.startActivity(intent)
            },
          leadingContent = {
            Icon(imageVector = Icons.AutoMirrored.Default.ViewList, contentDescription = null)
          },
          headlineContent = {
            Text(text = stringResource(R.string.view_log))
          },
          colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
          ),
        )
        ListItem(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              savedLogsSheetState = SavedLogsBottomSheetState.Hide
              ShareUtils.shareSavedLogs(
                context = context,
                uri = saveInfo.uri,
                isCustom = saveInfo.isCustomLocation,
              )
            },
          leadingContent = {
            Icon(imageVector = Icons.Default.Share, contentDescription = null)
          },
          headlineContent = {
            Text(text = stringResource(R.string.share))
          },
          colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
          ),
        )
      }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LogsList(
      modifier = Modifier
        .fillMaxSize()
        .consumeWindowInsets(innerPadding)
        .pointerInput(Unit) {
          lifecycle.currentStateFlow.collectLatest { state ->
            if (state == Lifecycle.State.RESUMED) {
              awaitPointerEventScope {
                while (true) {
                  val event = awaitPointerEvent()
                  when (event.type) {
                    PointerEventType.Press -> {
                      snapToBottom = false
                      focusManager.clearFocus()
                    }
                  }
                }
              }
            }
          }
        },
      contentPadding = innerPadding,
      logs = logsState,
      searchHits = searchHitsMap,
      onClick = {},
      onLongClick = {},
      state = lazyListState,
      currentSearchHitLogId = currentSearchHitLogId,
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LogsList(
  modifier: Modifier,
  contentPadding: PaddingValues,
  logs: List<Log>,
  searchHits: Map<SearchHitKey, Pair<Int, Int>>,
  currentSearchHitLogId: Int,
  onClick: (Int) -> Unit,
  onLongClick: (Int) -> Unit,
  state: LazyListState = rememberLazyListState(),
) {
  val textSelectionColors = LocalTextSelectionColors.current
  val currentSearchHitColor = currentSearchHitColor()
  LazyColumn(
    modifier = modifier,
    state = state,
    contentPadding = contentPadding,
  ) {
    itemsIndexed(
      items = logs,
      key = { index, _ -> logs[index].id }
    ) { index, item ->
      if (index > 0) {
        HorizontalDivider()
      }

      fun maybeHighlightSearchHit(target: String, searchHitKey: SearchHitKey): AnnotatedString {
        val hit = if (searchHits.isNotEmpty()) searchHits[searchHitKey] else null
        return if (hit != null) {
          buildAnnotatedString {
            append(target)
            val color = if (index == currentSearchHitLogId) {
              currentSearchHitColor
            } else {
              textSelectionColors.backgroundColor
            }
            addStyle(
              SpanStyle(
                background = color
              ),
              start = hit.first,
              end = hit.second
            )
          }
        } else {
          AnnotatedString(target)
        }
      }

      LogItem(
        modifier = Modifier
          .fillMaxWidth()
          .combinedClickable(
            onLongClick = { onLongClick(index) },
            onClick = { onClick(index) },
          )
          .wrapContentHeight(),
        priority = item.priority,
        tag = maybeHighlightSearchHit(
          target = item.tag,
          searchHitKey = SearchHitKey(logId = item.id, component = LogComponent.TAG),
        ),
        message = maybeHighlightSearchHit(
          target = item.msg,
          searchHitKey = SearchHitKey(logId = item.id, component = LogComponent.MSG),
        ),
        date = item.date,
        time = item.time,
        pid = item.pid,
        tid = item.tid,
        priorityColor = when (item.priority) {
          LogPriority.ASSERT -> LogPriorityColors.priorityAssert
          LogPriority.DEBUG -> LogPriorityColors.priorityDebug
          LogPriority.ERROR -> LogPriorityColors.priorityError
          LogPriority.FATAL -> LogPriorityColors.priorityFatal
          LogPriority.INFO -> LogPriorityColors.priorityInfo
          LogPriority.VERBOSE -> LogPriorityColors.priorityVerbose
          LogPriority.WARNING -> LogPriorityColors.priorityWarning
          else -> LogPriorityColors.prioritySilent
        },
      )
    }
  }
}

@Composable
private fun LogItem(
  modifier: Modifier,
  priority: String,
  tag: AnnotatedString,
  message: AnnotatedString,
  date: String,
  time: String,
  pid: String,
  tid: String,
  priorityColor: Color,
) {
  Row(
    modifier = modifier
      .height(IntrinsicSize.Max),
  ) {
    Box(
      modifier = Modifier
        .fillMaxHeight()
        .background(priorityColor)
        .padding(5.dp),
    ) {
      Text(
        modifier = Modifier.align(Alignment.Center),
        text = priority,
        style = TextStyle.Default.copy(
          fontSize = 12.sp,
          fontFamily = RobotoMonoFontFamily,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          textAlign = TextAlign.Center,
        ),
      )
    }
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(all = 5.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        modifier = Modifier.fillMaxWidth(),
        text = tag,
        style = TextStyle.Default.copy(
          fontSize = 13.sp,
          fontFamily = RobotoMonoFontFamily,
          fontWeight = FontWeight.Medium,
        )
      )
      Text(
        modifier = Modifier.fillMaxWidth(),
        text = message,
        style = TextStyle.Default.copy(
          fontSize = 12.sp,
          fontFamily = RobotoMonoFontFamily,
        )
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Absolute.spacedBy(10.dp),
      ) {
        Text(
          text = date,
          style = TextStyle.Default.copy(
            fontSize = 12.sp,
            fontFamily = RobotoMonoFontFamily,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
          )
        )
        Text(
          text = time,
          style = TextStyle.Default.copy(
            fontSize = 12.sp,
            fontFamily = RobotoMonoFontFamily,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
          )
        )
        Text(
          text = pid,
          style = TextStyle.Default.copy(
            fontSize = 12.sp,
            fontFamily = RobotoMonoFontFamily,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
          )
        )
        Text(
          text = tid,
          style = TextStyle.Default.copy(
            fontSize = 12.sp,
            fontFamily = RobotoMonoFontFamily,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
          )
        )
      }
    }
  }
}

@Composable
private fun rememberLogcatServiceConnection(): LogcatService? {
  var logcatService by remember { mutableStateOf<LogcatService?>(null) }
  // Connect to service.
  val context = LocalContext.current
  DisposableEffect(Unit) {
    val serviceBinder = ServiceBinder(LogcatService::class.java, object : ServiceConnection {
      override fun onServiceConnected(
        name: ComponentName?,
        service: IBinder,
      ) {
        Logger.debug(TAG, "onServiceConnected")
        logcatService = service.getService()
      }

      override fun onServiceDisconnected(name: ComponentName) {
        Logger.debug(TAG, "onServiceDisconnected")
        logcatService = null
      }
    })
    serviceBinder.bind(context)

    onDispose {
      serviceBinder.unbind(context)
    }
  }

  return logcatService
}

data class SnapScrollInfo(
  val isScrollSnapperVisible: Boolean = false,
  val shouldSnapScrollDown: Boolean = false,
  val shouldSnapScrollUp: Boolean = false,
)

@Composable
private fun rememberSnapScrollInfo(
  lazyListState: LazyListState,
  snapToBottom: Boolean,
  snapUpInteractionSource: InteractionSource,
  snapDownInteractionSource: InteractionSource,
): SnapScrollInfo {
  var snapScrollInfo by remember { mutableStateOf(SnapScrollInfo()) }

  if (snapToBottom) {
    LaunchedEffect(lazyListState) {
      snapScrollInfo = SnapScrollInfo()
      snapshotFlow { lazyListState.layoutInfo.totalItemsCount }
        .filter { lastIndex -> lastIndex > 0 }
        .collect { lastIndex ->
          lazyListState.scrollToItem(lastIndex)
        }
    }
  } else {
    LaunchedEffect(lazyListState, snapUpInteractionSource, snapDownInteractionSource) {
      data class LastItemOffsetInfo(
        val lastItem: Boolean,
        val lastItemSize: Int,
        val lastVisibleOffset: Int,
      )

      data class ItemOffsetInfo(
        val viewportEndOffset: Int,
        val firstVisibleIndex: Int,
        val firstVisibleOffset: Int,
        val lastItemInfo: LastItemOffsetInfo?,
        val lastScrolledForward: Boolean,
        val lastScrolledBackward: Boolean,
      )

      launch {
        combine(
          snapshotFlow {
            val layoutInfo = lazyListState.layoutInfo
            ItemOffsetInfo(
              viewportEndOffset = layoutInfo.viewportEndOffset,
              firstVisibleIndex = lazyListState.firstVisibleItemIndex,
              firstVisibleOffset = lazyListState.firstVisibleItemScrollOffset,
              lastItemInfo = layoutInfo.visibleItemsInfo.lastOrNull()?.let { info ->
                LastItemOffsetInfo(
                  lastItem = info.index == layoutInfo.totalItemsCount - 1,
                  lastItemSize = info.size,
                  lastVisibleOffset = info.offset,
                )
              },
              lastScrolledForward = lazyListState.lastScrolledForward,
              lastScrolledBackward = lazyListState.lastScrolledBackward,
            )
          },
          snapUpInteractionSource.interactions.stateIn(this, Eagerly, null),
          snapDownInteractionSource.interactions.stateIn(this, Eagerly, null),
        ) { offsetInfo, snapUpInteraction, snapDownInteraction ->
          Triple(offsetInfo, snapUpInteraction, snapDownInteraction)
        }.collectLatest { (offsetInfo, snapUpInteraction, snapDownInteraction) ->
          var shouldSnapScrollUp = false
          var shouldSnapScrollDown = false
          if (offsetInfo.lastScrolledForward) {
            val lastItemInfo = offsetInfo.lastItemInfo
            if (lastItemInfo != null) {
              val canScrollDown = !lastItemInfo.lastItem ||
                (lastItemInfo.lastVisibleOffset + lastItemInfo.lastItemSize) > offsetInfo.viewportEndOffset
              shouldSnapScrollUp = false
              shouldSnapScrollDown = canScrollDown
            } else {
              shouldSnapScrollDown = false
            }
          } else if (offsetInfo.lastScrolledBackward) {
            val canScrollUp =
              offsetInfo.firstVisibleIndex != 0 || offsetInfo.firstVisibleOffset > 0
            shouldSnapScrollDown = false
            shouldSnapScrollUp = canScrollUp
          }
          var isScrollSnapperVisible = shouldSnapScrollUp || shouldSnapScrollDown
          snapScrollInfo = snapScrollInfo.copy(
            shouldSnapScrollUp = shouldSnapScrollUp,
            shouldSnapScrollDown = shouldSnapScrollDown,
            isScrollSnapperVisible = isScrollSnapperVisible,
          )

          // Do not hide while the FABs are being pressed.
          val isFabPressed = snapUpInteraction is Press || snapDownInteraction is Press
          if (isScrollSnapperVisible && !isFabPressed) {
            delay(SNAP_SCROLL_HIDE_DELAY_MS)
            snapScrollInfo = snapScrollInfo.copy(
              isScrollSnapperVisible = false,
            )
          }
        }
      }
    }
  }
  return snapScrollInfo
}

sealed interface SaveResult {
  data object InProgress : SaveResult
  data class Success(
    val fileName: String,
    val uri: Uri,
    val isCustomLocation: Boolean,
  ) : SaveResult

  data class Failure(
    val emptyLogs: Boolean = false,
  ) : SaveResult
}

@WorkerThread
private fun saveLogsToFile(context: Context, logs: List<Log>): Flow<SaveResult> = flow {
  emit(SaveResult.InProgress)

  if (logs.isEmpty()) {
    emit(SaveResult.Failure(emptyLogs = true))
    return@flow
  }

  val (uri, isUsingCustomLocation) = createFile(context)
  if (uri == null) {
    emit(SaveResult.Failure())
    return@flow
  }

  val success = withContext(Dispatchers.IO) {
    if (isUsingCustomLocation) {
      LogcatUtil.writeToFile(context, logs, uri)
    } else {
      LogcatUtil.writeToFile(logs, uri.toFile())
    }
  }

  if (success) {
    val fileName = if (isUsingCustomLocation) {
      DocumentFile.fromSingleUri(context, uri)?.name
    } else {
      uri.toFile().name
    }

    if (fileName == null) {
      emit(SaveResult.Failure())
      return@flow
    }

    val db = MyDB.getInstance(context)
    withContext(Dispatchers.IO) {
      db.savedLogsDao().insert(
        SavedLogInfo(
          fileName,
          uri.toString(), isUsingCustomLocation
        )
      )
    }

    emit(
      SaveResult.Success(
        fileName = fileName,
        uri = uri,
        isCustomLocation = isUsingCustomLocation,
      )
    )
  } else {
    emit(SaveResult.Failure())
  }
}

// Returns a pair of Uri and custom save location flag (true if custom save location is used).
@WorkerThread
private fun createFile(context: Context): Pair<Uri?, Boolean> {
  val timeStamp = SimpleDateFormat("MM-dd-yyyy_HH-mm-ss", Locale.getDefault())
    .format(Date())
  val fileName = "logcat_$timeStamp"

  val customSaveLocation = context.getDefaultSharedPreferences().getString(
    PreferenceKeys.Logcat.KEY_SAVE_LOCATION,
    PreferenceKeys.Logcat.Default.SAVE_LOCATION
  )!!

  return if (customSaveLocation.isEmpty()) {
    val file = File(context.filesDir, LOGCAT_DIR)
    file.mkdirs()
    Pair(File(file, "$fileName.txt").toUri(), false)
  } else {
    val documentFile = DocumentFile.fromTreeUri(context, customSaveLocation.toUri())
    Pair(documentFile?.createFile("text/plain", fileName)?.uri, true)
  }
}

sealed interface RecordStatus {
  data object Idle : RecordStatus
  data object RecordingInProgress : RecordStatus
  data class SaveRecordedLogs(val logs: List<Log>) : RecordStatus
}

data class SearchHitKey(
  val logId: Int,
  val component: LogComponent,
) {
  enum class LogComponent {
    MSG,
    TAG,
  }
}

sealed interface SavedLogsBottomSheetState {
  data object Hide : SavedLogsBottomSheetState
  data class Show(
    val fileName: String,
    val uri: Uri,
    val isCustomLocation: Boolean,
  ) : SavedLogsBottomSheetState
}

private class LogFilter(
  filterInfo: FilterInfo,
) : Filter {
  private val type = filterInfo.type
  private val content = filterInfo.content
  private val priorities: Set<String> = if (type == FilterType.LOG_LEVELS) {
    filterInfo.content.split(",").toSet()
  } else {
    emptySet()
  }

  override fun apply(log: Log): Boolean {
    if (content.isEmpty()) {
      return true
    }

    return when (type) {
      FilterType.LOG_LEVELS -> {
        log.priority in priorities
      }
      FilterType.KEYWORD -> log.msg.containsIgnoreCase(content)
      FilterType.TAG -> log.tag.containsIgnoreCase(content)
      FilterType.PID -> log.pid.containsIgnoreCase(content)
      FilterType.TID -> log.tid.containsIgnoreCase(content)
      else -> false
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun LogItemPreview() {
  LogcatReaderTheme {
    LogItem(
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight(),
      priority = "D",
      tag = AnnotatedString("Tag"),
      message = AnnotatedString("This is a log"),
      date = "01-12",
      time = "21:10:46.123",
      pid = "1600",
      tid = "123123",
      priorityColor = LogPriorityColors.priorityDebug,
    )
  }
}