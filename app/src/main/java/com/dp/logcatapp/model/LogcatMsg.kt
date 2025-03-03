package com.dp.logcatapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LogcatMsg(
  var keyword: String,
  var tag: String,
  var pid: String,
  var tid: String,
  var logLevels: MutableSet<String>
) : Parcelable {

  constructor() : this("", "", "", "", mutableSetOf())
}