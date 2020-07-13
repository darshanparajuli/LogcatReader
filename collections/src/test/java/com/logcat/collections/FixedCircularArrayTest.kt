package com.logcat.collections

import org.junit.Assert.*
import org.junit.Test

class FixedCircularArrayTest {

    @Test
    fun testAdd() {
        val array = FixedCircularArray<Int>(12)
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
        val array = FixedCircularArray<Int>(10)
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
        val array = FixedCircularArray<Int>(10)
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
        FixedCircularArray<Int>(0)
    }

    @Test
    fun testGet() {
        val array = FixedCircularArray<Int>(10)

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
        val array = FixedCircularArray<Int>(100)
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
        val array = FixedCircularArray<Int>(10)
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
        val array = FixedCircularArray<Int>(100)
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
        val array = FixedCircularArray<Int>(5)

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
}