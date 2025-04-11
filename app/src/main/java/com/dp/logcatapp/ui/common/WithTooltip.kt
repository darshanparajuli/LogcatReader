package com.dp.logcatapp.ui.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithTooltip(
  text: String,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  TooltipBox(
    modifier = modifier,
    positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
    tooltip = {
      RichTooltip {
        Text(text)
      }
    },
    state = rememberTooltipState(),
    content = content,
  )
}