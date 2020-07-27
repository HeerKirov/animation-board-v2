package com.heerkirov.animation.service.statistics

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class TimelineTest {
    @Test fun testGetTimePointOfProgress() {
        assertEquals(emptyList(), getTimePointOfProgress(emptyList(), 12, null, null))
        assertEquals(emptyList(), getTimePointOfProgress(listOf(null, null), 12, null, null))

        assertEquals(
                listOf(
                        LocalDate.of(2020, 1, 1),
                        LocalDate.of(2020, 1, 1),
                        LocalDate.of(2020, 1, 1)
                ),
                getTimePointOfProgress(emptyList(), 3, null, LocalDate.of(2020, 1, 1))
        )

        assertEquals(
                listOf(
                        LocalDate.of(2020, 1, 1),
                        LocalDate.of(2020, 1, 1),
                        LocalDate.of(2020, 1, 1)
                ),
                getTimePointOfProgress(emptyList(), 3, LocalDate.of(2020, 1, 1), null)
        )

        assertEquals(
                listOf(
                        LocalDate.of(2020, 1, 1),
                        LocalDate.of(2020, 1, 3),
                        LocalDate.of(2020, 1, 4)
                ),
                getTimePointOfProgress(emptyList(), 3, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 4))
        )

        assertEquals(
                listOf(
                        LocalDate.of(2020, 1, 1),
                        LocalDate.of(2020, 1, 2),
                        LocalDate.of(2020, 1, 3)
                ),
                getTimePointOfProgress(listOf(
                        LocalDate.of(2020, 1, 1),
                        null,
                        LocalDate.of(2020, 1, 3)
                ), 3, null, null)
        )

        assertEquals(
                listOf(
                        LocalDate.of(2020, 1, 1),
                        LocalDate.of(2020, 1, 4),
                        LocalDate.of(2020, 1, 4)
                ),
                getTimePointOfProgress(listOf(
                        LocalDate.of(2020, 1, 1),
                        LocalDate.of(2020, 1, 4)
                ), 3, null, null)
        )

        assertEquals(
                listOf(
                        LocalDate.of(2020, 1, 1),
                        LocalDate.of(2020, 1, 4),
                        LocalDate.of(2020, 1, 5)
                ),
                getTimePointOfProgress(listOf(
                        LocalDate.of(2020, 1, 1),
                        LocalDate.of(2020, 1, 4)
                ), 3, LocalDate.of(2019, 12, 20), LocalDate.of(2020, 1, 5))
        )

        assertEquals(
                listOf(
                        LocalDate.of(2019, 12, 31),
                        LocalDate.of(2020, 1, 1),
                        LocalDate.of(2020, 1, 3),
                        LocalDate.of(2020, 1, 4)
                ),
                getTimePointOfProgress(listOf(
                        null,
                        null,
                        null,
                        LocalDate.of(2020, 1, 4)
                ), 4, LocalDate.of(2019, 12, 31), LocalDate.of(2020, 1, 4))
        )
    }
}