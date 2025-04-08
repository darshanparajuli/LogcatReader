package com.dp.logcatapp.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import android.os.Process
import androidx.activity.compose.BackHandler
import androidx.annotation.WorkerThread
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction.Press
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dp.logcat.Filter
import com.dp.logcat.Log
import com.dp.logcat.LogcatSession.RecordingFileInfo
import com.dp.logcat.LogcatUtil
import com.dp.logcatapp.BuildConfig
import com.dp.logcatapp.R
import com.dp.logcatapp.activities.FiltersActivity
import com.dp.logcatapp.activities.SavedLogsActivity
import com.dp.logcatapp.activities.SavedLogsViewerActivity
import com.dp.logcatapp.activities.SettingsActivity
import com.dp.logcatapp.db.FilterInfo
import com.dp.logcatapp.db.LogcatReaderDatabase
import com.dp.logcatapp.db.SavedLogInfo
import com.dp.logcatapp.services.LogcatService
import com.dp.logcatapp.services.LogcatService.LogcatSessionStatus
import com.dp.logcatapp.services.getService
import com.dp.logcatapp.ui.common.CopyLogClipboardBottomSheet
import com.dp.logcatapp.ui.common.Dialog
import com.dp.logcatapp.ui.common.LOGCAT_DIR
import com.dp.logcatapp.ui.common.LogsList
import com.dp.logcatapp.ui.common.LogsListStyle
import com.dp.logcatapp.ui.common.SearchHitKey
import com.dp.logcatapp.ui.common.SearchLogsTopBar
import com.dp.logcatapp.ui.common.SearchResult.SearchHitInfo
import com.dp.logcatapp.ui.common.SearchResult.SearchHitSpan
import com.dp.logcatapp.ui.common.searchLogs
import com.dp.logcatapp.ui.theme.AppTypography
import com.dp.logcatapp.util.PreferenceKeys
import com.dp.logcatapp.util.ShareUtils
import com.dp.logcatapp.util.SuCommander
import com.dp.logcatapp.util.getDefaultSharedPreferences
import com.dp.logcatapp.util.rememberBooleanSharedPreference
import com.dp.logcatapp.util.showToast
import com.dp.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "HomeScreen"
private const val SNAP_SCROLL_HIDE_DELAY_MS = 2000L
private const val SHOW_PERMISSION_GRANTED_INFO_PREF_KEY = "show_permission_info_dialog"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceLogsScreen(
  modifier: Modifier,
  stopRecordingSignal: Boolean,
  onStartRecording: () -> Unit,
  onStopRecording: () -> Unit,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current

  val logcatService = rememberLogcatServiceConnection()
  val lazyListState = rememberLazyListState()

  val updatedOnStartRecording by rememberUpdatedState(onStartRecording)
  val updatedOnStopRecording by rememberUpdatedState(onStopRecording)
  val updatedStopRecordingSignal by rememberUpdatedState(stopRecordingSignal)

  var snapToBottom by remember { mutableStateOf(true) }
  val scrollToTopInteractionSource = remember { MutableInteractionSource() }
  val scrollToBottomInteractionSource = remember { MutableInteractionSource() }
  val snapScrollInfo = rememberSnapScrollInfo(
    lazyListState = lazyListState,
    snapToBottom = snapToBottom,
    snapUpInteractionSource = scrollToTopInteractionSource,
    snapDownInteractionSource = scrollToBottomInteractionSource,
  )

  val logsState = remember { mutableStateListOf<Log>() }
  var searchInProgress by remember { mutableStateOf(false) }
  var showDropDownMenu by remember { mutableStateOf(false) }
  var showSearchBar by remember { mutableStateOf(false) }
  var logcatPaused by remember { mutableStateOf(false) }
  var compactViewPreference = rememberBooleanSharedPreference(
    key = PreferenceKeys.Logcat.KEY_COMPACT_VIEW,
    default = PreferenceKeys.Logcat.Default.COMPACT_VIEW,
  )
  var searchQuery by remember { mutableStateOf("") }
  // Value: tagIndex start and end.
  val searchHitsMap = remember { mutableStateMapOf<SearchHitKey, SearchHitSpan>() }
  // List of pair of logId and it's index on the list.
  var sortedHitsByLogIdsState by remember { mutableStateOf<List<SearchHitInfo>>(emptyList()) }
  var currentSearchHitIndex by remember { mutableIntStateOf(-1) }
  var currentSearchHitLogId by remember { mutableIntStateOf(-1) }
  var showHitCount by remember { mutableStateOf(false) }
  var recordStatus by remember { mutableStateOf<RecordStatus>(RecordStatus.Idle) }
  val snackbarHostState = remember { SnackbarHostState() }
  var savedLogsSheetState by remember {
    mutableStateOf<SavedLogsBottomSheetState>(SavedLogsBottomSheetState.Hide)
  }
  var appliedFilters by remember { mutableStateOf(false) }
  var isLogcatSessionLoading by remember { mutableStateOf(true) }
  var errorStartingLogcat by remember { mutableStateOf(false) }

  if (showSearchBar) {
    BackHandler { showSearchBar = false }
  }

  if (logcatService != null) {
    val db = remember(context) { LogcatReaderDatabase.getInstance(context) }
    LaunchedEffect(logcatService) {
      logcatService.logcatSessionStatus
        .filterNotNull()
        .collectLatest { status ->
          if (status is LogcatSessionStatus.Started) {
            val logcatSession = status.session
            if (logcatSession.isRecording) {
              recordStatus = RecordStatus.RecordingInProgress
            }

            if (updatedStopRecordingSignal) {
              if (recordStatus == RecordStatus.RecordingInProgress) {
                if (logcatSession.isRecording) {
                  recordStatus = RecordStatus.SaveRecordedLogs
                } else {
                  recordStatus = RecordStatus.Idle
                }
              }
            }

            db.filterDao().filters()
              .collectLatest { filters ->
                appliedFilters = filters.isNotEmpty()

                val includeFilters = filters.filterNot { it.exclude }
                val excludeFilters = filters.filter { it.exclude }

                logcatSession.setFilters(
                  filters = includeFilters.map(::LogFilter),
                  exclusion = false
                )
                logcatSession.setFilters(
                  filters = excludeFilters.map(::LogFilter),
                  exclusion = true
                )

                logsState.clear()
                isLogcatSessionLoading = false
                logcatSession.logs.collect { logs ->
                  logsState += logs
                  if (logcatPaused) {
                    snapshotFlow { logcatPaused }.first { !it }
                  }
                }
              }
          } else {
            errorStartingLogcat = true
            isLogcatSessionLoading = false
          }
        }
    }
  }

  Scaffold(
    modifier = modifier,
    topBar = {
      val startedRecordingMessage = stringResource(R.string.started_recording)
      val saveFailedMessage = stringResource(R.string.failed_to_save_logs)

      if (logcatService != null) {
        LaunchedEffect(logcatService) {
          snapshotFlow { recordStatus }
            .map { it == RecordStatus.RecordingInProgress }
            .collect { isRecording ->
              logcatService.updateNotification(showStopRecording = isRecording)
            }
        }
      }

      LaunchedEffect(logcatService) {
        var lastShownSnackBar: Job? = null
        snapshotFlow { recordStatus }
          .collect { status ->
            when (status) {
              RecordStatus.Idle -> Unit
              RecordStatus.RecordingInProgress -> {
                val logcatSession = snapshotFlow { logcatService }.filterNotNull()
                  .mapNotNull { it.logcatSessionStatus.filterNotNull().first() }
                  .filterIsInstance<LogcatSessionStatus.Started>()
                  .map { it.session }
                  .first()

                if (!logcatSession.isRecording) {
                  val recordingFileInfo = createFileToStartRecording(context)
                  if (recordingFileInfo == null) {
                    context.showToast(context.getString(R.string.error))
                    recordStatus = RecordStatus.Idle
                    return@collect
                  }

                  val writer = recordingFileInfo.createBufferedWriter(context)
                  if (writer == null) {
                    context.showToast(context.getString(R.string.error))
                    recordStatus = RecordStatus.Idle
                    return@collect
                  }

                  logcatSession.startRecording(
                    recordingFileInfo = recordingFileInfo,
                    writer = writer,
                  )

                  updatedOnStartRecording()
                  lastShownSnackBar?.cancel()
                  lastShownSnackBar = launch {
                    snackbarHostState.showSnackbar(
                      message = startedRecordingMessage,
                      withDismissAction = true,
                      duration = SnackbarDuration.Short,
                    )
                  }
                }
              }
              RecordStatus.SaveRecordedLogs -> {
                val logcatSession = snapshotFlow { logcatService }.filterNotNull()
                  .mapNotNull { it.logcatSessionStatus.filterNotNull().first() }
                  .filterIsInstance<LogcatSessionStatus.Started>()
                  .map { it.session }
                  .first()
                val info = withContext(Dispatchers.IO) { logcatSession.stopRecording() }

                if (info != null) {
                  lastShownSnackBar?.cancel()
                  lastShownSnackBar = null
                  recordStatus = RecordStatus.Idle
                  savedLogsSheetState = SavedLogsBottomSheetState.Show(
                    fileName = info.fileName,
                    uri = info.uri,
                    isCustomLocation = info.isCustomLocation,
                  )
                } else {
                  recordStatus = RecordStatus.Idle
                  lastShownSnackBar?.cancel()
                  lastShownSnackBar = launch {
                    snackbarHostState.showSnackbar(
                      message = saveFailedMessage,
                      withDismissAction = true,
                      duration = SnackbarDuration.Short,
                    )
                  }
                }
                updatedOnStopRecording()
              }
            }
          }
      }

      var saveLogsInProgress by remember { mutableStateOf(false) }

      AppBar(
        title = stringResource(R.string.device_logs),
        subtitle = logsState.size.toString(),
        filtered = appliedFilters,
        isPaused = logcatPaused,
        pauseEnabled = recordStatus == RecordStatus.Idle &&
          !isLogcatSessionLoading && !errorStartingLogcat,
        recordEnabled = !logcatPaused && logcatService != null &&
          recordStatus != RecordStatus.SaveRecordedLogs &&
          !isLogcatSessionLoading && !errorStartingLogcat,
        recordStatus = recordStatus,
        compactViewEnabled = compactViewPreference.value,
        showDropDownMenu = showDropDownMenu,
        saveEnabled = logcatService != null && !isLogcatSessionLoading && !errorStartingLogcat,
        saveLogsInProgress = saveLogsInProgress,
        restartLogcatEnabled = logcatService != null && recordStatus == RecordStatus.Idle,
        onClickSearch = {
          showSearchBar = true
        },
        onClickPause = {
          logcatPaused = !logcatPaused
        },
        onClickRecord = {
          when (recordStatus) {
            RecordStatus.Idle -> {
              recordStatus = RecordStatus.RecordingInProgress
            }
            RecordStatus.RecordingInProgress -> {
              recordStatus = RecordStatus.SaveRecordedLogs
            }
            RecordStatus.SaveRecordedLogs -> Unit
          }
        },
        onShowDropdownMenu = {
          showDropDownMenu = true
        },
        onDismissDropdownMenu = {
          showDropDownMenu = false
        },
        onClickClear = {
          logsState.clear()
          showDropDownMenu = false
        },
        onClickCompactView = {
          compactViewPreference.value = !compactViewPreference.value
        },
        onClickFilter = {
          showDropDownMenu = false
          val intent = Intent(context, FiltersActivity::class.java)
          context.startActivity(intent)
        },
        onClickSave = {
          coroutineScope.launch {
            val logs =
              logcatService?.logcatSessionStatus?.value?.sessionOrNull?.let { logcatSession ->
                withContext(Dispatchers.Default) { logcatSession.getAllLogsFiltered() }
              }
            if (logs != null) {
              saveLogsToFile(context, logs).collect { result ->
                when (result) {
                  SaveResult.InProgress -> {
                    saveLogsInProgress = true
                  }
                  is SaveResult.Failure -> {
                    context.showToast(saveFailedMessage)
                    saveLogsInProgress = false
                    showDropDownMenu = false
                  }
                  is SaveResult.Success -> {
                    saveLogsInProgress = false
                    showDropDownMenu = false
                    savedLogsSheetState = SavedLogsBottomSheetState.Show(
                      fileName = result.fileName,
                      uri = result.uri,
                      isCustomLocation = result.isCustomLocation,
                    )
                  }
                }
              }
            } else {
              context.showToast(saveFailedMessage)
              showDropDownMenu = false
            }
          }
        },
        onClickSavedLogs = {
          showDropDownMenu = false
          context.startActivity(Intent(context, SavedLogsActivity::class.java))
        },
        onClickRestartLogcat = {
          showDropDownMenu = false
          if (logcatService != null) {
            isLogcatSessionLoading = true
            logcatService.restartLogcatSession()
          }
        },
        onClickSettings = {
          showDropDownMenu = false
          context.startActivity(Intent(context, SettingsActivity::class.java))
        },
      )
      AnimatedVisibility(
        visible = showSearchBar,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        SearchLogsTopBar(
          searchQuery = searchQuery,
          searchInProgress = searchInProgress,
          showHitCount = showHitCount,
          hitCount = searchHitsMap.size,
          currentHitIndex = currentSearchHitIndex,
          onQueryChange = { searchQuery = it },
          onClose = {
            showSearchBar = false
            searchHitsMap.clear()
            sortedHitsByLogIdsState = emptyList()
            currentSearchHitIndex = -1
            currentSearchHitLogId = -1
            focusManager.clearFocus()
            searchQuery = ""
          },
          onPrevious = {
            focusManager.clearFocus()
            if (currentSearchHitIndex - 1 >= 0) {
              currentSearchHitIndex -= 1
            } else {
              currentSearchHitIndex = searchHitsMap.size - 1
            }
          },
          onNext = {
            focusManager.clearFocus()
            currentSearchHitIndex = (currentSearchHitIndex + 1) % searchHitsMap.size
          }
        )
      }
    },
    floatingActionButton = {
      FloatingActionButtons(
        visible = snapScrollInfo.isScrollSnapperVisible,
        scrollToTopInteractionSource = scrollToTopInteractionSource,
        scrollToBottomInteractionSource = scrollToBottomInteractionSource,
        onClickScrollToTop = {
          coroutineScope.launch {
            lazyListState.scrollToItem(0)
          }
        },
        onClickScrollToBottom = {
          coroutineScope.launch {
            if (lazyListState.layoutInfo.totalItemsCount > 0) {
              if (!showSearchBar || searchQuery.isEmpty()) {
                snapToBottom = true
              }
              lazyListState.scrollToItem(lazyListState.layoutInfo.totalItemsCount - 1)
            }
          }
        },
      )
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
                  val (map, sortedHitsByLogId) = searchLogs(logs, searchQuery)
                  searchHitsMap.clear()
                  searchHitsMap.putAll(map)
                  sortedHitsByLogIdsState = sortedHitsByLogId

                  if (!scrolled) {
                    searchInProgress = false
                    if (sortedHitsByLogIdsState.isNotEmpty()) {
                      currentSearchHitIndex = 0
                      currentSearchHitLogId = sortedHitsByLogIdsState.first().logId
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
                currentSearchHitLogId = hits[index].logId
                val scrollIndex = hits[index].index
                if (scrollIndex != -1 && scrollIndex < lazyListState.layoutInfo.totalItemsCount) {
                  lazyListState.scrollToItem(scrollIndex)
                }
              }
            }
        }
      }
    }

    if (savedLogsSheetState is SavedLogsBottomSheetState.Show) {
      val saveInfo = savedLogsSheetState as SavedLogsBottomSheetState.Show
      SavedLogsBottomSheet(
        fileName = saveInfo.fileName,
        uri = saveInfo.uri,
        isCustomLocation = saveInfo.isCustomLocation,
        onDismiss = {
          savedLogsSheetState = SavedLogsBottomSheetState.Hide
        },
      )
    }

    MaybeShowPermissionRequiredDialog()

    if (isLogcatSessionLoading) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .consumeWindowInsets(innerPadding),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(48.dp),
        )
      }
    } else if (errorStartingLogcat) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .consumeWindowInsets(innerPadding),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            modifier = Modifier.size(32.dp),
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
          )
          Spacer(Modifier.height(8.dp))
          Text(
            text = stringResource(R.string.unable_to_start_logcat_error_msg),
            style = AppTypography.bodyMedium,
          )
        }
      }
    } else {
      var showCopyToClipboardSheet by remember { mutableStateOf<Log?>(null) }
      var showLongClickOptionsSheet by remember { mutableStateOf<Log?>(null) }

      if (logsState.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(innerPadding),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              modifier = Modifier.size(32.dp),
              imageVector = Icons.Default.Info,
              contentDescription = null,
            )
            Spacer(Modifier.height(8.dp))
            Text(
              text = stringResource(R.string.waiting_for_logs),
              style = AppTypography.bodyMedium,
            )
          }
        }
      } else {
        val lifecycle = LocalLifecycleOwner.current.lifecycle

        if (snapToBottom) {
          LaunchedEffect(lazyListState, compactViewPreference) {
            if (lazyListState.layoutInfo.totalItemsCount > 0) {
              lazyListState.scrollToItem(lazyListState.layoutInfo.totalItemsCount)
            }
          }
        }

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
          listStyle = if (!showSearchBar && compactViewPreference.value) {
            LogsListStyle.Compact
          } else {
            LogsListStyle.Default
          },
          logs = logsState,
          searchHits = searchHitsMap,
          onClick = if (!compactViewPreference.value) {
            { index ->
              showCopyToClipboardSheet = logsState[index]
              snapToBottom = false
            }
          } else null,
          onLongClick = { index ->
            showLongClickOptionsSheet = logsState[index]
            snapToBottom = false
          },
          state = lazyListState,
          currentSearchHitLogId = currentSearchHitLogId,
        )

        showCopyToClipboardSheet?.let { log ->
          CopyLogClipboardBottomSheet(
            log = log,
            onDismiss = { showCopyToClipboardSheet = null },
          )
        }
      }

      showLongClickOptionsSheet?.let { log ->
        LongClickOptionsSheet(
          showCopyToClipboard = compactViewPreference.value,
          onDismiss = { showLongClickOptionsSheet = null },
          onClickFilter = {
            val intent = Intent(context, FiltersActivity::class.java)
            intent.putExtra(FiltersActivity.EXTRA_LOG, log)
            intent.putExtra(FiltersActivity.EXTRA_EXCLUDE, false)
            context.startActivity(intent)
            showLongClickOptionsSheet = null
          },
          onClickExclude = {
            val intent = Intent(context, FiltersActivity::class.java)
            intent.putExtra(FiltersActivity.EXTRA_LOG, log)
            intent.putExtra(FiltersActivity.EXTRA_EXCLUDE, true)
            context.startActivity(intent)
            showLongClickOptionsSheet = null
          },
          onClickCopyToClipboard = {
            showCopyToClipboardSheet = log
            showLongClickOptionsSheet = null
          }
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LongClickOptionsSheet(
  showCopyToClipboard: Boolean,
  onDismiss: () -> Unit,
  onClickFilter: () -> Unit,
  onClickExclude: () -> Unit,
  onClickCopyToClipboard: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    if (showCopyToClipboard) {
      ListItem(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            onClickCopyToClipboard()
          },
        headlineContent = {
          Text(stringResource(R.string.copy_to_clipboard))
        },
        colors = ListItemDefaults.colors(
          containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
      )
    }
    ListItem(
      modifier = Modifier
        .fillMaxWidth()
        .clickable {
          onClickFilter()
        },
      headlineContent = {
        Text(stringResource(R.string.filter))
      },
      colors = ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
      ),
    )
    ListItem(
      modifier = Modifier
        .fillMaxWidth()
        .clickable {
          onClickExclude()
        },
      headlineContent = {
        Text(stringResource(R.string.exclude))
      },
      colors = ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
      ),
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedLogsBottomSheet(
  fileName: String,
  uri: Uri,
  isCustomLocation: Boolean,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    ListItem(
      modifier = Modifier.fillMaxWidth(),
      headlineContent = {
        Text(
          modifier = Modifier.weight(1f),
          text = stringResource(R.string.saved_as_filename).format(fileName),
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
          onDismiss()
          val intent = Intent(context, SavedLogsViewerActivity::class.java)
          intent.setDataAndType(uri, "text/plain")
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
          onDismiss()
          ShareUtils.shareSavedLogs(
            context = context,
            uri = uri,
            isCustom = isCustomLocation,
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

@Composable
private fun FloatingActionButtons(
  visible: Boolean,
  scrollToTopInteractionSource: MutableInteractionSource,
  scrollToBottomInteractionSource: MutableInteractionSource,
  onClickScrollToTop: () -> Unit,
  onClickScrollToBottom: () -> Unit,
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
    exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
  ) {
    Column {
      FloatingActionButton(
        modifier = Modifier.size(48.dp),
        onClick = onClickScrollToTop,
        interactionSource = scrollToTopInteractionSource,
      ) {
        Icon(
          imageVector = Icons.Filled.ArrowUpward,
          contentDescription = null
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      FloatingActionButton(
        modifier = Modifier.size(48.dp),
        onClick = onClickScrollToBottom,
        interactionSource = scrollToBottomInteractionSource,
      ) {
        Icon(
          imageVector = Icons.Filled.ArrowDownward,
          contentDescription = null
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBar(
  title: String,
  subtitle: String,
  filtered: Boolean,
  isPaused: Boolean,
  pauseEnabled: Boolean,
  recordEnabled: Boolean,
  recordStatus: RecordStatus,
  showDropDownMenu: Boolean,
  saveEnabled: Boolean,
  compactViewEnabled: Boolean,
  saveLogsInProgress: Boolean,
  restartLogcatEnabled: Boolean,
  onClickSearch: () -> Unit,
  onClickPause: () -> Unit,
  onClickRecord: () -> Unit,
  onShowDropdownMenu: () -> Unit,
  onDismissDropdownMenu: () -> Unit,
  onClickCompactView: () -> Unit,
  onClickClear: () -> Unit,
  onClickFilter: () -> Unit,
  onClickSave: () -> Unit,
  onClickSavedLogs: () -> Unit,
  onClickRestartLogcat: () -> Unit,
  onClickSettings: () -> Unit,
) {
  TopAppBar(
    title = {
      Column {
        Text(
          text = title,
          overflow = TextOverflow.Ellipsis,
          maxLines = 1,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (filtered) {
            Icon(
              modifier = Modifier.size(16.dp),
              imageVector = Icons.Default.FilterList,
              contentDescription = null,
            )
            Spacer(modifier = Modifier.width(4.dp))
          }
          Text(
            text = subtitle,
            style = AppTypography.titleSmall,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
          )
        }
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer,
      titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ),
    actions = {
      IconButton(
        onClick = onClickSearch,
        colors = IconButtonDefaults.iconButtonColors(
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
      ) {
        Icon(Icons.Default.Search, contentDescription = null)
      }
      IconButton(
        onClick = onClickPause,
        enabled = pauseEnabled,
        colors = IconButtonDefaults.iconButtonColors(
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
      ) {
        if (isPaused) {
          Icon(Icons.Default.PlayArrow, contentDescription = null)
        } else {
          Icon(Icons.Default.Pause, contentDescription = null)
        }
      }

      IconButton(
        onClick = onClickRecord,
        enabled = recordEnabled,
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
          RecordStatus.SaveRecordedLogs -> {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp,
            )
          }
        }
      }
      Box {
        IconButton(
          onClick = onShowDropdownMenu,
          colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
        ) {
          Icon(Icons.Default.MoreVert, contentDescription = null)
        }
        DropdownMenu(
          expanded = showDropDownMenu,
          onDismissRequest = onDismissDropdownMenu,
        ) {
          DropdownMenuItem(
            leadingIcon = {
              Icon(Icons.Default.ViewCompact, contentDescription = null)
            },
            text = {
              Text(
                text = stringResource(R.string.compact_view),
              )
            },
            trailingIcon = {
              Checkbox(checked = compactViewEnabled, onCheckedChange = null)
            },
            onClick = onClickCompactView,
          )
          DropdownMenuItem(
            leadingIcon = {
              Icon(Icons.Default.Clear, contentDescription = null)
            },
            text = {
              Text(
                text = stringResource(R.string.clear),
              )
            },
            onClick = onClickClear,
          )
          DropdownMenuItem(
            leadingIcon = {
              Icon(Icons.Default.FilterList, contentDescription = null)
            },
            text = {
              Text(
                text = stringResource(R.string.filters),
              )
            },
            onClick = onClickFilter,
          )
          DropdownMenuItem(
            leadingIcon = {
              if (saveLogsInProgress) {
                CircularProgressIndicator(
                  modifier = Modifier.size(24.dp),
                  strokeWidth = 2.dp,
                )
              } else {
                Icon(Icons.Default.Save, contentDescription = null)
              }
            },
            text = {
              Text(
                text = stringResource(R.string.save),
              )
            },
            onClick = onClickSave,
            enabled = saveEnabled,
          )
          DropdownMenuItem(
            leadingIcon = {
              Icon(Icons.Default.FolderOpen, contentDescription = null)
            },
            text = {
              Text(
                text = stringResource(R.string.saved_logs),
              )
            },
            onClick = onClickSavedLogs,
          )
          DropdownMenuItem(
            leadingIcon = {
              Icon(Icons.Default.RestartAlt, contentDescription = null)
            },
            text = {
              Text(
                text = stringResource(R.string.restart_logcat),
              )
            },
            onClick = onClickRestartLogcat,
            enabled = restartLogcatEnabled,
          )
          DropdownMenuItem(
            leadingIcon = {
              Icon(Icons.Default.Settings, contentDescription = null)
            },
            text = {
              Text(
                text = stringResource(R.string.settings),
              )
            },
            onClick = onClickSettings,
          )
        }
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaybeShowPermissionRequiredDialog() {
  val context = LocalContext.current
  val permissionGranted = remember(context) {
    isReadLogsPermissionGranted(context)
  }
  var showPermissionRequiredDialog by remember(permissionGranted) {
    mutableStateOf(!permissionGranted)
  }
  var showAskingForRootPermissionDialog by remember { mutableStateOf(false) }
  var showRestartAppDialog by remember { mutableStateOf(false) }
  var showManualMethodDialog by remember { mutableStateOf(false) }
  val coroutineScope = rememberCoroutineScope()

  if (showPermissionRequiredDialog) {
    val failMessage = stringResource(R.string.fail)
    Dialog(
      modifier = Modifier.fillMaxWidth(),
      confirmButton = {
        TextButton(
          onClick = {
            showPermissionRequiredDialog = false
            showManualMethodDialog = true
          }
        ) {
          Text(stringResource(R.string.manual_method))
        }
      },
      onDismissRequest = {
        showPermissionRequiredDialog = false
      },
      title = { Text(stringResource(R.string.read_logs_permission_required)) },
      content = { Text(stringResource(R.string.read_logs_permission_required_msg)) },
      dismissButton = {
        TextButton(
          onClick = {
            showPermissionRequiredDialog = false
            showAskingForRootPermissionDialog = true
            coroutineScope.launch {
              val result = withContext(Dispatchers.IO) {
                val cmd = "pm grant ${BuildConfig.APPLICATION_ID} ${Manifest.permission.READ_LOGS}"
                SuCommander(cmd).run()
              }
              showAskingForRootPermissionDialog = false
              if (result) {
                showRestartAppDialog = true
              } else {
                showManualMethodDialog = true
                context.showToast(failMessage)
              }
            }
          }
        ) {
          Text(stringResource(R.string.root_method))
        }
      },
      icon = {
        Icon(Icons.Default.Info, contentDescription = null)
      }
    )
  } else if (permissionGranted) {
    val showPermissionInfoDialog = rememberBooleanSharedPreference(
      key = SHOW_PERMISSION_GRANTED_INFO_PREF_KEY,
      default = true,
    )
    if (showPermissionInfoDialog.value) {
      Dialog(
        onDismissRequest = {
          showPermissionInfoDialog.value = false
        },
        title = {
          Text(stringResource(R.string.permission_granted_info_title))
        },
        content = {
          Text(stringResource(R.string.permission_granted_info_body))
        },
        confirmButton = {
          TextButton(
            onClick = { showPermissionInfoDialog.value = false },
          ) {
            Text(stringResource(android.R.string.ok))
          }
        }
      )
    }
  }

  if (showAskingForRootPermissionDialog) {
    Dialog(
      onDismissRequest = {},
      icon = {
        CircularProgressIndicator(
          modifier = Modifier.size(24.dp),
          strokeWidth = 2.dp,
        )
      },
      content = {
        Text(stringResource(R.string.asking_permission_for_root_access))
      }
    )
  }

  if (showRestartAppDialog) {
    Dialog(
      onDismissRequest = {
        showRestartAppDialog = false
      },
      title = {
        Text(stringResource(R.string.app_restart_dialog_title))
      },
      content = {
        Text(stringResource(R.string.app_restart_dialog_msg_body))
      },
      confirmButton = {
        TextButton(
          onClick = {
            context.stopService(Intent(context, LogcatService::class.java))
            Process.killProcess(Process.myPid())
          }
        ) {
          Text(stringResource(android.R.string.ok))
        }
      }
    )
  }

  if (showManualMethodDialog) {
    Dialog(
      onDismissRequest = {
        showManualMethodDialog = false
      },
      title = {
        Text(stringResource(R.string.manual_method))
      },
      content = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
          Text(
            text = buildAnnotatedString {
              append(stringResource(R.string.permission_instruction0))
              appendLine(); appendLine()
              append(stringResource(R.string.permission_instruction1))
              appendLine()
              append(stringResource(R.string.permission_instruction2))
              appendLine()
              append(
                AnnotatedString(
                  text = stringResource(R.string.permission_instruction3),
                  spanStyle = SpanStyle(
                    color = MaterialTheme.colorScheme.tertiary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                  )
                )
              )
              appendLine()
              append(stringResource(R.string.permission_instruction4))
              appendLine()
              append(stringResource(R.string.permission_instruction5))
              appendLine(); appendLine()
              append(stringResource(R.string.permission_instruction6))
            },
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            showManualMethodDialog = false
            val cmd = "adb shell pm grant ${BuildConfig.APPLICATION_ID} " +
              Manifest.permission.READ_LOGS
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE)
              as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Adb command", cmd))
          }
        ) {
          Text(stringResource(R.string.copy_adb_command))
        }
      }
    )
  }
}

@Composable
private fun rememberLogcatServiceConnection(): LogcatService? {
  var logcatService by remember { mutableStateOf<LogcatService?>(null) }
  val context = LocalContext.current
  DisposableEffect(context) {
    val serviceConnection = object : ServiceConnection {
      override fun onServiceConnected(
        name: ComponentName?,
        service: IBinder,
      ) {
        Logger.debug(TAG, "LogcatService - onServiceConnected")
        logcatService = service.getService()
      }

      override fun onServiceDisconnected(name: ComponentName) {
        Logger.debug(TAG, "LogcatService - onServiceDisconnected")
        logcatService = null
      }
    }

    Logger.debug(TAG, "LogcatService - bind")
    context.bindService(
      Intent(context, LogcatService::class.java),
      serviceConnection,
      Context.BIND_ABOVE_CLIENT,
    )

    onDispose {
      Logger.debug(TAG, "LogcatService - unbind")
      context.unbindService(serviceConnection)
      logcatService = null
    }
  }

  return logcatService
}

data class SnapScrollInfo(
  val isScrollSnapperVisible: Boolean = false,
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

  data object Failure : SaveResult
}

private suspend fun createFileToStartRecording(context: Context): RecordingFileInfo? {
  val (uri, isCustomLocation) = withContext(Dispatchers.IO) {
    createFile(context)
  }

  if (uri == null) return null

  val fileName = if (isCustomLocation) {
    DocumentFile.fromSingleUri(context, uri)?.name
  } else {
    uri.toFile().name
  }

  if (fileName == null) return null

  val db = LogcatReaderDatabase.getInstance(context)
  withContext(Dispatchers.IO) {
    db.savedLogsDao().insert(
      SavedLogInfo(
        fileName = fileName,
        path = uri.toString(),
        isCustom = isCustomLocation,
      )
    )
  }

  return RecordingFileInfo(
    fileName = fileName,
    uri = uri,
    isCustomLocation = isCustomLocation
  )
}

private suspend fun RecordingFileInfo.createBufferedWriter(
  context: Context
): BufferedWriter? = withContext(Dispatchers.IO) {
  try {
    if (isCustomLocation) {
      context.contentResolver.openOutputStream(uri)?.bufferedWriter()
    } else {
      uri.toFile().bufferedWriter()
    }
  } catch (_: IOException) {
    null
  }
}

private fun saveLogsToFile(context: Context, logs: List<Log>): Flow<SaveResult> = flow {
  emit(SaveResult.InProgress)
  check(logs.isNotEmpty()) { "logs list is empty" }

  val (uri, isUsingCustomLocation) = withContext(Dispatchers.IO) { createFile(context) }
  if (uri == null) {
    emit(SaveResult.Failure)
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
      emit(SaveResult.Failure)
      return@flow
    }

    val db = LogcatReaderDatabase.getInstance(context)
    withContext(Dispatchers.IO) {
      db.savedLogsDao().insert(
        SavedLogInfo(
          fileName = fileName,
          path = uri.toString(),
          isCustom = isUsingCustomLocation,
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
    emit(SaveResult.Failure)
  }
}

// Returns a pair of Uri and custom save location flag (true if custom save location is used).
@WorkerThread
private fun createFile(context: Context): Pair<Uri?, Boolean> {
  val timeStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
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

enum class RecordStatus {
  Idle,
  RecordingInProgress,
  SaveRecordedLogs,
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
  private val filterInfo: FilterInfo,
) : Filter {
  private val priorities: Set<String> = if (!filterInfo.logLevels.isNullOrEmpty()) {
    filterInfo.logLevels.split(",").toSet()
  } else {
    emptySet()
  }

  private fun matches(keyword: String?, target: String): Boolean {
    return if (keyword.isNullOrEmpty()) {
      true
    } else {
      target.contains(keyword, ignoreCase = true)
    }
  }

  private fun matchesPriority(log: Log): Boolean {
    return if (priorities.isEmpty()) {
      true
    } else {
      log.priority in priorities
    }
  }

  override fun apply(log: Log): Boolean {
    if (!matches(keyword = filterInfo.tag, target = log.tag)) {
      return false
    }

    if (!matches(keyword = filterInfo.message, target = log.msg)) {
      return false
    }

    if (!matches(keyword = filterInfo.pid?.toString(), target = log.pid)) {
      return false
    }

    if (!matches(keyword = filterInfo.tid?.toString(), target = log.tid)) {
      return false
    }

    if (!matchesPriority(log)) {
      return false
    }

    return true
  }
}

private fun isReadLogsPermissionGranted(context: Context): Boolean {
  return ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.READ_LOGS
  ) == PackageManager.PERMISSION_GRANTED
}
