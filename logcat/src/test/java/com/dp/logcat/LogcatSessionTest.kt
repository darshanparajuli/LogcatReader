package com.dp.logcat

import android.net.Uri
import com.dp.logcatapp.util.ProcessStarter
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.BufferedWriter
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.StringWriter

@RunWith(RobolectricTestRunner::class)
class LogcatSessionTest {
  // -- Initial state --

  @Test
  fun `isPaused is false on fresh session`() {
    val session = createSession()
    assertFalse(session.isPaused)
  }

  @Test
  fun `isRecording is false on fresh session`() {
    val session = createSession()
    assertFalse(session.isRecording)
  }

  // -- isPaused behavior --

  @Test
  fun `isPaused can be set to true`() = runTest {
    val session = createSession()
    assertTrue(session.start())
    try {
      session.isPaused = true
      assertTrue(session.isPaused)
    } finally {
      session.stop()
    }
  }

  @Test
  fun `isPaused can be toggled back to false`() = runTest {
    val session = createSession()
    assertTrue(session.start())
    try {
      session.isPaused = true
      session.isPaused = false
      assertFalse(session.isPaused)
    } finally {
      session.stop()
    }
  }

  @Test
  fun `isPaused set to false when already false does not throw`() = runTest {
    val session = createSession()
    assertTrue(session.start())
    try {
      session.isPaused = false
      assertFalse(session.isPaused)
    } finally {
      session.stop()
    }
  }

  // -- start() preconditions --

  @Test(expected = IllegalStateException::class)
  fun `start after stop throws IllegalStateException`() = runTest {
    val session = createSession()
    session.stop()
    session.start()
  }

  @Test(expected = IllegalStateException::class)
  fun `start when already active throws IllegalStateException`() = runTest {
    val session = createSession()
    try {
      assertTrue(session.start())
      session.start()
    } finally {
      session.stop()
    }
  }

  @Test
  fun `cancelling coroutine calling start stops the session`() = runTest {
    val session = createSession()
    val job = launch {
      session.start()
    }
    // Yield until the continuation resumes (start sets active and resumes with result).
    while (job.isActive && !session.isActive) {
      kotlinx.coroutines.yield()
    }
    job.cancel(CancellationException("test cancellation"))
    job.join()
    // invokeOnCancellation calls stop(), which sets active = false.
    assertFalse(session.isActive)
  }

  @Test
  fun `start returns false when process fails to start`() = runTest {
    val session = createSession(processStarter = mockProcessStarter(failToStart = true))
    assertFalse(session.start())
    session.stop()
  }

  // -- stop() behavior --

  @Test
  fun `stop on fresh session without start does not throw`() {
    val session = createSession()
    session.stop()
    assertFalse(session.isPaused)
    assertFalse(session.isRecording)
  }

  // -- Recording lifecycle --

  @Test
  fun `startRecording sets isRecording to true`() = runTest {
    val session = createSession()
    assertTrue(session.start())
    try {
      val writer = BufferedWriter(StringWriter())
      session.startRecording(createRecordingFileInfo(), writer)
      assertTrue(session.isRecording)
      session.stopRecording()
    } finally {
      session.stop()
    }
  }

  @Test
  fun `stopRecording returns the RecordingFileInfo passed to startRecording`() = runTest {
    val session = createSession()
    assertTrue(session.start())
    try {
      val info = createRecordingFileInfo(fileName = "my_log.txt")
      val writer = BufferedWriter(StringWriter())
      session.startRecording(info, writer)
      val result = session.stopRecording()
      assertEquals(info, result)
    } finally {
      session.stop()
    }
  }

  @Test
  fun `isRecording is false after stopRecording`() = runTest {
    val session = createSession()
    assertTrue(session.start())
    try {
      val writer = BufferedWriter(StringWriter())
      session.startRecording(createRecordingFileInfo(), writer)
      session.stopRecording()
      assertFalse(session.isRecording)
    } finally {
      session.stop()
    }
  }

  @Test
  fun `stopRecording when not recording returns null`() = runTest {
    val session = createSession()
    assertTrue(session.start())
    try {
      assertNull(session.stopRecording())
    } finally {
      session.stop()
    }
  }

  @Test
  fun `startRecording when already recording is a no-op`() = runTest {
    val session = createSession()
    assertTrue(session.start())
    try {
      val info1 = createRecordingFileInfo(fileName = "first.log")
      val info2 = createRecordingFileInfo(fileName = "second.log")
      val writer1 = BufferedWriter(StringWriter())
      val writer2 = BufferedWriter(StringWriter())
      session.startRecording(info1, writer1)
      session.startRecording(info2, writer2)
      val result = session.stopRecording()
      assertEquals(info1, result)
    } finally {
      session.stop()
    }
  }

  @Test
  fun `stopRecording can be called multiple times safely`() = runTest {
    val session = createSession()
    assertTrue(session.start())
    try {
      val writer = BufferedWriter(StringWriter())
      session.startRecording(createRecordingFileInfo(), writer)
      val first = session.stopRecording()
      val second = session.stopRecording()
      assertEquals(createRecordingFileInfo(), first)
      assertNull(second)
      assertFalse(session.isRecording)
    } finally {
      session.stop()
    }
  }

  // -- setFilters and clearLogs --

  @Test
  fun `setFilters with empty inclusion filters does not throw`() = runTest {
    val session = createSession()
    assertTrue(session.start())
    try {
      session.setFilters(emptyList())
    } finally {
      session.stop()
    }
  }

  @Test
  fun `setFilters with exclusion flag does not throw`() = runTest {
    val session = createSession()
    assertTrue(session.start())
    try {
      session.setFilters(emptyList(), exclusion = true)
    } finally {
      session.stop()
    }
  }

  @Test
  fun `clearLogs does not throw`() = runTest {
    val session = createSession()
    assertTrue(session.start())
    try {
      session.clearLogs()
    } finally {
      session.stop()
    }
  }

  // -- logs Flow --

  @Test
  fun `logs flow emits empty list from started session`() = runTest {
    val session = createSession()
    assertTrue(session.start())
    try {
      val result = session.logs.first()
      assertTrue(result.isEmpty())
    } finally {
      session.stop()
    }
  }

  // -- Helpers --

  private fun createSession(
    capacity: Int = 1000,
    buffers: Set<String> = setOf("main"),
    pollIntervalMs: Long = 250,
    processStarter: ProcessStarter = mockProcessStarter(),
  ) = LogcatSession(
    capacity = capacity,
    buffers = buffers,
    pollIntervalMs = pollIntervalMs,
    processStarter = processStarter,
  )

  private fun mockProcessStarter(
    failToStart: Boolean = false,
  ): ProcessStarter {
    val process = mockk<Process> {
      every { inputStream } returns ByteArrayInputStream(ByteArray(0))
      every { errorStream } returns ByteArrayInputStream(ByteArray(0))
      every { exitValue() } returns 0
      every { waitFor() } returns 0
      every { destroy() } returns Unit
      every { destroyForcibly() } returns this@mockk
    }
    return mockk {
      if (failToStart) {
        every { start(any(), any()) } throws IOException("mock: failed to start process")
      } else {
        every { start(any(), any()) } returns process
      }
    }
  }

  private fun createRecordingFileInfo(
    fileName: String = "test.log",
    uri: Uri = Uri.EMPTY,
    isCustomLocation: Boolean = false,
  ) = LogcatSession.RecordingFileInfo(
    fileName = fileName,
    uri = uri,
    isCustomLocation = isCustomLocation,
  )
}
