package com.dp.logcatapp.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
      listOf(
        R.string.log to log.toString(),
        R.string.tag to log.tag,
        R.string.message to log.msg,
        R.string.date to log.date,
        R.string.time to log.time,
        R.string.process_id to log.pid,
        R.string.thread_id to log.tid,
      ).forEach { (labelRes, data) ->
        ListItem(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              clipboard.setText(AnnotatedString(data))
              onDismiss()
            },
          headlineContent = {
            Text(stringResource(labelRes))
          },
          supportingContent = {
            Text(
              text = data,
              maxLines = if (labelRes == R.string.log) 2 else 1,
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