package com.dp.logcatapp.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.TextFieldColors
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dp.logcatapp.R
import com.dp.logcatapp.ui.theme.AppTypography
import com.dp.logcatapp.ui.theme.LogcatReaderTheme

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
      SearchTextField(
        modifier = Modifier
          .fillMaxWidth()
          .focusRequester(focusRequester),
        value = searchQuery,
        onValueChange = onQueryChange,
        isError = regexError,
        placeholder = stringResource(R.string.search),
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
          cursorColor = MaterialTheme.colorScheme.primary,
          errorCursorColor = MaterialTheme.colorScheme.error,
        ),
        textStyle = LocalTextStyle.current.copy(
          fontSize = 18.sp,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        suffix = {
          if (searchInProgress) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp,
            )
          }
        },
        supportingText = {
          AnimatedVisibility(
            visible = showHitCount,
            enter = fadeIn(
              animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ),
            exit = fadeOut(
              animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ),
            modifier = Modifier.fillMaxWidth(),
          ) {
            val current = currentHitIndex.takeIf { it != -1 }?.let { it + 1 } ?: 0
            Text(
              text = "$current/$hitCount",
              style = LocalTextStyle.current.merge(AppTypography.bodySmall),
            )
          }
        },
        trailingIcon = if (showRegexOption) {
          {
            val textButtonColors = ButtonDefaults.textButtonColors()
            WithTooltip(
              text = stringResource(R.string.regex)
            ) {
              TextButton(
                onClick = onClickRegex,
                modifier = Modifier.size(36.dp),
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
              modifier = Modifier.size(36.dp),
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
              modifier = Modifier.size(36.dp),
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

@Composable
private fun SearchTextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  placeholder: String? = null,
  colors: TextFieldColors = TextFieldDefaults.colors(),
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  isError: Boolean = false,
  suffix: @Composable (() -> Unit)? = null,
  supportingText: @Composable (() -> Unit)? = null,
  trailingIcon: @Composable (() -> Unit)? = null,
  textStyle: TextStyle = LocalTextStyle.current,
) {
  val mergedTextStyle = textStyle.merge(
    if (isError) {
      LocalTextStyle.current.copy(color = colors.errorTextColor)
    } else {
      LocalTextStyle.current
    }
  )
  BasicTextField(
    value = value,
    modifier = modifier,
    onValueChange = onValueChange,
    enabled = true,
    readOnly = false,
    textStyle = mergedTextStyle,
    cursorBrush = SolidColor(if (isError) colors.errorTextColor else colors.cursorColor),
    visualTransformation = VisualTransformation.None,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    interactionSource = interactionSource,
    singleLine = true,
    minLines = 1,
    decorationBox = { innerTextField ->
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Column(
          modifier = Modifier
            .weight(1f)
            .animateContentSize(
              animationSpec = spring(
                stiffness = Spring.StiffnessMedium,
                visibilityThreshold = IntSize.VisibilityThreshold,
              )
            )
        ) {
          Box {
            if (value.isEmpty() && placeholder != null) {
              val isFocused = interactionSource.collectIsFocusedAsState().value
              Text(
                text = placeholder,
                fontSize = textStyle.fontSize,
                color = if (isFocused) {
                  colors.focusedPlaceholderColor
                } else {
                  colors.unfocusedPlaceholderColor
                },
              )
            }
            innerTextField()
          }
          supportingText?.invoke()
        }
        suffix?.invoke()
        trailingIcon?.invoke()
      }
    },
  )
}

@Preview(showBackground = true)
@Composable
private fun SearchLogsTopBarPreview() {
  LogcatReaderTheme {
    SearchLogsTopBar(
      searchQuery = "",
      searchInProgress = false,
      showHitCount = true,
      hitCount = 42,
      currentHitIndex = 5,
      onQueryChange = {},
      onClose = {},
      onPrevious = {},
      onNext = {},
      regexEnabled = false,
      regexError = false,
      onClickRegex = {},
    )
  }
}

@Preview(showBackground = true, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Composable
private fun SearchLogsTopBarDarkModePreview() {
  LogcatReaderTheme {
    SearchLogsTopBar(
      searchQuery = "",
      searchInProgress = false,
      showHitCount = true,
      hitCount = 42,
      currentHitIndex = 5,
      onQueryChange = {},
      onClose = {},
      onPrevious = {},
      onNext = {},
      regexEnabled = false,
      regexError = false,
      onClickRegex = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun SearchLogsTopBarSearchInProgressPreview() {
  LogcatReaderTheme {
    SearchLogsTopBar(
      searchQuery = "searching...",
      searchInProgress = true,
      showHitCount = false,
      hitCount = 0,
      currentHitIndex = -1,
      onQueryChange = {},
      onClose = {},
      onPrevious = {},
      onNext = {},
      regexEnabled = true,
      regexError = false,
      onClickRegex = {},
    )
  }
}

@Preview(showBackground = true, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Composable
private fun SearchLogsTopBarSearchInProgressDarkModePreview() {
  LogcatReaderTheme {
    SearchLogsTopBar(
      searchQuery = "searching...",
      searchInProgress = true,
      showHitCount = false,
      hitCount = 0,
      currentHitIndex = -1,
      onQueryChange = {},
      onClose = {},
      onPrevious = {},
      onNext = {},
      regexEnabled = true,
      regexError = false,
      onClickRegex = {},
    )
  }
}