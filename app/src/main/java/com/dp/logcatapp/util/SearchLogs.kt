package com.dp.logcatapp.util

import com.dp.logcat.Log
import com.dp.logcatapp.util.SearchHitKey.LogComponent
import com.dp.logcatapp.util.SearchResult.SearchHit
import com.dp.logcatapp.util.SearchResult.SearchHitSpan
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

private const val CHUNKED_SEARCH_THRESHOLD = 1000

suspend fun searchLogs(
  logs: List<Log>,
  appInfoMap: Map<String, AppInfo>,
  searchRegex: Regex,
): SearchResult = coroutineScope {
  searchLogs(
    logs = logs,
    appInfoMap = appInfoMap,
    searchFunction = { target ->
      search(
        target = target,
        query = searchRegex,
        cancellationChecker = {
          ensureActive()
        }
      )
    }
  )
}

suspend fun searchLogs(
  logs: List<Log>,
  appInfoMap: Map<String, AppInfo>,
  searchQuery: String,
): SearchResult = coroutineScope {
  searchLogs(
    logs = logs,
    appInfoMap = appInfoMap,
    searchFunction = { target ->
      search(
        target = target,
        query = searchQuery,
        cancellationChecker = {
          ensureActive()
        }
      )
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
  // Use HashMap since insertion-order does not matter and indexing is slightly faster.
  val map = HashMap<SearchHitKey, List<HitIndex>>()
  val hits = mutableListOf<SearchHit>()
  val chunkSize = logs.size / (Runtime.getRuntime().availableProcessors() - 1)
    .coerceAtLeast(1)
  if (logs.size > CHUNKED_SEARCH_THRESHOLD) {
    val deferred = mutableListOf<Deferred<Unit>>()
    logs.asSequence()
      .withIndex()
      .chunked(chunkSize)
      .forEach { logs ->
        deferred += async {
          logs.forEach { (index, log) ->
            ensureActive()
            performSearch(
              index = index,
              log = log,
              appInfoMap = appInfoMap,
              searchFunction = searchFunction,
              hits = hits,
              map = map,
            )
          }
        }
      }
    deferred.awaitAll()
  } else {
    logs.forEachIndexed { index, log ->
      ensureActive()
      performSearch(
        index = index,
        log = log,
        appInfoMap = appInfoMap,
        searchFunction = searchFunction,
        hits = hits,
        map = map,
      )
    }
  }
  SearchResult(
    hitIndexMap = map,
    hits = hits,
  )
}

private fun performSearch(
  index: Int,
  log: Log,
  appInfoMap: Map<String, AppInfo>,
  searchFunction: (String) -> Sequence<SearchHitSpan>,
  hits: MutableList<SearchHit>,
  map: MutableMap<SearchHitKey, List<HitIndex>>,
) {
  val tagSearchResult = searchFunction(log.tag)
  val msgSearchResult = searchFunction(log.msg)
  val packageNameSearchResult = log.uid?.let { uid ->
    val packageName = if (uid.isNum) {
      appInfoMap[uid.value]?.packageName
    } else {
      uid.value
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

private fun search(
  target: String,
  query: String,
  cancellationChecker: () -> Unit,
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
      cancellationChecker()
      nextSearchHit(startIndex = span.end)
    }
  )
}

private fun search(
  target: String,
  query: Regex,
  cancellationChecker: () -> Unit,
): Sequence<SearchHitSpan> {
  return query.findAll(target).map { result ->
    cancellationChecker()
    SearchHitSpan(start = result.range.first, end = result.range.last + 1)
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