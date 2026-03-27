package com.dp.logcat

import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LogcatUtilTest {

  private lateinit var tempDir: File

  @Before
  fun setUp() {
    tempDir = File(System.getProperty("java.io.tmpdir"), "logcat_util_test")
    tempDir.mkdirs()
  }

  @After
  fun tearDown() {
    tempDir.deleteRecursively()
  }

  private fun createLog(
    id: Int = 0,
    date: String = "01-15",
    time: String = "12:34:56.789",
    uid: Log.Uid? = Log.Uid("1000"),
    pid: String = "1234",
    tid: String = "5678",
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

  // -- writeToFile tests --

  @Test
  fun `writeToFile with empty list creates empty file`() {
    val file = File(tempDir, "empty.log")
    val result = LogcatUtil.writeToFile(logs = emptyList(), file = file)
    assertTrue(result)
    assertTrue(file.exists())
    assertEquals("", file.readText())
  }

  @Test
  fun `writeToFile with single log`() {
    val file = File(tempDir, "single.log")
    val log = createLog(msg = "Hello world")
    val result = LogcatUtil.writeToFile(logs = listOf(log), file = file)
    assertTrue(result)
    assertEquals(log.toString(), file.readText())
  }

  @Test
  fun `writeToFile with multiple logs`() {
    val file = File(tempDir, "multi.log")
    val logs = listOf(
      createLog(id = 0, msg = "First"),
      createLog(id = 1, msg = "Second"),
      createLog(id = 2, msg = "Third"),
    )
    val result = LogcatUtil.writeToFile(logs = logs, file = file)
    assertTrue(result)
    val expected = logs.joinToString(separator = "") { it.toString() }
    assertEquals(expected, file.readText())
  }

  @Test
  fun `writeToFile overwrites existing content`() {
    val file = File(tempDir, "overwrite.log")
    file.writeText("old content")

    val log = createLog(msg = "new content")
    LogcatUtil.writeToFile(logs = listOf(log), file = file)
    assertEquals(log.toString(), file.readText())
  }

  @Test
  fun `writeToFile returns false for invalid path`() {
    val file = File("/nonexistent/path/test.log")
    val result = LogcatUtil.writeToFile(logs = listOf(createLog()), file = file)
    assertFalse(result)
  }

  // -- countLogs tests --

  /** Builds a logcat entry: metadata line, message lines, then a blank-line terminator. */
  private fun logcatEntry(metadata: String, vararg msgLines: String): String = buildString {
    appendLine(metadata)
    for (line in msgLines) {
      appendLine(line)
    }
    appendLine()
  }

  @Test
  fun `countLogs with empty file returns 0`() = runBlocking {
    val file = File(tempDir, "empty.log")
    file.writeText("")
    assertEquals(0L, LogcatUtil.countLogs(file))
  }

  @Test
  fun `countLogs counts entries correctly`() = runBlocking {
    val file = File(tempDir, "multi.log")
    val content = logcatEntry("[ 01-15 12:00:00.000 1000:100:200 D/Tag ]", "msg one") +
      logcatEntry("[ 01-15 12:00:01.000 1000:100:200 I/Tag ]", "msg two") +
      logcatEntry("[ 01-15 12:00:02.000 1000:100:200 E/Tag ]", "msg three")
    file.writeText(content)
    assertEquals(3L, LogcatUtil.countLogs(file))
  }

  @Test
  fun `countLogs with multi-line messages`() = runBlocking {
    val file = File(tempDir, "multiline.log")
    val content = logcatEntry(
      "[ 01-15 12:00:00.000 1000:100:200 D/Tag ]",
      "line one",
      "line two",
      "line three",
    ) + logcatEntry("[ 01-15 12:00:01.000 1000:100:200 D/Tag ]", "single line")
    file.writeText(content)
    assertEquals(2L, LogcatUtil.countLogs(file))
  }

  @Test
  fun `countLogs with nonexistent file returns 0`() = runBlocking {
    val file = File(tempDir, "does_not_exist.log")
    assertEquals(0L, LogcatUtil.countLogs(file))
  }
}
