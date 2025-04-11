package com.dp.logcatapp.ui.common

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Process
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dp.logcatapp.BuildConfig
import com.dp.logcatapp.R
import com.dp.logcatapp.services.LogcatService
import com.dp.logcatapp.util.SuCommander
import com.dp.logcatapp.util.isReadLogsPermissionGranted
import com.dp.logcatapp.util.rememberBooleanSharedPreference
import com.dp.logcatapp.util.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SHOW_PERMISSION_GRANTED_INFO_PREF_KEY = "show_permission_info_dialog"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaybeShowPermissionRequiredDialog(
  forceShow: Boolean = false,
  onDismissed: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val permissionGranted = remember(context) {
    context.isReadLogsPermissionGranted()
  }
  var showPermissionRequiredDialog by remember(permissionGranted, forceShow) {
    mutableStateOf(!permissionGranted || forceShow)
  }
  var showAskingForRootPermissionDialog by remember { mutableStateOf(false) }
  var showRestartAppDialog by remember { mutableStateOf(false) }
  var showManualMethodDialog by remember { mutableStateOf(false) }
  val coroutineScope = rememberCoroutineScope()

  if (showPermissionRequiredDialog) {
    val failMessage = stringResource(R.string.fail)
    Dialog(
      modifier = Modifier.fillMaxWidth(),
      primaryButton = DialogButton(
        text = stringResource(R.string.manual_method),
        onClick = {
          showPermissionRequiredDialog = false
          showManualMethodDialog = true
        },
      ),
      onDismissRequest = {
        showPermissionRequiredDialog = false
        onDismissed?.invoke()
      },
      arrangement = DialogButtonArrangement.Stack,
      title = stringResource(R.string.read_logs_permission_required),
      content = { Text(stringResource(R.string.read_logs_permission_required_msg)) },
      secondaryButton = DialogButton(
        text = stringResource(R.string.root_method),
        onClick = {
          showPermissionRequiredDialog = false
          showAskingForRootPermissionDialog = true
          coroutineScope.launch {
            val result = grantPermissionWithRoot()
            showAskingForRootPermissionDialog = false
            if (result) {
              showRestartAppDialog = true
            } else {
              showManualMethodDialog = true
              context.showToast(failMessage)
            }
          }
        }
      ),
      icon = {
        Icon(Icons.Default.Info, contentDescription = null)
      }
    )
  } else if (permissionGranted) {
    val showPermissionInfoDialog = rememberBooleanSharedPreference(
      key = SHOW_PERMISSION_GRANTED_INFO_PREF_KEY,
      default = true,
    )
    if (showPermissionInfoDialog.value) {
      Dialog(
        onDismissRequest = {
          showPermissionInfoDialog.value = false
          onDismissed?.invoke()
        },
        title = stringResource(R.string.permission_granted_info_title),
        content = {
          Text(stringResource(R.string.permission_granted_info_body))
        },
        primaryButton = DialogButton(
          text = stringResource(android.R.string.ok),
          onClick = {
            showPermissionInfoDialog.value = false
            onDismissed?.invoke()
          },
        )
      )
    }
  }

  if (showAskingForRootPermissionDialog) {
    Dialog(
      onDismissRequest = {},
      icon = {
        CircularProgressIndicator(
          modifier = Modifier.size(24.dp),
          strokeWidth = 2.dp,
        )
      },
      content = {
        Text(stringResource(R.string.asking_permission_for_root_access))
      }
    )
  }

  if (showRestartAppDialog) {
    Dialog(
      onDismissRequest = {
        showRestartAppDialog = false
        onDismissed?.invoke()
      },
      title = stringResource(R.string.app_restart_dialog_title),
      content = {
        Text(stringResource(R.string.app_restart_dialog_msg_body))
      },
      primaryButton = DialogButton(
        text = stringResource(android.R.string.ok),
        onClick = {
          context.stopService(Intent(context, LogcatService::class.java))
          Process.killProcess(Process.myPid())
        }
      )
    )
  }

  if (showManualMethodDialog) {
    Dialog(
      onDismissRequest = {
        showManualMethodDialog = false
        onDismissed?.invoke()
      },
      title = stringResource(R.string.manual_method),
      arrangement = DialogButtonArrangement.Stack,
      content = {
        Text(
          text = buildAnnotatedString {
            append(stringResource(R.string.permission_instruction0))
            appendLine(); appendLine()
            append(stringResource(R.string.permission_instruction1))
            appendLine()
            append(stringResource(R.string.permission_instruction2))
            appendLine()
            append(
              AnnotatedString(
                text = stringResource(R.string.permission_instruction3),
                spanStyle = SpanStyle(
                  color = MaterialTheme.colorScheme.tertiary,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 11.sp,
                )
              )
            )
            appendLine()
            append(stringResource(R.string.permission_instruction4))
            appendLine()
            append(stringResource(R.string.permission_instruction5))
            appendLine(); appendLine()
            append(stringResource(R.string.permission_instruction6))
          },
        )
      },
      primaryButton = DialogButton(
        text = stringResource(R.string.copy_adb_command),
        onClick = {
          showManualMethodDialog = false
          onDismissed?.invoke()
          val cmd = "adb shell pm grant ${BuildConfig.APPLICATION_ID} " +
            Manifest.permission.READ_LOGS
          val cm = context.getSystemService(Context.CLIPBOARD_SERVICE)
            as ClipboardManager
          cm.setPrimaryClip(ClipData.newPlainText("Adb command", cmd))
        }
      )
    )
  }
}

private suspend fun grantPermissionWithRoot(): Boolean {
  return withContext(Dispatchers.IO) {
    val cmd = "pm grant ${BuildConfig.APPLICATION_ID} ${Manifest.permission.READ_LOGS}"
    SuCommander(cmd).run()
  }
}