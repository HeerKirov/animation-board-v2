package com.heerkirov.animation.util

import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val dateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

object DateTimeUtil {
    fun now(): LocalDateTime = LocalDateTime.now(Clock.systemUTC())
}

fun String.toDate(): LocalDate = LocalDate.parse(this, dateFormat)

fun String.toDateTime(): LocalDateTime = LocalDateTime.parse(this, dateTimeFormat)

fun LocalDateTime.toDateTimeString(): String = this.format(dateTimeFormat)