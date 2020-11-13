package com.dp.logcatapp.fragments.filters.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModel
import com.dp.logcat.Log
import com.dp.logcat.LogPriority
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.base.BaseDialogFragment
import com.dp.logcatapp.fragments.filters.FiltersFragment
import com.dp.logcatapp.model.LogcatMsg
import com.dp.logcatapp.util.getViewModel
import com.dp.logcatapp.util.inflateLayout

class FilterDialogFragment : BaseDialogFragment() {

  companion object {
    val TAG = FilterDialogFragment::class.qualifiedName
    private val KEY_LOG = TAG + "_key_log"

    fun newInstance(log: Log?): FilterDialogFragment {
      val bundle = Bundle()
      bundle.putParcelable(KEY_LOG, log)

      val fragment = FilterDialogFragment()
      fragment.arguments = bundle
      return fragment
    }
  }

  private lateinit var viewModel: MyViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel()
    initViewModel(getLog())
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val rootView = inflateLayout(R.layout.filter_dialog)

    val editTextKeyword = rootView.findViewById<EditText>(R.id.keyword)
    editTextKeyword.setText(viewModel.keyword)
    editTextKeyword.addTextChangedListener(object : TextWatcher {
      override fun afterTextChanged(s: Editable) {
        viewModel.keyword = s.toString()
      }

      override fun beforeTextChanged(
        s: CharSequence,
        start: Int,
        count: Int,
        after: Int
      ) {
      }

      override fun onTextChanged(
        s: CharSequence,
        start: Int,
        before: Int,
        count: Int
      ) {
      }
    })

    val editTextTag = rootView.findViewById<EditText>(R.id.tag)
    editTextTag.setText(viewModel.tag)
    editTextTag.addTextChangedListener(object : TextWatcher {
      override fun afterTextChanged(s: Editable) {
        viewModel.tag = s.toString()
      }

      override fun beforeTextChanged(
        s: CharSequence,
        start: Int,
        count: Int,
        after: Int
      ) {
      }

      override fun onTextChanged(
        s: CharSequence,
        start: Int,
        before: Int,
        count: Int
      ) {
      }
    })

    val editTextPid = rootView.findViewById<EditText>(R.id.pid)
    editTextPid.setText(viewModel.pid)
    editTextPid.addTextChangedListener(object : TextWatcher {
      override fun afterTextChanged(s: Editable) {
        viewModel.pid = s.toString()
      }

      override fun beforeTextChanged(
        s: CharSequence,
        start: Int,
        count: Int,
        after: Int
      ) {
      }

      override fun onTextChanged(
        s: CharSequence,
        start: Int,
        before: Int,
        count: Int
      ) {
      }
    })

    val editTextTid = rootView.findViewById<EditText>(R.id.tid)
    editTextTid.setText(viewModel.tid)
    editTextTid.addTextChangedListener(object : TextWatcher {
      override fun afterTextChanged(s: Editable) {
        viewModel.tid = s.toString()
      }

      override fun beforeTextChanged(
        s: CharSequence,
        start: Int,
        count: Int,
        after: Int
      ) {
      }

      override fun onTextChanged(
        s: CharSequence,
        start: Int,
        before: Int,
        count: Int
      ) {
      }
    })

    val checkBoxMap = mutableMapOf<CheckBox, String>()
    checkBoxMap[rootView.findViewById(R.id.checkboxAssert)] = LogPriority.ASSERT
    checkBoxMap[rootView.findViewById(R.id.checkboxDebug)] = LogPriority.DEBUG
    checkBoxMap[rootView.findViewById(R.id.checkboxError)] = LogPriority.ERROR
    checkBoxMap[rootView.findViewById(R.id.checkboxFatal)] = LogPriority.FATAL
    checkBoxMap[rootView.findViewById(R.id.checkboxInfo)] = LogPriority.INFO
    checkBoxMap[rootView.findViewById(R.id.checkboxVerbose)] = LogPriority.VERBOSE
    checkBoxMap[rootView.findViewById(R.id.checkboxWarning)] = LogPriority.WARNING

    for ((k, v) in checkBoxMap) {
      k.isChecked = v in viewModel.logPriorities
      val logPriority = checkBoxMap[k]!!
      k.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
          viewModel.logPriorities += logPriority
        } else {
          viewModel.logPriorities -= logPriority
        }
      }
    }

    val title = if ((targetFragment as FiltersFragment).isExclusions()) {
      getString(R.string.exclusion)
    } else {
      getString(R.string.filter)
    }

    return AlertDialog.Builder(requireActivity())
      .setTitle(title)
      .setView(rootView)
      .setPositiveButton(android.R.string.ok) { _, _ ->
        var logcatMsg = LogcatMsg()
        logcatMsg.logLevels = mutableSetOf<String>()
        logcatMsg.keyword = editTextKeyword.text.toString().trim()
        logcatMsg.tag = editTextTag.text.toString().trim()
        logcatMsg.pid = editTextPid.text.toString().trim()
        logcatMsg.tid = editTextTid.text.toString().trim()
        for ((k, v) in checkBoxMap) {
          if (k.isChecked) {
            logcatMsg.logLevels.add(v)
          }
        }
        (targetFragment as FiltersFragment).addFilter(logcatMsg)
      }
      .setNegativeButton(android.R.string.cancel) { _, _ ->
        dismiss()
      }
      .create()
  }

  fun initViewModel(log: Log?) {
    log?.let {
      viewModel.tag = log.tag
      viewModel.pid = log.pid
      viewModel.tid = log.tid
      viewModel.logPriorities.add(log.priority)
    }
  }

  fun getLog() = arguments?.getParcelable<Log>(KEY_LOG)
}

internal class MyViewModel : ViewModel() {
  var keyword = ""
  var tag = ""
  var pid = ""
  var tid = ""
  val logPriorities = mutableSetOf<String>()
}