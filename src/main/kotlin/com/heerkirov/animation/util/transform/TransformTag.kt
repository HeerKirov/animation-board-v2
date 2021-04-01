package com.heerkirov.animation.util.transform

import com.heerkirov.animation.dao.AnimationTagRelations
import com.heerkirov.animation.dao.Tags
import com.heerkirov.animation.util.logger
import org.ktorm.database.Database
import org.ktorm.dsl.batchInsert
import org.ktorm.dsl.insertAndGenerateKey
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.sortedBy
import org.ktorm.entity.toList

class TransformTag(private val userLoader: UserLoader,
                   private val v1Database: Database,
                   private val database: Database) {
    private val log = logger<TransformTag>()

    fun transform(animationIdMap: Map<Long, Int>) {
        val tagIdMap = HashMap<Long, Int>()

        for (v1Tag in v1Database.sequenceOf(V1Tags).sortedBy { it.id }) {
            val id = database.insertAndGenerateKey(Tags) {
                set(it.name, v1Tag.name)
                set(it.introduction, v1Tag.introduction)
                set(it.createTime, v1Tag.createTime.toV2Time())
                set(it.updateTime, v1Tag.updateTime?.toV2Time() ?: v1Tag.createTime.toV2Time())
                set(it.creator, userLoader[v1Tag.creator].id)
                set(it.updater, userLoader[v1Tag.updater ?: v1Tag.creator].id)
            } as Int
            tagIdMap[v1Tag.id] = id
        }
        log.info("Transform ${tagIdMap.size} tags from v1.")

        val v1Relations = v1Database.sequenceOf(V1AnimationTags).toList()
        database.batchInsert(AnimationTagRelations) {
            for (v1Relation in v1Relations) {
                item {
                    set(it.tagId, tagIdMap[v1Relation.tagId])
                    set(it.animationId, animationIdMap[v1Relation.animationId])
                }
            }
        }
        log.info("Transform ${v1Relations.size} animation-tag's relation from v1.")
    }
}