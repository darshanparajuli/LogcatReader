package com.dp.logcat

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogcatStreamReaderTest {

  private fun readerOf(text: String): LogcatStreamReader =
    LogcatStreamReader(ByteArrayInputStream(text.toByteArray()))

  /** Builds a logcat entry: metadata line, message lines, then a blank-line terminator. */
  private fun entry(metadata: String, vararg msgLines: String): String = buildString {
    appendLine(metadata)
    for (line in msgLines) {
      appendLine(line)
    }
    appendLine() // empty line terminator
  }

  @Test
  fun `empty input returns no logs`() {
    readerOf("").use { reader ->
      assertFalse(reader.hasNext())
    }
  }

  @Test
  fun `single log entry with uid`() {
    val input = entry("[ 01-15 12:34:56.789 1000:1234:5678 D/MyTag ]", "Hello world")

    readerOf(input).use { reader ->
      assertTrue(reader.hasNext())
      val log = reader.next()
      assertEquals(0, log.id)
      assertEquals("01-15", log.date)
      assertEquals("12:34:56.789", log.time)
      assertEquals(Log.Uid("1000"), log.uid)
      assertEquals("1234", log.pid)
      assertEquals("5678", log.tid)
      assertEquals(LogPriority.DEBUG, log.priority)
      assertEquals("MyTag", log.tag)
      assertEquals("Hello world", log.msg)
      assertFalse(reader.hasNext())
    }
  }

  @Test
  fun `single log entry without uid`() {
    val input = entry("[ 01-15 12:34:56.789 1234:5678 D/MyTag ]", "Hello world")

    readerOf(input).use { reader ->
      assertTrue(reader.hasNext())
      val log = reader.next()
      assertEquals(0, log.id)
      assertEquals("01-15", log.date)
      assertEquals("12:34:56.789", log.time)
      assertEquals(null, log.uid)
      assertEquals("1234", log.pid)
      assertEquals("5678", log.tid)
      assertEquals(LogPriority.DEBUG, log.priority)
      assertEquals("MyTag", log.tag)
      assertEquals("Hello world", log.msg)
      assertFalse(reader.hasNext())
    }
  }

  @Test
  fun `multiple log entries`() {
    val input = entry("[ 01-15 12:34:56.789 1000:1234:5678 D/TagA ]", "First message") +
      entry("[ 01-15 12:34:57.000 1000:1234:5678 I/TagB ]", "Second message")

    readerOf(input).use { reader ->
      assertTrue(reader.hasNext())
      val first = reader.next()
      assertEquals(0, first.id)
      assertEquals("First message", first.msg)
      assertEquals(LogPriority.DEBUG, first.priority)
      assertEquals("TagA", first.tag)

      assertTrue(reader.hasNext())
      val second = reader.next()
      assertEquals(1, second.id)
      assertEquals("Second message", second.msg)
      assertEquals(LogPriority.INFO, second.priority)
      assertEquals("TagB", second.tag)

      assertFalse(reader.hasNext())
    }
  }

  @Test
  fun `multi-line message`() {
    val input = entry(
      "[ 01-15 12:34:56.789 1000:1234:5678 E/MyTag ]",
      "Line one",
      "Line two",
      "Line three",
    )

    readerOf(input).use { reader ->
      assertTrue(reader.hasNext())
      val log = reader.next()
      assertEquals("Line one\nLine two\nLine three", log.msg)
      assertEquals(LogPriority.ERROR, log.priority)
      assertFalse(reader.hasNext())
    }
  }

  @Test
  fun `hasNext is idempotent`() {
    val input = entry("[ 01-15 12:34:56.789 1000:1234:5678 D/MyTag ]", "Message")

    readerOf(input).use { reader ->
      assertTrue(reader.hasNext())
      assertTrue(reader.hasNext())
      assertTrue(reader.hasNext())
      val log = reader.next()
      assertEquals("Message", log.msg)
      assertFalse(reader.hasNext())
    }
  }

  @Test
  fun `hasNext called multiple times does not skip entries`() {
    val input = entry("[ 01-15 12:34:56.789 1000:1234:5678 D/TagA ]", "First") +
      entry("[ 01-15 12:34:57.000 1000:1234:5678 D/TagB ]", "Second")

    readerOf(input).use { reader ->
      assertTrue(reader.hasNext())
      assertTrue(reader.hasNext())
      assertEquals("First", reader.next().msg)

      assertTrue(reader.hasNext())
      assertTrue(reader.hasNext())
      assertEquals("Second", reader.next().msg)

      assertFalse(reader.hasNext())
    }
  }

  @Test(expected = NullPointerException::class)
  fun `next without hasNext throws`() {
    readerOf("").use { reader ->
      reader.next()
    }
  }

  @Test
  fun `skips non-metadata lines before first log`() {
    val input = "some garbage line\nanother garbage line\n" +
      entry("[ 01-15 12:34:56.789 1000:1234:5678 D/MyTag ]", "Actual message")

    readerOf(input).use { reader ->
      assertTrue(reader.hasNext())
      val log = reader.next()
      assertEquals("Actual message", log.msg)
      assertFalse(reader.hasNext())
    }
  }

  @Test
  fun `all log priorities`() {
    val priorities = listOf(
      "V" to LogPriority.VERBOSE,
      "D" to LogPriority.DEBUG,
      "I" to LogPriority.INFO,
      "W" to LogPriority.WARNING,
      "E" to LogPriority.ERROR,
      "F" to LogPriority.FATAL,
      "A" to LogPriority.ASSERT,
    )

    for ((letter, expected) in priorities) {
      val input = entry("[ 01-15 12:00:00.000 1000:100:200 $letter/Tag ]", "msg")

      readerOf(input).use { reader ->
        assertTrue("Expected hasNext for priority $letter", reader.hasNext())
        val log = reader.next()
        assertEquals("Priority mismatch for $letter", expected, log.priority)
      }
    }
  }

  @Test
  fun `ids increment across entries`() {
    val input = entry("[ 01-15 12:00:00.000 1000:100:200 D/Tag ]", "a") +
      entry("[ 01-15 12:00:01.000 1000:100:200 D/Tag ]", "b") +
      entry("[ 01-15 12:00:02.000 1000:100:200 D/Tag ]", "c")

    readerOf(input).use { reader ->
      assertTrue(reader.hasNext())
      assertEquals(0, reader.next().id)
      assertTrue(reader.hasNext())
      assertEquals(1, reader.next().id)
      assertTrue(reader.hasNext())
      assertEquals(2, reader.next().id)
      assertFalse(reader.hasNext())
    }
  }

  @Test
  fun `close on already closed reader does not throw`() {
    val reader = readerOf("")
    reader.close()
    reader.close()
  }

  @Test
  fun `metadata with no message line returns false`() {
    val input = "[ 01-15 12:34:56.789 1000:1234:5678 D/MyTag ]"

    readerOf(input).use { reader ->
      assertFalse(reader.hasNext())
    }
  }

  @Test
  fun `iterate using for-in loop`() {
    val input = entry("[ 01-15 12:00:00.000 1000:100:200 D/Tag ]", "first") +
      entry("[ 01-15 12:00:01.000 1000:100:200 D/Tag ]", "second")

    val messages = mutableListOf<String>()
    readerOf(input).use { reader ->
      for (log in reader) {
        messages.add(log.msg)
      }
    }
    assertEquals(listOf("first", "second"), messages)
  }
}
