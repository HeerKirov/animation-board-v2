package com.heerkirov.animation.dao

import com.heerkirov.animation.model.data.Comment
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.schema.BaseTable
import me.liuwj.ktorm.schema.datetime
import me.liuwj.ktorm.schema.int
import me.liuwj.ktorm.schema.varchar

object Comments : BaseTable<Comment>("comment") {
    val id = int("id").primaryKey()
    val ownerId = int("owner_id")
    val animationId = int("animation_id")
    val score = int("score")
    val title = varchar("title")
    val article = varchar("article")
    val createTime = datetime("create_time")
    val updateTime = datetime("update_time")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = Comment(
            id = row[id]!!,
            ownerId = row[ownerId]!!,
            animationId = row[animationId]!!,
            score = row[score],
            title = row[title],
            article = row[article],
            createTime = row[createTime]!!,
            updateTime = row[updateTime]!!
    )
}