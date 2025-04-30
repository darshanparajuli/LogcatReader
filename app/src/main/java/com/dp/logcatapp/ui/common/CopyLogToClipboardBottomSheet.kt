package com.dp.logcatapp.ui.common

import android.content.ClipData
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastForEach
import com.dp.logcat.Log
import com.dp.logcatapp.R
import com.dp.logcatapp.ui.theme.AppTypography
import com.dp.logcatapp.util.rememberAppInfoByUidMap
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyLogClipboardBottomSheet(
  log: Log,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val appInfo = rememberAppInfoByUidMap()
  val coroutineScope = rememberCoroutineScope()
  val clipboard = LocalClipboard.current
  val clipLabel = stringResource(R.string.app_name)
  ModalBottomSheet(
    modifier = modifier.statusBarsPadding(),
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
      ListItem(
        modifier = Modifier.fillMaxWidth(),
        headlineContent = {
          Text(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.copy_to_clipboard),
            style = AppTypography.titleMedium,
          )
        },
        colors = ListItemDefaults.colors(
          containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
      )
      val logData = remember(log, appInfo) {
        listOfNotNull(
          R.string.log to log.toString(),
          R.string.tag to log.tag,
          R.string.message to log.msg,
          R.string.date to log.date,
          log.uid?.let { uid ->
            R.string.package_name to (appInfo?.get(uid)?.packageName ?: uid)
          },
          R.string.time to log.time,
          R.string.process_id to log.pid,
          R.string.thread_id to log.tid,
        )
      }
      logData.fastForEach { (labelRes, data) ->
        ListItem(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              coroutineScope.launch {
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(clipLabel, data)))
                onDismiss()
              }
            },
          headlineContent = {
            Text(stringResource(labelRes))
          },
          supportingContent = {
            val maxLines = when (labelRes) {
              R.string.log -> 3
              R.string.message -> 4
              else -> 1
            }
            Text(
              text = data,
              maxLines = maxLines,
              overflow = TextOverflow.Ellipsis,
            )
          },
          colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
          ),
        )
      }
    }
  }
}