package com.dp.logcatapp.util

import android.content.Context
import android.widget.Toast

fun Context.showToast(msg: String, length: Int = Toast.LENGTH_SHORT) =
        Toast.makeText(this, msg, length).show()
