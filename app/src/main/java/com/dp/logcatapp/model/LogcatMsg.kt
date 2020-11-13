package com.dp.logcatapp.model

data class LogcatMsg(
  var keyword: String,
  var tag: String,
  var pid: String,
  var tid: String,
  var logLevels: MutableSet<String>
) {

  constructor() : this("", "", "", "", mutableSetOf())
}