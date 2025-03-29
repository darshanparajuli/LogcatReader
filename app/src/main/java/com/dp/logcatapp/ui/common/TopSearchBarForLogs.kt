package com.dp.logcatapp.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dp.logcatapp.ui.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchLogsTopBar(
  searchQuery: String,
  searchInProgress: Boolean,
  showHitCount: Boolean,
  hitCount: Int,
  currentHitIndex: Int,
  onQueryChange: (String) -> Unit,
  onClose: () -> Unit,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
) {
  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(focusRequester) {
    focusRequester.requestFocus()
  }

  TopAppBar(
    modifier = Modifier.fillMaxWidth(),
    navigationIcon = {
      IconButton(
        onClick = onClose,
        colors = IconButtonDefaults.iconButtonColors(
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
      ) {
        Icon(imageVector = Icons.Default.Close, contentDescription = null)
      }
    },
    title = {
      TextField(
        modifier = Modifier.focusRequester(focusRequester),
        value = searchQuery,
        onValueChange = onQueryChange,
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
            val current = currentHitIndex.takeIf { it != -1 }?.let { it + 1 } ?: 0
            Text(
              text = "$current/$hitCount",
              style = AppTypography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      )
    },
    actions = {
      IconButton(
        onClick = onPrevious,
        enabled = hitCount > 0,
        colors = IconButtonDefaults.iconButtonColors(
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
      ) {
        Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = null)
      }
      IconButton(
        onClick = onNext,
        enabled = hitCount > 0,
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