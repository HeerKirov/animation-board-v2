package com.heerkirov.animation.util.ktorm

import com.heerkirov.animation.enums.RelationType
import com.heerkirov.animation.enums.toRelationType
import com.heerkirov.animation.util.*
import me.liuwj.ktorm.schema.BaseTable
import me.liuwj.ktorm.schema.SqlType
import me.liuwj.ktorm.schema.TypeReference
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.time.LocalDateTime

class JsonType<T: Any>(private val converter: JsonConverter<T>) : SqlType<T>(Types.OTHER, typeName = "jsonb") {
    override fun doGetResult(rs: ResultSet, index: Int): T? {
        val s = rs.getString(index)
        return if(s.isNullOrBlank()) {
            null
        }else{
            converter.getter(s)
        }
    }

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: T) {
        ps.setObject(index, converter.setter(parameter), Types.OTHER)
    }
}

fun <E: Any, C: Any> BaseTable<E>.json(name: String, typeReference: TypeReference<C>): BaseTable<E>.ColumnRegistration<C> {
    return registerColumn(name, JsonType(JacksonConverter(typeReference)))
}

fun <E: Any, C: Any> BaseTable<E>.json(name: String, converter: JsonConverter<C>): BaseTable<E>.ColumnRegistration<C> {
    return registerColumn(name, JsonType(converter))
}

interface JsonConverter<T: Any> {
    fun getter(json: String): T
    fun setter(obj: T): String
}

class JacksonConverter<T: Any>(private val typeReference: TypeReference<T>) : JsonConverter<T> {
    override fun getter(json: String): T {
        return json.parseJSONObject(typeReference)
    }

    override fun setter(obj: T): String {
        return obj.toJSONString()
    }
}

class DateTimeListConverter : JsonConverter<List<LocalDateTime>> {
    override fun getter(json: String): List<LocalDateTime> {
        return json.parseJsonNode().map { it.asText().toDateTime() }
    }

    override fun setter(obj: List<LocalDateTime>): String {
        return obj.map { it.toDateTimeString() }.toJSONString()
    }
}

class RelationConverter : JsonConverter<Map<RelationType, List<Int>>> {
    override fun getter(json: String): Map<RelationType, List<Int>> {
        val map = HashMap<RelationType, List<Int>>()
        json.parseJsonNode().fields().forEach { entry -> map[entry.key.toRelationType()] = entry.value.map { it.asInt() } }
        return map
    }

    override fun setter(obj: Map<RelationType, List<Int>>): String {
        val map = HashMap<String, List<Int>>()
        obj.map { entry -> map[entry.key.name] = entry.value }
        return map.toJSONString()
    }
}