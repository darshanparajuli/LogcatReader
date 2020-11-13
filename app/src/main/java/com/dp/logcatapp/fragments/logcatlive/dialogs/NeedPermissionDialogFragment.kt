package com.dp.logcatapp.fragments.logcatlive.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.base.BaseDialogFragment
import com.dp.logcatapp.fragments.logcatlive.LogcatLiveFragment

class NeedPermissionDialogFragment : BaseDialogFragment() {

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return AlertDialog.Builder(requireActivity())
      .setTitle(R.string.read_logs_permission_required)
      .setMessage(R.string.read_logs_permission_required_msg)
      .setPositiveButton(R.string.manual_method) { _, _ ->
        ManualMethodToGrantPermissionDialogFragment().show(
          parentFragmentManager,
          ManualMethodToGrantPermissionDialogFragment.TAG
        )
      }
      .setNegativeButton(R.string.root_method) { _, _ ->
        (targetFragment as LogcatLiveFragment).useRootToGrantPermission()
      }
      .create()
  }

  companion object {
    val TAG = NeedPermissionDialogFragment::class.qualifiedName
  }
}