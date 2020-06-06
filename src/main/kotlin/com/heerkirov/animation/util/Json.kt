package com.heerkirov.animation.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import me.liuwj.ktorm.schema.TypeReference

private val objectMapper = jacksonObjectMapper()

fun objectMapper(): ObjectMapper = objectMapper

fun <T> T.toJSONString(): String {
    return objectMapper.writeValueAsString(this)
}

fun <T> T.toJsonNode(): JsonNode {
    return objectMapper.valueToTree(this)
}

inline fun <reified T> String.parseJSONObject(): T {
    return objectMapper().readValue(this, T::class.java)
}

fun <T: Any> String.parseJSONObject(typeReference: TypeReference<T>): T {
    return objectMapper.readValue(this, objectMapper.constructType(typeReference.referencedType))
}

fun String.parseJsonNode(): JsonNode {
    return objectMapper.readTree(this)
}

inline fun <reified T> JsonNode.parseJSONObject(): T {
    return objectMapper().convertValue(this, T::class.java)
}

fun <T: Any> JsonNode.parseJSONObject(clazz: Class<T>): T {
    return objectMapper.convertValue(this, clazz)
}

fun <T: Any> JsonNode.parseJSONObject(typeReference: TypeReference<T>): T {
    return objectMapper.convertValue(this, objectMapper.constructType(typeReference.referencedType))
}