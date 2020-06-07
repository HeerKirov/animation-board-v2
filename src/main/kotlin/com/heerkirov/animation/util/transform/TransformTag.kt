package com.heerkirov.animation.util.transform

import com.heerkirov.animation.dao.AnimationTagRelations
import com.heerkirov.animation.dao.Tags
import com.heerkirov.animation.util.logger
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.batchInsert
import me.liuwj.ktorm.dsl.insertAndGenerateKey
import me.liuwj.ktorm.entity.sequenceOf
import me.liuwj.ktorm.entity.sortedBy
import me.liuwj.ktorm.entity.toList

class TransformTag(private val userLoader: UserLoader,
                   private val v1Database: Database,
                   private val database: Database) {
    private val log = logger<TransformTag>()

    fun transform(animationIdMap: Map<Long, Int>) {
        val tagIdMap = HashMap<Long, Int>()

        for (v1Tag in v1Database.sequenceOf(V1Tags).sortedBy { it.id }) {
            val id = database.insertAndGenerateKey(Tags) {
                it.name to v1Tag.name
                it.introduction to v1Tag.introduction
                it.createTime to v1Tag.createTime.toV2Time()
                it.updateTime to (v1Tag.updateTime?.toV2Time() ?: v1Tag.createTime.toV2Time())
                it.creator to userLoader[v1Tag.creator].id
                it.updater to userLoader[v1Tag.updater ?: v1Tag.creator].id
            } as Int
            tagIdMap[v1Tag.id] = id
        }
        log.info("Transform ${tagIdMap.size} tags from v1.")

        val v1Relations = v1Database.sequenceOf(V1AnimationTags).toList()
        database.batchInsert(AnimationTagRelations) {
            for (v1Relation in v1Relations) {
                item {
                    it.tagId to tagIdMap[v1Relation.tagId]
                    it.animationId to animationIdMap[v1Relation.animationId]
                }
            }
        }
        log.info("Transform ${v1Relations.size} animation-tag's relation from v1.")
    }
}