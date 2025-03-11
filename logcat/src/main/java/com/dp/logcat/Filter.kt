package com.dp.logcat

interface Filter {

  fun apply(log: Log): Boolean

  object AlwaysAllow : Filter {
    override fun apply(log: Log): Boolean {
      return true
    }
  }
}