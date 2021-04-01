package com.heerkirov.animation.util.ktorm

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.heerkirov.animation.enums.RelationType
import com.heerkirov.animation.enums.toRelationType
import com.heerkirov.animation.util.*
import org.ktorm.schema.TypeReference
import java.time.LocalDateTime


interface JsonConverter<T: Any> {
    fun getter(json: JsonNode): T
    fun setter(obj: T, objectMapper: ObjectMapper): JsonNode
}

class JacksonConverter<T: Any>(private val typeReference: TypeReference<T>) : JsonConverter<T> {
    override fun getter(json: JsonNode): T {
        return json.parseJSONObject(typeReference)
    }

    override fun setter(obj: T, objectMapper: ObjectMapper): JsonNode {
        return obj.toJsonNode()
    }
}

class NullableListConverter<T: Any>(private val itemConverter: JsonConverter<T>) : JsonConverter<List<T?>> {
    override fun getter(json: JsonNode): List<T?> {
        return json.map {
            if(it != null && !it.isNull) itemConverter.getter(it) else null
        }
    }

    override fun setter(obj: List<T?>, objectMapper: ObjectMapper): JsonNode {
        return objectMapper.createArrayNode().apply { addAll(obj.map { if(it != null) itemConverter.setter(it, objectMapper) else null }) }
    }
}

class ListConverter<T: Any>(private val itemConverter: JsonConverter<T>) : JsonConverter<List<T>> {
    override fun getter(json: JsonNode): List<T> {
        return json.map { itemConverter.getter(it) }
    }

    override fun setter(obj: List<T>, objectMapper: ObjectMapper): JsonNode {
        return objectMapper.createArrayNode().apply { addAll(obj.map { itemConverter.setter(it, objectMapper) }) }
    }
}

class DateTimeConverter: JsonConverter<LocalDateTime> {
    override fun getter(json: JsonNode): LocalDateTime {
        return json.asText().parseDateTime()
    }

    override fun setter(obj: LocalDateTime, objectMapper: ObjectMapper): JsonNode {
        return obj.toDateTimeString().toJsonNode()
    }
}

class RelationConverter : JsonConverter<Map<RelationType, List<Int>>> {
    override fun getter(json: JsonNode): Map<RelationType, List<Int>> {
        val map = HashMap<RelationType, List<Int>>()
        json.fields().forEach { entry -> map[entry.key.toRelationType()] = entry.value.map { it.asInt() } }
        return map
    }

    override fun setter(obj: Map<RelationType, List<Int>>, objectMapper: ObjectMapper): JsonNode {
        val map = HashMap<String, List<Int>>()
        obj.map { entry -> map[entry.key.name] = entry.value }
        return map.toJsonNode()
    }
}