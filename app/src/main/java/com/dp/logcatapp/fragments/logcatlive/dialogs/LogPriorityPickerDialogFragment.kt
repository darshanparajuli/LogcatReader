package com.dp.logcatapp.fragments.logcatlive.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import com.dp.logcat.LogPriority
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.base.BaseDialogFragment
import com.dp.logcatapp.fragments.logcatlive.LogcatLiveFragment

class LogPriorityPickerDialogFragment : BaseDialogFragment() {
    companion object {
        val TAG = LogPriorityPickerDialogFragment::class.qualifiedName
        private val KEY_ALLOWED_PRIORITIES = TAG + "_allowed_priorities"

        fun newInstance(allowedPriorites: Array<String>): LogPriorityPickerDialogFragment {
            val frag = LogPriorityPickerDialogFragment()
            val bundle = Bundle()
            bundle.putStringArray(KEY_ALLOWED_PRIORITIES, allowedPriorites)
            frag.arguments = bundle
            return frag
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val priorities = arrayOf(LogPriority.ASSERT,
                LogPriority.DEBUG,
                LogPriority.ERROR,
                LogPriority.FATAL,
                LogPriority.INFO,
                LogPriority.VERBOSE,
                LogPriority.WARNING)

        val prioritiesToStrings = arrayOf("Assert",
                "Debug",
                "Error",
                "Fatal",
                "Info",
                "Verbose",
                "Warning")
        val prevAllowedPrioritiesSetting = arguments!!.getStringArray(KEY_ALLOWED_PRIORITIES)
        val allowed = mutableSetOf<String>()
        allowed.addAll(prevAllowedPrioritiesSetting)

        val map = mutableMapOf<Int, String>()
        for (i in 0 until priorities.size) {
            map[i] = priorities[i]
        }

        val checkedItems = BooleanArray(priorities.size, { i ->
            allowed.contains(priorities[i])
        })

        return AlertDialog.Builder(activity!!)
                .setMultiChoiceItems(prioritiesToStrings, checkedItems, { _, which, isChecked ->
                    val p = map[which]
                    if (p != null) {
                        if (isChecked) {
                            allowed.add(p)
                        } else {
                            allowed.remove(p)
                        }
                    }
                })
                .setPositiveButton(getString(android.R.string.ok), { _, _ ->
                    (targetFragment as LogcatLiveFragment).addPriorityFilter(allowed.toSet())
                })
                .create()
    }
}