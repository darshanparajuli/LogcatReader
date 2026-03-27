package com.dp.logcat.collections

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FixedCircularBufferTest {

  @Test
  fun testAdd() {
    val array = FixedCircularBuffer<Int>(12)
    assertTrue(array.isEmpty())

    array.add(1)
    assertTrue(array.isNotEmpty())

    array.clear()
    assertTrue(array.isEmpty())

    for (i in 0..100) {
      array.add(i)
    }
    assertEquals(12, array.size)

    val expectedValues = arrayOf(89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100)
    for (i in 0 until array.size) {
      assertEquals(expectedValues[i], array[i])
    }
  }

  @Test
  fun testRemove() {
    val array = FixedCircularBuffer<Int>(10)
    array.add(5)
    assertEquals(5, array.remove(5))
    assertTrue(array.isEmpty())

    for (i in 0..100) {
      array.add(i)
    }

    println("================================")
    assertEquals(10, array.size)
    var index = 0
    while (array.isNotEmpty()) {
      assertEquals(91 + index++, array.removeAt(0))
    }
    assertTrue(array.isEmpty())

    for (i in 0 until 5) {
      array.add(i)
    }
    assertEquals(5, array.size)

    for (i in 0 until 20) {
      array.add(i)
    }
    assertEquals(10, array.size)

    index = 0
    while (array.isNotEmpty()) {
      assertEquals(10 + index++, array.removeAt(0))
    }
    assertTrue(array.isEmpty())
  }

  @Test
  fun testIndexOf() {
    val array = FixedCircularBuffer<Int>(10)
    assertEquals(-1, array.indexOf(12))

    for (i in 0..1000) {
      array += i
    }

    assertEquals(8, array.indexOf(999))

    array.clear()
    assertTrue(array.isEmpty())

    array += 1
    array += 5

    assertEquals(1, array.indexOf(5))
  }

  @Test(expected = IllegalStateException::class)
  fun testCapacityZero() {
    FixedCircularBuffer<Int>(0)
  }

  @Test
  fun testGet() {
    val array = FixedCircularBuffer<Int>(10)

    array += 3
    array += 5

    assertEquals(3, array[0])
    assertEquals(5, array[1])

    for (i in 0..100) {
      array += i
    }

    assertEquals(100, array[array.size - 1])
    assertEquals(91, array[0])
  }

  @Test
  fun testIsFull() {
    val array = FixedCircularBuffer<Int>(100)
    assertTrue(array.isEmpty())
    assertFalse(array.isFull())

    for (i in 0..98) {
      array += 1
    }

    assertTrue(array.isNotEmpty())
    assertFalse(array.isFull())

    array += 1
    assertTrue(array.isFull())
  }

  @Test
  fun testIterator() {
    val array = FixedCircularBuffer<Int>(10)
    val expected = mutableListOf<Int>()
    for (i in 0 until array.size) {
      array += i * i
      expected += i * i
    }

    val actual = mutableListOf<Int>()
    for (i in array) {
      actual += i
    }

    assertEquals(expected, actual)
  }

  @Test
  fun testContains() {
    val array = FixedCircularBuffer<Int>(100)
    array += 10
    array += 20

    assertTrue(10 in array)
    assertTrue(20 in array)
    assertTrue(30 !in array)

    for (i in 0..1000) {
      array += i
    }

    assertTrue(901 in array)
    assertTrue(1000 in array)
    assertTrue(900 !in array)
  }

  @Test
  fun testAddAfterRemove() {
    val array = FixedCircularBuffer<Int>(5)

    for (i in 1..5) {
      array += i
    }
    assertTrue(array.isNotEmpty())

    array.removeAt(0)
    array += 6

    val excepted = arrayOf(2, 3, 4, 5, 6)
    for (i in 0 until array.size) {
      assertEquals(excepted[i], array[i])
    }
  }

  @Test(expected = NegativeArraySizeException::class)
  fun testCapacityNegative() {
    FixedCircularBuffer<Int>(-1)
  }

  @Test(expected = IndexOutOfBoundsException::class)
  fun testGetOutOfBounds() {
    val buffer = FixedCircularBuffer<Int>(5)
    buffer.add(1)
    buffer[5]
  }

  @Test(expected = IndexOutOfBoundsException::class)
  fun testGetEmptyBuffer() {
    val buffer = FixedCircularBuffer<Int>(5)
    buffer[0]
  }

  @Test
  fun testAddIterable() {
    val buffer = FixedCircularBuffer<Int>(5)
    buffer.add(listOf(1, 2, 3))
    assertEquals(3, buffer.size)
    assertEquals(1, buffer[0])
    assertEquals(2, buffer[1])
    assertEquals(3, buffer[2])

    buffer.add(listOf(4, 5, 6, 7))
    assertEquals(5, buffer.size)
    assertEquals(3, buffer[0])
    assertEquals(7, buffer[4])
  }

  @Test
  fun testPlusAssignList() {
    val buffer = FixedCircularBuffer<Int>(5)
    buffer += listOf(1, 2, 3, 4, 5)
    assertEquals(5, buffer.size)
    for (i in 0 until 5) {
      assertEquals(i + 1, buffer[i])
    }
  }

  @Test
  fun testLastIndexOf() {
    val buffer = FixedCircularBuffer<Int>(10)
    buffer.add(1)
    buffer.add(2)
    buffer.add(3)
    buffer.add(2)
    buffer.add(5)

    assertEquals(3, buffer.lastIndexOf(2))
    assertEquals(0, buffer.lastIndexOf(1))
    assertEquals(4, buffer.lastIndexOf(5))
    assertEquals(-1, buffer.lastIndexOf(99))
  }

  @Test
  fun testContainsAll() {
    val buffer = FixedCircularBuffer<Int>(10)
    buffer.add(listOf(1, 2, 3, 4, 5))

    assertTrue(buffer.containsAll(listOf(1, 3, 5)))
    assertTrue(buffer.containsAll(listOf(1)))
    assertFalse(buffer.containsAll(listOf(1, 6)))
    assertFalse(buffer.containsAll(listOf(99)))
    assertTrue(buffer.containsAll(emptyList()))
  }

  @Test
  fun testRemoveNonExistent() {
    val buffer = FixedCircularBuffer<Int>(5)
    buffer.add(1)
    assertNull(buffer.remove(99))
    assertEquals(1, buffer.size)
  }

  @Test
  fun testRemoveAtMiddle() {
    val buffer = FixedCircularBuffer<Int>(5)
    buffer.add(listOf(1, 2, 3, 4, 5))
    assertEquals(3, buffer.removeAt(2))
    assertEquals(4, buffer.size)
    assertEquals(listOf(1, 2, 4, 5), buffer.toList())
  }

  @Test
  fun testListIterator() {
    val buffer = FixedCircularBuffer<Int>(5)
    buffer.add(listOf(10, 20, 30))

    val iter = buffer.listIterator()
    assertTrue(iter.hasNext())
    assertFalse(iter.hasPrevious())

    assertEquals(10, iter.next())
    assertTrue(iter.hasPrevious())
    assertEquals(20, iter.next())
    assertEquals(30, iter.next())
    assertFalse(iter.hasNext())
  }

  @Test
  fun testListIteratorWithIndex() {
    val buffer = FixedCircularBuffer<Int>(5)
    buffer.add(listOf(10, 20, 30, 40))

    val iter = buffer.listIterator(2)
    assertTrue(iter.hasNext())
    assertEquals(30, iter.next())
    assertEquals(40, iter.next())
    assertFalse(iter.hasNext())
  }

  @Test
  fun testListIteratorPrevious() {
    val buffer = FixedCircularBuffer<Int>(5)
    buffer.add(listOf(10, 20, 30))

    val iter = buffer.listIterator()
    iter.next()
    iter.next()

    assertTrue(iter.hasPrevious())
    assertEquals(30, iter.previous())
    assertEquals(20, iter.previous())
    assertFalse(iter.hasPrevious())
  }

  @Test(expected = NoSuchElementException::class)
  fun testListIteratorPreviousAtStart() {
    val buffer = FixedCircularBuffer<Int>(5)
    buffer.add(1)
    val iter = buffer.listIterator()
    iter.previous()
  }

  @Test
  fun testListIteratorNextAndPreviousIndex() {
    val buffer = FixedCircularBuffer<Int>(5)
    buffer.add(listOf(10, 20, 30))

    val iter = buffer.listIterator()
    assertEquals(-1, iter.previousIndex())
    assertEquals(1, iter.nextIndex())

    iter.next()
    assertEquals(0, iter.previousIndex())
    assertEquals(2, iter.nextIndex())
  }

  @Test
  fun testSubList() {
    val buffer = FixedCircularBuffer<Int>(10)
    buffer.add(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))

    val sub = buffer.subList(2, 6)
    assertEquals(4, sub.size)
    assertEquals(2, sub[0])
    assertEquals(3, sub[1])
    assertEquals(4, sub[2])
    assertEquals(5, sub[3])
    assertFalse(sub.isEmpty())
  }

  @Test
  fun testSubListContains() {
    val buffer = FixedCircularBuffer<Int>(10)
    buffer.add(listOf(0, 1, 2, 3, 4))

    val sub = buffer.subList(1, 4)
    assertTrue(sub.contains(1))
    assertTrue(sub.contains(3))
    assertFalse(sub.contains(0))
    assertFalse(sub.contains(4))
  }

  @Test
  fun testSubListIndexOf() {
    val buffer = FixedCircularBuffer<Int>(10)
    buffer.add(listOf(10, 20, 30, 20, 50))

    val sub = buffer.subList(1, 5)
    assertEquals(0, sub.indexOf(20))
    assertEquals(3, sub.indexOf(50))
    assertEquals(-1, sub.indexOf(10))
  }

  @Test
  fun testSubListLastIndexOf() {
    val buffer = FixedCircularBuffer<Int>(10)
    buffer.add(listOf(10, 20, 30, 20, 50))

    val sub = buffer.subList(1, 5)
    assertEquals(2, sub.lastIndexOf(20))
    assertEquals(-1, sub.lastIndexOf(10))
  }

  @Test
  fun testSubListContainsAll() {
    val buffer = FixedCircularBuffer<Int>(10)
    buffer.add(listOf(0, 1, 2, 3, 4))

    val sub = buffer.subList(1, 4)
    assertTrue(sub.containsAll(listOf(1, 2, 3)))
    assertFalse(sub.containsAll(listOf(1, 4)))
  }

  @Test
  fun testSubListIterator() {
    val buffer = FixedCircularBuffer<Int>(10)
    buffer.add(listOf(0, 1, 2, 3, 4))

    val sub = buffer.subList(1, 4)
    val items = mutableListOf<Int>()
    for (item in sub) {
      items += item
    }
    assertEquals(listOf(1, 2, 3), items)
  }

  @Test
  fun testSubListListIterator() {
    val buffer = FixedCircularBuffer<Int>(10)
    buffer.add(listOf(0, 1, 2, 3, 4))

    val sub = buffer.subList(1, 4)
    val iter = sub.listIterator()
    assertEquals(1, iter.next())
    assertEquals(2, iter.next())
    assertTrue(iter.hasPrevious())
    assertEquals(3, iter.previous())
  }

  @Test
  fun testSubListOfSubList() {
    val buffer = FixedCircularBuffer<Int>(10)
    buffer.add(listOf(0, 1, 2, 3, 4, 5))

    val sub = buffer.subList(1, 5)
    val subSub = sub.subList(1, 3)
    assertEquals(2, subSub.size)
    assertEquals(1, subSub[0])
    assertEquals(2, subSub[1])
  }

  @Test(expected = IllegalStateException::class)
  fun testSubListInvalidRange() {
    val buffer = FixedCircularBuffer<Int>(10)
    buffer.add(listOf(0, 1, 2))
    buffer.subList(2, 1)
  }

  @Test(expected = IllegalStateException::class)
  fun testSubListNegativeFromIndex() {
    val buffer = FixedCircularBuffer<Int>(10)
    buffer.add(listOf(0, 1, 2))
    buffer.subList(-1, 2)
  }

  @Test(expected = IllegalStateException::class)
  fun testSubListToIndexExceedsSize() {
    val buffer = FixedCircularBuffer<Int>(10)
    buffer.add(listOf(0, 1, 2))
    buffer.subList(0, 10)
  }

  @Test
  fun testAddWrapsAroundWithSmallInitialSize() {
    val buffer = FixedCircularBuffer<Int>(capacity = 8, initialSize = 2)
    for (i in 0 until 8) {
      buffer.add(i)
    }
    assertEquals(8, buffer.size)
    for (i in 0 until 8) {
      assertEquals(i, buffer[i])
    }

    buffer.add(100)
    assertEquals(8, buffer.size)
    assertEquals(1, buffer[0])
    assertEquals(100, buffer[7])
  }

  @Test
  fun testCloneBasic() {
    val buffer = FixedCircularBuffer<Int>(5)
    buffer.add(listOf(1, 2, 3))
    val clone = buffer.clone()
    assertEquals(buffer.size, clone.size)
    for (i in 0 until buffer.size) {
      assertEquals(buffer[i], clone[i])
    }
  }

  @Test
  fun testCloneIsIndependent() {
    val buffer = FixedCircularBuffer<Int>(5)
    buffer.add(listOf(1, 2, 3))
    val clone = buffer.clone()

    buffer.add(4)
    assertEquals(3, clone.size)
    assertEquals(listOf(1, 2, 3), clone.toList())

    clone.add(99)
    assertEquals(4, buffer.size)
    assertEquals(listOf(1, 2, 3, 4), buffer.toList())
  }

  @Test
  fun testCloneEmpty() {
    val buffer = FixedCircularBuffer<Int>(5)
    val clone = buffer.clone()
    assertTrue(clone.isEmpty())
    assertEquals(0, clone.size)
  }

  @Test
  fun testCloneWrappedAround() {
    val buffer = FixedCircularBuffer<Int>(5)
    for (i in 0 until 8) {
      buffer.add(i)
    }
    // Buffer now contains [3, 4, 5, 6, 7] with head wrapped
    val clone = buffer.clone()
    assertEquals(buffer.size, clone.size)
    assertEquals(buffer.toList(), clone.toList())
  }

  @Test
  fun testClonePreservesCapacity() {
    val buffer = FixedCircularBuffer<Int>(10)
    buffer.add(listOf(1, 2, 3))
    val clone = buffer.clone()
    assertEquals(buffer.capacity, clone.capacity)

    // Fill clone to capacity to verify it respects the same limit
    for (i in 0 until 10) {
      clone.add(i)
    }
    assertTrue(clone.isFull())
  }

  @Test
  fun testEqualsIdenticalBuffers() {
    val a = FixedCircularBuffer<Int>(5)
    val b = FixedCircularBuffer<Int>(5)
    a.add(listOf(1, 2, 3))
    b.add(listOf(1, 2, 3))
    assertEquals(a, b)
  }

  @Test
  fun testEqualsSameContentDifferentCapacity() {
    val a = FixedCircularBuffer<Int>(5)
    val b = FixedCircularBuffer<Int>(10)
    a.add(listOf(1, 2, 3))
    b.add(listOf(1, 2, 3))
    assertFalse(a == b)
  }

  @Test
  fun testHashCodeConsistentWithEquals() {
    val a = FixedCircularBuffer<Int>(5)
    val b = FixedCircularBuffer<Int>(5)
    a.add(listOf(1, 2, 3))
    b.add(listOf(1, 2, 3))
    assertEquals(a, b)
    assertEquals(a.hashCode(), b.hashCode())
  }
}