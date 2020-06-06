package com.heerkirov.animation.util.transform

import com.heerkirov.animation.util.ktorm.StringConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class V1TimestampStrConverter : StringConverter<LocalDateTime> {
    private val dateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override fun getter(string: String): LocalDateTime {
        return LocalDateTime.parse(string.substring(0, 19), dateTimeFormat)
    }

    override fun setter(obj: LocalDateTime): String {
        return obj.format(dateTimeFormat)
    }
}