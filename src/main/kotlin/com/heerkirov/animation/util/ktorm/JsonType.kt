package com.heerkirov.animation.util.ktorm

import com.heerkirov.animation.util.objectMapper
import com.heerkirov.animation.util.parseJsonNode
import com.heerkirov.animation.util.toJSONString
import me.liuwj.ktorm.schema.BaseTable
import me.liuwj.ktorm.schema.Column
import me.liuwj.ktorm.schema.SqlType
import me.liuwj.ktorm.schema.TypeReference
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

class JsonType<T: Any>(private val converter: JsonConverter<T>) : SqlType<T>(Types.OTHER, typeName = "jsonb") {
    override fun doGetResult(rs: ResultSet, index: Int): T? {
        val s = rs.getString(index)
        return if(s.isNullOrBlank()) {
            null
        }else{
            converter.getter(s.parseJsonNode())
        }
    }

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: T) {
        ps.setObject(index, converter.setter(parameter, objectMapper()).toJSONString(), Types.OTHER)
    }
}

class JsonStringType : SqlType<String>(Types.OTHER, typeName = "jsonb") {
    override fun doGetResult(rs: ResultSet, index: Int): String? {
        val s = rs.getString(index)
        return if(s.isNullOrBlank()) null else s
    }

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: String) {
        ps.setObject(index, parameter, Types.OTHER)
    }
}

fun <E: Any, C: Any> BaseTable<E>.json(name: String, typeReference: TypeReference<C>): Column<C> {
    return registerColumn(name, JsonType(JacksonConverter(typeReference)))
}

fun <E: Any, C: Any> BaseTable<E>.json(name: String, converter: JsonConverter<C>): Column<C> {
    return registerColumn(name, JsonType(converter))
}

fun <E: Any> BaseTable<E>.jsonString(name: String): Column<String> {
    return registerColumn(name, JsonStringType())
}