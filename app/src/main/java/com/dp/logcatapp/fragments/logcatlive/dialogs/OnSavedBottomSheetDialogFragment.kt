package com.dp.logcatapp.fragments.logcatlive.dialogs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.dp.logcatapp.R
import com.dp.logcatapp.activities.SavedLogsViewerActivity
import com.dp.logcatapp.util.ShareUtils
import com.dp.logcatapp.util.Utils
import com.dp.logcatapp.util.showSnackbar
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class OnSavedBottomSheetDialogFragment : BottomSheetDialogFragment() {
  companion object {
    val TAG = OnSavedBottomSheetDialogFragment::class.qualifiedName

    private val KEY_URI = TAG + "_uri"
    private val KEY_FILE_NAME = TAG + "_file_name"

    fun newInstance(
      fileName: String,
      uri: Uri
    ): OnSavedBottomSheetDialogFragment {
      val fragment = OnSavedBottomSheetDialogFragment()
      val bundle = Bundle()
      bundle.putString(KEY_URI, uri.toString())
      bundle.putString(KEY_FILE_NAME, fileName)
      fragment.arguments = bundle
      return fragment
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val rootView = inflater.inflate(R.layout.saved_log_bottom_sheet, container, false)

    val arguments = requireArguments()
    val fileName = arguments.getString(KEY_FILE_NAME)
    val uri = Uri.parse(arguments.getString(KEY_URI))

    rootView.findViewById<TextView>(R.id.savedFileName).text = fileName
    rootView.findViewById<TextView>(R.id.actionView).setOnClickListener {
      if (!viewSavedLog(uri)) {
        showSnackbar(view, getString(R.string.could_not_open_log_file))
      }
      dismiss()
    }

    rootView.findViewById<TextView>(R.id.actionShare).setOnClickListener {
      val context = requireContext()
      ShareUtils.shareSavedLogs(context, uri, Utils.isUsingCustomSaveLocation(context))
      dismiss()
    }

    return rootView
  }

  private fun viewSavedLog(uri: Uri): Boolean {
    val intent = Intent(context, SavedLogsViewerActivity::class.java)
    intent.setDataAndType(uri, "text/plain")
    startActivity(intent)
    return true
  }
}
