package com.heerkirov.animation.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import me.liuwj.ktorm.schema.TypeReference


fun <T> T.toJSONString(): String {
    return jacksonObjectMapper().writeValueAsString(this)
}

inline fun <reified T> String.parseJSONObject(): T {
    return jacksonObjectMapper().readValue(this, T::class.java)
}

fun <T: Any> String.parseJSONObject(typeReference: TypeReference<T>): T {
    return jacksonObjectMapper().readValue(this, jacksonObjectMapper().constructType(typeReference.referencedType))
}

fun String.parseJsonNode(): JsonNode {
    return jacksonObjectMapper().readTree(this)
}

inline fun <reified T> JsonNode.parseJSONObject(): T {
    return jacksonObjectMapper().convertValue(this, T::class.java)
}

fun <T: Any> JsonNode.parseJSONObject(clazz: Class<T>): T {
    return jacksonObjectMapper().convertValue(this, clazz)
}