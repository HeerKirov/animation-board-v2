package com.heerkirov.animation.util.ktorm

import com.heerkirov.animation.util.parseJSONObject
import com.heerkirov.animation.util.toJSONString
import me.liuwj.ktorm.schema.BaseTable
import me.liuwj.ktorm.schema.SqlType
import me.liuwj.ktorm.schema.TypeReference
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

class JsonType<T: Any>(private val typeReference: TypeReference<T>) : SqlType<T>(Types.OTHER, typeName = "jsonb") {
    override fun doGetResult(rs: ResultSet, index: Int): T? {
        val s = rs.getString(index)
        return if(s.isNullOrBlank()) {
            null
        }else{
            s.parseJSONObject(typeReference)
        }
    }

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: T) {
        ps.setObject(index, parameter.toJSONString(), Types.OTHER)
    }
}

class EnumType<T: Enum<T>>(private val enumClass: Class<T>) : SqlType<T>(Types.SMALLINT, typeName = "smallint") {
    private val values = {
        val getValues = enumClass.getDeclaredMethod("values")
        @Suppress("UNCHECKED_CAST")
        getValues(null) as Array<T>
    }()

    override fun doGetResult(rs: ResultSet, index: Int): T? {
        return values[rs.getInt(index)]
    }

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: T) {
        ps.setShort(index, parameter.ordinal.toShort())
    }
}

fun <E: Any, C: Enum<C>> BaseTable<E>.enum(name: String, typeReference: TypeReference<C>): BaseTable<E>.ColumnRegistration<C> {
    @Suppress("UNCHECKED_CAST")
    return registerColumn(name, EnumType(typeReference.referencedType as Class<C>))
}

fun <E: Any, C: Any> BaseTable<E>.json(name: String, typeReference: TypeReference<C>): BaseTable<E>.ColumnRegistration<C> {
    return registerColumn(name, JsonType(typeReference))
}