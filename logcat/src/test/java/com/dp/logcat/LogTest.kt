package com.dp.logcat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogTest {

  // -- Log.parse tests --

  @Test
  fun `parse with uid`() {
    val log = Log.parse(
      id = 0,
      metadata = "[ 01-15 12:34:56.789 1000:1234:5678 D/MyTag ]",
      msg = "Hello world",
    )
    assertEquals(0, log.id)
    assertEquals("01-15", log.date)
    assertEquals("12:34:56.789", log.time)
    assertEquals(Log.Uid("1000"), log.uid)
    assertEquals("1234", log.pid)
    assertEquals("5678", log.tid)
    assertEquals(LogPriority.DEBUG, log.priority)
    assertEquals("MyTag", log.tag)
    assertEquals("Hello world", log.msg)
  }

  @Test
  fun `parse without uid`() {
    val log = Log.parse(
      id = 5,
      metadata = "[ 03-22 09:00:00.000 4567:8910 I/SomeTag ]",
      msg = "No uid here",
    )
    assertEquals(5, log.id)
    assertEquals("03-22", log.date)
    assertEquals("09:00:00.000", log.time)
    assertNull(log.uid)
    assertEquals("4567", log.pid)
    assertEquals("8910", log.tid)
    assertEquals(LogPriority.INFO, log.priority)
    assertEquals("SomeTag", log.tag)
    assertEquals("No uid here", log.msg)
  }

  @Test
  fun `parse all priorities`() {
    val cases = listOf(
      "V" to LogPriority.VERBOSE,
      "D" to LogPriority.DEBUG,
      "I" to LogPriority.INFO,
      "W" to LogPriority.WARNING,
      "E" to LogPriority.ERROR,
      "F" to LogPriority.FATAL,
      "A" to LogPriority.ASSERT,
    )
    for ((letter, expected) in cases) {
      val log = Log.parse(
        id = 0,
        metadata = "[ 01-01 00:00:00.000 100:200 $letter/Tag ]",
        msg = "msg",
      )
      assertEquals("Priority mismatch for $letter", expected, log.priority)
    }
  }

  @Test
  fun `parse with extra spaces in metadata`() {
    val log = Log.parse(
      id = 0,
      metadata = "[ 01-15 12:34:56.789  1000:1234:5678 D/MyTag ]",
      msg = "extra spaces",
    )
    assertEquals(Log.Uid("1000"), log.uid)
    assertEquals("1234", log.pid)
    assertEquals("5678", log.tid)
  }

  @Test
  fun `parse trims tag`() {
    val log = Log.parse(
      id = 0,
      metadata = "[ 01-15 12:00:00.000 100:200 D/MyTag   ]",
      msg = "msg",
    )
    assertEquals("MyTag", log.tag)
  }

  @Test
  fun `parse with multi-line message`() {
    val log = Log.parse(
      id = 0,
      metadata = "[ 01-15 12:00:00.000 100:200 E/CrashTag ]",
      msg = "line one\nline two\nline three",
    )
    assertEquals("line one\nline two\nline three", log.msg)
  }

  @Test
  fun `parse preserves id`() {
    val log = Log.parse(
      id = 42,
      metadata = "[ 01-15 12:00:00.000 100:200 D/Tag ]",
      msg = "msg",
    )
    assertEquals(42, log.id)
  }

  @Test(expected = Exception::class)
  fun `parse with invalid priority throws`() {
    Log.parse(
      id = 0,
      metadata = "[ 01-15 12:00:00.000 100:200 X/Tag ]",
      msg = "msg",
    )
  }

  @Test(expected = Exception::class)
  fun `parse with malformed metadata throws`() {
    Log.parse(
      id = 0,
      metadata = "not valid metadata",
      msg = "msg",
    )
  }

  // -- metadataToString / toString tests --

  @Test
  fun `metadataToString format`() {
    val log = Log(
      id = 0,
      date = "01-15",
      time = "12:34:56.789",
      uid = Log.Uid("1000"),
      pid = "1234",
      tid = "5678",
      priority = LogPriority.DEBUG,
      tag = "MyTag",
      msg = "Hello",
    )
    assertEquals("[01-15 12:34:56.789 Uid(value=1000):1234:5678 D/MyTag]", log.metadataToString())
  }

  @Test
  fun `metadataToString with null uid`() {
    val log = Log(
      id = 0,
      date = "01-15",
      time = "12:34:56.789",
      uid = null,
      pid = "1234",
      tid = "5678",
      priority = LogPriority.DEBUG,
      tag = "MyTag",
      msg = "Hello",
    )
    assertEquals("[01-15 12:34:56.789 null:1234:5678 D/MyTag]", log.metadataToString())
  }

  @Test
  fun `toString contains metadata and message`() {
    val log = Log(
      id = 0,
      date = "01-15",
      time = "12:00:00.000",
      uid = null,
      pid = "100",
      tid = "200",
      priority = LogPriority.INFO,
      tag = "Tag",
      msg = "the message",
    )
    val expected = "${log.metadataToString()}\nthe message\n\n"
    assertEquals(expected, log.toString())
  }

  // -- Uid tests --

  @Test
  fun `uid isNum true for numeric value`() {
    assertTrue(Log.Uid("1000").isNum)
  }

  @Test
  fun `uid isNum true for zero`() {
    assertTrue(Log.Uid("0").isNum)
  }

  @Test
  fun `uid isNum false for non-numeric value`() {
    assertFalse(Log.Uid("u0_a123").isNum)
  }

  @Test
  fun `uid isNum false for empty string`() {
    assertFalse(Log.Uid("").isNum)
  }

  // -- LogPriority tests --

  @Test
  fun `LogPriority toString returns single letter`() {
    assertEquals("A", LogPriority.ASSERT.toString())
    assertEquals("D", LogPriority.DEBUG.toString())
    assertEquals("E", LogPriority.ERROR.toString())
    assertEquals("F", LogPriority.FATAL.toString())
    assertEquals("I", LogPriority.INFO.toString())
    assertEquals("V", LogPriority.VERBOSE.toString())
    assertEquals("W", LogPriority.WARNING.toString())
  }
}
