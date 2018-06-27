package com.dp.logcatapp.fragments.filters.dialogs

import android.app.Dialog
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.widget.CheckBox
import android.widget.EditText
import com.dp.logcat.LogPriority
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.base.BaseDialogFragment
import com.dp.logcatapp.fragments.filters.FiltersFragment
import com.dp.logcatapp.util.inflateLayout

class FilterDialogFragment : BaseDialogFragment() {

    companion object {
        val TAG = FilterDialogFragment::class.qualifiedName
    }

    private lateinit var viewModel: MyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this)
                .get(MyViewModel::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val rootView = inflateLayout(R.layout.filter_dialog)

        val editTextKeyword = rootView.findViewById<EditText>(R.id.keyword)
        editTextKeyword.setText(viewModel.keyword)

        editTextKeyword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                viewModel.keyword = s.toString()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
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
                    (targetFragment as FiltersFragment).addFilter(keyword, prioritySet)
                })
                .setNegativeButton(android.R.string.cancel, { _, _ ->
                    dismiss()
                })
                .create()
    }

}

internal class MyViewModel : ViewModel() {
    var keyword = ""
    val logPriorities = mutableSetOf<String>()
}