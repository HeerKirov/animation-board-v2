package com.heerkirov.animation.util

import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.BadRequestException
import me.liuwj.ktorm.dsl.asc
import me.liuwj.ktorm.dsl.desc
import me.liuwj.ktorm.expression.OrderByExpression
import me.liuwj.ktorm.schema.ColumnDeclaring

/**
 * 提供一个工具类，方便地将在filter order属性中string定义的列转换为Ktorm列，并快速提取。
 */
class OrderTranslator(private val orderFieldName: String = "order", initializer: Builder.() -> Unit) {
    private val map: HashMap<String, ColumnDeclaring<*>> = hashMapOf()

    init { initializer(Builder()) }

    inner class Builder {
        infix fun String.to(column: ColumnDeclaring<*>) {
            map[this] = column
        }
    }

    operator fun get(field: String, direction: Int): OrderByExpression {
        val column = map[field] ?: throw BadRequestException(ErrCode.PARAM_ERROR, "Param '$orderFieldName' cannot accept value '$field'.")
        return if(direction > 0) {
            column.asc()
        }else{
            column.desc()
        }
    }
}