package com.dp.logcatapp.util

import androidx.compose.runtime.snapshots.StateObject
import androidx.compose.runtime.snapshots.StateRecord
import androidx.compose.runtime.snapshots.readable
import androidx.compose.runtime.snapshots.writable
import com.logcat.collections.FixedCircularBuffer

class SnapshotFixedCircularBuffer<E> internal constructor(
  buffer: FixedCircularBuffer<E>,
) : StateObject, List<E> {

  override var firstStateRecord: StateRecord = FixedCircularBufferStateRecord(buffer)
    private set

  override fun prependStateRecord(value: StateRecord) {
    @Suppress("UNCHECKED_CAST")
    firstStateRecord = value as FixedCircularBufferStateRecord<E>
  }

  private val readable: FixedCircularBufferStateRecord<E>
    get() {
      @Suppress("UNCHECKED_CAST")
      return (firstStateRecord as FixedCircularBufferStateRecord<E>).readable(this)
    }

  private val readableBuffer: FixedCircularBuffer<E>
    get() = readable.buffer

  private fun <R> writable(block: FixedCircularBufferStateRecord<E>.() -> R): R {
    @Suppress("UNCHECKED_CAST")
    return (firstStateRecord as FixedCircularBufferStateRecord<E>).writable(this, block)
  }

  // Note: do not solely rely on observing this value for whatever logic as it won't change after
  // it reaches the capacity.
  override val size: Int
    get() = readableBuffer.size

  fun add(e: E) {
    writable {
      // TODO: this can be an expensive operation. One thing we can explore is to use linked array
      // buffers of small fixed size arrays instead, so that we would only need to copy/replace a
      // small array (size 32 maybe?) instead of the whole array.
      buffer = buffer.clone().also { it.add(e) }
    }
  }

  fun add(list: Iterable<E>) {
    writable {
      buffer = buffer.clone().also { it.add(list) }
    }
  }

  fun remove(e: E): E? {
    return writable {
      val clone = buffer.clone()
      val removed = clone.remove(e)
      buffer = clone
      removed
    }
  }

  fun removeAt(index: Int): E {
    return writable {
      val clone = buffer.clone()
      val removed = clone.removeAt(index)
      buffer = clone
      removed
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
    writable {
      buffer = buffer.clone().also { it.clear() }
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

  private class FixedCircularBufferStateRecord<T>(
    var buffer: FixedCircularBuffer<T>,
  ) : StateRecord() {
    override fun create(): StateRecord {
      return FixedCircularBufferStateRecord(buffer)
    }

    override fun assign(value: StateRecord) {
      value as FixedCircularBufferStateRecord<T>
      buffer = value.buffer
    }
  }
}

fun <T> mutableFixedCircularBuffer(
  capacity: Int
): SnapshotFixedCircularBuffer<T> {
  return SnapshotFixedCircularBuffer(FixedCircularBuffer(capacity))
}
