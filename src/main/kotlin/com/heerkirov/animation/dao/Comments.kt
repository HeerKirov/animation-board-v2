package com.heerkirov.animation.dao

import com.heerkirov.animation.model.data.Comment
import org.ktorm.dsl.QueryRowSet
import org.ktorm.schema.BaseTable
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.varchar

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