package com.dp.logcatapp.util

import androidx.compose.runtime.snapshots.StateObject
import androidx.compose.runtime.snapshots.StateRecord
import androidx.compose.runtime.snapshots.readable
import androidx.compose.runtime.snapshots.writable
import com.logcat.collections.FixedCircularBuffer

class SnapshotFixedCircularBuffer<E> internal constructor(
  buffer: FixedCircularBuffer<E>,
) : StateObject, List<E> {

  override var firstStateRecord: StateRecord = Record(buffer)

  override fun prependStateRecord(value: StateRecord) {
    @Suppress("UNCHECKED_CAST")
    firstStateRecord = value as Record<E>
  }

  private val readable: Record<E>
    get() {
      @Suppress("UNCHECKED_CAST")
      return (firstStateRecord as Record<E>).readable(this)
    }

  private val readableBuffer: FixedCircularBuffer<E>
    get() = readable.buffer

  private fun <R> writable(block: SnapshotFixedCircularBuffer<E>.Record<E>.() -> R): R {
    @Suppress("UNCHECKED_CAST")
    return (firstStateRecord as Record<E>).writable(this, block)
  }

  private fun <R> writableBuffer(block: FixedCircularBuffer<E>.() -> R): R {
    return writable { buffer.block() }
  }

  // Note: do not solely rely on observing this value for whatever logic as it won't change after
  // it reaches the capacity.
  override val size: Int
    get() = readableBuffer.size

  fun add(e: E) {
    writableBuffer {
      add(e)
    }
  }

  fun add(list: Iterable<E>) {
    writableBuffer {
      add(list)
    }
  }

  fun remove(e: E): E? {
    return writableBuffer {
      remove(e)
    }
  }

  fun removeAt(index: Int): E {
    return writableBuffer {
      removeAt(index)
    }
  }

  fun changeCapacity(newCapacity: Int) {
    @Suppress("UNCHECKED_CAST")
    writable {
      val newBuffer = FixedCircularBuffer<E>(
        capacity = newCapacity,
        initialSize = buffer.size,
      )
      newBuffer += buffer
      buffer = newBuffer
    }
  }

  override fun lastIndexOf(element: E): Int {
    return readableBuffer.lastIndexOf(element)
  }

  override fun listIterator(): ListIterator<E> {
    return readableBuffer.listIterator()
  }

  override fun listIterator(index: Int): ListIterator<E> {
    return readableBuffer.listIterator(index)
  }

  override fun subList(fromIndex: Int, toIndex: Int): List<E> {
    return readableBuffer.subList(fromIndex = fromIndex, toIndex = toIndex)
  }

  operator fun plusAssign(e: E) = add(e)

  operator fun plusAssign(list: List<E>) = add(list)

  operator fun plusAssign(list: FixedCircularBuffer<E>) = add(list)

  fun clear() {
    writableBuffer {
      clear()
    }
  }

  override fun isEmpty() = readableBuffer.isEmpty()

  fun isFull() = readableBuffer.isFull()

  override fun contains(element: E): Boolean {
    return readableBuffer.contains(element)
  }

  override fun iterator(): Iterator<E> = readableBuffer.iterator()

  override fun containsAll(elements: Collection<E>): Boolean {
    return readableBuffer.containsAll(elements)
  }

  override fun get(index: Int): E {
    return readableBuffer[index]
  }

  override fun indexOf(element: E): Int {
    return readableBuffer.indexOf(element)
  }

  fun buffer(): List<E> = readable.buffer

  private inner class Record<T>(
    var buffer: FixedCircularBuffer<T>,
  ) : StateRecord() {
    override fun create(): StateRecord {
      return Record(buffer)
    }

    override fun assign(value: StateRecord) {
      value as Record<T>
      buffer = value.buffer
    }
  }
}

fun <T> mutableFixedCircularBuffer(
  capacity: Int
): SnapshotFixedCircularBuffer<T> {
  return SnapshotFixedCircularBuffer(FixedCircularBuffer(capacity))
}
