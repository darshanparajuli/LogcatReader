package com.dp.logcatapp.ui.screens

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.alpha
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dp.logcat.Log
import com.dp.logcat.LogPriority
import com.dp.logcatapp.R
import com.dp.logcatapp.services.LogcatService
import com.dp.logcatapp.services.getService
import com.dp.logcatapp.ui.screens.SearchHitKey.LogComponent
import com.dp.logcatapp.ui.theme.AppTypography
import com.dp.logcatapp.ui.theme.LogPriorityColors
import com.dp.logcatapp.ui.theme.LogcatReaderTheme
import com.dp.logcatapp.ui.theme.RobotoMonoFontFamily
import com.dp.logcatapp.ui.theme.currentSearchHitColor
import com.dp.logcatapp.util.ServiceBinder
import com.dp.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "HomeScreen"
private const val SNAP_SCROLL_HIDE_DELAY_MS = 2000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  modifier: Modifier,
) {

  val lazyListState = rememberLazyListState()
  val logsState = remember { mutableStateListOf<Log>() }
  val coroutineScope = rememberCoroutineScope()
  var snapToBottom by remember { mutableStateOf(true) }
  var showDropDownMenu by remember { mutableStateOf(false) }
  var showSearchBar by remember { mutableStateOf(false) }
  var recordLogs by remember { mutableStateOf(false) }
  var logcatPaused by remember { mutableStateOf(false) }
  val snapUpInteractionSource = remember { MutableInteractionSource() }
  val snapDownInteractionSource = remember { MutableInteractionSource() }
  var searchQuery by remember { mutableStateOf("") }
  val searchHits = remember { mutableStateMapOf<SearchHitKey, Pair<Int, Int>>() }
  var currentSearchHitIndex by remember { mutableIntStateOf(-1) }
  var currentSearchHitLogId by remember { mutableIntStateOf(-1) }
  var showHitCount by remember { mutableStateOf(false) }
  val snapScrollInfo = rememberSnapScrollInfo(
    lazyListState = lazyListState,
    snapToBottom = snapToBottom,
    snapUpInteractionSource = snapUpInteractionSource,
    snapDownInteractionSource = snapDownInteractionSource,
  )

  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Logcat Reader",
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
            }
          ) {
            Icon(Icons.Default.Search, contentDescription = null)
          }
          IconButton(
            onClick = {
              logcatPaused = !logcatPaused
            }
          ) {
            if (logcatPaused) {
              Icon(Icons.Default.PlayArrow, contentDescription = null)
            } else {
              Icon(Icons.Default.Pause, contentDescription = null)
            }
          }
          IconButton(
            onClick = {
              recordLogs = true
            }
          ) {
            Icon(Icons.Default.FiberManualRecord, contentDescription = null)
          }
          Box {
            IconButton(
              onClick = {
                showDropDownMenu = true
              }
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
                  // TODO: also handle exclusions in this screen
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
                }
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
                  // TODO
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
                  // TODO
                }
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
                  // TODO
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
        val focusManager = LocalFocusManager.current
        LaunchedEffect(focusRequester) {
          focusRequester.requestFocus()
        }

        TopAppBar(
          modifier = Modifier.fillMaxWidth(),
          navigationIcon = {
            IconButton(
              onClick = {
                showSearchBar = false
                searchHits.clear()
                currentSearchHitIndex = -1
                currentSearchHitLogId = -1
                searchQuery = ""
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
              ),
              textStyle = LocalTextStyle.current.copy(
                fontSize = 18.sp,
              ),
              suffix = {
                val current = currentSearchHitIndex.takeIf { it != -1 }?.let { it + 1 } ?: 0
                Text(
                  modifier = Modifier.alpha(
                    if (showHitCount) 1f else 0f
                  ),
                  text = "$current/${searchHits.size}",
                  style = AppTypography.bodySmall,
                )
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
                  currentSearchHitIndex = searchHits.size - 1
                }
              },
              enabled = searchHits.isNotEmpty(),
            ) {
              Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = null)
            }
            IconButton(
              onClick = {
                focusManager.clearFocus()
                currentSearchHitIndex = (currentSearchHitIndex + 1) % searchHits.size
              },
              enabled = searchHits.isNotEmpty(),
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
                  if (!showSearchBar) {
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
  ) { innerPadding ->
    val logcatService = rememberLogcatServiceConnection()
    if (logcatService != null) {
      if (!logcatPaused) {
        LaunchedEffect(logcatService) {
          val session = logcatService.logcatSession
          logsState.clear()
          session.logs.collect { logs ->
            logsState += logs
          }
        }
      }
    }

    if (showSearchBar) {
      LaunchedEffect(Unit) {
        snapshotFlow { searchQuery }
          .collectLatest { searchQuery ->
            delay(100L)
            showHitCount = searchQuery.isNotEmpty()
            if (searchQuery.isNotEmpty()) {
              var scrolled = false
              snapshotFlow { logsState.toList() }
                .collect { logs ->
                  val map = withContext(Dispatchers.Default) {
                    val map = mutableMapOf<SearchHitKey, Pair<Int, Int>>()
                    val deferred = logs.map { log ->
                      async {
                        val msgIndex = log.msg.indexOf(string = searchQuery, ignoreCase = true)
                        val tagIndex = log.tag.indexOf(string = searchQuery, ignoreCase = true)
                        Triple(log.id, msgIndex, tagIndex)
                      }
                    }
                    deferred.awaitAll().forEach { (logId, msgIndex, tagIndex) ->
                      if (msgIndex != -1) {
                        map[SearchHitKey(logId = logId, component = LogComponent.MSG)] =
                          Pair(msgIndex, msgIndex + searchQuery.length)
                      }
                      if (tagIndex != -1) {
                        map[SearchHitKey(logId = logId, component = LogComponent.TAG)] =
                          Pair(tagIndex, tagIndex + searchQuery.length)
                      }
                    }
                    map
                  }
                  searchHits.clear()
                  searchHits.putAll(map)

                  if (!scrolled) {
                    if (searchHits.isNotEmpty()) {
                      currentSearchHitIndex = 0
                      currentSearchHitLogId = searchHits.keys.minByOrNull { it.logId }!!.logId
                      snapToBottom = false
                      scrolled = true
                    } else {
                      currentSearchHitIndex = -1
                      currentSearchHitLogId = -1
                    }
                  }
                }
            } else {
              searchHits.clear()
              currentSearchHitIndex = -1
              currentSearchHitLogId = -1
            }
          }
      }
      if (searchQuery.isNotEmpty()) {
        LaunchedEffect(lazyListState, searchQuery) {
          snapshotFlow {
            searchHits.toMap() to currentSearchHitIndex
          }
            .filter { (_, index) -> index != -1 }
            .collectLatest { (hitsMap, index) ->
              val hits = hitsMap.keys.sortedBy { it.logId }
              if (index < hits.size) {
                currentSearchHitLogId = hits[index].logId
                lazyListState.scrollToItem(currentSearchHitLogId)
              }
            }
        }
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
                    }
                  }
                }
              }
            }
          }
        },
      contentPadding = innerPadding,
      logs = logsState,
      searchHits = searchHits,
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

data class SearchHitKey(
  val logId: Int,
  val component: LogComponent,
) {
  enum class LogComponent {
    MSG,
    TAG,
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