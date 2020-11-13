package com.dp.logcatapp.views

import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.dp.logcatapp.R
import com.google.android.material.snackbar.Snackbar

class IndeterminateProgressSnackBar(
  view: View,
  message: String
) {

  private val progressBar: ProgressBar
  private val textView: TextView
  private val snackBar = Snackbar.make(view, "", Snackbar.LENGTH_INDEFINITE)

  init {
    val rootView = LayoutInflater.from(view.context)
      .inflate(R.layout.indeterminate_progress_snackbar, null)
    progressBar = rootView.findViewById(R.id.progressBar)
    textView = rootView.findViewById(R.id.message)
    textView.text = message

    val snackBarLayout = snackBar.view as Snackbar.SnackbarLayout
    snackBarLayout.removeAllViews()
    snackBarLayout.addView(rootView)
  }

  fun show() {
    snackBar.show()
  }

  fun dismiss() {
    snackBar.dismiss()
  }
}