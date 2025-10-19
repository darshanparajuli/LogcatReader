package com.dp.logcatapp.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dp.logcatapp.R
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
  regexEnabled: Boolean,
  regexError: Boolean,
  onClickRegex: () -> Unit,
  showSearchNav: Boolean = true,
  showRegexOption: Boolean = true,
) {
  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(focusRequester) {
    focusRequester.requestFocus()
  }

  TopAppBar(
    modifier = Modifier.fillMaxWidth(),
    navigationIcon = {
      WithTooltip(
        modifier = Modifier.windowInsetsPadding(
          WindowInsets.safeDrawing.only(WindowInsetsSides.Start)
        ),
        text = stringResource(R.string.close),
      ) {
        IconButton(
          onClick = onClose,
          colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
        ) {
          Icon(imageVector = Icons.Default.Close, contentDescription = null)
        }
      }
    },
    title = {
      TextField(
        modifier = Modifier
          .fillMaxWidth()
          .focusRequester(focusRequester),
        value = searchQuery,
        onValueChange = onQueryChange,
        maxLines = 1,
        singleLine = true,
        isError = regexError,
        placeholder = {
          Text(text = stringResource(R.string.search))
        },
        colors = TextFieldDefaults.colors(
          focusedContainerColor = Color.Transparent,
          unfocusedContainerColor = Color.Transparent,
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent,
          errorIndicatorColor = Color.Transparent,
          focusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
          focusedSuffixColor = MaterialTheme.colorScheme.onPrimaryContainer,
          unfocusedSuffixColor = MaterialTheme.colorScheme.onPrimaryContainer,
          unfocusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
          errorContainerColor = Color.Transparent,
          errorLabelColor = Color.Transparent,
          errorTextColor = MaterialTheme.colorScheme.error,
          errorSuffixColor = MaterialTheme.colorScheme.error,
          errorTrailingIconColor = Color.Transparent,
          errorLeadingIconColor = Color.Transparent,
          errorPlaceholderColor = MaterialTheme.colorScheme.error,
        ),
        textStyle = LocalTextStyle.current.copy(
          fontSize = 18.sp,
        ),
        suffix = if (searchInProgress || showHitCount) {
          {
            if (searchInProgress) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
              )
            } else if (showHitCount) {
              val current = currentHitIndex.takeIf { it != -1 }?.let { it + 1 } ?: 0
              Text(
                text = "$current/$hitCount",
                style = LocalTextStyle.current.merge(AppTypography.bodySmall),
              )
            }
          }
        } else null,
        trailingIcon = if (showRegexOption) {
          {
            val textButtonColors = ButtonDefaults.textButtonColors()
            WithTooltip(
              text = stringResource(R.string.regex)
            ) {
              TextButton(
                onClick = onClickRegex,
                colors = ButtonDefaults.textButtonColors(
                  contentColor = if (regexEnabled) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                  } else {
                    textButtonColors.disabledContentColor
                  },
                ),
                contentPadding = PaddingValues(),
              ) {
                Text(
                  text = ".*",
                )
              }
            }
          }
        } else null,
      )
    },
    actions = {
      if (showSearchNav) {
        Row(
          modifier = Modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.End)
          ),
        ) {
          WithTooltip(
            text = stringResource(R.string.previous),
          ) {
            IconButton(
              onClick = onPrevious,
              enabled = hitCount > 0,
              colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
            ) {
              Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = null)
            }
          }
          WithTooltip(
            text = stringResource(R.string.next),
          ) {
            IconButton(
              onClick = onNext,
              enabled = hitCount > 0,
              colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
            ) {
              Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
            }
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