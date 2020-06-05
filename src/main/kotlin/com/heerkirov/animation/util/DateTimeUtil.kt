package com.heerkirov.animation.util

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

private val dateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private val monthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

private val monthPattern: Pattern = Pattern.compile("""(\d{1,4})(-(\d{1,2}))?""")

/**
 * 本模块提供时间日期相关的转换处理。
 * 在此项目中，做出统一规定。
 * 所有的时间日期数据都使用不包含时区的LocalDate、LocalDateTime保存，都基于UTC时区而不是本地时区表示。
 * 项目对外的I/O接口，时间日期数据也都基于UTC时区表示，使用UTC时间戳。
 * 在部分需要切换至别的时区的业务中，使用工具函数将UTC LocalDateTime切换至目标时区ZonedDateTime，再进行处理。
 */
object DateTimeUtil {
    /**
     * 获得不包含时区的当前UTC时间。
     */
    fun now(): LocalDateTime = LocalDateTime.now(Clock.systemUTC())
}

/**
 * 将字符串解析为yyyy-MM-ddTHH:mm:ssZ的时间格式。
 */
fun String.parseDateTime(): LocalDateTime = LocalDateTime.parse(this, dateTimeFormat)

/**
 * 将字符串解析为yyyy-MM-dd的日期格式。
 */
fun String.parseDate(): LocalDate = LocalDate.parse(this, dateFormat)

/**
 * 将字符串解析为yyyy-MM的不包含日的日期格式。
 */
fun String.parseDateMonth(): LocalDate? {
    val (year, month) = this.parseYearAndMonth() ?: return null
    try {
        return LocalDate.of(year, month ?: return null, 1)
    }catch (e: DateTimeException) {
        return null
    }
}

/**
 * 将字符串按yyyy-MM解析为年和月，并且月可省略。
 */
fun String.parseYearAndMonth(): Pair<Int, Int?>? {
    val match = monthPattern.matcher(this)
    if(match.find()) {
        val year = match.group(1).toInt()
        val month = match.group(3)?.toInt()
        return Pair(year, month)
    }
    return null
}

/**
 * 将日期转换为yyyy-MM的不包含日的日期字符串。
 */
fun LocalDate.toDateMonthString(): String = this.format(monthFormat)

/**
 * 将时间转换为yyyy-MM-ddTHH:mm:ssZ的时间戳。
 */
fun LocalDateTime.toDateTimeString(): String = this.format(dateTimeFormat)

/**
 * 将时间转换为毫秒时间戳。
 */
fun LocalDateTime.toTimestamp(): Long = this.toEpochSecond(ZoneOffset.UTC) * 1000L

/**
 * 将UTC时间转换为目标时区的时间。
 */
fun LocalDateTime.asZonedTime(zoneId: ZoneId): ZonedDateTime = this.atZone(ZoneId.of("UTC")).withZoneSameInstant(zoneId)

/**
 * 将目标时区时间转换为UTC时间。
 */
fun ZonedDateTime.asUTCTime(): LocalDateTime = this.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()

/**
 * 计算a和b之间的周数差。
 * 例如，当a在本周，b在下周时，返回1。
 */
fun weekDuration(a: LocalDateTime, b: LocalDateTime): Int {
    val firstDayOfA = if(a.dayOfWeek.ordinal > 0) a.minusDays(a.dayOfWeek.ordinal.toLong()) else a
    val firstDayOfB = if(b.dayOfWeek.ordinal > 0) b.minusDays(b.dayOfWeek.ordinal.toLong()) else b
    return (Duration.between(firstDayOfA, firstDayOfB).toDays() / 7).toInt()
}