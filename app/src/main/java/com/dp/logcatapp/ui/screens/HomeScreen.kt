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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dp.logcat.Log
import com.dp.logcat.LogPriority
import com.dp.logcatapp.R
import com.dp.logcatapp.services.LogcatService
import com.dp.logcatapp.services.getService
import com.dp.logcatapp.ui.theme.AppTypography
import com.dp.logcatapp.ui.theme.LogPriorityColors
import com.dp.logcatapp.ui.theme.LogcatReaderTheme
import com.dp.logcatapp.ui.theme.RobotoMonoFontFamily
import com.dp.logcatapp.util.ServiceBinder
import com.dp.logger.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
              Icons.Filled.KeyboardArrowUp,
              contentDescription = null
            )
          }
          Spacer(modifier = Modifier.height(12.dp))
          FloatingActionButton(
            modifier = Modifier.size(48.dp),
            onClick = {
              coroutineScope.launch {
                if (lazyListState.layoutInfo.totalItemsCount > 0) {
                  snapToBottom = true
                  lazyListState.scrollToItem(lazyListState.layoutInfo.totalItemsCount - 1)
                }
              }
            },
            interactionSource = snapDownInteractionSource,
          ) {
            Icon(
              Icons.Filled.KeyboardArrowDown,
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

    LogsList(
      modifier = Modifier
        .fillMaxSize()
        .consumeWindowInsets(innerPadding)
        .pointerInput(Unit) {
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
        },
      contentPadding = innerPadding,
      logs = logsState,
      onClick = {},
      onLongClick = {},
      state = lazyListState,
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LogsList(
  modifier: Modifier,
  contentPadding: PaddingValues,
  logs: List<Log>,
  onClick: (Int) -> Unit,
  onLongClick: (Int) -> Unit,
  state: LazyListState = rememberLazyListState(),
) {
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
      LogItem(
        modifier = Modifier
          .fillMaxWidth()
          .combinedClickable(
            onLongClick = { onLongClick(index) },
            onClick = { onClick(index) },
          )
          .wrapContentHeight(),
        priority = item.priority,
        tag = item.tag,
        message = item.msg,
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
  tag: String,
  message: String,
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

@Preview(showBackground = true)
@Composable
private fun LogItemPreview() {
  LogcatReaderTheme {
    LogItem(
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight(),
      priority = "D",
      tag = "Tag",
      message = "This is a log",
      date = "01-12",
      time = "21:10:46.123",
      pid = "1600",
      tid = "123123",
      priorityColor = LogPriorityColors.priorityDebug,
    )
  }
}