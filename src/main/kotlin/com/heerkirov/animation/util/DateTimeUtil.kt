package com.heerkirov.animation.util

import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.regex.Pattern

private val dateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private val monthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

private val monthPattern: Pattern = Pattern.compile("""(\d{1,4})(-(\d{1,2}))?""")

object DateTimeUtil {
    fun now(): LocalDateTime = LocalDateTime.now(Clock.systemUTC())
}

fun String.toDate(): LocalDate = LocalDate.parse(this, dateFormat)

fun String.toDateTime(): LocalDateTime = LocalDateTime.parse(this, dateTimeFormat)

fun String.toDateMonth(): LocalDate? {
    val (year, month) = this.toYearAndMonth() ?: return null
    return LocalDate.of(year, month ?: return null, 1)
}

fun LocalDate.toDateMonthString(): String = this.format(monthFormat)

fun LocalDateTime.toDateTimeString(): String = this.format(dateTimeFormat)

fun String.toYearAndMonth(): Pair<Int, Int?>? {
    val match = monthPattern.matcher(this)
    if(match.find()) {
        val year = match.group(1).toInt()
        val month = match.group(3)?.toInt()
        return Pair(year, month)
    }
    return null
}