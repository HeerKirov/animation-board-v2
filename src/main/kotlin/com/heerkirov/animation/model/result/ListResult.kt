package com.heerkirov.animation.model.result

import me.liuwj.ktorm.dsl.Query
import me.liuwj.ktorm.dsl.QueryRowSet

data class ListResult<T>(val total: Int, val result: List<T>)

fun <T> Query.toListResult(transform: (QueryRowSet) -> T): ListResult<T> {
    return ListResult(this.totalRecords, this.map(transform))
}

fun <T, R> ListResult<T>.map(transform: (T) -> R): ListResult<R> {
    return ListResult(this.total, this.result.map(transform))
}