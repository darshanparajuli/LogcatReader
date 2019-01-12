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

class InstructionToGrantPermissionDialogFragment : BaseDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = inflateLayout(R.layout.permission_instruction)
        return AlertDialog.Builder(activity!!)
                .setTitle(R.string.read_logs_permission_required)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.copy_adb_command) { _, _ ->
                    val cmd = "adb shell pm grant ${activity!!.packageName} " +
                            Manifest.permission.READ_LOGS
                    val cm = activity!!.getSystemService(Context.CLIPBOARD_SERVICE)
                            as ClipboardManager
                    cm.primaryClip = ClipData.newPlainText("Adb command",
                            cmd)
                }
                .create()
    }

    companion object {
        val TAG = InstructionToGrantPermissionDialogFragment::class.qualifiedName
    }
}
