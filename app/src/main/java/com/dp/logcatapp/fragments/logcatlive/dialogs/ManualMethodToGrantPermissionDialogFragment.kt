package com.dp.logcatapp.fragments.logcatlive.dialogs

import android.Manifest
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.base.BaseDialogFragment
import com.dp.logcatapp.util.inflateLayout

class ManualMethodToGrantPermissionDialogFragment : BaseDialogFragment() {

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val view = inflateLayout(R.layout.manual_method_dialog_fragment)
    return AlertDialog.Builder(requireActivity())
      .setTitle(getString(R.string.manual_method))
      .setView(view)
      .setPositiveButton(R.string.copy_adb_command) { _, _ ->
        val activity = requireActivity()
        val cmd = "adb shell pm grant ${activity.packageName} " +
          Manifest.permission.READ_LOGS
        val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE)
          as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Adb command", cmd))
      }
      .create()
  }

  companion object {
    val TAG = ManualMethodToGrantPermissionDialogFragment::class.qualifiedName
  }
}
