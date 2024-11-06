package com.dp.logcatapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dp.logcat.Log
import com.dp.logcat.LogPriority
import com.dp.logcatapp.ui.theme.LogPriorityColors
import com.dp.logcatapp.ui.theme.LogcatReaderTheme
import com.dp.logcatapp.ui.theme.RobotoMonoFontFamily
import com.dp.logger.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

const val SNAP_SCROLL_HIDE_DELAY_MS = 2000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  modifier: Modifier,
) {
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = {
          Text(text = "Logcat Reader")
        },
        colors = TopAppBarDefaults.topAppBarColors()
          .copy(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          )
      )
    }
  ) { padding ->
    val logs = remember { mutableStateListOf<Log>() }
    val state = rememberLazyListState()

    // TODO: use actual logs.
    LaunchedEffect(Unit) {
      launch {
        var i = 0
        while (i < 50) {
          logs += Log(
            id = i,
            priority = "D",
            tag = "Tag",
            msg = "This is a log - $i",
            date = "01-12",
            time = "21:10:46.123",
            pid = "1600",
            tid = "123123",
          )
          delay(100)
          i += 1
        }
      }
    }

    LogsList(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      logs = logs,
      onClick = {},
      onLongClick = {},
      state = state,
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LogsList(
  modifier: Modifier,
  logs: List<Log>,
  onClick: (Int) -> Unit,
  onLongClick: (Int) -> Unit,
  state: LazyListState = rememberLazyListState(),
) {
  Box(
    modifier = modifier,
  ) {
    var isScrollDownVisible by remember { mutableStateOf(false) }
    var isScrollUpVisible by remember { mutableStateOf(false) }
    var snapToBottom by remember { mutableStateOf(true) }

    if (snapToBottom) {
      LaunchedEffect(state) {
        isScrollDownVisible = false
        isScrollUpVisible = false
        snapshotFlow { state.layoutInfo.totalItemsCount }
          .filter { lastIndex -> lastIndex > 0 }
          .collect { lastIndex ->
            state.scrollToItem(lastIndex)
          }
      }
    } else {
      LaunchedEffect(state) {
        data class ItemOffsetInfo(
          val viewportEndOffset: Int,
          val isLastItem: Boolean,
          val size: Int,
          val offset: Int,
        )
        launch {
          snapshotFlow {
            val layoutInfo = state.layoutInfo
            layoutInfo.visibleItemsInfo.lastOrNull()?.let { info ->
              ItemOffsetInfo(
                viewportEndOffset = layoutInfo.viewportEndOffset,
                isLastItem = info.index == layoutInfo.totalItemsCount - 1,
                size = info.size,
                offset = info.offset,
              )
            }
          }
            .filterNotNull()
            .map { offsetInfo ->
              Logger.debug("darshan", offsetInfo.toString())
              !offsetInfo.isLastItem ||
                (offsetInfo.offset + offsetInfo.size) > offsetInfo.viewportEndOffset
            }
            .collectLatest { canScrollDown ->
              isScrollDownVisible = canScrollDown
              if (isScrollDownVisible) {
                delay(SNAP_SCROLL_HIDE_DELAY_MS)
                isScrollDownVisible = false
              }
            }
        }

        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
          .map { (index, offset) ->
            index != 0 || offset > 0
          }
          .collectLatest { canScrollUp ->
            isScrollUpVisible = canScrollUp
            if (isScrollUpVisible) {
              delay(SNAP_SCROLL_HIDE_DELAY_MS)
              isScrollUpVisible = false
            }
          }
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      state = state,
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
            .pointerInput(Unit) {
              awaitPointerEventScope {
                when (currentEvent.type) {
                  PointerEventType.Press -> {
                    snapToBottom = false
                  }
                }
              }
            }
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

    val coroutineScope = rememberCoroutineScope()

    AnimatedVisibility(
      visible = isScrollUpVisible,
      modifier = Modifier
        .padding(16.dp)
        .align(Alignment.TopEnd),
      enter = fadeIn(),
      exit = fadeOut(),
    ) {
      FloatingActionButton(
        modifier = Modifier.size(48.dp),
        onClick = {
          isScrollUpVisible = false
          coroutineScope.launch {
            state.scrollToItem(0)
          }
        }
      ) {
        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null)
      }
    }

    AnimatedVisibility(
      visible = isScrollDownVisible,
      modifier = Modifier
        .padding(16.dp)
        .align(Alignment.BottomEnd),
      enter = fadeIn(),
      exit = fadeOut(),
    ) {
      FloatingActionButton(
        modifier = Modifier.size(48.dp),
        onClick = {
          coroutineScope.launch {
            if (state.layoutInfo.totalItemsCount > 0) {
              snapToBottom = true
              state.scrollToItem(state.layoutInfo.totalItemsCount - 1)
            }
          }
        }
      ) {
        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
      }
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