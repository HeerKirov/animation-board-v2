package com.heerkirov.animation.dao

import com.heerkirov.animation.enums.StatisticType
import com.heerkirov.animation.model.data.Statistic
import com.heerkirov.animation.util.ktorm.jsonString
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.schema.*

object Statistics : BaseTable<Statistic>("statistics") {
    val id by long("id").primaryKey()
    val ownerId by int("owner_id")
    val type by enum("type", typeRef<StatisticType>())
    val key by varchar("key")
    val content by jsonString("content")
    val updateTime by datetime("update_time")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = Statistic(
            id = row[id]!!,
            ownerId = row[ownerId]!!,
            type = row[type]!!,
            key = row[key],
            content = row[content]!!,
            updateTime = row[updateTime]!!
    )
}