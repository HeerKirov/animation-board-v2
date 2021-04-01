package com.heerkirov.animation.util.ktorm

import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.SqlType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types


class ArrayType<T: Any>(private val stringConverter: StringConverter<T>) : SqlType<List<T?>>(Types.OTHER, typeName = "array") {
    override fun doGetResult(rs: ResultSet, index: Int): List<T?>? {
        @Suppress("UNCHECKED_CAST")
        val arr = rs.getArray(index).array as Array<Any?>
        return arr.map { if(it == null) null else stringConverter.getter(it.toString()) }
    }

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: List<T?>) {
        val arr = ps.connection.createArrayOf("text", parameter.map { if(it != null) stringConverter.setter(it) else null }.toTypedArray())
        ps.setArray(index, arr)
    }
}

fun <E: Any, C: Any> BaseTable<E>.array(name: String, stringConverter: StringConverter<C>): Column<List<C?>> {
    return registerColumn(name, ArrayType(stringConverter))
}
