package com.dp.logcatapp.util

import com.dp.logcat.Log
import com.dp.logcatapp.util.SearchHitKey.LogComponent
import com.dp.logcatapp.util.SearchResult.SearchHit
import com.dp.logcatapp.util.SearchResult.SearchHitSpan
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
  if (logs.size > CHUNKED_SEARCH_THRESHOLD) {
    val chunkSize = logs.size / (Runtime.getRuntime().availableProcessors() - 1)
      .coerceAtLeast(1)
    val spans = mutableListOf<SearchHitSpanData>()
    logs.asSequence()
      .withIndex()
      .chunked(chunkSize)
      .map { logs ->
        async {
          logs.map { (index, log) ->
            ensureActive()
            SearchHitSpanData(
              index = index,
              logId = log.id,
              tag = searchFunction(log.tag),
              msg = searchFunction(log.msg),
              packageName = log.uid?.let { uid ->
                val packageName = if (uid.isNum) {
                  appInfoMap[uid.value]?.packageName
                } else {
                  uid.value
                }
                packageName?.let { searchFunction(it) }.orEmpty()
              } ?: emptySequence(),
              date = searchFunction(log.date),
              time = searchFunction(log.time),
              pid = searchFunction(log.pid),
              tid = searchFunction(log.tid),
            )
          }
        }
      }.toList()
      .awaitAll()
      .forEach { spans += it }

    ensureActive()

    spans.sortBy { it.logId }
    spans.forEach { spanData ->
      addSpans(
        spanData = spanData,
        hits = hits,
        map = map,
      )
    }
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
  addSpans(
    spanData = SearchHitSpanData(
      index = index,
      logId = log.id,
      tag = searchFunction(log.tag),
      msg = searchFunction(log.msg),
      packageName = log.uid?.let { uid ->
        val packageName = if (uid.isNum) {
          appInfoMap[uid.value]?.packageName
        } else {
          uid.value
        }
        packageName?.let { searchFunction(it) }.orEmpty()
      } ?: emptySequence(),
      date = searchFunction(log.date),
      time = searchFunction(log.time),
      pid = searchFunction(log.pid),
      tid = searchFunction(log.tid),
    ),
    hits = hits,
    map = map,
  )
}

private fun addSpans(
  spanData: SearchHitSpanData,
  hits: MutableList<SearchHit>,
  map: MutableMap<SearchHitKey, List<HitIndex>>,
) {
  val index = spanData.index
  val logId = spanData.logId
  addSpans(
    spans = spanData.tag,
    component = LogComponent.Tag,
    index = index,
    logId = logId,
    hits = hits,
    map = map
  )
  addSpans(
    spans = spanData.msg,
    component = LogComponent.Message,
    index = index,
    logId = logId,
    hits = hits,
    map = map
  )
  addSpans(
    spans = spanData.packageName,
    component = LogComponent.PackageName,
    index = index,
    logId = logId,
    hits = hits,
    map = map
  )
  addSpans(
    spans = spanData.date,
    component = LogComponent.Date,
    index = index,
    logId = logId,
    hits = hits,
    map = map
  )
  addSpans(
    spans = spanData.time,
    component = LogComponent.Time,
    index = index,
    logId = logId,
    hits = hits,
    map = map
  )
  addSpans(
    spans = spanData.pid,
    component = LogComponent.Pid,
    index = index,
    logId = logId,
    hits = hits,
    map = map
  )
  addSpans(
    spans = spanData.tid,
    component = LogComponent.Tid,
    index = index,
    logId = logId,
    hits = hits,
    map = map
  )
}

private fun addSpans(
  spans: Sequence<SearchHitSpan>,
  component: LogComponent,
  index: Int,
  logId: Int,
  hits: MutableList<SearchHit>,
  map: MutableMap<SearchHitKey, List<HitIndex>>,
) {
  val hitIndices = mutableListOf<HitIndex>()
  spans.forEach { span ->
    hitIndices += HitIndex(hits.size)
    hits += SearchHit(
      index = index,
      span = span,
    )
  }
  if (hitIndices.isNotEmpty()) {
    map[SearchHitKey(logId = logId, component = component)] = hitIndices
  }
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

private data class SearchHitSpanData(
  val index: Int,
  val logId: Int,
  val tag: Sequence<SearchHitSpan>,
  val msg: Sequence<SearchHitSpan>,
  val packageName: Sequence<SearchHitSpan>,
  val date: Sequence<SearchHitSpan>,
  val time: Sequence<SearchHitSpan>,
  val pid: Sequence<SearchHitSpan>,
  val tid: Sequence<SearchHitSpan>,
)

data class SearchResult(
  val hitIndexMap: Map<SearchHitKey, List<HitIndex>>,
  val hits: List<SearchHit>
) {
  data class SearchHitSpan(
    val start: Int,
    val end: Int,
  )

  data class SearchHit(
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