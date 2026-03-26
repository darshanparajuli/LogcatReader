package com.dp.logcatapp.microbenchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import com.dp.logcat.Log
import com.dp.logcat.LogPriority
import com.dp.logcatapp.searchlogs.SearchResult
import com.dp.logcatapp.searchlogs.searchLogs
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class SearchLogsBenchmark {

  @get:Rule
  val benchmarkRule = BenchmarkRule()

  @Test
  fun benchmarkSearch() {
    val logs = mutableListOf<Log>()
    repeat(MAX_LOGS) { index ->
      val log = Log(
        id = index,
        date = "some-date",
        time = "some-time",
        uid = Log.Uid("foo-$index"),
        pid = (1000L..10000).random().toString(),
        tid = (1000L..10000).random().toString(),
        priority = LogPriority.DEBUG,
        tag = "LogcatReaderApp",
        msg = MSG_TEXT,
      )
      logs += log
    }
    lateinit var result: SearchResult
    benchmarkRule.measureRepeated {
      result = runBlocking {
        searchLogs(
          logs = logs,
          appInfoMap = emptyMap(),
          searchQuery = "b"
        )
      }
    }
    println(result)
  }

  companion object {
    private val MSG_TEXT = """
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec sollicitudin nibh sed lectus 
      pretium tempus. Sed aliquam arcu odio, ac pulvinar lacus vehicula in. Nam non volutpat nunc. 
      Curabitur ut fermentum nisi. In lectus eros, consectetur at efficitur eu, lacinia in eros. 
      Phasellus vel quam nec purus ullamcorper elementum ac et arcu. Cras quis erat convallis, 
      feugiat sem quis, tincidunt mauris. Nam elementum mattis nisi, vel porta metus blandit et. 
      Duis interdum dolor lorem, nec ullamcorper nulla commodo a. Maecenas dictum interdum risus, 
      quis ultrices mi. Aliquam at laoreet quam, vitae porta ligula. Vivamus auctor, felis placerat 
      vehicula pulvinar, nibh risus scelerisque orci, non fermentum urna tellus et purus. 
      Sed aliquet dapibus fringilla. Duis ut finibus nunc.
      """.trimIndent()
    private const val MAX_LOGS = 100_000
  }
}