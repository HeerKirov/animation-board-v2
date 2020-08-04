package com.heerkirov.animation.dao

import com.heerkirov.animation.model.data.Tag
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.schema.*

object Tags : BaseTable<Tag>("tag") {
    val id by int("id").primaryKey()
    val name by varchar("name")
    val introduction by text("introduction")
    val group by varchar("group")
    val ordinal by int("ordinal")
    val animationCount by int("animation_count")
    val createTime by datetime("create_time")
    val updateTime by datetime("update_time")
    val creator by int("creator")
    val updater by int("updater")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = Tag(
            id = row[id]!!,
            name = row[name]!!,
            introduction = row[introduction],
            group = row[group],
            ordinal = row[ordinal]!!,
            animationCount = row[animationCount]!!,
            createTime = row[createTime]!!,
            updateTime = row[updateTime]!!,
            creator = row[creator]!!,
            updater = row[updater]!!
    )
}