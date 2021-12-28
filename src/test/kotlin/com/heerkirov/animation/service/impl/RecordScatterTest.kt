package com.heerkirov.animation.service.impl

import com.heerkirov.animation.model.data.ScatterRecord
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class RecordScatterTest {
    @Test fun `test group in scatter record`() {
        assertEquals(
            Pair(
                listOf(
                    ScatterRecord(1, "2020-10-01T00:00:00Z")
                ),
                listOf(
                    LocalDateTime.of(2020, 10, 10, 0, 0),
                    LocalDateTime.of(2020, 10, 7, 0, 0)
                )
            ),
            RecordScatterServiceImpl.Companion.groupInScatterRecord(
                listOf(
                    ScatterRecord(1, "2020-10-01T00:00:00Z"),
                    ScatterRecord(1, "2020-10-10T00:00:00Z"),
                    ScatterRecord(2, "2020-10-07T00:00:00Z")
                ),
                1, 12,
                LocalDateTime.of(2020, 10, 30, 0, 0)
            )
        )
    }
}