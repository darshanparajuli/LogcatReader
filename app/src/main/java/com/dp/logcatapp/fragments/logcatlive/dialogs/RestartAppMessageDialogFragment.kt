package com.dp.logcatapp.fragments.logcatlive.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.appcompat.app.AlertDialog
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.base.BaseDialogFragment
import com.dp.logcatapp.services.LogcatService

class RestartAppMessageDialogFragment : BaseDialogFragment() {

  companion object {
    val TAG = RestartAppMessageDialogFragment::class.qualifiedName

    fun newInstance(): RestartAppMessageDialogFragment {
      return RestartAppMessageDialogFragment()
    }
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    isCancelable = false
    return AlertDialog.Builder(requireActivity())
      .setTitle(R.string.app_restart_dialog_title)
      .setMessage(getString(R.string.app_restart_dialog_msg_body))
      .setCancelable(false)
      .setPositiveButton(android.R.string.ok) { _, _ ->
        val context = requireContext()
        context.stopService(Intent(context, LogcatService::class.java))
        Process.killProcess(Process.myPid())
      }
      .create()
  }
}