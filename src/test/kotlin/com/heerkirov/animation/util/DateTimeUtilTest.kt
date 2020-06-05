package com.heerkirov.animation.util

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeUtilTest {
    @Test fun testToDateTime() {
        assertEquals(LocalDateTime.of(2020, 2, 2, 12, 34, 56),
                "2020-02-02T12:34:56Z".parseDateTime()
        )
    }

    @Test fun testToDateTimeString() {
        assertEquals("2020-02-02T12:34:56Z",
                LocalDateTime.of(2020, 2, 2, 12, 34, 56).toDateTimeString()
        )
    }

    @Test fun testWeekDuration() {
        assertEquals(-2, weekDuration(
                LocalDateTime.of(2020, 6, 1, 0, 0),
                LocalDateTime.of(2020, 5, 24, 0, 0)))
        assertEquals(-1, weekDuration(
                LocalDateTime.of(2020, 6, 3, 0, 0),
                LocalDateTime.of(2020, 5, 25, 0, 0)))
        assertEquals(-1, weekDuration(
                LocalDateTime.of(2020, 6, 3, 0, 0),
                LocalDateTime.of(2020, 5, 31, 0, 0)))
        assertEquals(0, weekDuration(
                LocalDateTime.of(2020, 6, 3, 0, 0),
                LocalDateTime.of(2020, 6, 1, 0, 0)))
        assertEquals(0, weekDuration(
                LocalDateTime.of(2020, 6, 3, 0, 0),
                LocalDateTime.of(2020, 6, 3, 0, 0)))
        assertEquals(0, weekDuration(
                LocalDateTime.of(2020, 6, 3, 0, 0),
                LocalDateTime.of(2020, 6, 7, 0, 0)))
        assertEquals(0, weekDuration(
                LocalDateTime.of(2020, 6, 1, 0, 0),
                LocalDateTime.of(2020, 6, 7, 0, 0)))
        assertEquals(1, weekDuration(
                LocalDateTime.of(2020, 6, 7, 0, 0),
                LocalDateTime.of(2020, 6, 8, 0, 0)))
        assertEquals(1, weekDuration(
                LocalDateTime.of(2020, 6, 7, 0, 0),
                LocalDateTime.of(2020, 6, 14, 0, 0)))
        assertEquals(2, weekDuration(
                LocalDateTime.of(2020, 6, 7, 0, 0),
                LocalDateTime.of(2020, 6, 15, 0, 0)))
        assertEquals(2, weekDuration(
                LocalDateTime.of(2020, 6, 7, 0, 0),
                LocalDateTime.of(2020, 6, 21, 0, 0)))
        assertEquals(3, weekDuration(
                LocalDateTime.of(2020, 6, 7, 0, 0),
                LocalDateTime.of(2020, 6, 22, 0, 0)))
    }
}