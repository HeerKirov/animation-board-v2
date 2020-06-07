package com.heerkirov.animation.util.transform

import com.heerkirov.animation.dao.Comments
import com.heerkirov.animation.util.logger
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.batchInsert
import me.liuwj.ktorm.entity.sequenceOf
import me.liuwj.ktorm.entity.sortedBy
import me.liuwj.ktorm.entity.toList

class TransformComment(private val userLoader: UserLoader,
                       private val v1Database: Database,
                       private val database: Database) {
    private val log = logger<TransformComment>()

    fun transform(animationIdMap: Map<Long, Int>) {
        val v1Comments = v1Database.sequenceOf(V1Comments).sortedBy { it.id }.toList()
        database.batchInsert(Comments) {
            for (v1Comment in v1Comments) {
                item {
                    it.ownerId to userLoader[v1Comment.ownerId].id
                    it.animationId to animationIdMap[v1Comment.animationId]
                    it.score to v1Comment.score
                    it.title to v1Comment.shortComment
                    it.article to v1Comment.article
                    it.createTime to v1Comment.createTime.toV2Time()
                    it.updateTime to (v1Comment.updateTime?.toV2Time() ?: v1Comment.createTime.toV2Time())
                }
            }
        }
        log.info("Transform ${v1Comments.size} comments from v1.")
    }
}