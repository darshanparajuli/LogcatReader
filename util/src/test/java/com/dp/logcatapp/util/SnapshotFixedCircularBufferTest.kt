package com.dp.logcatapp.util

import androidx.compose.runtime.snapshots.Snapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotFixedCircularBufferTest {

  // region Basic operations

  @Test
  fun `add and get elements`() {
    val buffer = mutableFixedCircularBuffer<Int>(5)
    buffer.add(10)
    buffer.add(20)
    buffer.add(30)

    assertEquals(3, buffer.size)
    assertEquals(10, buffer[0])
    assertEquals(20, buffer[1])
    assertEquals(30, buffer[2])
  }

  @Test
  fun `add wraps around at capacity`() {
    val buffer = mutableFixedCircularBuffer<Int>(3)
    buffer.add(1)
    buffer.add(2)
    buffer.add(3)
    buffer.add(4)

    assertEquals(3, buffer.size)
    assertEquals(2, buffer[0])
    assertEquals(3, buffer[1])
    assertEquals(4, buffer[2])
  }

  @Test
  fun `add iterable`() {
    val buffer = mutableFixedCircularBuffer<Int>(5)
    buffer.add(listOf(1, 2, 3))
    assertEquals(3, buffer.size)
    assertEquals(1, buffer[0])
    assertEquals(3, buffer[2])
  }

  @Test
  fun `remove element`() {
    val buffer = mutableFixedCircularBuffer<Int>(5)
    buffer.add(10)
    buffer.add(20)

    assertEquals(20, buffer.remove(20))
    assertEquals(1, buffer.size)
    assertEquals(10, buffer[0])
  }

  @Test
  fun `remove non-existent element returns null`() {
    val buffer = mutableFixedCircularBuffer<Int>(5)
    buffer.add(10)
    assertNull(buffer.remove(99))
    assertEquals(1, buffer.size)
  }

  @Test
  fun `removeAt index`() {
    val buffer = mutableFixedCircularBuffer<Int>(5)
    buffer.add(listOf(10, 20, 30))
    assertEquals(20, buffer.removeAt(1))
    assertEquals(2, buffer.size)
    assertEquals(listOf(10, 30), buffer.toList())
  }

  @Test
  fun `clear empties the buffer`() {
    val buffer = mutableFixedCircularBuffer<Int>(5)
    buffer.add(listOf(1, 2, 3))
    buffer.clear()
    assertTrue(buffer.isEmpty())
    assertEquals(0, buffer.size)
  }

  @Test
  fun `isEmpty and isFull`() {
    val buffer = mutableFixedCircularBuffer<Int>(2)
    assertTrue(buffer.isEmpty())
    assertFalse(buffer.isFull())

    buffer.add(1)
    assertFalse(buffer.isEmpty())
    assertFalse(buffer.isFull())

    buffer.add(2)
    assertFalse(buffer.isEmpty())
    assertTrue(buffer.isFull())
  }

  @Test
  fun `contains and containsAll`() {
    val buffer = mutableFixedCircularBuffer<Int>(5)
    buffer.add(listOf(1, 2, 3))

    assertTrue(buffer.contains(2))
    assertFalse(buffer.contains(99))
    assertTrue(buffer.containsAll(listOf(1, 3)))
    assertFalse(buffer.containsAll(listOf(1, 4)))
  }

  @Test
  fun `indexOf and lastIndexOf`() {
    val buffer = mutableFixedCircularBuffer<Int>(10)
    buffer.add(listOf(10, 20, 30, 20, 50))

    assertEquals(1, buffer.indexOf(20))
    assertEquals(3, buffer.lastIndexOf(20))
    assertEquals(-1, buffer.indexOf(99))
  }

  @Test
  fun `iterator yields correct elements`() {
    val buffer = mutableFixedCircularBuffer<Int>(5)
    buffer.add(listOf(10, 20, 30))

    val items = mutableListOf<Int>()
    for (item in buffer) {
      items += item
    }
    assertEquals(listOf(10, 20, 30), items)
  }

  @Test
  fun `listIterator forward and backward`() {
    val buffer = mutableFixedCircularBuffer<Int>(5)
    buffer.add(listOf(10, 20, 30))

    val iter = buffer.listIterator()
    assertEquals(10, iter.next())
    assertEquals(20, iter.next())
    assertTrue(iter.hasPrevious())
    assertEquals(30, iter.previous())
  }

  @Test
  fun `listIterator with starting index`() {
    val buffer = mutableFixedCircularBuffer<Int>(5)
    buffer.add(listOf(10, 20, 30, 40))

    val iter = buffer.listIterator(2)
    assertEquals(30, iter.next())
    assertEquals(40, iter.next())
    assertFalse(iter.hasNext())
  }

  @Test
  fun `subList returns correct range`() {
    val buffer = mutableFixedCircularBuffer<Int>(10)
    buffer.add(listOf(0, 1, 2, 3, 4))

    val sub = buffer.subList(1, 4)
    assertEquals(3, sub.size)
    assertEquals(listOf(1, 2, 3), sub.toList())
  }

  @Test
  fun `plusAssign single element`() {
    val buffer = mutableFixedCircularBuffer<Int>(5)
    buffer += 42
    assertEquals(1, buffer.size)
    assertEquals(42, buffer[0])
  }

  @Test
  fun `plusAssign list`() {
    val buffer = mutableFixedCircularBuffer<Int>(5)
    buffer += listOf(1, 2, 3)
    assertEquals(3, buffer.size)
    assertEquals(listOf(1, 2, 3), buffer.toList())
  }

  @Test
  fun `mutableFixedCircularBuffer factory creates working buffer`() {
    val buffer = mutableFixedCircularBuffer<String>(3)
    assertTrue(buffer.isEmpty())
    buffer.add("a")
    buffer.add("b")
    buffer.add("c")
    assertTrue(buffer.isFull())
    assertEquals("a", buffer[0])
  }

  @Test
  fun `changeCapacity preserves elements`() {
    val buffer = mutableFixedCircularBuffer<Int>(3)
    buffer.add(listOf(1, 2, 3))

    buffer.changeCapacity(5)
    assertEquals(3, buffer.size)
    assertEquals(listOf(1, 2, 3), buffer.toList())
    assertFalse(buffer.isFull())

    buffer.add(4)
    buffer.add(5)
    assertTrue(buffer.isFull())
  }

  // endregion

  // region Snapshot isolation

  @Test
  fun `writes in mutable snapshot are not visible globally`() {
    val buffer = mutableFixedCircularBuffer<Int>(5)
    buffer.add(1)

    val snapshot = Snapshot.takeMutableSnapshot()
    try {
      snapshot.enter {
        buffer.add(2)
        buffer.add(3)
        assertEquals(3, buffer.size)
      }

      // Global state should still only have the original element
      assertEquals(1, buffer.size)
      assertEquals(1, buffer[0])
    } finally {
      snapshot.dispose()
    }
  }

  @Test
  fun `writes become visible after snapshot apply`() {
    val buffer = mutableFixedCircularBuffer<Int>(5)
    buffer.add(1)

    val snapshot = Snapshot.takeMutableSnapshot()
    try {
      snapshot.enter {
        buffer.add(2)
        buffer.add(3)
      }

      // Not visible yet
      assertEquals(1, buffer.size)

      snapshot.apply().check()

      // Now visible
      assertEquals(3, buffer.size)
      assertEquals(listOf(1, 2, 3), buffer.toList())
    } finally {
      snapshot.dispose()
    }
  }

  @Test
  fun `disposed mutable snapshot discards changes`() {
    val buffer = mutableFixedCircularBuffer<Int>(5)
    buffer.add(listOf(1, 2))

    val snapshot = Snapshot.takeMutableSnapshot()
    snapshot.enter {
      buffer.add(3)
      buffer.add(4)
      assertEquals(4, buffer.size)
    }
    snapshot.dispose()

    // Changes should be discarded
    assertEquals(2, buffer.size)
    assertEquals(listOf(1, 2), buffer.toList())
  }

  @Test
  fun `read-only snapshot captures state at creation time`() {
    val buffer = mutableFixedCircularBuffer<Int>(5)
    buffer.add(listOf(1, 2))

    val snapshot = Snapshot.takeSnapshot()
    try {
      // Modify global state after snapshot was taken
      buffer.add(3)
      buffer.add(4)

      // Read-only snapshot should still see the original state
      snapshot.enter {
        assertEquals(2, buffer.size)
        assertEquals(listOf(1, 2), buffer.toList())
      }

      // Global state has the new elements
      assertEquals(4, buffer.size)
    } finally {
      snapshot.dispose()
    }
  }

  @Test
  fun `concurrent mutable snapshots are independent`() {
    val buffer = mutableFixedCircularBuffer<Int>(10)
    buffer.add(1)

    val snapshot1 = Snapshot.takeMutableSnapshot()
    val snapshot2 = Snapshot.takeMutableSnapshot()
    try {
      snapshot1.enter {
        buffer.add(10)
        assertEquals(2, buffer.size)
        assertEquals(listOf(1, 10), buffer.toList())
      }

      snapshot2.enter {
        buffer.add(20)
        // snapshot2 should not see snapshot1's write
        assertEquals(2, buffer.size)
        assertEquals(listOf(1, 20), buffer.toList())
      }

      // Global state unchanged
      assertEquals(1, buffer.size)

      snapshot1.apply().check()
      assertEquals(2, buffer.size)
      assertEquals(listOf(1, 10), buffer.toList())
    } finally {
      snapshot1.dispose()
      snapshot2.dispose()
    }
  }

  @Test
  fun `changeCapacity within snapshot is isolated`() {
    val buffer = mutableFixedCircularBuffer<Int>(3)
    buffer.add(listOf(1, 2, 3))
    assertTrue(buffer.isFull())

    val snapshot = Snapshot.takeMutableSnapshot()
    try {
      snapshot.enter {
        buffer.changeCapacity(5)
        assertFalse(buffer.isFull())
        buffer.add(4)
        assertEquals(4, buffer.size)
      }

      // Global state unaffected
      assertEquals(3, buffer.size)
      assertTrue(buffer.isFull())

      snapshot.apply().check()

      // After apply
      assertEquals(4, buffer.size)
      assertFalse(buffer.isFull())
    } finally {
      snapshot.dispose()
    }
  }

  @Test
  fun `clear within snapshot is isolated`() {
    val buffer = mutableFixedCircularBuffer<Int>(5)
    buffer.add(listOf(1, 2, 3))

    val snapshot = Snapshot.takeMutableSnapshot()
    try {
      snapshot.enter {
        buffer.clear()
        assertTrue(buffer.isEmpty())
      }

      // Global state still has elements
      assertEquals(3, buffer.size)
      assertFalse(buffer.isEmpty())

      snapshot.apply().check()

      // After apply, buffer is cleared
      assertTrue(buffer.isEmpty())
    } finally {
      snapshot.dispose()
    }
  }

  @Test
  fun `remove within snapshot is isolated`() {
    val buffer = mutableFixedCircularBuffer<Int>(5)
    buffer.add(listOf(10, 20, 30))

    val snapshot = Snapshot.takeMutableSnapshot()
    try {
      snapshot.enter {
        assertEquals(20, buffer.remove(20))
        assertEquals(2, buffer.size)
      }

      // Global state unchanged
      assertEquals(3, buffer.size)
      assertTrue(buffer.contains(20))

      snapshot.apply().check()

      assertEquals(2, buffer.size)
      assertFalse(buffer.contains(20))
    } finally {
      snapshot.dispose()
    }
  }

  @Test
  fun `changeCapacity on empty buffer then add does not crash`() {
    val buffer = mutableFixedCircularBuffer<Int>(3)
    buffer.changeCapacity(5)
    buffer.add(1)
    assertEquals(1, buffer.size)
    assertEquals(1, buffer[0])
  }

  // endregion
}
