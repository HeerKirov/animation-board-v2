package com.heerkirov.animation.util.transform

import com.heerkirov.animation.dao.Comments
import com.heerkirov.animation.util.logger
import org.ktorm.database.Database
import org.ktorm.dsl.batchInsert
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.sortedBy
import org.ktorm.entity.toList

class TransformComment(private val userLoader: UserLoader,
                       private val v1Database: Database,
                       private val database: Database) {
    private val log = logger<TransformComment>()

    fun transform(animationIdMap: Map<Long, Int>) {
        val v1Comments = v1Database.sequenceOf(V1Comments).sortedBy { it.id }.toList()
        database.batchInsert(Comments) {
            for (v1Comment in v1Comments) {
                item {
                    set(it.ownerId, userLoader[v1Comment.ownerId].id)
                    set(it.animationId, animationIdMap[v1Comment.animationId])
                    set(it.score, v1Comment.score)
                    set(it.title, v1Comment.shortComment)
                    set(it.article, v1Comment.article)
                    set(it.createTime, v1Comment.createTime.toV2Time())
                    set(it.updateTime, v1Comment.updateTime?.toV2Time() ?: v1Comment.createTime.toV2Time())
                }
            }
        }
        log.info("Transform ${v1Comments.size} comments from v1.")
    }
}