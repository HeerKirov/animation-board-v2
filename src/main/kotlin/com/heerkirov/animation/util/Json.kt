package com.heerkirov.animation.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import me.liuwj.ktorm.schema.TypeReference


fun <T> T.toJSONString(): String = jacksonObjectMapper().writeValueAsString(this)

inline fun <reified T> String.parseJSONObject(): T = jacksonObjectMapper().readValue(this, T::class.java)

fun <T: Any> String.parseJSONObject(typeReference: TypeReference<T>): T {
    return jacksonObjectMapper().readValue(this, jacksonObjectMapper().constructType(typeReference.referencedType))
}