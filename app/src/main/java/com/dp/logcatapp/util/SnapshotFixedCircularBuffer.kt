package com.dp.logcatapp.util

import androidx.compose.runtime.snapshots.StateObject
import androidx.compose.runtime.snapshots.StateRecord
import androidx.compose.runtime.snapshots.readable
import androidx.compose.runtime.snapshots.writable
import com.logcat.collections.FixedCircularArray

class SnapshotFixedCircularBuffer<T> internal constructor(
  array: FixedCircularArray<T>,
) : StateObject, Iterable<T>, List<T> {

  override var firstStateRecord: StateRecord = Record(array)

  override fun prependStateRecord(value: StateRecord) {
    @Suppress("UNCHECKED_CAST")
    firstStateRecord = value as Record<T>
  }

  private val readable: FixedCircularArray<T>
    get() {
      @Suppress("UNCHECKED_CAST")
      return (firstStateRecord as Record<T>).readable(this).array
    }

  private fun <R> writable(block: FixedCircularArray<T>.() -> R): R {
    @Suppress("UNCHECKED_CAST")
    return (firstStateRecord as Record<T>).writable(this) {
      block(array)
    }
  }

  // Note: do not solely rely on observing this value for whatever logic as it won't change after
  // it reaches the capacity.
  override val size: Int
    get() = readable.size

  fun add(e: T) {
    writable {
      add(e)
    }
  }

  fun add(list: Iterable<T>) {
    writable {
      add(list)
    }
  }

  fun remove(e: T): T? {
    return writable {
      remove(e)
    }
  }

  fun removeAt(index: Int): T {
    return writable {
      removeAt(index)
    }
  }

  fun changeCapacity(newCapacity: Int) {
    @Suppress("UNCHECKED_CAST")
    (firstStateRecord as Record<T>).writable(this) {
      val newArray = FixedCircularArray<T>(capacity = newCapacity)
      newArray.add(array.toList())
      array = newArray
    }
  }

  override fun lastIndexOf(element: T): Int {
    val array = readable
    var i = array.size - 1
    while (i >= 0 && array[i] != element) {
      i--
    }
    return i
  }

  override fun listIterator(): ListIterator<T> {
    return SnapshotFixedCircularArrayIterator(array = readable)
  }

  override fun listIterator(index: Int): ListIterator<T> {
    return SnapshotFixedCircularArrayIterator(array = readable, index = index)
  }

  override fun subList(fromIndex: Int, toIndex: Int): List<T> {
    throw UnsupportedOperationException("subList is not supported")
  }

  operator fun plusAssign(e: T) = add(e)

  operator fun plusAssign(list: List<T>) = add(list)

  operator fun plusAssign(list: FixedCircularArray<T>) = add(list)

  fun clear() {
    writable {
      clear()
    }
  }

  override fun isEmpty() = size == 0

  override fun contains(element: T): Boolean {
    return readable.contains(element)
  }

  override fun iterator(): Iterator<T> =
    SnapshotFixedCircularArrayIterator(readable)

  override fun containsAll(elements: Collection<T>): Boolean {
    return readable.let { array ->
      elements.all { array.contains(it) }
    }
  }

  override fun get(index: Int): T {
    return readable[index]
  }

  override fun indexOf(element: T): Int {
    return readable.indexOf(element)
  }

  private class SnapshotFixedCircularArrayIterator<T>(
    private val array: FixedCircularArray<T>,
    var index: Int = 0
  ) : ListIterator<T> {

    override fun hasNext(): Boolean = index < array.size

    override fun hasPrevious(): Boolean {
      return index > 0
    }

    override fun previous(): T {
      if (index == 0) {
        throw NoSuchElementException()
      }
      return array[index--]
    }

    override fun nextIndex(): Int {
      if (index >= array.size) return index
      return index + 1
    }

    override fun previousIndex(): Int {
      if (index == 0) return -1
      return index - 1
    }

    override fun next(): T = array[index++]
  }

  private inner class Record<T>(
    var array: FixedCircularArray<T>,
  ) : StateRecord() {
    override fun create(): StateRecord {
      return Record(array)
    }

    override fun assign(value: StateRecord) {
      value as Record<T>
      array = value.array
    }
  }
}

fun <T> mutableFixedCircularBuffer(
  capacity: Int
): SnapshotFixedCircularBuffer<T> {
  return SnapshotFixedCircularBuffer(FixedCircularArray(capacity))
}
