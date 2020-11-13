package com.dp.logcatapp.fragments.logcatlive.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.base.BaseDialogFragment
import com.dp.logcatapp.util.inflateLayout

class AskingForRootAccessDialogFragment : BaseDialogFragment() {

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val view = inflateLayout(R.layout.asking_for_root_access_dialog_fragment)

    isCancelable = false
    return AlertDialog.Builder(requireActivity())
      .setView(view)
      .setCancelable(false)
      .create()
  }

  companion object {
    val TAG = AskingForRootAccessDialogFragment::class.qualifiedName
  }
}