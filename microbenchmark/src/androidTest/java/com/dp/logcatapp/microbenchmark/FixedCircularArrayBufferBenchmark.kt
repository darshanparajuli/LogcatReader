package com.dp.logcatapp.microbenchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import com.dp.logcat.collections.FixedCircularBuffer
import org.junit.Rule
import org.junit.Test

class FixedCircularArrayBufferBenchmark {

  @get:Rule
  val benchmarkRule = BenchmarkRule()

  @Test
  fun benchmarkAdd() {
    val buffer = FixedCircularBuffer<Int>(capacity = 100_000)
    benchmarkRule.measureRepeated {
      buffer.add(12)
    }
  }

  @Test
  fun benchmarkCloneAdd() {
    var buffer = FixedCircularBuffer<Int>(capacity = 100_000)
    benchmarkRule.measureRepeated {
      buffer = buffer.clone().also { it.add(12) }
    }
  }
}