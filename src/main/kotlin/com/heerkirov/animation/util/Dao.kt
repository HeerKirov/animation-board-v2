package com.heerkirov.animation.util

import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.BadRequestException
import org.ktorm.dsl.*
import org.ktorm.expression.OrderByExpression
import org.ktorm.schema.ColumnDeclaring

/**
 * 提供一个工具类，方便地将在filter order属性中string定义的列转换为Ktorm列，并快速提取。
 */
class OrderTranslator(private val orderFieldName: String = "order", initializer: Builder.() -> Unit) {
    private val map: HashMap<String, ColumnDefinition> = hashMapOf()

    init { initializer(Builder()) }

    inner class Builder {
        val first = NullDefinition.FIRST
        val last = NullDefinition.LAST

        infix fun String.to(column: ColumnDeclaring<*>): ColumnDefinition {
            val columnDefinition = ColumnDefinition(column)
            map[this] = columnDefinition
            return columnDefinition
        }
        infix fun ColumnDefinition.nulls(nullDefinition: NullDefinition) {
            this.nullDefinition = nullDefinition
        }
    }

    enum class NullDefinition {
        FIRST, LAST
    }

    inner class ColumnDefinition(val column: ColumnDeclaring<*>) {
        var nullDefinition: NullDefinition? = null
    }

    operator fun get(field: String, direction: Int): OrderByExpression {
        val column = map[field] ?: throw BadRequestException(ErrCode.PARAM_ERROR, "Param '$orderFieldName' cannot accept value '$field'.")
        return if(direction > 0) {
            column.column.asc()
        }else{
            column.column.desc()
        }
    }

    fun orderFor(orders: List<Pair<Int, String>>): Array<OrderByExpression> {
        return orders.flatMap { (direction, field) ->
            val column = map[field] ?: throw BadRequestException(ErrCode.PARAM_ERROR, "Param '$orderFieldName' cannot accept value '$field'.")
            val orderByExpression = if(direction > 0) {
                column.column.asc()
            }else{
                column.column.desc()
            }
            if(column.nullDefinition != null) {
                arrayListOf(column.column.isNull().asc(), orderByExpression)
            }else{
                arrayListOf(orderByExpression)
            }
        }.toTypedArray()
    }
}

fun Query.orderBy(orders: List<Pair<Int, String>>, translator: OrderTranslator): Query {
    return this.orderBy(*translator.orderFor(orders))
}