package com.dp.logcatapp.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastForEachIndexed
import com.dp.logcat.Log
import com.dp.logcatapp.R
import com.dp.logcatapp.ui.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyLogClipboardBottomSheet(
  log: Log,
  onDismiss: () -> Unit,
) {
  val clipboard = LocalClipboardManager.current
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
  ) {
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
    val labels = listOf(
      stringResource(R.string.tag),
      stringResource(R.string.message),
      stringResource(R.string.date),
      stringResource(R.string.time),
      stringResource(R.string.process_id),
      stringResource(R.string.thread_id),
    )
    val data = listOf(
      log.tag,
      log.msg,
      log.date,
      log.time,
      log.pid,
      log.tid,
    )
    labels.zip(data).fastForEachIndexed { index, (label, data) ->
      ListItem(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            clipboard.setText(AnnotatedString(data))
            onDismiss()
          },
        headlineContent = {
          Text(label)
        },
        supportingContent = {
          Text(
            text = data,
            maxLines = 1,
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