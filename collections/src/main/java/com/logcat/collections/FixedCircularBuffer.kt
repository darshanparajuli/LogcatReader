package com.logcat.collections

import kotlin.math.min

class FixedCircularBuffer<E>(
  val capacity: Int,
  initialSize: Int = INITIAL_SIZE
) : List<E> {

  companion object {
    private const val INITIAL_SIZE = 16
  }

  private var array = arrayOfNulls<Any>(min(capacity, initialSize))
  private var head = 0
  private var next = 0

  init {
    resetHead()
    if (capacity <= 0) {
      throw IllegalStateException("capacity (= $capacity) must be > 0")
    }
  }

  override val size: Int
    get() {
      return when {
        head == -1 -> 0
        next <= head -> (capacity - head) + next
        else -> next - head
      }
    }

  fun add(list: Iterable<E>) {
    for (e in list) {
      add(e)
    }
  }

  fun add(e: E) {
    tryGrow()

    if (head < 0) {
      head = 0
    } else if (next == head) {
      head = ++head % capacity
    }

    array[next] = e
    next = ++next % capacity
  }

  fun remove(e: E): E? {
    if (e == null) {
      return null
    }

    val index = indexOf(e)
    if (index != -1) {
      return removeAt(index)
    }
    return null
  }

  @Suppress("unchecked_cast")
  fun removeAt(index: Int): E {
    checkIOBAndThrow(index)
    val result = array[(head + index) % capacity] as E
    for (i in index until (size - 1)) {
      array[(head + i) % capacity] = array[(head + i + 1) % capacity]
    }
    array[(head + size - 1) % capacity] = null

    next = (head + size - 1) % capacity
    if (next == head) {
      // empty array, reset
      resetHead()
    }

    return result
  }

  override fun indexOf(element: E): Int {
    if (element == null) {
      return -1
    }

    for (i in 0 until size) {
      if (array[(head + i) % capacity] == element) {
        return i
      }
    }

    return -1
  }

  override fun lastIndexOf(element: E): Int {
    var i = size - 1
    while (i >= 0 && get(i) != element) {
      i--
    }
    return i
  }

  override fun subList(fromIndex: Int, toIndex: Int): List<E> {
    check(fromIndex < toIndex) {
      "fromIndex ($fromIndex) must be less than toIndex ($toIndex)"
    }
    check(fromIndex >= 0) {
      "fromIndex ($fromIndex) must be >= 0"
    }
    check(toIndex <= size) {
      "toIndex ($toIndex) must be <= size ($size)"
    }
    return SubList(
      buffer = this,
      offset = fromIndex,
      size = toIndex - fromIndex,
    )
  }

  override operator fun get(index: Int): E {
    checkIOBAndThrow(index)
    @Suppress("unchecked_cast")
    return array[(head + index) % capacity] as E
  }

  private fun checkIOBAndThrow(index: Int) {
    if (index !in 0..<size) {
      throw IndexOutOfBoundsException("index = $index, size = $size")
    }
  }

  private fun tryGrow() {
    if (size < array.size || array.size == capacity) {
      return
    }

    val newSize = min(array.size * 2, capacity)
    val newArray = arrayOfNulls<Any>(newSize)
    System.arraycopy(array, 0, newArray, 0, array.size)
    array = newArray
  }

  operator fun plusAssign(e: E) = add(e)

  operator fun plusAssign(list: List<E>) = add(list)

  fun removeAll(): List<E> {
    val list = toList()
    clear()
    return list
  }

  fun clear() {
    resetHead()
    array.fill(null)
  }

  private fun resetHead() {
    head = -1
    next = 0
  }

  override fun isEmpty() = size == 0

  fun isFull() = size == capacity

  override operator fun contains(element: E): Boolean {
    for (i in 0 until size) {
      if (element == array[(head + i) % capacity]) {
        return true
      }
    }
    return false
  }

  override fun containsAll(elements: Collection<E>): Boolean {
    return elements.all { contains(it) }
  }

  override fun listIterator(): ListIterator<E> {
    return FixedCircularBufferIterator(buffer = this)
  }

  override fun listIterator(index: Int): ListIterator<E> {
    return FixedCircularBufferIterator(buffer = this, index = index)
  }

  override fun iterator(): Iterator<E> = FixedCircularBufferIterator(buffer = this)

  private data class SubList<E>(
    private val buffer: FixedCircularBuffer<E>,
    private val offset: Int,
    override val size: Int,
  ) : List<E> {

    override fun isEmpty(): Boolean {
      return size == 0
    }

    override fun contains(element: E): Boolean {
      var i = 0
      while (i < size) {
        if (buffer[i + offset] == element) return true
        i += 1
      }
      return false
    }

    override fun containsAll(elements: Collection<E>): Boolean {
      return elements.all { contains(it) }
    }

    override fun get(index: Int): E {
      return buffer[index + offset]
    }

    override fun indexOf(element: E): Int {
      var i = 0
      while (i < size) {
        if (buffer[i + offset] == element) return i
        i += 1
      }
      return -1
    }

    override fun lastIndexOf(element: E): Int {
      var i = size - 1
      while (i >= 0 && buffer[i + offset] != element) {
        i--
      }
      return i
    }

    override fun listIterator(): ListIterator<E> {
      return SubListIterator(subList = this)
    }

    override fun listIterator(index: Int): ListIterator<E> {
      return SubListIterator(subList = this, index = index)
    }

    override fun iterator(): Iterator<E> {
      return SubListIterator(subList = this)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<E> {
      check(fromIndex < toIndex) {
        "fromIndex ($fromIndex) must be less than toIndex ($toIndex)"
      }
      check(fromIndex >= 0) {
        "fromIndex ($fromIndex) must be >= 0"
      }
      check(toIndex <= size) {
        "toIndex ($toIndex) must be <= size ($size)"
      }
      return SubList(
        buffer = buffer, offset = fromIndex, size = toIndex - fromIndex,
      )
    }

    // TODO(darshan): add concurrent modification checks.
    private class SubListIterator<E>(
      private val subList: SubList<E>,
      private var index: Int = 0
    ) : ListIterator<E> {

      override fun hasNext(): Boolean = index < subList.size

      override fun hasPrevious(): Boolean {
        return index > 0
      }

      override fun previous(): E {
        if (index == 0) {
          throw NoSuchElementException()
        }
        return subList[index--]
      }

      override fun nextIndex(): Int {
        if (index >= subList.size) return index
        return index + 1
      }

      override fun previousIndex(): Int {
        if (index == 0) return -1
        return index - 1
      }

      override fun next(): E = subList[index++]
    }
  }

  // TODO(darshan): add concurrent modification checks.
  private class FixedCircularBufferIterator<E>(
    private val buffer: FixedCircularBuffer<E>,
    private var index: Int = 0
  ) : ListIterator<E> {

    override fun hasNext(): Boolean = index < buffer.size

    override fun hasPrevious(): Boolean {
      return index > 0
    }

    override fun previous(): E {
      if (index == 0) {
        throw NoSuchElementException()
      }
      return buffer[index--]
    }

    override fun nextIndex(): Int {
      if (index >= buffer.size) return index
      return index + 1
    }

    override fun previousIndex(): Int {
      if (index == 0) return -1
      return index - 1
    }

    override fun next(): E = buffer[index++]
  }
}