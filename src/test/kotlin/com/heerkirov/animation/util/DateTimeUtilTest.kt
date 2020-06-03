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
}