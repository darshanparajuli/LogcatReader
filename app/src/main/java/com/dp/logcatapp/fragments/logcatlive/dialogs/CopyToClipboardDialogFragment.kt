package com.dp.logcatapp.fragments.logcatlive.dialogs

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import com.dp.logcat.Log
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.base.BaseDialogFragment
import com.dp.logcatapp.util.showToast

class CopyToClipboardDialogFragment : BaseDialogFragment(), DialogInterface.OnClickListener {

    companion object {
        val TAG = CopyToClipboardDialogFragment::class.qualifiedName

        private val KEY_LOG = TAG + "_key_log"

        fun newInstance(log: Log): CopyToClipboardDialogFragment {
            val bundle = Bundle()
            bundle.putParcelable(KEY_LOG, log)

            val fragment = CopyToClipboardDialogFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    private enum class LogContentType {
        TAG, MESSAGE, DATE, TIME, PID, TID
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity!!)
                .setTitle(R.string.copy_to_clipboard)
                .setItems(R.array.log_content_types, this)
                .create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val log = arguments!!.getParcelable<Log>(KEY_LOG)
        val cm = activity!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = when (which) {
            LogContentType.TAG.ordinal -> ClipData.newPlainText("Log Tag", log.tag)
            LogContentType.MESSAGE.ordinal -> ClipData.newPlainText("Log Tag", log.msg)
            LogContentType.DATE.ordinal -> ClipData.newPlainText("Log Tag", log.date)
            LogContentType.TIME.ordinal -> ClipData.newPlainText("Log Tag", log.time)
            LogContentType.PID.ordinal -> ClipData.newPlainText("Log Tag", log.pid)
            else -> ClipData.newPlainText("Log Tag", log.tid)
        }
        cm.primaryClip = clip
        activity!!.showToast(getString(R.string.copied_to_clipboard))
    }
}