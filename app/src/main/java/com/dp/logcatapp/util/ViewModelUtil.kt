package com.dp.logcatapp.util

import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

inline fun <reified T : ViewModel> ComponentActivity.getAndroidViewModel(): T {
  val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
  return ViewModelProvider(this, factory).get(T::class.java)
}

inline fun <reified T : ViewModel> Fragment.getAndroidViewModel(): T {
  val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)
  return ViewModelProvider(this, factory).get(T::class.java)
}

inline fun <reified T : ViewModel> AppCompatActivity.getViewModel(): T {
  return ViewModelProvider(this).get(T::class.java)
}

inline fun <reified T : ViewModel> Fragment.getViewModel(): T {
  return ViewModelProvider(this).get(T::class.java)
}
