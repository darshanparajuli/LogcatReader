package com.dp.logcatapp.fragments.logcatlive.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.setFragmentResult
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.base.BaseDialogFragment

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
        setFragmentResult(
          RESULT_ROOT_METHOD,
          Bundle().apply { putBoolean(RESULT_ROOT_METHOD, true) })
      }
      .create()
  }

  companion object {
    val TAG = NeedPermissionDialogFragment::class.qualifiedName
    const val REQ_ROOT_METHOD = "req-root-method"
    const val RESULT_ROOT_METHOD = "result-root-method"
  }
}