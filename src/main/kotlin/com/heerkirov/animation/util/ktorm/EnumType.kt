package com.heerkirov.animation.util.ktorm

import me.liuwj.ktorm.schema.BaseTable
import me.liuwj.ktorm.schema.SqlType
import me.liuwj.ktorm.schema.TypeReference
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types


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
