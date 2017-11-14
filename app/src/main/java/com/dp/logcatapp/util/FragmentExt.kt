package com.dp.logcatapp.util

import android.support.annotation.LayoutRes
import android.support.v4.app.Fragment
import android.view.LayoutInflater

fun Fragment.inflateLayout(@LayoutRes layoutResId: Int) = LayoutInflater.from(activity)
        .inflate(layoutResId, null, false)