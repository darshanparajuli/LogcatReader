package com.dp.logcatapp.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ByteUnitFormatterTest {

  @Test
  fun `format zero bytes`() {
    assertEquals("0.00 B", ByteUnitFormatter.format(0))
  }

  @Test
  fun `format bytes below 1 KB`() {
    assertEquals("1.00 B", ByteUnitFormatter.format(1))
    assertEquals("512.00 B", ByteUnitFormatter.format(512))
    assertEquals("1023.00 B", ByteUnitFormatter.format(1023))
  }

  @Test
  fun `format exactly 1 KB`() {
    assertEquals("1.00 KB", ByteUnitFormatter.format(1024))
  }

  @Test
  fun `format kilobytes`() {
    assertEquals("1.50 KB", ByteUnitFormatter.format(1536))
    assertEquals("10.00 KB", ByteUnitFormatter.format(10 * 1024))
  }

  @Test
  fun `format exactly 1 MB`() {
    assertEquals("1.00 MB", ByteUnitFormatter.format(1024L * 1024))
  }

  @Test
  fun `format megabytes`() {
    assertEquals("5.50 MB", ByteUnitFormatter.format((5.5 * 1024 * 1024).toLong()))
  }

  @Test
  fun `format exactly 1 GB`() {
    assertEquals("1.00 GB", ByteUnitFormatter.format(1024L * 1024 * 1024))
  }

  @Test
  fun `format gigabytes`() {
    assertEquals("2.00 GB", ByteUnitFormatter.format(2L * 1024 * 1024 * 1024))
  }

  @Test
  fun `format exactly 1 TB`() {
    assertEquals("1.00 TB", ByteUnitFormatter.format(1024L * 1024 * 1024 * 1024))
  }

  @Test
  fun `format large value stays at TB`() {
    assertEquals("1024.00 TB", ByteUnitFormatter.format(1024L * 1024 * 1024 * 1024 * 1024))
  }
}
