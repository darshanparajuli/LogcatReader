package com.dp.logcatapp.util

import androidx.core.text.isDigitsOnly
import com.dp.logcat.Log
import com.dp.logcatapp.util.SearchHitKey.LogComponent
import com.dp.logcatapp.util.SearchResult.SearchHit
import com.dp.logcatapp.util.SearchResult.SearchHitSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun searchLogs(
  logs: List<Log>,
  appInfoMap: Map<String, AppInfo>,
  searchRegex: Regex,
): SearchResult {
  return searchLogs(
    logs = logs,
    appInfoMap = appInfoMap,
    searchFunction = { target ->
      search(target = target, query = searchRegex)
    }
  )
}

suspend fun searchLogs(
  logs: List<Log>,
  appInfoMap: Map<String, AppInfo>,
  searchQuery: String,
): SearchResult {
  return searchLogs(
    logs = logs,
    appInfoMap = appInfoMap,
    searchFunction = { target ->
      search(target = target, query = searchQuery)
    }
  )
}

@JvmInline
value class HitIndex(val value: Int)

suspend fun searchLogs(
  logs: List<Log>,
  appInfoMap: Map<String, AppInfo>,
  searchFunction: (String) -> Sequence<SearchHitSpan>,
): SearchResult = withContext(Dispatchers.Default) {
  val map = mutableMapOf<SearchHitKey, List<HitIndex>>()
  val hits = mutableListOf<SearchHit>()
  logs.forEachIndexed { index, log ->
    val tagSearchResult = searchFunction(log.tag)
    val msgSearchResult = searchFunction(log.msg)
    val packageNameSearchResult = log.uid?.let { uid ->
      val packageName = if (uid.isDigitsOnly()) {
        appInfoMap[log.uid]?.packageName
      } else {
        uid
      }
      packageName?.let { searchFunction(it) }.orEmpty()
    } ?: emptySequence()
    val dateSearchResult = searchFunction(log.date)
    val timeSearchResult = searchFunction(log.time)
    val pidSearchResult = searchFunction(log.pid)
    val tidSearchResult = searchFunction(log.tid)

    fun addSpans(
      spans: Sequence<SearchHitSpan>,
      component: LogComponent,
    ) {
      val hitIndices = mutableListOf<HitIndex>()
      spans.forEach { span ->
        hitIndices += HitIndex(hits.size)
        hits += SearchHit(
          logId = log.id,
          index = index,
          span = span,
        )
      }
      map[SearchHitKey(logId = log.id, component = component)] = hitIndices
    }

    addSpans(tagSearchResult, LogComponent.Tag)
    addSpans(msgSearchResult, LogComponent.Message)
    addSpans(packageNameSearchResult, LogComponent.PackageName)
    addSpans(dateSearchResult, LogComponent.Date)
    addSpans(timeSearchResult, LogComponent.Time)
    addSpans(pidSearchResult, LogComponent.Pid)
    addSpans(tidSearchResult, LogComponent.Tid)
  }
  SearchResult(
    hitIndexMap = map,
    hits = hits,
  )
}

private fun search(
  target: String,
  query: String,
): Sequence<SearchHitSpan> {
  fun nextSearchHit(
    startIndex: Int = 0,
  ): SearchHitSpan? {
    return target.indexOf(string = query, ignoreCase = true, startIndex = startIndex)
      .takeIf { it != -1 }
      ?.let { index ->
        SearchHitSpan(start = index, end = index + query.length)
      }
  }
  return generateSequence(
    seedFunction = ::nextSearchHit,
    nextFunction = { span ->
      nextSearchHit(startIndex = span.end)
    }
  )
}

private fun search(
  target: String,
  query: Regex,
): Sequence<SearchHitSpan> {
  return query.findAll(target).map { result ->
    SearchHitSpan(start = result.range.start, end = result.range.endInclusive + 1)
  }
}

data class SearchResult(
  val hitIndexMap: Map<SearchHitKey, List<HitIndex>>,
  val hits: List<SearchHit>
) {
  data class SearchHitSpan(
    val start: Int,
    val end: Int,
  )

  data class SearchHit(
    val logId: Int,
    val index: Int,
    val span: SearchHitSpan,
  )
}

data class SearchHitKey(
  val logId: Int,
  val component: LogComponent,
) {
  enum class LogComponent {
    Message,
    Tag,
    PackageName,
    Time,
    Date,
    Pid,
    Tid,
  }
}