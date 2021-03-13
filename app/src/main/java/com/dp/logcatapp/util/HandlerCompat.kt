package com.dp.logcatapp.util

import android.os.Handler
import android.os.Looper.getMainLooper

fun mainHandler() = Handler(getMainLooper())