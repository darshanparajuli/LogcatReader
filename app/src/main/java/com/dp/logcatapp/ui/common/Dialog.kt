package com.dp.logcatapp.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dp.logcatapp.ui.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dialog(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  confirmButton: (@Composable () -> Unit)? = null,
  dismissButton: @Composable (() -> Unit)? = null,
  icon: @Composable (() -> Unit)? = null,
  title: @Composable (() -> Unit)? = null,
  content: @Composable (ColumnScope.() -> Unit)? = null,
) {
  BasicAlertDialog(
    modifier = modifier,
    onDismissRequest = onDismissRequest,
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.background,
      tonalElevation = 2.dp,
      shadowElevation = 8.dp,
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
      ) {
        Column(
          modifier = Modifier.weight(weight = 1f, fill = false),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          if (icon != null) {
            Box(
              modifier = Modifier.fillMaxWidth(),
              contentAlignment = Alignment.Center,
            ) {
              icon()
            }
          }
          if (title != null) {
            Box(
              modifier = Modifier.fillMaxWidth(),
              contentAlignment = if (icon != null) {
                Alignment.Center
              } else {
                Alignment.TopStart
              },
            ) {
              CompositionLocalProvider(LocalTextStyle provides AppTypography.titleLarge) {
                title()
              }
            }
          }
          if (content != null) {
            CompositionLocalProvider(LocalTextStyle provides AppTypography.bodyMedium) {
              Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
              ) {
                content()
              }
            }
          }
        }
        if (dismissButton != null || confirmButton != null) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            CompositionLocalProvider(LocalTextStyle provides AppTypography.bodyMedium) {
              dismissButton?.invoke()
              confirmButton?.invoke()
            }
          }
        }
      }
    }
  }
}