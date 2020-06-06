package com.heerkirov.animation.util.ktorm

import com.heerkirov.animation.util.parseDateTime
import com.heerkirov.animation.util.toDateTimeString
import java.time.LocalDateTime

interface StringConverter<T: Any> {
    fun getter(string: String): T
    fun setter(obj: T): String
}

class DateTimeStrConverter : StringConverter<LocalDateTime> {
    override fun getter(string: String): LocalDateTime {
        return string.parseDateTime()
    }

    override fun setter(obj: LocalDateTime): String {
        return obj.toDateTimeString()
    }
}