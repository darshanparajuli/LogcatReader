package com.dp.logcatapp.fragments.logcatlive.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.base.BaseDialogFragment
import com.dp.logcatapp.fragments.logcatlive.LogcatLiveFragment

class NeedPermissionDialogFragment : BaseDialogFragment() {

  object FragmentConstants {
    val PERMISSION_DIALOG = LogcatLiveFragment.TAG + "_permission_dialog"
    val ROOT_METHOD = LogcatLiveFragment.TAG + "_root_method"
  }
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return AlertDialog.Builder(requireActivity())
      .setTitle(R.string.read_logs_permission_required)
      .setMessage(R.string.read_logs_permission_required_msg)
      .setPositiveButton(R.string.manual_method) { _, _ ->
        ManualMethodToGrantPermissionDialogFragment().show(
          parentFragmentManager,
          ManualMethodToGrantPermissionDialogFragment.TAG
        )
        setFragmentResult(
          FragmentConstants.PERMISSION_DIALOG,
          bundleOf(FragmentConstants.ROOT_METHOD to false))
      }
      .setNegativeButton(R.string.root_method) { _, _ ->
        setFragmentResult(
          FragmentConstants.PERMISSION_DIALOG,
          bundleOf(FragmentConstants.ROOT_METHOD to true))
      }
      .create()
  }

  companion object {
    val TAG = NeedPermissionDialogFragment::class.qualifiedName
  }
}