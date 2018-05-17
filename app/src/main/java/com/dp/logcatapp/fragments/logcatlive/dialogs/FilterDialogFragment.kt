package com.dp.logcatapp.fragments.logcatlive.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.widget.CheckBox
import android.widget.EditText
import com.dp.logcat.LogPriority
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.base.BaseDialogFragment
import com.dp.logcatapp.fragments.logcatlive.LogcatLiveFragment
import com.dp.logcatapp.util.inflateLayout

class FilterDialogFragment : BaseDialogFragment() {

    companion object {
        val TAG = FilterDialogFragment::class.qualifiedName

        private val KEY_KEYWORD = TAG + "_key_keyword"
        private val KEY_LOG_PRIORITIES = TAG + "_key_log_priorities"

        fun newInstance(keyword: String, logPriorities: Set<String>): FilterDialogFragment {
            val bundle = Bundle()
            bundle.putString(KEY_KEYWORD, keyword)
            bundle.putStringArray(KEY_LOG_PRIORITIES, logPriorities.toTypedArray())

            val frag = FilterDialogFragment()
            frag.arguments = bundle
            return frag
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val rootView = inflateLayout(R.layout.filter_dialog)

        val editTextKeyword = rootView.findViewById<EditText>(R.id.keyword)
        editTextKeyword.setText(arguments!!.getString(KEY_KEYWORD))

        val checkBoxMap = mutableMapOf<CheckBox, String>()
        checkBoxMap[rootView.findViewById(R.id.checkboxAssert)] = LogPriority.ASSERT
        checkBoxMap[rootView.findViewById(R.id.checkboxDebug)] = LogPriority.DEBUG
        checkBoxMap[rootView.findViewById(R.id.checkboxError)] = LogPriority.ERROR
        checkBoxMap[rootView.findViewById(R.id.checkboxFatal)] = LogPriority.FATAL
        checkBoxMap[rootView.findViewById(R.id.checkboxInfo)] = LogPriority.INFO
        checkBoxMap[rootView.findViewById(R.id.checkboxVerbose)] = LogPriority.VERBOSE
        checkBoxMap[rootView.findViewById(R.id.checkboxWarning)] = LogPriority.WARNING

        val logPriorities = arguments!!.getStringArray(KEY_LOG_PRIORITIES).toSet()
        for ((k, v) in checkBoxMap) {
            k.isChecked = v in logPriorities
        }

        return AlertDialog.Builder(activity!!)
                .setTitle(R.string.filter)
                .setView(rootView)
                .setPositiveButton(android.R.string.ok, { _, _ ->
                    val prioritySet = mutableSetOf<String>()
                    val keyword = editTextKeyword.text.toString().trim()
                    for ((k, v) in checkBoxMap) {
                        if (k.isChecked) {
                            prioritySet.add(v)
                        }
                    }
                    (targetFragment as LogcatLiveFragment).setFilterAndSave(keyword, prioritySet)
                })
                .setNegativeButton(android.R.string.cancel, { _, _ ->
                    dismiss()
                })
                .create()
    }

}