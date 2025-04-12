package com.dp.logcatapp.util

import java.util.regex.PatternSyntaxException

fun String.toRegexOrNull(
  ignoreCase: Boolean = false,
): Regex? {
  return try {
    toRegex(
      setOfNotNull(
        if (ignoreCase) {
          RegexOption.IGNORE_CASE
        } else {
          null
        },
      )
    )
  } catch (_: PatternSyntaxException) {
    null
  }
}