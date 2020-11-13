package com.dp.logcatapp.fragments.shared.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.dp.logcat.Log
import com.dp.logcatapp.R
import com.dp.logcatapp.activities.FiltersActivity
import com.dp.logcatapp.fragments.base.BaseDialogFragment

class FilterExclusionDialogFragment : BaseDialogFragment(), DialogInterface.OnClickListener {

  companion object {
    val TAG = FilterExclusionDialogFragment::class.qualifiedName

    private val KEY_LOG = TAG + "_key_log"

    fun newInstance(log: Log): FilterExclusionDialogFragment {
      val bundle = Bundle()
      bundle.putParcelable(KEY_LOG, log)

      val fragment = FilterExclusionDialogFragment()
      fragment.arguments = bundle
      return fragment
    }
  }

  private enum class LogContentType {
    FILTER,
    EXCLUDE
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return AlertDialog.Builder(requireActivity())
      .setItems(R.array.filter_exclude, this)
      .create()
  }

  override fun onClick(
    dialog: DialogInterface,
    which: Int
  ) {
    val log = requireArguments().getParcelable<Log>(KEY_LOG)!!
    when (which) {
      LogContentType.FILTER.ordinal -> moveToFilterActivity(log, false)
      LogContentType.EXCLUDE.ordinal -> moveToFilterActivity(log, true)
    }
    dismiss()
  }

  private fun moveToFilterActivity(
    log: Log,
    isExclusion: Boolean
  ) {
    val intent = Intent(requireActivity(), FiltersActivity::class.java)
    intent.putExtra(FiltersActivity.EXTRA_EXCLUSIONS, isExclusion)
    intent.putExtra(FiltersActivity.KEY_LOG, log)
    startActivity(intent)
  }
}