package com.dp.logcatapp.microbenchmark

import androidx.benchmark.MicrobenchmarkScope
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import kotlinx.coroutines.runBlocking

fun BenchmarkRule.measureRepeatedSuspend(
  block: suspend MicrobenchmarkScope.() -> Unit
) {
  measureRepeated {
    pauseMeasurement()
    runBlocking {
      resumeMeasurement()
      this@measureRepeated.block()
      pauseMeasurement()
    }
    resumeMeasurement()
  }
}
