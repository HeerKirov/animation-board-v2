package com.heerkirov.animation.dao

import com.heerkirov.animation.model.data.Tag
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.schema.*

object Tags : BaseTable<Tag>("tag") {
    val id = int("id").primaryKey()
    val name = varchar("name")
    val introduction = text("introduction")
    val group = varchar("group")
    val ordinal = int("ordinal")
    val animationCount = int("animation_count")
    val createTime = datetime("create_time")
    val updateTime = datetime("update_time")
    val creator = int("creator")
    val updater = int("updater")

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