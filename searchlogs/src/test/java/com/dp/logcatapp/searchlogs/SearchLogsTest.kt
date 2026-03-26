package com.dp.logcatapp.searchlogs

import android.graphics.drawable.ColorDrawable
import com.dp.logcat.Log
import com.dp.logcat.LogPriority
import com.dp.logcatapp.searchlogs.SearchHitKey.LogComponent
import com.dp.logcatapp.util.AppInfo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchLogsTest {

  @Test
  fun `search with no logs returns empty result`() = runTest {
    val result = searchLogs(
      logs = emptyList(),
      appInfoMap = emptyMap(),
      searchQuery = "test",
    )
    assertTrue(result.hits.isEmpty())
    assertTrue(result.hitIndexMap.isEmpty())
  }

  @Test
  fun `search with no matches returns empty result`() = runTest {
    val result = searchLogs(
      logs = listOf(createLog(id = 0, tag = "MyTag", msg = "hello world")),
      appInfoMap = emptyMap(),
      searchQuery = "xyz",
    )
    assertTrue(result.hits.isEmpty())
    assertTrue(result.hitIndexMap.isEmpty())
  }

  @Test
  fun `search matches tag`() = runTest {
    val result = searchLogs(
      logs = listOf(createLog(id = 0, tag = "MyTag", msg = "hello")),
      appInfoMap = emptyMap(),
      searchQuery = "MyTag",
    )
    val key = SearchHitKey(logId = 0, component = LogComponent.Tag)
    assertNotNull(result.hitIndexMap[key])
    assertEquals(1, result.hitIndexMap[key]!!.size)

    val hit = result.hits[result.hitIndexMap[key]!![0].value]
    assertEquals(0, hit.span.start)
    assertEquals(5, hit.span.end)
  }

  @Test
  fun `search matches message`() = runTest {
    val result = searchLogs(
      logs = listOf(createLog(id = 0, tag = "Tag", msg = "error occurred here")),
      appInfoMap = emptyMap(),
      searchQuery = "error",
    )
    val key = SearchHitKey(logId = 0, component = LogComponent.Message)
    assertNotNull(result.hitIndexMap[key])
    assertEquals(1, result.hitIndexMap[key]!!.size)

    val hit = result.hits[result.hitIndexMap[key]!![0].value]
    assertEquals(0, hit.span.start)
    assertEquals(5, hit.span.end)
  }

  @Test
  fun `search is case insensitive`() = runTest {
    val result = searchLogs(
      logs = listOf(createLog(id = 0, tag = "Tag", msg = "Error Occurred")),
      appInfoMap = emptyMap(),
      searchQuery = "error",
    )
    val key = SearchHitKey(logId = 0, component = LogComponent.Message)
    assertNotNull(result.hitIndexMap[key])
    assertEquals(1, result.hitIndexMap[key]!!.size)
  }

  @Test
  fun `search finds multiple matches in same field`() = runTest {
    val result = searchLogs(
      logs = listOf(createLog(id = 0, tag = "Tag", msg = "ab ab ab")),
      appInfoMap = emptyMap(),
      searchQuery = "ab",
    )
    val key = SearchHitKey(logId = 0, component = LogComponent.Message)
    assertNotNull(result.hitIndexMap[key])
    assertEquals(3, result.hitIndexMap[key]!!.size)

    val spans = result.hitIndexMap[key]!!.map { result.hits[it.value].span }
    assertEquals(0, spans[0].start)
    assertEquals(2, spans[0].end)
    assertEquals(3, spans[1].start)
    assertEquals(5, spans[1].end)
    assertEquals(6, spans[2].start)
    assertEquals(8, spans[2].end)
  }

  @Test
  fun `search matches across multiple log fields`() = runTest {
    val result = searchLogs(
      logs = listOf(
        createLog(id = 0, tag = "test", msg = "this is a test", pid = "test"),
      ),
      appInfoMap = emptyMap(),
      searchQuery = "test",
    )
    val tagKey = SearchHitKey(logId = 0, component = LogComponent.Tag)
    val msgKey = SearchHitKey(logId = 0, component = LogComponent.Message)
    val pidKey = SearchHitKey(logId = 0, component = LogComponent.Pid)
    assertNotNull(result.hitIndexMap[tagKey])
    assertNotNull(result.hitIndexMap[msgKey])
    assertNotNull(result.hitIndexMap[pidKey])
  }

  @Test
  fun `search matches across multiple logs`() = runTest {
    val result = searchLogs(
      logs = listOf(
        createLog(id = 0, tag = "Tag1", msg = "hello"),
        createLog(id = 1, tag = "Tag2", msg = "hello world"),
        createLog(id = 2, tag = "Tag3", msg = "goodbye"),
      ),
      appInfoMap = emptyMap(),
      searchQuery = "hello",
    )
    val key0 = SearchHitKey(logId = 0, component = LogComponent.Message)
    val key1 = SearchHitKey(logId = 1, component = LogComponent.Message)
    val key2 = SearchHitKey(logId = 2, component = LogComponent.Message)
    assertNotNull(result.hitIndexMap[key0])
    assertNotNull(result.hitIndexMap[key1])
    assertTrue(result.hitIndexMap[key2] == null)
  }

  @Test
  fun `search matches date field`() = runTest {
    val result = searchLogs(
      logs = listOf(createLog(id = 0, date = "03-26")),
      appInfoMap = emptyMap(),
      searchQuery = "03-26",
    )
    val key = SearchHitKey(logId = 0, component = LogComponent.Date)
    assertNotNull(result.hitIndexMap[key])
  }

  @Test
  fun `search matches time field`() = runTest {
    val result = searchLogs(
      logs = listOf(createLog(id = 0, time = "12:30:00.000")),
      appInfoMap = emptyMap(),
      searchQuery = "12:30",
    )
    val key = SearchHitKey(logId = 0, component = LogComponent.Time)
    assertNotNull(result.hitIndexMap[key])
  }

  @Test
  fun `search matches pid field`() = runTest {
    val result = searchLogs(
      logs = listOf(createLog(id = 0, pid = "1234")),
      appInfoMap = emptyMap(),
      searchQuery = "1234",
    )
    val key = SearchHitKey(logId = 0, component = LogComponent.Pid)
    assertNotNull(result.hitIndexMap[key])
  }

  @Test
  fun `search matches tid field`() = runTest {
    val result = searchLogs(
      logs = listOf(createLog(id = 0, tid = "5678")),
      appInfoMap = emptyMap(),
      searchQuery = "5678",
    )
    val key = SearchHitKey(logId = 0, component = LogComponent.Tid)
    assertNotNull(result.hitIndexMap[key])
  }

  @Test
  fun `search matches package name via appInfoMap for numeric uid`() = runTest {
    val appInfoMap = mapOf(
      "10001" to createAppInfo(uid = "10001", packageName = "com.example.app"),
    )
    val result = searchLogs(
      logs = listOf(createLog(id = 0, uid = Log.Uid("10001"))),
      appInfoMap = appInfoMap,
      searchQuery = "com.example",
    )
    val key = SearchHitKey(logId = 0, component = LogComponent.PackageName)
    assertNotNull(result.hitIndexMap[key])
  }

  @Test
  fun `search matches non-numeric uid value directly as package name`() = runTest {
    val result = searchLogs(
      logs = listOf(createLog(id = 0, uid = Log.Uid("com.test.pkg"))),
      appInfoMap = emptyMap(),
      searchQuery = "com.test",
    )
    val key = SearchHitKey(logId = 0, component = LogComponent.PackageName)
    assertNotNull(result.hitIndexMap[key])
  }

  @Test
  fun `search with no uid does not match package name`() = runTest {
    val result = searchLogs(
      logs = listOf(createLog(id = 0, uid = null)),
      appInfoMap = emptyMap(),
      searchQuery = "com.example",
    )
    val key = SearchHitKey(logId = 0, component = LogComponent.PackageName)
    assertTrue(result.hitIndexMap[key] == null)
  }

  @Test
  fun `search with regex`() = runTest {
    val result = searchLogs(
      logs = listOf(createLog(id = 0, msg = "error code 404")),
      appInfoMap = emptyMap(),
      searchRegex = Regex("\\d+"),
    )
    val key = SearchHitKey(logId = 0, component = LogComponent.Message)
    assertNotNull(result.hitIndexMap[key])
    val hit = result.hits[result.hitIndexMap[key]!![0].value]
    assertEquals(11, hit.span.start)
    assertEquals(14, hit.span.end)
  }

  @Test
  fun `search with regex finds multiple matches`() = runTest {
    val result = searchLogs(
      logs = listOf(createLog(id = 0, msg = "foo123bar456")),
      appInfoMap = emptyMap(),
      searchRegex = Regex("\\d+"),
    )
    val key = SearchHitKey(logId = 0, component = LogComponent.Message)
    assertNotNull(result.hitIndexMap[key])
    assertEquals(2, result.hitIndexMap[key]!!.size)
  }

  @Test
  fun `hit index values are correct`() = runTest {
    val result = searchLogs(
      logs = listOf(
        createLog(id = 0, tag = "match", msg = "match"),
      ),
      appInfoMap = emptyMap(),
      searchQuery = "match",
    )
    val tagKey = SearchHitKey(logId = 0, component = LogComponent.Tag)
    val msgKey = SearchHitKey(logId = 0, component = LogComponent.Message)

    val tagHitIndex = result.hitIndexMap[tagKey]!![0].value
    val msgHitIndex = result.hitIndexMap[msgKey]!![0].value

    assertTrue(tagHitIndex != msgHitIndex)
    assertTrue(tagHitIndex >= 0 && tagHitIndex < result.hits.size)
    assertTrue(msgHitIndex >= 0 && msgHitIndex < result.hits.size)
  }

  @Test
  fun `search with large log list uses chunked path`() = runTest {
    val logs = (0 until 1500).map { i ->
      createLog(id = i, tag = "Tag$i", msg = if (i % 100 == 0) "target" else "other")
    }
    val result = searchLogs(
      logs = logs,
      appInfoMap = emptyMap(),
      searchQuery = "target",
    )
    // 15 logs have "target" in msg (i = 0, 100, 200, ..., 1400)
    assertEquals(15, result.hitIndexMap.count { it.key.component == LogComponent.Message })
  }

  private fun createAppInfo(
    uid: String,
    packageName: String,
  ) = AppInfo(
    uid = uid,
    packageName = packageName,
    name = packageName,
    enabled = true,
    icon = ColorDrawable(),
    isSystem = false,
  )

  private fun createLog(
    id: Int = 0,
    date: String = "01-01",
    time: String = "00:00:00.000",
    uid: Log.Uid? = Log.Uid("1000"),
    pid: String = "100",
    tid: String = "200",
    priority: LogPriority = LogPriority.DEBUG,
    tag: String = "TestTag",
    msg: String = "test message",
  ) = Log(
    id = id,
    date = date,
    time = time,
    uid = uid,
    pid = pid,
    tid = tid,
    priority = priority,
    tag = tag,
    msg = msg,
  )
}
